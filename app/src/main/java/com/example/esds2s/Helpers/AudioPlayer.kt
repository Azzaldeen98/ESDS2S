package com.example.esds2s.Helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.media.MediaPlayer
import android.util.Log
import com.example.esds2s.ApiClient.Interface.IMediaPlayerListener
import com.example.esds2s.Helpers.Enums.AvailableLanguages
import com.example.esds2s.Helpers.Enums.GenderType
import com.google.gson.Gson
import java.util.concurrent.Semaphore

class AudioPlayer(private val context: Context?) {
     var mediaPlayer: MediaPlayer? =null
    private val sim = Semaphore(1)

    fun startAsync(filePath: String?,callback: IMediaPlayerListener): MediaPlayer? {
        if(filePath?.isNullOrEmpty()==true)
            return  null

        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(filePath)
            mediaPlayer?.setOnPreparedListener {
                it?.start()
                it.setOnErrorListener { mp, what, extra ->
                    try {
                        mp?.stop()
                        mp?.reset()
                        mp?.release()
                    }  finally {
                        callback.onErrorListener(mp,what,extra)
                    }
                    true
                }
                it.setOnCompletionListener { mp ->
                    try {
                        mp?.stop()
                        mp?.reset()
                        mp?.release()
                    }  finally {
                        callback.onCompletionListener(mp)
                    }
                }
            }
            mediaPlayer?.prepareAsync()
            return mediaPlayer
        } catch (e: Exception) {
            e.printStackTrace()
            throw  Exception(e.message.toString())
            return  null
        }
    }
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
        finally{
            return mediaPlayer
        }

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
    fun startFromRowResourceAsync(context:Context,row_id: Int,callback: IMediaPlayerListener): MediaPlayer? {

        if(row_id!=null && row_id>-1 ){
//        if(row_id>-1 && modelInfo!=null && modelInfo?.getLanguage() == AvailableLanguages.ARABIC && modelInfo?.getGender()==GenderType.MALE ) {
            try {
                if (isResourceExist(context, row_id)) {
                    mediaPlayer = MediaPlayer.create(context, row_id)
                    mediaPlayer?.setOnPreparedListener {
                        it.start()
//                        callback.onPreparedListener(it)
                        it.setOnErrorListener { mp, what, extra ->
                            try {
                                mp?.stop()
                                mp?.reset()
                                mp?.release()
                            } catch (e: Exception) {
                                callback.onErrorListener(mediaPlayer,what,extra)
                            } finally {
                                callback.onErrorListener(mediaPlayer,what,extra)
                            }
                            true
                        }
                        it.setOnCompletionListener { mp ->
                            try {
                                mp?.stop()
                                mp?.reset()
                                mp?.release()

                            } catch (e: Exception) {

                            } finally {
                                callback.onCompletionListener(mp)
                            }
                        }
                    }
                    mediaPlayer?.prepareAsync()
                    return mediaPlayer
                } else {
                    throw IllegalStateException("not found resource!!")
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                return  null
            } catch (e: Exception) {
                e.printStackTrace()
//                throw Exception(e.message.toString())
                return  null
            }
            return  mediaPlayer
        } else{ return  null }
    }
    fun startFromRowResource(context: Context, row_id: Int): MediaPlayer? {

//       val lang=LanguageInfo.getStorageSelcetedLanguage(context)
//        val modelInfo=ModelInfo(context)

        if(row_id!=null && row_id>-1 ){ //&& modelInfo!=null && modelInfo?.getLanguage() == AvailableLanguages.ARABIC && modelInfo?.getGender()==GenderType.MALE ) {
//            Log.d("startFromRowResourcemodelInfo",Gson().toJson(modelInfo?.getInfo()))
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
            }  catch (e: Exception) {
                e.printStackTrace()
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
            if(mediaPlayer!=null) {
                mediaPlayer?.takeIf { it.isPlaying }?.let { it.stop() }
                mediaPlayer?.reset()
                mediaPlayer?.release()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }finally {
            mediaPlayer = null
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
            if(mediaPlayer!=null)
                return mediaPlayer?.isPlaying?:false

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