package com.example.esds2s

import android.content.Intent
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.esds2s.Helpers.Helper
import org.w3c.dom.Text

class SplashActivity : AppCompatActivity() {

    private  var logo_content:LinearLayout?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        logo_content=findViewById(R.id.first_logo_content)
        Helper.setAnimateAlphaForTool(logo_content!!)

        Handler().postDelayed({

            // Start the main activity
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)

            // Close the splash screen activity
            finish()
        }, 3000)
    }
}