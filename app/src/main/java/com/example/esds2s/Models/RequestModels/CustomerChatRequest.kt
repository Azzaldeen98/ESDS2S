package com.example.esds2s.Models.RequestModels

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerChatRequest(
//    @SerializedName("token_chat")
//    @Expose
    val token_chat:String,
//    val consumertoken:String,
//    @SerializedName("name")
//    @Expose
    val name:String,
//    @SerializedName("description")
//    @Expose
    val description:String,
)
