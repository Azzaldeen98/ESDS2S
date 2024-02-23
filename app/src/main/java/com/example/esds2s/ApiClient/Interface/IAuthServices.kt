package com.example.esds2s.ApiClient.Interface

import com.example.esds2s.Models.RequestModels.GeminiRequest
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.Models.ResponseModels.RegisterResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface IAuthServices {

    @POST("/todos/api/register")
     fun RegisterUser(): Call<RegisterResponse?>?

}