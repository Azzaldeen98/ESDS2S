package com.example.esds2s.Helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.esds2s.ContentApp.ContentApp
import java.io.*

class RecorderAudio(@NonNull  private val activity: Activity) {

    private var recorder: MediaRecorder? = null
    var RECORDER_SAMPLERATE = 8000
    var RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_CONFIGURATION_MONO
    var RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT

    //**********************************************************

    private var minBufferSizeIn = 0
    private var audioRecord: AudioRecord? = null
    private lateinit var audioData: ShortArray
    private var recording: Boolean? = true
    private var isPlayVoice=false;
    private var sampleRateInHz = 16000 //48000
    private  val TAG = "TAG"
    private var recordFilePath:String="";
    private var audioSessionId:Int = 0;

    init {
        recordFilePath="${activity?.externalCacheDir?.absolutePath}/recordAudio.pcm"
    }
    fun getRecordFilePath():String {
        return recordFilePath;
    }
    fun startRecord() {

        if (ContextCompat.checkSelfPermission(activity.applicationContext, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it from the user
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                ContentApp.REQUEST_MICROPHONE_PERMISSION_CODE
            )
        } else {
            val recordThread = Thread {
                record(activity.applicationContext)
            }
            recordThread.start()

        }

    }
    fun stopRecord() {
        recording=false;
    }
    fun getRecordDataBytes():ByteArray? {

        try {
            if(recordFilePath.isNullOrEmpty()) {
                throw java.lang.NullPointerException("file path is null.")
            }

            val file = File(recordFilePath)
            if (!file.exists()) {
                throw FileNotFoundException("The specified file does not exist.")
            }

           return file.readBytes();

        }catch (e:Exception){
            e.printStackTrace()
           return null
        }
    }
    fun onDestroy() {
        try{
            if(!recordFilePath.isNullOrEmpty()) {
                val file = File(recordFilePath)
                if(file!=null && file.exists())
                    file.delete()
            }

        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if(audioRecord!=null)
                audioRecord?.release()
        }
    }
    private fun record(@NonNull context:Context) {
        recording = true
        val file = File(recordFilePath)
        try {

            minBufferSizeIn = AudioRecord.getMinBufferSize(
                sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSizeIn)

            audioData = ShortArray(minBufferSizeIn)

            audioSessionId = audioRecord!!.audioSessionId

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }

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



//    private fun audioRecord2() {
//        var audioRecorder: AudioRecord?=null
//        val bufferSizeInBytes: Int
//        val bufferSizeInShorts: Int
//        var shortsRead: Int
//        val audioBuffer: ShortArray
//        var isRecording: Boolean
//        try {
//            // Get the minimum buffer size required for the successful creation of an AudioRecord object.
//            bufferSizeInBytes = AudioRecord.getMinBufferSize(
//                RECORDER_SAMPLERATE,
//                RECORDER_CHANNELS,
//                RECORDER_AUDIO_ENCODING
//            )
//            bufferSizeInShorts = bufferSizeInBytes / 2
//
//            // Initialize Audio Recorder.
//            if (ActivityCompat.checkSelfPermission(
//                    context,
//                    Manifest.permission.RECORD_AUDIO
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return
//            }
//            audioRecorder = AudioRecord(
//                MediaRecorder.AudioSource.VOICE_RECOGNITION,
//                RECORDER_SAMPLERATE,
//                RECORDER_CHANNELS,
//                RECORDER_AUDIO_ENCODING,
//                bufferSizeInBytes
//            )
//
//            // Start Recording.
//            audioBuffer = ShortArray(bufferSizeInShorts)
//            audioRecorder.startRecording()
//            isRecording = true
//            while (isRecording) {
//                shortsRead = audioRecorder.read(audioBuffer, 0, bufferSizeInShorts)
//                if (shortsRead == AudioRecord.ERROR_BAD_VALUE || shortsRead == AudioRecord.ERROR_INVALID_OPERATION) {
//                    Log.e("record()", "Error reading from microphone.")
//                    isRecording = false
//                    break
//                }
//
//                // Whatever your code needs to do with the audio here...
//            }
//        } finally {
//            if (audioRecorder != null) {
//                audioRecorder.stop()
//                audioRecorder.release()
//            }
//        }
//    }


}


class AndroidAudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    var RECORDER_SAMPLERATE = 8000
    var RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_CONFIGURATION_MONO
    var RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT

    //**********************************************************

    private var minBufferSizeIn = 0
    private var audioRecord: AudioRecord? = null
    private lateinit var audioData: ShortArray
    private var recording: Boolean? = true
    private var isPlayVoice=false;
    private var sampleRateInHz = 16000 //48000
    private  val TAG = "TAG"
    private var recordFilePath:String="";
    private var audioPlayer: AudioPlayer? = null
    private var audioSessionId:Int = 0;

    private fun createMediaRecorder(): MediaRecorder {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else
            MediaRecorder()
    }



    private fun audioRecord() {

    }
    private fun audioRecord2() {
        var audioRecorder: AudioRecord?=null
        val bufferSizeInBytes: Int
        val bufferSizeInShorts: Int
        var shortsRead: Int
        val audioBuffer: ShortArray
        var isRecording: Boolean
        try {
            // Get the minimum buffer size required for the successful creation of an AudioRecord object.
            bufferSizeInBytes = AudioRecord.getMinBufferSize(
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING
            )
            bufferSizeInShorts = bufferSizeInBytes / 2

            // Initialize Audio Recorder.
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
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
            audioRecorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                bufferSizeInBytes
            )

            // Start Recording.
            audioBuffer = ShortArray(bufferSizeInShorts)
            audioRecorder.startRecording()
            isRecording = true
            while (isRecording) {
                shortsRead = audioRecorder.read(audioBuffer, 0, bufferSizeInShorts)
                if (shortsRead == AudioRecord.ERROR_BAD_VALUE || shortsRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e("record()", "Error reading from microphone.")
                    isRecording = false
                    break
                }

                // Whatever your code needs to do with the audio here...
            }
        } finally {
            if (audioRecorder != null) {
                audioRecorder.stop()
                audioRecorder.release()
            }
        }
    }

    fun start(filePath: String) {
        try {

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setParameters("mic_zoom=1")

            recorder = MediaRecorder()
            recorder?.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            recorder?.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder?.setAudioChannels(1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                Log.d("Build.VERSION.SDK_INT","SDK_INT")
                recorder?.setPreferredMicrophoneDirection(MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER )
            }
            recorder?.setAudioEncodingBitRate(16000)
            recorder?.setAudioSamplingRate(44100)
            recorder?.setOutputFile(filePath)

            recorder?.prepare()
            recorder?.start()

            // Enable AEC and NS
            val audioSessionId = 0
//            if (AcousticEchoCanceler.isAvailable()) {
//                Log.d("AcousticEchoCanceler","Available")
//                val echoCanceler = AcousticEchoCanceler.create(audioSessionId)
//                echoCanceler?.enabled = true
//            }
//
//            if (NoiseSuppressor.isAvailable()) {
//                Log.d("NoiseSuppressor","Available")
//                val noiseSuppressor = NoiseSuppressor.create(audioSessionId)
//                noiseSuppressor?.enabled = true
//            }

            if (AcousticEchoCanceler.isAvailable()) {
                val aec = AcousticEchoCanceler.create(audioSessionId)
                if (aec != null) {
                    // Configure Acoustic Echo Canceler
                 aec?.enabled = true
                }
            } else {
                Log.e("AcousticEchoCanceler","Available")

                // Handle the case when Acoustic Echo Canceler is not available
            }

            if (NoiseSuppressor.isAvailable()) {
                val ns = NoiseSuppressor.create(audioSessionId)
                if (ns != null) {
                    // Configure Noise Suppressor
                    ns?.enabled = true
                }
            } else {
                Log.e("NoiseSuppressor","Available")
                // Handle the case when Noise Suppressor is not available
            }

        } catch (e: IllegalStateException) {
            Log.e("MediaRecorder", "IllegalStateException: ${e.message}")
        } catch (e: IOException) {
            Log.e("MediaRecorder", "IOException: ${e.message}")
        }

//        try {
//
//        } catch (e: IOException) {
//            e.printStackTrace();
////            Log.e("RecordAudio", "prepare() failed")
//        }





//        createRecorder().apply {
//            setAudioSource(MediaRecorder.AudioSource.MIC)
//            setOutputFormat(MediaRecorder.OutputFormat.MPEG_2_TS)
//            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//            setOutputFile(FileOutputStream(outputFile).fd)
//            prepare()
//            start()


    }

     fun stop() {
        recorder?.stop()
        recorder?.reset()
        recorder?.release()
        recorder = null
    }
}