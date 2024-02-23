package com.example.esds2s

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.set
import com.example.esds2s.ApiClient.Controlls.ChatAiServiceControll
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.Interface.IUplaodAudioEventListener
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.google.ai.client.generativeai.Chat
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChatBotTextFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatBotTextFragment : Fragment(), IUplaodAudioEventListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var message:String?=null
    private  var chatAiServiceControll: ChatAiServiceControll? = null
    private var count: Int? = 0;
    private var btnSend: TextView? = null;
    private var textResult: TextView ? = null;
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
        textInput=this.activity?.findViewById<TextInputEditText>(R.id.textInput1);
        text_gchat_message_me=this.activity?.findViewById<TextView>(R.id.text_gchat_message_me);

        chatAiServiceControll=ChatAiServiceControll(this.context!!)

        btnSend?.setOnClickListener{v->
            btnSend?.backgroundTintList=ColorStateList.valueOf(Color.GRAY);
            Thread.sleep(1000)
            val color = Color.parseColor("#FF6200EE")
            btnSend?.backgroundTintList=ColorStateList.valueOf(color);

            sendMessage();
        }
    }
    fun sendMessage() {

         message= textInput?.text.toString()

        if(message!=null && !message.isNullOrEmpty()) {

            btnSend?.isEnabled=false;
            textInput?.isEnabled=false;
            text_gchat_message_me?.visibility=View.VISIBLE
            text_gchat_message_me?.text=message
            textInput?.text?.clear()


//            Thread{ activity?.runOnUiThread { GlobalScope.launch {
//                    chatAiServiceControll?.messageToGeneratorAudio(message,)
//                } } }.start()
        }
    }

    override fun onRequestIsSuccess(response: GeminiResponse) {
        count=0
        if (response != null) {
            btnSend?.isEnabled=true;
            textInput?.isEnabled=true;
            Log.d("res",response!!.description)
            val aud=AudioPlayer((this@ChatBotTextFragment).context!!)
            aud.start(response?.description)?.setOnCompletionListener { v-> aud.stop() }
        }
    }
    override fun onRequestIsFailure(error: String) {
        Log.e("Error33",error)
        btnSend?.isEnabled=true;
        textInput?.isEnabled=true;

        if(count!!<3 && message!=null){
            sendMessage();
            count?.plus(1)
        }
        else
            count=0
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