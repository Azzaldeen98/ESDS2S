package com.example.esds2s.Ui

import android.R
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.esds2s.ApiClient.Controlls.SessionChatControl
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.CustomAdapter
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.Enums.TypeChat
import com.example.esds2s.Helpers.JsonStorageManager
import com.example.esds2s.Interface.IBaseServiceEventListener
import com.example.esds2s.Models.ResponseModels.BaseChatResponse
import com.example.esds2s.Services.TestConnection
import com.example.esds2s.databinding.FragmentMainHomeBinding
import com.google.firebase.inappmessaging.dagger.Component
import com.google.gson.Gson
import com.google.rpc.Help
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MainHomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainHomeFragment : Fragment(), IBaseServiceEventListener<ArrayList<BaseChatResponse>> {
    private var binding: FragmentMainHomeBinding?=null

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private  var recyclerview:RecyclerView?=null
    private var sessionChatControl: SessionChatControl? =null

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
        binding = FragmentMainHomeBinding.inflate(inflater, container, false)
        return binding?.getRoot()
    }

    override fun onStart() {
        super.onStart()

        binding?.btnMainCreateChat?.setOnClickListener({v-> openSelectedChat(TypeChat.NEWCHAT) })
//        binding?.btnMainChat1?.setOnClickListener({v-> openSelectedChat(TypeChat.CHAT1) })
//        binding?.btnMainChat2?.setOnClickListener({v-> openSelectedChat(TypeChat.CHAT2) })
//        binding?.btnMainChat3?.setOnClickListener({v-> openSelectedChat(TypeChat.CHAT3) })
//        binding?.btnMainChat4?.setOnClickListener({v-> openSelectedChat(TypeChat.CHAT4) })

        // getting the recyclerview by its id
//         recyclerview = activity?.findViewById<RecyclerView>(com.example.esds2s.R.id.recyclerview)
//        // this creates a vertical layout Manager
//         recyclerview?.layoutManager = LinearLayoutManager(this?.context)
        Helper.setAnimateAlphaForTool(binding?.mainContinerChatButtons,0)


    }

    private  fun laoudAllChats(){

        if(TestConnection.isOnline(this.context!!)) {
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    sessionChatControl?.getAllChats(this@MainHomeFragment)
                }
            }
        }
    }

    fun openSelectedChat(typeChat: TypeChat) {

        val newFragment=CreateNewChatFragment()
        val bundle = Bundle()
        bundle.putInt("typeChat", typeChat.ordinal)
        newFragment.setArguments(bundle)
        Helper.LoadFragment(newFragment,activity?.supportFragmentManager, com.example.esds2s.R.id.main_frame_layout)
//        Toast.makeText(this.context,"Chat: "+ typeChat?.toString(), Toast.LENGTH_SHORT).show()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MainHomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainHomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }


    override fun onRequestIsSuccess(response: ArrayList<BaseChatResponse>) {

        if(response!=null) {

            val storage= JsonStorageManager(this?.context!!,ContentApp.API_TEMP_STORAGE)
            storage.saveList(ContentApp.CHATS_LIST_STORAGE,response)
            val chats_list: List<BaseChatResponse>?= storage.getList(ContentApp.CHATS_LIST_STORAGE,BaseChatResponse::class.java)

            Log.d("CHATS_LIST_STORAGE",Gson().toJson(chats_list)!!)


//            val adapter = CustomAdapter(response)
//            recyclerview?.adapter = adapter

        }

    }

    override fun onRequestIsFailure(error: String) {
        Log.d("onRequestIsFailure",error)
    }


}