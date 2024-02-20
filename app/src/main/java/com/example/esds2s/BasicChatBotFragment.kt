package com.example.esds2s

import android.R
import android.media.MediaPlayer
import android.os.Bundle
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
import com.example.esds2s.Interface.IUplaodAudioEventListener
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import okhttp3.internal.wait
import java.util.*
import java.util.concurrent.ThreadPoolExecutor


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BasicChatBotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BasicChatBotFragment : Fragment() , IUplaodAudioEventListener {

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
    var reorderCounter:Int?=0;
    var reply_music:MediaPlayer?=null

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

        return inflater.inflate(com.example.esds2s.R.layout.fragment_basic_chat_bot, container, false)
    }

fun sendRecordAudio()
{
//    ThreadPoolExecutor {
        activity?.runOnUiThread {
            try {
                if (audioRecorder != null)
                    audioRecorder?.stop();
                chatAiServiceControll?.uploadAudioFile(
                    file_record_Path!!,
                    this@BasicChatBotFragment.activity,
                    this@BasicChatBotFragment
                );

            } catch (e: Exception) {
                Log.d("Error ! ", e.message.toString())
            }
        }
//    }.start();

}
    override fun onStart() {
        super.onStart()

        chatAiServiceControll= ChatAiServiceControll(this.activity)
        file_record_Path="${this.context?.externalCacheDir?.absolutePath}/audiorecordtest.3gp"  //com.example.esds2s.ApiClient.BuildConfig.AudioFilePath
        audioRecorder = AndroidAudioRecorder(this.context!!)
        audioPlayer = AudioPlayer(this.context!!)

        Toast.makeText(this.context,"onStart", Toast.LENGTH_SHORT).show()
        micButton=activity?.findViewById(com.example.esds2s.R.id.micButton)
        editText=activity?.findViewById(com.example.esds2s.R.id.text)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.activity)


        micButton?.setOnClickListener{v->

            if(!isRecord!!){
                micButton!!.setImageResource(com.example.esds2s.R.drawable.ic_mic_black_24dp)
                Log.d("startRecorder", "Recorder....");
                audioRecorder?.start(file_record_Path!!)
            } else{
                micButton!!.setImageResource(com.example.esds2s.R.drawable.ic_mic_black_off)
                micButton?.isEnabled=false;
                Log.d("stopRecorder", "Recorder....");
                editText?.hint="Wait ...";

//                sendRecordAudio();
//                Thread {
//                    activity?.runOnUiThread {
//
//                        Thread.sleep(1000)
//                        val vois: Int = getAutomaticReplyVoice()!!
//                        reply_music = MediaPlayer.create(this@BasicChatBotFragment.context, vois.toInt())!!
//                        reply_music?.start()
//                        reply_music?.setOnCompletionListener { v -> { v.stop() }
//                        }
//                    }}.start()
            }

            isRecord=(!isRecord!!)
        }

    }
    fun getAutomaticReplyVoice():Int
    {
        val randomValues = Random().nextInt(7)!!
        var sound=com.example.esds2s.R.raw.res1;
        when(randomValues) {
            0 -> sound = com.example.esds2s.R.raw.res1
            1 -> sound = com.example.esds2s.R.raw.res2
            2 -> sound = com.example.esds2s.R.raw.res4
            3 -> sound = com.example.esds2s.R.raw.res5
            4 -> sound = com.example.esds2s.R.raw.res6
            5 -> sound = com.example.esds2s.R.raw.res7
        }
        return sound;
    }
    override fun onRequestIsSuccess(response: GeminiResponse) {

        reorderCounter=0;
        if(response!=null) {
            if (response?.description != null) {
                if (audioPlayer != null) {
                    if(reply_music!=null && reply_music!!.isPlaying()) {
                        reply_music!!.stop()
//                        reply_music!!.
                        Thread.sleep(1000)
                    }
                    Log.d("responseSuccess", "Player....");
                   editText?.hint="Listen ...";
                    audioPlayer?.start(response?.description)?.setOnCompletionListener { mPlayer ->
                        Log.e("onUplaodAudioIsSuccess", "Complate Plyer Museic");
                        audioPlayer?.stop();
                        micButton?.isEnabled=true;
                        editText?.hint="Speaking ...";
                    }
                }
            }
        } else {
            // Handle unsuccessful response here
            Log.e("responseError","!! response is empty or  null");
        }

        Helper.deleteFile(file_record_Path)
    }


    override fun onRequestIsFailure(error: String) {

        try {
            if(reorderCounter!!<3) {
                chatAiServiceControll?.uploadAudioFile(
                    file_record_Path!!,
                    this@BasicChatBotFragment.activity,
                    this@BasicChatBotFragment
                );
            }

//            Helper.deleteFile(file_record_Path)
//            editText?.hint="!! please try again";
        }catch (e:java.lang.Exception){}
        finally {
            if(reorderCounter!! >=3)
                micButton?.isEnabled=true;

            reorderCounter = reorderCounter?.plus(1);
        }

        Log.e("onFailure",error!!);
    }

    //        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
//        speechRecognizerIntent.putExtra(
//            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
//
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.context)
//        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
//            override fun onReadyForSpeech(bundle: Bundle) {
//
//                Log.d("Tag-onReadyForSpeech", "Beginning Of Speech")
//            }
//            override fun onBeginningOfSpeech() {
//                Log.d("Tag", "Beginning Of Speech")
//                editText!!.setText("")
//                editText!!.hint = "Listening..."
//
//            }
//            override fun onRmsChanged(v: Float) {
//                Log.d("Tag-onRmsChanged",v.toString())
//            }
//            override fun onBufferReceived(bytes: ByteArray) {
//                Log.d("Buffer_Size",bytes?.size.toString())
//
//            }
//            override fun onEndOfSpeech() {
//
//                Log.d("Tag", "End Of Speech")
//            }
//            override fun onError(i: Int) {
//
//                Log.e("Tag", i.toString())
//                editText!!.hint="Sorry, talking again"
//            }
//            override fun onResults(bundle: Bundle) {
//
//                micButton!!.setImageResource(R.drawable.ic_mic_black_off)
//                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                Log.d("Tag-Result", data!![0])
//                editText!!.setText(data!![0])
////                audioRecorder?.stop()
//
//                if(data!![0]!=null) {
//                    // Start a background thread
//
//
//                }
//
//            }
//            override fun onPartialResults(bundle: Bundle) {
//
//                Log.e("Tag-onPartialResults", "error")
//            }
//            override fun onEvent(i: Int, bundle: Bundle) {
//
//                Log.e("Tag-onEvent", i.toString())
//            }
//        })

//        micButton!!.setOnTouchListener { view, motionEvent ->
//            if (motionEvent.action == MotionEvent.ACTION_UP) {
//
//              speechRecognizer?.stopListening()
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
//                speechRecognizer?.startListening(speechRecognizerIntent)
//
//            }
//            false
//        }


}