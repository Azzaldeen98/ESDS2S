package com.example.esds2s.Ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.esds2s.Activies.RecordAudioActivity
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.Enums.AudioPlayerStatus
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.LanguageInfo
import com.example.esds2s.R
import com.example.esds2s.Services.RecordVoiceService
import com.example.esds2s.Services.SessionManagement
import com.example.esds2s.Services.SettingsResourceForRecordServices
import com.example.esds2s.databinding.FragmentAutomatedChatBotBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
/**
 * A simple [Fragment] subclass.
 * Use the [AutomatedChatBotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AutomatedChatBotFragment : Fragment() , AdapterView.OnItemSelectedListener {


    private var dropDownListLanguage: com.google.android.material.textfield.TextInputLayout? = null
    private var autocompleteTV: AutoCompleteTextView? = null
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var arrayAdapterLanguage: ArrayAdapter<String>? =null
    private  var alert_btn_ok: TextView?=null
    private  var languageCodes : Array<String>?=null
    private  var languageNames : Array<String>?=null
    private  var genderList: Array<String>?=null
    private  lateinit var serviceIntent: Intent;
    private lateinit var binding: FragmentAutomatedChatBotBinding
    private lateinit var sessionManagement: SessionManagement<RecordAudioActivity>
    private var alert_msg: TextView?=null
    private var selectedLanguageCode: String?=null
    private var selectedLanguageIndex: Int=-1
    private var alert_btn_cancel: TextView?=null
    private var notify_layout_back : LinearLayout?=null
    private var alert_notify: LinearLayout?=null
    private var isMuteVoice: Boolean=false
    private var progressBar: RelativeLayout?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAutomatedChatBotBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }
    override fun onStart() {
        super.onStart()

        sessionManagement=SessionManagement(this?.activity!!)
        loadPresetUserLanguage()
        internalHeader()
        initializationComponent()

        progressBar=activity?.findViewById(R.id.progressPar1)!!
        initializationEvents();
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}

    }
    private fun internalHeader(){

        val btn_back: TextView?=activity?.findViewById(R.id.internal_head_btn_back)
        val internaal_header: TextView?=activity?.findViewById(R.id.internal_head_title)
        internaal_header?.setText(getString(R.string.automated_chat_page))
//        btn_back?.setOnClickListener {onBack()}
    }
    private fun initializationComponent() {

        notify_layout_back = activity?.findViewById(R.id.background_notify_dialog1)
        alert_msg = activity?.findViewById(R.id.alert_title)
        alert_btn_ok = activity?.findViewById(R.id.alert_btn_ok)
        alert_notify = activity?.findViewById(R.id.alert_notify)
        alert_btn_cancel = activity?.findViewById(R.id.alert_btn_cancel)
        languageCodes = resources.getStringArray(R.array.language_codes)
        if(Helper.isRecordServiceRunningInForeground(this?.activity!!, RecordVoiceService::class.java))
            setStartRecordForGroundServiceMode();
        else
            setStopRecordForGroundServiceMode()
    }
    private fun loadPresetUserLanguage() {
            val languageInfo = LanguageInfo.getStorageSelcetedLanguage(requireContext());
            if(languageInfo!=null) {
               selectedLanguageCode=languageInfo.code!!
               selectedLanguageIndex=languageInfo.index!!
            }
    }

    private fun initializationEvents() {

        alert_btn_cancel?.setOnClickListener{v-> notify_layout_back?.setVisibility(View.GONE)}
        binding?.btnAutomatedChat?.setOnClickListener{v-> onClickGenerateAutomatedChatService(v) }
        binding?.btnCloseService?.setOnClickListener{v->  sessionManagement?.onClickStopService() }
        binding?.robotSpeekerBtn?.setOnClickListener{v-> ControllChatRobotVoice() }

        alert_btn_ok?.setOnClickListener{v->
            if(selectedLanguageCode!=null) {
                AlertDialog.Builder(requireContext())
                                .setTitle("Switch to vibration sound mode ")
                                .setIcon(R.drawable.baseline_vibration_24)
                                .setMessage(getString(R.string.msg_mute_microphone_alarm_tone))
                                .setPositiveButton(getString(R.string.btn_ok)) { dialog, which ->
                                    SettingsResourceForRecordServices.vibrateSoundMode(this@AutomatedChatBotFragment.activity!!)
                                    checkMicrophonPermision() }
                                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which ->}
                                .create()
                                .show()
                }
            notify_layout_back?.setVisibility(View.GONE)
        }
    }
    private  fun initializationLanguagesList(){

    }


    fun onClickGenerateAutomatedChatService(v: View) {


            notify_layout_back?.visibility = View.VISIBLE;
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.entry_to_top_animation)
            alert_msg?.text = getString(R.string.notify1_Automated_msg)
            alert_notify?.startAnimation(animation)

//        }
    }
    private  fun  setStartRecordForGroundServiceMode(){
        binding?.btnAutomatedChat?.visibility = View.GONE
        binding?.btnCloseService?.visibility = View.VISIBLE
//        binding?.robotSpeekerBtn?.visibility = View.VISIBLE
    }
    private  fun  setStopRecordForGroundServiceMode(){
        binding?.btnAutomatedChat?.visibility = View.VISIBLE
        binding?.btnCloseService?.visibility = View.GONE
//        binding?.robotSpeekerBtn?.visibility = View.GONE
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Toast.makeText(requireContext(), selectedLanguageCode, Toast.LENGTH_SHORT).show();
    }
    override fun onNothingSelected(parent: AdapterView<*>?) {}
    fun startRecordServicesOnForground(){
        setStartRecordForGroundServiceMode()
        if(!Helper.isRecordServiceRunningInForeground(this?.activity!!, RecordVoiceService::class.java)) {
             serviceIntent = Intent(requireContext(), RecordVoiceService::class.java)
             serviceIntent.putExtra(ContentApp.LANGUAGE,selectedLanguageCode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                this?.activity?.startForegroundService(serviceIntent)
            else
                this?.activity?.startService(serviceIntent)
        }
    }
    fun ControllChatRobotVoice(){

        if(!isMuteVoice) {
            binding?.robotSpeekerBtn?.setImageResource(R.drawable.baseline_voice_over_off_24)
            ExternalStorage.storage(
                activity, ContentApp.ROBOT_CHAT_SETTINGS, ContentApp.PLAYER_ROBOT_AUDIO,
                AudioPlayerStatus.STOP.ordinal.toString()
            )
        }
        else {
            binding?.robotSpeekerBtn?.setImageResource(R.drawable.baseline_record_voice_over_24)
            ExternalStorage.storage(
                activity,
                ContentApp.ROBOT_CHAT_SETTINGS,
                ContentApp.PLAYER_ROBOT_AUDIO,
                AudioPlayerStatus.START.ordinal.toString()
            )
        }

            isMuteVoice=!isMuteVoice
    }
    fun checkMicrophonPermision(){


        // Check if the permission has been granted
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