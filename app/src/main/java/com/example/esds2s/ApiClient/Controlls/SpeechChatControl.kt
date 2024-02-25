package com.example.esds2s.ApiClient.Controlls

import android.content.Context
import android.util.Log
import com.example.esds2s.ApiClient.Adapter.ApiClient
import com.example.esds2s.ApiClient.Adapter.RequestMethod
import com.example.esds2s.ApiClient.BuildConfig
import com.example.esds2s.ApiClient.Interface.IChatServices
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Interface.IGeminiServiceEventListener
import com.example.esds2s.Interface.IUplaodAudioEventListener
import com.example.esds2s.Models.RequestModels.GeminiRequest
import com.example.esds2s.Models.RequestModels.GeminiRequestMessage
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import java.io.File

class SpeechChatControl(private val context: Context):BaseControl(context) {

    fun  messageToGeneratorAudio(inputText:String?,callBack: IGeminiServiceEventListener) {

        if(inputText!=null) {
            val client: IChatServices by lazy {
                ApiClient.getClient(context, RequestMethod.POST,
                    BuildConfig.BASE_URL)
                    ?.create(IChatServices::class.java)!! }

            var lang= ExternalStorage.getValue(context,ContentApp.LANGUAGE) as String?
            val token=ExternalStorage.getValue(context,ContentApp.CURRENT_SESSION_TOKEN)

            if(token!=null) {
                val body = GeminiRequestMessage(
                    token_chat = token?.toString(),
                    description = inputText,
                    voice_code = lang!!)

                Log.d("rquestData", Gson().toJson(body));
                val call: Call<GeminiResponse?> by lazy { client?.messageToGenerator(body)!! }
                call?.enqueue(object : retrofit2.Callback<GeminiResponse?> {
                    override fun onResponse(
                        call: Call<GeminiResponse?>,
                        response: retrofit2.Response<GeminiResponse?>
                    ) {
                        if (response!!.isSuccessful) {
                            val responseData: GeminiResponse? = response.body()
                            Log.d("response", Gson().toJson(responseData));
                            if (callBack != null) {
                                callBack.onRequestIsSuccess(responseData!!)
                            }
                        } else
                            if (callBack != null)
                                callBack.onRequestIsFailure(response?.message()!!)
                    }

                    override fun onFailure(call: Call<GeminiResponse?>, t: Throwable) {
                        // التعامل مع الأخطاء هنا
                        if (callBack != null)
                            callBack?.onRequestIsFailure(t.message!!)

                    }
                })
            }

        } else {
            throw java.lang.Exception("message is empty !!")
        }
    }



//    fun  audioToGeneratorAudio(inputText:String): Call<GeminiResponse?>? {
//
//        if(inputText!=null) {
//
//            val client : IChatServices  by lazy{
//                ApiClient.getClient(context, RequestMethod.POST,
//                BuildConfig.BASE_URL_AUDIO_TO_GENERATOR)
//                ?.create(IChatServices::class.java)!!}
//
//            val body = GeminiRequest(_content = inputText)
//            Log.d("generateAiAudio",inputText);
//
//            return try{
//                val call by lazy { client?.audioToGenerator(body) }
//                call
//            }catch (e:java.lang.Exception) {
//                throw Exception(e.message)
//            }
//        }
//        else {
//            throw java.lang.Exception("inputText is empty !!")
//        }
//    }
//    fun  textToGeneratorAudio(inputText:String?,callBack: IUplaodAudioEventListener) {
//
//        if(inputText!=null) {
//
//            val client: IChatServices by lazy {
//                ApiClient.getClient(context, RequestMethod.POST,
//                    BuildConfig.BASE_URL_TEXT_TO_GENERATOR)
//                    ?.create(IChatServices::class.java)!! }
//
//            val body = GeminiRequest(_content = inputText!!)
//
//
//                    val call: Call<GeminiResponse?> by lazy { client?.textToGenerator(body)!! }
//                    call?.enqueue(object : retrofit2.Callback<GeminiResponse?> {
//                        override fun onResponse(
//                            call: Call<GeminiResponse?>,
//                            response: retrofit2.Response<GeminiResponse?>
//                        ) {
//                            if (response!!.isSuccessful) {
//                                val responseData: GeminiResponse? = response.body()
//                                Log.d("response", Gson().toJson(responseData));
//                                if (callBack != null) {
//                                    callBack.onRequestIsSuccess(responseData!!)
//                                }
//                            } else
//                                if (callBack != null)
//                                    callBack.onRequestIsFailure(response?.message()!!)
//                        }
//
//                        override fun onFailure(call: Call<GeminiResponse?>, t: Throwable) {
//                            // التعامل مع الأخطاء هنا
//                            if (callBack != null)
//                                callBack?.onRequestIsFailure(t.message!!)
//                        }
//                    })
//
//
//
//        } else {
//            throw java.lang.Exception("inputText is empty !!")
//        }
//    }
//    fun  uploadAudioFile(file_path:String,context: Context?,callBack: IUplaodAudioEventListener) {
//        val file = File(file_path)
//        val flag=file?.exists()
//        if(flag!=null && !flag) {
//            Log.e("read file", "file not exists !!");
//            return;
//        }
//        val requestBody = MultipartBody.Part.createFormData(
//            "file",
//            file.name,
//            file.asRequestBody("audio/*".toMediaTypeOrNull()))
//
//        val client : IChatServices?  by lazy{
//            ApiClient.getClientFile(context,RequestMethod.POST,
//                BuildConfig.BASE_URL,"multipart/form-data")
//                ?.create(IChatServices::class.java)!! }
//
//
//                val call: Call<GeminiResponse?> by lazy { client?.uploadFileAudio(requestBody)!! }
//                call?.enqueue(object : retrofit2.Callback<GeminiResponse?> {
//                    override fun onResponse(
//                        call: Call<GeminiResponse?>,
//                        response: retrofit2.Response<GeminiResponse?>
//                    ) {
//                        // التعامل مع الاستجابة هنا
//                        if (response!!.isSuccessful) {
//                            val responseData: GeminiResponse? = response.body()
//                            Log.d("response file", Gson().toJson(responseData));
//                            if (callBack != null) {
//                                callBack.onRequestIsSuccess(responseData!!)
//                            }
//                        } else
//                            if (callBack != null)
//                                callBack.onRequestIsFailure(response?.message()!!)
//                    }
//
//                    override fun onFailure(call: Call<GeminiResponse?>, t: Throwable) {
//                        // التعامل مع الأخطاء هنا
//                        if (callBack != null)
//                            callBack?.onRequestIsFailure(t.message!!)
//                    }
//                })
//
//
//
//    }


}