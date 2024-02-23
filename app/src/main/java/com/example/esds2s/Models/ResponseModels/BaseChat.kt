package com.example.esds2s.Models.ResponseModels

import kotlinx.serialization.Serializable

@Serializable
data class BaseChat(
    var token:String?="",
    var modeldescription:String?="",
    var scope:String?="",

)
