package com.example.esds2s.Services
//14/9/2024
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.exoplayer.ExoPlayer
import com.example.esds2s.Activies.MainActivity
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.ApiClient.Interface.ICustomPlayerListener
import com.example.esds2s.Core.Notifications.LocalNotification
import com.example.esds2s.Core.State.SpeechChatBotState.*
import com.example.esds2s.Helpers.*
import com.example.esds2s.Interface.IBaseCallbackListener
import com.example.esds2s.Interface.ISpeechRecognizerServices
import com.example.esds2s.Interface.IWasmServiceEventListener
import com.example.esds2s.R
import com.example.esds2s.Services.ExternalServices.SpeechRecognizerService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.*
import kotlin.coroutines.CoroutineContext



//private val stateFlow = MutableStateFlow<SpeechChatBotState>(Completed)


//LifecycleService()
class RecordVoiceService3 : Service(), ISpeechRecognizerServices, IWasmServiceEventListener {


    companion object {
        const val LOG_TAG = "AudioRecordService"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "SERVICE_CHANNEL_ID"

    }
    private var isResponse:Boolean=true // AtomicBoolean(false)
    private var speechRecognizerIsListening: Boolean? = false
    private var backgrounSendRequestJob: Job? = null
    private var voiceResponseCount:Int=0
    private var textSpeachResult:String?=null;
    private lateinit var audioPlayer: AudioPlayer
    private var speechChatControl: SpeechChatControl? = null

    private var exoPlayer : ExoPlayerMedia?=null;
    private  var job:Job?=null;
    private var isSpeaking: Boolean = false
    private val simaphor = Semaphore(1)
    private var speechRecognizerService: SpeechRecognizerService?=null;
    private var dispatcher: CoroutineContext = Dispatchers.IO+SupervisorJob()
    private var  scope:CoroutineScope?=null;
    private var  localNotification: LocalNotification?=null;

    private val _speechFlow = MutableStateFlow<String?>(null)
    private val speechFlow: StateFlow<String?> = _speechFlow

    init {

    }
    @SuppressLint("SuspiciousIndentation")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
        try {
            localNotification= LocalNotification()
            speechRecognizerService = SpeechRecognizerService(this,this)
            speechChatControl = SpeechChatControl(this);
            Toast.makeText(this, "Background is working ", Toast.LENGTH_SHORT).show()
            startForegroundServiceWithNotification(this,getString(R.string.notify_service_description));
            speechRecognizerService?.initialization(
                recognizer = true,
                workingInTheContinuously = true,
                lang = "ar"
            );

            scope= CoroutineScope(dispatcher)
            startSpeechRecognition()
            speechRecognizerService?.speechRecognizerListenAgain()

        }
        catch (e:Exception){
            e.printStackTrace()
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
            stopSelf()
        }
        return START_STICKY
    }
    override fun onSpeechRecognizerResult(result:String?){
        Log.d("onSpeechRecognizerResult",result!!)
        if(result?.isNotEmpty()==true){
//            sendRequestToGenerator(result)
            onSpeechRecognized(result)
        }else{
            speechRecognizerService?.speechRecognizerListenAgain()
        }
    }
    private  fun sendRequestToGenerator2(speechText:String) {
        isResponse=false

        try {

            if(TestConnection.isOnline(this@RecordVoiceService3,false)) {
//                lifecycleScope?.launch{
                    try {
                        if (scope?.isActive == true) {
//                        if (job?.isActive == true) {
//                            job?.cancel()
//                        }
                            scope?.cancel()
                        }

                    } finally {
                        scope= CoroutineScope(dispatcher)
                        job = speechChatControl?.generateStreamTextAudio7Last4(
                            speechText,
                            dispatcher,
                            scope!!
                        )
                        job?.invokeOnCompletion {
                            println("End-CoroutineScope...");
                            scope?.launch(Dispatchers.Main) {
//                            simaphor?.withPermit {
//                                 scope?.cancel()
                                speechRecognizerService?.speechRecognizerListenAgain()
//                            }
                            }
                        }
                    }
//                }

            } else {

                localNotification?.showNotification(this,
                    getString(R.string.msg_no_internet_ar),
                    getString(R.string.msg_no_internet),
                    R.drawable.baseline_signal_wifi_statusbar_connected_no_internet_4_24)

                scope?.launch {
                    while(scope?.isActive==true && !TestConnection.isOnline(this@RecordVoiceService3,false)){
                        delay(2000)
                    }
                }
                Log.e("Internet", "Not Connection Internet !!!!")
            }

        } catch (e: Exception) {
            Log.e("Error !", e.message.toString())
            try {
                if(scope?.isActive==true){
                    scope?.cancel()
                }
            }finally {
                speechRecognizerService?.speechRecognizerListenAgain()
            }

        }
    }
    @SuppressLint("SuspiciousIndentation")
    private fun sendRequestToGenerator3(speechText: String) {
        isResponse = false

        if (TestConnection.isOnline(this, false)) {

                try {
                    if(scope?.isActive==true)
                    scope?.cancel()  // إلغاء الـ Scope الحالي إن وجد
                } finally {
                    scope = CoroutineScope(dispatcher)


//                    runBlocking {
//                    var stateFlow: MutableSharedFlow<SpeechChatBotState>? =null
                        scope?.launch{

                            var  stateFlow = speechChatControl?.generateStreamTextAudio7Last6(
                                speechText,
                                scope!!)
                            stateFlow?.collect { state ->
                                when (state) {
                                    is Listening -> Log.d("SpeechChatBotState", "Listening")
                                    is Completed -> {
                                        Log.d("SpeechChatBotState", "Completed")
                                        isResponse=true
                                       this@launch.cancel()
                                    }
                                    is Error -> {
                                        Log.d("SpeechChatBotState", "Error: ${state?.message}")
                                        state?.message
                                        this@launch.cancel()
                                    }
                                    is Exception ->  this@launch.cancel()
                                    is Initial -> {
                                        Log.d("SpeechChatBotState", "Initial")
                                    }
                                    else -> {
                                        Log.d("SpeechChatBotState", "Else")
                                    }
                                }

                            }
                    }?.invokeOnCompletion {
                            scope?.launch(Dispatchers.Main) {
                                speechRecognizerService?.speechRecognizerListenAgain()
                            }
                        }
//                    job?.join()
//                    delay(3000)

//                        job?.invokeOnCompletion {
//                            scope?.launch(Dispatchers.Main) {
//                                speechRecognizerService?.speechRecognizerListenAgain()
//                            }
//                        }

                }

        } else {
            notifyNoInternetConnection()
            reconnectOnInternet()
        }
    }


    fun onSpeechRecognized(text: String) {
        _speechFlow.value = text
    }
    fun startTimerWithScope(scope: CoroutineScope?) {
        val timer = Timer()

        // جدولة عملية يتم تنفيذها بعد 5 ثواني
        timer.schedule(object : TimerTask() {
            override fun run() {
                // إطلاق Coroutine في النطاق المحدد
                scope?.launch {
                    // هنا يمكنك وضع الكود الذي تريد تنفيذه
                    println("Task executed after delay!")
                }
            }
        }, 5000)  // التأخير بـ 5000 ميلي ثانية (5 ثواني)
    }
    private  fun startSpeechRecognition() {

         scope= CoroutineScope(dispatcher)
         scope?.launch {
             try {
//                 withTimeout(20000) {  // تحديد مدة 5000 ميلي ثانية (5 ثواني)
                     speechFlow.collect { speechText ->
                         speechText?.let {
                             processSpeechText(it)
                         }
//                     }
                 }
             } catch (e: TimeoutCancellationException) {
                 println("Coroutine timed out and was cancelled.")
                 withContext(Dispatchers.Main) {
                     speechRecognizerService?.speechRecognizerListenAgain()  // إعادة تشغيل خدمة التعرف
                 }
             }
         }
    }

    private fun processSpeechText(speechText: String) {

        scope?.launch {
            simaphor.withPermit { isResponse=false }
            // إرسال النص لتحويله إلى صوت
            speechChatControl?.generateStreamTextAudio7Last7(speechText, scope!!)
//            withContext(Dispatchers.Main) {
//                speechRecognizerService?.speechRecognizerListenAgain()  // إعادة تشغيل خدمة التعرف
//            }
            simaphor.withPermit { isResponse=true }
        }
        scope?.launch {
            while (!simaphor.withPermit { isResponse }){
                delay(2000)
            }
            withContext(Dispatchers.Main) {
                speechRecognizerService?.speechRecognizerListenAgain()  // إعادة تشغيل خدمة التعرف
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private  fun sendRequestToGenerator(speechText: String) {
        isResponse = false

        if (TestConnection.isOnline(this, false)) {

            try {
                if(scope?.isActive==true)
                    scope?.cancel()  // إلغاء الـ Scope الحالي إن وجد
            } finally {
                scope = CoroutineScope(dispatcher)


//                    runBlocking {
//                    var stateFlow: MutableSharedFlow<SpeechChatBotState>? =null
                scope?.launch{

                   speechChatControl?.generateStreamTextAudio7Last7(
                        speechText,
                        scope!!)

                }?.invokeOnCompletion {

                       scope?.launch(Dispatchers.Main) {
                           speechRecognizerService?.speechRecognizerListenAgain()
                       }

                }


//                    job?.join()
//                    delay(3000)



            }

        } else {
            notifyNoInternetConnection()
            reconnectOnInternet()
        }
    }
    suspend fun myFunction() {
        withContext(Dispatchers.Default) {
            // Coroutine work here
        }
        // Code here runs after the coroutine inside withContext finishes
    }


    private fun notifyNoInternetConnection() {
        localNotification?.showNotification(
            this,
            getString(R.string.msg_no_internet_ar),
            getString(R.string.msg_no_internet),
            R.drawable.baseline_signal_wifi_statusbar_connected_no_internet_4_24
        )
    }
    private fun reconnectOnInternet() {
        scope?.launch {
            while (isActive && !TestConnection.isOnline(this@RecordVoiceService3, false)) {
                delay(2000)
            }
        }
    }




    //===========================================================================================
    // Method to create the notification channel
    private fun createNotificationChannel(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Record_Audio_Service",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
    }
    // Method to start the foreground service with notification
    private fun startForegroundServiceWithNotification(context: Context,description: String) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent:PendingIntent
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(description)
            .setSmallIcon(R.drawable.baseline_mic_24)
            .addAction(R.drawable.baseline_mic_off_24, "إيقاف", pendingIntent)
            .setContentIntent(pendingIntent)
            .build()
        // Create the notification channel
        createNotificationChannel(context)
        // Start the foreground service with the notification
        startForeground(NOTIFICATION_ID, notification)
    }

//    private var requestPermissionLauncher: ActivityResultLauncher<String> =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (!isGranted) {
//                Log.d("POST_NOTIFICATION_PERMISSION", "USER DENIED PERMISSION")
//            } else {
//                Log.d("POST_NOTIFICATION_PERMISSION", "USER GRANTED PERMISSION")
//            }
//        }
    //===========================================================================================
    override fun onRequestIsFailure(error: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.e("onRequestIsFailure", error)
//            simaphor.withPermit {
                isResponse = true
                speechRecognizerService?.speechRecognizerListenAgain()
//            }
        }




//                runBlocking {

//                    isResponse=true
//                    if (audioPlayer==null)
//                        audioPlayer=AudioPlayer(this@RecordVoiceService)
//                    try {
//                        Log.d("onRequestIsFailure", error)
//                        if (speakerJob != null && speakerJob?.isActive == true) {
//                            speakerJob?.join()
//                            delay(1000)
//                        }

//                    }finally{
//                        playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService), DefaultAudioStatus.After, true)
//                    }
//                }




//            if (isSpeaking){ //audioPlayer != null && audioPlayer?.isPlayer() == true) {
//                _jop = CoroutineScope(Dispatchers.IO).async {
//                    val duration = audioPlayer?.getRemainingDuration()?.toLong() ?: 0
//                    delay(duration)
//                }
//
//            }

//        } catch (e:Exception){
//            Log.d("Error", e.message.toString());
//        }finally {
//            Log.d("onRequestIsFailure", "")
////            if(_jop!=null && _jop.isActive){
////                _jop.invokeOnCompletion {
////                    runBlocking {
////                        withContext(Dispatchers.Main) {
////                              playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService), DefaultAudioStatus.After, true)
////                        }
////                    }
////                }
////            }else{
////                 playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService),DefaultAudioStatus.After,true)
////            }
//
//        }
    }
    override fun startListener() {
//
        try {
            stopAudioPlayer()
            speechRecognizerService?.speechRecognizerListenAgain()
        }catch (e:java.lang.Exception){}
    }
    override fun stopService(name: Intent?): Boolean {
        Log.d("Stopping","Stopping Service")
        speechRecognizerService?.stopSpeechRecognizer()
        return super.stopService(name)
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            scope?.cancel()
            speechChatControl?.onDestroy()
//            stopAudioPlayer()
        } finally {
            speechRecognizerService?.destroy()
        }

    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun stopAudioPlayer(){
         try {
                 audioPlayer?.takeIf { it.isPlayer() }?.stop()
                 exoPlayer?.onDestroy()
         }catch (e:java.lang.Exception){
             e.printStackTrace()
         }
    }

    //===========================================================================================

    @SuppressLint("SuspiciousIndentation")
    override fun onRequestIsSuccess2(callBack: IBaseCallbackListener<Any?>?) {

        Log.d("onRequestIsSuccess2", "true")
        try {
            callBack?.onCallBackExecuted(null)
        }finally {
            CoroutineScope(Dispatchers.Main).launch {
                isResponse = true
                speechRecognizerService?.speechRecognizerListenAgain()

//                  try {
//                      if(job?.isActive==true)
//                          job?.cancelAndJoin()
//                  }finally {
//                      isResponse = true
//                      speechRecognizerListenAgain();
//                  }

            }
        }
    }
    @SuppressLint("SuspiciousIndentation")
    override fun onRequestIsSuccess(responseURL:String) {

        if(job?.isActive==true)
            job?.cancel()
        CoroutineScope(Dispatchers.Main).launch {
            try {

                isResponse = true
//                    if(responseURL==null || responseURL.isEmpty())
//                        throw Exception("Responce is null");

//                 if(exoPlayer!=null)
//                     exoPlayer= ExoPlayerMedia(this);

//                    (this as LifecycleOwner).lifecycle.addObserver(object : LifecycleObserver {
//                        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
//                        fun onPause() {
//                            exoPlayer?.player?.pause()
//                        }
//
//                        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
//                        fun onDestroy() {
//                            exoPlayer?.player?.release()
//                        }
//                    })

                exoPlayer?.start(responseURL, object : ICustomPlayerListener<ExoPlayer> {
                    override fun onErrorListener(mp: ExoPlayer?) {
                        try {
                            exoPlayer?.stop()
                        } finally {
                            speechRecognizerService?.speechRecognizerListenAgain()
                        }
                    }

                    override fun onCompletionListener(mp: ExoPlayer?) {
                        try {
                            exoPlayer?.stop()
                        } finally {
                            speechRecognizerService?.speechRecognizerListenAgain()
                        }
                    }
                });
                speechRecognizerService?.speechRecognizerListenAgain()



//                    speechResponseResult(responseURL)

            } catch (e: Exception) {
                Log.e("responseError", e.message.toString());
                speechRecognizerService?.speechRecognizerListenAgain()

//                    playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService),DefaultAudioStatus.After,true)
            }
        }


    }
    @SuppressLint("SuspiciousIndentation")
    fun speechResponseResult(result: String) {

        try {
            isResponse=true
            if( result?.isNullOrEmpty()==false  && Helper.isLocalAudioFile(result)){
                Log.e("speechResponseResult: ", result);
                var player = audioPlayer?.start(result)
                player?.setOnErrorListener { mp, what, extra ->
                    try {
                        isSpeaking = false
                        Log.e("errorPlyer", "OnErrorListener");
                        audioPlayer?.takeIf { it.isPlayer() }?.stop()
                    } catch (e: Exception) {
                        Log.e("Audio Player has Error", e.message.toString()); }
                    finally {     speechRecognizerService?.speechRecognizerListenAgain(); }
                    true // Return true if the error is considered handled, false otherwise
                }
                player?.setOnCompletionListener { mp ->
                    try {
                        isSpeaking = false
                        audioPlayer?.takeIf { it.isPlayer() }?.stop()
                        Log.d("Complate Plyer", "Complate Plyer Museic");
                    } catch (e: Exception) { Log.e("Complate Plyer", e.message.toString()); }
                    finally {     speechRecognizerService?.speechRecognizerListenAgain(); }
                }

            }else{
                throw  Exception("audioPlayer is null !!");
            }

        }catch (e:Exception){
            speechRecognizerService?.speechRecognizerListenAgain()
//            playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService),DefaultAudioStatus.After,true)
        }
    }
}