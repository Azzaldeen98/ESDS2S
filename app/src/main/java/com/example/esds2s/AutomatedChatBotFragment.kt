package com.example.esds2s

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.esds2s.ApiClient.Controlls.SessionChatControl
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.Enums.AudioPlayerStatus
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.LanguageInfo
import com.example.esds2s.Services.RecordVoiceService
import com.example.esds2s.Services.TestConnection
import com.example.esds2s.Ui.MainHomeFragment
import com.example.esds2s.databinding.FragmentAutomatedChatBotBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AutomatedChatBotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AutomatedChatBotFragment : Fragment() , AdapterView.OnItemSelectedListener{
    // TODO: Rename and change types of parameters

    var alert_btn_ok: TextView?=null
    var languageCodes : Array<String>?=null
    var languageNames : Array<String>?=null
    private  lateinit var serviceIntent:Intent;
    private lateinit var binding: FragmentAutomatedChatBotBinding
    private var alert_msg: TextView?=null
    private var selectedLanguageCode: String?=null
    private var alert_btn_cancel: TextView?=null
    private var notify_layout_back : LinearLayout?=null
    private var alert_notify: LinearLayout?=null
    private var isMuteVoice: Boolean=false
    private var progressBar: RelativeLayout?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {}
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAutomatedChatBotBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }
    override fun onStart() {
        super.onStart()

        internalHeader()
        initializationComponent()
        progressBar=activity?.findViewById(R.id.progressPar1)!!
        initializationEvents();
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this@AutomatedChatBotFragment?.getContext()!!, android.R.layout.simple_spinner_item, languageNames?.toList()!!)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguages.setAdapter(adapter)
        binding.spinnerLanguages.setOnItemSelectedListener(this@AutomatedChatBotFragment)
        loadPresetUserLanguage()

    }
    private fun internalHeader(){

        val btn_back:TextView?=activity?.findViewById(R.id.internal_head_btn_back)
        val internaal_header:TextView?=activity?.findViewById(R.id.internal_head_title)
        internaal_header?.setText(getString(R.string.automated_chat_page))
        btn_back?.setOnClickListener {onBack()}
    }

    override fun onResume() {
        super.onResume()
        internalHeader()
    }
    private fun onBack() {

        if(Helper.isRecordServiceRunningInForeground(this?.context,RecordVoiceService::class.java)) {
            onClickStopService()
        } else{
            AlertDialog.Builder(this.context)
                .setTitle("Warning")
                .setIcon(R.drawable.baseline_info_24)
                .setMessage(getString(R.string.msg_stop_session_chat))
                .setPositiveButton(getString(R.string.btn_ok)) { dialog, which -> stopSession() }
                .setNegativeButton(getString(R.string.btn_no)) { dialog, which ->}
                .create()
                .show()
        }

    }
    fun stopSession() {
        progressBar?.visibility=View.VISIBLE
         activity?.runOnUiThread {
             GlobalScope.launch {
                 try {
                     if(TestConnection.isOnline(this@AutomatedChatBotFragment.context!!)) {
                         val respons = SessionChatControl(this@AutomatedChatBotFragment?.context!!).removeSession()
                          }
                         stopRecordForGroundService()
                         val intent = Intent(this@AutomatedChatBotFragment.activity, MainActivity::class.java)
                         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                         startActivity(intent)
                         activity?.finish()

                 }catch (e:Exception){

                     stopRecordForGroundService()
                     val intent = Intent(this@AutomatedChatBotFragment.activity, MainActivity::class.java)
                     intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                     startActivity(intent)
                     activity?.finish()
                 }
             }}

//         Helper.LoadFragment(MainHomeFragment(), activity?.supportFragmentManager, R.id.main_frame_layout)
     }
    private fun initializationComponent() {

        languageCodes = resources.getStringArray(R.array.language_codes)
        languageNames = resources.getStringArray(R.array.language_names)
        notify_layout_back = activity?.findViewById(R.id.background_notify_dialog1)
        alert_msg = activity?.findViewById(R.id.alert_title)
        alert_btn_ok = activity?.findViewById(R.id.alert_btn_ok)
        alert_notify = activity?.findViewById(R.id.alert_notify)
        alert_btn_cancel = activity?.findViewById(R.id.alert_btn_cancel)

    }
    private fun loadPresetUserLanguage() {
        val languageInfo = LanguageInfo.getStorageSelcetedLanguage(this?.context);
        if(languageInfo!=null) {
            binding.spinnerLanguages.setSelection(languageInfo.index)
        }

    }
    private fun initializationEvents() {
        if(Helper.isRecordServiceRunningInForeground(this?.activity!!, RecordVoiceService::class.java))
            setStartRecordForGroundServiceMode();
        else
            setStopRecordForGroundServiceMode()

        alert_btn_cancel?.setOnClickListener{v-> notify_layout_back?.setVisibility(View.GONE)}
        binding?.btnAutomatedChat?.setOnClickListener{v-> onClickGenerateAutomatedChatService(v) }
        binding?.btnCloseService?.setOnClickListener{v-> onClickStopService() }
        binding?.robotSpeekerBtn?.setOnClickListener{v-> VoiceControll() }
        alert_btn_ok?.setOnClickListener{v->
            if(selectedLanguageCode!=null)
                checkMicrophonPermision()
            notify_layout_back?.setVisibility(View.GONE)
        }
    }
    fun onClickGenerateAutomatedChatService(v:View) {

//        if(!Helper.isRecordServiceRunningInForeground(this?.activity!!, RecordVoiceService::class.java)) {
            notify_layout_back?.visibility = View.VISIBLE;
            val animation = AnimationUtils.loadAnimation(this.context, R.anim.entry_to_top_animation)
            alert_msg?.text = getString(R.string.notify1_Automated_msg)
            alert_notify?.startAnimation(animation)

//        }
    }
    fun onClickStopService() {

        AlertDialog.Builder(this.context)
            .setTitle("Alert")
            .setIcon(R.drawable.baseline_info_24)
            .setMessage(getString(R.string.msg_stop_record_service))
            .setPositiveButton(getString(R.string.btn_ok)) { dialog, which ->
                stopSession()
            }.setNegativeButton(getString(R.string.btn_no)) { dialog, which ->}
            .create()
            .show()
    }
    private  fun  setStartRecordForGroundServiceMode(){
        binding?.btnAutomatedChat?.visibility = View.GONE
        binding?.btnCloseService?.visibility = View.VISIBLE
        binding?.robotSpeekerBtn?.visibility = View.VISIBLE
    }
    private  fun  setStopRecordForGroundServiceMode(){
        binding?.btnAutomatedChat?.visibility = View.VISIBLE
        binding?.btnCloseService?.visibility = View.GONE
        binding?.robotSpeekerBtn?.visibility = View.GONE
    }
    private  fun stopRecordForGroundService(){

        try {
            if (Helper.isRecordServiceRunningInForeground(this@AutomatedChatBotFragment?.context, RecordVoiceService::class.java)) {
                 serviceIntent = Intent(this?.context!!, RecordVoiceService::class.java)
                activity?.stopService(serviceIntent)
                setStopRecordForGroundServiceMode()
            }
        }catch (e:Exception){
            Log.d("Erorr-Close Forground Service",e.message.toString())
        }
    }
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedLanguageCode = languageCodes?.get(position)
        if(selectedLanguageCode!=null){
            LanguageInfo.setStorageSelcetedLanguage(this?.context,selectedLanguageCode,position)
        }

        Toast.makeText(this?.context,selectedLanguageCode, Toast.LENGTH_SHORT).show();
    }
    override fun onNothingSelected(parent: AdapterView<*>?) {}
    fun startRecordServicesOnForground(){

        setStartRecordForGroundServiceMode()
        if(!Helper.isRecordServiceRunningInForeground(this?.activity!!, RecordVoiceService::class.java)) {
             serviceIntent = Intent(this?.context!!, RecordVoiceService::class.java)
            serviceIntent.putExtra(ContentApp.LANGUAGE,selectedLanguageCode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                this?.activity?.startForegroundService(serviceIntent)
            else
                this?.activity?.startService(serviceIntent)
        }
    }
    fun VoiceControll(){

        if(!isMuteVoice) {
            binding?.robotSpeekerBtn?.setImageResource(R.drawable.baseline_volume_mute_24)
            ExternalStorage.storage(activity,ContentApp.ROBOT_CHAT_SETTINGS,ContentApp.PLAYER_ROBOT_AUDIO,
                AudioPlayerStatus.PAUSE.ordinal.toString())
        }
        else {
            binding?.robotSpeekerBtn?.setImageResource(R.drawable.baseline_volume_up_24)
            ExternalStorage.storage(activity,ContentApp.ROBOT_CHAT_SETTINGS,ContentApp.PLAYER_ROBOT_AUDIO,AudioPlayerStatus.RESUME.ordinal.toString())
        }

            isMuteVoice=!isMuteVoice
    }
    fun checkMicrophonPermision(){


        // Check if the permission has been granted
        if (ContextCompat.checkSelfPermission(this?.context!!, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it from the user
            ActivityCompat.requestPermissions(this?.activity!!,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                ContentApp.REQUEST_MICROPHONE_PERMISSION_CODE)
        } else {
            // Permission has already been granted, proceed with your app logic
            // ------------------------------------
            // start ForGround Service
            startRecordServicesOnForground();
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ContentApp.REQUEST_MICROPHONE_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, proceed with your app logic
                    startRecordServicesOnForground();
                } else {
                    // Permission denied, handle accordingly (e.g., show explanation, disable functionality, etc.)
                }
                return
            }
            // Add more cases for other permissions if needed
        }
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AutomatedChatBotFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AutomatedChatBotFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

    }

}


