package com.example.esds2s.Helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.esds2s.Helpers.Enums.AvailableLanguages
import com.example.esds2s.Helpers.Enums.GenderType
import com.google.gson.Gson

class AudioPlayer(private val context: Context?) {
     var mediaPlayer: MediaPlayer? =null

    fun start(filePath: String?): MediaPlayer? {

        if(filePath?.isNullOrEmpty()==true)
            return  null

        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(filePath)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
            return mediaPlayer
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
        //  mediaPlayer?.setOnCompletionListener({mp -> stop()});
    }

    fun isResourceExist(context: Context, resourceId: Int): Boolean {
        return try {
            context?.resources?.getResourceName(resourceId)
            true
        } catch (e: Resources.NotFoundException) {
            false
        }
    }

    fun startFromRowResource(context:Context,row_id: Int): MediaPlayer? {


//       val lang=LanguageInfo.getStorageSelcetedLanguage(context)
        val modelInfo=ModelInfo(context)
        Log.d("startFromRowResourcemodelInfo",Gson().toJson(modelInfo?.getInfo()))
        if(modelInfo!=null && modelInfo?.getLanguage() == AvailableLanguages.ARABIC && modelInfo?.getGender()==GenderType.MALE ) {

            try {

                if (isResourceExist(context, row_id)) {
                    mediaPlayer = MediaPlayer.create(context, row_id)
                    if (mediaPlayer != null) {
                        mediaPlayer?.start()
                    }
                    return mediaPlayer
                } else {
                    throw IllegalStateException("ot found resource!!")
                }
            } catch (e: Exception) {
                throw Exception(e.message.toString())
            }
            return  mediaPlayer
        }else{
            return  null
        }

    }
    @SuppressLint("SuspiciousIndentation")
    fun stop() {

        try{
            mediaPlayer?.takeIf { it.isPlaying }?.let { it.stop() }
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = null

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    fun getRemainingDuration():Int {
        if(mediaPlayer==null) return  0
        var totalDuration = mediaPlayer?.duration?:0
        var currentPosition = mediaPlayer?.currentPosition?:0
        return totalDuration - currentPosition
    }
    fun isPlayer():Boolean {
        try {
            return mediaPlayer?.isPlaying==true
        }catch (e: Exception) {
            e.printStackTrace()
        }
        return  false
    }
    fun pause() {
        try{
            mediaPlayer?.takeIf { it.isPlaying }?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    fun resume() {
        try{
            mediaPlayer?.takeIf { !it.isPlaying }?.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}