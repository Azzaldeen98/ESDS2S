package com.example.esds2s.Services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.media3.exoplayer.ExoPlayer
import com.example.esds2s.Activies.MainActivity
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.ApiClient.Interface.ICustomPlayerListener
import com.example.esds2s.Helpers.*
import com.example.esds2s.Helpers.Enums.DefaultAudioStatus
import com.example.esds2s.Interface.IBaseCallbackListener
import com.example.esds2s.Interface.IWasmServiceEventListener
import com.example.esds2s.R
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
//, ViewModelStoreOwner
class RecordVoiceService : LifecycleService() , IWasmServiceEventListener {


    companion object {
        const val LOG_TAG = "AudioRecordService"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "YOUR_CHANNEL_ID"

    }
    private var isResponse:Boolean=true // AtomicBoolean(false)
    private var speechRecognizerIsListening: Boolean? = false
    private var backgroundMonitorOrderStatusJob: Job? = null
    private var speakerJob: Job? = null
    private var backgrounSendRequestJob: Job? = null
    private var complatePlayerJop: Job? = null
    private lateinit var audioPlayer: AudioPlayer
    private var speechChatControl: SpeechChatControl? = null
    private var speechRecognizerIntent: Intent? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var reorderCounter:Int=0;
    private var voiceResponseCount:Int=0
    private var textSpeachResult:String?=null;
    private var exoPlayer : ExoPlayerMedia?=null;
    private var atomAudioPlayer=AtomicReference<AudioPlayer>()
    var job:Job?=null;
    var isSpeaking: Boolean = false
    get() { return audioPlayer?.isPlayer() ?: false }
    val simaphor = Semaphore(1)
//    private val handler = Handler(Looper.getMainLooper())
    @SuppressLint("SuspiciousIndentation")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
        try {
            exoPlayer = ExoPlayerMedia(this)
            speechChatControl = SpeechChatControl(this);
            audioPlayer = AudioPlayer(this);
            atomAudioPlayer.compareAndSet(null, AudioPlayer(this))

            Toast.makeText(this, "Background is working ", Toast.LENGTH_SHORT).show()
            startForegroundServiceWithNotification(this);
            InitializeSpeechRecognizer(intent);
            startSpeechRecognizerListening();

        } catch (e:Exception){
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
            stopSelf()
        }
        return START_STICKY
    }
    //===========================================================================================
    private fun InitializeSpeechRecognizer(intent: Intent?) {
        try {
            if(this==null) return
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@RecordVoiceService);
            speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            // Define the language model used for voice recognition
            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_PROMPT, "")
            // Specify the preferred language for voice recognition
            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar");
//            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true);

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(bundle: Bundle) {
                    Log.d("onReadyForSpeech", "Ready Speech")
                }
                override fun onBeginningOfSpeech() {
                    Log.d("BeginningOfSpeech", "Start Speech")
                }
                override fun onRmsChanged(v: Float) {
//                if(isSpeaking)
//                 Log.e("RMS", v.toString() + "")
                }
                override fun onBufferReceived(bytes: ByteArray) {
                    Log.d("onBufferReceived", bytes.size.toString() + "")
                }
                override fun onEndOfSpeech() {
                    Log.d("EndOfSpeech", "End Speech")
                }
                override fun onError(i: Int) {

                    Log.d("onError", i.toString() + "")
//                when(i) {
//                    //1
//                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {}
//                    //2
//                    SpeechRecognizer.ERROR_NETWORK -> {
//                        Log.d("ERROR_NETWORK", i.toString() + "")
//                    }
//                    //3
//                    SpeechRecognizer.ERROR_AUDIO -> {
//                        Toast.makeText(this@RecordVoiceService3, "Audio recording error", Toast.LENGTH_SHORT).show()
//                    }
//                    //4
//                    SpeechRecognizer.ERROR_SERVER -> {}
//                    //5
//                    SpeechRecognizer.ERROR_CLIENT -> {}
//                    //6
//                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {}
//                    //7
//                    SpeechRecognizer.ERROR_NO_MATCH -> {}
//                    //8
//                    SpeechRecognizer. ERROR_RECOGNIZER_BUSY -> {
//
//                        val errorList = ArrayList<String>(1)
//                        errorList.add("ERROR RECOGNIZER BUSY")
////                        if (mListener != null) mListener.onResults(errorList)
//                    }
//                    //9
//                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {}
//                }
                    speechRecognizerListenAgain()
                }
                @SuppressLint("SuspiciousIndentation")
                override fun onResults(bundle: Bundle) {
                    textSpeachResult=null;
                    try {
                        val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (data == null || data.count()!! <1 || data?.get(0)?.isEmpty()!!)
                            speechRecognizerListenAgain();
                        else {
                            textSpeachResult = data.get(0).toString().trim()
                            Log.d("onResults", textSpeachResult.toString());
                            Toast.makeText(this@RecordVoiceService, textSpeachResult.toString(), Toast.LENGTH_SHORT).show()
                            sendRequestToGenerator(textSpeachResult.toString());
                        }

                    }catch (ex:Exception){
                        speechRecognizerListenAgain();
                    }

                }
                override fun onPartialResults(bundle: Bundle) {}
                override fun onEvent(i: Int, bundle: Bundle) {
                    Log.d("onEvent", i.toString() + "")
                }
            })

        }catch (e:Exception){
            Toast.makeText(this, "SpeechRecognizer:"+e.message.toString(), Toast.LENGTH_SHORT).show()
        }

    }
    private fun startSpeechRecognizerListening() {

        if (speechRecognizerIsListening==false) speechRecognizerIsListening=true
        if (this@RecordVoiceService.speechRecognizer != null && this@RecordVoiceService.speechRecognizerIntent != null){
            val lang= LanguageInfo.getStorageSelcetedLanguage(this)
//            if(lang!=null && speechRecognizerIntent?.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE)?.lowercase()!=lang.code?.lowercase())
            if(lang!=null && speechRecognizerIntent?.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE)?.equals(lang?.code,true) == false)
                 speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.code);
            this@RecordVoiceService.speechRecognizer ?.startListening(this@RecordVoiceService.speechRecognizerIntent !!) }
    }
    @SuppressLint("SuspiciousIndentation")
    fun speechRecognizerListenAgain() {

        textSpeachResult=""
        try{
            if(speechRecognizer!=null )
                speechRecognizer?.cancel();
        } finally {
                startSpeechRecognizerListening();
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
    private fun startForegroundServiceWithNotification(context: Context) {
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
            .setContentTitle(" خدمة الاستماع")
            .setContentText("هذه الخدمة تعمل بشكل مستمر دون انقطاع ")
            .setSmallIcon(R.drawable.baseline_mic_24)
            .addAction(R.drawable.baseline_mic_off_24, "إيقاف", pendingIntent)
        .setContentIntent(pendingIntent)
            .build()
        // Create the notification channel
        createNotificationChannel(context)
        // Start the foreground service with the notification
        startForeground(NOTIFICATION_ID, notification)
    }
    //===========================================================================================

    private  fun playDefaultVoiceResponse(sound_num:Int?,status: DefaultAudioStatus=DefaultAudioStatus.Before,listenSpeechRecognizer:Boolean=false){
        isSpeaking=false
        if(sound_num==null) {
            if(listenSpeechRecognizer)
                speechRecognizerListenAgain();
        } else {

            var sound = DefaultSoundResource.getAudioResource(this@RecordVoiceService, status)
            if (sound_num > -1)
                sound = sound_num

            playInitialDefaultVoiceResponse(sound, listenSpeechRecognizer)
        }
    }
    private  fun playInitialDefaultVoiceResponse(sound_num:Int?,listenSpeechRecognizer:Boolean=false){

        if(sound_num==null) {
            if(listenSpeechRecognizer)
                speechRecognizerListenAgain();
            return
        }

        try {
            if(audioPlayer==null) audioPlayer= AudioPlayer(this@RecordVoiceService)
            else if(audioPlayer?.isPlayer()==true) audioPlayer?.stop()

            Log.d("playInitialDefaultVoiceResponse88", sound_num.toString())
            isSpeaking=true
            var player = audioPlayer?.startFromRowResource(this@RecordVoiceService, sound_num)
            if(player==null){
                isSpeaking=false
                if(listenSpeechRecognizer)
                    speechRecognizerListenAgain();
            } else {

                player?.setOnErrorListener { mp, what, extra ->
                    isSpeaking = false
                    try {
                        audioPlayer.stop()
                    } finally {
                        if (listenSpeechRecognizer)
                            speechRecognizerListenAgain();
                    }
                    true
                };
                player?.setOnCompletionListener { mp ->
                    isSpeaking = false
                    try {
                        audioPlayer.stop()
                    } finally {
                        if (listenSpeechRecognizer)
                            speechRecognizerListenAgain();
                    }
                };
            }

        }catch (e:Exception){
            Log.e("error",e.message.toString())
            isSpeaking=false
            if(listenSpeechRecognizer)
                speechRecognizerListenAgain();
        }

    }
    @SuppressLint("SuspiciousIndentation")
    private   fun startDefaultVoiceResponse():AudioPlayer?{
        try {

                if(audioPlayer==null)
                    audioPlayer=AudioPlayer(this@RecordVoiceService)

                Log.d("startDefaultVoiceResponse", voiceResponseCount.toString())
                var sound_id = DefaultSoundResource.getAudioResource(this@RecordVoiceService, DefaultAudioStatus.Before)
                Log.d("getAudioResource", sound_id.toString())
                val player = audioPlayer?.startFromRowResource(this@RecordVoiceService, sound_id)
                if (player == null)
                    return null

                player.setOnErrorListener { mp, what, extra ->
                        isSpeaking = false
                    try{ audioPlayer?.stop()}
                    finally { }
                        true // Return true if the error is considered handled, false otherwise
                    }
                player?.setOnCompletionListener { mp ->
                        isSpeaking = false
                       try{ audioPlayer?.stop()}
                       finally { }
                    }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("ErrorSuspiciousIndentation", e.message.toString())
//            return
            throw Exception(e.message.toString())
        }finally {
            return audioPlayer
        }

    }
    //===========================================================================================
    private  fun sendRequestToGenerator(speechText:String) {

        voiceResponseCount = 0
        isResponse=false

        try {
            if (speechChatControl == null)
                speechChatControl = SpeechChatControl(this);

            if(TestConnection.isOnline(this@RecordVoiceService,false)) {

//                if(speakerJob!=null && speakerJob?.isActive==true)
//                         speakerJob?.cancel()

//                speakerJob=CoroutineScope(Dispatchers.IO).async {
//                    isSpeaking=true
//                    while (voiceResponseCount<3 && !isResponse) {
//                        var _audio:AudioPlayer?=null
//                        try {
//                            delay(3000)
//                            _audio=startDefaultVoiceResponse()
//                            if(_audio!=null) {
//                                Log.d("_audio","_audio")
//                                val duration = _audio.getRemainingDuration()?.toLong() ?: 0
//                                delay(duration)
//                            }
//                        } finally {
//                            voiceResponseCount++
//                            if(_audio!=null && _audio?.isPlayer()==true)
//                                _audio?.stop()
//                        }
//                    }
//                    if(voiceResponseCount>=3) {
//                        voiceResponseCount = 0;
//                        withContext(Dispatchers.Main) {
//                            playDefaultVoiceResponse(
//                                DefaultSoundResource.getAgainQuestions(this@RecordVoiceService)!!,
//                                DefaultAudioStatus.After,
//                                true
//                            );
//                        }
//                    }
//                }

//                Log.d("speechText=>",speechText)
//               speechChatControl?.generateBasicTextAudio(speechText,this@RecordVoiceService);

                      try{
                          println("End-runBlocking...");

        //
                          runBlocking {
                              if(job?.isActive==true)
                                job?.cancelAndJoin()
                          }

                      }
                      finally {
//                          runBlocking {
                          var dispatcher: CoroutineContext = Dispatchers.IO+ SupervisorJob()
                              job=CoroutineScope(dispatcher).launch {
                                  speechChatControl?.generateStreamTextAudio7Last2(
                                      speechText,
                                      dispatcher)
                              }
                              job?.invokeOnCompletion {
                                  println("End-CoroutineScope...");
                                  CoroutineScope(Dispatchers.Main).launch {
                                      speechRecognizerListenAgain()
                                  }
                              }
                              println("End-RunBlocking...");
//                          }
                      println("End-RunBlocking...");
//                  runBlocking{
////                      coroutineScope {
//                        CoroutineScope(dispatcher).launch {
//                          var jop=launch {
//                              speechChatControl?.generateStreamTextAudio8Last(
//                                  speechText,
//                                  dispatcher
//                              )
//                          }
//                          jop?.join()
//                      }
////                      }
//                      println("azd-runBlocking")
//                  }
//                  println("End-RunBlocking")
//
//                  speechRecognizerListenAgain()
//                  runBlocking {
//
//                      //.}
//                      CoroutineScope(Job() + Dispatchers.IO).launch {
//                          val job = launch {
//                              speechChatControl?.generateStreamTextAudio4Last(
//                                  speechText,
//                                  this@RecordVoiceService
//                              );
//                          }
////                      delay(1000)
////                      Log.d("Completed", "generateStreamText")
////                            withContext(Dispatchers.Main){
////                      speechRecognizerListenAgain()
////                            }
//                          //generateStreamTextAudioLast
//
//
//                          job?.join()
//                          Log.d("Completed", "Main Job")
//                          withContext(Dispatchers.Main){
//                              speechRecognizerListenAgain()
//                          }
//
//                      }
////                      job?.invokeOnCompletion { exception ->
////
////                          if (exception != null) {
////                              Log.d("CompletedError", exception.message ?: "Unknown error")
////                          }
////                          CoroutineScope(Dispatchers.Main).launch {
////                              speechRecognizerListenAgain()
////                          }
////                    }
//                  }
              }



//              }


//
//               speechChatControl?.getAudioFromApiServer(speechText,this@RecordVoiceService);
//              speechChatControl?.messageToGeminiAndGeneratorAudio(speechText, this@RecordVoiceService);
//              }
            } else {
                Log.e("Internet", "Not Connection Internet !!!!")
            }

        } catch (e: Exception) {
            Log.e("Error !", e.message.toString())
            try {
                if(job!=null && job?.isActive!! ){
                    job?.cancel()
                }
            }finally {
                speechRecognizerListenAgain();
            }
//            playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService)!!,DefaultAudioStatus.After,true)

        }
    }
    @SuppressLint("SuspiciousIndentation")
    override fun onRequestIsSuccess2(callBack: IBaseCallbackListener<Any?>?) {

        Log.d("onRequestIsSuccess2", "true")
       try {
           callBack?.onCallBackExecuted(null)
       }finally {
           CoroutineScope(Dispatchers.Main).launch {
               isResponse = true
               speechRecognizerListenAgain();
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
                            speechRecognizerListenAgain(); }
                    }

                    override fun onCompletionListener(mp: ExoPlayer?) {
                        try {
                            exoPlayer?.stop()
                        } finally {
                            speechRecognizerListenAgain(); }
                    }
                });
                speechRecognizerListenAgain()


//                    speechResponseResult(responseURL)
//
            } catch (e: Exception) {
                Log.e("responseError", e.message.toString());
                speechRecognizerListenAgain()
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
                         finally { speechRecognizerListenAgain(); }
                         true // Return true if the error is considered handled, false otherwise
                     }
                 player?.setOnCompletionListener { mp ->
                         try {
                             isSpeaking = false
                             audioPlayer?.takeIf { it.isPlayer() }?.stop()
                             Log.d("Complate Plyer", "Complate Plyer Museic");
                         } catch (e: Exception) { Log.e("Complate Plyer", e.message.toString()); }
                         finally { speechRecognizerListenAgain(); }
                    }

             }else{
                throw  Exception("audioPlayer is null !!");
            }

        }catch (e:Exception){
            speechRecognizerListenAgain();
//            playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService),DefaultAudioStatus.After,true)
         }
    }
    override fun onRequestIsFailure(error: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.e("onRequestIsFailure", error)
//            simaphor.withPermit {
                isResponse = true
                speechRecognizerListenAgain();
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

        try {
            stopAudioPlayer()
            this.startSpeechRecognizerListening()

        }catch (e:java.lang.Exception){}
    }
    override fun stopListener() {
        try {
           runBlocking {
                   simaphor.withPermit {
                   if (speechRecognizer != null)
                       speechRecognizer?.stopListening();
               }
           }



        }catch (e:java.lang.Exception){

        }
    }
    //===========================================================================================
    private  fun stopSpeechRecognizer(){
        try {
            if (speechRecognizer != null) {
                speechRecognizer?.stopListening();
                speechRecognizer?.cancel();
                speechRecognizer?.destroy();
                speechRecognizer=null;
            }
        }
        catch (e: Exception) { Log.d("Error ! ", e.message.toString()) }
        finally {

            speechRecognizerIntent=null;
        }
    }
    override fun stopService(name: Intent?): Boolean {
        Log.d("Stopping","Stopping Service")
            stopSpeechRecognizer();
        return super.stopService(name)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {

            speechChatControl?.onDestroy()
            stopAudioPlayer()

            if(backgrounSendRequestJob!=null)
                backgrounSendRequestJob?.takeIf { it.isActive }?.cancel()
            if(backgroundMonitorOrderStatusJob!=null)
                backgroundMonitorOrderStatusJob?.takeIf { it.isActive }?.cancel()

        }catch (e:Exception){ }
        finally {
            stopSpeechRecognizer();
        }

    }

    private fun stopAudioPlayer(){
     try {
             audioPlayer?.takeIf { it.isPlayer() }?.stop()
             exoPlayer?.onDestroy()
     }catch (e:java.lang.Exception){ }
    }


}