package com.example.esds2s

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.esds2s.ApiClient.Controlls.ChatAiServiceControll
import com.example.esds2s.Helpers.AndroidAudioRecorder
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.Services.IUplaodAudioEventListener
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BasicChatBotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BasicChatBotFragment : Fragment() ,IUplaodAudioEventListener {

    companion object {
        val RecordAudioRequestCode: Int? = 1
        var speechRecognizer: SpeechRecognizer? = null
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BasicChatBotFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BasicChatBotFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    var file_record_Path: String?=null
    var  micButton: ImageView?=null
    var  editText: EditText?=null
    var isRecord:Boolean?=false



    private var audioPlayer: AudioPlayer? = null
    private var chatAiServiceControll: ChatAiServiceControll? = null
    private var audioRecorder: AndroidAudioRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_basic_chat_bot, container, false)
    }


    override fun onStart() {
        super.onStart()

        chatAiServiceControll= ChatAiServiceControll(this.activity)
        file_record_Path="${this.context?.externalCacheDir?.absolutePath}/audiorecordtest.3gp"  //com.example.esds2s.ApiClient.BuildConfig.AudioFilePath
        audioRecorder = AndroidAudioRecorder(this.context!!)
        audioPlayer = AudioPlayer(this.context!!)

        Toast.makeText(this.context,"onStart", Toast.LENGTH_SHORT).show()
        micButton=activity?.findViewById(R.id.micButton)
        editText=activity?.findViewById(R.id.text)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.activity)

        micButton?.setOnClickListener{v->

            if(!isRecord!!){
                micButton!!.setImageResource(R.drawable.ic_mic_black_24dp)
                Log.d("startRecorder", "Recorder....");
                audioRecorder?.start(file_record_Path!!)
            } else{
                micButton!!.setImageResource(R.drawable.ic_mic_black_off)
                micButton?.isEnabled=false;
                Log.d("stopRecorder", "Recorder....");

//                Thread {
//                    activity?.runOnUiThread {
                        try {

                            if (audioRecorder != null)
                                audioRecorder?.stop();
                            chatAiServiceControll?.uploadAudioFile(
                                file_record_Path!!,
                                this@BasicChatBotFragment.activity,
                                this@BasicChatBotFragment
                            );

//                            if (audioPlayer != null) {
//                                Log.d("audioPlayer", "Player....");
//                                audioPlayer?.start(file_record_Path)?.setOnCompletionListener { mPlayer ->
//                                    audioPlayer?.stop();
//                                }
//                            }

                        } catch (e: Exception) {
                            Log.d("Error ! ", e.message.toString())
                        }
//                    }
//                }.start()
            }
            isRecord=(!isRecord!!)
        }
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {}
            override fun onBeginningOfSpeech() {
                Log.d("Tag", "Beginning Of Speech")
                editText!!.setText("")
                editText!!.hint = "Listening..."

            }
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(bytes: ByteArray) {}
            override fun onEndOfSpeech() {

                Log.d("Tag", "End Of Speech")
            }
            override fun onError(i: Int) {

                Log.e("Tag", i.toString())
                editText!!.hint="Sorry, talking again"
            }
            override fun onResults(bundle: Bundle) {

                micButton!!.setImageResource(R.drawable.ic_mic_black_off)
                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d("Tag-Result", data!![0])
                editText!!.setText(data!![0])
//                audioRecorder?.stop()

                if(data!![0]!=null) {
                    // Start a background thread


                }

            }
            override fun onPartialResults(bundle: Bundle) {}
            override fun onEvent(i: Int, bundle: Bundle) {}
        })

//        micButton!!.setOnTouchListener { view, motionEvent ->
//            if (motionEvent.action == MotionEvent.ACTION_UP) {
//
//
////              speechRecognizer?.stopListening()
//
//            }
//            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
//                micButton!!.setImageResource(R.drawable.ic_mic_black_24dp)
//
////                Thread {
////                    activity?.runOnUiThread {
////                        try {
////                            audioRecorder?.start(file_record_Path!!)
////
////                        } catch (e: Exception) { Log.d("Error ! ", e.message.toString()) }
////                    }
////                }.start()
////                speechRecognizer?.startListening(speechRecognizerIntent)
//
//            }
//            false
//        }


    }



    override fun onUplaodAudioIsSuccess(response: GeminiResponse) {


        if(response!=null) {
            if (response?.description != null) {
                if (audioPlayer != null) {
                    Log.d("responseSuccess", "Player....");
                    audioPlayer?.start(response?.description)?.setOnCompletionListener { mPlayer ->
                        Log.e("onUplaodAudioIsSuccess", "Complate Plyer Museic");
                        audioPlayer?.stop();
                        micButton?.isEnabled=true;
                    }
                }
            }
        } else {
            // Handle unsuccessful response here
            Log.e("responseError","!! response is empty or  null");
        }

        Helper.deleteFile(file_record_Path)
    }

    override fun onUplaodAudioIsFailure(error: String) {


        try {
            micButton?.isEnabled=true;
            Helper.deleteFile(file_record_Path)
            editText?.hint="!! please try again";
        }catch (e:java.lang.Exception){}

        Log.e("onFailure",error!!);
    }


}