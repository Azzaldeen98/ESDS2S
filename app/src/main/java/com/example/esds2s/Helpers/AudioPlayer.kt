package com.example.esds2s.Helpers

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

class AudioPlayer(private val context: Context?) {
     var mediaPlayer: MediaPlayer? =null
    fun start(filePath: String?): MediaPlayer? {
        if(filePath==null) return  null
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(filePath)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
//            mediaPlayer?.setOnCompletionListener({mp -> stop()});
            return mediaPlayer
        } catch (e: Exception) {
            e.printStackTrace()
            return  null
        }
    }
    fun startFromRowResource(context:Context,row_id: Int): MediaPlayer? {

        try {

            mediaPlayer=MediaPlayer.create(context,row_id)

            mediaPlayer?.start()
//            mediaPlayer?.setOnCompletionListener({mp -> stop()});
            return mediaPlayer
        } catch (e: Exception) {
            e.printStackTrace()
            return  null
        }
    }
    @SuppressLint("SuspiciousIndentation")
    fun stop() {

        try{
            if (mediaPlayer != null) {
                if (mediaPlayer?.isPlaying()!!)
                    mediaPlayer?.stop()
                    mediaPlayer?.reset()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    fun isPlayer():Boolean {
        try {
            return (mediaPlayer != null && mediaPlayer?.isPlaying() ?: false)?:false
        }catch (e: Exception) { return  false}
    }
    fun pause() {
        try{
            if (mediaPlayer != null) {
                if (mediaPlayer?.isPlaying()!!)
                    mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    fun resume() {
        try{

            if (mediaPlayer != null &&  !mediaPlayer?.isPlaying!!)
                    mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}