package com.example.esds2s.ApiClient.Interface

import com.example.esds2s.Models.RequestModels.CustomerChatRequest
import com.example.esds2s.Models.RequestModels.GeminiRequest
import com.example.esds2s.Models.ResponseModels.CustomerChatResponse
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST

interface ISessionChatServices {

//    @Headers("Content-Type:application/json")
    @POST("/todos/consumerchatsg/")
    fun createSessionChat(@Body body: CustomerChatRequest): Call<CustomerChatResponse?>?

    @POST("/todos/consumerchats-delete/")
    fun removeSession(@Body body: GeminiRequest): Call<GeminiResponse?>?
}