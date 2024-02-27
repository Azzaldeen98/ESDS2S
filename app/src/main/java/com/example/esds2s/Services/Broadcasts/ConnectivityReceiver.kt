package com.example.esds2s.Services.Broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.net.ConnectivityManager
import com.example.esds2s.Services.TestConnection
import android.content.Intent

class ConnectivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            // internet connection is available
//            val intent = Intent(context, MainActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            context.startActivity(intent)
        } else {
            // internet connection is not available
            TestConnection.showNotification(context)
        }
    }
}

