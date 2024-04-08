package com.example.esds2s.ApiClient.Interface

import android.media.MediaPlayer

interface IMediaPlayerListener {

//    fun onPreparedListener(it:MediaPlayer)
    fun onErrorListener(mp: MediaPlayer?, what: Int, extra: Int)
    fun onCompletionListener(mp: MediaPlayer)
}