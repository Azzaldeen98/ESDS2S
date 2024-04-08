package com.example.esds2s.Services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.LanguageInfo
import com.example.esds2s.Interface.IGeminiServiceEventListener
import com.example.esds2s.Activies.MainActivity
import com.example.esds2s.Helpers.DefaultSoundResource
import com.example.esds2s.Helpers.Enums.DefaultAudioStatus
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.R
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class RecordVoiceService : Service() , IGeminiServiceEventListener {


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

    private var atomAudioPlayer=AtomicReference<AudioPlayer>()
    var isSpeaking: Boolean = false
    get() { return audioPlayer?.isPlayer() ?: false }
    val simaphor = Semaphore(1)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {

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
            if (audioPlayer!=null && audioPlayer?.isPlayer()==true)
                audioPlayer?.stop();
        } finally {

            if(simaphor!=null && !simaphor.isFair)
                simaphor.release()
            if(speechRecognizer!=null )
                speechRecognizer?.cancel();
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
    private  fun sendRequestToGenerator(speechText:String) {

        voiceResponseCount = 0
        isResponse=false

        try {

            if (speechChatControl == null)
                speechChatControl = SpeechChatControl(this);

            if(TestConnection.isOnline(this@RecordVoiceService,false)) {

                if(speakerJob!=null && speakerJob?.isActive==true)
                         speakerJob?.cancel()
                
                speakerJob=CoroutineScope(Dispatchers.IO).async {
                    isSpeaking=true
                    while (voiceResponseCount<3 && !isResponse) {
                        var _audio:AudioPlayer?=null
                        try {
                            delay(3000)
                            _audio=startDefaultVoiceResponse()
                            if(_audio!=null) {
                                Log.d("_audio","_audio")
                                val duration = _audio.getRemainingDuration()?.toLong() ?: 0
                                delay(duration)
                            }
                        } finally {
                            voiceResponseCount++
                            if(_audio!=null && _audio?.isPlayer()==true)
                                _audio?.stop()
                        }
                    }
                }
                Log.d("beforeSpeechChatControl","_audio")
                speechChatControl?.messageToGeminiAndGeneratorAudio(speechText, this@RecordVoiceService);

            } else {
                Log.e("Internet", "Not Connection Internet !!!!")
            }

        } catch (e: Exception) {
            //TODO : Error in Request
            playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService)!!,DefaultAudioStatus.After,true)
            Log.d("Error ! ", e.message.toString())
        }
    }
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
    @SuppressLint("SuspiciousIndentation")
    override fun onRequestIsSuccess(response:GeminiResponse) {

        Log.e("onRequestIsSuccessr", "onRequestIsSuccess");


//                simaphor.acquire()

                runBlocking {

                try {

                    isResponse = true
                    if(response==null || response.description.isEmpty())
                        throw Exception("Responce is null");
                    if (speakerJob != null && speakerJob?.isActive == true) {
                        speakerJob?.join()
                        delay(2000)
                    }
                    Log.e("description", response?.description!!);
                    speechResponseResult(response?.description!!)

                }catch (e:Exception){
                    Log.e("responseError", e.message.toString());
                    playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService),DefaultAudioStatus.After,true)
                }
                }

    }
    @SuppressLint("SuspiciousIndentation")
    fun speechResponseResult(result: String) {

        try {

            if( result?.isNullOrEmpty()==false  && Helper.isAudioFile(result)){

                 var player = audioPlayer?.start(result)
                     if(player==null)
                         throw  Exception("audioPlayer is null !!");

                     Log.e("audioPlayer","audioPlayer");
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

            playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService),DefaultAudioStatus.After,true)
         }
    }
    override fun onRequestIsFailure(error: String) {
                runBlocking {

                    isResponse=true
                    if (audioPlayer==null)
                        audioPlayer=AudioPlayer(this@RecordVoiceService)
                    try {
                        Log.d("onRequestIsFailure", error)
                        if (speakerJob != null && speakerJob?.isActive == true) {
                            speakerJob?.join()
                            delay(2000)
                        }
                        Log.d("speakerJob.join", error)
                    }finally{
                        playDefaultVoiceResponse(DefaultSoundResource.getAgainQuestions(this@RecordVoiceService), DefaultAudioStatus.After, true)
                    }
                }

            Log.d("onRequestIsFailure", error)



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
            if(audioPlayer!=null)
                audioPlayer?.takeIf { it.isPlayer() }?.stop()
            speechRecognizerIntent=null;
        }
    }
    override fun stopService(name: Intent?): Boolean {
        Log.d("Stopping","Stopping Service")
            stopSpeechRecognizer();
        return super.stopService(name)
    }
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
    override fun onDestroy() {
        super.onDestroy()
        stopSpeechRecognizer();
        try {

            if(backgrounSendRequestJob!=null)
                backgrounSendRequestJob?.takeIf { it.isActive }?.cancel()
            if(backgroundMonitorOrderStatusJob!=null)
                backgroundMonitorOrderStatusJob?.takeIf { it.isActive }?.cancel()

        }catch (e:Exception){ }
        finally {

        }

    }


}