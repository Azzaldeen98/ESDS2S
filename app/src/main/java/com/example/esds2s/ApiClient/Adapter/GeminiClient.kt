package com.example.esds2s.ApiClient.Adapter


import android.annotation.SuppressLint
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Interface.IListenerStream
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class GeminiApiClient() {

    private val model:GenerativeModel
    private val chat : Chat
    private  val docs=" يجب ان تكون اجابتك دقيقة ومختصرة وان لا تتعدا سطر واحد ويجب ان تكون  الاجابة باللغة العربية"
    //    var chatHistory : listOf<Content>;
    val chatHistory = listOf(
        content("user") {
            text("السلام عليكم اريد منك ان ترد على اسئلتي  دائما باللهجة السعودية النجدية ")
        },
        content("model") {
            text("هلا ومرحبا،  اسأل يا حبيبي  و عس")
        },
        content("user") {
            text("\"كيف حالك اخبارك\\")
        },
        content("model") {
            text("والله الحمد لله تمام،  وأنت وش اخبارك؟ عساك طيب؟ \n")
        },
        content("user") {
            text("اريد ايضا ان تكون اجابتك مختصره على سبيل المثال اكثر اجابة سطرين\\")
        },
        content("user") {
            text(" يجب ان تنتهي  كل جملة في النص  بنقطة  \\")
        },
        content("model") {
            text("طيب يا حبيبي،  انا  جاهز ، اسأل  وراح أختصر لك  قدر  المستطاع . \n")
        },
        content("user") {
            text("كيف  علومك")
        },
        content("model") {
            text("زين الحمد لله، وانت عساك طيب؟ \n")
        },
    )
    init {

            model = GenerativeModel(
                "gemini-1.5-flash",
                "AIzaSyBr66VW5NcNyxCJ92EZj-Pgeef1CWS8Bvk",
                generationConfig = generationConfig {
                    temperature = 1f
                    topK = 64
                    topP = 0.95f
                    maxOutputTokens = 8192
                    responseMimeType = "text/plain"
                },
                // safetySettings = Adjust safety settings
                // See https://ai.google.dev/gemini-api/docs/safety-settings
            )
            chat= model.startChat(chatHistory)
    }

    @SuppressLint("SuspiciousIndentation")
    suspend fun sendMessage(text:String): String? {
        val response = chat?.sendMessage("$text $docs")
//        println(response?.text)
//        println(response?.candidates?.first()?.content?.parts?.first()?.asTextOrNull())
        return  response?.text;
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
    fun splitTextIntoChunks(text: String, chunkSize: Int): List<String> {
        val chunks = mutableListOf<String>()
        var startIndex = 0
        while (startIndex < text.length) {
            val endIndex = (startIndex + chunkSize).coerceAtMost(text.length)
            chunks.add(text.substring(startIndex, endIndex))
            startIndex = endIndex
        }
        return chunks
    }

    suspend fun sendMessageStream(text:String, callBack: IListenerStream<String>) {
        var isCallBackReader = false;
        val responseBuilder = StringBuilder()
        var index = 0;
        chat?.let { it ->

            val responseFlow: Flow<GenerateContentResponse> = it.sendMessageStream(text)
            responseFlow.flowOn(Dispatchers.IO)
                .onCompletion { cause ->
                    if (cause == null) {
                        if (!isCallBackReader && responseBuilder?.toString()?.trim()
                                ?.isNotEmpty() == true
                        ) {
                            callBack.onStreamReader(responseBuilder.toString().trim(), index++, 0)
                            responseBuilder.clear()
                        }
                        callBack.onStreamReader(ContentApp.END_SYMBOL, index, index)
                        println("responseFlow: Completed successfully")
//                            callBack.onStreamComplete(index++)
                    } else {
                        println("responseFlow: Completed with error: ${cause.message}")
                        callBack.onStreamError(cause) // استدعاء دالة للتعامل مع الخطأ
                    }
                }.collect { response ->
                    val content = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()
                        ?.asTextOrNull()
                    content?.trim().let { it ->
                        if (!it.isNullOrEmpty()) {
                            isCallBackReader = false
                            var text = it.replace("*", "")
                            text = text.replace(Regex("\\s+"), " ")
                            responseBuilder.append(text).append(" ")
                            if (isEndOfSentence(responseBuilder.toString()) || responseBuilder.length >= 50) {
                                var lines = spiltSentence(responseBuilder.toString())
                                lines?.filter { it.isNotEmpty() }
                                    .let {
                                        var line = ""
                                        it?.forEach { sentence ->
                                            line += "$sentence ";
                                            if (line?.length!! >= 10) {
                                                isCallBackReader = true
                                                callBack.onStreamReader(line, index++, 0)
                                                responseBuilder.clear()
                                                line = ""
                                            } else {
                                                isCallBackReader = false
                                            }
                                        }

                                    }
                            }
                        }
                    }
                    delay(200)
                }
        }
    }
    suspend fun sendMessageStream3(text:String, callBack: IListenerStream<String>)  {
        var isCallBackReader = false;
        val responseBuilder = StringBuilder()
        var index=0;
        chat?.let { it ->

            val responseFlow: Flow<GenerateContentResponse> = it.sendMessageStream(text)
            responseFlow.flowOn(Dispatchers.IO)
                .onCompletion { cause ->
                    if (cause == null) {
                        if (!isCallBackReader && responseBuilder?.toString()?.trim()?.isNotEmpty()==true) {
//                                isCallBackReader=true
//                             println("responseFlow Completed[${count++}]: ${responseBuilder.append(it).toString().trim()}")
                            callBack.onStreamReader(responseBuilder.toString().trim(),index++,0)
                            responseBuilder.clear()
                        }
                        callBack.onStreamReader(ContentApp.END_SYMBOL,index,index)
                        println("responseFlow: Completed successfully")
//                            callBack.onStreamComplete(index++)
                    } else {
//                        println("responseFlow: Completed with error: ${cause.message}") // التعامل مع الأخطاء إن وجدت
                        callBack.onStreamError(cause) // استدعاء دالة للتعامل مع الخطأ
                    }
                }
                .collect { response ->
                    val content = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.asTextOrNull()
                    content?.trim().let { it ->
                        if (!it.isNullOrEmpty()) {
                            isCallBackReader = false
                            var text=it.replace("*","")
                            text=text.replace(Regex("\\s+"), " ")
                            responseBuilder.append(text).append(" ")
                            if (isEndOfSentence(responseBuilder.toString()) || responseBuilder.length >= 50) {
                                var lines = spiltSentence(responseBuilder.toString())
                                lines?.filter { it.isNotEmpty() }
                                    .let {
                                        var line=""
                                        it?.forEach { sentence ->
                                            line+= "$sentence ";
                                            if(line?.length!!>=10){
                                                isCallBackReader = true
                                                callBack.onStreamReader(line,index++,0)
                                                responseBuilder.clear()
                                                line=""
                                            }else{
                                                isCallBackReader = false
                                            }
                                        }

                                    }

//                                    println("responseFlow: ${responseBuilder.toString().trim()}")

//                                    val chunks = splitTextIntoChunks(responseBuilder.toString(), 50)
//                                    chunks.forEach {
//
//                                        if(it?.length!!<50){
//                                            isCallBackReader = false
//                                            responseBuilder.clear()
//                                            responseBuilder.append(it)
//                                        }else{
//                                            isCallBackReader = true
//                                            callBack.onStreamReader(it,index++,0)
//                                            responseBuilder.clear()
//                                        }
//                                    }





                            }
//                                if (!text.lastOrNull()!!.isLetter() || text.length > 10)
//                                    responseBuilder.append(" ")

//                                if (!text.lastOrNull()!!.isLetter() || text.length > 50) {
//                                    isCallBackReader = true
//                                    callBack.onStreamReader(text, index++, 0)
//                                }
//


                        }
//
////                            else if (it.lineSequence().toList().isNotEmpty()) {
////                                for (item in it.lineSequence()) {
////                                    isCallBackReader = true
////                                    println("Line:$item")
////                                    callBack.onStreamReader(item)
//////                                    delay(100)
////                                }
////                            }
//////                            else {
//////                                isCallBackReader = false
////////                                println("responseFlow: ${it}")
//////                                responseBuilder.append(it)
//////                                if (!it.lastOrNull()!!.isLetter())
//////                                    responseBuilder.append(" ")
//////                            }
//
////                            val lines = it.split("\n")
////                            for (line in lines) {
////                                if (line.isNotBlank()) {
////                                    println("responseFlow: $line") // Print each line for debugging
////                                    callBack.onStreamReader(line)
////                                }
////                            }
////                    .scan("") { accumulatedText, newText ->
////                        val updatedText = if (accumulatedText.isNotEmpty()) "$accumulatedText $newText" else newText?.toString()
////                        if (isEndOfSentence(updatedText)) {
////                            updatedText.trim() // نص مكتمل
////                        } else {
////                            updatedText // النص غير مكتمل
////                        }
////                    }
////                    .filter { it.isNotEmpty() }
                    }
                }
        }
    }
    suspend fun sendMessageStream2(text:String, callBack: IListenerStream<String>)  {
        var isCallBackReader = false;
        val responseBuilder = StringBuilder()
        var count=0;

        chat?.let {
            val responseFlow: Flow<GenerateContentResponse> = it.sendMessageStream(text)
            responseFlow.flowOn(Dispatchers.IO) // Ensure flow is collected on IO dispatcher
                .onCompletion { cause ->
                    if (cause == null) {
                        if (!isCallBackReader && responseBuilder.isNotEmpty()) {
//                            println("responseFlow Completed[${count++}]: ${responseBuilder.append(it).toString().trim()}")
                            callBack.onStreamReader(responseBuilder.toString().trim())
                            responseBuilder.clear()
                        }
                        println("responseFlow: Completed successfully")
                        callBack.onStreamComplete()
                    } else {
//                        println("responseFlow: Completed with error: ${cause.message}") // التعامل مع الأخطاء إن وجدت
                        callBack.onStreamError(cause) // استدعاء دالة للتعامل مع الخطأ
                    }
                }
                .collect { response ->
                    for (i in 0 until 10) {
                        callBack.onStreamReader("${count++}")
                    }
//                        yield()
//                        val content = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.asTextOrNull()
//                        content?.trim().let { it ->
//                            if (!it.isNullOrEmpty()) {
//                                isCallBackReader = false
//                                responseBuilder.append(it)
//                                if (!it.lastOrNull()!!.isLetter() || it.length > 10)
//                                    responseBuilder.append(" ")
//
//                                if (isEndOfSentence(responseBuilder.toString()) || responseBuilder.length >= 50) {
//                                    isCallBackReader = true
//                                    println("responseFlow: ${responseBuilder.toString().trim()}")
//                                    var lines = spiltSentence(responseBuilder.toString())
//                                    lines?.filter { it.isNotEmpty() }
//                                        .let {
//                                            it?.forEach { sentence ->
//                                                println("Sentence:${sentence}");
//                                                sentence.trim()?.isNotEmpty().let {
//                                                    callBack.onStreamReader(sentence)
////                                                delay(100)
//                                                }
//                                            }
//                                            responseBuilder.clear()
////                                        delay(100)
//                                        }
////                                delay(100)
//                                }
////                            else {
////                                isCallBackReader = false
//////                                println("responseFlow: ${it}")
////                                responseBuilder.append(it)
////                                if (!it.lastOrNull()!!.isLetter())
////                                    responseBuilder.append(" ")
////                            }
//                            }
//
////                            else if (it.lineSequence().toList().isNotEmpty()) {
////                                for (item in it.lineSequence()) {
////                                    isCallBackReader = true
////                                    println("Line:$item")
////                                    callBack.onStreamReader(item)
//////                                    delay(100)
////                                }
////                            }
//
//
////                            val lines = it.split("\n")
////                            for (line in lines) {
////                                if (line.isNotBlank()) {
////                                    println("responseFlow: $line") // Print each line for debugging
////                                    callBack.onStreamReader(line)
////                                }
////                            }
////                    .scan("") { accumulatedText, newText ->
////                        val updatedText = if (accumulatedText.isNotEmpty()) "$accumulatedText $newText" else newText?.toString()
////                        if (isEndOfSentence(updatedText)) {
////                            updatedText.trim() // نص مكتمل
////                        } else {
////                            updatedText // النص غير مكتمل
////                        }
////                    }
////                    .filter { it.isNotEmpty() }
//                        }
                }
        }
    }
    suspend fun sendMessageStreamTest(text:String, callBack: IListenerStream<String>)  {
        var isCallBackReader = false;
        val responseBuilder = StringBuilder()
        var index=0;

            val responseFlow=getTextFlow()
            responseFlow.flowOn(Dispatchers.IO) // Ensure flow is collected on IO dispatcher
                .onCompletion { cause ->
                    if (cause == null) {
                        callBack.onStreamReader(ContentApp.END_SYMBOL,index,index++)
                        println("responseFlow: Completed successfully")
                        callBack.onStreamComplete()
                    } else {
                        println("responseFlow: Completed with error: ${cause.message}") // التعامل مع الأخطاء إن وجدت
                        callBack.onStreamError(cause) // استدعاء دالة للتعامل مع الخطأ
                    }
                }
                .collect { response ->
//                        for (i in 0 until 10) {
//                            callBack.onStreamReader("${count++}")
//                        }
//                        yield()
                    val content = response//.firstOrNull()?.content?.parts?.firstOrNull()?.asTextOrNull()
                    content?.trim().let { it ->
                        if (!it.isNullOrEmpty()) {
                            isCallBackReader = true
                            callBack.onStreamReader(it,index++,0)
//                            responseBuilder.append(it)
//                            if (!it.lastOrNull()!!.isLetter() || it.length > 10)
//                                responseBuilder.append(" ")
//
//                            if (isEndOfSentence(responseBuilder.toString()) || responseBuilder.length >= 50) {
//                                isCallBackReader = true
//                                println("responseFlow: ${responseBuilder.toString().trim()}")
//                                var lines = spiltSentence(responseBuilder.toString())
//                                lines?.filter { it.isNotEmpty() }
//                                    .let {
//                                        it?.forEach { sentence ->
//                                            println("Sentence:${sentence}");
//                                            sentence.trim()?.isNotEmpty().let {
//                                                callBack.onStreamReader(sentence,index++)
////                                                delay(100)
//                                            }
//                                        }
//                                        responseBuilder.clear()
////                                        delay(100)
//                                    }
////                                delay(100)
//                            }
                        }

                    }
//
////                            else if (it.lineSequence().toList().isNotEmpty()) {
////                                for (item in it.lineSequence()) {
////                                    isCallBackReader = true
////                                    println("Line:$item")
////                                    callBack.onStreamReader(item)
//////                                    delay(100)
////                                }
////                            }
//////                            else {
//////                                isCallBackReader = false
////////                                println("responseFlow: ${it}")
//////                                responseBuilder.append(it)
//////                                if (!it.lastOrNull()!!.isLetter())
//////                                    responseBuilder.append(" ")
//////                            }
//
////                            val lines = it.split("\n")
////                            for (line in lines) {
////                                if (line.isNotBlank()) {
////                                    println("responseFlow: $line") // Print each line for debugging
////                                    callBack.onStreamReader(line)
////                                }
////                            }
////                    .scan("") { accumulatedText, newText ->
////                        val updatedText = if (accumulatedText.isNotEmpty()) "$accumulatedText $newText" else newText?.toString()
////                        if (isEndOfSentence(updatedText)) {
////                            updatedText.trim() // نص مكتمل
////                        } else {
////                            updatedText // النص غير مكتمل
////                        }
////                    }
////                    .filter { it.isNotEmpty() }
//                        }
                }

    }
     fun sendMessageStreamLastTest(text:String):Flow<GenerateContentResponse>  {
        var isCallBackReader = false;
        val responseBuilder = StringBuilder()
        var index=0;
        val response: Flow<GenerateContentResponse> = chat.sendMessageStream(text)
        return response
    }
    fun isWordComplete(text: String): Boolean {
        val lastChar = text.lastOrNull()
        return lastChar != null && !lastChar.isLetter()
    }

        private fun spiltSentence(text: String): List<String> {
//            var txt=text.replace(Regex("\\s+"), " ")
             return text.split(Regex("[،!؟?,.]"))
        }
//    suspend fun sendMessageStreamFlow(text:String){
////        text.chunked(10) // Break text into chunks of 10 characters
////            .forEach { chunk ->
////                emit(chunk) // Emit each chunk as a flow element
////                delay(100) // Simulate delay between chunks
////            }
//        chat?.let {
//
//            var isCallBackReader=false;
//            val responseBuilder = StringBuilder()
//            // $docs
//                val responseFlow: Flow<GenerateContentResponse> = chat.sendMessageStream("$text ")
//            responseFlow.flowOn(Dispatchers.IO)
//                .scan("") { accumulatedText, newText ->
//                    val updatedText = "$accumulatedText $newText"
//                    if (isEndOfSentence(updatedText)) {
//                        println("Complete sentence: $updatedText")
//                        "" // Reset for the next sentence
//                    } else {
//                        updatedText
//                    }
//                }
//                .filter { it.isNotEmpty() }
//                .collect { completeSentence ->
//                    // Process complete sentence here
//                    completeSentence?.trim().let {
//
//                            if(!it.isNullOrEmpty()) {
//                                if (isEndOfSentence(it)) {
//                                    isCallBackReader = true
//                                    println("isEndOfSentence: ${responseBuilder.append(it).toString().trim()}")
//                                    responseBuilder.clear()
//                                    delay(100)
//                                }
//                                else if (it.lineSequence().toList().isNotEmpty()) {
//                                    for (item in it.lineSequence()) {
//                                        isCallBackReader=true
//                                        println("Line:$item")
////                                        responseBuilder.append(it)
//                                        delay(100)
//                                    }
//                                }
//                                else {
//                                    isCallBackReader = false
//                                    println("responseFlow: ${it}")
//                                    responseBuilder.append(it) // النص غير مكتمل
//                                }
//                            }
//                }
////                responseFlow.flowOn(Dispatchers.IO) // Ensure flow is collected on IO dispatcher
////                    .onCompletion { cause ->
////                        if (cause == null) {
////                            println("isCallBackReader: $isCallBackReader ")
////                            if(!isCallBackReader && responseBuilder.isNotEmpty()) {
////                                responseBuilder.clear()
////                            }
////
//////                                callBack.onStreamReader("####")
////
////                            println("responseFlow: Completed successfully") // أو أي إجراء آخر عند اكتمال الدفق بنجاح
////
////                        } else {
////                            println("responseFlow: Completed with error: ${cause.message}") // التعامل مع الأخطاء إن وجدت
////
////                        }
////                    }
////                    .collect { response ->
////                        val content = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.asTextOrNull()
////                        content?.trim().let {
////
////                            if(!it.isNullOrEmpty()) {
////                                if (isEndOfSentence(it)) {
////                                    isCallBackReader = true
////                                    println("isEndOfSentence: ${responseBuilder.append(it).toString().trim()}")
////                                    responseBuilder.clear()
////                                    delay(100)
////                                }
////                                else if (it.lineSequence().toList().isNotEmpty()) {
////                                    for (item in it.lineSequence()) {
////                                        isCallBackReader=true
////                                        println("Line:$item")
//////                                        responseBuilder.append(it)
////                                        delay(100)
////                                    }
////                                } else {
////                                    isCallBackReader = false
////                                    println("responseFlow: ${it}")
////                                    responseBuilder.append(it) // النص غير مكتمل
////                                }
////                            }
////
////
//////                            val lines = it.split("\n")
//////                            for (line in lines) {
//////                                if (line.isNotBlank()) {
//////                                    println("responseFlow: $line") // Print each line for debugging
//////                                    callBack.onStreamReader(line)
//////                                }
//////                            }
//////                    .scan("") { accumulatedText, newText ->
//////                        val updatedText = if (accumulatedText.isNotEmpty()) "$accumulatedText $newText" else newText?.toString()
//////                        if (isEndOfSentence(updatedText)) {
//////                            updatedText.trim() // نص مكتمل
//////                        } else {
//////                            updatedText // النص غير مكتمل
//////                        }
//////                    }
//////                    .filter { it.isNotEmpty() }
////                        }
////                    }
////        }
//    }
//    // تحديد إذا كانت الجملة قد انتهت
    fun isEndOfSentence(text: String): Boolean {
        return text.endsWith(".") || text.endsWith("!") || text.endsWith("?") || text.endsWith("،")
                || text.endsWith(",")   || text.endsWith("؟")
    }
//

////    private suspend fun readStreamFromChat(chat: Chat, text: String,callBack:IBaseCallbackListener<String>)  {
////        //        val responseBuilder = StringBuilder()
////    }
//
}

//class GeminiApiClient {
//    private lateinit var apiKey: String
//    private lateinit var chat: ChatSession
//    private lateinit var model: GenerativeModel
//    private lateinit var visionModel: GenerativeModel
//    private val generationConfig = GenerationConfig(
//        temperature = 0.9,
//        topK = 1,
//        topP = 1,
//        maxOutputTokens = 2048
//    )
//
//    init {
//        apiKey = "AIzaSyBr66VW5NcNyxCJ92EZj-Pgeef1CWS8Bvk"
//
//        val history = listOf(
//            Content("model", listOf(TextPart("Hello! How can I help you today?".tr))),
//            Content("user", listOf(TextPart("اخبرني عن نفسك"))),
//            Content("model", listOf(TextPart("أنا مساعد افتراضي تم تدريبي لتقديم المساعدة والمعلومات حول الرعاية الصحية للأطفال الرضع والأمهات")))
//        )
//
//        val safetySettings = listOf(
//            SafetySetting(HarmCategory.sexuallyExplicit, HarmBlockThreshold.high),
//            SafetySetting(HarmCategory.hateSpeech, HarmBlockThreshold.medium),
//            SafetySetting(HarmCategory.dangerousContent, HarmBlockThreshold.medium),
//            SafetySetting(HarmCategory.harassment, HarmBlockThreshold.medium)
//        )
//
//        model = GenerativeModel(
//            model = "gemini-pro",
//            apiKey = apiKey,
//            generationConfig = generationConfig
//        )
//
//        chat = model.startChat(history = history, safetySettings = safetySettings)
//    }
//
//    suspend fun generateText(text: String): String {
//        val docs = "docs".tr
//        if (apiKey.isEmpty()) {
//            throw IllegalArgumentException("API key is missing")
//        }
//
//        val content = listOf(Content.text("$docs\n$text\n${Gemini_Model_Rules}"))
//        val response = model.generateContent(content)
//        return response.text ?: throw Exception("Null response")
//    }
//}
