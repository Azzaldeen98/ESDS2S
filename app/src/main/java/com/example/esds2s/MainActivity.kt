package com.example.esds2s

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Services.TestConnection
import com.example.esds2s.Ui.MainHomeFragment

class MainActivity : AppCompatActivity() {



    private lateinit var noConnectionLayout: LinearLayout
    private lateinit var frameLayout: FrameLayout
    private lateinit var btnRefresh: Button
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateFormat = DateFormat.getDateFormat(applicationContext)
        setContentView(R.layout.activity_main)
        initialization()
    }

    fun initialization(){

        frameLayout=findViewById(R.id.main_frame_layout)
        noConnectionLayout=findViewById(R.id.noConnectionPage)
        btnRefresh=findViewById(R.id.buttonRetry)

        btnRefresh.setOnClickListener{v->  restartActivity()}
        if(TestConnection.isOnline(this,false)) {
            noConnectionLayout?.visibility=View.GONE
            frameLayout?.visibility=View.VISIBLE

            if (ExternalStorage.existing(this, ContentApp.CURRENT_SESSION_TOKEN)) {
                val intent = Intent(this, RecordAudioActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                return }
            Helper.LoadFragment(MainHomeFragment(), supportFragmentManager, R.id.main_frame_layout)
        }else{
            noConnectionLayout?.visibility=View.VISIBLE
            frameLayout?.visibility=View.GONE
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
    private fun restartActivity() {

        val intent = intent
        finish() // Finish the current instance of the activity
        startActivity(intent) // Start a new instance of the activity
    }
}