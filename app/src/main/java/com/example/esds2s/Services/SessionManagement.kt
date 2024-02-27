package com.example.esds2s.Services

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.esds2s.Activies.MainActivity
import com.example.esds2s.ApiClient.Controlls.SessionChatControl
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.JsonStorageManager
import com.example.esds2s.Helpers.LanguageInfo
import com.example.esds2s.Interface.IBaseCallbackListener
import com.example.esds2s.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SessionManagement<T>(private val activity:Activity,private val callBackListener: IBaseCallbackListener<T>?=null) {

    private var context:Context = activity?.applicationContext!!

    companion object {
        fun OnLogOutFromSession(context: Context) {
            if (context == null)
                return
            if (ExternalStorage.existing(context, ContentApp.CURRENT_SESSION_TOKEN)) {
                ExternalStorage.remove(context, ContentApp.CURRENT_SESSION_TOKEN)
            }
            LanguageInfo.removeStorageSelcetedLanguage(context)
            if (JsonStorageManager(context)?.exists(ContentApp.CHATS_LIST_STORAGE)!!)
                JsonStorageManager(context)?.delete(ContentApp.CHATS_LIST_STORAGE)
        }
    }

    fun logOutSession() {
        activity.applicationContext
        if(Helper.isRecordServiceRunningInForeground(context, RecordVoiceService::class.java)) {
            onClickStopService()
        } else{
            AlertDialog.Builder(activity)
                .setTitle("Warning")
                .setIcon(R.drawable.baseline_info_24)
                .setMessage(activity?.getString(R.string.msg_stop_session_chat))
                .setPositiveButton(activity?.getString(R.string.btn_yes)) { dialog, which -> stopSession() }
                .setNegativeButton(activity?.getString(R.string.btn_no)) { dialog, which ->}
                .create()
                .show()
        }
    }
    fun stopSession() {
//        progressBar?.visibility= View.VISIBLE
        activity.runOnUiThread {
            GlobalScope.launch {
                try {
                    if(TestConnection.isOnline(context)) {
                        val respons = SessionChatControl(context!!).removeSession()
                        stopRecordForGroundService()
                        backToMainPage()
                    }
                }catch (e:Exception){
                 Log.e("StopSessionError!! ",e.message.toString())
                }
            }}

//         Helper.LoadFragment(MainHomeFragment(), activity?.supportFragmentManager, R.id.main_frame_layout)
    }
    fun onClickStopService() {

        AlertDialog.Builder(activity)
            .setTitle(context?.getString(R.string.nav_close_session))
            .setIcon(R.drawable.baseline_warning_24)
            .setMessage(context?.getString(R.string.msg_stop_session_chat))
            .setPositiveButton(context?.getString(R.string.btn_yes)) { dialog, which ->
                stopSession()
            }.setNegativeButton(context?.getString(R.string.btn_no)) { dialog, which ->}
            .create()
            .show()
    }
    fun stopRecordForGroundService(){

        try {
            if (Helper.isRecordServiceRunningInForeground(activity,
                    RecordVoiceService::class.java)) {
                val serviceIntent = Intent(activity!!, RecordVoiceService::class.java)
                activity.stopService(serviceIntent)
                callBackListener?.onCallBackExecuted()
//                setStopRecordForGroundServiceMode()
            }
        }catch (e:Exception){
            Log.d("Erorr-Close Forground Service", e.message.toString())
        }
    }
    fun backToMainPage(){
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity?.startActivity(intent)
        activity?.finish()
    }
}