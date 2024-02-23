package com.example.esds2s

import android.media.MediaPlayer
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.Helpers.AndroidAudioRecorder
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Interface.IGeminiServiceEventListener
import com.example.esds2s.Interface.ISpeechRecognizerServices
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.Services.ExternalServices.SpeechRecognizerService
import java.util.*
import kotlin.collections.ArrayList


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BasicChatBotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BasicChatBotFragment : Fragment() , IGeminiServiceEventListener , ISpeechRecognizerServices {

    companion object {
        val RecordAudioRequestCode: Int? = 1
        lateinit var speechRecognizerService:SpeechRecognizerService
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
    var isRecord:Boolean=false
    var reorderCounter:Int?=0;
    var speechTextResult:String?=null;
    var reply_music:MediaPlayer?=null

    private var audioPlayer: AudioPlayer? = null
    private var speechChatControl: SpeechChatControl? = null
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

    fun sendRecordAudio(data:String) {
        speechChatControl?.messageToGeneratorAudio(data,this)
    }
    override fun onStart() {
        super.onStart()

        speechChatControl= this.context?.let { SpeechChatControl(it) }
        file_record_Path="${this.context?.externalCacheDir?.absolutePath}/audiorecordtest.3gp"  //com.example.esds2s.ApiClient.BuildConfig.AudioFilePath
        audioRecorder = AndroidAudioRecorder(this.context!!)
        audioPlayer = AudioPlayer(this.context!!)
        micButton=activity?.findViewById(com.example.esds2s.R.id.micButton)
        editText=activity?.findViewById(com.example.esds2s.R.id.text)

       speechRecognizerService = SpeechRecognizerService(this?.context!!, this,this)
    var lang:String?=null
      if( ExternalStorage.existing(this?.context,"Lang"))
          lang= ExternalStorage.getValue(this.activity,"Lang") as String?
         speechRecognizerService?.Initialization(true,true,false,lang)

//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.activity)


        micButton?.setOnClickListener{v->
            if(isRecord==false){
                micButton!!.setImageResource(com.example.esds2s.R.drawable.ic_mic_black_24dp)
                Log.d("startRecorder", "Recorder....");
                speechRecognizerService?.startSpeechRecognizerListening()

            } else{
                micButton!!.setImageResource(com.example.esds2s.R.drawable.ic_mic_black_off)
                micButton?.isEnabled=false;
                Log.d("stopRecorder", "Recorder....");
                editText?.hint="Wait ...";
                speechRecognizerService?.stopSpeechRecognizer()
            }

            isRecord=(!isRecord!!)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        speechRecognizerService?.destroy()
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
        speechTextResult=null;
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
            if(reorderCounter!!<3 && speechTextResult!=null) {
                speechChatControl?.messageToGeneratorAudio(speechTextResult,this);
            }

        }catch (e:java.lang.Exception){}
        finally {
            if(reorderCounter!! >=3) {
                micButton?.isEnabled = true;
                speechTextResult=null;
            }

            reorderCounter = reorderCounter?.plus(1);
        }

        Log.e("onFailure",error!!);
    }

    override fun onSpeechRecognizerResults(results: ArrayList<String>?) {

        if(results!=null&& results?.count()!!>0) {
            speechTextResult = results[0]
            sendRecordAudio(speechTextResult!!)
            Log.e("onSpeechRecognizerResults90",speechTextResult!!);
        }
    }


}