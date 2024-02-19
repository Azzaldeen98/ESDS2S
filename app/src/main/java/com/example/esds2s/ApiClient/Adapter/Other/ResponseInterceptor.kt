package com.example.esds2s.ApiClient.Adapter.Other

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class ResponseInterceptor(context: Context) : Interceptor {
    //    private val context: Context
//
//    @Throws(IOException::class)
//    override fun intercept(chain: Interceptor.Chain): Response {
//        val response: Response = chain.proceed(chain.request())
//        if (response.code == 401 || !response.request.url.toUri().toString()
//                .endsWith("ValidateOtp")
//        ) {
//            endSession(context)
//        } else if (response.code == 403) {
//            // reActivate(context);
//        }
//        val contentType: MediaType?
//        val body: ResponseBody
//        contentType = response.body!!.contentType()
//        body = create.create(contentType, response.body!!.string())
//        //        body = ResponseBody.create(contentType, response.body().string());
//        return response.newBuilder().body(body).build()
//    }
//
//    internal inner class RequestMethod
//
//    var APIHelper: `object`? = null
//
//    init {
//        var retrofit: `val`
//        var by: Retrofit
//        lazy
//        run { createRetrofitInstance() }
//        var createRetrofitInstance: `fun`
//        ()
//        Retrofit
//        run {
//            val loggingInterceptor: `val` = HttpLoggingInterceptor()
//            loggingInterceptor.level =
//                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
//            val httpClient: `val` = OkHttpClient.Builder()
//                .addInterceptor(loggingInterceptor)
//                .addInterceptor(APIInterceptor(GET)) // Example of using an interceptor
//                .build()
//            return Retrofit.Builder()
//                .baseUrl("https://api.example.com/")
//                .client(httpClient)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build()
//        }
//        var getClient: `fun`
//        ()
//        Retrofit
//        run { return retrofit }
//    }
//
//    init {
//        this.context = context
//    }
//
//    internal inner class APIInterceptor
//
//    fun Interceptor() {
//        var `fun`: override
//        intercept(chain)
//        Chain
//        Response
//        run {
//            val originalRequest: `val` = chain.request()
//            val modifiedRequest: `val` = originalRequest.newBuilder()
//
//            // Example of modifying request based on request method
//            `when`(requestMethod)
//            run {
//                POST
//                modifiedRequest.post(RequestBody.create(parse.parse("application/json"), "{}"))
//                PUT
//                modifiedRequest.put(RequestBody.create(parse.parse("application/json"), "{}"))
//                DELETE
//                modifiedRequest.delete(RequestBody.create(parse.parse("application/json"), "{}"))
//                modifiedRequest.get()
//            }
//            return chain.proceed(modifiedRequest.build())
//        }
//    }
//
//    // Usage
//    //    fun main() {
//    //        val apiService = APIHelper.getClient().create(ApiService::class.java)
//    //
//    //        // Example of using Retrofit interface method
//    //        val call = apiService.getUser(123)
//    //        call.enqueue(object : Callback<UserResponse> {
//    //            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
//    //                if (response.isSuccessful) {
//    //                    val userResponse = response.body()
//    //                    println("User details: $userResponse")
//    //                } else {
//    //                    println("Failed to fetch user details: ${response.code()}")
//    //                }
//    //            }
//    //
//    //            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
//    //                println("Failed to fetch user details: ${t.message}")
//    //            }
//    //        })
//    //    }
//
//
//    internal inner class UserResponse
//    companion object {
//        fun openLogin(context: Context?) {
//            try {
////            Hawk.put("Token",null);
////            Intent intent = new Intent(context, LoginActivity.class);
////            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
////            context.startActivity(intent);
//            } catch (e: Exception) {
//            }
//        }
//
//        fun endSession(context: Context?) {
//            openLogin(context)
//        }
//    }
    override fun intercept(chain: Interceptor.Chain): Response {
        TODO("Not yet implemented")
    }
}




//object APIHelper {
//    private val retrofit: Retrofit by lazy { createRetrofitInstance() }
//
//    private fun createRetrofitInstance(): Retrofit {
//        val loggingInterceptor = HttpLoggingInterceptor()
//        loggingInterceptor.level = if (BuildConfig.DEBUG)
//            HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
//
//        val httpClient = OkHttpClient.Builder()
//            .addInterceptor(loggingInterceptor)
//            .addInterceptor(APIInterceptor(RequestMethod.GET)) // Example of using an interceptor
//            .build()
//
//        return Retrofit.Builder()
//            .baseUrl(BuildConfig.GEMINI_BASE_URL)
//            .client(httpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    fun getClient(): Retrofit {
//        return retrofit
//    }
//}

//class APIInterceptor(private val requestMethod: RequestMethod) : Interceptor {
//    override fun intercept(chain: Interceptor.Chain): Response {
//        val originalRequest = chain.request()
//        val modifiedRequest = originalRequest.newBuilder()
//
//        // Example of modifying request based on request method
//        when (requestMethod) {
//            RequestMethod.POST -> modifiedRequest.post(RequestBody.create(MediaType.parse("application/json"), "{}"))
//            RequestMethod.PUT -> modifiedRequest.put(RequestBody.create(MediaType.parse("application/json"), "{}"))
//            RequestMethod.DELETE -> modifiedRequest.delete(RequestBody.create(MediaType.parse("application/json"), "{}"))
//            else -> modifiedRequest.get()
//        }
//
//        return chain.proceed(modifiedRequest.build())
//    }
//}

//// Usage
//fun main() {
//    val apiService = APIHelper.getClient().create(IGeminiApiServices::class.java)
//
//    // Example of using Retrofit interface method
//    val call = apiService.generateAiAudio(GeminiRequest(_content = "ss"))
//    call.enqueue(Callback<GeminiResponse> {
//        override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
//            if (response.isSuccessful) {
//                val geminiResponse = response.body()
//                println("Gemini response: $geminiResponse")
//            } else {
//                println("Failed to fetch Gemini response: ${response.code()}")
//            }
//        }
////
//        override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
//            println("Failed to fetch Gemini response: ${t.message}")
//        }
//    })
//
//    })
//}



