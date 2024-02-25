package com.example.esds2s.Helpers

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class AudioPlayer(private val context: Context?) {
     var mediaPlayer: MediaPlayer? =null
    fun start(filePath: String?): MediaPlayer? {
//        if(mediaPlayer!=null) stop();
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(filePath)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
//            mediaPlayer?.setOnCompletionListener({mp -> stop()});
            return mediaPlayer
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    fun stop() {

        try{
            if (mediaPlayer != null) {
                if (mediaPlayer?.isPlaying()!!)
                    mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    fun isPlayer():Boolean {
        return (mediaPlayer != null && mediaPlayer?.isPlaying()!!)
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