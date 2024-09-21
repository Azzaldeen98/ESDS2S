package com.example.esds2s.ApiClient.Controlls

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.esds2s.ApiClient.Adapter.ApiClient
import com.example.esds2s.ApiClient.Adapter.GeminiApiClient
import com.example.esds2s.ApiClient.Adapter.RequestMethod
import com.example.esds2s.ApiClient.Adapter.RetrofitInstance
import com.example.esds2s.ApiClient.BuildConfig
import com.example.esds2s.ApiClient.Interface.IChatServices
import com.example.esds2s.ApiClient.Interface.ICustomPlayerListener
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Core.State.SpeechChatBotState
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.Helpers.ExoPlayerMedia
import com.example.esds2s.Helpers.StorageSpeechModels
import com.example.esds2s.Interface.IBaseCallbackListener
import com.example.esds2s.Interface.IListenerStream
import com.example.esds2s.Interface.IWasmServiceEventListener
import com.example.esds2s.Models.MeasureNanoTimeModel
import com.example.esds2s.Models.ResponseModels.WasmAudioResponse
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.type.DateTime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import org.json.JSONObject
import retrofit2.Call
import java.io.*
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resumeWithException


data class RequestData(val data: List<String>)

// 2. إنشاء Factory لـ DataSource
class FileStreamDataSourceFactory(private val file: File) : DataSource.Factory {
    @SuppressLint("UnsafeOptInUsageError")
    override fun createDataSource(): DataSource {
        return FileStreamDataSource(file)
    }
}
class FileStreamDataSource(private val file: File) : DataSource {

    private var inputStream: FileInputStream? = null
    private var position: Long = 0
    private var transferListener: TransferListener? = null
    private var dataSpec: DataSpec? = null

    @SuppressLint("UnsafeOptInUsageError")
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec=dataSpec
        inputStream = FileInputStream(file)
        inputStream!!.skip(dataSpec.position)
        position = dataSpec.position
        transferListener?.onTransferStart(this, dataSpec,false)
        return dataSpec.length
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
//        val available = inputStream!!.available()
//        if (available == 0) {
//            // لا توجد بيانات جديدة، الانتظار لبعض الوقت قبل المحاولة مرة أخرى
//            Thread.sleep(100)
//            return 0
//        }
//        val bytesRead = inputStream!!.read(buffer, offset, readLength)
//        if (bytesRead > 0) {
//            position += bytesRead
//        }
//        return bytesRead

        val available = inputStream!!.available()
        if (available == 0) {
            // لا توجد بيانات جديدة، الانتظار لبعض الوقت قبل المحاولة مرة أخرى
            Thread.sleep(100)
            return 0
        }
        val bytesRead = inputStream!!.read(buffer, offset, readLength)
        if (bytesRead > 0) {
            position += bytesRead
            transferListener?.onBytesTransferred(this, dataSpec!!,false, bytesRead)
        }
        return bytesRead
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun getUri(): Uri? {
        return Uri.fromFile(file)
    }
    @SuppressLint("UnsafeOptInUsageError")
    override fun close() {
        inputStream?.close()
        transferListener?.onTransferEnd(this,dataSpec!!,false)
    }

    @UnstableApi
    override fun addTransferListener(transferListener: TransferListener) {
        this.transferListener = transferListener
    }
}



// وظيفة لاستخراج EVENT_ID من الرد الأول
private fun extractEventId(responseString: String?): String? {
    responseString?.let {
        val jsonObject = JSONObject(it)
        return jsonObject.getString("event_id")
    }
    return null
}

class SpeechChatControl(private val context: Context):BaseControl(context) {

    var  geminiApiClient:GeminiApiClient = GeminiApiClient();
//    var  API_URL:String = "https://api-inference.huggingface.co/models/wasmdashai/vits-ar-sa-huba";
//    var  API_URL:String = "https://api-inference.huggingface.co/models/wasmdashai/vits-ar-sa-huba-v2";
//    var  API_URL:String = "https://api-inference.huggingface.co/models/wasmdashai/vits-ar-sa-A";
    var  BASE_API_URL:String = "https://api-inference.huggingface.co/models/wasmdashai/";
    var  AUTHORIZATION:String = "Bearer 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";
    var  API_URL:String=BASE_API_URL+"vits-ar-sa-A"
    var  API_URL_ACTION_DEFAULT:String="vits-ar-sa-huba-v2"
    private var exoPlayer : ExoPlayerMedia?=null
    private var audioPlayer : AudioPlayer?=AudioPlayer(context);
    val semaphore = Semaphore(1)
    val latch = CountDownLatch(1)
    var  audioPlayerIsComplete=false
    var stopTheProcess=false;
    val flowAudioFileMap = mutableMapOf<Int, Any?>()
    var jopStreamFlow:Job?=null;
    var jopStreamAudio:Job?=null;
    var myExoPlayer:ExoPlayer?=null

    init {
        myExoPlayer =  ExoPlayer.Builder(context).build()
        exoPlayer=ExoPlayerMedia(context);
        exoPlayer?.initial()
    }
    fun generateWasmAudio(inputText:String, callBack: IWasmServiceEventListener) {
        try {
            val call = RetrofitInstance?.api?.getWasmAudio(inputText)!!;
            call?.enqueue(object : retrofit2.Callback<WasmAudioResponse?> {
                override fun onResponse(call: Call<WasmAudioResponse?>,
                    response: retrofit2.Response<WasmAudioResponse?>) {
                    try {
                        if (response != null && response!!.isSuccessful) {
                            val responseData: WasmAudioResponse? = response!!.body()
                            if (responseData != null && responseData.url.isNotEmpty()) {
                                Log.d("getResponseWasmAudioURL", responseData.url);
                                callBack?.onRequestIsSuccess(responseData.url)
                            } else
                                throw  Exception(response?.message()!!)
                        } else
                            throw  Exception(response?.message()!!)

                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (callBack != null)
                            callBack?.onRequestIsFailure(e.message!!)
                    }
                }

                override fun onFailure(call: Call<WasmAudioResponse?>, t: Throwable) {
                    if (callBack != null)
                        callBack?.onRequestIsFailure(t.message!!)
                }
            })

        } catch (e: IOException) {
            e.printStackTrace()
            if (callBack != null)
                callBack?.onRequestIsFailure(e.message!!)
        }


    }
    fun generateBasicTextAudio(inputText:String, callBack: IWasmServiceEventListener) {
        CoroutineScope(Dispatchers.Main).launch {

            var audioBytes =  withContext(Dispatchers.IO) {
                try {
                    var generat_text = generateText2(inputText)
                    if(generat_text==null || generat_text.trim().isEmpty())
                        return@withContext null
                    var text: String? = removeSymbols(generat_text!!) ?: return@withContext null
                    println(text)
                    if (text == null || text.isEmpty() || !containsLetters(text))
                        return@withContext null

                    return@withContext queryTextToSpeech(generat_text)
                } catch (e: Exception) {
                    return@withContext null
                }
            }
            try{
                if (audioBytes == null || audioBytes.isEmpty())
                    throw Exception("Failed to get audio bytes")

                val filePath = saveIntoTempFile(audioBytes);
                if(filePath!=null) {
                    playAudio2(
                        filePath,
                        myExoPlayer!!,
                        semaphore,
                        object : ICustomPlayerListener<ExoPlayer> {
                            override fun onErrorListener(mp: ExoPlayer?) {
                                stopTheProcess = true
                            }

                            override fun onCompletionListener(mp: ExoPlayer?) {
//                                                  Log.d("onCompletionListener5", "$audioPlayerIsComplete")
//                                                  if (audioPlayerIsComplete) {
//                                                      Log.d("onCompletionListener5", "$audioPlayerIsComplete")
//                                                      stopTheProcess = true
//                                                  }
                            }
                        })
                    delay(1000)
                    while (myExoPlayer?.isPlaying == true) {
                        delay(1000)
                    }
                    callBack?.onRequestIsSuccess2(null)
//                        }
//                            callBack?.onRequestIsSuccess(filePath)
                } else
                    throw Exception("Failed to save file")

                //                        var player = audioPlayer?.start(tempFile.absolutePath)
                //                        player?.setOnErrorListener { mp, what, extra ->
                //
                //                            try { audioPlayer?.takeIf { it.isPlayer() }?.stop() }
                //                            finally { callBack?.onRequestIsSuccess2() }
                //                            true // Return true if the error is considered handled, false otherwise
                //                        }
                //                        player?.setOnCompletionListener { mp ->
                //                            try { audioPlayer?.takeIf { it.isPlayer() }?.stop() }
                //                            finally { callBack?.onRequestIsSuccess() }
                //                        }
                //  println("Received audio bytes length: ${audioBytes.size}")


            } catch (e: Exception) {
                e.printStackTrace()
                callBack?.onRequestIsFailure(e.message!!)
            }
        }

    }
   suspend fun generateBasicTextAudioNew(inputText:String, callBack: IWasmServiceEventListener) {

           try {
               var audioBytes: ByteArray? = null;
                   var generat_text = generateText2(inputText)

                   if (generat_text != null || generat_text?.trim()?.isNotEmpty() == true) {
                       var text: String? = removeSymbols(generat_text!!) ?: null
                       println(text)
                       if (text != null && text.trim().isNotEmpty()) {
                           audioBytes =   queryTextToSpeech(text!!)
                           if (audioBytes == null || audioBytes?.isEmpty() == true) {
                               withContext(Dispatchers.Main) {
                                   callBack?.onRequestIsFailure("Server Error")
                               }
                           } else {
                               val tempFile = createTempFile(null, audioBytes!!);
                               withContext(Dispatchers.Main) {
                                   if (tempFile != null && tempFile != null && tempFile.length() > 0 && tempFile.canRead()) {
                                       exoPlayer?.playMedia(tempFile?.absolutePath!!, true,
                                           object : ICustomPlayerListener<ExoPlayer> {
                                               @OptIn(UnstableApi::class)
                                               override fun onErrorListener(
                                                   mp: ExoPlayer?,
                                                   error: Exception
                                               ) {
                                                   Log.e("Error", "ExoPlayer is Error")
                                                   try {
                                                       if (tempFile?.exists() == true)
                                                           tempFile?.delete()
                                                   } finally {
                                                       callBack?.onRequestIsFailure(error?.message!!)
                                                   }
                                               }

                                               override fun onCompletionListener(
                                                   mp: ExoPlayer?,
                                                   lastAudioClip: Boolean
                                               ) {

                                                   try {
                                                       if (tempFile?.exists() == true)
                                                           tempFile?.delete()
                                                   } finally {
                                                       callBack?.onRequestIsSuccess2(null)
                                                   }
                                               }
                                           })
                                   } else {
                                       callBack?.onRequestIsFailure("Invalid audio data file")
//                                       try {
//                                           delay(1000)
//                                           while (isActive && exoPlayer?.isPlayer() == true) {
//                                               delay(500)
//                                           }
//                                           if (tempFile?.exists() == true)
//                                               tempFile?.delete()
//                                           else {
//                                           }
//                                       } finally {
//                                           callBack?.onRequestIsSuccess2(null)
//                                       }
                                   }

                               }
                           }

                       } else {
                           withContext(Dispatchers.Main) {
                               callBack?.onRequestIsFailure("gemini generate text is null")
                           }
                       }
                   }
                   else {
                       withContext(Dispatchers.Main) {
                           callBack?.onRequestIsFailure("gemini generate text is null")
                       }
                   }

           } catch (e: Exception) {
               e.printStackTrace()
               withContext(Dispatchers.Main) {
                   callBack?.onRequestIsFailure(e?.message!!)
               }
           }

    }
    suspend fun generateBasicTextAudioNew2(inputText:String, callBack: IWasmServiceEventListener) {

        try {
            var audioBytes: ByteArray? = null;
            var generat_text = generateText2(inputText)

            if (generat_text != null || generat_text?.trim()?.isNotEmpty() == true) {
                var text: String? = removeSymbols(generat_text!!) ?: null
                println(text)
                if (text != null && text.trim().isNotEmpty()) {
                    audioBytes =   queryTextToSpeech(text!!)
                    if (audioBytes == null || audioBytes?.isEmpty() == true) {
                            callBack?.onRequestIsFailure("Server Error")
                    } else {
                           val tempFile = createTempFile(null, audioBytes!!);

                            if (tempFile != null && tempFile.canRead()) {
                                callBack?.onRequestIsSuccess(tempFile?.absolutePath?:"")
                            }
                            else {
                                callBack?.onRequestIsFailure("Invalid audio data file")
                        }
                    }

                } else {

                        callBack?.onRequestIsFailure("gemini generate text is null")

                }
            }
            else {

                callBack?.onRequestIsFailure("gemini generate text is null")
            }

        } catch (e: Exception) {
            e.printStackTrace()

                callBack?.onRequestIsFailure(e?.message!!)

        }

    }
    suspend  fun generateBasicTextAudio1(inputText:String, callBack: IWasmServiceEventListener) {

            try {
                var audioBytes:ByteArray?=null;
                var generat_text = generateText2(inputText)
                if (generat_text != null || generat_text?.trim()?.isNotEmpty() == true) {
                    var text: String? = removeSymbols(generat_text!!) ?: null
                    println(text)
                    if (text != null && text.trim().isNotEmpty()) {
                        try{
                             audioBytes = queryTextToSpeech(text!!)
                            if (audioBytes == null || audioBytes?.isEmpty()==true){
                                withContext(Dispatchers.Main) {
                                    callBack?.onRequestIsFailure("Server Error")
                                }
                            }
                            else {
                                val tempFile = createTempFile(null, audioBytes!!);
                                withContext(Dispatchers.Main) {
                                    if (tempFile != null && tempFile != null && tempFile.length() > 0 && tempFile.canRead()) {
                                        exoPlayer?.playMedia(tempFile?.absolutePath!!, true,
                                            object : ICustomPlayerListener<ExoPlayer> {
                                                @OptIn(UnstableApi::class)
                                                override fun onErrorListener(
                                                    mp: ExoPlayer?,
                                                    error: Exception
                                                ) {
                                                    Log.e("Error", "ExoPlayer is Error")
                                                    try {
                                                        if (tempFile?.exists() == true)
                                                            tempFile?.delete()
                                                    } finally {
                                                        callBack?.onRequestIsSuccess2(null)
                                                    }

//                                            callBack?.onRequestIsFailure()
                                                }

                                                override fun onCompletionListener(
                                                    mp: ExoPlayer?,
                                                    lastAudioClip: Boolean
                                                ) {

                                                    try {
                                                        if (tempFile?.exists() == true)
                                                            tempFile?.delete()
                                                    } finally {
                                                        callBack?.onRequestIsSuccess2(null)
                                                    }
                                                }


                                            })
                                    } else {
                                        try {
                                            delay(1000)
                                            while (isActive && exoPlayer?.isPlayer() == true) {
                                                delay(500)
                                            }
                                            if (tempFile?.exists() == true)
                                                tempFile?.delete()
                                            else {
                                            }
                                        } finally {
                                            callBack?.onRequestIsSuccess2(null)
                                        }
                                    }

                                }
                            }
                        }catch (e:Exception){
                            withContext(Dispatchers.Main){
                                callBack?.onRequestIsFailure(e.message!!)
                            }
                        }

                    }else{
                        withContext(Dispatchers.Main){
                            callBack?.onRequestIsSuccess2(null)
                        }
                    }
                }else{
                    withContext(Dispatchers.Main){
                        callBack?.onRequestIsSuccess2(null)
                    }
                }

            }catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    callBack?.onRequestIsSuccess2(null)
                }
            }


    }
    suspend  fun generateBasicTextAudio2(inputText:String, callBack: IWasmServiceEventListener) : Flow<MeasureNanoTimeModel> = flow{

            try {
                var audioBytes:ByteArray?=null;
                var generat_text:String? =null;

                var startTime = System.currentTimeMillis()
                    generat_text=generateText2(inputText)
//                }
                var elapsedTime =  System.currentTimeMillis()- startTime
                emit(MeasureNanoTimeModel("gemini",elapsedTime));
                if (generat_text != null || generat_text?.trim()?.isNotEmpty() == true) {
                    var text: String? = removeSymbols(generat_text!!) ?: null
                    println(text)
                    if (text != null && text.trim().isNotEmpty()) {
                        try{
                             startTime = System.currentTimeMillis()
                             audioBytes = queryTextToSpeech3(text!!)
                             elapsedTime =  System.currentTimeMillis()- startTime
                             emit(MeasureNanoTimeModel("query",elapsedTime));
                            if (audioBytes == null || audioBytes?.isEmpty()==true){
                                withContext(Dispatchers.Main){
                                    callBack?.onRequestIsFailure("Server return Null")
                                }
                            } else{
                                startTime = System.currentTimeMillis()
                                val tempFile = createTempFile(null, audioBytes!!);
                                withContext(Dispatchers.Main) {
                                        if (tempFile != null && tempFile != null && tempFile.length() > 0 && tempFile.canRead()) {
                                            exoPlayer?.playMedia(tempFile?.absolutePath!!, true,
                                                object : ICustomPlayerListener<ExoPlayer> {
                                                    @OptIn(UnstableApi::class)
                                                    override fun onErrorListener(
                                                        mp: ExoPlayer?,
                                                        error: Exception
                                                    ) {
                                                        Log.e("Error", "ExoPlayer is Error")
                                                        try {
                                                            if (tempFile?.exists() == true)
                                                                tempFile?.delete()
                                                        } finally {
                                                            callBack?.onRequestIsSuccess2(null)
                                                        }

    //                                            callBack?.onRequestIsFailure()
                                                    }

                                                    override fun onCompletionListener(
                                                        mp: ExoPlayer?,
                                                        lastAudioClip: Boolean
                                                    ) {

                                                        try {
                                                            if (tempFile?.exists() == true)
                                                                tempFile?.delete()
                                                        } finally {
                                                            callBack?.onRequestIsSuccess2(null)
                                                        }
                                                    }


                                                })
                                        } else {
                                            try {
                                                delay(1000)
                                                while (isActive && exoPlayer?.isPlayer() == true) {
                                                    delay(500)
                                                }
                                                if (tempFile?.exists() == true)
                                                    tempFile?.delete()
                                                else {
                                                }
                                            } finally {
                                                callBack?.onRequestIsSuccess2(null)
                                            }
                                        }

                                    }
                                elapsedTime =  System.currentTimeMillis()- startTime
                                emit(MeasureNanoTimeModel("handler",elapsedTime));
                            }
                        }catch (e:Exception){

                            withContext(Dispatchers.Main){
                                callBack?.onRequestIsFailure(e.message?:"")
                            }
                        }
                    }else{
                        withContext(Dispatchers.Main){
                            callBack?.onRequestIsSuccess2(null)
                        }
                    }
                }else{
                    withContext(Dispatchers.Main){
                        callBack?.onRequestIsSuccess2(null)
                    }
                }

            }catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    callBack?.onRequestIsFailure(e.message?:"")
                }
            }


    }

    suspend  fun convertBasicTextToAudio(text:String, callBack: IWasmServiceEventListener): Flow<MeasureNanoTimeModel> = flow {


        try {
            var audioBytes:ByteArray?=null;
            if (text != null || text?.trim()?.isNotEmpty() == true) {
                println(text)
                try{
                       var startTime = System.currentTimeMillis()
                        audioBytes = queryTextToSpeech3(text!!)
                       var elapsedTime =  System.currentTimeMillis()- startTime
                        emit(MeasureNanoTimeModel("query",elapsedTime));
                        if (audioBytes == null || audioBytes?.isEmpty()==true){
                                withContext(Dispatchers.Main) {
                                    callBack?.onRequestIsFailure("Server Error")
                                }
                        } else{
                            startTime = System.currentTimeMillis()
                                val tempFile = createTempFile(null, audioBytes!!);
                                withContext(Dispatchers.Main) {
                                    if (tempFile != null && tempFile != null && tempFile.length() > 0 && tempFile.canRead()) {
                                        exoPlayer?.playMedia(tempFile?.absolutePath!!, true,
                                            object : ICustomPlayerListener<ExoPlayer> {
                                                @OptIn(UnstableApi::class)
                                                override fun onErrorListener(
                                                    mp: ExoPlayer?,
                                                    error: Exception
                                                ) {
                                                    Log.e("Error", "ExoPlayer is Error")
                                                    try {
                                                        if (tempFile?.exists() == true)
                                                            tempFile?.delete()
                                                    } finally {
                                                        callBack?.onRequestIsSuccess2(null)
                                                    }

        //                                            callBack?.onRequestIsFailure()
                                                }

                                                override fun onCompletionListener(
                                                    mp: ExoPlayer?,
                                                    lastAudioClip: Boolean
                                                ) {

                                                    try {
                                                        if (tempFile?.exists() == true)
                                                            tempFile?.delete()
                                                    } finally {
                                                        callBack?.onRequestIsSuccess2(null)
                                                    }
                                                }


                                            })
                                    } else {
                                        try {
                                            delay(1000)
                                            while (isActive && exoPlayer?.isPlayer() == true) {
                                                delay(500)
                                            }
                                            if (tempFile?.exists() == true)
                                                tempFile?.delete()
                                            else {
                                            }
                                        } finally {
                                            callBack?.onRequestIsSuccess2(null)
                                        }
                                    }

                                }
                            elapsedTime =  System.currentTimeMillis()- startTime
                            emit(MeasureNanoTimeModel("handler",elapsedTime));
                        }
                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        callBack?.onRequestIsFailure(e.message!!)
                    }
                }
            }else{
                withContext(Dispatchers.Main){
                    callBack?.onRequestIsSuccess2(null)
                }
            }

        }catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main){
                callBack?.onRequestIsFailure(e.message!!)
            }
        }

    }
    suspend fun generateBasicTextAudio2(inputText:String, callBack: IWasmServiceEventListener,
                                        scope:CoroutineScope) {


        var audioBytes =  withContext(Dispatchers.IO) {
            try {
                var generat_text = generateText2(inputText)
                if(generat_text==null || generat_text.trim().isEmpty())
                    return@withContext null
                var text: String? = removeSymbols(generat_text!!) ?: return@withContext null
                println(text)
                if (text == null || text.isEmpty() || !containsLetters(text))
                    return@withContext null

                return@withContext queryTextToSpeech(generat_text)
            }
            catch (e: Exception) {
                return@withContext null
            }
        }
        try{
            if (audioBytes == null || audioBytes.isEmpty())
                throw Exception("Failed to get audio bytes")

            val filePath = saveIntoTempFile(audioBytes);
            withContext(Dispatchers.Main) {
                if(filePath!=null) {

                    exoPlayer?.playMedia(filePath , false,
                        object : ICustomPlayerListener<ExoPlayer> {
                            @OptIn(UnstableApi::class)
                            override fun onErrorListener(
                                mp: ExoPlayer?,
                                error: Exception
                            ) {
                                Log.e("Error","ExoPlayer is Error")
                                callBack?.onRequestIsSuccess2(null)
//                                            callBack?.onRequestIsFailure()
                            }

                            override fun onCompletionListener(
                                mp: ExoPlayer?,
                                lastAudioClip: Boolean
                            ) {

                                callBack?.onRequestIsSuccess2(null)
                            }


                        })
                }
                else {

                    delay(1000)
                    while (exoPlayer?.isPlayer() == true) {
                        delay(1000)
                    }
                    callBack?.onRequestIsSuccess2(null)
                }

            }

        } catch (e: Exception) {
            e.printStackTrace()
            callBack?.onRequestIsFailure(e.message!!)
        }


    }

    fun generateBasicTextAudio3(inputText:String, callBack: IWasmServiceEventListener,
                                scope:CoroutineScope) {
     CoroutineScope(Dispatchers.IO).launch {

                 try {
                     var audioBytes:ByteArray?=null;
                     var generat_text = generateText2(inputText)
                     if (generat_text != null || generat_text?.trim()?.isNotEmpty() == true) {
                         var text: String? = removeSymbols(generat_text!!) ?: null
                         println(text)
                         if (text != null && text.isNotEmpty()) {
                             audioBytes = queryTextToSpeech(generat_text!!)
                             if (audioBytes == null || audioBytes.isEmpty())
                                 throw Exception("Failed to get audio bytes")

                             val filePath = saveIntoTempFile(audioBytes);
                             withContext(Dispatchers.Main) {
                                 if(filePath!=null) {

                                     exoPlayer?.playMedia(filePath , false,
                                         object : ICustomPlayerListener<ExoPlayer> {
                                             @OptIn(UnstableApi::class)
                                             override fun onErrorListener(
                                                 mp: ExoPlayer?,
                                                 error: Exception
                                             ) {
                                                 Log.e("Error","ExoPlayer is Error")
                                                 callBack?.onRequestIsSuccess2(null)
//                                            callBack?.onRequestIsFailure()
                                             }

                                             override fun onCompletionListener(
                                                 mp: ExoPlayer?,
                                                 lastAudioClip: Boolean
                                             ) {

                                                 callBack?.onRequestIsSuccess2(null)
                                             }


                                         })
                                 }
                                 else {

                                     delay(1000)
                                     while (exoPlayer?.isPlayer() == true) {
                                         delay(1000)
                                     }
                                     callBack?.onRequestIsSuccess2(null)
                                 }

                             }
                         }else{
                             callBack?.onRequestIsSuccess2(null)
                         }
                     }else{
                         callBack?.onRequestIsSuccess2(null)
                     }

                 }catch (e: Exception) {
                 e.printStackTrace()
                 callBack?.onRequestIsFailure(e.message!!)
             }
         }

    }
    fun generateStreamTextAudio2(inputText:String, callBack: IWasmServiceEventListener) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                    override fun onStreamReader(response: String?,lastFlow:Boolean) {
                        var text = removeSymbols(response!!)?.trim()?.replace("*","")

                        if (text != null && text.isNotEmpty() && containsLetters(text)) {
                            CoroutineScope(Dispatchers.IO).launch {
                                Log.d("audioBytes ",text!!)
                                semaphore.withPermit {

                                    Log.d("audioBytes ",response!!)
                                    val audioBytes = queryTextToSpeech(text)
                                    withContext(Dispatchers.Main) {
                                        try {
                                            if (audioBytes == null || audioBytes.isEmpty()) {
                                                throw Exception("Failed to get audio bytes")
                                            }
                                            val filePath = saveIntoTempFile(audioBytes)
                                            if (filePath != null) {
                                                Log.d("filePath",filePath!!)
//                                                var player = audioPlayer?.start(filePath)
//                                                player?.setOnErrorListener { mp, what, extra ->
//                                                    audioPlayer?.stop()
//                                                    true // Return true if the error is considered handled, false otherwise
//                                                }
//                                                player?.setOnCompletionListener { mp ->
//                                                    try { audioPlayer?.stop() }
//                                                    finally {   latch.countDown() }
//                                                }
                                                exoPlayer?.start(filePath, object :
                                                    ICustomPlayerListener<ExoPlayer> {
                                                     override fun onErrorListener(mp: ExoPlayer?) {
                                                         try { exoPlayer?.stop() }
                                                         finally { latch.countDown() }
                                                     }
                                                     override fun onCompletionListener(mp: ExoPlayer?) {
                                                         try { exoPlayer?.stop() }
                                                         finally {   latch.countDown() }
                                                     }
                                                 });
                                            }else
                                                throw Exception("Failed to get audio bytes")

                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            callBack?.onRequestIsFailure("EERR: "+e.message!!)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    override fun onStreamComplete(lastFlowIndex:Int) {

                        try {
                            latch.await()
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        } finally {
                            CoroutineScope(Dispatchers.Main).launch {

                                try { exoPlayer?.stop() }
                                finally {
                                    callBack?.onRequestIsSuccess("")
                                }

                            }
                        }

                    }

                    override fun onStreamError(e: Throwable) {
                        CoroutineScope(Dispatchers.Main).launch {
                            callBack?.onRequestIsFailure(e.message!!)
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        //        CoroutineScope(Dispatchers.IO).launch {
        //
        //                    try {
        //                         geminiApiClient?.sendMessageStream(inputText,object :
        //                             IListenerStream<String> {
        //                             override fun onStreamReader(data: String?) {
        //                                 var text = removeSymbols(data!!)?.trim()
        //                                 if(text != null && text!!.isNotEmpty() || !containsLetters(text!!)){
        //                                     var audioBytes=queryTextToSpeech(text)
        //                                     CoroutineScope(Dispatchers.Main).launch {
        //                                         try{
        //                                             if (audioBytes == null || audioBytes.isEmpty())
        //                                                 throw Exception("Failed to get audio bytes")
        //
        //                                             val filePath = saveIntoTempFile(audioBytes);
        //                                             if(filePath!=null)
        //                                                 println(filePath)
        ////                                             {
        ////                                                 exoPlayer?.startPlayer(filePath, object :
        ////                                                     ICustomPlayerListener<ExoPlayer> {
        ////                                                     override fun onErrorListener(mp: ExoPlayer?) {
        ////                                                         try { exoPlayer?.stop() }
        ////                                                         finally { speechRecognizerListenAgain(); }
        ////                                                     }
        ////                                                     override fun onCompletionListener(mp: ExoPlayer?) {
        ////                                                         try { exoPlayer?.stop() }
        ////                                                         finally { speechRecognizerListenAgain(); }
        ////                                                     }
        ////                                                 });
        ////                                             }
        ////                                                 callBack?.onRequestIsSuccess(filePath)
        ////                                             else
        ////                                                 throw Exception("Failed to save file")
        //
        //                                         } catch (e: Exception) {
        //                                             e.printStackTrace()
        //                                             callBack?.onRequestIsFailure(e.message!!)
        //                                         }
        //                                     }
        //                                 }
        //                             }
        //
        //                             override fun onStreamCompletion() {
        //                                 TODO("Not yet implemented")
        //                             }
        //                         })
        //
        //
        //
        //                    } catch (e: Exception) {
        //
        //                    }
        //                }

    }
    suspend fun generateStreamTextAudio4(inputText: String, callBack: IWasmServiceEventListener) {
        audioPlayerIsComplete=false
        stopTheProcess=false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                    override fun onStreamReader(response: String?,indexFlow:Int,lastIndexFlow:Int) {
                        if(response!=null){
                                    Log.d("response", "$response - $indexFlow")
                                    var text: String? = removeSymbols(response?.trim()!!) ?: return
//                                    text = text?.trim()?.replace("*", "")
                                    Log.d("audioBytes", text!!)
                                    if (!text.isNullOrEmpty() && containsLetters(text)) {
                                        Log.d("text", "$text")
                                        CoroutineScope(Dispatchers.IO).launch {

                                            try {
                                                var audioBytes = queryTextToSpeech(text)
                                                if (audioBytes == null) {
                                                    semaphore.withPermit {
                                                        stopTheProcess=true
                                                        audioPlayerIsComplete=true
                                                    }
//                                                    callBack?.onRequestIsSuccess2(stopExoPlayer)
                                                } else {
//                                                    delay((indexFlow * 100) as Long)
                                                    withContext(Dispatchers.Main) {
                                                        semaphore.withPermit {
                                                            handleAudioBytes(audioBytes, null)
                                                            delay(1000)
                                                            while (myExoPlayer?.isPlaying == true) {
                                                                delay(1000)
                                                            }
                                                            Log.d("CompletedExoPlayer", "$audioPlayerIsComplete")
                                                            if(audioPlayerIsComplete) {
                                                            stopTheProcess=true
//                                                            callBack?.onRequestIsSuccess2(
//                                                                stopExoPlayer
//                                                            )
                                                        }
                                                        }
                                                    }
                                                }

                                            } catch (e:Exception){
                                                semaphore.withPermit { stopTheProcess=true }
//                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                            }
                                        }
                                    }
                                    else if(response?.trim()==ContentApp.END_SYMBOL) {

                                        Log.d("Completed", response!!)
                                           CoroutineScope(Dispatchers.Main).launch {
                                               semaphore.withPermit {
                                                   audioPlayerIsComplete = true;
                                               }
                                           }
                                       }
                                    }


                        }
                    override fun onStreamComplete(lastFlowIndex:Int) {

                        CoroutineScope(Dispatchers.Main).launch {

//                                    latch.await()
                                try {
                                    semaphore.withPermit {
                                        audioPlayerIsComplete = true;
//                                        exoPlayer?.waitForRemainingTime();
//                                        exoPlayer?.stop()
                                    }

                                } finally {
//                                  CoroutineScope(Dispatchers.Main)   callBack?.onRequestIsSuccess("")
                                }
                        }

                    }
                    override fun onStreamError(e: Throwable) {
                        CoroutineScope(Dispatchers.Main).launch {
                            semaphore.withPermit {
                                try { myExoPlayer?.stop() }
                                finally {
                                    audioPlayerIsComplete=true
                                    stopTheProcess=true
//                                    callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                }
                            }
                        }
                    }
                })
            }
            catch (e: Exception) {
                stopTheProcess=true
                e.printStackTrace()
            }


        }
//        delay(1000)
       var jop=CoroutineScope(Dispatchers.IO).launch {

            while (true){
                Log.d("Monitor", "Monitor")
                delay(1000)
                var res= semaphore.withPermit {
                    return@withPermit stopTheProcess
                }
                if(res) break
            }
           withContext(Dispatchers.Main){
                   try {
                           semaphore.withPermit {
                               if(myExoPlayer?.isPlaying==true)
                                   myExoPlayer?.stop()
                           }
                   }catch (e:Exception){
                       e.printStackTrace()
                   }
               }
           Log.d("onEndTask","End")
        }
        jop?.start()
        jop?.join()

        Log.d("MonitorEnd", "MonitorEnd")
    }
    suspend fun generateStreamTextAudio4Last(inputText: String, callBack: IWasmServiceEventListener){
        audioPlayerIsComplete=false
        stopTheProcess=false
//        var completeAllAudio:Boolean=false
        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                        override fun onStreamReader(response: String?,indexFlow:Int,lastIndexFlow:Int) {
                            if(response!=null){
                                Log.d("response", "$response - $indexFlow")
                                var text: String? = removeSymbols(response?.trim()!!) ?: return
                                Log.d("audioBytes", text!!)
                                if (!text.isNullOrEmpty() && containsLetters(text)) {
                                    Log.d("text", "$text")
                                    CoroutineScope(Dispatchers.IO).launch {

                                        try {
                                            var audioBytes = queryTextToSpeech(text)
                                            if (audioBytes == null) {
                                                semaphore.withPermit {
                                                    stopTheProcess=true
                                                    audioPlayerIsComplete=true
                                                }
    //                                                    callBack?.onRequestIsSuccess2(stopExoPlayer)
                                            } else {

                                                val tempFile: File? = createTempFile(indexFlow, audioBytes!!)
                                                if (tempFile != null && tempFile?.absolutePath?.isNullOrEmpty() == false) {
                                                    semaphore.withPermit {
                                                        flowAudioFileMap?.put(indexFlow, tempFile)
                                                        Log.d("flow", "$indexFlow - $lastIndexFlow")
                                                        return@withPermit
                                                    }
                                                }
    //
                                            }

                                        } catch (e:Exception){
                                            semaphore.withPermit { stopTheProcess=true }
    //                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                        }
                                    }
                                }
                                else if(response?.trim()==ContentApp.END_SYMBOL) {
                                    Log.d("Completed", response!!)
                                    CoroutineScope(Dispatchers.Main).launch {
                                        semaphore.withPermit {
                                            audioPlayerIsComplete=true
                                            flowAudioFileMap?.put(indexFlow, response?.trim())
                                        }
                                    }
                                }
                            }
                        }
                        override fun onStreamComplete(lastFlowIndex:Int) {

                            CoroutineScope(Dispatchers.Main).launch {

    //                                    latch.await()
                                try {
                                    semaphore.withPermit {
                                        audioPlayerIsComplete = true;
    //                                        exoPlayer?.waitForRemainingTime();
    //                                        exoPlayer?.stop()
                                    }

                                } finally {
    //                                  CoroutineScope(Dispatchers.Main)   callBack?.onRequestIsSuccess("")
                                }
                            }

                        }
                        override fun onStreamError(e: Throwable) {
                            CoroutineScope(Dispatchers.Main).launch {
                                semaphore.withPermit {
                                    try { myExoPlayer?.stop() }
                                    finally {
                                        audioPlayerIsComplete=true
                                        stopTheProcess=true
    //                                    callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                    }
                                }
                            }
                        }
                    })
                }
                catch (e: Exception) {
                    stopTheProcess=true
                    e.printStackTrace()
                }
            }
            var jop=CoroutineScope(Dispatchers.IO).launch {
                var index=0;
                delay(1000)
                while (!stopTheProcess){
                    withContext(Dispatchers.Main) {
                      semaphore?.withPermit {
                          if(flowAudioFileMap?.isNullOrEmpty()==false && flowAudioFileMap?.containsKey(index)==true) {
                              Log.d("index","$index")
                              var value = flowAudioFileMap?.getValue(index)
                              if(value != null) {

                                  if (value is File) {
                                      playAudio2(
                                          value?.absolutePath!!,
                                          myExoPlayer!!,
                                          semaphore,
                                          object : ICustomPlayerListener<ExoPlayer> {
                                              override fun onErrorListener(mp: ExoPlayer?) {
                                                          stopTheProcess = true
                                              }
                                              override fun onCompletionListener(mp: ExoPlayer?) {
//                                                  Log.d("onCompletionListener5", "$audioPlayerIsComplete")
//                                                  if (audioPlayerIsComplete) {
//                                                      Log.d("onCompletionListener5", "$audioPlayerIsComplete")
//                                                      stopTheProcess = true
//                                                  }
                                              }
                                          })
                                      delay(1000)
                                      while (myExoPlayer?.isPlaying == true) {
                                          delay(1000)
                                      }
                                  }
                                  else if (value is String) {
                                      stopTheProcess = true
                                      audioPlayerIsComplete = true
                                  }
                              }
                              index+=1
                          }
                          return@withPermit
                        }
                        return@withContext
                    }
//                    delay(500)
                }
                withContext(Dispatchers.Main){
                    try {
                        semaphore.withPermit {
                            if(myExoPlayer?.isPlaying==true)
                                myExoPlayer?.stop()
                        }
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }
                Log.d("onEndTask","End")
            }
            jop?.start()
            jop?.join()

        }finally {
            semaphore?.withPermit {
                if(flowAudioFileMap?.isNullOrEmpty()==false) {
                    for (item in  flowAudioFileMap){
                        if(item?.value is File)
                            (item?.value as File)?.delete()
                    }
                    flowAudioFileMap.clear()
                }
                Log.d("flowAudioFileMap", "${flowAudioFileMap?.count()}")
            }
        }
        Log.d("MonitorEnd", "MonitorEnd")
    }
    suspend fun generateStreamTextAudio5Last(inputText: String,dispatcher:CoroutineDispatcher)= coroutineScope {
        audioPlayerIsComplete=false
        stopTheProcess=false

        try {

            var responseAsync = async(dispatcher) {
                return@async geminiApiClient?.sendMessageStreamLastTest(inputText)
            }

            try {
                var indexFlow=0;
                var ioDispatcher=Dispatchers.IO
                var responseFlow = responseAsync.await()
                responseFlow?.flowOn(dispatcher)
                    ?.onCompletion { cause ->
                        if (cause == null) {
                            println("responseFlow: Completed successfully")
                        }
                        else {
                            println("responseFlow: Completed with error: ${cause.message}") // التعامل مع الأخطاء إن وجدت
                        }
                        semaphore.withPermit {
                            stopTheProcess=true
                        }
                    }
                    ?.collect { response ->
                        val content = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.asTextOrNull()
                        var responseText=content?.trim()
                        if (!responseText.isNullOrEmpty()) {
                            var text=responseText.replace("*","")
                            text=text.replace(Regex("\\s+"), " ")
                            Log.d("$indexFlow:",text)
                            CoroutineScope(ioDispatcher).launch {
                                try {
                                    var audioBytes = queryTextToSpeech(text)
                                    if (audioBytes == null) {
                                        semaphore.withPermit {
                                            stopTheProcess=true
                                            audioPlayerIsComplete=true
                                        }
//                                              callBack?.onRequestIsSuccess2(stopExoPlayer)
                                    } else {

                                        val tempFile: File? = createTempFile(indexFlow, audioBytes!!)
                                        if (tempFile != null && tempFile?.absolutePath?.isNullOrEmpty() == false) {
                                            semaphore.withPermit {
                                                flowAudioFileMap?.put(indexFlow, tempFile)
                                                Log.d("flow", "$indexFlow ")
                                                return@withPermit
                                            }
                                        }
                                        //
                                    }

                                } catch (e:Exception){
                                    semaphore.withPermit { stopTheProcess=true }
                                    //                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                }
                            }
                            semaphore.withPermit {
                                indexFlow++
                            }
                        }

                    }
            }
            catch (e: Exception) {
                stopTheProcess=true
                e.printStackTrace()
            }

           launch(dispatcher) {
              println("PlayerAudio")
               var playListener:Player.Listener?=null
               var index=0;
               delay(1000)
               while (!stopTheProcess){
                   withContext(Dispatchers.Main) {
                       semaphore?.withPermit {
                           if(flowAudioFileMap?.isNullOrEmpty()==false && flowAudioFileMap?.containsKey(index)==true) {
                               Log.d("index","$index")
                               var value = flowAudioFileMap?.getValue(index)
                               if(value != null) {

                                   if (value is File) {
                                       playAudio2(
                                           value?.absolutePath!!,
                                           myExoPlayer!!,
                                           semaphore,
                                           object : ICustomPlayerListener<ExoPlayer> {
                                               override fun onErrorListener(mp: ExoPlayer?) {
                                                   stopTheProcess = true
                                               }
                                               override fun onCompletionListener(mp: ExoPlayer?) {
//                                                  Log.d("onCompletionListener5", "$audioPlayerIsComplete")
//                                                  if (audioPlayerIsComplete) {
//                                                      Log.d("onCompletionListener5", "$audioPlayerIsComplete")
//                                                      stopTheProcess = true
//                                                  }
                                               }
                                           })
                                       delay(1000)
                                       while (myExoPlayer?.isPlaying == true) {
                                           delay(1000)
                                       }
                                   } else if (value is String) {
                                       stopTheProcess = true
                                       audioPlayerIsComplete = true
                                       if(playListener!=null)
                                            myExoPlayer?.removeListener(playListener)
                                   }
                               }
                               index+=1
                           }
                           return@withPermit
                       }
                       return@withContext
                   }
//                    delay(500)
               }
               withContext(Dispatchers.Main){
                   try {
                       semaphore.withPermit {
                           if(myExoPlayer?.isPlaying==true)
                               myExoPlayer?.stop()
                       }
                   }catch (e:Exception){
                       e.printStackTrace()
                   }
               }
               Log.d("onEndTask","End")
           }
//                var index=0;
//
//                while (index<10){
//                        println("Speak - ${index++}")
//                }
//                Log.d("onEndTask","End")
//            }

        }finally {
            //  response?.cancelAndJoin()
        }
        Log.d("MonitorEnd", "MonitorEnd")
    }
    suspend fun generateStreamTextAudio6Last(inputText: String,dispatcher:CoroutineDispatcher)= coroutineScope {
            audioPlayerIsComplete=false
            stopTheProcess=false

            try {

                launch {
                    var ioDispatcher=Dispatchers.IO
                    try {
                        geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                            override fun onStreamReader(
                                response: String?,
                                indexFlow: Int,
                                lastIndexFlow: Int
                            ) {
                                if (response != null) {
                                    Log.d("response", "$response - $indexFlow")
                                    var text: String? = removeSymbols(response?.trim()!!) ?: return
                                    Log.d("audioBytes", text!!)
                                    if (!text.isNullOrEmpty() && containsLetters(text)) {
                                        Log.d("text", "$text")
                                        CoroutineScope(ioDispatcher).launch {

                                            try {

                                                var audioBytes = queryTextToSpeech(text)
                                                if (audioBytes == null) {
                                                    semaphore.withPermit {
                                                        stopTheProcess = true
                                                        audioPlayerIsComplete = true
                                                    }
                                                    //                                                    callBack?.onRequestIsSuccess2(stopExoPlayer)
                                                } else {

                                                    val tempFile: File? =
                                                        createTempFile(indexFlow, audioBytes!!)
                                                    if (tempFile != null && tempFile?.absolutePath?.isNullOrEmpty() == false) {
                                                        semaphore.withPermit {
                                                            flowAudioFileMap?.put(indexFlow, tempFile)
                                                            Log.d("flow", "$indexFlow - $lastIndexFlow")
                                                            return@withPermit
                                                        }
                                                    }
                                                    //
                                                }

                                            } catch (e: IOException) {
                                                semaphore.withPermit { stopTheProcess = true }
                                                //                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                            }  catch (e: Exception) {
                                                semaphore.withPermit { stopTheProcess = true }
                                                //                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                            }
                                        }
                                    } else if (response?.trim() == ContentApp.END_SYMBOL) {
                                        Log.d("Completed", response!!)
                                        CoroutineScope(Dispatchers.Main).launch {
                                            semaphore.withPermit {
                                                audioPlayerIsComplete = true
                                                flowAudioFileMap?.put(indexFlow, response?.trim())
                                            }
                                        }
                                    }
                                }
                            }
                            override fun onStreamComplete(lastFlowIndex: Int) {

                                CoroutineScope(Dispatchers.Main).launch {

                                    //                                    latch.await()
                                    try {
                                        semaphore.withPermit {
                                            audioPlayerIsComplete = true;
                                            //                                        exoPlayer?.waitForRemainingTime();
                                            //                                        exoPlayer?.stop()
                                        }

                                    } finally {
                                        //                                  CoroutineScope(Dispatchers.Main)   callBack?.onRequestIsSuccess("")
                                    }
                                }

                            }
                            override fun onStreamError(e: Throwable) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    semaphore.withPermit {
                                        try {
                                            myExoPlayer?.stop()
                                        } finally {
                                            audioPlayerIsComplete = true
                                            stopTheProcess = true
                                            //                                    callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                        }
                                    }
                                }
                            }
                        })

                    } catch (e: IOException) {
                        stopTheProcess=true
                        e.printStackTrace()
                    } catch (e: Exception) {
                        stopTheProcess=true
                        e.printStackTrace()
                    }
                }
                launch {
                    println("PlayerAudio")
                    var playListener:Player.Listener?=null
                    var index=0;
                    var count=0;
                    delay(1500)
                    while (!stopTheProcess){
                        withContext(Dispatchers.Main) {
                            semaphore?.withPermit {
                                if(flowAudioFileMap?.isNullOrEmpty()==false && flowAudioFileMap?.containsKey(index)==true) {

                                    var value = flowAudioFileMap?.getValue(index)
                                    if(value != null) {
                                        if (value is File) {
                                            if(myExoPlayer?.mediaItemCount==0 || index==0) {
                                                myExoPlayer?.addMediaItem(MediaItem.fromUri(value?.absolutePath!!))
                                                 playListener = playExoPlayer(
                                                    value?.absolutePath!!,
                                                    myExoPlayer!!,
                                                    object : ICustomPlayerListener<ExoPlayer> {
                                                        override fun onErrorListener(mp: ExoPlayer?) {
                                                            stopTheProcess = true
                                                        }
                                                        override fun onCompletionListener(mp: ExoPlayer?) {
                                                          Log.d("onCompletionListener5", "${mp?.currentMediaItemIndex} - $count")
//                                                            if(count>0 && mp?.currentMediaItemIndex!! >= count-1){
//
//                                                            }
        //                                                  if (audioPlayerIsComplete) {
        //                                                      Log.d("onCompletionListener5", "$audioPlayerIsComplete")
        //                                                      stopTheProcess = true
        //                                                  }
                                                        }
                                                    })
                                            }else{
                                                Log.d("addMediaItem","$index")
                                                myExoPlayer?.addMediaItem(MediaItem.fromUri(value?.absolutePath!!))
                                                Log.d("myExoPlayer","${myExoPlayer?.isPlaying}")
                                               if(myExoPlayer?.isPlaying==false)
                                                    myExoPlayer?.prepare()
//                                                myExoPlayer?.currentMediaItemIndex
//                                                myExoPlayer?.hasNextMediaItem()
                                            }

//                                            delay(1000)
//                                            while (myExoPlayer?.isPlaying == true) {
//                                                delay(1000)
//                                            }
                                        }
                                        else if (value is String) {
                                            count=(flowAudioFileMap?.count()?:0)-1
                                            playExoPlayer("",myExoPlayer!!, object : ICustomPlayerListener<ExoPlayer> {
                                                override fun onErrorListener(mp: ExoPlayer?) {
                                                    stopTheProcess = true
                                                    Log.d("onErrorListener", "${mp?.currentMediaItemIndex} - $count")
                                                }
                                                override fun onCompletionListener(mp: ExoPlayer?) {
                                                    Log.d("onCompletionListener", "${mp?.currentMediaItemIndex} - $count")
                                                }
                                            })
                                            stopTheProcess = true
                                            audioPlayerIsComplete = true
                                        }
                                        index+=1
                                    }
                                }
                                return@withPermit
                            }
                            return@withContext
                        }
//                    delay(500)
                    }
                    withContext(Dispatchers.Main){
                        try {
                            semaphore.withPermit {
                                if(playListener!=null && myExoPlayer!=null){
                                    myExoPlayer?.removeListener(playListener!!)
                                }
                                if(myExoPlayer?.isPlaying==true)
                                    myExoPlayer?.stop()
                            }
                        }catch (e:Exception){
                            e.printStackTrace()
                        }finally {

                        }
                    }
                    Log.d("onEndTask","End")
                   return@launch
                }
//                jop?.await()
            }
            finally {
                //  response?.cancelAndJoin()
            }
            Log.d("MonitorEnd", "MonitorEnd")
            return@coroutineScope
        }
    suspend fun generateStreamTextAudio7Last(inputText: String,dispatcher:CoroutineContext)= coroutineScope {
            audioPlayerIsComplete=false
            stopTheProcess=false
            var playListener:Player.Listener?=null
//            var scope=CoroutineScope(dispatcher)
            try {

                launch {
                    var ioDispatcher=Dispatchers.IO
                    try {
                        geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                            override fun onStreamReader(
                                response: String?,
                                indexFlow: Int,
                                lastIndexFlow: Int
                            ) {
                                if (response != null) {
                                    Log.d("response", "$response - $indexFlow")
                                    var text: String? = removeSymbols(response?.trim()!!) ?: return
                                    Log.d("audioBytes", text!!)
                                    CoroutineScope(ioDispatcher).launch {

                                    if (!text.isNullOrEmpty() && containsLetters(text)) {
                                        Log.d("text", "$text")
                                            try {
                                                delay(100)
                                                var audioBytes = queryTextToSpeech(text)
                                                if (audioBytes == null) {
                                                    semaphore.withPermit {
                                                        stopTheProcess = true
                                                        audioPlayerIsComplete = true }
//                                          callBack?.onRequestIsSuccess2(stopExoPlayer)
                                                } else {

                                                    val tempFile: File? = createTempFile(indexFlow, audioBytes!!)
                                                    if (tempFile != null && tempFile?.absolutePath?.isNullOrEmpty() == false) {
                                                        semaphore.withPermit {
                                                            flowAudioFileMap?.put(
                                                                indexFlow,
                                                                tempFile)
                                                            Log.d("flow", "$indexFlow - $lastIndexFlow")
                                                            return@withPermit
                                                        }
                                                    }
                                                }

                                            }
                                            catch (e: IOException) {
                                                semaphore.withPermit { stopTheProcess = true }
                                                //                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                            } catch (e: Exception) {
                                                semaphore.withPermit { stopTheProcess = true }
                                                //                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                            }

                                        }
                                    else if (response?.trim() == ContentApp.END_SYMBOL) {
                                            Log.d("Completed", response!!)
                                                semaphore.withPermit {
                                                 audioPlayerIsComplete = true
                                                    flowAudioFileMap?.put(indexFlow, ContentApp.END_SYMBOL)
                                                }
                                        }

                                    }
                                }
                            }
                            override fun onStreamComplete(lastFlowIndex: Int) {

                            }
                            override fun onStreamError(e: Throwable) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    semaphore.withPermit {
                                        try {
                                            myExoPlayer?.stop()
                                        } finally {
                                            audioPlayerIsComplete = true
                                            stopTheProcess = true
                                            //                                    callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                        }
                                    }
                                }
                            }
                        })
                    } catch (e: IOException) {
                        stopTheProcess=true
                        e.printStackTrace()
                    } catch (e: Exception) {
                        stopTheProcess=true
                        e.printStackTrace()
                    }
                }
                var jop = CoroutineScope(Dispatchers.IO).launch {
                    var audioPlayerIsComplete=false
                    println("PlayerAudio")
                    var index=0;
                    delay(1000)
                    while (!stopTheProcess){
//                        println("PlayerAudio:$index")
                        var value = semaphore?.withPermit {
                                if(flowAudioFileMap?.isNullOrEmpty()==false && flowAudioFileMap?.containsKey(index)==true) {
                                    return@withPermit flowAudioFileMap?.getValue(index)
                                }
                                return@withPermit null
                            }
                        if(value != null) {
                            if (value is File) {
                                withContext(Dispatchers.Main) {
                                    semaphore?.withPermit {
                                        var isComplete=false;
                                        if(flowAudioFileMap?.containsValue(ContentApp.END_SYMBOL)==true){
                                            isComplete= this@SpeechChatControl.flowAudioFileMap?.get(index+1) is String
                                        }
                                        audioPlayerIsComplete=false
                                        playListener = playExoPlayer(
                                            value?.absolutePath!!,
                                            myExoPlayer!!,
                                            isComplete,
                                            object : ICustomPlayerListener<ExoPlayer> {
                                                override fun onErrorListener(mp: ExoPlayer?) {
                                                    stopTheProcess = true
                                                }
                                                override fun onCompletionListener(mp: ExoPlayer?, isComplete:Boolean) {
                                                    Log.d("onCompletionListener5", "$isComplete -- $index")
                                                           audioPlayerIsComplete=true
                                                            if(isComplete){
                                                                stopTheProcess = true
                                                            }
                                                }
                                            })
                                    }
                                    delay(1000L)
                                    while (semaphore?.withPermit{ myExoPlayer?.isPlaying } == true || !audioPlayerIsComplete) {
                                        delay(500L)
                                    }
                                }
                            } else if (value is String) {
                                Log.d("onEndTask",value)
                                semaphore?.withPermit {
                                    stopTheProcess = true
                                    audioPlayerIsComplete = true
                                }
                                break
                            }
                            index+=1
                        }
//                    delay(500)
                    }
                    Log.d("onEndTask","End")
                    return@launch
                }
                jop?.join()

            }catch (e:IOException){
                e.printStackTrace()
            } catch (e:Exception){
                e.printStackTrace()
            }finally {
                semaphore.withPermit {
                    if(flowAudioFileMap?.isNotEmpty()==true){
                        try {
                            flowAudioFileMap?.forEach { i ->
                                if (i.value is File) {
                                    var file = (i.value as File)
                                    if (file.exists())
                                        file.delete()
                                }
                            }
                        }finally {
                            flowAudioFileMap?.clear()
                        }
                    }
                }
                withContext(Dispatchers.Main){
                    try {
                        semaphore.withPermit {
                            if(playListener!=null && myExoPlayer!=null){
                                myExoPlayer?.removeListener(playListener!!)
                            }
                            if(myExoPlayer?.isPlaying==true)
                                myExoPlayer?.stop()
                        }
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            }

            Log.d("MonitorEnd", "MonitorEnd")
            return@coroutineScope
        }
    suspend fun generateStreamTextAudio7Last2(inputText: String,dispatcher:CoroutineContext)= coroutineScope {

            audioPlayerIsComplete=false
//            isFinishTalking=false
            stopTheProcess=false
            var playListener:Player.Listener?=null
            var scope=CoroutineScope(dispatcher)
            try {
                scope.launch {
                    var ioDispatcher=Dispatchers.IO
                    try {
                        geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                            override fun onStreamReader(
                                response: String?,
                                indexFlow: Int,
                                lastIndexFlow: Int
                            ) {
                                if (response != null) {
                                    Log.d("response", "$response - $indexFlow")
                                    var text: String? = removeSymbols(response?.trim()!!) ?: return
                                    Log.d("audioBytes", text!!)
                                    CoroutineScope(ioDispatcher).launch {

                                        if (!text.isNullOrEmpty() && containsLetters(text)) {
                                            Log.d("text", "$text")
                                            try {
                                                delay(100)
                                                var audioBytes = queryTextToSpeech(text)
                                                if (audioBytes == null) {
                                                    semaphore.withPermit {
                                                        stopTheProcess = true
                                                        audioPlayerIsComplete = true }
//                                          callBack?.onRequestIsSuccess2(stopExoPlayer)
                                                } else {

                                                    val tempFile: File? = createTempFile(indexFlow, audioBytes!!)
                                                    if (tempFile != null && tempFile?.absolutePath?.isNullOrEmpty() == false) {
                                                        semaphore.withPermit {
                                                            flowAudioFileMap?.put(
                                                                indexFlow,
                                                                tempFile)
                                                            Log.d("flow", "$indexFlow - $lastIndexFlow")
                                                            return@withPermit
                                                        }
                                                    }
                                                }

                                            }
                                            catch (e: IOException) {
                                                semaphore.withPermit { stopTheProcess = true }
                                                //                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                            } catch (e: Exception) {
                                                semaphore.withPermit { stopTheProcess = true }
                                                //                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                            }

                                        }
                                        else if (response?.trim() == ContentApp.END_SYMBOL) {
                                            Log.d("Completed", response!!)
                                            semaphore.withPermit {
                                                audioPlayerIsComplete = true
                                                flowAudioFileMap?.put(indexFlow, ContentApp.END_SYMBOL)
                                            }
                                        }

                                    }
                                }
                            }
                            override fun onStreamComplete(lastFlowIndex: Int) {

                            }
                            override fun onStreamError(e: Throwable) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    semaphore.withPermit {
                                        try {
                                            myExoPlayer?.stop()
                                        } finally {
                                            audioPlayerIsComplete = true
                                            stopTheProcess = true
                                            //                                    callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                        }
                                    }
                                }
                            }
                        })
                    } catch (e: IOException) {
                        stopTheProcess=true
                        e.printStackTrace()
                    } catch (e: Exception) {
                        stopTheProcess=true
                        e.printStackTrace()
                    }
                }
              var jop=scope.launch {
                    println("PlayerAudio")
                    var index=0;
                    delay(1000)
                    while (!stopTheProcess){
//                        println("PlayerAudio:$index")
                        var value = semaphore?.withPermit {
                            if(flowAudioFileMap?.isNullOrEmpty()==false && flowAudioFileMap?.containsKey(index)==true) {
                                return@withPermit flowAudioFileMap?.getValue(index)
                            }
                            return@withPermit null
                        }

                        if(value != null) {
                            if (value is File) {
                                audioPlayerIsComplete=false

                                withContext(Dispatchers.Main) {
                                    semaphore?.withPermit {
                                        val temp:Any?= if(flowAudioFileMap?.containsValue(ContentApp.END_SYMBOL)==true) flowAudioFileMap?.get(index+1) else null
                                        val lastAudio = (temp!=null && temp is String && temp == ContentApp.END_SYMBOL)

                                        playListener = playExoPlayer(
                                            value?.absolutePath!!,
                                            myExoPlayer!!,
                                            lastAudio,
                                            object : ICustomPlayerListener<ExoPlayer> {
                                            @OptIn(UnstableApi::class)
                                                override fun onErrorListener(mp: ExoPlayer?, error:java.lang.Exception) {
                                                CoroutineScope(Dispatchers.Default).launch  {
                                                        semaphore?.withPermit {
                                                            audioPlayerIsComplete=true
//                                                            stopTheProcess=lastAudioClip
                                                          }
                                                        }

                                                    throw  PlaybackException(error?.message!!,error?.cause!!,error?.hashCode()!!)
                                                }
                                                override fun onCompletionListener(mp: ExoPlayer?, lastAudioClip:Boolean) {
                                                    Log.d(
                                                        "onCompletionListener",
                                                        "$lastAudioClip -- $index"
                                                    )

                                                            audioPlayerIsComplete = true
                                                            stopTheProcess = lastAudioClip
                                                        }

                                                })
                                    delay(1000L)

                                    while (myExoPlayer?.isPlaying  == true || !audioPlayerIsComplete) {
                                        delay(500L)
                                    }
                            }
                                }

                            } else if (value is String) {
                                Log.d("onEndTask",value)
                                audioPlayerIsComplete = true
                                semaphore?.withPermit {
                                    stopTheProcess = true
                                }
                                break
                            }
                            index+=1
                        }
//                    delay(500)
                    }
                    Log.d("onEndTask","End")
                    return@launch
                }
                jop?.join()
            }catch (e:IOException){
                e.printStackTrace()
            } catch (e:Exception){
                e.printStackTrace()
            }finally {
                semaphore.withPermit {
                    if(flowAudioFileMap?.isNotEmpty()==true){
                        try {
                            flowAudioFileMap?.forEach { i ->
                                if (i.value is File) {
                                    var file = (i.value as File)
                                    if (file.exists())
                                        file.delete()
                                }
                            }
                        }finally {
                            flowAudioFileMap?.clear()
                        }
                    }
                }
                withContext(Dispatchers.Main){
                    try {
                        semaphore.withPermit {
                            if(playListener!=null && myExoPlayer!=null){
                                myExoPlayer?.removeListener(playListener!!)
                            }
                            if(myExoPlayer?.isPlaying==true)
                                myExoPlayer?.stop()
                        }
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            }

            Log.d("MonitorEnd", "MonitorEnd")
            return@coroutineScope
        }
    @SuppressLint("SuspiciousIndentation")
    fun generateStreamTextAudio7Last3(inputText: String, dispatcher: CoroutineContext, scope:CoroutineScope)=scope.launch(dispatcher){
        audioPlayerIsComplete = false
        stopTheProcess = false
        var playListener: Player.Listener? = null

        try {
            scope.launch(dispatcher) {
                var scopeInternal=CoroutineScope(dispatcher)
                try {
                    geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                        override fun onStreamReader(response: String?, indexFlow: Int, lastIndexFlow: Int) {
                            Log.d("response", "${response?.trim()}")
                            if(response!=null && response?.trim()?.isNotEmpty()==true) {
                                scope.launch(dispatcher) {
//                                    delay(300)
                                    try {
                                        if (response == ContentApp.END_SYMBOL) {
                                            //                                        audioPlayerIsComplete = true
                                            Log.d("END_SYMBOL", "${response.trim()}")
                                            semaphore?.withPermit {
                                                flowAudioFileMap?.put(
                                                    indexFlow,
                                                    ContentApp.END_SYMBOL
                                                )
                                            }
                                        }
                                        else{
                                            val text = removeSymbols(response.trim()) ?: return@launch
                                            Log.d("metaData", "${text}")
                                            if (text?.trim()?.isNotEmpty()==true && containsLetters(text)) {
                                                val audioBytes = secondQueryTextToSpeech(text)
                                                if (audioBytes == null) {
                                                    semaphore?.withPermit { stopTheProcess = true }
                                                } else {
                                                    val tempFile: File? = createTempFile(indexFlow, audioBytes)

                                                    tempFile?.let { flowAudioFileMap?.put(indexFlow, it) }
                                                }

                                            }
                                        }

                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Log.e("onStreamReaderError","Error")
                                        semaphore?.withPermit { stopTheProcess = true }
                                    }
                                }
                            }
                        }
                        override fun onStreamComplete(lastFlowIndex: Int) {}
                        override fun onStreamError(e: Throwable) {
                            scope.launch(Dispatchers.Default) {
                                semaphore?.withPermit { stopTheProcess = true }
                            }
                        }
                    })
                }
                catch (e: Exception) {
                    Log.e("sendMessageStreamError",e.message!!)
                    semaphore?.withPermit { stopTheProcess = true }
//                    scopeInternal?.cancel()
                    e.printStackTrace()
                    return@launch
                }
            }
            var job=scope.launch {
                try {
                    var index = 0
                    delay(1000)
                    while (semaphore?.withPermit { return@withPermit stopTheProcess } == false) {
                        val value = semaphore?.withPermit { flowAudioFileMap?.get(index) }
                        if (value != null) {
                            if (value is File) {
                                audioPlayerIsComplete = false
                                val lastAudio = semaphore?.withPermit {
                                    flowAudioFileMap?.containsValue(ContentApp.END_SYMBOL) == true
                                            && flowAudioFileMap?.get(index + 1) == ContentApp.END_SYMBOL
                                }
                                Log.d("lastAudio", "$lastAudio")
                                withContext(Dispatchers.Main) {
                                  semaphore?.withPermit {
                                      playListener = playExoPlayer(
                                          value.absolutePath,
                                          myExoPlayer!!,
                                          lastAudio ?: false,
                                          object : ICustomPlayerListener<ExoPlayer> {
                                              @OptIn(UnstableApi::class)
                                              override fun onErrorListener(
                                                  mp: ExoPlayer?,
                                                  error: Exception
                                              ) {
                                                  audioPlayerIsComplete = true
                                                  stopTheProcess = true
                                                  error.printStackTrace()
                                                  Log.d("ExoPlayer_ErrorListener", error.message!!)
                                                  this@launch.cancel()
                                              }

                                              override fun onCompletionListener(
                                                  mp: ExoPlayer?,
                                                  lastAudioClip: Boolean
                                              ) {
                                                  Log.d(
                                                      "onCompletionListener",
                                                      "$lastAudioClip -- $index"
                                                  )
                                                  audioPlayerIsComplete = true
                                                  if (lastAudioClip) {
                                                      stopTheProcess = true
                                                      this@launch.cancel()
                                                  }

                                              }
                                          })

                                          delay(1000L)
                                          while (myExoPlayer?.isPlaying == true) {
                                              delay(1000L)
                                          }
                                  }
                                     }

                            } else if (value is String) {
                                Log.d("onEndTask", value)
                                audioPlayerIsComplete = true
                                semaphore?.withPermit { stopTheProcess = true }
                                break
                            }
                            index += 1
                        } else {
                            delay(1000)
                        }
                    }
                }catch (e: Exception) {
                    Log.e("ReadResponseAudio",e.message!!)
                    e.printStackTrace()
                    return@launch
                }
            }
            job.join()

        } finally {
            semaphore?.withPermit {
                flowAudioFileMap?.forEach { (_, value) ->
                    if (value is File && value.exists()) {
                        value.delete()
                    }
                }
                flowAudioFileMap?.clear()
            }
            scope.launch(Dispatchers.Main) {
                try {
                    playListener?.let { myExoPlayer?.removeListener(it) }
                    if (myExoPlayer?.isPlaying == true) myExoPlayer?.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Clean ExoPlayer Listener",e.message!!)
                }
            }
        }
        Log.d("MonitorEnd", "MonitorEnd")
    }
    fun generateStreamTextAudio7Last4(inputText: String, dispatcher: CoroutineContext,scope:CoroutineScope) =
        scope.launch(dispatcher){
            var  audioPlayerIsComplete = false
            var  stopTheProcess = false
            var playListener: Player.Listener? = null
            var job:Job?=null
            try {
                scope.launch(dispatcher) {
                    var scopeInternal=CoroutineScope(Dispatchers.IO+Job())
                    try {
                        geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                            override fun onStreamReader(response: String?, indexFlow: Int, lastIndexFlow: Int) {
                                Log.d("response", "${response?.trim()}")
                                if(response!=null && response?.trim()?.isNotEmpty()==true) {
                                    scope.launch(dispatcher) {

                                        try {
                                            if (response == ContentApp.END_SYMBOL) {
                                                //                                        audioPlayerIsComplete = true
                                                Log.d("END_SYMBOL", "${response.trim()}")
                                                semaphore?.withPermit {
                                                    flowAudioFileMap?.put(
                                                        indexFlow,
                                                        ContentApp.END_SYMBOL
                                                    )
                                                }
                                            }
                                            else{
                                                val text = removeSymbols(response.trim()) ?: return@launch
                                                Log.d("metaData", "${text}")
                                                if (text?.trim()?.isNotEmpty()==true && containsLetters(text)) {
                                                    val audioBytes = secondQueryTextToSpeech(text)
                                                    if (audioBytes == null) {
                                                        semaphore?.withPermit { stopTheProcess = true }
                                                    } else {
                                                        val tempFile: File? = createTempFile(indexFlow, audioBytes)
                                                        tempFile?.let { flowAudioFileMap?.put(indexFlow, it) }
                                                    }

                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Log.e("onStreamReaderError","Error")
                                            semaphore?.withPermit { stopTheProcess = true }
                                        }
                                    }
                                }
                            }
                            override fun onStreamComplete(lastFlowIndex: Int) {}
                            override fun onStreamError(e: Throwable) {
                                scope.launch {
                                    semaphore?.withPermit { stopTheProcess = true }
                                }
                            }
                        })
                    }
                    catch (e: Exception) {
                        Log.e("sendMessageStreamError",e.message!!)
                        semaphore?.withPermit { stopTheProcess = true }
                        e.printStackTrace()
                        return@launch
                    }
                }
                delay(1000)
                job = CoroutineScope(Dispatchers.Default+Job()).async {
                    return@async try {
                        var index = 0
                        while (isActive && semaphore?.withPermit { return@withPermit stopTheProcess } == false) {
//                     ensureActive()
                            val value = semaphore?.withPermit { flowAudioFileMap?.get(index) }
                            if (value != null) {
                                if (value is File) {

                                    val lastAudio = semaphore?.withPermit {
                                        flowAudioFileMap?.containsValue(ContentApp.END_SYMBOL) == true
                                                && flowAudioFileMap?.get(index + 1) == ContentApp.END_SYMBOL
                                    }
                                    Log.d("lastAudio", "$lastAudio")
                                    withContext(Dispatchers.Main) {
                                        audioPlayerIsComplete = false
                                        exoPlayer?.playMedia(value.absolutePath, lastAudio ?: false,
                                            object : ICustomPlayerListener<ExoPlayer> {
                                                @OptIn(UnstableApi::class)
                                                override fun onErrorListener(
                                                    mp: ExoPlayer?,
                                                    error: Exception
                                                ) {
                                                    audioPlayerIsComplete = true
                                                    stopTheProcess = true
                                                    error.printStackTrace()
                                                    Log.d(
                                                        "ExoPlayer_ErrorListener",
                                                        error.message!!
                                                    )
                                                    this@async.cancel()
                                                    //                                                  throw PlaybackException(
                                                    //                                                      error.message!!,
                                                    //                                                      error.cause!!,
                                                    //                                                      error.hashCode()
                                                    //                                                  )
                                                }

                                                override fun onCompletionListener(
                                                    mp: ExoPlayer?,
                                                    lastAudioClip: Boolean
                                                ) {
                                                    Log.d(
                                                        "onCompletionListener",
                                                        "$lastAudioClip -- $index"
                                                    )

                                                    audioPlayerIsComplete = true
                                                    if (lastAudioClip) {
                                                        stopTheProcess = true
                                                        this@async.cancel()
                                                    }

                                                    }


                                            })
//                                var playListener = playExoPlayer(
//                                      value.absolutePath,
//                                      myExoPlayer!!,
//                                      lastAudio ?: false,
//                                      object : ICustomPlayerListener<ExoPlayer> {
//                                          @OptIn(UnstableApi::class)
//                                          override fun onErrorListener(
//                                              mp: ExoPlayer?,
//                                              error: Exception
//                                          ) {
//                                              audioPlayerIsComplete = true
//                                              stopTheProcess = true
//                                              error.printStackTrace()
//                                              Log.d("ExoPlayer_ErrorListener", error.message!!)
//                                              this@launch.cancel()
////                                                  throw PlaybackException(
////                                                      error.message!!,
////                                                      error.cause!!,
////                                                      error.hashCode()
////                                                  )
//                                          }
//
//                                          override fun onCompletionListener(
//                                              mp: ExoPlayer?,
//                                              lastAudioClip: Boolean
//                                          ) {
//                                              Log.d(
//                                                  "onCompletionListener",
//                                                  "$lastAudioClip -- $index"
//                                              )
//
//                                              audioPlayerIsComplete = true
//                                              if (lastAudioClip) {
//                                                  scope.launch {
//                                                      semaphore.withPermit { stopTheProcess = true }
//                                                  }
//                                                  this@launch.cancel()
//                                              }
//
//                                          }
//                                      })

                                        delay(500L)
                                        while (exoPlayer?.isPlayer() == true || !audioPlayerIsComplete) {
                                            delay(1000L)
                                        }
//                                        playListener?.let { myExoPlayer?.removeListener(it) }
//                                        return@withContext
//                                    }
                                }
                                } else if (value is String) {
                                    Log.d("onEndTask", value)
                                    audioPlayerIsComplete = true
                                    semaphore?.withPermit { stopTheProcess = true }
                                    break
                                }
                                index += 1
//                          delay(500)
                            }
                            else {
                                delay(1000)
                            }
                        }
                    }catch (e: Exception) {
                        Log.e("ReadResponseAudio",e.message!!)
                        e.printStackTrace()
                    }
                }
                job?.await()
            }
            catch (e:java.util.concurrent.CancellationException){
                Log.e("CancellationException","CancellationScope")
            }
            finally {
                try{
                    if(job?.isActive==true)
                        job?.cancelAndJoin()
                } finally {
                    semaphore?.withPermit {
                        flowAudioFileMap?.forEach { (_, value) ->
                            if (value is File && value.exists()) {
                                value.delete()
                            }
                        }
                        flowAudioFileMap?.clear()
                    }
                    scope.launch(Dispatchers.Main) {
                        try {
                            if (exoPlayer?.isPlayer() == true) {
                                exoPlayer?.stop()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("Clean ExoPlayer Listener",e.message!!)
                        }
                    }
                }
                return@launch
            }
            Log.d("MonitorEnd", "MonitorEnd")
        }
    suspend fun generateStreamTextAudio7Last5(inputText: String, dispatcher: CoroutineContext,scope:CoroutineScope)  {


        var job:Job?=null;
        try{
            scope.launch(dispatcher) {
                val responseFlow = geminiApiClient?.sendMessageStreamLastTest2(inputText)
                responseFlow?.let {
//               it.toList().forEach({ item ->
//                   Log.d("ResponseFlow - ${item.index}", item.text)
//               })
                    it.onCompletion { cause ->
                        if (cause != null) {
                            println("Flow completed with error: ${cause.message}")
                        }
                        else {
                            println("Flow completed successfully")
                        }
                    }
                        .collect { response ->
                            scope.launch {
                                Log.d("ResponseFlow - ${response.index}",response.text)
                                processResponse(response.text,response.index)
                            }
                        }
                }
            }
//            job=startPlayer(scope)
//            job?.join()
        }
        finally {
            try{
                if(job?.isActive==true)
                    job?.cancelAndJoin()
            }
            finally {
                semaphore?.withPermit {
                    flowAudioFileMap?.forEach { (_, value) ->
                        if (value is File && value.exists()) {
                            value.delete()
                        }
                    }
                    flowAudioFileMap?.clear()
                }
                scope.launch(Dispatchers.Main) {
                    try {
                        if (exoPlayer?.isPlayer() == true) {
                            exoPlayer?.stop()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("Clean ExoPlayer Listener",e.message!!)
                    }
                }
            }
        }

    }
   suspend fun generateStreamTextAudio7Last6(inputText: String, scope:CoroutineScope)
   : MutableSharedFlow<SpeechChatBotState>  {
        semaphore?.withPermit {
            stopTheProcess=false
            flowAudioFileMap?.clear()
        }

        val stateFlow = MutableStateFlow<SpeechChatBotState>(SpeechChatBotState.Initial)
        var job:Job?=null;
        try{

            var jop=scope.launch {
                val responseFlow = geminiApiClient?.sendMessageStreamLastTest2(inputText)
                responseFlow?.let {
                    it.onCompletion { cause ->
                        if (cause != null) {
                            println("Flow completed with error: ${cause.message}")
                        }
                        else {
                            println("Flow completed successfully")
                        }
                    }.collect { response ->
                        launch {
//                                Log.d("ResponseFlow - ${response.index}",response.text)
//                            try {

                                if (response.text == ContentApp.END_SYMBOL) {

                                    Log.d("END_SYMBOL", "${response.text.trim()} - ${response.index}")
                                    semaphore?.withPermit {
                                        flowAudioFileMap?.put(
                                            response.index,
                                            ContentApp.END_SYMBOL
                                        )
                                    }

                                } else {

                                    val text = removeSymbols(response.text.trim()) ?: return@launch
                                    Log.d("metaData", "${text}")
                                    if (text?.trim()
                                            ?.isNotEmpty() == true && containsLetters(text)
                                    ) {
                                        val audioBytes = secondQueryTextToSpeech(text)
                                        if (audioBytes == null) {
                                            semaphore?.withPermit { stopTheProcess = true }
                                            this@launch.cancel(CancellationException(""))
                                        } else {
                                            val tempFile: File? =
                                                createTempFile(response.index, audioBytes)
                                            tempFile?.let {
                                                flowAudioFileMap?.put(response.index, it)
                                            }
                                            Log.e("createTempFile", "${response.index}")
                                            stateFlow.emit(SpeechChatBotState.Listening)
                                        }

                                    }
                                }
//                            }
//                            catch (e: Exception) {
//                                e.printStackTrace()
//                                Log.e("onStreamReaderError", e.message!!)
//                                 throw  CancellationException(e.message!!);
////                                semaphore?.withPermit { stopTheProcess = true }
////                                scope?.cancel()
////                                stateFlow.emit(SpeechChatBotState.Error(e.message!!))
//                            }
                            }
//                            .invokeOnCompletion {it->
//                            Log.e("invokeOnCompletion", "invokeOnCompletion")
//                            if(it?.cause!=null){
//                                Log.e("invokeOnCompletion", "is Canceld")
//                                this@launch.cancel()
//                            }
//                        }
                        }
                }
            }
            delay(1000)
            scope.launch {
                var audioPlayerIsComplete=false
                var index=0
                var count=0;
                try {
                    while (scope?.isActive == true && (count<10 && semaphore?.withPermit { stopTheProcess } == false)) {
                       Log.d("value", "$index")

                        var value = semaphore?.withPermit {
                            if (flowAudioFileMap?.isNullOrEmpty()!!) null else flowAudioFileMap?.get(
                                index
                            )
                        }

                        if (value != null) {
                            if (value is File) {

                                val lastAudio = semaphore?.withPermit {
                                    flowAudioFileMap?.containsValue(ContentApp.END_SYMBOL) == true
                                            && flowAudioFileMap?.get(index + 1) == ContentApp.END_SYMBOL
                                }
                                Log.d("lastAudio", "$lastAudio")

                                withContext(Dispatchers.Main) {
                                    exoPlayer?.playMedia(value.absolutePath, lastAudio ?: false,
                                        object : ICustomPlayerListener<ExoPlayer> {
                                            @OptIn(UnstableApi::class)
                                            override fun onErrorListener(
                                                mp: ExoPlayer?,
                                                error: java.lang.Exception
                                            ) {

                                                audioPlayerIsComplete = true
                                                stopTheProcess = true
                                                error.printStackTrace()
                                                Log.d("ExoPlayer_ErrorListener3", error.message!!)
                                                this@launch.cancel()
    //                                  stateFlow.emit(PlayerSpeechState.Error(error.message!!))
    //                                       throw PlaybackException(error.message!!, error.cause!!, error.hashCode())
                                            }

                                            override fun onCompletionListener(
                                                mp: ExoPlayer?,
                                                isTheLastAudioClip: Boolean
                                            ) {
                                                Log.d(
                                                    "onCompletionListener3",
                                                    "$isTheLastAudioClip -- $index"
                                                )
                                                audioPlayerIsComplete = true
                                                if (isTheLastAudioClip) {
    //                                          stateFlow.emit(SpeechChatBotState.Completed)

                                                    stopTheProcess = true
//                                                    runBlocking {
//                                                        semaphore.withPermit { stopTheProcess = true }
//                                                    }
                                                    this@launch.cancel()

                                                }
                                            }
                                        })

                                    delay(1000L)
                                    while (scope?.isActive == true && (exoPlayer?.isPlayer() == true )) {
                                        delay(1000L)
                                    }
                                }

                            } else if (value is String) {
                                Log.d("onEndTask", value)
                                audioPlayerIsComplete = true
                                break
                            }
                            index += 1

                        } else {
                            count++
                            delay(1000)
                        }
                    }
                }catch (e:CancellationException) {
                    clearFlowAudioFileMap()
                    stateFlow.emit(SpeechChatBotState.Completed)
                }
            }.invokeOnCompletion {

              scope?.launch{
                  clearFlowAudioFileMap()
                 try{
                     jop?.cancelAndJoin()
                 }finally{
                     stateFlow.emit(SpeechChatBotState.Completed)
                 }

              }
            }


//                delay(1000)
//                startPlayer2(scope)
////                state_flow?.collect { state ->
////                    when (state) {
////                        PlayerSpeechState.Canceled -> {
////                            stateFlow.emit(SpeechChatBotState.Completed)
////                        }
////                        PlayerSpeechState.Completed -> {
////                            startPlayer2()
////                        }
////                        else -> {
////
////                        }
////                    }
////
////                }
//            }
        }
        catch (e:CancellationException) {
            clearFlowAudioFileMap()
          stateFlow.emit(SpeechChatBotState.Completed)
        } catch (e:Exception) {
            clearFlowAudioFileMap()
            stateFlow.emit(SpeechChatBotState.Error(e.message!!))


//            try{
////                if(job?.isActive==true)
////                    job?.cancelAndJoin()
//            }
//            finally {
////                semaphore?.withPermit {
//////                    flowAudioFileMap?.forEach { (_, value) ->
//////                        if (value is File && value.exists()) {
//////                            value.delete()
//////                        }
//////                    }
////                    flowAudioFileMap?.clear()
////                }
////                scope.launch(Dispatchers.Main) {
////                    try {
////                        if (exoPlayer?.isPlayer() == true) {
////                            exoPlayer?.stop()
////                        }
////                    } catch (e: Exception) {
////                        e.printStackTrace()
////                        Log.e("Clean ExoPlayer Listener",e.message!!)
////                    }
////                }
//            }
        }
            return  stateFlow;
    }
    suspend fun generateStreamTextAudio7Last7(inputText: String, scope:CoroutineScope) {

        semaphore?.withPermit {
            stopTheProcess=false
            flowAudioFileMap?.clear()
        }

        var job:Job?=null;
        var jobStream:Job?=null;
        try{
            jobStream=scope.launch {
                val responseFlow = geminiApiClient?.sendMessageStreamLastTest2(inputText)
                responseFlow?.let {
//                    it.onCompletion { cause ->
//                        if (cause != null) {
//                            println("Flow completed with error: ${cause.message}")
//                        }
//                        else {
//                            println("Flow completed successfully")
//                        }
//                    }
                        it.collect { response ->
                        launch {
//                                Log.d("ResponseFlow - ${response.index}",response.text)
                          try {

                                if (response.text == ContentApp.END_SYMBOL) {
                                Log.d("END_SYMBOL", "${response.text.trim()} - ${response.index}")
                                semaphore?.withPermit {
                                    flowAudioFileMap?.put(
                                        response.index,
                                        ContentApp.END_SYMBOL
                                    )
                                }

                            } else {

                                val text = removeSymbols(response.text.trim()) ?: return@launch
                                Log.d("metaData", "${text}")
                                if (text?.trim()
                                        ?.isNotEmpty() == true && containsLetters(text)
                                ) {
                                    val audioBytes = secondQueryTextToSpeech(text)
                                    if (audioBytes != null) {
//                                        semaphore?.withPermit { stopTheProcess = true }
//                                        this@launch.cancel(CancellationException(""))
//                                    } else {
                                        val tempFile: File? = createTempFile(response.index, audioBytes)
                                        tempFile?.let { flowAudioFileMap?.put(response.index, it) }
                                        Log.e("createTempFile", "${response.index}")
//                                        stateFlow.emit(SpeechChatBotState.Listening)
                                    }

                                }
                            }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("onStreamReaderError", e.message!!)
//                                 throw  CancellationException(e.message!!);
                                semaphore?.withPermit { stopTheProcess = true }
//                                scope?.cancel()
//                                stateFlow.emit(SpeechChatBotState.Error(e.message!!))
                            }
                        }
//                            .invokeOnCompletion {it->
//                            Log.e("invokeOnCompletion", "invokeOnCompletion")
//                            if(it?.cause!=null){
//                                Log.e("invokeOnCompletion", "is Canceld")
//                                this@launch.cancel()
//                            }
//                        }
                    }
                }
            }
            var job=scope.launch {
                var audioPlayerIsComplete=false
                var index=0
                var count=0;

                delay(1000)

                try {
                    Log.d("stopTheProcess","${stopTheProcess!!}")
                    while (isActive == true && (count<20 && semaphore?.withPermit { stopTheProcess } == false)) {
                        Log.d("value", "$index")

                        var value = semaphore?.withPermit {
                            if (flowAudioFileMap?.isNullOrEmpty()!!) null else flowAudioFileMap?.get(index)
                        }

                        if (value != null) {
                            count=0
                            if (value is File) {

                                val lastAudio = semaphore?.withPermit {
                                    flowAudioFileMap?.containsValue(ContentApp.END_SYMBOL) == true
                                            && flowAudioFileMap?.get(index + 1) == ContentApp.END_SYMBOL
                                }
                                Log.d("lastAudio", "$lastAudio")
                                withContext(Dispatchers.Main) {
                                    exoPlayer?.playMedia(value.absolutePath, lastAudio ?: false,
                                            object : ICustomPlayerListener<ExoPlayer> {
                                                @OptIn(UnstableApi::class)
                                                override fun onErrorListener(
                                                    mp: ExoPlayer?,
                                                    error: java.lang.Exception
                                                ) {

                                                    audioPlayerIsComplete = true
                                                    stopTheProcess = true
                                                    error.printStackTrace()
                                                    Log.d(
                                                        "ExoPlayer_ErrorListener3",
                                                        error.message!!
                                                    )
                                                    this@launch.cancel()
                                                    //                                  stateFlow.emit(PlayerSpeechState.Error(error.message!!))
                                                    //                                       throw PlaybackException(error.message!!, error.cause!!, error.hashCode())
                                                }

                                                override fun onCompletionListener(
                                                    mp: ExoPlayer?,
                                                    isTheLastAudioClip: Boolean
                                                ) {
                                                    Log.d(
                                                        "onCompletionListener3",
                                                        "$isTheLastAudioClip -- $index"
                                                    )
                                                    audioPlayerIsComplete = true
                                                    if (isTheLastAudioClip) {
                                                        stopTheProcess = true
                                                        this@launch.cancel()
                                                    }
                                                }
                                            })
                                    delay(1000L)
                                    while (isActive && exoPlayer?.isPlayer() == true) {
                                        delay(1000L)
                                    }
                                }

                            } else if (value is String) {
                                Log.d("onEndTask", value)
                                audioPlayerIsComplete = true
                                break
                            }
                            index += 1

                        } else {
                            count++
                            Log.d("count", "$count")
                            delay(1000)
                        }
                    }
                }catch (e:CancellationException) {
                    e.printStackTrace()
                }
            }
            job.join()
        } finally {
            clearFlowAudioFileMap()
            if(job?.isActive==true)
                job?.cancelAndJoin()
            if(jobStream?.isActive==true)
                jobStream?.cancelAndJoin()
        }
    }
    private  suspend fun  clearFlowAudioFileMap(){

    semaphore?.withPermit {
        flowAudioFileMap?.forEach { (_, value) ->
            if (value is File && value.exists()) {
                value.delete()
            }
        }
        flowAudioFileMap?.clear()
    }
}
    private suspend fun processResponse(response:String, indexFlow:Int){
        try {
            if (response == ContentApp.END_SYMBOL) {
                //                                        audioPlayerIsComplete = true
                Log.d("END_SYMBOL", "${response.trim()} - $indexFlow")
                semaphore?.withPermit {
                    flowAudioFileMap?.put(
                        indexFlow,
                        ContentApp.END_SYMBOL
                    )
                }
            } else{
                val text = removeSymbols(response.trim()) ?: return
                Log.d("metaData", "${text}")
                if (text?.trim()?.isNotEmpty()==true && containsLetters(text)) {
                    val audioBytes = secondQueryTextToSpeech(text)
                    if (audioBytes == null) {
                        semaphore?.withPermit { stopTheProcess = true }
                    } else {
                        val tempFile: File? = createTempFile(indexFlow, audioBytes)
                        tempFile?.let { flowAudioFileMap?.put(indexFlow, it) }
                        Log.e("createTempFile","$indexFlow")
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("onStreamReaderError","Error")
            semaphore?.withPermit { stopTheProcess = true }
        }
    }
    private suspend fun secondQueryTextToSpeech(input: String): ByteArray? {
        val apiUrl = API_URL
        val authorization = AUTHORIZATION
        val maxRetries = 3
        var currentAttempt = 0
        while (currentAttempt < maxRetries) {
            try {
                currentAttempt++

                val url = URL(apiUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", authorization)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val jsonInputString = "{\"inputs\": \"$input\"}"

//                conn.outputStream.use { os ->
//                    OutputStreamWriter(os, "UTF-8").use { writer ->
//                        writer.write(jsonInputString)
//                        writer.flush()
//                    }
//                }


                conn.outputStream.use { os ->
                    val inputBytes = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(inputBytes, 0, inputBytes.size)
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return conn.inputStream.use { it.readBytes() }
                } else {
                    Log.e("WasmApiAudio", "Failed with HTTP response code: $responseCode")
                    delay(1000)
                }
            } catch (e: Exception) {
                Log.e("WasmApiAudio", "An error occurred: ${e.message}")
                if (e.message?.contains("SAFETY") == true) {
                    Log.e("WasmApiAudio", "Content generation stopped due to safety reasons.")
                }else if (currentAttempt >= maxRetries) {
                    e.printStackTrace()
                    return null
                } else {
                    Log.w("WasmApiAudio", "Retrying... Attempt $currentAttempt")
                    delay(500)
                }
            }
        }
        return null
    }
    private suspend  fun  startPlayer2(scope: CoroutineScope) {
        var  audioPlayerIsComplete=false
        var index=0

//           try {
               while (scope?.isActive == true
                   && semaphore?.withPermit { return@withPermit stopTheProcess } == false) {
//                   Log.d("value", "$index")

                   var value = semaphore?.withPermit { flowAudioFileMap?.get(index) }

                   Log.d("value", "$value")
                   if (value != null) {
                       if (value is File) {

                           val lastAudio = semaphore?.withPermit {
                               flowAudioFileMap?.containsValue(ContentApp.END_SYMBOL) == true
                                       && flowAudioFileMap?.get(index + 1) == ContentApp.END_SYMBOL
                           }
                           Log.d("lastAudio", "$lastAudio")

                           withContext(Dispatchers.Main) {
                               exoPlayer?.playMedia(value.absolutePath, lastAudio ?: false,
                                   object : ICustomPlayerListener<ExoPlayer> {
                                       @OptIn(UnstableApi::class)
                                       override fun onErrorListener(
                                           mp: ExoPlayer?,
                                           error: java.lang.Exception
                                       ) {

                                           audioPlayerIsComplete = true
                                           stopTheProcess = true
                                           error.printStackTrace()
                                           Log.d("ExoPlayer_ErrorListener", error.message!!)
                                           scope?.cancel()
//                                  stateFlow.emit(PlayerSpeechState.Error(error.message!!))
//                                       throw PlaybackException(error.message!!, error.cause!!, error.hashCode())
                                       }

                                       override fun onCompletionListener(
                                           mp: ExoPlayer?,
                                           isTheLastAudioClip: Boolean
                                       ) {
                                           Log.d(
                                               "onCompletionListener",
                                               "$isTheLastAudioClip -- $index"
                                           )
                                           audioPlayerIsComplete = true
                                           if (isTheLastAudioClip) {
//                                          stateFlow.emit(PlayerSpeechState.Completed)

//                                               stopTheProcess = true
                                               scope?.launch { semaphore.withPermit { stopTheProcess = true } }
//                                          stateFlow.emit(PlayerSpeechState.Canceled)
                                               scope?.cancel()


                                           }
                                       }
                                   })

                               delay(1000L)
                               while (scope?.isActive == true && (exoPlayer?.isPlayer() == true || !audioPlayerIsComplete)) {
                                   delay(1000L)
                               }
                           }

                       } else if (value is String) {
                           Log.d("onEndTask", value)
                           audioPlayerIsComplete = true
                           semaphore?.withPermit { stopTheProcess = true }
                       }

                       index += 1


//               }
//           }catch (e:CancellationException){
//               e.printStackTrace()
//               this@flow.emit(SpeechChatBotState.Completed)
//           }catch (e:Exception){
//               e.printStackTrace()
//               this@flow.emit(SpeechChatBotState.Error(e.message!!))
                   }
                   else{
                       delay(1000)
                   }
               }
//        return  stateFlow
    }
    private suspend fun  startPlayer(scope: CoroutineScope) {
            var  audioPlayerIsComplete=false

//           try {
//               while (scope?.isActive == true && semaphore?.withPermit { return@withPermit stopTheProcess } == false) {
//                   val value = semaphore?.withPermit { flowAudioFileMap?.get(index) }
//                   if (value != null) {
//                       if (value is File) {
//
//                           val lastAudio = semaphore?.withPermit {
//                               flowAudioFileMap?.containsValue(ContentApp.END_SYMBOL) == true
//                                       && flowAudioFileMap?.get(index + 1) == ContentApp.END_SYMBOL
//                           }
//                           Log.d("lastAudio", "$lastAudio")
//
//                           exoPlayer?.playMedia(value.absolutePath, lastAudio ?: false,
//                               object : ICustomPlayerListener<ExoPlayer> {
//                                   @OptIn(UnstableApi::class)
//                                   override fun onErrorListener(mp: ExoPlayer?, error: Exception) {
//                                       audioPlayerIsComplete = true
//                                       stopTheProcess = true
//                                       error.printStackTrace()
//                                       Log.d("ExoPlayer_ErrorListener", error.message!!)
//                                       scope?.cancel()
////                                       throw PlaybackException(error.message!!, error.cause!!, error.hashCode())
//                                   }
//                                   override fun onCompletionListener(mp: ExoPlayer?, lastAudioClip: Boolean) {
//                                       Log.d("onCompletionListener", "$lastAudioClip -- $index")
//                                       audioPlayerIsComplete = true
//                                       if (!lastAudioClip) {
//                                           startPlayer(index+1,scope)
//                                       } else {
//                                           stopTheProcess = true
//                                            //  CoroutineScope().launch { semaphore.withPermit { stopTheProcess = true } }
//                                           scope?.cancel()
//                                       }
//
//                                   }
//                               })
//                           delay(500L)
////                           while (scope?.isActive == true && (exoPlayer?.isPlayer() == true || !audioPlayerIsComplete)) {
////                               delay(1000L)
////                           }
//                       } else if (value is String) {
//                           Log.d("onEndTask", value)
//                           audioPlayerIsComplete = true
////                           semaphore?.withPermit { stopTheProcess = true }
//
//                       }
////                       index += 1
//                   } else {
//                       delay(1000)
//                   }
//               }
//           }catch (e:CancellationException){
//               e.printStackTrace()
//               this@flow.emit(SpeechChatBotState.Completed)
//           }catch (e:Exception){
//               e.printStackTrace()
//               this@flow.emit(SpeechChatBotState.Error(e.message!!))
//           }
        }

    private suspend fun createTempFile(flow_index:Int?=null, audioBytes:ByteArray):File?= withContext(Dispatchers.IO){
        try {
            var tempFile = File.createTempFile("stream_temp_audio"+(flow_index?:""), ".wav", context.cacheDir)
            tempFile.deleteOnExit();
            val fos = FileOutputStream(tempFile)
            fos.write(audioBytes)
            fos.close()
            return@withContext tempFile
        }catch (e:Exception){
            e.printStackTrace()
            return@withContext null
        }

    }
    fun onDestroy(){
         if(myExoPlayer!=null)
            myExoPlayer?.release()
     }
    suspend fun generateStreamTextAudio8Last(inputText: String,dispatcher: CoroutineContext){
        var scope=CoroutineScope(dispatcher)
            audioPlayerIsComplete=false
            stopTheProcess=false
            var playListener:Player.Listener?=null

            try {

        scope.launch {
                    var ioDispatcher=Dispatchers.IO
                    try {
                        geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                            override fun onStreamReader(
                                response: String?,
                                indexFlow: Int,
                                lastIndexFlow: Int
                            ) {
                                if (response != null) {
                                    Log.d("response", "$response - $indexFlow")
                                    var text: String? = removeSymbols(response?.trim()!!) ?: return
                                    Log.d("audioBytes", text!!)
                                    CoroutineScope(ioDispatcher).launch {

                                        if (!text.isNullOrEmpty() && containsLetters(text)) {
                                            Log.d("text", "$text")
                                            try {
                                                delay(100)
                                                var audioBytes = queryTextToSpeech(text)
                                                if (audioBytes == null) {
                                                    semaphore.withPermit {
                                                        stopTheProcess = true
                                                        audioPlayerIsComplete = true }
//                                          callBack?.onRequestIsSuccess2(stopExoPlayer)
                                                }
                                                else {
                                                    val tempFile: File? = createTempFile(indexFlow, audioBytes!!)
                                                    if (tempFile != null && tempFile?.absolutePath?.isNullOrEmpty() == false) {
                                                        semaphore.withPermit {
                                                            flowAudioFileMap?.put(
                                                                indexFlow,
                                                                tempFile)
                                                            Log.d("flow", "$indexFlow - $lastIndexFlow")
                                                            return@withPermit
                                                        }
                                                    }
                                                }

                                            } catch (e: IOException) {
                                                Log.e("IOException", e.message!!)
                                                semaphore.withPermit { stopTheProcess = true }
                                                //                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                            } catch (e: Exception) {
                                                Log.e("Exception", e.message!!)
                                                semaphore.withPermit { stopTheProcess = true }
                                                //                                                callBack?.onRequestIsSuccess2(stopExoPlayer)
                                            }

                                        } else if (response?.trim() == ContentApp.END_SYMBOL) {
                                            Log.d("Completed", response!!)
                                            semaphore.withPermit {
                                                audioPlayerIsComplete = true
                                                flowAudioFileMap?.put(indexFlow, ContentApp.END_SYMBOL)
                                            }
                                        }

                                    }
                                }
                            }
                            override fun onStreamComplete(lastFlowIndex: Int) {

                            }
                            override fun onStreamError(e: Throwable) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    semaphore.withPermit {
                                        try {
                                            myExoPlayer?.stop()
                                        } finally {
                                            audioPlayerIsComplete = true
                                            stopTheProcess = true
                                            //                                    callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                        }
                                    }
                                }
                            }
                        })
                    } catch (e: IOException) {
                        stopTheProcess=true
                        e.printStackTrace()
                    } catch (e: Exception) {
                        stopTheProcess=true
                        e.printStackTrace()
                    }
                }
//                var jop = CoroutineScope(Dispatchers.IO).

            }catch (e:IOException){
                e.printStackTrace()
            } catch (e:Exception){
                e.printStackTrace()
            }finally {
                semaphore.withPermit {
                    if(flowAudioFileMap?.isNotEmpty()==true){
                        try {
                            flowAudioFileMap?.forEach { i ->
                                if (i.value is File) {
                                    var file = (i.value as File)
                                    if (file.exists())
                                        file.delete()
                                }
                            }
                        }finally {
                            flowAudioFileMap?.clear()
                        }
                    }
                }
                withContext(Dispatchers.Main){
                    try {
                        semaphore.withPermit {
                            if(playListener!=null && myExoPlayer!=null){
                                myExoPlayer?.removeListener(playListener!!)
                            }
                            if(myExoPlayer?.isPlaying==true)
                                myExoPlayer?.stop()
                        }
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            }

            Log.d("MonitorEnd", "MonitorEnd")

        }
    private suspend fun handleAudioBytes(audioBytes: ByteArray?, callBack: IWasmServiceEventListener?) {
//        semaphore.withPermit {
//            audioPlayerIsComplete = false;
//        }
        callBack?.stopListener()

        Log.d("handleAudioBytes", "$audioPlayerIsComplete")
        try {
            if (audioBytes == null || audioBytes!!.isEmpty()) {
                throw Exception()
            }
            val filePath = saveIntoTempFile(audioBytes)
            if (filePath != null) {
                Log.d("filePath", filePath)

                playAudio2(filePath,myExoPlayer!!,semaphore, object : ICustomPlayerListener<ExoPlayer> {
                    override fun onErrorListener(mp: ExoPlayer?) {
                        runBlocking {
                            semaphore.withPermit {
                                stopTheProcess=true
                            }
                        }
//                        callBack?.onRequestIsSuccess2(stopExoPlayer)
                    }
                    override fun onCompletionListener(mp: ExoPlayer?) {
                                    Log.d("onCompletionListener5","$audioPlayerIsComplete")
                                    if (audioPlayerIsComplete) {
                                        stopTheProcess=true
                                    }

//                                        callBack?.onRequestIsSuccess2(stopExoPlayer)
                                    }

                })

//                exoPlayer?.start(filePath, object : ICustomPlayerListener<ExoPlayer> {
//                        override fun onErrorListener(mp: ExoPlayer?) {
//                            try { exoPlayer?.stop() }
//                            finally {
//                                CoroutineScope(Dispatchers.Main).launch {
//                                    semaphore.withPermit {
//                                        if (audioPlayerIsComplete) {
//                                            callBack?.onRequestIsSuccess2(stopMediaPlayer)
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                        override fun onCompletionListener(mp: ExoPlayer?) {
//
//                            try {
//                                    CoroutineScope(Dispatchers.Main).launch {
//                                        semaphore.withPermit {
//                                          exoPlayer?.stop()
//                                        }
//                                    }
//                                } finally {
//                                        if (audioPlayerIsComplete) {
//                                            callBack?.onRequestIsSuccess2(stopMediaPlayer)
//                                        }
//                                    }
//                                }
//                    })


            } else {
                throw Exception()
            }
//        } catch (e:PlaybackException) {
//            e.printStackTrace()
//            callBack?.onRequestIsFailure("ERROR: ${e.message}")
//        }catch (e:EmptyStackException) {
//            e.printStackTrace()
////            callBack?.onRequestIsFailure("ERROR: ${e.message}")
//        }catch (e:FileNotFoundException) {
//            e.printStackTrace()
//            callBack?.onRequestIsFailure("ERROR: ${e.message}")
//        }
        }catch (e:Exception) {
            e.printStackTrace()
            throw  Exception(e)
//            callBack?.onRequestIsSuccess2(null)
//            callBack?.onRequestIsFailure("ERROR:: ${e.message}")
        }
    }
    fun generateStreamTextAudio3(inputText: String, callBack: IWasmServiceEventListener) {
        val tempFile = File.createTempFile("stream_temp_audio", ".Wav")
        val fileOutputStream = FileOutputStream(tempFile)
        var isFirstRate=true;
        val player = initializePlayer(context, tempFile)
        player.playWhenReady = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {

                    override fun onStreamReader(response: String?,lastFlow:Boolean) {

                        response?.let {
                            val text = removeSymbols(it)?.trim()?.replace("*", "")
                            if (!text.isNullOrEmpty() && containsLetters(text)) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    semaphore.withPermit {
//                                        Log.d("audioBytes", response)
                                        val audioBytes = queryTextToSpeech(text)
                                        if (audioBytes != null) {
                                            fileOutputStream.write(audioBytes)
                                            fileOutputStream.flush()
                                            if(isFirstRate) {
                                                isFirstRate=false
//                                                CoroutineScope(Dispatchers.Main).launch {
//                                                    exoPlayer?.start(
//                                                        tempFile.path,
//                                                        object : ICustomPlayerListener<ExoPlayer> {
//                                                            override fun onErrorListener(mp: ExoPlayer?) {
//                                                                try {
//                                                                    exoPlayer?.stop()
//                                                                } finally {
//                                                                    latch.countDown()
//                                                                }
//                                                            }
//
//                                                            override fun onCompletionListener(mp: ExoPlayer?) {
//                                                                try {
//                                                                    exoPlayer?.stop()
//                                                                } finally {
//                                                                    latch.countDown()
//                                                                }
//                                                            }
//                                                        })
//                                                }
                                            }
                                        } else {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                callBack.onRequestIsFailure("Failed to get audio bytes")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    override fun onStreamComplete(lastFlowIndex:Int) {
                        try {
                            latch.await()
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        } finally {
                            fileOutputStream.close()
                            CoroutineScope(Dispatchers.Main).launch {
                                try { exoPlayer?.stop() } finally {
                                    callBack?.onRequestIsSuccess("")
                                    tempFile.delete() // حذف الملف المؤقت بعد الانتهاء
                                }
                            }
                        }
                    }
                    override fun onStreamError(e: Throwable) {
                        CoroutineScope(Dispatchers.Main).launch {
                            callBack.onRequestIsFailure(e.message ?: "Unknown error")
                            tempFile.delete() // حذف الملف المؤقت في حالة حدوث خطأ
                        }
                    }
                })

            } catch (e: Exception) {
                e.printStackTrace()
                callBack.onRequestIsFailure(e.message ?: "Unknown error")
            }
        }
    }
    fun generateStreamTextAudio5(inputText: String, callBack: IWasmServiceEventListener) {
        audioPlayerIsComplete=false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                    override fun onStreamReader(response: String?,lastFlow:Boolean) {
                        if(response!=null){
                            response?.let {
                                Log.d("response", response!!)

                                var text: String? = removeSymbols(it) ?: return
                                Log.d("metaData", text!!)

//                                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

                                if (text?.isNullOrEmpty()==false && containsLetters(text)) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        semaphore.withPermit {
                                            audioPlayerIsComplete=false
                                            var audioBytes = queryTextToSpeech(text)
                                            if (audioBytes == null || audioBytes.isEmpty()) {
                                                audioBytes = queryTextToSpeech(text)
                                            }
                                            withContext(Dispatchers.Main) {
                                                if(audioBytes == null ) {
                                                    delay(1000)
                                                    callBack?.onRequestIsSuccess2(stopMediaPlayer)
                                                } else{
                                                    handleAudioBytes(audioBytes, callBack)
                                                }
                                                delay(1000)
                                                Log.d("getRemainingTime","${exoPlayer?.getRemainingTime()}")
                                                delay(exoPlayer?.getRemainingTime()?:1000)
                                            }
//                                            delay(5000)

                                        }
                                    }
                                }
                            }
                        }
                    }
                    override fun onStreamComplete(lastFlowIndex:Int) {
                        CoroutineScope(Dispatchers.Main).launch {

                            semaphore.withPermit {
                                audioPlayerIsComplete=true;
                                if(exoPlayer?.isPlayer()!!) {
                                    delay(exoPlayer?.getRemainingTime()?:1000)
                                    Log.d("onStreamComplete", "Cancel")
                                    callBack?.onRequestIsSuccess2(stopMediaPlayer)
                                }
                            }
                        }
                    }
                    override fun onStreamError(e: Throwable) {
                        CoroutineScope(Dispatchers.Main).launch {
                            semaphore.withPermit {
                                try {
                                    audioPlayerIsComplete=true;
//                                    if(!exoPlayer!!.isPlayer())
//                                        callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                    exoPlayer?.stop()
                                } finally {
                                    Log.d("onStreamError", "Cancel")
                                    callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                }
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                callBack?.onRequestIsSuccess2(stopMediaPlayer)
            }
        }
    }
    fun generateStreamTextAudio0(inputText: String, callBack: IWasmServiceEventListener) {
        audioPlayerIsComplete = false
        val exoPlayer =  ExoPlayer.Builder(context).build()
        val semaphore = Semaphore(1)
        val audioFileChannel = Channel<String>(Channel.UNLIMITED)
        var index=0;
        val queue: Queue<ByteArray?> = LinkedList()
        val mutableMap = mutableMapOf<Int, ByteArray?>()
        val channel = Channel<ByteArray>()
        // callBack?.stopListener()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                    @SuppressLint("SuspiciousIndentation")
                    override fun onStreamReader(response: String?, lastFlow:Boolean) {
                        Log.d("response::", "${index}")
//                        var audioBytes:ByteArray?=null
                        if (response != null) {

//                            if(response == ContentApp.END_SYMBOL ){
//                                if(!channel.isEmpty && !channel.isClosedForSend)
//                                    channel.close()
//                            } else

//                            {
                            CoroutineScope(Dispatchers.IO).launch {

//                                    withContext(Dispatchers.IO) {}
//                                semaphore.withPermit {
//                                    audioPlayerIsComplete = false;
//                                }
                                response?.let {

                                    var text: String? = removeSymbols(it) ?: return@let
                                    Log.d("metaData", text!!)
                                    if (text?.isNullOrEmpty() == false && containsLetters(text)) {

                                        val audioBytes = queryTextToSpeechAPI(text)?:return@launch

                                            Log.d("queryTextToSpeech","${audioBytes.size}")
                                            val filePath = saveIntoTempFile(audioBytes)
//                                            launch(Dispatchers.IO) {
                                             Log.d("filePath",filePath!!)

//                                                    playAudio(filePath!!, exoPlayer, semaphore)
//
//                                                    if(lastFlow){
//                                                        callBack?.onRequestIsSuccess2(null)
//                                                    }
//                                                }

                                    }
//                                    if(lastFlow){
//                                        launch {
//                                            semaphore.withPermit {
//                                                if (exoPlayer?.isPlaying!!) {
//                                                    delay(getRemainingTime(exoPlayer))
//                                                }
//                                                delay(1000)
//                                                callBack?.onRequestIsSuccess2(null)
//                                            }
//
//                                        }
//                                    }
//                                            audioFileChannel.send(filePath!!)


//                                        var audioBytes = queryTextToSpeech(text)
//
//                                        //                                            if (audioBytes == null || audioBytes.isEmpty()) {
//                                        //                                                audioBytes = queryTextToSpeech(text)
//                                        //                                            }
//
//                                        if (audioBytes == null) {
//                                            delay(1000)
//                                            withContext(Dispatchers.Main) {
//                                                callBack?.onRequestIsSuccess2(stopMediaPlayer)
//                                            }
//                                        } else {
//                                            semaphore.withPermit {
//                                                queue.add(audioBytes)
//                                                mutableMap.put(index++, audioBytes)
//                                                channel.send(audioBytes)
//                                                handleAudioBytes(audioBytes, callBack)
//                                                withContext(Dispatchers.Main) {
//                                                    Log.d(
//                                                        "getRemainingTime",
//                                                        "${exoPlayer?.getRemainingTime()}"
//                                                    )
//                                                    delay(1000)
//                                                    Log.d(
//                                                        "getRemainingTime",
//                                                        "${exoPlayer?.getRemainingTime()}"
//                                                    )
//                                                    delay(exoPlayer?.getRemainingTime() ?: 1000)
//                                                }
//                                            }
//                                        }
//                                    }

                            }
                        }
                    }
                }

                    override fun onStreamComplete(lastFlowIndex:Int) {
                        CoroutineScope(Dispatchers.Main).launch {

                            semaphore.withPermit {
                                audioPlayerIsComplete = true;
    //                                delay(1000)
                                Log.d("onStreamComplete", "..")
    //                                if(!exoPlayer?.isPlayer()!!) {
    //                                    delay(exoPlayer?.getRemainingTime()?:1000)
    //
    ////                                    callBack?.onRequestIsSuccess2(stopMediaPlayer)
    //                                }
                            }
                        }
                    }
                    override fun onStreamError(e: Throwable) {
                        CoroutineScope(Dispatchers.Main).launch {
                            semaphore.withPermit {
                                try {
                                    audioPlayerIsComplete = true;
    //                                    if(!exoPlayer!!.isPlayer())
    //                                        callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                    exoPlayer?.stop()
                                    exoPlayer?.release()
                                } finally {
                                    Log.d("onStreamError", "Cancel")
                                    callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                }
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                callBack?.onRequestIsFailure(e.message!!)
            }
        }
    }
    fun generateStreamTextAudio6(inputText: String, callBack: IWasmServiceEventListener) {

        val exoPlayer =  ExoPlayer.Builder(context).build()
        val semaphore = Semaphore(1)
        val audioFileChannel = Channel<String>(Channel.UNLIMITED)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                    @SuppressLint("SuspiciousIndentation")
                    override fun onStreamReader(response: String?, lastFlow:Boolean) {
                        if (response != null) {
                            response?.let {
                                var text: String? = removeSymbols(it) ?: return@let
                                Log.d("metaData", text!!)
                                if (text?.isNullOrEmpty() == false && containsLetters(text)) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val audioBytes = queryTextToSpeechAPI(text) ?: return@launch
                                        if(audioBytes==null){
                                            if (lastFlow) {
                                                callBack?.onRequestIsSuccess2(null)
                                            }
                                        }
                                        Log.d("queryTextToSpeech", "${audioBytes.size}")
                                        val filePath = saveIntoTempFile(audioBytes)
//                                        launch(Dispatchers.IO) {
                                            Log.d("filePath", filePath!!)

                                                playAudio(
                                                    filePath!!,
                                                    exoPlayer,
                                                    semaphore,
                                                    object : ICustomPlayerListener<ExoPlayer> {
                                                        override fun onErrorListener(mp: ExoPlayer?) {
                                                            if (lastFlow) {
                                                                callBack?.onRequestIsSuccess2(null)
                                                            }
                                                        }
                                                        override fun onCompletionListener(mp: ExoPlayer?) {
                                                            if (lastFlow) {
                                                                callBack?.onRequestIsSuccess2(null)
                                                            }
                                                        }
                                                    })

//                                        }
                                    }
                                }
                            }
//                            if (lastFlow) {
//
//                                launch{
//                                    delay(1000)
//
//                                withContext(Dispatchers.Main) {
//                                    semaphore.withPermit {
//                                        if (exoPlayer?.isPlaying == true) {
//                                            delay(getRemainingTime(exoPlayer))
//                                        }
//                                        delay(1000)
//                                        callBack?.onRequestIsSuccess2(null)
//                                    }
//                                }
//                                }
//                            }
                        }
                    }

                    override fun onStreamComplete(lastFlowIndex:Int) {
//                        CoroutineScope(Dispatchers.Main).launch {
////                            semaphore.withPermit {
////                                Log.d("onStreamComplete", "..")
////                            }
//                        }
                    }
                    override fun onStreamError(e: Throwable) {
                        CoroutineScope(Dispatchers.Main).launch {
                            semaphore.withPermit {
                                try {
                                    exoPlayer?.stop()
                                    exoPlayer?.release()
                                } finally {
                                    Log.d("onStreamError", "Cancel")
                                    callBack?.onRequestIsFailure(e.message ?: "Unknown error")
                                }
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                callBack?.onRequestIsFailure(e.message!!)
            }
        }
    }
    fun generateStreamTextAudio7(inputText: String, callBack: IWasmServiceEventListener) {

        var isStartSpeak=false;
        callBack?.stopListener()

        if(jopStreamFlow!=null && jopStreamFlow?.isCancelled==false)
            jopStreamFlow?.cancel()
        if(jopStreamAudio!=null && jopStreamAudio?.isCancelled==false)
            jopStreamAudio?.cancel()

        try {
            if(flowAudioFileMap!=null && flowAudioFileMap?.isNotEmpty()==true)
                flowAudioFileMap?.clear()

            jopStreamFlow=CoroutineScope(Dispatchers.IO).launch {
                try {
                    geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                        override fun onStreamReader(response: String?,flowIndex:Int,lastFlowIndex:Int) {
                            try {
                                Log.d("metaData", "$response : $flowIndex - $lastFlowIndex")
                                if (response?.trim() == ContentApp.END_SYMBOL && jopStreamAudio?.isActive==false) {
                                    cancelJop(callBack)
                                }else{
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            if (response != null && response.trim().isNotEmpty()) {
                                                // Log.d("response","$response  $flowIndex  $lastFlowIndex")

                                                if (response == ContentApp.END_SYMBOL) {
                                                    Log.d("Completed", "$flowIndex")
                                                    semaphore.withPermit {
                                                        isStartSpeak = true
                                                        flowAudioFileMap?.put(flowIndex, "END")
                                                    }
                                                } else {

                                                    var text: String? = removeSymbols(response.trim()) ?: return@launch

                                                    if (text?.isNullOrEmpty() == false && containsLetters(text)
                                                    ) {
                                                        try {
                                                            val audioBytes = queryTextToSpeech(text)

                                                            if (audioBytes != null) {
                                                                val tempFile: File? = createTempFile(flowIndex, audioBytes!!)

                                                                Log.d("FilePath", tempFile?.absolutePath!!)
                                                                Log.d("FileLength", "${tempFile?.length()}")
                                                                if (tempFile != null && tempFile?.absolutePath?.isNullOrEmpty() == false
                                                                    && tempFile.length() > 0 && tempFile.canRead()
                                                                ) {
                                                                    semaphore.withPermit {
                                                                        isStartSpeak = true
                                                                        flowAudioFileMap?.put(flowIndex, tempFile)
                                                                        Log.d("flow", "$flowIndex - $lastFlowIndex")
                                                                    }
//                                                                return@withPermit

                                                                }
                                                            }
                                                        } catch (e: CancellationException) {
                                                            Log.d(
                                                                "NullPointerException",
                                                                "audioBytes null"
                                                            )
                                                            this@launch.cancel()
                                                        } catch (e: Exception) {
                                                            this@launch.cancel()
                                                        }
                                                    }
                                                }
                                            }
                                        }catch (e:Exception){
                                            e.printStackTrace()
                                            cancelJop(callBack)
                                        }
                                    }
                                }

                            }
                            catch (e:CancellationException) {
                                e.printStackTrace()
                                Log.e("Cancellation Stream Flow Exception", e.message!!)
                               this@launch.cancel()
                            }catch (e:Exception) {
                                e.printStackTrace()
                                Log.e("Stream Flow Exception", e.message!!)
                                this@launch.cancel()
                            }
                    }
                        override fun onStreamComplete(_lastFlowIndex:Int) {

//                            CoroutineScope(Dispatchers.Main).launch {
//                                semaphore.withPermit {
//                                    Log.d("onStreamComplete","$_lastFlowIndex - ${flowAudioFileMap?.count()}")
//                                    isStartSpeak=true
////                                    lastFlowIndex=_lastFlowIndex
////                                    isCompleted=true
//                                    flowAudioFileMap?.put(lastFlowIndex, "END")
//                                    Log.d("onStreamComplete"," ${flowAudioFileMap?.count()}")
//                                }
//                            }
                        }
                        override fun onStreamError(e: Throwable) {
//                           launch {
//                                semaphore.withPermit {
//                                    isCompleted=true
//                                    Log.d("onStreamError", "Cancel")
//                                    }
//                                }
                            cancelJop(callBack)
                        }
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Cancellation Stream Flow Exception",e.message!!)
                    cancelJop(callBack)
                }
        }
            jopStreamAudio=CoroutineScope(Dispatchers.IO).launch {

                var index=0;
                var isCompleted = false
                try {
                    while (true) {
                        delay(1000)
                        semaphore.withPermit {
                            if (isStartSpeak && flowAudioFileMap?.isNullOrEmpty() == false && flowAudioFileMap?.containsKey(index) == true) {
                                Log.d("getFlowAudioFileMap","${flowAudioFileMap?.count()}")
                                var value = flowAudioFileMap?.get(index++)?:null
//                                Log.d("value ","${value!!}")
                                if (value != null) {
                                    if (value is String && !isCompleted) {
                                        var text = value as? String
                                        Log.d("value_text",text!!)
                                        isCompleted = text?.isNullOrEmpty() == false && text.trim() == "END"
                                    } else if (value is File) {
                                        var file: File? = value as? File
                                        withContext(Dispatchers.Main) {
                                            playAudio2(
                                                file?.absolutePath!!,
                                                myExoPlayer!!,
                                                semaphore,
                                                object : ICustomPlayerListener<ExoPlayer> {
                                                    override fun onErrorListener(mp: ExoPlayer?) {
                                                        if (isCompleted) {
//                                                        callBack.onRequestIsSuccess2(null)
                                                            this@launch?.cancel()
                                                        }

                                                    }
                                                    override fun onCompletionListener(mp: ExoPlayer?) {
//                                                    CoroutineScope(Dispatchers.Main).launch{
//                                                        var len = flowAudioFileMap?.count()
//                                                        isCompleted = lastFlowIndex == index && flowAudioFileMap?.get(len) == null
                                                        Log.d("onCompletionListener", "$isCompleted - ${flowAudioFileMap?.count()}")
//                                                        if (isStartSpeak) {
//                                                            Log.d("CompletedPlayFlowsAudio", "${flowAudioFileMap?.count()}")
//                                                            withContext(Dispatchers.Main){
//                                                                if(exoPlayer?.isPlayer()==false)
//                                                                    this@launch?.cancel()
//                                                            }
                                                        var value=flowAudioFileMap?.get(index)?:null
                                                        Log.d("onCompletionListener2", "$value")
                                                        if (value!=null && value is String && value as? String=="END") {
//                                                            callBack?.onRequestIsSuccess2(null)
                                                            Log.d("onCompletionListener2", "true")
                                                            this@launch?.cancel()
                                                        }
                                                    }
                                                })
                                            delay(1000)
                                            var time = getRemainingTime(myExoPlayer!!)
                                            Log.d("RemainingTime", "$time")
                                            delay(if (time > 0) time else 1000)
                                            if (isCompleted) {
                                                this@launch?.cancel()
                                            }
                                            return@withContext
                                        }
                                    }
                                }
                                else {
                                    this@launch?.cancel()
                                }

//                                var len = flowAudioFileMap?.count()
//                                var isCompleted =
//                                    lastFlowIndex == index && lastFlowIndex > 0 && flowAudioFileMap?.get(
//                                        lastFlowIndex
//                                    ) == null
//                                Log.d("count", "$index - len:$len - lastIndex:$lastFlowIndex -  $isCompleted  -  ${flowAudioFileMap?.get(lastFlowIndex)}")
//File?

                            }
                            else if (isCompleted) {
                                    Log.d("CompletedPlayFlowsAudio", "${flowAudioFileMap?.count()}")
                                    this@launch?.cancel()
                                }
                        }
                    }
                } catch (e:CancellationException) {
                    e.printStackTrace()
                    Log.e("Cancellation Stream Audio Exception", e.message!!)
                    cancelJop(callBack)
                } catch (e:Exception){
                    e.printStackTrace()
                    Log.e("Stream Audio Exception",e.message!!)
                    cancelJop(callBack)
                }
            }
            jopStreamFlow?.start()
            jopStreamAudio?.start()

        } catch (e:Exception) {
            e.printStackTrace()
            Log.e("Cancellation Two Coroutine  Exception", e.message!!)
            cancelJop(callBack)
        }
    }
    fun generateStreamTextAudio(inputText: String, callBack: IWasmServiceEventListener) {

        var isStartSpeak=false;
        callBack?.stopListener()

        try {
            if(flowAudioFileMap!=null && flowAudioFileMap?.isNotEmpty()==true)
                       flowAudioFileMap?.clear()

            jopStreamFlow=CoroutineScope(Dispatchers.IO).launch {

                try {

                    geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                        override fun onStreamReader(response: String?,flowIndex:Int,lastFlowIndex:Int) {
                            try {
                                Log.d("metaData", "$response : $flowIndex - $lastFlowIndex")
                                if (response?.trim() == ContentApp.END_SYMBOL && jopStreamAudio?.isActive==false) {
                                    cancelJop(callBack)
                                }else{
                                    CoroutineScope(Dispatchers.IO).launch {

                                        try {
                                            if (response != null && response.trim().isNotEmpty()) {
                                                // Log.d("response","$response  $flowIndex  $lastFlowIndex")

                                                if (response == ContentApp.END_SYMBOL) {
                                                    Log.d("Completed", "$flowIndex")
                                                    semaphore.withPermit {
                                                        isStartSpeak = true
                                                        flowAudioFileMap?.put(flowIndex, "END")
                                                    }
                                                } else {

                                                    var text: String? = removeSymbols(response.trim()) ?: return@launch

                                                    if (text?.isNullOrEmpty() == false && containsLetters(text)) {
                                                        try {
                                                            val audioBytes = queryTextToSpeech(text)
                                                            if (audioBytes != null) {
                                                                val tempFile: File? = createTempFile(flowIndex, audioBytes!!)

//                                                                Log.d("FilePath", tempFile?.absolutePath!!)
//                                                                Log.d("FileLength", "${tempFile?.length()}")
                                                                if (tempFile != null && tempFile?.absolutePath?.isNullOrEmpty() == false) {
                                                                    semaphore.withPermit {
                                                                        isStartSpeak = true
                                                                        flowAudioFileMap?.put(flowIndex, tempFile)
                                                                        Log.d("flow", "$flowIndex - $lastFlowIndex")
                                                                        return@withPermit
                                                                    }
                                                                }
                                                            }
                                                        } catch (e: CancellationException) {
                                                            Log.d(
                                                                "NullPointerException",
                                                                "audioBytes null"
                                                            )
                                                            this@launch.cancel()
                                                        } catch (e: Exception) {
                                                            this@launch.cancel()
                                                        }
                                                    }
                                                }
                                            }
                                        }catch (e:Exception){
                                            e.printStackTrace()
                                            cancelJop(callBack)
                                            return@launch
                                        }

                                        return@launch
                                    }
                                }

                            }
                            catch (e:CancellationException) {
                                e.printStackTrace()
                                Log.e("Cancellation Stream Flow Exception", e.message!!)
                               this@launch.cancel()
                            }catch (e:Exception) {
                                e.printStackTrace()
                                Log.e("Stream Flow Exception", e.message!!)
                                this@launch.cancel()
                            }
                    }
                        override fun onStreamComplete(_lastFlowIndex:Int) {

//                            CoroutineScope(Dispatchers.Main).launch {
//                                semaphore.withPermit {
//                                    Log.d("onStreamComplete","$_lastFlowIndex - ${flowAudioFileMap?.count()}")
//                                    isStartSpeak=true
////                                    lastFlowIndex=_lastFlowIndex
////                                    isCompleted=true
//                                    flowAudioFileMap?.put(lastFlowIndex, "END")
//                                    Log.d("onStreamComplete"," ${flowAudioFileMap?.count()}")
//                                }
//                            }
                        }
                        override fun onStreamError(e: Throwable) {
//                           launch {
//                                semaphore.withPermit {
//                                    isCompleted=true
//                                    Log.d("onStreamError", "Cancel")
//                                    }
//                                }
                            cancelJop(callBack)
                        }
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Cancellation Stream Flow Exception",e.message!!)
                    cancelJop(callBack)
                }
        }
            jopStreamAudio=CoroutineScope(Dispatchers.IO).launch {

                var index=0;
                var isCompleted = false
                try {
                    while (true) {


//                            if(index>=flowAudioFileMap?.count()!!)
//                                delay(500)
////                            println("isStartSpeak: $isStartSpeak")
//                            else
                            var value = semaphore.withPermit {
                                if (isStartSpeak && flowAudioFileMap?.containsKey(index) == true) {
                                    Log.d("getFlowAudioFileMap", "${flowAudioFileMap?.count()}")
                                    Log.d("jopStreamAudio", "$index")
                                    return@withPermit flowAudioFileMap?.get(index++) ?: null
                                }
                                return@withPermit null
                            }
//                            if (isStartSpeak && flowAudioFileMap?.containsKey(index) == true) {
//                                Log.d("getFlowAudioFileMap", "${flowAudioFileMap?.count()}")
//                                Log.d("jopStreamAudio", "$index")
//                                var value = flowAudioFileMap?.get(index) ?: null
//
//                            }
                            if (value != null) {
                                Log.d("value_text", "$index - $value ")
                                if (value is String) {
                                    var text = value as? String
//                                        Log.d("value_text",text!!)
                                    isCompleted = text?.isNullOrEmpty() == false && text.trim() == "END"
                                    withContext(Dispatchers.Main) {
                                        var time = getRemainingTime(myExoPlayer!!)
                                        delay(if (time > 0) time else 100)
                                        this@launch?.cancel()
                                    }
                                }
                                else if (value is File) {
                                    var file: File? = value
                                    withContext(Dispatchers.Main) {
                                        semaphore.withPermit {
                                            playAudio2(
                                                file?.absolutePath!!,
                                                myExoPlayer!!,
                                                semaphore,
                                                object : ICustomPlayerListener<ExoPlayer> {
                                                    override fun onErrorListener(mp: ExoPlayer?) {
                                                        if (isCompleted) {
//                                                        callBack.onRequestIsSuccess2(null)
                                                            this@launch?.cancel()
                                                        }

                                                    }
                                                    override fun onCompletionListener(mp: ExoPlayer?) {

                                                                Log.d(
                                                                    "onCompletionListener",
                                                                    "$isCompleted - $index - ${flowAudioFileMap?.count()}"
                                                                )
                                                                if (isCompleted) {
                                                                    Log.d(
                                                                        "onCompletionListener2",
                                                                        "true"
                                                                    )
                                                                    try{
                                                                        jopStreamAudio?.cancel()
                                                                        jopStreamFlow?.cancel()
                                                                    }finally {
                                                                        callBack?.onRequestIsSuccess2(null)
                                                                    }
                                                                }

                                                    }
                                                })

                                            delay(500)
                                            var time = getRemainingTime(myExoPlayer!!)
                                            Log.d("RemainingTime", "$time")
                                            delay(if(time>0) time else 100)
                                            if (isCompleted) {
                                                cancelJop(callBack)
                                            }
                                            return@withPermit 0
                                        }
                                        return@withContext 0
                                    }
                                }
                            } else if (isCompleted) {
                                    Log.d("CompletedPlayFlowsAudio", "${flowAudioFileMap?.count()}")
                                    this@launch?.cancel()
                            }
                            delay(500)
                        }

                } catch (e:CancellationException) {
                    e.printStackTrace()
                    Log.e("Cancellation Stream Audio Exception", e.message!!)
                    cancelJop(callBack)
                } catch (e:Exception){
                    e.printStackTrace()
                    Log.e("Stream Audio Exception",e.message!!)
                    cancelJop(callBack)
                }
                return@launch
            }
            jopStreamFlow?.start()
            jopStreamAudio?.start()

        } catch (e:Exception) {
            e.printStackTrace()
            Log.e("Cancellation Two Coroutine  Exception", e.message!!)
            cancelJop(callBack)
        }
    }
    suspend fun generateStreamTextAudioLast(inputText: String, callBack: IWasmServiceEventListener) {

//        var isStartSpeak=false;
        callBack?.stopListener()
        try {
//                semaphore.withPermit {
//                    if(flowAudioFileMap!=null && flowAudioFileMap?.isNotEmpty()==true)
//                        flowAudioFileMap?.clear()
//                }

                println("generateStreamText.....")


                var jopStreamAudio = CoroutineScope(Dispatchers.IO).launch {

                    println("jopStreamAudio_Start.....")
                    try {
                        generateStreamText(inputText)
//                        delay(2000)
//                        withContext(Dispatchers.Main) {
//                            playAllAudio()
//                        }

                    } catch (e: CancellationException) {
                        e.printStackTrace()
                        Log.e("Cancellation Stream Audio Exception", e.message!!)
                        cancelJop(callBack)
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("Stream Audio Exception", e.message!!)
                        cancelJop(callBack)
                    }
                    return@launch
                }

                jopStreamAudio?.cancelAndJoin()

        } catch (e:Exception) {
            e.printStackTrace()
            Log.e("Cancellation Two Coroutine  Exception", e.message!!)
//            cancelJop(callBack)
        }finally {
            Log.d("Completes","generateStreamTextAudioLast")
//            cancelJop(callBack)
            return
        }
    }
    fun generateStreamTextAudioV1(inputText:String, callBack: IWasmServiceEventListener) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                generateStreamText(inputText)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private suspend fun generateStreamText(inputText: String){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                    override fun onStreamReader(response: String?, flowIndex: Int, lastFlowIndex: Int) {
                        try {
                            Log.d("metaData", "$response : $flowIndex - $lastFlowIndex")
                            if (response != null && response.trim().isNotEmpty()) {
                                val text: String? = removeSymbols(response.trim()) ?: return
                                if (!text.isNullOrEmpty() && containsLetters(text)) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val audioBytes = queryTextToSpeech(text)
                                            if (audioBytes != null) {
                                                val tempFile: File? = createTempFile(flowIndex, audioBytes!!)
                                                if (tempFile != null && tempFile.absolutePath?.isNotEmpty() == true) {
                                                    withContext(Dispatchers.Main) {
                                                        semaphore.withPermit {
                                                            myExoPlayer?.setMediaItem(MediaItem.fromUri(tempFile?.absolutePath!!))
                                                            myExoPlayer?.prepare()
                                                            myExoPlayer?.playWhenReady = true
                                                            Log.d("BeforeCompletionAudio", "1")
                                                            while (myExoPlayer?.isPlaying==true){
                                                                delay(500)
                                                            }
                                                            if (response?.trim() == ContentApp.END_SYMBOL) {
                                                                this@launch.cancel()
                                                            }
//                                                                    waitForExoPlayerCompletion(
//                                                                        myExoPlayer!!
//                                                                    )
                                                            //                                                                flowAudioFileMap?.put(flowIndex, tempFile)
                                                            Log.d("flow", "$flowIndex - $lastFlowIndex")
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (e: CancellationException) {
                                            Log.d("CancellationException", e.message!!)
                                            this@launch.cancel()
                                        } catch (e: Exception) {
                                            this@launch.cancel()
                                            Log.d("Exception", e.message!!)
                                        }
                                    }
                                }
                            }
                        } catch (e: CancellationException) {
                            e.printStackTrace()
                            Log.e("Cancellation Stream Flow Exception", e.message!!)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("Stream Flow Exception", e.message!!)
                        }
                    }
                    override fun onStreamComplete(_lastFlowIndex: Int) {
                        Log.e("onStreamComplete", "$_lastFlowIndex")
                    }
                    override fun onStreamError(e: Throwable) {
                        e.printStackTrace()
                        Log.e("onStreamError", e.message!!)
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    var callBack:IWasmServiceEventListener?=null;
    var respobseText:String?=null;
    suspend  fun generateStreamText3(inputText: String, callBack: IWasmServiceEventListener) {
         try {


//                 CoroutineScope(Dispatchers.IO).launch {
             geminiApiClient?.sendMessageStream(inputText, object : IListenerStream<String> {
                         override fun onStreamReader(
                             response: String?,
                             flowIndex: Int,
                             lastFlowIndex: Int
                         ) {
                             try {
                                 Log.d("metaData", "$response : $flowIndex - $lastFlowIndex")
                                 if (response != null && response.trim().isNotEmpty()) {
                                     CoroutineScope(Dispatchers.IO).launch {
                                         withContext(Dispatchers.Main) {
                                             semaphore.withPermit {
                                                 callBack?.stopListener()
                                             }
                                         }
                                         respobseText=response?.trim()
                                         if (respobseText != ContentApp.END_SYMBOL) {
                                             val text: String? = removeSymbols(respobseText!!) ?: return@launch
                                             if (!text.isNullOrEmpty() && containsLetters(text)) {
                                                 try {

                                                     val audioBytes = queryTextToSpeech(text)
                                                     if (audioBytes != null) {
                                                         Log.d("metaData",audioBytes?.size?.toString()!!)
                                                         val tempFile: File? = createTempFile(flowIndex,audioBytes!!)
                                                         if (tempFile != null && tempFile.absolutePath?.isNotEmpty() == true) {
                                                             withContext(Dispatchers.Main) {
                                                                 try {
                                                                     semaphore.withPermit {
                                                                         callBack?.stopListener()
                                                                         Log.d("tempFile", tempFile?.absolutePath!!)
                                                                         playAudio2(tempFile?.absolutePath!!, myExoPlayer!!, semaphore, object : ICustomPlayerListener<ExoPlayer> {
                                                                                 override fun onErrorListener(mp: ExoPlayer?) {
                                                                                     Log.d("onErrorListener", "onErrorListener")

                                                                                     this@SpeechChatControl.callBack?.onRequestIsSuccess2(null)

                                                                                 }
                                                                                 override fun onCompletionListener(mp:ExoPlayer?) {
                                                                                     Log.d("onCompletionListener", respobseText!!)
                                                                                     if (respobseText == ContentApp.END_SYMBOL) {
                                                                                         Log.d("onCompletionListener", "End All Audio")
                                                                                         callBack?.onRequestIsSuccess2(null)
                                                                                     }
                                                                                 }
                                                                             })
    //                                                                     myExoPlayer?.setMediaItem(MediaItem.fromUri(tempFile?.absolutePath!!))
    //                                                                     myExoPlayer?.prepare()
    //                                                                     myExoPlayer?.prepare()
    //                                                                     myExoPlayer?.playWhenReady = true
    //                                                                     delay(1000)
                                                                         while (myExoPlayer?.isPlaying == true) {
                                                                             delay(1000)
                                                                         }
                                                                         Log.d("myExoPlayer_completed", "$flowIndex - $lastFlowIndex")

    //                                                                            if (tempFile?.exists() == true)
    //                                                                                tempFile?.delete()


                                                                     }

                                                                 } catch (e: PlaybackException) {
                                                                     e.printStackTrace()
                                                                 } catch (e: Exception) {
                                                                     e.printStackTrace()
                                                                 }

                                                             }

                                                         }
                                                     }


    //                                                 this@launch.cancel()

                                                 }
                                                 catch (e: ConnectException) {
                                                     Log.d("ConnectException8", e.message!!)
                                                 }
                                                 catch (e: FileNotFoundException) {
                                                     Log.d("FileNotFoundException8", e.message!!)
                                                 }
                                                 catch (e: SecurityException) {
                                                     Log.d("SecurityException8", e.message!!)
                                                 }
                                                 catch (e: IOException) {
                                                     Log.d("IOException8", e.message!!)
                                                 }
                                                 catch (e: CancellationException) {
                                                     Log.d("CancellationException8", e.message!!)
                                                 }
                                                 catch (e: Exception) {
                                                     Log.d("Exception8", e.message!!)
                                                 }
                                             }
                                         } else {
                                             Log.d("Completed", "yes")
    //                                         withContext(Dispatchers.Main) {
    //                                             delay(1000)
    //                                             semaphore.withPermit {
    //                                                 while (myExoPlayer?.isPlaying == true) {
    //                                                     delay(1000)
    //                                                 }
    //                                             }
    //                                             callBack?.onRequestIsSuccess2(null)
    //                                         }
                                         }
                                     }
                                 }
                             } catch (e: CancellationException) {
                                 e.printStackTrace()
                                 Log.e("Cancellation Stream Flow Exception", e.message!!)
                             } catch (e: Exception) {
                                 e.printStackTrace()
                                 Log.e("Stream Flow Exception", e.message!!)
                             }
                         }

                         override fun onStreamComplete(_lastFlowIndex: Int) {
                             Log.e("onStreamComplete", "$_lastFlowIndex")
                         }

                         override fun onStreamError(e: Throwable) {
                             e.printStackTrace()
                             Log.e("onStreamError", e.message!!)
                         }
                     })
//                 }
             } catch (e: Exception) {
                 e.printStackTrace()
                 Log.e("End-generateStreamText3", e.message!!)
             }
    }
    private suspend fun generateStreamText2(inputText:String) : Unit = suspendCancellableCoroutine { continuation ->

        try {

            CoroutineScope(Dispatchers.IO).launch {
                var listener =object : IListenerStream<String> {
                    override fun onStreamReader(response: String?,flowIndex:Int,lastFlowIndex:Int) {
                        try {
                            Log.d("metaData", "$response : $flowIndex - $lastFlowIndex")
                            if (response?.trim() == ContentApp.END_SYMBOL) {
                                CoroutineScope(Dispatchers.Main).launch{
                                    semaphore.withPermit {
                                        flowAudioFileMap?.put(flowIndex, "END")
                                    }
                                }
                            } else{
                                try {
                                    if (response != null && response.trim().isNotEmpty()) {
                                        var text: String? = removeSymbols(response.trim()) ?: return
                                        if (text?.isNullOrEmpty() == false && containsLetters(text)) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                try {
                                                    val audioBytes = queryTextToSpeech(text)
                                                    if (audioBytes != null) {
                                                        val tempFile: File? = createTempFile(flowIndex, audioBytes!!)

//                                                                Log.d("FilePath", tempFile?.absolutePath!!)
//                                                                Log.d("FileLength", "${tempFile?.length()}")
                                                        if (tempFile != null && tempFile?.absolutePath?.isNullOrEmpty() == false) {
                                                            semaphore.withPermit {
                                                                flowAudioFileMap?.put(flowIndex, tempFile)
                                                                Log.d("flow", "$flowIndex - $lastFlowIndex")
                                                                return@withPermit
                                                            }
                                                        }
                                                    }
                                                } catch (e: CancellationException) {
                                                    Log.d("NullPointerException", "audioBytes null")
                                                    this@launch.cancel()
                                                } catch (e: Exception) {
                                                    this@launch.cancel()
                                                }
                                            }
                                        }
                                    }
                                }catch (e:Exception){
                                    e.printStackTrace()
                                    return
                                }
                            }

                        } catch (e:CancellationException) {
                            e.printStackTrace()
                            Log.e("Cancellation Stream Flow Exception", e.message!!)
                        }catch (e:Exception) {
                            e.printStackTrace()
                            Log.e("Stream Flow Exception", e.message!!)
                        }finally {
                            if (continuation.isActive) {
                                continuation.resume(Unit, null)
                            }
                        }
                    }
                    override fun onStreamComplete(_lastFlowIndex:Int) {
                        if (continuation.isActive) {
                            continuation.resume(Unit, null)
                        }
//                            CoroutineScope(Dispatchers.Main).launch {
//                                semaphore.withPermit {
//                                    Log.d("onStreamComplete","$_lastFlowIndex - ${flowAudioFileMap?.count()}")
//                                    isStartSpeak=true
////                                    lastFlowIndex=_lastFlowIndex
////                                    isCompleted=true
//                                    flowAudioFileMap?.put(lastFlowIndex, "END")
//                                    Log.d("onStreamComplete"," ${flowAudioFileMap?.count()}")
//                                }
//                            }
                    }
                    override fun onStreamError(e: Throwable) {
                        continuation.resumeWithException(e)
//                cancelJop(callBack)
                    }
                }
                geminiApiClient?.sendMessageStream(inputText, listener)
            }

        } catch (e:Exception){
            e.printStackTrace()
            throw Exception()
        }

    }
    private suspend fun playAllAudio() {

        try {
            Log.d("playAllAudio", "-")
            semaphore.withPermit {

                    var items =  flowAudioFileMap?.toSortedMap()
                    Log.d("playAllAudio", "${items?.count()}")
                if (items != null && items?.isNotEmpty() == true) {
                    var value = items[0] ?: null
                    if (value is File && value?.absolutePath?.isNullOrEmpty() == false) {

                        myExoPlayer?.setMediaItem(MediaItem.fromUri(value?.absolutePath!!))
                        myExoPlayer?.prepare()
                        myExoPlayer?.playWhenReady = true
                        Log.d("BeforeCompletionAudio", "1")
                        waitForExoPlayerCompletion(myExoPlayer!!)

                        Log.d("CompletionAudio", "1")
                        items.remove(0)
                        playAllAudio()
                    } else if (value is String) {
                        return@withPermit
                    }
                }
            }
            playAllAudio()
        } catch (e:Exception){
            e.printStackTrace()
            Log.e("Stream Audio Exception",e.message!!)
        }
    }
    private fun cancelJop(callBack: IWasmServiceEventListener) {
        CoroutineScope(Dispatchers.Main).launch {
            try {

                semaphore.withPermit {
                    if (myExoPlayer != null && myExoPlayer?.isPlaying == true)
                        myExoPlayer?.stop()

                    if (flowAudioFileMap != null && flowAudioFileMap?.isNotEmpty() == true)
                        flowAudioFileMap?.forEach { file ->
                            if (file is File) {
                                file?.delete()
                            }
                        }
                    flowAudioFileMap?.clear()
                }

                if (jopStreamFlow != null && jopStreamFlow?.isCancelled == false && jopStreamFlow?.isActive == true)//&& jopStreamFlow?.isCompleted==false
                    jopStreamFlow?.cancel()
                if (jopStreamAudio != null && jopStreamAudio?.isCancelled == false && jopStreamAudio?.isActive == true) //&& jopStreamAudio?.isCompleted==false
                    jopStreamAudio?.cancel()


            } catch (e: CancellationException) {
                Log.e("CancellationException", e.message!!)
            } catch (e: Exception) {
                Log.e("cancelJop", e.message!!)
            } finally {
                Log.e("cancelJop", "finally")
                callBack?.onRequestIsSuccess2(null)
            }
        }

    }

    suspend fun queryTextToSpeechAPI(text: String): ByteArray? {
        // استدعاء API لتحويل النص إلى صوت
//        delay(3000) // محاكاة وقت الاستجابة
        return queryTextToSpeech(text) // إرجاع البيانات الصوتية

    }
    suspend fun playAudio(filePath: String, exoPlayer: ExoPlayer, semaphore: Semaphore,callback: ICustomPlayerListener<ExoPlayer>) {
        withContext(Dispatchers.Main) {
            semaphore.withPermit {
                var listener= object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        try {

                            when (playbackState) {
                                ExoPlayer.STATE_IDLE -> {
                                    println("ExoPlayer.STATE_IDLE     -")
                                }
                                ExoPlayer.STATE_BUFFERING -> {
                                    println("ExoPlayer.STATE_BUFFERING     -")
                                }
                                ExoPlayer.STATE_READY -> {

                                    println("ExoPlayer.STATE_READY     -")
                                }
                                ExoPlayer.STATE_ENDED -> {
                                    println("ExoPlayer.STATE_ENDED     -")
                                    callback.onCompletionListener(exoPlayer)
                                }
                                else -> {
                                    println("ExoPlayer.UNKNOWN_STATE     -")
                                }

                            }
                        } catch (e: Exception) {

                        }

                    }
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("ExoPlayer", "Error occurred: ${error.message}")
                        exoPlayer?.release()
                        callback?.onErrorListener(exoPlayer!!)
                    }
                }
                exoPlayer.setMediaItem(MediaItem.fromUri(filePath))
                exoPlayer.prepare()
                exoPlayer?.addListener(listener)
                exoPlayer.playWhenReady = true
                delay(1000)
                delay(getRemainingTime(exoPlayer))
            }

//                delayWhilePlaying(exoPlayer)
//                exoPlayer?.start(filePath, object : ICustomPlayerListener<ExoPlayer> {
//                    override fun onErrorListener(mp: ExoPlayer?) {
//                    }
//                    override fun onCompletionListener(mp: ExoPlayer?) {
//                    }
//
//                } )
        }
    }
    private fun playAudio2(filePath: String, exoPlayer: ExoPlayer, semaphore: Semaphore, callback: ICustomPlayerListener<ExoPlayer>):Player.Listener? {
                Log.d("StartSpeakerAudio","$filePath")
                var listener=object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                try {
                    when (playbackState) {
                        ExoPlayer.STATE_IDLE -> {
                            println("ExoPlayer.STATE_IDLE     -")
                        }
                        ExoPlayer.STATE_BUFFERING -> {
                            println("ExoPlayer.STATE_BUFFERING     -")
                        }
                        ExoPlayer.STATE_READY -> {
                            println("ExoPlayer.STATE_READY     -")
                        }
                        ExoPlayer.STATE_ENDED -> {
                            println("ExoPlayer.STATE_ENDED     90-")
                                    callback?.onCompletionListener(exoPlayer!!)
                        }
                        else -> {
                            println("ExoPlayer.UNKNOWN_STATE     -")
                        }

                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }

            }
            override fun onPlayerError(error: PlaybackException) {
                Log.e("ExoPlayer", "Error occurred: ${error.message}")
//                        exoPlayer?.release()
                callback?.onErrorListener(exoPlayer!!)
            }
        }
                exoPlayer.setMediaItem(MediaItem.fromUri(filePath))
                exoPlayer.prepare()
                exoPlayer?.addListener(listener)
                exoPlayer.playWhenReady = true
                return  listener
    }
    @SuppressLint("UnsafeOptInUsageError")
    private fun playExoPlayer(filePath: String, exoPlayer: ExoPlayer, callback: ICustomPlayerListener<ExoPlayer>):Player.Listener? {
        Log.d("StartSpeakerAudio","$filePath")

        var listener=object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                Log.d("onMediaItemTransition","${mediaItem?.mediaId!!} - ${reason}")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                try {
                    when (playbackState) {
                        ExoPlayer.STATE_IDLE -> {
                            println("ExoPlayer.STATE_IDLE     -")
                        }
                        ExoPlayer.STATE_BUFFERING -> {
                            println("ExoPlayer.STATE_BUFFERING     -")
                        }
                        ExoPlayer.STATE_READY -> {
                            println("ExoPlayer.STATE_READY     -")
                        }
                        ExoPlayer.STATE_ENDED -> {
                            println("ExoPlayer.STATE_ENDED     90-")
                                    callback?.onCompletionListener(exoPlayer!!)
                        }
                        else -> {
                            println("ExoPlayer.UNKNOWN_STATE     -")
                        }

                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }

            }
            override fun onPlayerError(error: PlaybackException) {
                Log.e("ExoPlayer", "Error occurred: ${error.message}")
//                        exoPlayer?.release()
                callback?.onErrorListener(exoPlayer!!)
            }
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(filePath))
        exoPlayer?.skipSilenceEnabled=true
        exoPlayer.prepare()
        exoPlayer?.addListener(listener)
        exoPlayer.playWhenReady = true
        return listener
    }
    @SuppressLint("UnsafeOptInUsageError")
    private fun playExoPlayer(filePath: String, exoPlayer: ExoPlayer,isComplete:Boolean, callback: ICustomPlayerListener<ExoPlayer>):Player.Listener? {
        Log.d("StartSpeakerAudio","$filePath")

        var listener=object : Player.Listener {


            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    ExoPlayer.STATE_READY -> {
                            println("ExoPlayer.STATE_READY     -")
                    }Player.STATE_ENDED-> {
                        // قم بتنفيذ الكود الخاص بانتهاء التشغيل هنا
                        Log.d("onCompletionListener", "Playback completed")
                        callback?.onCompletionListener(exoPlayer!!, isComplete)
                    }

                }
//                try {
////                    when (playbackState) {
////                        ExoPlayer.STATE_IDLE -> {
////                            println("ExoPlayer.STATE_IDLE     -")
////                        }
////                        ExoPlayer.STATE_BUFFERING -> {
////                            println("ExoPlayer.STATE_BUFFERING     -")
////                        }
////                        ExoPlayer.STATE_READY -> {
////                            println("ExoPlayer.STATE_READY     -")
////                        }
////                        ExoPlayer.STATE_ENDED -> {
////                            println("ExoPlayer.STATE_ENDED     90-")
////                            callback?.onCompletionListener(exoPlayer!!,isComplete)
////                        }
////                        else -> {
////                            println("ExoPlayer.UNKNOWN_STATE     -")
////                        }
////
////                    }
//                }catch (e:Exception){
//                    e.printStackTrace()
//                }

            }
            override fun onPlayerError(error: PlaybackException) {
                Log.e("ExoPlayer", "Error occurred: ${error.message}")
//                        exoPlayer?.release()
                callback?.onErrorListener(exoPlayer!!,error)
            }
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(filePath))
        exoPlayer?.skipSilenceEnabled=true
        exoPlayer.prepare()
        exoPlayer?.addListener(listener)
        exoPlayer.playWhenReady = true
        return listener
    }
    private suspend fun playAudio3(filePath: String, exoPlayer: ExoPlayer) {
        Log.d("StartSpeakerAudio","$filePath")
        exoPlayer.setMediaItem(MediaItem.fromUri(filePath))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        waitForExoPlayerCompletion(exoPlayer!!)


    }
    suspend fun delayWhilePlaying(exoPlayer: ExoPlayer) {
        while (exoPlayer.isPlaying) {
            delay(500)
        }
    }
    @SuppressLint("SuspiciousIndentation")
    fun CoroutineScope.handleTextStream(textFlow: Flow<String>, semaphore: Semaphore, exoPlayer: ExoPlayer) {
        val audioFileChannel = Channel<String>(Channel.UNLIMITED)

        launch(Dispatchers.IO) {
            textFlow.collect { text ->
                launch {
                    Log.d("TextStream",text)
                    val audioBytes = queryTextToSpeechAPI(text)?:return@launch
                    semaphore.withPermit {
                    Log.d("queryTextToSpeech","${audioBytes.size}")
                    val filePath = saveIntoTempFile(audioBytes)
                        audioFileChannel.send(filePath!!)
                    }

                }
            }
        }

        launch(Dispatchers.IO) {
            for (filePath in audioFileChannel) {
                playAudio(filePath, exoPlayer, semaphore,object:ICustomPlayerListener<ExoPlayer>{
                    override fun onErrorListener(mp: ExoPlayer?) {
                        TODO("Not yet implemented")
                    }

                    override fun onCompletionListener(mp: ExoPlayer?) {
                        TODO("Not yet implemented")
                    }

                })
                delay(1000)
//                withContext(Dispatchers.Main){
//                    delay(getRemainingTime(exoPlayer)?:1000)
//                }

            }
        }
    }
    private fun getRemainingTime(player: ExoPlayer): Long {
        val duration = player?.duration ?:0
        val currentPosition = player?.currentPosition ?:0
        return if (duration != C.TIME_UNSET) {
            duration!! - currentPosition!!
        } else {
            1000
        }
    }
    fun main() {
        val exoPlayer =  ExoPlayer.Builder(context).build()
        val semaphore = Semaphore(1)
        val textFlow: Flow<String> = getTextFlow() // استبدل هذا بالدفق النصي الفعلي

        CoroutineScope(Dispatchers.IO).launch {
            handleTextStream(textFlow, semaphore, exoPlayer)
        }
    }
    fun getTextFlow(): Flow<String> {
        // إنشاء دفق نصي تجريبي
        return flow {
            emit("النص الأول")
            delay(1000)
            emit("النص الثاني")
            delay(1000)
            emit("النص الثالث")
            delay(1000)
            emit("النص الرابع")
            delay(1000)
            emit("النص الخامس")
        }
    }

//    private suspend fun handleAudioBytes(, callBack: IWasmServiceEventListener?) {
//
//        callBack?.stopListener()
//
//        Log.d("handleAudioBytes", "$audioPlayerIsComplete")
//        try {
//            if (audioBytes == null || audioBytes!!.isEmpty()) {
//                throw Exception()
//            }
//            val filePath = saveIntoTempFile(audioBytes)
//            if (filePath != null) {
//                Log.d("filePath", filePath)
//                withContext(Dispatchers.Main) {
//                    exoPlayer?.start(filePath, object : ICustomPlayerListener<ExoPlayer> {
//                        override fun onErrorListener(mp: ExoPlayer?) {
//                            try { exoPlayer?.stop() }
//                            finally {
//                                CoroutineScope(Dispatchers.Main).launch {
//                                    semaphore.withPermit {
//                                        if (audioPlayerIsComplete) {
//                                            callBack?.onRequestIsSuccess2(stopMediaPlayer)
//                                        }
//                                    }
//                                }
//                            }
//                        }
//
//                        override fun onCompletionListener(mp: ExoPlayer?) {
//
//                            CoroutineScope(Dispatchers.Main).launch {
//                                semaphore.withPermit {
//                                    try { exoPlayer?.stop() }
//                                    finally {
//                                        CoroutineScope(Dispatchers.Main).launch {
//                                            semaphore.withPermit {
//                                                if (audioPlayerIsComplete) {
//                                                    callBack?.onRequestIsSuccess2(stopMediaPlayer)
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    })
//                }
//            } else {
//                throw Exception()
//            }
////        } catch (e:PlaybackException) {
////            e.printStackTrace()
////            callBack?.onRequestIsFailure("ERROR: ${e.message}")
////        }catch (e:EmptyStackException) {
////            e.printStackTrace()
//////            callBack?.onRequestIsFailure("ERROR: ${e.message}")
////        }catch (e:FileNotFoundException) {
////            e.printStackTrace()
////            callBack?.onRequestIsFailure("ERROR: ${e.message}")
////        }
//        }catch (e:Exception) {
//            e.printStackTrace()
//            callBack?.onRequestIsSuccess2(stopMediaPlayer)
////            callBack?.onRequestIsFailure("ERROR:: ${e.message}")
//        }
//    }

    private   fun queryTextToSpeech3(input: String): ByteArray?  {


        return try {

            val model= StorageSpeechModels.getModel(context)?:null
            var model_name=if(model!=null) model?.model?:"" else API_URL_ACTION_DEFAULT;
            Log.d("model",model_name)
            if(model_name?.isNullOrEmpty()==true)
                throw Exception("Speech Model is Error");

            var apiUrl="${BASE_API_URL}${model_name}"
            Log.d("URL",apiUrl)
            val authorization =AUTHORIZATION

            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", authorization)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonInputString = "{\"inputs\": \"$input\"}"

            conn.outputStream.use { os ->
                val inputBytes = jsonInputString.toByteArray(Charsets.UTF_8)
                os.write(inputBytes, 0, inputBytes.size)
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { it.readBytes() }
            } else {
                Log.e("WasmApiAudio", "Failed with HTTP response code: $responseCode")
                throw java.lang.Exception("Failed with HTTP response code: $responseCode")
            }
//            conn.inputStream.use { it.readBytes() }
//        } catch (e: ConnectException) {
//            e.printStackTrace()
//            throw  ConnectException()
//        } catch (e: FileNotFoundException) {
//            e.printStackTrace()
//            throw FileNotFoundException()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("WasmApiAudio",e.message.toString())
            throw java.lang.Exception(e.message.toString())
        }
    }
    private   fun queryTextToSpeech(input: String): ByteArray?  {

        return try {

//            val model= StorageSpeechModels.getModel(context)?:null
//            var model_name=API_URL_ACTION_DEFAULT //if(model!=null) model?.model!! else API_URL_ACTION_DEFAULT;

            var apiUrl="${BASE_API_URL}${API_URL_ACTION_DEFAULT}"
            val authorization =AUTHORIZATION
            Log.d("apiUrl",apiUrl)
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", authorization)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonInputString = "{\"inputs\": \"$input\"}"

            conn.outputStream.use { os ->
                val inputBytes = jsonInputString.toByteArray(Charsets.UTF_8)
                os.write(inputBytes, 0, inputBytes.size)
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                 conn.inputStream.use { it.readBytes() }
            } else {
                Log.e("WasmApiAudio", "Failed with HTTP response code: $responseCode")
                null
            }
//            conn.inputStream.use { it.readBytes() }
//        } catch (e: ConnectException) {
//            e.printStackTrace()
//            throw  ConnectException()
//        } catch (e: FileNotFoundException) {
//            e.printStackTrace()
//            throw FileNotFoundException()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("WasmApiAudio",e.message.toString())
           null
        }
    }
    // 4. تهيئة ExoPlayer واستخدام MediaSource
    @SuppressLint("UnsafeOptInUsageError")
    private fun initializePlayer(context: Context, file: File): ExoPlayer {
        val player = ExoPlayer.Builder(context).build()
        val mediaSource = createMediaSource(file)
        player.setMediaSource(mediaSource)
        player?.addListener(exoPlayer?.playbackStateListener( null, object : ICustomPlayerListener<ExoPlayer> {
                override fun onErrorListener(mp: ExoPlayer?) {
                    try {
                        exoPlayer?.stop()
                    } finally {
                        latch.countDown()
                    }
                }
                override fun onCompletionListener(mp: ExoPlayer?) {
                    try {
                        exoPlayer?.stop()
                    } finally {
                        latch.countDown()
                    }
                }
        })!!)
        player.prepare()
        return player
    }
    // 3. إنشاء MediaSource باستخدام DataSourceFactory
    @SuppressLint("UnsafeOptInUsageError")
    private fun createMediaSource(file: File): ProgressiveMediaSource {
        val dataSourceFactory = FileStreamDataSourceFactory(file)
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.fromFile(file)))
    }
    // Extension function for SemaphoreSlim to use with coroutines
    private suspend fun <T> Semaphore.withPermit(action: suspend () -> T): T {
        acquire()
        try {
            return action()
        } finally {
            release()
        }
    }
    private var stopExoPlayer=object : IBaseCallbackListener<Any?> {
        override fun onCallBackExecuted(item: Any?) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    semaphore.withPermit {
                        if(myExoPlayer!=null && myExoPlayer?.isPlaying==true)
                             myExoPlayer?.stop()
                    }
                }catch (e:Exception){

                }
            }
        }
    }
    private var stopMediaPlayer=object : IBaseCallbackListener<Any?> {
        override fun onCallBackExecuted(item: Any?) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    semaphore.withPermit {
                        exoPlayer?.stop()
                    }
                }catch (e:Exception){

                }
            }
        }
    }

    private suspend fun saveIntoTempFile(audioBytes:ByteArray):String?{
        var tempFile=createTempFile(0,audioBytes)
        if(tempFile!=null && tempFile.length()>0 && tempFile.canRead())
            return  tempFile.absolutePath
        return  null
    }
    private fun containsLetters(input: String): Boolean {
        val regex = Regex("\\p{L}")
        return regex.containsMatchIn(input)
    }
    private fun removeSymbols(input: String): String? {
        if(input==null) return  null
//        val regex = Regex("[^\\p{L}\\p{N}]")
        val regex = Regex("[^\\p{L}\\p{N}\\s]")
        //Regex("[^\\p{L}\\p{N}\\s]")
        var text=regex.replace(input, "")
       return text?.trim()?.replace("*", "")?:null
    }
    private fun fetchAudioBytes(_url:String): ByteArray? {
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
    private suspend fun query(input: String): ByteArray?= withContext(Dispatchers.IO)  {
        val apiUrl = API_URL
        val authorization =AUTHORIZATION

        return@withContext try {
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", authorization)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonInputString = "{\"inputs\": \"$input\"}"

            conn.outputStream.use { os ->
                val inputBytes = jsonInputString.toByteArray(Charsets.UTF_8)
                os.write(inputBytes, 0, inputBytes.size)
            }

            conn.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private suspend fun generateText(inputText: String?)
            : String? = withContext(Dispatchers.IO) {
        if (inputText != null) {
            var response=geminiApiClient?.sendMessage("$inputText يجب ان تكون اجابتك دقيقة ومختصرة وان لا تتعدا سطر واحد ويجب ان تكون  الاجابة باللغة العربية   ");
            if(response!=null)
                return@withContext response
        }
        return@withContext null
    }
    private suspend fun generateText2(inputText: String?): String? {
        if (inputText != null) {
            var response=geminiApiClient?.sendMessage(inputText);
            if(response!=null)
                return response
        }
        return null
    }
    private suspend fun messageToGeneratorAudioFromWasm(inputText: String?)
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

}
