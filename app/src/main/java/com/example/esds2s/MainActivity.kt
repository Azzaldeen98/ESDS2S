package com.example.esds2s

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.esds2s.ApiClient.Controlls.ChatAiServiceControll
import com.example.esds2s.ApiClient.Controlls.GeminiControll
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Services.RecordVoiceService
import com.google.ai.client.generativeai.Chat
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private  var chat: Chat? = null
    private  var chatAiServiceControll: ChatAiServiceControll? = null
    private  var geminiControll: GeminiControll? = null
    private var btnSend: TextView? = null;
    private var textResult: TextView ? = null;
    private var textInput: TextInputEditText? = null;

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val dateFormat = DateFormat.getDateFormat(
            applicationContext
        )

        btnSend=findViewById<TextView>(R.id.btnSend);
        textResult=findViewById<TextView>(R.id.textResult);
        textInput=findViewById<TextInputEditText>(R.id.textInput);


        checkMicrophonPermision();
        chatAiServiceControll= ChatAiServiceControll(this);
        geminiControll= GeminiControll(this);
        geminiControll?.generatModel();
        chat=geminiControll!!.getChat();

        val  intent = Intent(this, RecordAudioActivity::class.java)
        startActivity(intent)


    }
    fun startRecordServicesOnForground(){

        if(!isRecordServiceRunningInForeground(this, RecordVoiceService::class.java)) {
            val  serviceIntent = Intent(this, RecordVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(serviceIntent)
            else
                startService(serviceIntent)
        }
    }
    fun checkMicrophonPermision(){


        // Check if the permission has been granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it from the user
            ActivityCompat.requestPermissions(this,
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
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }
    fun sendMessage(v:View ) {


        if(!textInput?.text.isNullOrEmpty()) {
            runOnUiThread { textResult?.setText("");}
            GlobalScope.launch {
                val response = chat!!.sendMessage(textInput?.text.toString())

                if(response!=null) {
                    runOnUiThread {

                        textResult?.visibility = View.VISIBLE;
                        textResult?.setText(response.text);
                        textInput?.setText("");
                    }
                }
            }
        }
    }

}