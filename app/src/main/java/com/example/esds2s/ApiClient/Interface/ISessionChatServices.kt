package com.example.esds2s.ApiClient.Interface

import com.example.esds2s.Models.RequestModels.CustomerChatRequest
import com.example.esds2s.Models.ResponseModels.CustomerChatResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ISessionChatServices {

    @POST("/todos/api/text")
    fun createSessionChat(@Body body: CustomerChatRequest): Call<CustomerChatResponse?>?
}