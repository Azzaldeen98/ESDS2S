package com.example.esds2s.Models.ResponseModels

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable


@Serializable
data class GeminiResponse(
    @SerializedName("description")
    @Expose
    val description:String
)

@Serializable
data class WasmSplitorResponse(
    @SerializedName("event_id")
    @Expose
    val event_id:String
)

@Serializable
data class WasmAudioResponse(
    @SerializedName("url")
    @Expose
    val url:String
)
