package com.example.esds2s.Activies

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.esds2s.ApiClient.Controlls.SessionChatControl
import com.example.esds2s.Helpers.Enums.GenderType
import com.example.esds2s.Ui.AutomatedChatBotFragment
import com.example.esds2s.Ui.BasicChatBotFragment
import com.example.esds2s.Ui.ChatBotTextFragment
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.LanguageInfo
import com.example.esds2s.Helpers.Tools.SpinnerHandler
import com.example.esds2s.Interface.IBaseCallbackListener
import com.example.esds2s.R
import com.example.esds2s.Services.RecordVoiceService
import com.example.esds2s.Services.SessionManagement
import com.example.esds2s.Services.TestConnection
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Objects

class RecordAudioActivity : AppCompatActivity() {


    private var arrayAdapterLanguage: ArrayAdapter<String>? =null
    private var selectedLanguageIndex: Int=-1
    private var autocompleteTV: AutoCompleteTextView? = null
    private lateinit var selectedLanguageCode: String
    private lateinit var sessionManagement: SessionManagement<RecordAudioActivity>
    lateinit var bottomNav : BottomNavigationView
    private  var languageCodes : Array<String>?=null
    private  var languageNames : Array<String>?=null
    private var activeFragment :Fragment?=null



        @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateFormat = DateFormat.getDateFormat(applicationContext)
        setContentView(R.layout.activity_record_audio)

        bottomNav = findViewById(R.id.bottomNavigationView) as BottomNavigationView
        autocompleteTV = findViewById(R.id.autoCompleteTextViewLanguage)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navBtnBasicChat -> {
                    activeFragment=BasicChatBotFragment()
                    checkServiceAndLoudFragment(BasicChatBotFragment())
//                          loadFragment(BasicChatBotFragment())
                    true
                }
                R.id.navBtnAutomatedChat -> {
                    activeFragment=AutomatedChatBotFragment()
                    loadFragment(AutomatedChatBotFragment())
                    true
                }
                R.id.navBtnTextChat -> {
                    activeFragment=ChatBotTextFragment()
                    checkServiceAndLoudFragment(ChatBotTextFragment())
//                          loadFragment(ChatBotTextFragment())
                    true
                }
                R.id.navBtnCloseSession -> {

                    sessionManagement?.logOutSession()
                    true
                }
                else -> false


            }
        }
        sessionManagement=SessionManagement(this)

      if(!TestConnection.isOnline(this)) {
          val intent = Intent(this, MainActivity::class.java)
          intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          startActivity(intent)
      }
      else {
              if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                  checkPermission() }

//          genderType=if(selectedChat?.modeldescription=="Male") GenderType.MALE else GenderType.FEMALE
//          insilizationLanguagesList(genderType)
              loadPresetUserLanguage()
              initializationLanguagesList()
              activeFragment=AutomatedChatBotFragment()
              loadFragment(AutomatedChatBotFragment())

          }

    }

    private fun loadPresetUserLanguage() {
        val languageInfo = LanguageInfo.getStorageSelcetedLanguage(this);

        if(languageInfo!=null) {
            selectedLanguageCode=languageInfo.code!!
            selectedLanguageIndex=languageInfo.index!!
        }

    }
    private  fun initializationLanguagesList(){

        languageCodes = resources.getStringArray(R.array.language_codes)
        languageNames = resources.getStringArray(R.array.language_names)
        if(selectedLanguageIndex>-1)
            autocompleteTV?.setText(languageNames?.get(selectedLanguageIndex))
        arrayAdapterLanguage = ArrayAdapter<String>(this, R.layout.dropdown_item, languageNames!!)
        autocompleteTV?.setAdapter(arrayAdapterLanguage)
        autocompleteTV?.setOnItemClickListener { parent, view, position, id -> onSelectedLanguage(position)}
    }
    fun onSelectedLanguage(position:Int){

//        if (activeFragment is ChatBotTextFragment) {
//           loadFragment(ChatBotTextFragment())
//        }

        selectedLanguageCode = languageCodes?.get(position) as String
        selectedLanguageIndex=position
        selectedLanguageCode = languageCodes?.get(position).toString()
        if(selectedLanguageCode!=null){
            LanguageInfo.setStorageSelcetedLanguage(this, selectedLanguageCode, position)
        }
//        Toast.makeText(this, "Selected: $selectedLanguageCode", Toast.LENGTH_SHORT).show()


    }
    private  fun checkServiceAndLoudFragment(fragment: Fragment){

        try {
            if (Helper.isRecordServiceRunningInForeground(this, RecordVoiceService::class.java)) {

                AlertDialog.Builder(this)
                    .setTitle("warning")
                    .setIcon(R.drawable.baseline_warning_24)
                    .setMessage(getString(R.string.msg_stop_automated_chat))
                    .setPositiveButton(getString(R.string.btn_yes)) { dialog, which ->
                        val  serviceIntent = Intent(this, RecordVoiceService::class.java)
                        stopService(serviceIntent)
                        loadFragment(fragment)
                    }.setNegativeButton(getString(R.string.btn_no)) { dialog, which ->}
                    .create()
                    .show()
            }
            else
                loadFragment(fragment)

        }catch (e:Exception){
            Log.d("Erorr-Close Forground Service",e.message.toString())
        }
    }
    private  fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container,fragment)
        transaction.commit()
    }
    override fun onDestroy() {
        super.onDestroy()
        BasicChatBotFragment.speechRecognizer?.destroy()
    }
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                BasicChatBotFragment.RecordAudioRequestCode!!
            )
        }

        }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BasicChatBotFragment.RecordAudioRequestCode && grantResults.size > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(
                this,
                "Permission Granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


}