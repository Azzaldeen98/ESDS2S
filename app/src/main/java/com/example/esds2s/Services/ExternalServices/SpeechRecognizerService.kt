package com.example.esds2s.Services.ExternalServices

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.Interface.IGeminiServiceEventListener
import com.example.esds2s.Interface.ISpeechRecognizerServices
import com.example.esds2s.Models.ResponseModels.GeminiResponse

class SpeechRecognizerService(private val context: Context,
                              private val  callBack: IGeminiServiceEventListener,
                              private val  speechListenerCallback: ISpeechRecognizerServices)
    :IGeminiServiceEventListener{


    private lateinit var mOnErrorListener: OnErrorListener
    private var speechChatControl: SpeechChatControl? = null
    private var speechRecognizerIsListening: Boolean? = false
    private var recognizer:Boolean=true
    private var shareWithApiGenerator:Boolean=true
    private var workingInTheContinuously:Boolean=false
    private lateinit var textSpeachResult: String
    private var _lang:String="ar"
    var speechRecognizerIntent: Intent?=null
    private var speechRecognizer: SpeechRecognizer?=null
    var Language: String
        get() { return _lang }
        set(value) { _lang = value }

    fun  Initialization(_recognizer: Boolean?=true,_shareWithApiGenerator: Boolean?=true,
                        _workingInTheContinuously: Boolean?=false,lang:String?="ar")
    :SpeechRecognizerService {

        setOptions(lang!!,_recognizer!!,_shareWithApiGenerator!!,_workingInTheContinuously!!)
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            // Define the language model used for voice recognition
            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Specify the preferred language for voice recognition
            speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
            Toast.makeText(context, "Language : " + lang, Toast.LENGTH_SHORT)

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(bundle: Bundle) {
                    Toast.makeText(context, "onReadyForSpeech:", Toast.LENGTH_SHORT)
                }
                override fun onBeginningOfSpeech() {
                    Log.d("BeginningOfSpeech", "Start Speech")
                    Toast.makeText(context, "onBeginningOfSpeech:", Toast.LENGTH_SHORT)
                }
                override fun onRmsChanged(v: Float) {
                    Log.d("onRmsChanged", v.toString())
                }
                override fun onBufferReceived(bytes: ByteArray) {
                }
                override fun onEndOfSpeech() {
                    Log.d("EndOfSpeech", "End Speech")
                }
                override fun onError(i: Int) {

                    Toast.makeText(context, "onError:"+i, Toast.LENGTH_SHORT)
//                    //TODO
//                    mOnErrorListener?.onError(i)

                    when(i) {
                        //1
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
//                            mOnErrorListener?.onError(i)
                        }
                        //2
                        SpeechRecognizer.ERROR_NETWORK -> {
                            Log.d("ERROR_NETWORK", i.toString() + "")
                        }
                        //3
                        SpeechRecognizer.ERROR_AUDIO -> {
                            if(context!=null)
                                Toast.makeText(context , "Audio recording error", Toast.LENGTH_SHORT).show()
                        }
                        //4
                        SpeechRecognizer.ERROR_SERVER -> {}
                        //5
                        SpeechRecognizer.ERROR_CLIENT -> {}
                        //6
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {}
                        //7
                        SpeechRecognizer.ERROR_NO_MATCH -> {}
                        //8
                        SpeechRecognizer. ERROR_RECOGNIZER_BUSY -> {
                            Log.d("ERROR_RECOGNIZER_BUSY", i.toString() + "")
                        }
                        //9
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {}
                    }
                    if(workingInTheContinuously)
                        speechRecognizerListenAgain()

                }
                override fun onResults(bundle: Bundle) {
                    Log.e("onResults","Results")
                    val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    textSpeachResult=data!![0]
                    if(this@SpeechRecognizerService.context==null)
                        Log.e("Error","context object is null")
                    else
                        Toast.makeText(this@SpeechRecognizerService.context,textSpeachResult, Toast.LENGTH_SHORT).show()

                    if (textSpeachResult == null && data?.count()!! <=0)
                        speechRecognizerListenAgain();
                    else {
                        speechListenerCallback?.onSpeechRecognizerResults(data)

//                        if (shareWithApiGenerator) {
//                            if (textSpeachResult != null && textSpeachResult.length > 0)
//                                sendRequestToApiGenerator(textSpeachResult);
//
//                        }
                        if (workingInTheContinuously)
                            startSpeechRecognizerListening()
                        else
                            speechRecognizer?.stopListening()
                    }
                }
                override fun onPartialResults(bundle: Bundle) {}
                override fun onEvent(i: Int, bundle: Bundle) {
                    Log.d("onEvent", i.toString() + "")
                }
            })

        } catch (e: Exception) {
            Log.e("SpeechRecognizerServiceError",e.message.toString())
            Toast.makeText(context, "SpeechRecognizer:" + e.message.toString(), Toast.LENGTH_SHORT)
                .show()
        }


        // Specifies how complete silence is required for audio input to be considered complete
//        speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000);
        // Specifies the minimum amount of silence required to be considered audio input
//        speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000);
        // The amount of time that it should take after we stop hearing speech to consider the input possibly complete.
//        speechRecognizerIntent?.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);


        return  this
    }

    fun setOptions(lang:String,_recognizer: Boolean,_shareWithApiGenerator: Boolean,_workingInTheContinuously: Boolean,){
        lang.also { _lang = it }
        _recognizer.also { recognizer = it }
        _shareWithApiGenerator.also { shareWithApiGenerator = it }
        _workingInTheContinuously.also { workingInTheContinuously = it }
    }


    fun speechRecognizerListenAgain() {
        if (speechRecognizerIsListening!!) {
            speechRecognizerIsListening = false;
            speechRecognizer?.cancel();
            startSpeechRecognizerListening();
        }
    }
    fun startSpeechRecognizerListening() {
        if (speechRecognizerIsListening==false) speechRecognizerIsListening=true
        if (speechRecognizer != null && speechRecognizerIntent != null){
            Log.e("startSpeechRecognizerListening","Start")
            speechRecognizer?.startListening(speechRecognizerIntent)
        }
    }
    fun sendRequestToApiGenerator(speechText:String) {
        try {
            if (speechChatControl != null)
                speechChatControl?.messageToGeneratorAudio(speechText, this@SpeechRecognizerService);
        } catch (e: Exception) {
            Log.d("Error ! ", e.message.toString())
        }
    }

    fun setOnErrorListener(listener:OnErrorListener ){
        mOnErrorListener=listener
    }
    override fun onRequestIsSuccess(response: GeminiResponse) {

//        if(workingInTheContinuously==false)
          callBack?.onRequestIsSuccess(response)
    }
    override fun onRequestIsFailure(error: String) {
        callBack?.onRequestIsFailure(error)
    }
    fun stopSpeechRecognizer(){
        try {
            speechRecognizer?.stopListening()
        }
        catch (e: Exception) {
            Log.d("Error ! ", e.message.toString())
        }
    }
    fun destroy(){
        try {

            if(speechRecognizerIsListening!!)
                speechRecognizerIsListening=false

            if (speechRecognizer != null) {
                speechRecognizer?.stopListening();
                speechRecognizer?.cancel();
                speechRecognizer?.destroy();
                speechRecognizer=null;
            }
            speechRecognizerIntent=null;

        }
        catch (e: Exception) {
            Log.d("Error ! ", e.message.toString())
        }
    }

    /**
     * ---------------------------------------------------------
     **/

    interface OnErrorListener {

        fun onError(errorId: Int): Boolean
    }

}


//    fun startRecognitionListener(){
//        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
//            override fun onReadyForSpeech(bundle: Bundle) {
//                recListener?.onReadyForSpeech(bundle)
//            }
//            override fun onBeginningOfSpeech() {
//                recListener?.onBeginningOfSpeech()
//            }
//            override fun onRmsChanged(v: Float) {
//                recListener?.onRmsChanged(v)
//            }
//            override fun onBufferReceived(bytes: ByteArray) {
//                recListener?.onBufferReceived(bytes)
//            }
//            override fun onEndOfSpeech() {
//                onEndOfSpeech()
//                Log.d("EndOfSpeech", "End Speech")
//            }
//            override fun onError(i: Int) {
//
//                when(i) {
//                    //1
//                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {}
//                    //2
//                    SpeechRecognizer.ERROR_NETWORK -> {
//                        Log.d("ERROR_NETWORK", i.toString() + "")
//                    }
//                    //3
//                    SpeechRecognizer.ERROR_AUDIO -> {
//                        if(context!=null)
//                            Toast.makeText(context , "Audio recording error", Toast.LENGTH_SHORT).show()
//                    }
//                    //4
//                    SpeechRecognizer.ERROR_SERVER -> {}
//                    //5
//                    SpeechRecognizer.ERROR_CLIENT -> {}
//                    //6
//                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {}
//                    //7
//                    SpeechRecognizer.ERROR_NO_MATCH -> {}
//                    //8
//                    SpeechRecognizer. ERROR_RECOGNIZER_BUSY -> {
//                        Log.d("ERROR_RECOGNIZER_BUSY", i.toString() + "")
//                    }
//                    //9
//                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {}
//                }
//                if(workingInTheContinuously)
//                    speechRecognizerListenAgain()
//
//                recListener?.onError(i)
//
//            }
//            override fun onResults(bundle: Bundle) {
//                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                textSpeachResult=data!![0]
//                if(context==null)
//                    Log.e("Error","context object is null")
//                else
//                    Toast.makeText(context,textSpeachResult, Toast.LENGTH_SHORT).show()
//                if (textSpeachResult == null && data?.count()!! <=0)
//                    speechRecognizerListenAgain();
//                else
//                    speechListenerCallback?.onSpeechRecognizerResults(data)
//
//                if(shareWithApiGenerator) {
//                    if (textSpeachResult != null && textSpeachResult.length > 1)
//                        sendRequestToApiGenerator(textSpeachResult);
//
//                }
//
//            }
//            override fun onPartialResults(bundle: Bundle) {}
//            override fun onEvent(i: Int, bundle: Bundle) {
//                Log.d("onEvent", i.toString() + "")
//            }
//        })
//    }