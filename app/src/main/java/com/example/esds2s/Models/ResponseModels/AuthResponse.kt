package com.example.esds2s.Models.ResponseModels

import kotlinx.serialization.Serializable

@Serializable
sealed   class AuthResponse(
    open val token:String
)