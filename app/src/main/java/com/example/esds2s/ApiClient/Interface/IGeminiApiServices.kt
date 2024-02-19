package com.example.esds2s.ApiClient.Interface

import com.example.esds2s.Models.RequestModels.GeminiRequest
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*


interface IGeminiApiServices {

//    @Headers("Content-Type: application/json",/*"User-Agent: Retrofit-Sample-App"*/)

//    @GET("users/{id}")
//    fun getUser(@Path("id") userId: String): Call<User>

    @Headers("Content-Type: application/json")
    @POST("/todos/api")
    fun generateAudio(@Body body: GeminiRequest): Call<GeminiResponse?>?

    @POST("/todos/api/text")
    fun generateAiText(@Body body: GeminiRequest): Call<GeminiResponse?>?

    @Multipart
    @POST("/todos/upload-fileresulit/")
    fun uploadAudio(@Part file :MultipartBody.Part?): Call<GeminiResponse?>?
}