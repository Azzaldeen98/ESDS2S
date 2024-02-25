package com.example.esds2s.Services

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.*
import android.net.ConnectivityManager.NetworkCallback
import androidx.core.content.ContextCompat.getSystemService
import com.example.esds2s.MainActivity
import com.example.esds2s.R


class TestConnection {
//    private  lateinit var networkRequest:NetworkRequest;
//    private  lateinit var context:Context;

    companion object {
        fun isOnline(context: Context,showNotifiction:Boolean=true): Boolean {
            if(context==null)
                return  false

            val connMgr = context?.getSystemService(Context.CONNECTIVITY_SERVICE!!) as ConnectivityManager
            val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
            val isConnect=networkInfo?.isConnected == true
            if(!isConnect && showNotifiction) {
                showNotification(context)
            }

            return  isConnect
        }


        fun showNotification(context: Context) {

            try {

                AlertDialog.Builder(context)
                    .setTitle("Alert")
                    .setIcon(R.drawable.baseline_wifi_off_24)
                    .setMessage(context?.getString(R.string.msg_no_internet))
                    .setPositiveButton(context?.getString(R.string.btn_ok)) { dialog, which -> }
                    .create()
                    .show()
            }catch (e:Exception){}
        }
    }


//    public fun CheckConnection() {
//         networkRequest = NetworkRequest.Builder()
//            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
//            .build()
//    }
//
//    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
//        // network is available for use
//        override fun onAvailable(network: Network) {
//            super.onAvailable(network)
//        }
//
//        // Network capabilities have changed for the network
//        override fun onCapabilitiesChanged(
//            network: Network,
//            networkCapabilities: NetworkCapabilities
//        ) {
//            super.onCapabilitiesChanged(network, networkCapabilities)
//            val unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
//        }
//
//        // lost network connection
//        override fun onLost(network: Network) {
//            super.onLost(network)
//        }
//    }

    /*
You need to call the below method once. It register the callback and fire it when there is a change in network state.
Here I used a Global Static Variable, So I can use it to access the network state in anyware of the application.
*/

    /*
You need to call the below method once. It register the callback and fire it when there is a change in network state.
Here I used a Global Static Variable, So I can use it to access the network state in anyware of the application.
*/
    // You need to pass the context when creating the class
//    fun CheckNetwork(context: Context) {
//        this.context = context
//    }
//
//    // Network Check
//    fun registerNetworkCallback() {
//        try {
//            val connectivityManager =
//                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//            val builder = NetworkRequest.Builder()
//            connectivityManager.registerDefaultNetworkCallback(object : NetworkCallback() {
//                override fun onAvailable(network: Network) {
//                    Variables.isNetworkConnected = true // Global Static Variable
//                }
//
//                override fun onLost(network: Network) {
//                    Variables.isNetworkConnected = false // Global Static Variable
//                }
//            }
//            )
//            Variables.isNetworkConnected = false
//        } catch (e: Exception) {
//            Variables.isNetworkConnected = false
//        }
//    }
//
//    object Variables {
//        // Global variable used to store network state
//        var isNetworkConnected = false
//    }
}