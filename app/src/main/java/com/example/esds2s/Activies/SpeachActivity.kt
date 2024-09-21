package com.example.esds2s.Activies

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.ApiClient.Interface.ICustomPlayerListener
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.ExoPlayerMedia
import com.example.esds2s.Interface.IBaseCallbackListener
import com.example.esds2s.Interface.ISpeechRecognizerServices
import com.example.esds2s.Interface.IWasmServiceEventListener
import com.example.esds2s.R
import com.example.esds2s.Services.ExternalServices.SpeechRecognizerService
import kotlinx.coroutines.*

class SpeachActivity : AppCompatActivity(), ISpeechRecognizerServices, IWasmServiceEventListener {
//    private lateinit var binding:ActivityMainBinding;
    private  var btn_open:Button?=null;
    private  var btn_close:Button?=null;
    private var exoPlayer : ExoPlayerMedia?=null
    private var speechChatControl: SpeechChatControl? = null
    private lateinit var speechRecognizerService: SpeechRecognizerService
//    private lateinit var scope: CoroutineScope
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)
//    private  var startSpeechListen: Boolean=true
//    private  var startAudioPlay: Boolean=false
    private  var isSpeachLooping: Boolean=true
    private  var isIVRLooping: Boolean=true
//    private  var speechResult: String=""
//    private  var semaphore =Semaphore(1)
//    private var lock: ReentrantLock = ReentrantLock()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
        setContentView(R.layout.activity_speach)
        btn_open=findViewById(R.id.btn_open)
        btn_close=findViewById(R.id.btn_close)

        initialService();
        btn_open?.setOnClickListener { v -> startSpeech() }
        btn_close?.setOnClickListener { v -> stopSpeechService() }
    }



    private  fun initialService(){
//        scope=CoroutineScope(Dispatchers.IO)

        speechChatControl =  SpeechChatControl(this)
        speechRecognizerService = SpeechRecognizerService(this, this)
        exoPlayer= ExoPlayerMedia(this);
        exoPlayer?.initial()
        speechRecognizerService?.initialization(
            recognizer = true,
            workingInTheContinuously = true,
            lang = "ar"
        );
    }
    private  fun startSpeech(){
        checkMicrophonePermission()
    }
    private  fun onStartSpeechService(){
        btn_close?.visibility = View.VISIBLE;
        btn_open?.visibility = View.GONE;
        speechRecognizerService?.startSpeechRecognizerListening()
    }
    private  fun stopSpeechService(){
        try {
            ioScope?.cancel()
        }finally {
            try {
                mainScope?.cancel()
            }finally {
                btn_close?.visibility = View.GONE;
                btn_open?.visibility = View.VISIBLE;
                if(exoPlayer!=null)
                    exoPlayer?.stop()
            }

//            semaphore?.release()
            Log.e("stopSpeechService","stop")
        }
    }
    override fun onSpeechRecognizerResult(result: String?) {
        super.onSpeechRecognizerResult(result)
        if(result?.isNullOrEmpty()==false)
             processSpeechResult(result)
//            startSpeechListen=false
//            speechResult=result?:""
    }
    private  fun processSpeechResult(inputText:String){

        ioScope?.launch {
            try{
                speechChatControl?.generateBasicTextAudioNew2(inputText, this@SpeachActivity,);
            }catch (e:Exception){
                Log.e("CancellationException2",e.message!!)
                mainScope.launch{
                    speechRecognizerService?.startSpeechRecognizerListening()
                }
            }

        }
    }
    override  fun onRequestIsSuccess(filePath:String){
        mainScope.launch{
            if(filePath?.isNullOrEmpty()==false){
                playerAudio(filePath)
            }
        }
    }
    override  fun onRequestIsSuccess2(callBack: IBaseCallbackListener<Any?>?) {
        mainScope.launch{

        }
    }
    override fun onRequestIsFailure(error: String) {

        mainScope.launch{
            Toast.makeText(this@SpeachActivity,error, Toast.LENGTH_SHORT).show()
            speechRecognizerService?.startSpeechRecognizerListening()
        }

    }
    private fun playerAudio(filePath:String){

        exoPlayer?.playMedia(filePath, true,
            object : ICustomPlayerListener<ExoPlayer> {
                @OptIn(UnstableApi::class)
                override fun onErrorListener(mp: ExoPlayer?, error: Exception) {
                    Log.e("Error", "ExoPlayer is Error")
                    speechRecognizerService?.startSpeechRecognizerListening()
                }

                override fun onCompletionListener(mp: ExoPlayer?, lastAudioClip: Boolean) {
                    Log.e("onCompletionListener", "ExoPlayer is Complete")
                    speechRecognizerService?.startSpeechRecognizerListening()
                }
            })
    }
    private fun checkMicrophonePermission(){

        AlertDialog.Builder(this)
            .setTitle("Switch to vibration sound mode ")
            .setIcon(com.example.esds2s.R.drawable.baseline_vibration_24)
            .setMessage(getString(com.example.esds2s.R.string.msg_mute_microphone_alarm_tone))
            .setPositiveButton(getString(com.example.esds2s.R.string.btn_ok)) { dialog, which ->
//                SettingsResourceForRecordServices.vibrateSoundMode(this.activity!!)
                if (ContextCompat.checkSelfPermission(this!!, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission is not granted, request it from the user
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        ContentApp.REQUEST_MICROPHONE_PERMISSION_CODE
                    )
                } else {
                    onStartSpeechService()
//                    val recordThread = Thread {
//                        recording = true
//                        recorderAudio?.startRecord();
//                    }
//                    recordThread.start()


                }
            }
            .setNegativeButton(getString(com.example.esds2s.R.string.btn_cancel)) { dialog, which ->}
            .create()
            .show()
        // Check if the permission has been granted

    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ContentApp.REQUEST_MICROPHONE_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, proceed with your app logic
                    onStartSpeechService()
                } else {
                    // Permission denied, handle accordingly (e.g., show explanation, disable functionality, etc.)
                }
                return
            }
            // Add more cases for other permissions if needed
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
//            isSpeachLooping=false
//            isIVRLooping=false
            stopSpeechService()
        }finally {

        }
    }

//    private suspend fun  startSpeechListening(){
//        withContext(Dispatchers.Main){
//            speechRecognizerService?.startSpeechRecognizerListening()
//        }
//    }
//    private fun requestOnEnded(){
//
//        try {
//            semaphore?.tryAcquire()
//            startSpeechListen=true
//            speechResult=""
//        }finally {
//            semaphore?.release()
//        }
//    }
//    private  fun startSpeechService(){
//        isSpeachLooping=true
//        isIVRLooping=true
//        btn_close?.visibility = View.VISIBLE;
//        btn_open?.visibility = View.GONE;
//        speechRecognitionMonitor()
//        IVRProcessMonitor()
//
//        Log.e("startSpeechService","start")
//    }
//    private  fun speechRecognitionMonitor(){
//        scope?.launch(Dispatchers.Default) {
//            try{
//                while (isActive && isSpeachLooping){
//                    semaphore?.withPermit {
//                        if (startSpeechListen) {
//                            startSpeechListen=false
//                            withContext(Dispatchers.Main){
//                                speechRecognizerService?.startSpeechRecognizerListening()
//                            }
//                        }
//                    }
//                    delay(2000)
//                }
//            }catch (e:CancellationException){
//                Log.e("CancellationException1",e.message!!)
//            }
//        }
//    }
//    private  fun IVRProcessMonitor(){
//
//        scope?.launch(Dispatchers.IO) {
//            try{
//                while (isActive && isIVRLooping){
//                    semaphore?.withPermit {
//                        if (speechResult?.isNotEmpty() == true) {
//                            speechChatControl?.generateBasicTextAudioNew(speechResult, this@SpeachActivity,);
//                            speechResult = ""
//                        }
//                    }
//                    delay(2000)
//                }
//            }catch (e:CancellationException){
//                Log.e("CancellationException2",e.message!!)
//            }
//
//        }
//    }
}