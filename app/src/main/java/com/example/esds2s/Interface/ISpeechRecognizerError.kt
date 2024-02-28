package com.example.esds2s.Interface

import android.media.MediaPlayer
import com.example.esds2s.Services.ExternalServices.SpeechRecognizerService

interface ISpeechRecognizerError {

    fun setOnErrorNetworkTimeOutListener(listener: SpeechRecognizerService.OnErrorListener)
    fun setOnErrorAudioListener(listener:SpeechRecognizerService.OnErrorListener)
    fun setOnErrorNoMatchListener(listener:SpeechRecognizerService.OnErrorListener)
    fun setOnErrorRecognizerBusyListener(listener:SpeechRecognizerService.OnErrorListener)

}