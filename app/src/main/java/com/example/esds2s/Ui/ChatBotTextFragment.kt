package com.example.esds2s.Ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.LanguageInfo
import com.example.esds2s.Interface.IGeminiServiceEventListener
import com.example.esds2s.Interface.IUplaodAudioEventListener
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.R
import com.example.esds2s.Services.TestConnection
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
/**
 * A simple [Fragment] subclass.
 * Use the [ChatBotTextFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatBotTextFragment : Fragment(), IUplaodAudioEventListener, IGeminiServiceEventListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var message:String?=null
    private  var speechChatControl: SpeechChatControl? = null
    private var count: Int? = 0;
    private var btnSend: TextView? = null;
    private var textResult: TextView? = null;
    private var textInput: TextInputEditText? = null;
    private var text_gchat_message_me: TextView? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_chat_bot_text, container, false)
    }
    override fun onStart() {
        super.onStart()
        btnSend=this.activity?.findViewById<TextView>(R.id.btnSend1);
        textResult=this.activity?.findViewById<TextView>(R.id.textResult1);
        textInput=this.activity?.findViewById<TextInputEditText>(R.id.textInputMessage);
        text_gchat_message_me=this.activity?.findViewById<TextView>(R.id.text_gchat_message_me);
        speechChatControl= SpeechChatControl(this.context!!)
        btnSend?.setOnClickListener{v->
            btnSend?.backgroundTintList= ColorStateList.valueOf(Color.GRAY);
            val color = ContextCompat.getColor(
                this@ChatBotTextFragment.context!!,
                R.color.purple_700
            ) // Color.parseColor(R.color.purple_700.toString())
            btnSend?.backgroundTintList= ColorStateList.valueOf(color);
            sendMessage();
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}

        changeLanguageMode()
        loadPresetUserLanguage()
    }
    fun sendMessage() {

         message= textInput?.text.toString()

        if(message!=null && !message.isNullOrEmpty()) {

            btnSend?.isEnabled=false;
            textInput?.isEnabled=false;
            text_gchat_message_me?.visibility= View.VISIBLE
            text_gchat_message_me?.text=message
            textInput?.text?.clear()

            Thread{ activity?.runOnUiThread {
                GlobalScope.launch {
                    if(TestConnection.isOnline(this@ChatBotTextFragment.context!!, true)) {
                        try {
                            speechChatControl?.messageToGeneratorAudio(message, this@ChatBotTextFragment)
                        }catch (e:Exception){
                            Log.d("Error", e.message.toString())
                        }
                    }
                } } }.start()
        }
    }
    fun changeLanguageMode() {
        if(getPresetUserLanguage()!=null
            && getPresetUserLanguage()?.trim()?.contains("ar")!!){
            textInput?.textDirection=View.TEXT_DIRECTION_RTL
            textInput?.setHint(getString(R.string.input_chat_message_ar))
        }
        else{
            textInput?.textDirection=View.TEXT_DIRECTION_LTR
            textInput?.setHint(getString(R.string.input_chat_message_en))
        }
    }

//    override fun onResume() {
//        super.onResume()
//        Toast.makeText(this.context, "onResume: onResume", Toast.LENGTH_SHORT).show()
//        changeLanguageMode()
//    }
    private fun loadPresetUserLanguage() {
        val languageInfo = LanguageInfo.getStorageSelcetedLanguage(this?.context);
        if(languageInfo!=null) {
            Toast.makeText(this.context,  languageInfo.code!!,Toast.LENGTH_SHORT).show()
        }
    }
    override fun onRequestIsSuccess(response: GeminiResponse) {
        count=0
        if (response != null ){ //&&  Helper.isAudioFile(response?.description)) {
            btnSend?.isEnabled=true;
            textInput?.isEnabled=true;
            val player: MediaPlayer
            val media_player= AudioPlayer(this@ChatBotTextFragment.context)
            if(!Helper.isAudioFile(response?.description)) {
                val sound_id = Helper.getDefaultSoundResource()
                Log.e("isAudioFile", sound_id.toString());
                 player=media_player?.startFromRowResource(this.context!!, sound_id)!!

            }else {
                player = media_player?.start(response?.description)!!
            }
            player?.setOnCompletionListener{v->
                v.stop()
                v.reset()
                v.release()
            }
        }
    }
    override fun onRequestIsFailure(error: String) {
        Log.e("Error33", error)
        btnSend?.isEnabled=true;
        textInput?.isEnabled=true;

        if(count!!<3 && message!=null){
            sendMessage();
            count?.plus(1)
        }
        else
            count=0
    }
    private fun getPresetUserLanguage():String? {
        val languageInfo = LanguageInfo.getStorageSelcetedLanguage(this?.context);
        if(languageInfo!=null)
            return  languageInfo.code!!
        return  null
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ChatBotTextFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChatBotTextFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}