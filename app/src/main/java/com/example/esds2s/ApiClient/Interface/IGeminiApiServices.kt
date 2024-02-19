package com.example.esds2s.ApiClient.Interface

import com.example.esds2s.Models.RequestModels.GeminiRequest
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
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


//    @Multipart
//    @POST("/todos/upload-file/")
//    fun uploadAudio(@Body body: MultipartBody.Part?): Call<GeminiResponse>
//    @Multipart
//    @POST("/todos/upload-file/")
//    fun uploadAudio(@Part filePart:MultipartBody.Part ): Call<GeminiResponse>
//    @FormUrlEncoded
//    @POST("user/edit")
//    fun generateText( @Field("first_name")_content: String? ): Call<GeminiResponse?>?

    @Multipart
    @POST("upload")
    fun upload(@Part file: Part?): Call<ResponseBody?>?

    @Multipart
    @POST("/todos/upload-fileresulit/")
    fun uploadAudio(@Part file :MultipartBody.Part?): Call<GeminiResponse?>?
}