package com.example.esds2s.Models.ResponseModels

import kotlinx.serialization.Serializable

@Serializable
open class BaseResponse(
    open var token:String,
)
