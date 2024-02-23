package com.example.esds2s.Models.RequestModels

import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequestMessage(
    val token_chat:String?="12345",
    val description:String,
    val voice_code:String="ar-SA-1",
)
