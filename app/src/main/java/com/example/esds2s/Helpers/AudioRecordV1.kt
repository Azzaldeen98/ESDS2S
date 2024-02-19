package com.example.esds2s.Helpers

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Message
import android.util.Log
import java.io.IOException


class AudioRecordV2 {

    companion object {
        private const val LOG_TAG = "AudioRecordService"
        private const val MAX_SILENCE_DURATION = 10000 // 10 seconds
        private const val MSG_STOP_RECORDING = 1
        private const val MSG_START_RECORDING = 1
    }

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var handler: Handler? = null
    private var isRecording = false

     fun startRecording(filePath:String) {
        mediaRecorder = MediaRecorder()
        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder!!.setOutputFile(filePath)
        mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        try {
            mediaRecorder!!.prepare()
            mediaRecorder!!.start()
            isRecording = true
            startVoiceLevelDetection(filePath)
        } catch (e: IOException) {
            Log.e(LOG_TAG, "prepare() failed")
        }
    }
     fun startVoiceLevelDetection(filePath:String) {
        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_STOP_RECORDING) {
                    stopRecording()
                    playRecordedAudio(filePath)
                } else if (msg.what == MSG_START_RECORDING) {
                    startRecording(filePath) // Start a new recording session after stopping and playing
                }
            }
        }
        val startTime = System.currentTimeMillis()
        mediaRecorder!!.setOnInfoListener { mr, what, extra ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                // The maximum recording duration is reached
                handler?.sendEmptyMessage(MSG_STOP_RECORDING)
            }
        }
        // Use a Runnable to check the voice level and stop recording if silence is detected
        val silenceCheckRunnable: Runnable = object : Runnable {
            override fun run() {
                if (isRecording) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - startTime >= MAX_SILENCE_DURATION) {
                        // Stop recording if silence duration exceeds the maximum allowed
                        handler?.sendEmptyMessage(MSG_STOP_RECORDING)
                    } else {
                        // Check again after a short delay
                        handler?.postDelayed(this, 500) // Check every 500 milliseconds
                    }
                }
            }
        }
        handler?.postDelayed(silenceCheckRunnable, 500) // Initial check after 500 milliseconds
    }
     fun stopRecording() {
        if (mediaRecorder != null && isRecording) {
            mediaRecorder!!.stop()
            mediaRecorder!!.release()
            mediaRecorder = null
            isRecording = false
        }
    }
     fun playRecordedAudio(filePath:String) {
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(filePath)
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()
            mediaPlayer!!.setOnCompletionListener {
                // After playing the recorded audio, start a new recording session
                handler!!.sendEmptyMessage(MSG_START_RECORDING)
            }
        } catch (e: IOException) {
            Log.e(LOG_TAG, "playRecordedAudio() failed")
        }
    }
     fun onDestroy() {

        stopRecording()
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }


}