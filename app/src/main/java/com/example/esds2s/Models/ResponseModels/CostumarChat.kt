package com.example.esds2s.Models.ResponseModels

import kotlinx.serialization.Serializable

@Serializable
data class CostumarChat(
    var chat:String,
    var consumertoken:String,
    var name:String,
    var userdescription:String,
)
