package com.example.esds2s.Activies

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.DefaultSoundResource
import com.example.esds2s.Helpers.Enums.DefaultAudioStatus
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.R
import com.example.esds2s.Services.TestConnection
import com.example.esds2s.Ui.MainHomeFragment
import kotlinx.coroutines.launch
import java.util.concurrent.Semaphore

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
        onBackPressedDispatcher.addCallback() {}
//
//        println("tertertertertert")
//        val s = Semaphore(1)
//
//        val t1 = Thread(Runnable
//        {
//            s.acquireUninterruptibly()
//            println("t1")
//            Thread.sleep(100)
//            print(".")
//            s.release()
//        })
//
//        val t2 = Thread(Runnable
//        {
//            s.acquireUninterruptibly()
//            println("t2")
//            Thread.sleep(100)
//            print(".")
//            s.release()
//        })
//
//        val t3 = Thread(Runnable
//        {
//            s.acquireUninterruptibly()
//            println("t3")
//            Thread.sleep(100)
//            print(".")
//            s.release()
//        })
//
//        val t4 = Thread(Runnable
//        {
//            s.acquireUninterruptibly()
//            println("t4")
//            Thread.sleep(100)
//            print(".")
//            s.release()
//        })
//
//
//        t1.start()
//        t2.start()
//        t3.start()
//        t4.start()


    }



    fun jaccardSimilarity(set1: Set<String>, set2: Set<String>): Double {
        val intersectionSize = set1.intersect(set2).size
        val unionSize = set1.union(set2).size
        return intersectionSize.toDouble() / unionSize.toDouble()
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

            }else {
//                val intent = Intent(applicationContext, RecordAudioActivity::class.java)
//                startActivity(intent)
                Helper.LoadFragment(MainHomeFragment(), supportFragmentManager, R.id.main_frame_layout)
            }
        }else{
            noConnectionLayout?.visibility=View.VISIBLE
            frameLayout?.visibility=View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        Toast.makeText(this, "Selected: ", Toast.LENGTH_SHORT).show()

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