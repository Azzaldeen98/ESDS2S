package com.example.esds2s.ApiClient.Controlls

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.esds2s.ApiClient.Adapter.ApiClient
import com.example.esds2s.ApiClient.Adapter.RequestMethod
import com.example.esds2s.ApiClient.BuildConfig
import com.example.esds2s.ApiClient.Interface.IChatServices
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Interface.IGeminiServiceEventListener
import com.example.esds2s.Interface.IWasmServiceEventListener
import com.example.esds2s.Models.RequestModels.GeminiRequestMessage
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.Models.ResponseModels.WasmAudioResponse
import com.example.esds2s.Models.ResponseModels.WasmSplitorResponse
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import retrofit2.Call
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset


// وظيفة لاستخراج EVENT_ID من الرد الأول
private fun extractEventId(responseString: String?): String? {
    responseString?.let {
        val jsonObject = JSONObject(it)
        return jsonObject.getString("event_id")
    }
    return null
}

class SpeechChatControl2(private val context: Context):BaseControl(context) {



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
    fun query(payload: String): ByteArray? {
        val apiUrl = "https://api-inference.huggingface.co/models/asg2024/vits-ar-sa"
        val apiKey = "Bearer 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"

        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", apiKey)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val outputStream = connection.outputStream
        val inputPayload = "{\"inputs\": \"$payload\"}"
        outputStream.write(inputPayload.toByteArray(Charset.defaultCharset()))
        outputStream.flush()
        outputStream.close()

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            return connection.inputStream.readBytes()
        } else {
            println("Request failed with response code: ${connection.responseCode}")
            return null
        }
    }
    private fun playAudio(audioBytes: ByteArray) {
        try {
            val tempFile = File.createTempFile("response_temp_audio", ".wav", context.cacheDir)
            tempFile.deleteOnExit()
            val fos = FileOutputStream(tempFile)
            fos.write(audioBytes)
            fos.close()

            println("Audio saved as output.wav")
            println("Audio is playing from ${tempFile.absolutePath}")

            val mediaPlayer = MediaPlayer()

//            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.setOnPreparedListener {it->
                it.start()
            }
            mediaPlayer.setOnCompletionListener {
                mediaPlayer.release()
                tempFile.delete()
            }

            try {
                mediaPlayer.setDataSource(tempFile.absolutePath)
                mediaPlayer.prepareAsync() // Prepare asynchronously to not block the main thread
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("MediaPlayer", "IOException: ${e.message}")
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                Log.e("MediaPlayer", "IllegalArgumentException: ${e.message}")
            } catch (e: SecurityException) {
                e.printStackTrace()
                Log.e("MediaPlayer", "SecurityException: ${e.message}")
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                Log.e("MediaPlayer", "IllegalStateException: ${e.message}")
            }

        } catch (e: IOException) {

            e.printStackTrace()
        }
    }
    fun fetchAudioBytes(_url:String): ByteArray? {
        try {

            val url = URL(_url) //"https://asgmodel.pythonanywhere.com/api/vits/$text")
            return with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
//                connectTimeout = 5000
//                readTimeout = 5000
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    println("ResponseCode : $responseCode")
                    var data=inputStream.readBytes()
                    println("ByteArray : ${data.size}")
                    return data
                } else {
                    println("Error: ${responseCode}")
                    null // إرجاع null في حالة الخطأ
                }
            }
        }catch (e:IOException){
            println("fetchAudioBytes Exception :")
            e.printStackTrace()
            throw  IOException(e);
        }

    }
    fun getAudioFromApiServer(inputText:String,callBack: IGeminiServiceEventListener){
        Log.e("getAudioFromApiServer:", inputText);
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val audioBytes = withContext(Dispatchers.IO) {
//                    fetchAudioBytes(inputText);
                    var byteArray: ByteArray? = fetchAudioBytes("https://asgmodel.pythonanywhere.com/api/vits/$inputText");
//                    var count = 0
//                    while (byteArray == null && count < 3) {
//                        Log.e("ResendRequestCount:",count.toString())
//                        byteArray = fetchAudioBytes(inputText)
//                        if (byteArray != null) {
//                            return@withContext byteArray
//                        }
//                        count++
//                    }
                    return@withContext  byteArray;
                }

//            val audioBytes = fetchAudioBytes(inputText);
                if (audioBytes != null) {

                    val tempFile = File.createTempFile("response_temp_audio", "mp3", context.cacheDir)
                    tempFile.deleteOnExit()
                    val fos = FileOutputStream(tempFile)
                    fos.write(audioBytes)
                    fos.close()

                    println("Audio saved as output.wav")
                    println("Audio is playing from ${tempFile.absolutePath}")

                    callBack.onRequestIsSuccess(GeminiResponse(description = tempFile.absolutePath));

                }
                else {
                    throw IOException("Failed to retrieve audio bytes is null");
                }
            } catch (e: IOException) {

                callBack.onRequestIsFailure(e.message.toString());
                println("getAudioFromApiServer Exception :")
                e.printStackTrace()
            }
        }

    }
    fun readAudioFromAi(){
//        CoroutineScope(Dispatchers.Main).launch {
//            try {
//                val audioBytes = withContext(Dispatchers.IO) { fetchAudioBytes("مرحبا ماذا تعرف عن الجمهورية اليمنية ؟") }
//                if (audioBytes != null) {
//                    playAudio(audioBytes)
//                } else {
//                    println("Failed to fetch audio bytes.")
//                }
//            } catch (e: IOException) {
//                println("getAudioFromApiServer Exception :")
//                e.printStackTrace()
//            }

//       try {
//           val audioBytes = fetchAudioBytes("مرحبا ماذا تعرف عن الجمهورية اليمنية ؟");
//           if (audioBytes != null) {
//               playAudio(audioBytes);
//           }
//       } catch (e: IOException) {
//            println("getAudioFromApiServer Exception :")
//            e.printStackTrace()
//        }
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
                    val call: Call<GeminiResponse?> by lazy {
                        client?.messageToGenerator(body)!! }

                    val response = call.execute()
                    return@withContext response
                }
            } catch (e: Exception) {
                return@withContext null
            }
        }

        return@withContext null
    }
    suspend fun messageToGeneratorAudioFromWasm(inputText: String?)
            : retrofit2.Response<WasmAudioResponse?>? = withContext(Dispatchers.IO) {

        if (inputText != null) {
            try {
                val client: IChatServices by lazy {
                    ApiClient.getClient(context, RequestMethod.GET, BuildConfig.BASE_URL)
                        ?.create(IChatServices::class.java)!!
                }

                val call  by lazy { client?.getWasmAudio(inputText)!! }
                val response = call.execute()
                return@withContext response

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
    fun getStreamResponse4(inputText:String,callBack: IGeminiServiceEventListener) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
//                                                val url = "https://wasmdashai-wasm-splitor.hf.space/call/predict/$event_id"
                val url = "https://wasmdashtag.pythonanywhere.com/todos/api/$inputText"
                val audioBytes = withContext(Dispatchers.IO) {
                    var res=  fetchAudioBytes(url);
                    return@withContext  res
                }

                if (audioBytes != null) {

                    val tempFile = File.createTempFile("response_temp_audio", ".mp3", context.cacheDir)
//                                                    tempFile.deleteOnExit()
                    val fos = FileOutputStream(tempFile)
                    fos.write(audioBytes)
                    fos.close()

                    println("Audio saved as output.wav")
                    println("Audio is playing from ${tempFile.absolutePath}")

                    callBack.onRequestIsSuccess(GeminiResponse(description = tempFile.absolutePath));

                }
                else {
                    throw IOException("Failed to retrieve audio bytes is null");
                }
            } catch (e: IOException) {

                callBack.onRequestIsFailure(e.message.toString());
                println("getAudioFromApiServer Exception :")
                e.printStackTrace()
            }
        }
    }
    fun readBytesFromResponseBody(responseBody: ResponseBody) {
        val buffer = ByteArray(4096) // حجم المصفوفة المؤقتة
        val inputStream: InputStream = BufferedInputStream(responseBody.byteStream())
        var bytesRead: Int
        val outputStream = ByteArrayOutputStream()

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            val byteArray = outputStream.toByteArray()

            // التعامل مع البيانات
            println("Received bytes: ${byteArray.size}")

        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream.close()
            outputStream.close()
        }
    }
    fun getStreamResponse2(inputText:String,callBack: IGeminiServiceEventListener) {

//                if(inputText!=null)

                    val client: IChatServices by lazy {
                        ApiClient.getClient(context, RequestMethod.POST,
                            BuildConfig.BASE_URL)
                            ?.create(IChatServices::class.java)!! }

                        val call = client?.wasmSplitor(RequestData(listOf(inputText)))!!;
                        call?.enqueue(object : retrofit2.Callback<WasmSplitorResponse?> {
                            override fun onResponse(
                                call: Call<WasmSplitorResponse?>,
                                response: retrofit2.Response<WasmSplitorResponse?>
                            ) {
                                if (response!!.isSuccessful) {
                                    val responseData: WasmSplitorResponse? = response.body()
                                    Log.d("response", Gson().toJson(responseData));
                                    if (callBack != null) {


                                        var event_id: String = responseData?.event_id.toString();
                                        Log.d("event_id", event_id);


                                        val client2: IChatServices = ApiClient.getClient(
                                            context,
                                            RequestMethod.GET,
                                            BuildConfig.BASE_URL
                                        )?.create(IChatServices::class.java)!!

                                        val call2 = client2?.getWasmSplitor(event_id)!!;

                                        Log.d("getWasSplitorURL", call2.request().url.toString());
                                        call2?.enqueue(object : retrofit2.Callback<ResponseBody?> {

                                            override fun onResponse(call: Call<ResponseBody?>, response: retrofit2.Response<ResponseBody?>) {
                                                if (response!!.isSuccessful) {
                                                    val responseData: ResponseBody? = response!!.body()
                                                    Log.d("response2",response!!.body()!!.string());

                                                    readBytesFromResponseBody(responseData!!);
                                                    if (callBack != null) {


                                                    } else
                                                        if (callBack != null)
                                                            callBack.onRequestIsFailure(response?.message()!!)
                                                }
                                            }

                                            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                                                // التعامل مع الأخطاء هنا
                                                if (callBack != null)
                                                    callBack?.onRequestIsFailure(t.message!!)

                                            }
                                        })
//                                      callBack.onRequestIsSuccess(responseData!!)
//                                      callBack.onRequestIsFailure("$$$$")
                                  }
                                    } else
                                        if (callBack != null)
                                            callBack.onRequestIsFailure(response?.message()!!)
                                }

                            override fun onFailure(call: Call<WasmSplitorResponse?>, t: Throwable) {
                                // التعامل مع الأخطاء هنا
                                if (callBack != null)
                                    callBack?.onRequestIsFailure(t.message!!)

                            }
                        })


    }
    fun getAudioFromApiServer2(inputText:String,callBack: IGeminiServiceEventListener){
            Log.e("getAudioFromApiServer:", inputText);

//            val url = "https://wasmdashtag.pythonanywhere.com/todos/api/$inputText"

            val client: IChatServices = ApiClient.getClient(context, RequestMethod.GET,
                "https://wasmdashtag.pythonanywhere.com")
                    ?.create(IChatServices::class.java)!!

            val call = client?.getWasmAudio(inputText)!!;
//            call?.enqueue(object : retrofit2.Callback<ResponseBody?> {
//                override fun onResponse(call: Call<ResponseBody?>, response: retrofit2.Response<ResponseBody?>) {
//                    if (response!!.isSuccessful) {
//                        val responseData: ResponseBody? = response.body()
//                        Log.d("response77", Gson().toJson(responseData));
//                        if (callBack != null) {
//
//
////                            CoroutineScope(Dispatchers.Main).launch {
////                                try {
//////                                                val url = "https://wasmdashai-wasm-splitor.hf.space/call/predict/$event_id"
////                                    val url = "https://wasmdashtag.pythonanywhere.com/todos/api/$inputText"
////                                    val audioBytes = withContext(Dispatchers.IO) {
////                                        var res=  fetchAudioBytes(url);
////                                        return@withContext  res
////                                    }
////
////                                    if (audioBytes != null) {
////
////                                        val tempFile = File.createTempFile("response_temp_audio", ".mp3", context.cacheDir)
//////                                                    tempFile.deleteOnExit()
////                                        val fos = FileOutputStream(tempFile)
////                                        fos.write(audioBytes)
////                                        fos.close()
////
////                                        println("Audio saved as output.wav")
////                                        println("Audio is playing from ${tempFile.absolutePath}")
////
////                                        callBack.onRequestIsSuccess(GeminiResponse(description = tempFile.absolutePath));
////
////                                    }
////                                    else {
////                                        throw IOException("Failed to retrieve audio bytes is null");
////                                    }
////                                } catch (e: IOException) {
////
////                                    callBack.onRequestIsFailure(e.message.toString());
////                                    println("getAudioFromApiServer Exception :")
////                                    e.printStackTrace()
////                                }
////                            }
//
//                        }
//                    } else
//                        if (callBack != null)
//                            callBack.onRequestIsFailure(response?.message()!!)
//                }
//
//                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
//                    // التعامل مع الأخطاء هنا
//                    if (callBack != null)
//                        callBack?.onRequestIsFailure(t.message!!)
//
//                }
//            })
    }
    fun getStreamResponse3(inputText:String,callBack: IGeminiServiceEventListener){


            val url = "https://wasmdashai-wasm-splitor.hf.space/call/predict"
//            val data = JSONObject().put("data", listOf("Hello!!"))

            val client = OkHttpClient()

            val predictRequest = Request.Builder()
                .url(url)
                .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(),
                    Gson().toJson(RequestData(listOf(inputText)))))
                .build()

            val predictResponse = client.newCall(predictRequest).execute()

            if (predictResponse.isSuccessful) {
                val responseBodyString = predictResponse.body?.string() ?: ""
//                val responseJson = JSONObject(responseBodyString) // Assuming JSON response

                // Parse the response into a WasmSplitorResponse object
                val wasmSplitorResponse = Gson().fromJson(responseBodyString, WasmSplitorResponse::class.java)
                val eventId = wasmSplitorResponse.event_id

                Log.d("eventId3", eventId);
                CoroutineScope(Dispatchers.Main).launch {
                    try {

                        val audioBytes = withContext(Dispatchers.IO) {
                            var res=  fetchAudioBytes("$url/$eventId");
                            return@withContext  res
                        }

                        if (audioBytes != null) {

                            val tempFile = File.createTempFile("response_temp_audio", "mp3", context.cacheDir)
                            tempFile.deleteOnExit()
                            val fos = FileOutputStream(tempFile)
                            fos.write(audioBytes)
                            fos.close()

                            println("Audio saved as output.wav")
                            println("Audio is playing from ${tempFile.absolutePath}")

                            callBack.onRequestIsSuccess(GeminiResponse(description = tempFile.absolutePath));

                        }
                        else {
                            throw IOException("Failed to retrieve audio bytes is null");
                        }
                    } catch (e: IOException) {

                        callBack.onRequestIsFailure(e.message.toString());
                        println("getAudioFromApiServer Exception :")
                        e.printStackTrace()
                    }
                }

//                if (eventId != null) {
//                    val secondRequest = Request.Builder()
//                        .url("$url/$eventId")
//                        .build()
//
//                    val secondResponse = client.newCall(secondRequest).execute()
//
//                    if (secondResponse.isSuccessful) {
//                        // Handle successful second response here
//                        val finalResponse = secondResponse.body?.string()
//                        println(finalResponse)
//                    } else {
//                        println("Second request failed: ${secondResponse.code}")
//                    }
//                } else {
//                    println("Failed to parse event ID from first response")
//                }
            } else {
                println("First request failed: ${predictResponse.code}")
            }

            predictResponse.close()

        }
    fun getStreamWasmResponse(inputText:String,callBack: IWasmServiceEventListener) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
//                val audioBytes = withContext(Dispatchers.IO) {
//                   var response = messageToGeneratorAudioFromWasm(inputText);
//                    if(response!=null && response!!.body()!=null){
//                        var res = fetchAudioBytes(response.body()!!.url);
//                        return@withContext res
//                    } else {
//                        return@withContext null
//                    }
//                }
                val response = withContext(Dispatchers.IO) {
                    return@withContext messageToGeneratorAudioFromWasm(inputText);
                }
                if(response!=null && response!!.body()!=null) {
                    Log.d("response_url:", response?.body()!!.url);
                    val player = ExoPlayer.Builder(context).build()
                    val mediaItem = MediaItem.fromUri(response?.body()!!.url)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
//                if (audioBytes != null)
//                {
//                    Log.d("audioBytes", "audioBytes");
//                    // Build the media item.
//
//
////                    playAudio(audioBytes)
////                    val tempFile = File.createTempFile("response_temp_audio", ".wav", context.cacheDir)
////                    tempFile.deleteOnExit()
//////                    if(saveInputStreamToFile(audioBytes.inputStream(),tempFile)){
//////                        callBack?.onRequestIsSuccess(tempFile.absolutePath)
//////                    }
////                    val fos = FileOutputStream(tempFile)
////                    fos.write(audioBytes)
////                    fos.close()
////                    Log.d("voiceURL", tempFile.path);
////                    var response =
////                        Gson().fromJson(responseData.body()?.string(), JsonObject::class.java)
////                    var url = response["url"].toString()
////                    var voiceURL = "$url.mp3"
////                    Log.d("voiceURL", voiceURL);
////                    callBack?.onRequestIsSuccess(tempFile.path)
//                } else {
//                    throw IOException("Failed to retrieve audio bytes is null");
//                }
            } catch (e: IOException) {
                callBack.onRequestIsFailure(e.message.toString());
                e.printStackTrace()
            }
        }
    }
    fun getStreamResponse(inputText:String,callBack: IWasmServiceEventListener) {

        val client:IChatServices = ApiClient.getClient(context, RequestMethod.GET,
            BuildConfig.BASE_URL)?.create(IChatServices::class.java)!!

        val call = client?.getWasmAudio(inputText)!!;
        Log.d("getWasmAudioURL", call.request().url.toString());
        call?.enqueue(object : retrofit2.Callback<WasmAudioResponse?> {
            override fun onResponse(call: Call<WasmAudioResponse?>, response: retrofit2.Response<WasmAudioResponse?>) {
                if (response!!.isSuccessful) {
                    val responseData: WasmAudioResponse? = response!!.body()
                    if(responseData!=null){
                        Log.d("getResponseWasmAudioURL", responseData.url);
                        callBack?.onRequestIsSuccess(responseData.url)

//                        if (callBack != null) {
//                            val tempFile = File.createTempFile("response_temp_audio", ".mp3", context.cacheDir)
//                            tempFile.deleteOnExit()
//                            if(saveInputStreamToFile(responseData.byteStream(),tempFile)){
//                                callBack?.onRequestIsSuccess(responseData.url)
//                            }
//                                  Log.d("responseURL",tempFile.absolutePath);
//                            } else
//                                callBack.onRequestIsFailure(response?.message()!!)
                        }
                    else
                        callBack.onRequestIsFailure(response?.message()!!)
                }
            }
            override fun onFailure(call: Call<WasmAudioResponse?>, t: Throwable) {
                if (callBack != null)
                    callBack?.onRequestIsFailure(t.message!!)
            }
        })

//        val client = OkHttpClient()
//
//        // إنشاء جسم الطلب
//        val json = JSONObject()
//        json.put("data", listOf("Hello!!"))
//        val mediaType = "application/json".toMediaTypeOrNull()
//        val body = RequestBody.create(mediaType, json.toString())
////        curl -X POST https://wasmdashai-wasm-splitor.hf.space/call/predict -s -H "Content-Type: application/json" -d '
////        {
////        "data": [
////        "Hello!!"
////        ]}' \
////    | awk -F'"' '{ print $4}'  \
////    | read EVENT_ID; curl -N https://wasmdashai-wasm-splitor.hf.space/call/predict/$EVENT_ID
//        // إعداد طلب POST
//        val postRequest = Request.Builder()
//            .url("https://wasmdashai-wasm-splitor.hf.space/call/predict")
//            .post(body)
//            .build()
//    client.newCall(postRequest).enqueue(object : Callback {
//
//
//        override fun onFailure(call: okhttp3.Call, e: IOException) {
//            e.printStackTrace()
//        }
//
//        override fun onResponse(call: okhttp3.Call, response: Response) {
//            if (response.isSuccessful) {
//                // استجابة ناجحة
//                val responseBody = response.body!!.string()
//                val eventId = extractEventId(responseBody)
//                println(responseBody)
//
//                val getRequest = Request.Builder()
//                    .url("https://wasmdashai-wasm-splitor.hf.space/call/predict/$eventId")
//                    .build()
//
//                client.newCall(getRequest).enqueue(object : Callback {
//
//                    override fun onFailure(call: okhttp3.Call, e: IOException) {
//                        e.printStackTrace()
//                    }
//
//                    override fun onResponse(call: okhttp3.Call, response: Response) {
//                        if (!response.isSuccessful) {
//                            // استجابة ناجحة
//                            val responseBody = response.body!!.string()
//
//                            println(responseBody)
//                        } else {
//                            // استجابة غير ناجحة
//                            System.err.println("Response not successful: " + response)
//                        }
//                    }
//                })
//
//            } else {
//                // استجابة غير ناجحة
//                System.err.println("Response not successful: " + response.code())
//            }
//        }
//    })
        // تنفيذ طلب POST
//        client.newCall(postRequest).enqueue(object : Callback {
//            override fun onFailure(call: okhttp3.Call, e: IOException) {
//                e.printStackTrace()
//            }

//            override fun onResponse(call: okhttp3.Call, response: Response) {
//                response.use {
//                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
//
//                    // قراءة الـ EVENT_ID من الرد
//                    val responseString = response.body?.string()
//                    val eventId = extractEventId(responseString)
//                    println("EVENT_ID: $eventId")
//
//                    // إعداد طلب GET باستخدام EVENT_ID
//                    val getRequest = Request.Builder()
//                        .url("https://wasmdashai-wasm-splitor.hf.space/call/predict/$eventId")
//                        .build()
//
//                    // تنفيذ طلب GET
//                    client.newCall(getRequest).enqueue(object : okhttp3.Callback {
//                        override fun onFailure(call:  okhttp3.Call, e: IOException) {
//                            callBack.onRequestIsFailure(e.message!!)
//                            e.printStackTrace()
//                        }
//
//                        override fun onResponse(call: okhttp3.Call, response: Response) {
//                            response.use {
//                                if (!response.isSuccessful)
//                                    throw IOException("Unexpected code $response")
//
//                                // قراءة الرد النهائي
//                                val finalResponse = response.body?.string()
//                                println("Final Response: $finalResponse")
//                                callBack.onRequestIsSuccess(GeminiResponse(finalResponse!!))
//                                println("Final Response: $finalResponse")
//                            }
//                        }
//                    })
//                }
//            }
//            }
//        })
    }
    fun getStreamWasmResponse2(inputText:String,callBack: IWasmServiceEventListener) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    return@withContext messageToGeneratorAudioFromWasm(inputText);
                }
                if(response!=null && response!!.body()!=null) {
                    Log.d("response_url:", response?.body()!!.url);
                    callBack.onRequestIsSuccess(response.body()!!.url)
//                    val player = ExoPlayer.Builder(context).build()
//                    val mediaItem = MediaItem.fromUri(response?.body()!!.url)
//                    player.setMediaItem(mediaItem)
//                    player.prepare()
//                    player.play()
                }

            } catch (e: IOException) {
                callBack.onRequestIsFailure(e.message.toString());
                e.printStackTrace()
            }
        }
    }
    private fun downloadAndPlayAudio(url: String,callBack: IWasmServiceEventListener) {
        CoroutineScope(Dispatchers.IO).launch {
             val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val inputStream = response.body?.byteStream()

            if (inputStream != null) {
                val tempFile = createTempFile("audio", ".wav")
//                val tempFile = File.createTempFile("response_temp_audio", ".wav", context.cacheDir)
//                tempFile.deleteOnExit()
                if(saveInputStreamToFile(inputStream,tempFile)){
                    callBack?.onRequestIsSuccess(tempFile.absolutePath)
                }else{
                    callBack?.onRequestIsFailure("not saved file")
                }

            }
        }
    }
    private fun saveInputStreamToFile(inputStream: InputStream, file: File): Boolean {
        return try {
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.close()
            inputStream.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
    }
