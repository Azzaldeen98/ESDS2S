package com.example.esds2s.ApiClient.Adapter

import android.content.Context
import android.util.Log
import com.example.esds2s.ApiClient.BuildConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import java.util.concurrent.TimeUnit


class  ApiClient {


    companion object {


        private fun getHttpClientHeader(context: Context?, requestMethod: RequestMethod?,contentType:String?="application/json", token:String? = null): OkHttpClient {

            val interceptor = HttpLoggingInterceptor()
            if (!BuildConfig.DEBUG)
                interceptor.level = HttpLoggingInterceptor.Level.NONE
            else
                interceptor.level = HttpLoggingInterceptor.Level.BODY

            val httpClient = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                    // Add authorization header
                    val token = getTokenFromPreference(context)
                    if (token != null) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }
                    // Process request body
                    val requestBody = original.body
                    if (requestBody != null) {
                        val contentType = requestBody.contentType()
                        if (contentType != null) {
                            requestBuilder.method(original.method, requestBody)
                        } else {
                            // Assume JSON if content type is not set
                            val newRequestBody = RequestBody.create(
                                (contentType+"").toMediaTypeOrNull(),
                                requestBody.toString()
                            )
                            requestBuilder.method(original.method, newRequestBody)
                        }
                    }
                    // Set request method
                    if (requestMethod != null) {
                        when (requestMethod) {
                            RequestMethod.GET -> requestBuilder.get()
                            RequestMethod.POST -> requestBuilder.post(requestBody!!)
                            RequestMethod.PUT -> requestBuilder.put(requestBody!!)
                            RequestMethod.DELETE -> requestBuilder.delete(requestBody!!)
                            else -> {
                                Log.e("Request Method", "Unknown request method: $requestMethod")
                            }
                        }
                    }
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }.build() // Add other configurations as needed
//.callTimeout(100, TimeUnit.SECONDS)
            return httpClient
        }


        private fun getTokenFromPreference(context: Context?): String? {
            return null
        }

         fun getClient(context: Context?, requestMethod: RequestMethod?,BASE_URL:String): Retrofit? {
             try {
                    return try {
                            val retrofit by lazy {
                                Retrofit.Builder()
                                    .baseUrl(BASE_URL)
                                    .addConverterFactory(GsonConverterFactory.create())
                                    .build()
                            }
                     retrofit
                    } finally { }

            } catch (e: UnrecoverableKeyException) {
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: KeyStoreException) {
                e.printStackTrace()
            } catch (e: KeyManagementException) {
                e.printStackTrace()
            }
            return null
        }

         fun getClientFile(context: Context?, requestMethod: RequestMethod?,BASE_URL:String,contentType:String?="application/json"): Retrofit? {
            try {
                val httpClient = getHttpClientHeader(context,requestMethod,contentType);
                return try {
                    val retrofit by lazy {
                        Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(httpClient)
                            .build()
                    }
                    retrofit
                }
                finally { }
            } catch (e: UnrecoverableKeyException) {
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: KeyStoreException) {
                e.printStackTrace()
            } catch (e: KeyManagementException) {
                e.printStackTrace()
            }
            return null
        }
    }
}