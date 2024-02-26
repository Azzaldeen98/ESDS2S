package com.example.esds2s

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Services.RecordVoiceService
import com.example.esds2s.Services.TestConnection
import com.google.android.material.bottomnavigation.BottomNavigationView

class RecordAudioActivity : AppCompatActivity() {

    lateinit var bottomNav : BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateFormat = DateFormat.getDateFormat(applicationContext)
        setContentView(R.layout.activity_record_audio)

      if(TestConnection.isOnline(this)) {
              if (ContextCompat.checkSelfPermission(
                      this,
                      Manifest.permission.RECORD_AUDIO
                  ) != PackageManager.PERMISSION_GRANTED
              ) {
                  checkPermission()
              }
              loadFragment(AutomatedChatBotFragment())
              bottomNav = findViewById(R.id.bottomNavigationView) as BottomNavigationView
              bottomNav.setOnItemSelectedListener {
                  when (it.itemId) {
                      R.id.basicChatBot -> {
                          checkServiceAndLoudFragment(BasicChatBotFragment())
//                          loadFragment(BasicChatBotFragment())
                          true
                      }
                      R.id.automatedChatBot -> {

                          loadFragment(AutomatedChatBotFragment())
                          true
                      }
                      R.id.textChatBot -> {
                          checkServiceAndLoudFragment(ChatBotTextFragment())
//                          loadFragment(ChatBotTextFragment())
                          true
                      }
                      else -> false


                  }
              }
          }
        else {
          val intent = Intent(this, MainActivity::class.java)
          intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          startActivity(intent)
      }
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