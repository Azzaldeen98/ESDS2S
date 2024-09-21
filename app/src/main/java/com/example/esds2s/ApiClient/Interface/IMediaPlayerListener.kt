package com.example.esds2s.ApiClient.Interface

import android.media.MediaPlayer

interface IMediaPlayerListener {

//    fun onPreparedListener(it:MediaPlayer)
    fun onErrorListener(mp: MediaPlayer?, what: Int, extra: Int)
    fun onCompletionListener(mp: MediaPlayer)
}

interface ICustomPlayerListener<T> {

    //    fun onPreparedListener(it:MediaPlayer)
    fun onErrorListener(mp: T?){}
    suspend fun onSuspendErrorListener(mp: T?, error:java.lang.Exception){}
    fun onErrorListener(mp: T?, error:java.lang.Exception){}
    fun onCompletionListener(mp: T?){}
    fun onCompletionListener(mp: T?, isComplete:Boolean=false){}
    suspend fun onSuspendCompletionListener(mp: T?, isTheLastAudioClip:Boolean=false){}
//    fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int){}
}