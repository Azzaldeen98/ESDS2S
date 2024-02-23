package com.example.esds2s.Services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.esds2s.ApiClient.Controlls.ChatAiServiceControll
import com.example.esds2s.Helpers.AndroidAudioRecorder
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.Interface.IGeminiServiceEventListener
import com.example.esds2s.Interface.IUplaodAudioEventListener
import com.example.esds2s.MainActivity
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.R
import java.util.*


class RecordVoiceService : Service() , IGeminiServiceEventListener {


    companion object {
        const val LOG_TAG = "AudioRecordService"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "YOUR_CHANNEL_ID"
        const val MAX_SILENCE_DURATION = 10000 // 10 seconds
        const val MSG_STOP_RECORDING = 1

    }

    private var speechRecognizerIsListening: Boolean? = false
    private var audioRecorder: AndroidAudioRecorder? = null
    private var handler: Handler? = null
    private val isRecording = true
    private var audioPlayer: AudioPlayer? = null
    private var chatAiServiceControll: ChatAiServiceControll? = null
    private var record_audio_path = ""
    private var LANG = "ar"
    private lateinit var myRunnable: Runnable
    private var recordingThread: Thread? = null
    private var speechRecognizerIntent: Intent? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var reorderCounter:Int?=0;
    private var textSpeachResult:String?=null;

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {



        try {
            chatAiServiceControll = ChatAiServiceControll(this);
            record_audio_path = "${externalCacheDir?.absolutePath}/audiorecordtest.3gp"
            handler = Handler()
            audioPlayer = AudioPlayer(null);
            if(intent!=null) {
                if (intent != null && intent?.hasExtra("Lang") == true) {
                    LANG = intent?.getStringExtra("Lang") ?: "en-Us";
                }
            }
        } catch (e:Exception){
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(this, "Background is working ", Toast.LENGTH_SHORT).show()
        startForegroundServiceWithNotification(this);
        InitializeSpeechRecognizer(intent);
        startSpeechRecognizerListening();
        return START_STICKY
    }

    private fun InitializeSpeechRecognizer(intent: Intent?) {
        try {
            if(this==null) return
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@RecordVoiceService);
            speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            // Define the language model used for voice recognition
            speechRecognizerIntent?.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            // Specify the preferred language for voice recognition
            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANG);

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
            }
            override fun onBufferReceived(bytes: ByteArray) {
                Log.d("onBufferReceived", bytes.size.toString() + "")
            }
            override fun onEndOfSpeech() {
                Log.d("EndOfSpeech", "End Speech")
            }
            override fun onError(i: Int) {

                Log.d("onError", i.toString() + "")
                when(i) {
                    //1
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {}
                    //2
                    SpeechRecognizer.ERROR_NETWORK -> {
                        Log.d("ERROR_NETWORK", i.toString() + "")
                    }
                    //3
                    SpeechRecognizer.ERROR_AUDIO -> {
                        Toast.makeText(this@RecordVoiceService, "Audio recording error", Toast.LENGTH_SHORT).show()
                    }
                    //4
                    SpeechRecognizer.ERROR_SERVER -> {}
                    //5
                    SpeechRecognizer.ERROR_CLIENT -> {}
                    //6
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {}
                    //7
                    SpeechRecognizer.ERROR_NO_MATCH -> {}
                    //8
                    SpeechRecognizer. ERROR_RECOGNIZER_BUSY -> {

                        val errorList = ArrayList<String>(1)
                        errorList.add("ERROR RECOGNIZER BUSY")
//                        if (mListener != null) mListener.onResults(errorList)
                    }
                    //9
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {}
                }
                speechRecognizerListenAgain()
            }
            override fun onResults(bundle: Bundle) {
                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                textSpeachResult=data!![0]
                Toast.makeText(this@RecordVoiceService,textSpeachResult, Toast.LENGTH_SHORT).show()
                if(textSpeachResult?.length!!>1)
                    sendRequestToGenerator(textSpeachResult!!);
                else
                    speechRecognizerListenAgain();

            }
            override fun onPartialResults(bundle: Bundle) {}
            override fun onEvent(i: Int, bundle: Bundle) {
                Log.d("onEvent", i.toString() + "")
            }
        })
    }
    private fun startSpeechRecognizerListening() {
        if (!speechRecognizerIsListening!!) speechRecognizerIsListening=true
        if (this@RecordVoiceService.speechRecognizer != null && this@RecordVoiceService.speechRecognizerIntent != null){
            this@RecordVoiceService.speechRecognizer ?.startListening(this@RecordVoiceService.speechRecognizerIntent !!)
        }
    }
    private fun sendRequestToGenerator(speechText:String) {
        try {
            if (chatAiServiceControll != null)
                 chatAiServiceControll?.messageToGeneratorAudio(speechText, this@RecordVoiceService);
        } catch (e: Exception) {
            Log.d("Error ! ", e.message.toString())
        }
    }
    private fun speechRecognizerListenAgain() {
             if (speechRecognizerIsListening!!) {
                 speechRecognizerIsListening = false;
                 speechRecognizer?.cancel();
                 startSpeechRecognizerListening();
             }
         }
    // Method to create the notification channel
    private fun createNotificationChannel(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Record_Audio_Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
    // Method to start the foreground service with notification
    private fun startForegroundServiceWithNotification(context: Context) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(" خدمة الاستماع")
            .setContentText("هذه الخدمة تعمل بشكل مستمر دون انقطاع ")
            .setSmallIcon(R.drawable.baseline_mic_24)
            .addAction(R.drawable.baseline_mic_off_24, "إيقاف", pendingIntent)
//            .addAction( NotificationCompat.Action())
        .setContentIntent(pendingIntent)
            .build()

        // Create the notification channel
        createNotificationChannel(context)

        // Start the foreground service with the notification
        startForeground(NOTIFICATION_ID, notification)
    }
    private  fun startMediaRecorder(){}
    override fun onRequestIsSuccess(response:GeminiResponse) {

             if(response!=null) {
                 if (response?.description != null) {
                     if (audioPlayer != null) {
                        val player= audioPlayer?.start(response?.description)

                         player?.setOnErrorListener { mp, what, extra ->
                             // Handle the error here
                             try {
                                 mp?.stop();
                                 mp.reset();
                                 mp.release();
                             }catch (e:Exception){  Log.e("error",e.message.toString() );}
                             finally { speechRecognizerListenAgain(); }
                             Log.e("error Plyer","" );
                             true // Return true if the error is considered handled, false otherwise
                         }

                        player?.setOnCompletionListener{ mp ->
                             Log.e("Complate Plyer", "Complate Plyer Museic");
                             mp?.stop();
                             mp.reset();
                             mp.release();
                             speechRecognizerListenAgain();
                         }
                     }
                 }
             }
             else
             { Log.e("responseError","!! response is empty or  null"); }

//        try { Helper.deleteFile(record_audio_path) }
//        catch (e:java.lang.Exception){ Log.e("Delete File Error",e.message.toString());}

    }
    override fun onRequestIsFailure(error: String) {

        Log.e("Error",error);
        if(reorderCounter!!<3 && textSpeachResult?.length!!>1){
            chatAiServiceControll?.messageToGeneratorAudio(textSpeachResult!!, this@RecordVoiceService);
        }
        else {
            speechRecognizerListenAgain();
        }
    }

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
            speechRecognizerIntent=null;

        }
        catch (e: Exception) {
            Log.d("Error ! ", e.message.toString())
        }
    }
    override fun stopService(name: Intent?): Boolean {
        Log.d("Stopping","Stopping Service")
//            if (audioRecorder != null) audioRecorder!!.stop();
//            if (audioPlayer != null) audioPlayer!!.stop();
            stopSpeechRecognizer();
        return super.stopService(name)
    }
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
    override fun onDestroy() {
        super.onDestroy()
        stopSpeechRecognizer();
    }

}