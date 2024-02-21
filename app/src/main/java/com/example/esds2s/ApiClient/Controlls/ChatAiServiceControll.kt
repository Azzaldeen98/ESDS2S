package com.example.esds2s.ApiClient.Controlls

import android.content.Context
import android.media.MediaPlayer.OnCompletionListener
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.example.esds2s.ApiClient.Adapter.ApiClient
import com.example.esds2s.ApiClient.Adapter.RequestMethod
import com.example.esds2s.ApiClient.BuildConfig
import com.example.esds2s.ApiClient.Interface.IGeminiApiServices
import com.example.esds2s.Interface.IUplaodAudioEventListener
import com.example.esds2s.Models.RequestModels.GeminiRequest
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import java.io.File

class ChatAiServiceControll(private val context: FragmentActivity?) {


//    @UnsupportedAppUsage
    private val mOnCompletionListener: OnCompletionListener? = null
    fun  audioToGeneratorAudio(inputText:String): Call<GeminiResponse?>? {

        if(inputText!=null) {

            val client : IGeminiApiServices  by lazy{
                ApiClient.getClient(context, RequestMethod.POST,
                BuildConfig.GEMINI_BASE_URL_AUDIO_TO_GENERATOR)
                ?.create(IGeminiApiServices::class.java)!!}

            val body = GeminiRequest(_content = inputText)
            Log.d("generateAiAudio",inputText);

            return try{
                val call by lazy { client?.audioToGenerator(body)!! }
                call
            }catch (e:java.lang.Exception) {
                throw Exception(e.message)
            }
        }
        else {
            throw java.lang.Exception("inputText is empty !!")
        }
    }
    fun  textToGeneratorAudio(inputText:String?,callBack: IUplaodAudioEventListener) {

        Log.d("inputText", inputText!!);
        if(inputText!=null) {

            val client: IGeminiApiServices by lazy {
                ApiClient.getClient(context, RequestMethod.POST,
                    BuildConfig.GEMINI_BASE_URL_TEXT_TO_GENERATOR)
                    ?.create(IGeminiApiServices::class.java)!! }
            Log.d("inputText2", inputText!!);
            val body = GeminiRequest(_content = inputText!!)
            val call: Call<GeminiResponse?> by lazy { client?.textToGenerator(body)!! }
                call?.enqueue(object : retrofit2.Callback<GeminiResponse?> {
                    override fun onResponse(
                        call: Call<GeminiResponse?>,
                        response: retrofit2.Response<GeminiResponse?>) {
                        if (response!!.isSuccessful) {
                            val responseData: GeminiResponse? = response.body()
                            Log.d("response", Gson().toJson(responseData));
                            if (callBack != null) {
                                callBack.onRequestIsSuccess(responseData!!)
                            }
                        }
                        else
                            if (callBack != null)
                                callBack.onRequestIsFailure(response?.message()!!)
                    }

                    override fun onFailure(call: Call<GeminiResponse?>, t: Throwable) {
                        // التعامل مع الأخطاء هنا
                        if (callBack != null)
                            callBack?.onRequestIsFailure(t.message!!)
                    } })
        } else {
            throw java.lang.Exception("inputText is empty !!")
        }
    }
    fun  uploadAudioFile(file_path:String,context: Context?,callBack: IUplaodAudioEventListener) {

//        mOnCompletionListener.o
        Log.e("file_path", file_path);
        val file = File(file_path)
        if(!file!!.exists()) {
            Log.e("read file", "file not exists !!");
            return;
        }
        val requestBody = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("audio/*".toMediaTypeOrNull()))

        val client : IGeminiApiServices?  by lazy{
            ApiClient.getClientFile(context,RequestMethod.POST,
                BuildConfig.GEMINI_BASE_URL,"multipart/form-data")
                ?.create(IGeminiApiServices::class.java)!! }

        val call : Call<GeminiResponse?>  by lazy {
            client?.uploadFileAudio(requestBody)!!
        }

        call?.enqueue(object :retrofit2.Callback<GeminiResponse?> {
            override fun onResponse(
                call: Call<GeminiResponse?>,
                response: retrofit2.Response<GeminiResponse?>
            ) {
                // التعامل مع الاستجابة هنا
                if (response!!.isSuccessful) {
                    val responseData: GeminiResponse? = response.body()
                   Log.d("response file",Gson().toJson(responseData));
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

}