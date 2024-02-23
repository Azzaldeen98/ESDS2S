package com.example.esds2s.Models.ResponseModels

import kotlinx.serialization.Serializable
@Serializable
data class RegisterResponse(
    override val token:String
) : AuthResponse(token)


