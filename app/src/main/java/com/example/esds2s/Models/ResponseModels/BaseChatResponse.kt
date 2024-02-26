package com.example.esds2s.Models.ResponseModels

import kotlinx.serialization.Serializable

@Serializable
data class BaseChatResponse(
var token:String,
var modeldescription:String?="",
var scope:String
) : java.io.Serializable
