package com.example.esds2s

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Ui.MainHomeFragment
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {


    private  var btn_create_chat:Button?=null
    private  var btn_chat1: MaterialCardView?=null
    private  var btn_chat2:MaterialCardView?=null
    private  var btn_chat3:MaterialCardView?=null
    private  var btn_chat4:MaterialCardView?=null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateFormat = DateFormat.getDateFormat(applicationContext)

        setContentView(R.layout.activity_main)

        btn_create_chat=findViewById(R.id.btn_main_create_chat)
        btn_chat1=findViewById(R.id.btn_main_chat1)
        btn_chat2=findViewById(R.id.btn_main_chat2)
        btn_chat3=findViewById(R.id.btn_main_chat3)
        btn_chat4=findViewById(R.id.btn_main_chat4)

        if(ExternalStorage.existing(this,"Token")) {
            val  intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }

        checkMicrophonPermision();
        Helper.LoadFragment(MainHomeFragment(),supportFragmentManager,R.id.main_frame_layout)
//        val  intent = Intent(this, RecordAudioActivity::class.java)
//        startActivity(intent)
    }


//    fun loadFragment(fragment: Fragment) {
//        val transaction = supportFragmentManager.beginTransaction()
//        transaction.replace(R.id.main_frame_layout, fragment)
//        transaction.commit()
//    }


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

        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ContentApp.REQUEST_MICROPHONE_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, proceed with your app logic

                } else {
                    // Permission denied, handle accordingly (e.g., show explanation, disable functionality, etc.)
                }
                return
            }
            // Add more cases for other permissions if needed
        }
    }

}