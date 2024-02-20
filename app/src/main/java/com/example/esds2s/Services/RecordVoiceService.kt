package com.example.esds2s.Services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.esds2s.ApiClient.Controlls.ChatAiServiceControll
import com.example.esds2s.Helpers.AndroidAudioRecorder
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.MainActivity
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.R
import java.util.*
import com.example.esds2s.Helpers.Helper

interface IUplaodAudioEventListener {
    fun onUplaodAudioIsSuccess(response: GeminiResponse)
    fun onUplaodAudioIsFailure(error: String)
}

class RecordVoiceService : Service() ,IUplaodAudioEventListener {



    companion object {
        const val LOG_TAG = "AudioRecordService"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "YOUR_CHANNEL_ID"
        const val MAX_SILENCE_DURATION = 10000 // 10 seconds
        const val MSG_STOP_RECORDING = 1

    }
    private var audioRecorder: AndroidAudioRecorder? = null
    private var handler: Handler? = null
        private val isRecording = true
        private var audioPlayer: AudioPlayer? = null
        private var chatAiServiceControll: ChatAiServiceControll? = null
    private var record_audio_path=""
    private lateinit var myRunnable: Runnable
    private var  recordingThread : Thread? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


//    if(checkSelfPermission(this@RecordVoiceService,android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
//        checkPermission();
//    }
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this@RecordVoiceService);
//        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

//        if (intent != null) {
//            val action = intent.action
//            if ("STOP_ACTION" == action) {
//                stopService(intent) // قم بتنفيذ العملية التي تريدها عند الضغط على الزر
//            }
//        }
        handler= Handler()
        audioPlayer= AudioPlayer(this);
        chatAiServiceControll= ChatAiServiceControll(null);
        record_audio_path="${externalCacheDir?.absolutePath}/audiorecordtest.3gp"
        Toast.makeText(this, "Background is working ", Toast.LENGTH_SHORT).show()
        startForegroundServiceWithNotification(this);
        startWorker()
        return START_STICKY
    }

     fun startWorker() {
//         Toast.makeText(this@RecordVoiceService, "Start Record ", Toast.LENGTH_SHORT).show()
//         recordingThread = Thread {
//             Log.d("recordingThread", "onStartCommand")
////             startRecord()
//         }
//         recordingThread?.start()

//         myRunnable = object : Runnable {
//             override fun run() {
//                        // Place your repeating task here
//                 starRecord();
//                        // Reschedule the Runnable
//                        handler!!.postDelayed(this, 1000) // Example: repeat every 1 second
//                    }
//                }
//                // Post the Runnable
//                handler!!.post(myRunnable)

    }
     fun startRecord() {
         Log.d("starRecord","starRecord")
         if(!isRecording)
             return;
         audioRecorder = AndroidAudioRecorder(this)
         audioRecorder?.start(record_audio_path)

        val timer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                    try {
                            if (audioRecorder != null)
                                audioRecorder?.stop();
                           chatAiServiceControll?.uploadAudioFile(record_audio_path, this@RecordVoiceService,this@RecordVoiceService);
                        } catch (e: Exception) {
                            Log.d("Error ! ", e.message.toString())
                        }
                Log.d("Timer", "Timer expired after 10 seconds")
                timer.cancel() // Stop the timer after 10 seconds
            }
        }
        timer.schedule(task, 5000)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (audioRecorder != null) audioRecorder!!.stop();
            if (audioPlayer != null) audioPlayer!!.stop();
        }
        catch (e: Exception) {
            Log.d("Error ! ", e.message.toString())
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

    private  fun startMediaRecorder(){


//        recorder =  MediaRecorder();
//        recorder!!.setAudioSource(MediaRecorder.AudioSource.MIC);
//        recorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        recorder!!.setOutputFile(dir_audio_path +"/audio.3gp");
//        recorder!!.prepare();
//        recorder!!.start();
    }
//
    override fun stopService(name: Intent?): Boolean {
        Log.d("Stopping","Stopping Service")
        try {
            if (audioRecorder != null) audioRecorder!!.stop();
            if (audioPlayer != null) audioPlayer!!.stop();
        }
        catch (e: Exception) {
        Log.d("Error ! ", e.message.toString())
      }


        return super.stopService(name)
    }
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onUplaodAudioIsSuccess(response:GeminiResponse) {


             if(response!=null) {
                 if (response?.description != null) {
                     if (audioPlayer != null) {
                         Log.d("audioPlayer", "Player....");
                         audioPlayer?.start(response?.description)?.setOnCompletionListener { mPlayer ->
                             Log.e("onUplaodAudioIsSuccess", "Complate Plyer Museic");
                             audioPlayer?.stop();
                             try { recordingThread?.destroy() } catch (e: java.lang.Exception) {
                                 Log.e("Error", e.message.toString());
                             } finally { startWorker(); }
                         }
                     }
                 }
                } else {
                    // Handle unsuccessful response here
                    Log.e("responseError","!! response is empty or  null");
                }
        Helper.deleteFile(record_audio_path)

    }

    override fun onUplaodAudioIsFailure(error: String) {

        try {
            recordingThread?.destroy()
        }
        catch (e:java.lang.Exception){ Log.e("Error",e.message.toString());}
        finally {
            startWorker();
        }
        Log.e("onFailure",error!!);
        Helper.deleteFile(record_audio_path)
    }


}