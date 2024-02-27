package com.example.esds2s.Activies

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.JsonStorageManager
import com.example.esds2s.R
import com.example.esds2s.Services.Broadcasts.ConnectivityReceiver


class SplashActivity : AppCompatActivity() {

    private  var logo_content:LinearLayout?=null
    private var connectivityReceiver: ConnectivityReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        logo_content=findViewById(R.id.first_logo_content)
        Helper.setAnimateAlphaForTool(logo_content!!)
        Handler().postDelayed({

            // Start the main activity
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Close the splash screen activity
            finish()
        }, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        JsonStorageManager(this).delete(ContentApp.CHATS_LIST_STORAGE)
    }




}