package com.example.esds2s.Activies

import android.Manifest
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.Helpers.RecorderAudio
import com.example.esds2s.R
import java.io.*


class MainActivity2 : AppCompatActivity() {
    var startRec: Button? = null
    var stopRec : Button? = null
    var playBack: Button? = null
    var playVoice: Button? = null
    var minBufferSizeIn = 0
    var audioRecord: AudioRecord? = null
    lateinit var audioData: ShortArray
    var recording: Boolean? = true
    var isPlayVoice=false;
    var recorderAudio: RecorderAudio?=null;
    var sampleRateInHz = 16000 //48000
    private val TAG = "TAG"
    var recordFilePath:String="";
    private var audioPlayer: AudioPlayer? = null
     var audioSessionId:Int = 0;
    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.esds2s.R.layout.activity_main2)

        recordFilePath="${externalCacheDir?.absolutePath}/test.pcm"

        startRec = findViewById<View>(com.example.esds2s.R.id.startrec) as Button
        stopRec = findViewById<View>(com.example.esds2s.R.id.stoprec) as Button
        playBack = findViewById<View>(com.example.esds2s.R.id.playback) as Button
        playVoice = findViewById<View>(com.example.esds2s.R.id.playVoice) as Button


        playBack?.isEnabled = false
        startRec?.isEnabled = true
        stopRec?.isEnabled = false

        audioPlayer = AudioPlayer(this)
        recorderAudio=RecorderAudio(this);

        startRec?.setOnClickListener { v ->
            playBack?.isEnabled = false
            startRec?.isEnabled = false
            stopRec?.isEnabled = true
            checkMicrophonePermission();
        };
        stopRec?.setOnClickListener { v ->

            playBack?.isEnabled = true
            startRec?.isEnabled = false
            stopRec?.isEnabled = false
            recording = false
            recorderAudio!!.stopRecord()

        }
        playBack?.setOnClickListener { v ->
            playBack?.isEnabled = false
            startRec?.isEnabled = true
            stopRec?.isEnabled = false
            playRecord()

        }
        playVoice?.setOnClickListener { v ->
            if(!isPlayVoice) {
                startDefaultVoiceResponse()
                playVoice?.setText("Stop Voice")
            }
            else {
                audioPlayer?.completionAudio()
            }
            isPlayVoice=!isPlayVoice;
        }

//        minBufferSizeIn = AudioRecord.getMinBufferSize(
//            sampleRateInHz,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT
//        )
//        audioData = ShortArray(minBufferSizeIn)
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
//        audioRecord = AudioRecord(
//            MediaRecorder.AudioSource.MIC,
//            sampleRateInHz,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT,
//            minBufferSizeIn)
//
//
//        audioSessionId = audioRecord!!.audioSessionId



    }

    private fun checkMicrophonePermission(){

        AlertDialog.Builder(this)
            .setTitle("Switch to vibration sound mode ")
            .setIcon(com.example.esds2s.R.drawable.baseline_vibration_24)
            .setMessage(getString(com.example.esds2s.R.string.msg_mute_microphone_alarm_tone))
            .setPositiveButton(getString(com.example.esds2s.R.string.btn_ok)) { dialog, which ->
//                SettingsResourceForRecordServices.vibrateSoundMode(this.activity!!)
                if (ContextCompat.checkSelfPermission(this!!, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission is not granted, request it from the user
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        ContentApp.REQUEST_MICROPHONE_PERMISSION_CODE
                    )
                } else {
                    recording = true
                    recorderAudio?.startRecord();
//                    val recordThread = Thread {
//                        recording = true
//                        recorderAudio?.startRecord();
//                    }
//                    recordThread.start()


//                    speechRecognizerService?.startSpeechRecognizerListening()
                }
            }
            .setNegativeButton(getString(com.example.esds2s.R.string.btn_cancel)) { dialog, which ->}
            .create()
            .show()
        // Check if the permission has been granted

    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ContentApp.REQUEST_MICROPHONE_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, proceed with your app logic
                    recorderAudio?.startRecord();
                } else {
                    // Permission denied, handle accordingly (e.g., show explanation, disable functionality, etc.)
                }
                return
            }
            // Add more cases for other permissions if needed
        }
    }
    private   fun startDefaultVoiceResponse(){

        try {

            val player = audioPlayer?.startFromRowResource(this, R.raw.row10)

            player?.setOnErrorListener { mp, what, extra ->
                audioPlayer?.stop()
                playVoice?.setText("Play Voice")
                true // Return true if the error is considered handled, false otherwise
            }
            player?.setOnCompletionListener { mp ->
                audioPlayer?.stop()
                playVoice?.setText("Play Voice")
            }
        } catch (e: Exception) {
            Log.d("Error", e.message.toString())
        }

    }


    private fun startRecord() {
        val file = File(recordFilePath)

        // Ensure audioRecord and recording are properly initialized
//        val sampleRate = 16000 // Set your desired sample rate
//        val channelConfig = AudioFormat.CHANNEL_IN_MONO
//        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
//        val minBufferSizeIn = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
//        val audioData = ShortArray(minBufferSizeIn)
//        var audioRecord: AudioRecord? = null
//        var recording = true

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
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

//            audioRecord = AudioRecord(
//                MediaRecorder.AudioSource.MIC,
//                sampleRateInHz,
//                channelConfig,
//                audioFormat,
//                minBufferSizeIn)

            val outputStream = FileOutputStream(file)
            val bufferedOutputStream = BufferedOutputStream(outputStream)
            val dataOutputStream = DataOutputStream(bufferedOutputStream)


            val ns: NoiseSuppressor?
            val aec: AcousticEchoCanceler?

            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(audioSessionId)
                if (ns != null) {
                    ns.enabled = true
                } else {
                    Log.e(TAG, "AudioInput: NoiseSuppressor is null and not enabled")
                }
            }

            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(audioSessionId)
                if (aec != null) {
                    aec.enabled = true
                } else {
                    Log.e(TAG, "AudioInput: AcousticEchoCanceler is null and not enabled")
                }
            }
            audioRecord!!.startRecording()

            while (recording!!) {
                val numberOfShorts = audioRecord!!.read(audioData, 0, minBufferSizeIn)
                for (i in 0 until numberOfShorts) {
                    dataOutputStream.writeShort(audioData[i].toInt())
                    println("Record...")
                }
            }

            audioRecord!!.stop()
            dataOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            audioRecord?.release()
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun startRecord2() {
        val file = File(recordFilePath) //Environment.getExternalStorageDirectory(), "test.pcm")
        try {
            val outputStream = FileOutputStream(file)
            val bufferedOutputStream = BufferedOutputStream(outputStream)
            val dataOutputStream = DataOutputStream(bufferedOutputStream)
            val ns: NoiseSuppressor?
            val aec: AcousticEchoCanceler?
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(audioRecord!!.audioSessionId)
                if (ns != null) {
                    ns.enabled = true
                } else {
                    Log.e(TAG, "AudioInput: NoiseSuppressor is null and not enabled")
                }
            }
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(audioRecord!!.audioSessionId)
                if (aec != null) {
                    aec.enabled = true
                } else {
                    Log.e(TAG, "AudioInput: AcousticEchoCanceler is null and not enabled")
                }
            }
            audioRecord!!.startRecording()
            while (recording!!) {
                val numberOfShort = audioRecord!!.read(audioData, 0, minBufferSizeIn)
                for (i in 0 until numberOfShort) {
                    println("Record...")
                    dataOutputStream.writeShort(audioData[i].toInt())
                }
            }
            audioRecord!!.stop()
            dataOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(recorderAudio!=null) {
            recorderAudio?.onDestroy()
        }
    }
    private fun playRecord() {

        try {

            recordFilePath= recorderAudio!!.getRecordFilePath()
            println("PATH:${recordFilePath}")
            val file = File(recordFilePath)
            if (!file.exists()) {
                throw FileNotFoundException("The specified file does not exist.")
            }

            val shortSizeInBytes = java.lang.Short.SIZE / java.lang.Byte.SIZE
            val bufferSizeInBytes = (file.length() / shortSizeInBytes).toInt()
            val audioData = ShortArray(bufferSizeInBytes)

            val inputStream = FileInputStream(file)
            val bufferedInputStream = BufferedInputStream(inputStream)
            val dataInputStream = DataInputStream(bufferedInputStream)
            var i = 0
            while (dataInputStream.available() > 0) {
                audioData[i] = dataInputStream.readShort()
                i++
            }
            dataInputStream.close()
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes,
                AudioTrack.MODE_STREAM,
                audioSessionId)

            while (audioTrack.state != AudioTrack.STATE_INITIALIZED) {

            }
            audioTrack.play()
            audioTrack.write(audioData, 0, bufferSizeInBytes)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}