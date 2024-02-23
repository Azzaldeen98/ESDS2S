package com.example.esds2s.Models.RequestModels

import kotlinx.serialization.Serializable

@Serializable
data class CustomerChatRequest(
    var chat:String,
    var consumertoken:String?=null,
    var name:String,
    var userdescription:String,
)
