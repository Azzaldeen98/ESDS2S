package com.example.esds2s.Helpers

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class AndroidAudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null

    private fun createRecorder(): MediaRecorder {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else
            MediaRecorder()
    }

     fun start(filePath: String) {

         recorder = MediaRecorder().apply {
             setAudioSource(MediaRecorder.AudioSource.MIC)
             setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
             setOutputFile(filePath)
             setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

             try {
                 prepare()
             } catch (e: IOException) {
                 Log.e("RecordAudio", "prepare() failed")
             }

             start()
             recorder = this
         }



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
        recorder = null
    }
}