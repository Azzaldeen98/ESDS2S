package com.example.esds2s

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.example.esds2s.ApiClient.Controlls.ChatAiServiceControll
import com.example.esds2s.ApiClient.Controlls.GeminiControll
import com.example.esds2s.Helpers.AudioPlayer
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
class ChatBotTextFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null


    private  var chat: Chat? = null
    private  var chatAiServiceControll: ChatAiServiceControll? = null
    private  var geminiControll: GeminiControll? = null
    private var btnSend: TextView? = null;
    private var textResult: TextView ? = null;
    private var textInput: TextInputEditText? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat_bot_text, container, false)
    }

    override fun onStart() {
        super.onStart()
//
        btnSend=this.activity?.findViewById<TextView>(R.id.btnSend1);
        textResult=this.activity?.findViewById<TextView>(R.id.textResult1);
        textInput=this.activity?.findViewById<TextInputEditText>(R.id.textInput1);
        chatAiServiceControll=ChatAiServiceControll(this.activity)
        btnSend?.setOnClickListener{v->
            sendMessage();
        }
    }

    fun sendMessage() {

        if(textInput!=null && !textInput?.text.isNullOrEmpty()) {


            activity?.runOnUiThread{

//                textResult?.setText("")

                GlobalScope.launch {

                    val call = chatAiServiceControll?.textToGenerateAudio(textInput?.text!!.toString())

                    call?.enqueue(object : retrofit2.Callback<GeminiResponse?> {
                        override fun onResponse(
                            call: Call<GeminiResponse?>,
                            response: retrofit2.Response<GeminiResponse?>
                        ) {
                            // التعامل مع الاستجابة هنا
                            if (response!!.isSuccessful) {
                                val responseData: GeminiResponse? = response.body()

                                if (response != null) {
                                    Log.d("res",responseData!!.description)
                                    val aud=AudioPlayer((this@ChatBotTextFragment).context!!)
                                    aud.start(responseData?.description)?.setOnCompletionListener { v->
                                        aud.stop()
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: Call<GeminiResponse?>, t: Throwable) {
                            // التعامل مع الأخطاء هنا

                        }
                    })

                }
            }

        }
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