package com.example.esds2s.Services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.media3.exoplayer.ExoPlayer
import com.example.esds2s.Activies.MainActivity
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.ApiClient.Interface.ICustomPlayerListener
import com.example.esds2s.Helpers.*
import com.example.esds2s.Interface.IBaseCallbackListener
import com.example.esds2s.Interface.ISpeechRecognizerServices
import com.example.esds2s.Interface.IWasmServiceEventListener
import com.example.esds2s.R
import com.example.esds2s.Services.ExternalServices.SpeechRecognizerService
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.util.*
import kotlin.coroutines.CoroutineContext
//, ViewModelStoreOwner

class LocalNotification{
    private val NOTIFICATION_ID = 2
    private val channelId = "local_notification_channel_id"
    private val channelName = "Local Notification Channel"
    private  val channelDescription = "This is an Local notification channel"

    private fun createLocalNotificationChannel(context: Context) {
        // Check if we're running on Android 8.0 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun showNotification(context: Context,title: String,description: String, icon:Int=R.drawable.baseline_notifications_active_24) {

        createLocalNotificationChannel(context)
        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        // Build the notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)  // Replace with your app's icon
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)  // Automatically remove the notification when the user taps it

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(NOTIFICATION_ID, builder.build())  // notificationId is a unique int for each notification
        }
    }
}
class RecordVoiceService : LifecycleService() , ISpeechRecognizerServices, IWasmServiceEventListener {


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
    private var dispatcher: CoroutineContext = Dispatchers.IO+Job()
    private var  scope:CoroutineScope?=null;
    private var  localNotification:LocalNotification?=null;

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
            sendRequestToGenerator(result)
        }else{
            speechRecognizerService?.speechRecognizerListenAgain()
        }
    }
    private  fun sendRequestToGenerator(speechText:String) {
        isResponse=false
        var scope=CoroutineScope(dispatcher)

        try {

            if(TestConnection.isOnline(this,false)) {
                try{
                  if(job?.isActive==true){ job?.cancel() }
                } finally {
                    job=speechChatControl?.generateStreamTextAudio7Last4(speechText, dispatcher, scope)
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

            } else {

                localNotification?.showNotification(this,
                    getString(R.string.msg_no_internet_ar),
                    getString(R.string.msg_no_internet),
                    R.drawable.baseline_signal_wifi_statusbar_connected_no_internet_4_24)

                scope?.launch {
                    while(!TestConnection.isOnline(this@RecordVoiceService,false)){
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
            stopAudioPlayer()
        } finally {
            speechRecognizerService?.destroy()
        }

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