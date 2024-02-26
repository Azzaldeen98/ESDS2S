package com.example.esds2s.Ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.esds2s.ApiClient.Controlls.SessionChatControl
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.CustomAdapter
import com.example.esds2s.Helpers.Enums.TypeChat
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.JsonStorageManager
import com.example.esds2s.Interface.IBaseItemClickListener
import com.example.esds2s.Interface.IBaseServiceEventListener
import com.example.esds2s.Models.ResponseModels.BaseChatResponse
import com.example.esds2s.Services.TestConnection
import com.example.esds2s.databinding.FragmentMainHomeBinding
import com.google.android.flexbox.*
import com.google.gson.Gson
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
class MainHomeFragment : Fragment(), IBaseServiceEventListener<ArrayList<BaseChatResponse>>,IBaseItemClickListener<BaseChatResponse> {
    private lateinit var adapter: CustomAdapter
    private var binding: FragmentMainHomeBinding?=null
    private var  progressPar: RelativeLayout? = null

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

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()

        progressPar=activity?.findViewById(com.example.esds2s.R.id.progressPar1)
        // getting the recyclerview by its id
        recyclerview = activity?.findViewById<RecyclerView>(com.example.esds2s.R.id.RecyclerViewChatsList)
        val flexLayoutManager = FlexboxLayoutManager(context)
        flexLayoutManager.alignItems =   AlignItems.STRETCH  // يحدد flex-wrap: wrap
        flexLayoutManager.justifyContent =   JustifyContent.SPACE_AROUND  // يحدد flex-wrap: wrap
        flexLayoutManager.flexWrap =  FlexWrap.WRAP  // يحدد flex-wrap: wrap
        recyclerview?.layoutManager= flexLayoutManager
        binding?.btnMainCreateChat?.setOnClickListener({v-> openSelectedChat(TypeChat.NEWCHAT) })
//        binding?.btnMainCreateChat?.visibility = View.INVISIBLE
        laoudAllChats()

    }

    private  fun uplaodAllChatsFromLocalStorage():Boolean{

        progressPar?.visibility = View.VISIBLE
        var storage :JsonStorageManager?=null
        var chatsList :List<BaseChatResponse>?=null

//        binding?.btnMainCreateChat?.visibility = View.VISIBLE
        try {
            storage = JsonStorageManager(this?.context!!, ContentApp.API_TEMP_STORAGE)
            if(!storage.exists(ContentApp.CHATS_LIST_STORAGE))
                return  false
            chatsList = storage.getList(ContentApp.CHATS_LIST_STORAGE, BaseChatResponse::class.java)
        } catch (e:java.lang.Exception){
            return  false
        } finally {
            if (chatsList == null)
                return false
            else {
                insilizationChatsList(chatsList as ArrayList<BaseChatResponse>)
                return true
            }
        }
    }
    private  fun laoudAllChats(){
        progressPar?.visibility = View.VISIBLE
        try {
            // upload data from local storage
            if(!uplaodAllChatsFromLocalStorage()) {
                // upload data from Api server
                if (TestConnection.isOnline(this.context!!)) {
                    sessionChatControl = SessionChatControl(this.context!!)
                    GlobalScope.launch {
                        withContext(Dispatchers.IO) {
                            sessionChatControl?.getAllChats(this@MainHomeFragment)
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e("Error-laoudAllChats!! ", e?.message.toString())
            progressPar?.visibility = View.GONE
        }
    }


    override fun onRequestIsSuccess(response: ArrayList<BaseChatResponse>) {

        try {
            if (response != null) {

                insilizationChatsList(response)
                val storage= JsonStorageManager(this?.context!!,ContentApp.API_TEMP_STORAGE)
                storage.saveList(ContentApp.CHATS_LIST_STORAGE,response)
                val chats_list: List<BaseChatResponse>?= storage.getList(ContentApp.CHATS_LIST_STORAGE,BaseChatResponse::class.java)
                Log.d("CHATS_LIST_STORAGE", Gson().toJson(chats_list)!!)

            }
        }catch (e:Exception){
            Log.e("Error-laoudAllChats-response  ", e?.message.toString())
        }
        finally {
            progressPar?.visibility = View.GONE
        }

    }

    override fun onRequestIsFailure(error: String) {
        progressPar?.visibility = View.GONE
        Log.e("onRequestIsFailure",error)
    }
    private  fun insilizationChatsList(chats: ArrayList<BaseChatResponse>){

        binding?.btnMainCreateChat?.visibility = View.VISIBLE
        Helper.setAnimateAlphaForTool(binding?.mainContinerChatButtons,0)
         adapter = CustomAdapter(chats,this,this.activity!!)
        recyclerview?.adapter = adapter
        progressPar?.visibility = View.GONE
    }

    override fun onItemClick(item: BaseChatResponse) {

        if(item!=null) {
//            Toast.makeText(this?.context,item.scope,Toast.LENGTH_SHORT).show()
            openSelectedChat(TypeChat.SPACIFICCHAT,item)

        }

    }

    @SuppressLint("SuspiciousIndentation")
    fun openSelectedChat(typeChat: TypeChat, spacificChat:BaseChatResponse?=null) {


        try {
            val newFragment = CreateNewChatFragment()
            val bundle = Bundle()
            bundle.putInt("typeChat", typeChat.ordinal)
        if(typeChat==TypeChat.SPACIFICCHAT && spacificChat!=null)
            bundle.putSerializable("spacificChat",spacificChat!!)
            newFragment.setArguments(bundle)
            Helper.LoadFragment(newFragment,
                activity?.supportFragmentManager,
                com.example.esds2s.R.id.main_frame_layout)

        }catch (e:Exception){
            Log.e("Error - go to  CreateNewChatFragment",e.message.toString())
        }
//        Toast.makeText(this.context,"Chat: "+ typeChat?.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
//        progressPar?.visibility = View.VISIBLE

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




}