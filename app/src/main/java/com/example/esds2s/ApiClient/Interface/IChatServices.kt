package com.example.esds2s.ApiClient.Interface

import com.example.esds2s.Models.RequestModels.GeminiRequestMessage
import com.example.esds2s.Models.ResponseModels.BaseChatResponse
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import retrofit2.Call
import retrofit2.http.*


interface IChatServices {



    @GET("/todos/chats/")
    fun getAllChats(): Call<ArrayList<BaseChatResponse>?>?

//    @POST("/todos/consumerchats-message/")
    @POST("/todos/chat/message/")
    fun messageToGenerator(@Body body: GeminiRequestMessage): Call<GeminiResponse?>?

//    @Headers("Content-Type: application/json")
//    @POST("/todos/api")
//     fun audioToGenerator(@Body body: GeminiRequest): Call<GeminiResponse?>?

//    @POST("/todos/api/text")
//      fun textToGenerator(@Body body: GeminiRequest): Call<GeminiResponse?>?



//    @Multipart
//    @POST("/todos/upload-fileresulit/")
//     fun uploadFileAudio(@Part file :MultipartBody.Part?): Call<GeminiResponse?>?
}