package com.example.esds2s.Core.State

sealed class SpeechChatBotState {

    object Initial : SpeechChatBotState()
    object Listening : SpeechChatBotState()
    object Processing : SpeechChatBotState()
    object Completed : SpeechChatBotState()
    object Canceled : SpeechChatBotState()
    class Error(val message: String) : SpeechChatBotState()
}

sealed class MeasureSpeechChatBotNanoTimeState {

    object Start : SpeechChatBotState()
    object Listening : SpeechChatBotState()
    object Processing : SpeechChatBotState()
    object Completed : SpeechChatBotState()
    object Canceled : SpeechChatBotState()
    class Error(val message: String) : SpeechChatBotState()
}


