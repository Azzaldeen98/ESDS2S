package com.example.esds2s.Interface

import com.example.esds2s.Models.ResponseModels.GeminiResponse

interface IUplaodAudioEventListener {

    fun onRequestIsSuccess(response: GeminiResponse)
    fun onRequestIsFailure(error: String)
}