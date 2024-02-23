package com.example.esds2s.Models.ResponseModels

import kotlinx.serialization.Serializable

@Serializable
data class CustomerChatResponse(
    override var token:String
) : BaseResponse(token)
