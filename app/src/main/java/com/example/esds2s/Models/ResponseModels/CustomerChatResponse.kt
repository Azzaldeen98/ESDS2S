package com.example.esds2s.Models.ResponseModels

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerChatResponse(
    @SerializedName("token")
    @Expose
     val token:String
)
