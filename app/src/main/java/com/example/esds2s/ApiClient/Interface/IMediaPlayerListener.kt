package com.example.esds2s.ApiClient.Interface

import android.media.MediaPlayer
import androidx.media3.common.PlaybackException

interface IMediaPlayerListener {

//    fun onPreparedListener(it:MediaPlayer)
    fun onErrorListener(mp: MediaPlayer?, what: Int, extra: Int)
    fun onCompletionListener(mp: MediaPlayer)
}

interface ICustomPlayerListener<T> {

    //    fun onPreparedListener(it:MediaPlayer)
    fun onErrorListener(mp: T?){}

    fun onErrorListener(mp: T?, error:java.lang.Exception){}
    fun onCompletionListener(mp: T?){}
    fun onCompletionListener(mp: T?, isComplete:Boolean=false){}
}