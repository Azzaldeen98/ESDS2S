package com.example.esds2s

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Services.RecordVoiceService
import com.example.esds2s.databinding.FragmentAutomatedChatBotBinding
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

    private lateinit var binding: FragmentAutomatedChatBotBinding
    private var alert_msg: TextView?=null
    private var selectedLanguageCode: String?=null
    private var alert_btn_cancel: TextView?=null

    private var notify_layout_back : LinearLayout?=null
    private var alert_notify: LinearLayout?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAutomatedChatBotBinding.inflate(inflater, container, false)
        return binding.getRoot()

    }


    override fun onStart() {
        super.onStart()

        languageCodes = resources.getStringArray(R.array.language_codes)
        languageNames = resources.getStringArray(R.array.language_names)

        notify_layout_back = activity?.findViewById(R.id.background_notify_dialog1)
        alert_msg = activity?.findViewById(R.id.alert_title)
        alert_btn_ok = activity?.findViewById(R.id.alert_btn_ok)
        alert_notify = activity?.findViewById(R.id.alert_notify)
        alert_btn_cancel = activity?.findViewById(R.id.alert_btn_cancel)
        intiolizationEvent();


        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this@AutomatedChatBotFragment?.getContext()!!, android.R.layout.simple_spinner_item, languageNames?.toList()!!)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCarsPlate.setAdapter(adapter)
        binding.spinnerCarsPlate.setOnItemSelectedListener(this@AutomatedChatBotFragment)
    }

    private fun intiolizationEvent() {

        alert_btn_cancel?.setOnClickListener{v-> notify_layout_back?.setVisibility(View.GONE)}
        binding?.btnAutomatedChat?.setOnClickListener{v-> generateAutomatedChat(v) }
        alert_btn_ok?.setOnClickListener{v->
            if(selectedLanguageCode!=null)
                checkMicrophonPermision()
            notify_layout_back?.setVisibility(View.GONE)
        }

    }

    fun generateAutomatedChat(v:View) {

        notify_layout_back?.visibility=View.VISIBLE;
        val animation = AnimationUtils.loadAnimation(this.context, R.anim.entry_to_top_animation)
        alert_msg?.text=getString(R.string.notify1_Automated_msg)
        alert_notify?.startAnimation(animation)
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

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedLanguageCode = languageCodes?.get(position)
        Toast.makeText(this?.context,selectedLanguageCode, Toast.LENGTH_SHORT).show();

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    fun startRecordServicesOnForground(){

        if(!isRecordServiceRunningInForeground(this?.activity!!, RecordVoiceService::class.java)) {
            val  serviceIntent = Intent(this?.context!!, RecordVoiceService::class.java)
            serviceIntent.putExtra("Lang",selectedLanguageCode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                this?.activity?.startForegroundService(serviceIntent)
            else
                this?.activity?.startService(serviceIntent)
        }
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
    fun isRecordServiceRunningInForeground(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }

}