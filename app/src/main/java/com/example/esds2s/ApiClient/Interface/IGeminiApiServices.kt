package com.example.esds2s.ApiClient.Interface

import com.example.esds2s.Models.RequestModels.GeminiRequest
import com.example.esds2s.Models.RequestModels.GeminiRequestMessage
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*


interface IGeminiApiServices {


//    @GET("users/{id}")
//   suspend fun getUser(@Path("id") userId: String): Call<User>

    @Headers("Content-Type: application/json")
    @POST("/todos/api")
     fun audioToGenerator(@Body body: GeminiRequest): Call<GeminiResponse?>?

    @POST("/todos/api/text")
      fun textToGenerator(@Body body: GeminiRequest): Call<GeminiResponse?>?

    @POST("/todos/chat/message/")
     fun messageToGenerator(@Body body: GeminiRequestMessage): Call<GeminiResponse?>?

    @Multipart
    @POST("/todos/upload-fileresulit/")
     fun uploadFileAudio(@Part file :MultipartBody.Part?): Call<GeminiResponse?>?
}