package com.example.esds2s.Models.RequestModels

import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequestMessage(
    val token_chat:String,
    val description:String,
    val voice_code:String,
)
