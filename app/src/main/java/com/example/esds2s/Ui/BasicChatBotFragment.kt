package com.example.esds2s.Ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
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
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.*
import com.example.esds2s.Interface.IGeminiServiceEventListener
import com.example.esds2s.Interface.ISpeechRecognizerServices
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.R
import com.example.esds2s.Services.ExternalServices.SpeechRecognizerService
import com.example.esds2s.Services.SettingsResourceForRecordServices
import kotlinx.coroutines.*
import java.util.concurrent.Semaphore


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
    var isRecord:Boolean=false;
    var robotIsSpeaking:Boolean=false;
    var reorderCounter:Int?=0;
    var speechTextResult:String?=null;
    var reply_music: MediaPlayer?=null

    private var audioPlayer: AudioPlayer? = null
    private var speechChatControl: SpeechChatControl? = null
    private var audioRecorder: AndroidAudioRecorder? = null
    val simaphor = Semaphore(1)
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

    fun sendRecordAudio(inputText:String) {

        try {

            if(audioPlayer!=null && robotIsSpeaking && SettingsResourceForRecordServices().isStopWord(
                LanguageInfo.getStorageSelcetedLanguage(context).index, inputText)){
                Toast.makeText(this.context,inputText, Toast.LENGTH_SHORT).show()
                audioPlayer?.completionAudio()

            } else if(robotIsSpeaking==false)
                    startDefaultVoiceResponse(1)
            else{
                speechRecognizerService?.startSpeechRecognizerListening()
            }

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
//    val outputDir: File? =  // الحصول على مسار الدليل الداخلي للتطبيق
//    val outputFile = File(this.activity?.getFilesDir(), "myRecording.mp3")
    var recordFilePath:String="";// context?.getExternalFilesDir(null)?.absolutePath + "/myRecording.mp3"
;//    File(this.activity?.getFilesDir(), "myRecording.mp3").getAbsolutePath()
    var androidAudioRecorder:AndroidAudioRecorder?=null;
    @SuppressLint("SuspiciousIndentation")
    override fun onStart() {
        super.onStart()

        loadPresetUserLanguage()
        speechChatControl= this.context?.let { SpeechChatControl(it) }
        recordFilePath="${this.context?.externalCacheDir?.absolutePath}/myRecording.mp3"  //com.example.esds2s.ApiClient.BuildConfig.AudioFilePath
        audioRecorder = AndroidAudioRecorder(this.context!!)
        audioPlayer = AudioPlayer(this.context!!)
        micButton=activity?.findViewById(R.id.micButton)
        editText=activity?.findViewById(R.id.text)

        androidAudioRecorder=AndroidAudioRecorder(context!!);

       speechRecognizerService = SpeechRecognizerService(this?.context!!, this)
    var lang:String?="ar"
      if(ExternalStorage.existing(this?.context, ContentApp.LANGUAGE))
          lang= ExternalStorage.getValue(this.activity, ContentApp.LANGUAGE) as String?
         speechRecognizerService?.initialization(true,true,lang)

//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.activity)
        audioPlayer = AudioPlayer(this@BasicChatBotFragment.context)
        micButton?.setOnClickListener{v->
            if(isRecord==false){
                micButton?.isEnabled=true
                micButton!!.setImageResource(R.drawable.ic_mic_black_24dp)
                Log.d("startRecorder", "Recorder....");
//                speechRecognizerService?.startSpeechRecognizerListening()
                    robotIsSpeaking=false;
                    editText?.hint="Speaker ...";
                    checkMicrophonPermision();
                    startDefaultVoiceResponse();

//                isRecord=true;

            }
            else{
                micButton!!.setImageResource(R.drawable.ic_mic_black_off)
                micButton?.isEnabled=false;
                Log.d("stopRecorder", "Recorder....");
                editText?.hint="Wait ...";
                if(audioPlayer!!.isPlayer())
                    audioPlayer?.completionAudio()




//                startRecord();
//                isResponse=false
            }

            isRecord=(!isRecord!!)

        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}

    }

    fun playerRecordFile(){

        var record= audioPlayer?.start(recordFilePath)
        record?.setOnErrorListener {  mp, what, extra ->
            audioPlayer?.stop()
            true
        }
        record?.setOnCompletionListener { mp ->
            audioPlayer?.stop()
        }
    }
    fun checkMicrophonPermision(){

        AlertDialog.Builder(requireContext())
            .setTitle("Switch to vibration sound mode ")
            .setIcon(R.drawable.baseline_vibration_24)
            .setMessage(getString(R.string.msg_mute_microphone_alarm_tone))
            .setPositiveButton(getString(R.string.btn_ok)) { dialog, which ->
                SettingsResourceForRecordServices.vibrateSoundMode(this.activity!!)
                if (ContextCompat.checkSelfPermission(requireContext()!!, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission is not granted, request it from the user
                    ActivityCompat.requestPermissions(
                        this?.activity!!,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        ContentApp.REQUEST_MICROPHONE_PERMISSION_CODE
                    )
                } else {

                    startRecord();
                    
//                    speechRecognizerService?.startSpeechRecognizerListening()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which ->}
            .create()
            .show()
        // Check if the permission has been granted

    }

    fun startRecord(){
        // speechRecognizerService?.startSpeechRecognizerListening()
        androidAudioRecorder?.start(recordFilePath);
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ContentApp.REQUEST_MICROPHONE_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, proceed with your app logic
                    startRecord();
                } else {
                    // Permission denied, handle accordingly (e.g., show explanation, disable functionality, etc.)
                }
                return
            }
            // Add more cases for other permissions if needed
        }
    }
    private   fun startDefaultVoiceResponse(sound_num:Int=-1){

            robotIsSpeaking=true;
//            speechRecognizerService?.startSpeechRecognizerListening()

//        lifecycleScope.launch(Dispatchers.Main) {
//            if(isResponse==false) {

                    try {
//                        simaphor.acquire()
//                        if (audioPlayer == null)
//                        micButton?.isEnabled=false;
//                        editText?.hint="Wait ...";
//                        isRecord=false;



//                        val sound_id = DefaultSoundResource.getAudioResource(this@BasicChatBotFragment.context!!,DefaultAudioStatus.Before)
                        val player = audioPlayer?.startFromRowResource(this@BasicChatBotFragment.context!!, R.raw.test)
                        player?.setOnErrorListener { mp, what, extra ->
                            audioPlayer?.stop()
//                            speechRecognizerService?.stopSpeechRecognizer()
                            robotIsSpeaking=false;
                            playerRecordFile();
                            true // Return true if the error is considered handled, false otherwise
                        }
                        player?.setOnCompletionListener { mp ->
                            audioPlayer?.stop()
                            micButton!!.setImageResource(R.drawable.ic_mic_black_off)
                            micButton?.isEnabled = true;
                            robotIsSpeaking=false;
                            editText?.hint="Speaking ...";
                            playerRecordFile();
//                            speechRecognizerService?.stopSpeechRecognizer()

//                            if (isResponse == false && voiceResponseCount < 3) {
//                                voiceResponseCount++
//                                startDefaultVoiceResponse(TypesOfVoiceResponses.ASKYOU.ordinal)
//                            }
                        }
                    } catch (e: Exception) {
                        Log.d("Error", e.message.toString())
                    }finally {
//                        simaphor.release()
                    }

//            }
//        }

    }
//    private   fun playDefaultVoiceResponse(sound_num:Int){
//
//        val player = audioPlayer?.startFromRowResource(this.context!!, sound_num)
//            ?: null
//        if (player != null) {
//            player?.setOnErrorListener { mp, what, extra ->
//                audioPlayer?.stop()
//                true
//            }
//            player?.setOnCompletionListener { mp -> audioPlayer?.stop() }
//        }
//
//
//    }
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
                                    audioPlayer?.startFromRowResource(
                                        this@BasicChatBotFragment.context!!,
                                        sound_id!!)!!
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
//                speechChatControl?.messageToGeneratorAudio(speechTextResult,this);
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