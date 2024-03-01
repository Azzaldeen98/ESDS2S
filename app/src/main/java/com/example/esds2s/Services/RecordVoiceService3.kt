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
import com.example.esds2s.Helpers.Enums.TypesOfVoiceResponses
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.R
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.Semaphore

class RecordVoiceService3 : Service() , IGeminiServiceEventListener {


    companion object {
        const val LOG_TAG = "AudioRecordService"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "YOUR_CHANNEL_ID"

    }
    private var isResponse: Boolean=false
    private var speechRecognizerIsListening: Boolean? = false
    private var backgroundMonitorOrderStatusJob: Job? = null
    private var backgroundMonitorSpeakerStatusJob: Job? = null
    private var complatePlayerJop: Job? = null
    private var audioPlayer: AudioPlayer? = null
    private var speechChatControl: SpeechChatControl? = null
    private var speechRecognizerIntent: Intent? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var reorderCounter:Int=0;
    private var voiceResponseCount:Int=0
    private var textSpeachResult:String?=null;
    var isSpeaking: Boolean = false
    get() { return audioPlayer?.isPlayer() ?: false }

    val sim = Semaphore(1)


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {


            speechChatControl = SpeechChatControl(this);
            audioPlayer = AudioPlayer(this);

        } catch (e:Exception){
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(this, "Background is working ", Toast.LENGTH_SHORT).show()
        startForegroundServiceWithNotification(this);
        InitializeSpeechRecognizer(intent);
        startSpeechRecognizerListening();
        return START_STICKY
    }


    //===========================================================================================

    private fun InitializeSpeechRecognizer(intent: Intent?) {
        try {
            if(this==null) return
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@RecordVoiceService3);
            speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            // Define the language model used for voice recognition
            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_PROMPT, "")
            // Specify the preferred language for voice recognition
            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar");
//            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true);

        }catch (e:Exception){
            Toast.makeText(this, "SpeechRecognizer:"+e.message.toString(), Toast.LENGTH_SHORT).show()
        }
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
                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                textSpeachResult=data!![0]
                textSpeachResult=textSpeachResult.toString().trim() ?:""
                if(textSpeachResult?.isNullOrEmpty()==false && textSpeachResult?.length!!>0) {
//                    if(isSpeaking==false) {
                        Toast.makeText(this@RecordVoiceService3,textSpeachResult.toString(), Toast.LENGTH_SHORT).show()
                        sendRequestToGenerator(textSpeachResult!!);

                } else {
                    speechRecognizerListenAgain();
                }
            }
            override fun onPartialResults(bundle: Bundle) {}
            override fun onEvent(i: Int, bundle: Bundle) {
                Log.d("onEvent", i.toString() + "")
            }
        })
    }
    private fun startSpeechRecognizerListening() {
        if (!speechRecognizerIsListening!!) speechRecognizerIsListening=true
        if (this@RecordVoiceService3.speechRecognizer != null && this@RecordVoiceService3.speechRecognizerIntent != null){

            val lang= LanguageInfo.getStorageSelcetedLanguage(this)
//            if(lang!=null && speechRecognizerIntent?.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE)?.lowercase()!=lang.code?.lowercase())
            if(lang!=null && speechRecognizerIntent?.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE)?.equals(lang?.code,true) == false)
                 speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.code);

            this@RecordVoiceService3.speechRecognizer ?.startListening(this@RecordVoiceService3.speechRecognizerIntent !!) }
    }
    fun speechRecognizerListenAgain() {
             if (speechRecognizerIsListening!!) {
                 speechRecognizerIsListening = false;
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
//        val pendingIntent = PendingIntent.getActivity(context, 0,
//            notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
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

    private fun sendRequestToGenerator(speechText:String) {
        try {
            isResponse=false
            voiceResponseCount = 0
            if(TestConnection.isOnline(this@RecordVoiceService3,false)) {
                Log.d("sendRequestToGenerator ! ", speechText)

                if (speechChatControl != null) {
                    speechChatControl?.messageToGeneratorAudio(speechText, this@RecordVoiceService3);
                }
                voiceResponseCount = 0
                startDefaultVoiceResponse()
            }
            else{
                Log.d("Not Connection Internet !! ", "Faild!!")
            }
        } catch (e: Exception) {
            //TODO : Error in Request
            playDefaultVoiceResponse(TypesOfVoiceResponses.AGAINQUESTION.ordinal,true)
            Log.d("Error ! ", e.message.toString())
        }
    }
    private  fun playDefaultVoiceResponse(sound_num:Int,listenSpeechRecognizer:Boolean=false){

        try {


            val sound=Helper.getDefaultSoundResource(sound_num)
            val player = audioPlayer?.startFromRowResource(this@RecordVoiceService3, sound)

            if (player == null) {
                isSpeaking=false
                if(listenSpeechRecognizer)
                    speechRecognizerListenAgain();
                return
            }
            else {
                player?.setOnErrorListener { mp, what, extra ->
                    isSpeaking = false
                    try {
                        mp.stop()
                        mp.reset()
                        mp.release()
//                    audioPlayer?.takeIf { it.isPlayer() }?.stop()
                    }catch (e:Exception){ }
                    finally {
                        if (listenSpeechRecognizer)
                            speechRecognizerListenAgain();
                    }
                    true
                }
                player?.setOnCompletionListener { mp ->
                    isSpeaking = false
                    try {
                        mp.stop()
                        mp.reset()
                        mp.release()
//                    audioPlayer?.takeIf { it.isPlayer() }?.stop()
                    }catch (e:Exception){ }
                    finally {
                        if (listenSpeechRecognizer)
                            speechRecognizerListenAgain();
                    }
                }
            }

        }catch (e:Exception){
            e.printStackTrace()
            isSpeaking=false
            if(listenSpeechRecognizer)
                speechRecognizerListenAgain();
        }
    }
    @SuppressLint("SuspiciousIndentation")
    private   fun startDefaultVoiceResponse(sound_num:Int=-1){

        backgroundMonitorOrderStatusJob?.takeIf { it.isActive }?.cancel()
        backgroundMonitorOrderStatusJob = GlobalScope.launch(Dispatchers.Default) {

                delay(5000)


                Log.d("SuspiciousIndentation","5000" )

//                var sound_id = Helper.getDefaultSoundResource()

                try {
                    if(isResponse || audioPlayer == null){
                        return@launch
                    }

                    //  if(voiceResponseCount==0)
                    var  sound_id=Helper.getDefaultSoundResource(TypesOfVoiceResponses.MIDDLE.ordinal)
//                else if(voiceResponseCount==1)
//                    sound_id=Helper.getDefaultSoundResource(TypesOfVoiceResponses.ASKYOU.ordinal)
//                if(voiceResponseCount==2)
//                    sound_id=Helper.getDefaultSoundResource(TypesOfVoiceResponses.AGAINQUESTION.ordinal)

                    sim.acquire()
                    val player = audioPlayer?.startFromRowResource(this@RecordVoiceService3, sound_id)
                    if(player!=null) {
                        player?.setOnErrorListener { mp, what, extra ->
                            isSpeaking = false
                            voiceResponseCount = 0
                            audioPlayer?.takeIf { it.isPlayer() }?.stop()
                            playDefaultVoiceResponse(
                                TypesOfVoiceResponses.AGAINQUESTION.ordinal,
                                false
                            )
                            true // Return true if the error is considered handled, false otherwise
                        }
                        player?.setOnCompletionListener { mp ->
                            isSpeaking = false
                            audioPlayer?.takeIf { it.isPlayer() }?.stop()

//                            if (isResponse == false) {
//                                if (voiceResponseCount < 3) {
////                                    startDefaultVoiceResponse()
//                                } else {
//                                    voiceResponseCount = 0
////                                    speechRecognizerListenAgain()
//                                    return@setOnCompletionListener
////                                       playDefaultVoiceResponse(TypesOfVoiceResponses.AGAINQUESTION.ordinal,true)
//                                }
//                            } else
                            voiceResponseCount = 0
                        }
                    }

                }catch (e:Exception){
                    e.printStackTrace()
                }
                finally {
                    sim.release()
//                    if (voiceResponseCount < 3)
//                        voiceResponseCount++
//                    else
                        voiceResponseCount=0
                }
            }

    }

    //===========================================================================================
    @SuppressLint("SuspiciousIndentation")
    override fun onRequestIsSuccess(response:GeminiResponse) {

            try {

                sim.acquire()

                isResponse=true
                backgroundMonitorOrderStatusJob?.takeIf { it.isActive }?.cancel()

                if (audioPlayer==null)
                    audioPlayer=AudioPlayer(this@RecordVoiceService3)
                        if (audioPlayer?.isPlayer() == true) {
                            complatePlayerJop?.takeIf { it.isActive }?.cancel()
                            complatePlayerJop = GlobalScope?.launch {
                                try {
                                    sim.acquire()
                                    val duration = audioPlayer?.getRemainingDuration()?.toLong() ?: 0
                                    delay(duration)
                                    Log.e("duration audioPlayer", "duration");
                                } catch (e: Exception) {
                                    Log.e("stop audioPlayer", e.message.toString());
                                } finally {
                                    sim.release()
                                    speechResponseResult(response?.description!!)
                                }
                            }
                        }
                        else{
                             Log.e("description", response?.description!!);
                              speechResponseResult(response?.description!!)
                            }

            }catch (e:Exception){
                Log.e("responseError", e.message.toString());
                complatePlayerJop?.takeIf { it.isActive }?.cancel()
                backgroundMonitorOrderStatusJob?.takeIf { it.isActive }?.cancel()
            }
        finally {
            sim.release()
        }





    }
     @SuppressLint("SuspiciousIndentation")
     fun speechResponseResult(result: String) {


         backgroundMonitorOrderStatusJob?.takeIf { it.isActive }?.cancel()
         backgroundMonitorSpeakerStatusJob?.takeIf { it.isActive }?.cancel()
         try {

             if(audioPlayer == null)
                  audioPlayer = AudioPlayer(this@RecordVoiceService3)

             var player: MediaPlayer ?=null
                 if (result.isNullOrEmpty() || !Helper.isAudioFile(result)) {
                     Log.e("$$$$", "$$$$");
                     try {
//                         playDefaultVoiceResponse(TypesOfVoiceResponses.AGAINQUESTION.ordinal,false)
                         player = audioPlayer?.startFromRowResource(this@RecordVoiceService3, Helper.getDefaultSoundResource(TypesOfVoiceResponses.AGAINQUESTION.ordinal))
                     }catch (e:Exception){
                         Log.e("startPlayerFromRowResource", e.message.toString());
                     }
             }else{
                 try {
                     Log.e("audioPlayer","audioPlayer");
                     player = audioPlayer?.start(result)
                 }catch (e:Exception){
                     Log.e("audioPlayer_Error",e.message.toString());
                 }
             }

             if(player!=null){

                 Log.e("audioPlayer___MediaPlayer","audioPlayer");
                 isSpeaking=true
                 backgroundMonitorSpeakerStatusJob = GlobalScope.launch(Dispatchers.Default) {
                     Mutex().withLock {
                         while (isSpeaking) {
                             SettingsResourceForRecordServices.checkAudioPlayerSettings(this@RecordVoiceService3, audioPlayer)
                             delay(1000)
                         }
                     }
             }

             player?.setOnErrorListener { mp, what, extra ->
                 try {
                     isSpeaking = false
                     backgroundMonitorSpeakerStatusJob?.takeIf { it.isActive }?.cancel()
                     Log.e("errorPlyer","99999");
                     mp.stop()
                     mp.reset()
                     mp.release()

//                         audioPlayer?.takeIf { it.isPlayer() }?.stop()
                 } catch (e: Exception) {
                     Log.e("errorPlyer", e.message.toString()); }
                 finally {
                     speechRecognizerListenAgain(); }
                 true // Return true if the error is considered handled, false otherwise
             }
             player?.setOnCompletionListener { mp ->
                 try {
                     isSpeaking = false
                     backgroundMonitorSpeakerStatusJob?.takeIf { it.isActive }?.cancel()

                     mp.stop()
                     mp.reset()
                     mp.release()
//                         audioPlayer?.takeIf { it.isPlayer() }?.stop()
                     Log.d("Complate Plyer", "Complate Plyer Museic");
                 } catch (e: Exception) {
                     Log.e("Complate Plyer", e.message.toString()); }
                 finally {
                     speechRecognizerListenAgain(); }
             }

             }else{
                 isSpeaking=false
                 speechRecognizerListenAgain();
                 backgroundMonitorSpeakerStatusJob?.takeIf { it.isActive }?.cancel()
             }

         }catch (e:Exception){
             isSpeaking = false
             backgroundMonitorSpeakerStatusJob?.takeIf { it.isActive }?.cancel()
             playDefaultVoiceResponse(TypesOfVoiceResponses.AGAINQUESTION.ordinal,true)
             e.printStackTrace()
         }
         finally {
             Log.e("finallySpeechResponseResult","oooo");
         }

    }
    override  fun onRequestIsFailure(error: String) {

                isResponse = true
                try {
                    backgroundMonitorOrderStatusJob?.takeIf { it.isActive }?.cancel()
                    Log.d("onRequestIsFailure", reorderCounter.toString());
                    if (reorderCounter!! < 3 && textSpeachResult?.isNullOrEmpty() == false) {
                        speechChatControl?.messageToGeneratorAudio(textSpeachResult!!, this@RecordVoiceService3);

                        if(audioPlayer!=null && audioPlayer?.isPlayer()==true)
                             audioPlayer?.stop()

                        if(reorderCounter==1)
                             playDefaultVoiceResponse(Helper.getDefaultSoundResource(TypesOfVoiceResponses.ASKYOU.ordinal))

//                        if (audioPlayer?.isPlayer() == true) {
//                            complatePlayerJop?.takeIf { it.isActive }?.cancel()
//                            complatePlayerJop = GlobalScope.launch(Dispatchers.Default) {
//                                try {
//                                    val duration =
//                                        audioPlayer?.getRemainingDuration()?.toLong() ?: 0
//                                    delay(duration)
//                                    audioPlayer?.takeIf { it.isPlayer() }?.stop()
//                                    playDefaultVoiceResponse(Helper.getDefaultSoundResource())
//                                } catch (e: Exception) {
//                                    Log.e("stop audioPlayer", e.message.toString());
//                                }
//                            }
//                        } else {
//                            playDefaultVoiceResponse(Helper.getDefaultSoundResource())
//                        }
                        reorderCounter++
                    }
                    else {
                        Log.d("onRequestIsFailure", "")
                        reorderCounter = 0

                        playDefaultVoiceResponse(TypesOfVoiceResponses.AGAINQUESTION.ordinal, true)

//                        val sound=Helper.getDefaultSoundResource(TypesOfVoiceResponses.AGAINQUESTION.ordinal)
//                        val player = audioPlayer?.startFromRowResource(this@RecordVoiceService3, sound)
//                        if (player != null) {
//                            player?.setOnErrorListener { mp, what, extra ->
//                                audioPlayer?.takeIf { it.isPlayer() }?.stop()
//                                    speechRecognizerListenAgain();
//                                true }
//                            player?.setOnCompletionListener { mp ->
//                                audioPlayer?.takeIf { it.isPlayer() }?.stop()
//                                    speechRecognizerListenAgain();
//                            }
//                        }
//                        else{
//                            speechRecognizerListenAgain();
//                        }


//                        playDefaultVoiceResponse(TypesOfVoiceResponses.AGAINQUESTION.ordinal, true)
//                        speechRecognizerListenAgain()
//                        playDefaultVoiceResponse(TypesOfVoiceResponses.AGAINQUESTION.ordinal, false)
                    }


                } catch (e: Exception) {
                    Log.e("onRequestIsFailure", e.message.toString());
                    reorderCounter=0
                    speechRecognizerListenAgain();

                } finally {
//                    reorderCounter = if (reorderCounter < 3) reorderCounter + 1 else 0
                }


    }
    //===========================================================================================
    private  fun stopSpeechRecognizer(){
        try {

            if(speechRecognizerIsListening!!)
                speechRecognizerIsListening=false

            if (speechRecognizer != null) {
                speechRecognizer?.stopListening();
                speechRecognizer?.cancel();
                speechRecognizer?.destroy();
                speechRecognizer=null;
            }
            if(audioPlayer!=null) {
                audioPlayer?.stop()
            }
            speechRecognizerIntent=null;
        }
        catch (e: Exception) {
            Log.d("Error ! ", e.message.toString())
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
        backgroundMonitorOrderStatusJob?.takeIf { it.isActive }?.cancel()
        backgroundMonitorOrderStatusJob=null

    }

}