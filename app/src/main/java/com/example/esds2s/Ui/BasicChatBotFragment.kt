package com.example.esds2s.Ui

import android.annotation.SuppressLint
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
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.*
import com.example.esds2s.Helpers.Enums.TypesOfVoiceResponses
import com.example.esds2s.Interface.IGeminiServiceEventListener
import com.example.esds2s.Interface.ISpeechRecognizerServices
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.R
import com.example.esds2s.Services.ExternalServices.SpeechRecognizerService
import com.example.esds2s.Services.TestConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BasicChatBotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BasicChatBotFragment : Fragment() , IGeminiServiceEventListener, ISpeechRecognizerServices {

    companion object {
        val RecordAudioRequestCode: Int? = 1
        lateinit var speechRecognizerService: SpeechRecognizerService
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

    private var voiceResponseCount: Int = 0
    private var isResponse: Boolean=false

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    var file_record_Path: String?=null
    var  micButton: ImageView?=null
    var  editText: EditText?=null
    var isRecord:Boolean=false
    var reorderCounter:Int?=0;
    var speechTextResult:String?=null;
    var reply_music: MediaPlayer?=null

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

        return inflater.inflate(R.layout.fragment_basic_chat_bot, container, false)
    }

    fun sendRecordAudio(data:String) {

        try {
            if(TestConnection.isOnline(this.context!!, true)) {
                speechChatControl?.messageToGeneratorAudio(data, this)
            }

            startDefaultVoiceResponse()

//            if(audioPlayer==null)
//                audioPlayer= AudioPlayer(this.context!!)
//            try {
//                val player: MediaPlayer
//                val sound_id = Helper.getDefaultSoundResource()
//                player = audioPlayer?.startFromRowResource(this.context!!, sound_id)!!
//
////                    val backgroundTask = BackgroundTask()
////                    backgroundTask.execute(Pair(audioPlayer!!, this))
//                player?.setOnErrorListener { mp, what, extra ->
//                    // Handle the error here
//                    try {
//                        if (mp.isPlaying)
//                            mp?.stop();
//                        mp.reset();
//                        mp.release();
//                    } catch (e: Exception) {
//                        Log.e("error", e.message.toString());
//                    }
//                    Log.e("error Plyer", "");
//                    true // Return true if the error is considered handled, false otherwise
//                }
//                player?.setOnCompletionListener { mp ->
//                    mp?.stop();
//                    mp.reset();
//                    mp.release();
//                }
//            }catch (e:Exception){
//                Log.d("Error ! ", e.message.toString())
//            }
        }catch (e:java.lang.Exception){

        }

    }

    private fun loadPresetUserLanguage() {
        val languageInfo = LanguageInfo.getStorageSelcetedLanguage(this?.context);
        if(languageInfo!=null) {
         Toast.makeText(this.context,  languageInfo.code!!,Toast.LENGTH_SHORT).show()
        }
    }
    @SuppressLint("SuspiciousIndentation")
    override fun onStart() {
        super.onStart()

        loadPresetUserLanguage()
        speechChatControl= this.context?.let { SpeechChatControl(it) }
        file_record_Path="${this.context?.externalCacheDir?.absolutePath}/audiorecordtest.3gp"  //com.example.esds2s.ApiClient.BuildConfig.AudioFilePath
        audioRecorder = AndroidAudioRecorder(this.context!!)
        audioPlayer = AudioPlayer(this.context!!)
        micButton=activity?.findViewById(R.id.micButton)
        editText=activity?.findViewById(R.id.text)

       speechRecognizerService = SpeechRecognizerService(this?.context!!, this, this)
    var lang:String?="ar"
      if(ExternalStorage.existing(this?.context, ContentApp.LANGUAGE))
          lang= ExternalStorage.getValue(this.activity, ContentApp.LANGUAGE) as String?
         speechRecognizerService?.Initialization(true,true,false,lang)

//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.activity)

        micButton?.setOnClickListener{v->
            if(isRecord==false){
                micButton!!.setImageResource(R.drawable.ic_mic_black_24dp)
                Log.d("startRecorder", "Recorder....");
                speechRecognizerService?.startSpeechRecognizerListening()

            }
            else{
                micButton!!.setImageResource(R.drawable.ic_mic_black_off)
                micButton?.isEnabled=false;
                Log.d("stopRecorder", "Recorder....");
                editText?.hint="Wait ...";
                speechRecognizerService?.stopSpeechRecognizer()
                isResponse=false
            }

            isRecord=(!isRecord!!)

        }

    }
    private   fun startDefaultVoiceResponse(sound_num:Int=-1){

        val mtx = Mutex()
        GlobalScope.launch(Dispatchers.Default) {
            if(isResponse==false) {
                mtx?.withLock {
                    try {
                        if (audioPlayer == null)
                            audioPlayer = AudioPlayer(this@BasicChatBotFragment.context)
                        val sound_id = Helper.getDefaultSoundResource(sound_num)
                        val player = audioPlayer?.startFromRowResource(this@BasicChatBotFragment.context!!, sound_id) ?: null
                        if (player == null) return@launch
                        player?.setOnErrorListener { mp, what, extra ->
                            audioPlayer?.stop()
                            true // Return true if the error is considered handled, false otherwise
                        }
                        player?.setOnCompletionListener { mp ->
                            audioPlayer?.stop()
                            if (isResponse == false && voiceResponseCount < 3) {
                                voiceResponseCount++
                                startDefaultVoiceResponse(TypesOfVoiceResponses.ASKYOU.ordinal)
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("Error", e.message.toString())
                    }
                }
            }
        }

    }
    private   fun playDefaultVoiceResponse(sound_num:Int){

        val player = audioPlayer?.startFromRowResource(this.context!!, sound_num)
            ?: null
        if (player != null) {
            player?.setOnErrorListener { mp, what, extra ->
                audioPlayer?.stop()
                true
            }
            player?.setOnCompletionListener { mp -> audioPlayer?.stop() }
        }


    }
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizerService?.destroy()
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onRequestIsSuccess(response: GeminiResponse) {

        try {
            isResponse=true
            reorderCounter = 0;
            speechTextResult = null;
            if (response != null) {
                if (response?.description != null) {

                    if (audioPlayer == null )
                        audioPlayer= AudioPlayer(this@BasicChatBotFragment.context)

                        GlobalScope.launch(Dispatchers.Default) {

                            if (audioPlayer?.isPlayer() ?: false) {
                                val duration = audioPlayer?.getRemainingDuration()?.toLong()
                                Thread.sleep(duration!!)
                                try {
                                    audioPlayer?.stop()
                                } catch (e: Exception) {
                                    Log.e("stop audioPlayer", e.message.toString());
                                }
                            }
                            editText?.hint = "Listen ...";
                            var media_player: MediaPlayer
                            if (!Helper.isAudioFile(response?.description)) {

                                val sound_id = Helper.getDefaultSoundResource()
                                Log.e("isAudioFile", sound_id.toString());
                                media_player =
                                    audioPlayer?.startFromRowResource(this@BasicChatBotFragment.context!!, sound_id!!)!!
                            } else {
                                media_player = audioPlayer?.start(response?.description)!!
                            }
                            if (media_player != null) {


                                media_player?.setOnCompletionListener { mPlayer ->
                                    Log.e("onUplaodAudioIsSuccess", "Complate Plyer Museic");
                                    audioPlayer?.stop();
                                    micButton?.isEnabled = true;
                                    editText?.hint = "Speaking ...";
                                }

                            }
                        }
                }
            } else {
                // Handle unsuccessful response here
                Log.e("responseError", "!! response is empty or  null");
            }

        } catch (e:Exception){}

    }
    override fun onRequestIsFailure(error: String) {

        isResponse=true
        
        try {
            if(reorderCounter!!<3 && speechTextResult!=null) {
                speechChatControl?.messageToGeneratorAudio(speechTextResult,this);
            }
        }catch (e:java.lang.Exception){}
        finally {
            if(reorderCounter!! >=3) {
                micButton?.isEnabled = true;
                speechTextResult=null;
                reorderCounter=0
            }
            else {
                reorderCounter = reorderCounter?.plus(1);
            }
        }

        Log.e("onFailure", error!!);
    }
    override fun onSpeechRecognizerResults(results: ArrayList<String>?) {

        if(results!=null&& results?.count()!!>0) {
            speechTextResult = results[0]
            sendRecordAudio(speechTextResult!!)
            Log.e("onSpeechRecognizerResults90", speechTextResult!!);
        }
    }

}