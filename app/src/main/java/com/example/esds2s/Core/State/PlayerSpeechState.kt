package com.example.esds2s.Core.State

sealed class PlayerSpeechState {

    object Initial : PlayerSpeechState()
    object Listening : PlayerSpeechState()
    object Processing : PlayerSpeechState()
    object Completed : PlayerSpeechState()
    object Canceled : PlayerSpeechState()
    class Error(val message: String) : PlayerSpeechState()
}