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
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.internal.wait
import org.json.JSONObject
import retrofit2.Call
import java.io.File

class SpeechChatControl(private val context: Context):BaseControl(context) {

    var chatAIProvider: ChatAIProvider? = ChatAIProvider(context)

    fun messageToGeminiAndGeneratorAudio(inputText: String?, callBack: IGeminiServiceEventListener) {

        val lang = ExternalStorage.getValue(context, ContentApp.LANGUAGE) as String
        Log.d("lang", lang);
        runBlocking {
              var response: String? = null
              try {
                  var count: Int = 0
                  while (count < 3) {

                          response = async {
                            return@async  chatAIProvider?.askAndResponseAnswar(inputText!!, lang!!)
                          }.await()

                          Log.d("response_await", count.toString())
                          if (response != null && Helper.isAudioFile(response)) {
                              Log.d("onRequestIsSuccess", Gson().toJson(response))
                              callBack.onRequestIsSuccess(GeminiResponse(description = response))
                              return@runBlocking
                          }
                      count++
                      delay(1000)
                  }

              } finally {

                  if (response == null || response.isEmpty() || !Helper.isAudioFile(response)) {
                      callBack.onRequestIsFailure(response ?: "Error")
                  }
              }
          }
        }


    suspend fun getUrlAudio(text: String = "", voiceCode: String = "ar-SA-1"): String {
            val url = "https://cloudlabs-text-to-speech.p.rapidapi.com/synthesize"

            val client = OkHttpClient()

            val formBody = FormBody.Builder()
                .add("voice_code", voiceCode)
                .add("text", text)
                .add("speed", "1.00")
                .add("pitch", "1.00")
                .add("output_type", "audio_url")
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("X-RapidAPI-Key", "fc89c0c34fmsh67d77f98cbd7176p109440jsnb94ccf8b644d")
                .addHeader("X-RapidAPI-Host", "cloudlabs-text-to-speech.p.rapidapi.com")
                .build()

            return withContext(Dispatchers.IO) {
                try {
                    val response = client.newCall(request).execute()
                    val jsonResponse = JSONObject(response.body?.string())
                    Log.d("jsonResponse55", jsonResponse.toString())
                    Log.d(
                        "jsonResponse55",
                        jsonResponse.getJSONObject("result").getString("audio_url")
                    )
                    jsonResponse.getJSONObject("result").getString("audio_url")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("jsonResponse55", e.message.toString())
                    "$$$$"
                }
            }
        }

    ///=============================================================================================
    ///=============================================================================================
    fun messageToGeneratorAudio(inputText: String?, callBack: IGeminiServiceEventListener) {

        if (inputText != null) {
            val client: IChatServices by lazy {
                ApiClient.getClient(
                    context, RequestMethod.POST,
                    BuildConfig.BASE_URL
                )?.create(IChatServices::class.java)!!
            }

            var lang = ExternalStorage.getValue(context, ContentApp.LANGUAGE) as String?
            val token = ExternalStorage.getValue(context, ContentApp.CURRENT_SESSION_TOKEN)

            if (token != null) {
                val body = GeminiRequestMessage(
                    token_chat = token?.toString(),
                    description = inputText,
                    voice_code = lang!!
                )

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
//                                callBack.onRequestIsSuccess(GeminiResponse(description = "$$$$"))
//                                callBack.onRequestIsFailure("$$$$")
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
    suspend fun messageToGeneratorAudioFromFirstGemini(inputText: String?, callBack: IGeminiServiceEventListener)
            : retrofit2.Response<GeminiResponse?>? = withContext(Dispatchers.IO) {

        if (inputText != null) {
            try {
                val client: IChatServices by lazy {
                    ApiClient.getClient(context, RequestMethod.POST, BuildConfig.BASE_URL)
                        ?.create(IChatServices::class.java)!!
                }
                var lang = ExternalStorage.getValue(context, ContentApp.LANGUAGE) as String?
                val token = ExternalStorage.getValue(context, ContentApp.CURRENT_SESSION_TOKEN)
                if (token != null) {

                    val body = GeminiRequestMessage(
                        token_chat = token?.toString(),
                        description = inputText,
                        voice_code = lang!!
                    )

                    Log.d("rquestData", Gson().toJson(body));
                    val call: Call<GeminiResponse?> by lazy { client?.messageToGenerator(body)!! }

                    val response = call.execute()
                    return@withContext response
                }
            } catch (e: Exception) {
                return@withContext null
            }
        }

        return@withContext null
    }
    suspend fun messageToGeneratorAudio3(inputText: String?, callBack: IGeminiServiceEventListener) {

        var lang = ExternalStorage.getValue(context, ContentApp.LANGUAGE) as String?
        chatAIProvider?.askAndResponseAnswar(inputText, lang!!, callBack)

    }
    //----------------------------------------------------------------------------------------------
    fun messageToGeminiGeneratorAudio(inputText: String?, callBack: IGeminiServiceEventListener) {

        Log.d("inputText", inputText!!);

        val lang = ExternalStorage.getValue(context, ContentApp.LANGUAGE) as String
        var response: GeminiResponse? = null
        var error: String? = null

        Log.d("lang", lang);

        var jopRequest = CoroutineScope(Dispatchers.IO).async {
            var result: retrofit2.Response<GeminiResponse?>? = null
            var count: Int = 0

            while (count < 3) {
                Log.d("count", count.toString());
                try {
                    result = messageToGeneratorAudioFromFirstGemini(inputText, callBack)
                    // chatAIProvider?.askAndResponseAnswar(inputText!!, lang!!)
                    if (result != null) {
                        if (result?.isSuccessful!!) {
                            response = result?.body()
                            Log.d("GeminiResponse", Gson().toJson(response))
                            if (response != null && Helper.isAudioFile(response?.description!!.toString())) {
                                error = null
                                break
                            }
                        } else
                            error = result?.message();
                    } else
                        error = "Error !!";

                }catch (e:Exception){ break }
                finally { count++ }
//                delay(500)
            }


        }


//            withContext(Dispatchers.Main) {
//                if (response != null && response?.description != null)
//                    callBack.onRequestIsSuccess(GeminiResponse(description = response?.description.toString()!!))
//    //                if (result!=null && response != null && !Helper.isAudioFile(response?.description.toString()!!)) {
//    //
//    //                    callBack.onRequestIsFailure(result?.message()!!)
//    //                }
//                else {
//                    error=if (result == null) "Error" else result?.message()!!
//                }

//                }
//            }

//         }

        jopRequest?.invokeOnCompletion {

            if (response != null && Helper.isAudioFile(response?.description!!.toString())) {

                callBack.onRequestIsSuccess(response!!)
            } else {
                callBack.onRequestIsFailure(if (error == null) "Error" else error!!)
            }


        }

    }
    fun messageToGeneratorAudio2(inputText: String?, callBack: IGeminiServiceEventListener) {


        try {

            val client = OkHttpClient()
            val mediaType = "application/x-www-form-urlencoded".toMediaTypeOrNull()
            val body = RequestBody.create(
                mediaType,
                "voice_code=en-US-1&text=$inputText&speed=1.00&pitch=1.00&output_type=audio_url"
            )
            val request = Request.Builder()
                .url("https://cloudlabs-text-to-speech.p.rapidapi.com/synthesize")
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader(
                    "X-RapidAPI-Key",
                    "fc89c0c34fmsh67d77f98cbd7176p109440jsnb94ccf8b644d"
                )
                .addHeader("X-RapidAPI-Host", "cloudlabs-text-to-speech.p.rapidapi.com")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful)
                callBack.onRequestIsSuccess(GeminiResponse(description = response.body.toString()))

        } catch (e: Exception) {
            Log.e("rapidapi", e.message.toString())
        }
    }
    suspend fun getAudio(inputText: String?, callBack: IGeminiServiceEventListener) {

        var lang = ExternalStorage.getValue(context, ContentApp.LANGUAGE) as String?
        Log.d("messageToGeneratorAudio3", lang.toString())
        val response = getUrlAudio(inputText!!, lang!!)
        Log.d("response55", response!!)
        if (response != null && Helper.isAudioFile(response))
            callBack.onRequestIsSuccess(GeminiResponse(description = response))
        else
            callBack.onRequestIsFailure(response)
    }
    suspend fun messageToGeneratorAudio4(inputText: String?, callBack: IGeminiServiceEventListener) {

        var lang = ExternalStorage.getValue(context, ContentApp.LANGUAGE) as String?
        chatAIProvider?.askAndResponseAnswar(inputText, lang!!, callBack)

            //        if(inputText!=null) {
            //            val client: IChatServices by lazy {
            //                ApiClient.getClient(context, RequestMethod.POST,
            //                    BuildConfig.BASE_URL)
            //                    ?.create(IChatServices::class.java)!! }
            //
            //            var lang= ExternalStorage.getValue(context,ContentApp.LANGUAGE) as String?
            //            val token=ExternalStorage.getValue(context,ContentApp.CURRENT_SESSION_TOKEN)
            //
            //            if(token!=null) {
            //                val body = GeminiRequestMessage(
            //                    token_chat = token?.toString(),
            //                    description = inputText,
            //                    voice_code = lang!!)
            //
            //                Log.d("rquestData", Gson().toJson(body));
            //                val call: Call<GeminiResponse?> by lazy { client?.messageToGenerator(body)!! }
            //                call?.enqueue(object : retrofit2.Callback<GeminiResponse?> {
            //                    override fun onResponse(
            //                        call: Call<GeminiResponse?>,
            //                        response: retrofit2.Response<GeminiResponse?>
            //                    ) {
            //                        if (response!!.isSuccessful) {
            //                            val responseData: GeminiResponse? = response.body()
            //                            Log.d("response", Gson().toJson(responseData));
            //                            if (callBack != null) {
            //
            //                               runBlocking {
            //                                    getAudio(responseData?.description, callBack)
            //                                }
            ////                                    callBack.onRequestIsSuccess(responseData!!)
            //
            ////                                callBack.onRequestIsSuccess(GeminiResponse(description = "$$$$"))
            ////                                callBack.onRequestIsFailure("$$$$")
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
            //
            //                    }
            //                })
            //            }
            //
            //        } else {
            //            throw java.lang.Exception("message is empty !!")
            //        }
    }

}
