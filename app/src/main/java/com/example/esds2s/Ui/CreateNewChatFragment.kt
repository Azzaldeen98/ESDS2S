package com.example.esds2s.Ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.esds2s.ApiClient.Controlls.SessionChatControl
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.Enums.TypeChat
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.JsonStorageManager
import com.example.esds2s.Helpers.LanguageInfo
import com.example.esds2s.Interface.IBaseServiceEventListener
import com.example.esds2s.Models.RequestModels.CustomerChatRequest
import com.example.esds2s.Models.ResponseModels.BaseChatResponse
import com.example.esds2s.R
import com.example.esds2s.Activies.RecordAudioActivity
import com.example.esds2s.Helpers.Enums.GenderType
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Services.ModelLanguages
import com.example.esds2s.databinding.FragmentCreateNewChatBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import kotlinx.coroutines.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CreateNewChatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateNewChatFragment : Fragment(), IBaseServiceEventListener<ArrayList<BaseChatResponse>> {

    private var autocompleteTV: AutoCompleteTextView?=null
    private lateinit var receivedChat: BaseChatResponse
    private var binding: FragmentCreateNewChatBinding?=null
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var selectedLanguage: String? = null
    private var selectedLanguageIndex: Int? = 0
    private var selectedChat: BaseChatResponse? = null
    private var typeChat: TypeChat? = null
    private lateinit var  progressPar: RelativeLayout
    private var  btn_back: TextView? = null
    private var  internaal_header: TextView? = null
    private var  dropdownChats: TextInputLayout? = null
    private var arrayAdapterLanguage: ArrayAdapter<String>? =null
    private var arrayAdapterChats: ArrayAdapter<BaseChatResponse>? =null
    private var sessionChatControl: SessionChatControl? =null
    private var   arrayAdapter : ArrayAdapter<String>? =null
    private var  modelLanguages:ModelLanguages ?=null
    private var  genderType:GenderType =GenderType.MALE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCreateNewChatBinding.inflate(inflater, container, false)
        // Retrieve data from arguments
        // Retrieve data from arguments
        val args = arguments
        if (args != null) {
            val _value:Int?= args.getInt("typeChat", TypeChat.NEWCHAT.ordinal)
            if(_value!=null)
                typeChat=enumValues<TypeChat>().getOrNull(_value)
            if(args.containsKey("spacificChat"))
                selectedChat = (arguments?.getSerializable("spacificChat") as? BaseChatResponse)!!

            // Use the data as needed
        }
        return binding?.getRoot()
    }
    private fun internalHeader(){

       val btn_back:TextView?=activity?.findViewById(R.id.internal_head_btn_back)
        val internaal_header:TextView?=activity?.findViewById(R.id.internal_head_title)

        internaal_header?.setText(getString(R.string.chat_info_page))
        btn_back?.setOnClickListener {
            Helper.LoadFragment(MainHomeFragment(), activity?.supportFragmentManager, R.id.main_frame_layout)
        }
    }
    override fun onStart() {
        super.onStart()

        internalHeader()
        sessionChatControl= SessionChatControl(this.context!!)
        progressPar=activity?.findViewById(R.id.progressPar1)!!
        dropdownChats=activity?.findViewById(R.id.DropDownListChat)

        if(typeChat!=null && typeChat?.ordinal!! == TypeChat.SPACIFICCHAT.ordinal && selectedChat!=null) {
            dropdownChats?.isEnabled=false
            binding?.InputChatDescription?.isEnabled=false
            binding?.InputChatDescriptionData?.setText(selectedChat?.modeldescription)
            binding?.inputChatName?.visibility=View.GONE
            binding?.InputChatNameData?.setText(selectedChat?.scope)
        }
        binding?.btnSubmitChatInfo?.isEnabled=true
        binding?.btnSubmitChatInfo?.setOnClickListener{ submitCreateChat() }
        progressPar.visibility = View.VISIBLE
        genderType=if(selectedChat?.modeldescription=="Male") GenderType.MALE else GenderType.FEMALE
        insilizationLanguagesList(genderType)
        laoudAllChats()
    }
    private  fun uplaodAllChatsFromLocalStorage():Boolean{
        progressPar?.visibility=View.GONE
        var storage :JsonStorageManager?=null
        var chatsList :List<BaseChatResponse>?=null

        try {
             storage = JsonStorageManager(this?.context!!, ContentApp.API_TEMP_STORAGE)
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

        try {
                if(typeChat== TypeChat.NEWCHAT) {
                    if(!uplaodAllChatsFromLocalStorage()){
                        progressPar?.visibility = View.VISIBLE
                        GlobalScope.launch {
                          withContext(Dispatchers.IO) {
                        sessionChatControl?.getAllChats(this@CreateNewChatFragment) } }
                    }
                }
                else if(typeChat== TypeChat.SPACIFICCHAT && selectedChat!= null) {
                    val data = ArrayList<BaseChatResponse>()
                    data.add(selectedChat!!)
                    insilizationChatsList(data)
                    autocompleteTV?.setText(selectedChat?.scope, false)
                }
        } catch (e: java.lang.Exception) {
            progressPar?.visibility = View.GONE
        }

    }
    @SuppressLint("SuspiciousIndentation")
    private  fun insilizationChatsList(chats: ArrayList<BaseChatResponse>){

        progressPar?.visibility=View.GONE
        arrayAdapter = ArrayAdapter<String>(this?.context!!, R.layout.dropdown_item, chats.map { it.scope })
        autocompleteTV = activity?.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextViewChat)
        autocompleteTV?.setAdapter(arrayAdapter)
        autocompleteTV?.setOnItemClickListener { parent, view, position, id -> selectedChat=chats?.get(position)
            Toast.makeText(requireContext(), "Selected: ", Toast.LENGTH_SHORT).show()
        }
    }
    private  fun insilizationLanguagesList(gender:GenderType){

        modelLanguages = ModelLanguages(this?.context!!).getGenderLanguages(gender)
        val languageNames =ArrayList(modelLanguages?.getLanguagesName())
        arrayAdapterLanguage = ArrayAdapter<String>(this?.context!!, R.layout.dropdown_item, languageNames)
        val autocompleteTV = activity?.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextViewLanguage)
        autocompleteTV?.setAdapter(arrayAdapterLanguage)
        autocompleteTV?.setOnItemClickListener { parent, view, position, id ->
//            val languagesCode =  resources.getStringArray(R.array.language_codes)
            val languagesCode =ArrayList(modelLanguages?.getLanguagesCode())
            selectedLanguage = languagesCode?.get(position) as String
            selectedLanguageIndex=position
            Toast.makeText(requireContext(), "Selected: $selectedLanguage", Toast.LENGTH_SHORT).show()
        }
    }
    private  fun checkInputData():Boolean{

        var countEmpty=0
        if(selectedLanguage==null) {
            countEmpty++
            AlertDialog.Builder(this.context)
                .setTitle("Alert")
                .setIcon(R.drawable.baseline_warning_24)
                .setMessage(getString(R.string.msg_choose_the_language))
                .setPositiveButton(getString(R.string.btn_ok)) { dialog, which -> }
                .create()
                .show()
            return  false
        }

        if(countEmpty==0 && typeChat?.ordinal!! > TypeChat.NEWCHAT.ordinal)
            return  true;

        if(binding?.InputChatNameData?.text.isNullOrEmpty()) {
            countEmpty++
            Helper.setEditTextError(binding?.InputChatNameData,getString(R.string.input_is_empty))
        }
        if(binding?.InputChatDescriptionData?.text.isNullOrEmpty()) {
            countEmpty++
            Helper.setEditTextError(binding?.InputChatDescriptionData,getString(R.string.input_is_empty))
        }

        if(selectedChat==null) {
            countEmpty++
            AlertDialog.Builder(this.context)
                .setTitle("Alert")
                .setIcon(R.drawable.baseline_warning_24)
                .setMessage(getString(R.string.msg_choose_the_chat))
                .setPositiveButton(getString(R.string.btn_ok)) { dialog, which -> }
                .create()
                .show()
//            Helper.setEditTextError(dropdownChats,getString(R.string.input_is_empty))
        }



        Toast.makeText(this.context, countEmpty?.toString(), Toast.LENGTH_SHORT).show()
        return countEmpty==0
    }
    private  fun submitCreateChat(){

        progressPar.visibility = View.VISIBLE
        binding?.btnSubmitChatInfo?.isEnabled=false
        val mainHandler = Handler(Looper.getMainLooper())
        if(!checkInputData()) return;
        val body=CustomerChatRequest(
            token_chat =selectedChat?.token!!,
            name = selectedChat?.scope.toString(),
            description=binding?.InputChatDescriptionData?.text.toString())
        LanguageInfo.setStorageSelcetedLanguage(this?.context,selectedLanguage,selectedLanguageIndex!!)
        LanguageInfo.setStorageSelcetedLanguage(this?.context,selectedLanguage,selectedLanguageIndex!!)
        if(selectedChat!=null)
            ExternalStorage.storage(this?.activity,ContentApp.CURRENT_MODEL_INFO,Gson().toJson(selectedChat))

        GlobalScope.async {
            withContext(Dispatchers.IO) {
                try {
                    var response = sessionChatControl?.createSessionChat(body)
                    if (response != null) {
                        Log.d("CustomerChatResponse55", Gson().toJson(response))
                        val intent = Intent(this@CreateNewChatFragment.context, RecordAudioActivity::class.java)
                        startActivity(intent)
                    } else {
                        mainHandler.post(java.lang.Runnable {
                            AlertDialog.Builder(this@CreateNewChatFragment.context)
                                .setTitle("Alert")
                                .setIcon(R.drawable.baseline_warning_24)
                                .setMessage(getString(R.string.msg_failed_the_session_create))
                                .setPositiveButton(getString(R.string.btn_ok)) { dialog, which -> }
                                .create()
                                .show()
                        })
                    }
                }catch (e:Exception){ Log.e("Customer Chat",e.message.toString())}
                finally { mainHandler.post(java.lang.Runnable { progressPar!!.visibility = View.GONE }) }

//
//
            }
        }

//        val deferredResult by lazyDeferred {
//            // Simulate a long-running operation
//            sessionChatControl?.createSessionChat(body)
//
//        }



//        runBlocking {
//            val result = deferredResult.await()
//        }




    }
    override fun onRequestIsSuccess(response: ArrayList<BaseChatResponse>) {
        try {

            Log.d("response",Gson().toJson(response))

            if (response != null) {
                insilizationChatsList(response!!)
            }
        }catch (e:java.lang.Exception){

        }
        progressPar?.visibility = View.GONE
//            val storage= JsonStorageManager(this?.context!!,ContentApp.API_TEMP_STORAGE)
////            val arr: List<BaseChatResponse> =response.toList()
//            storage.saveList(ContentApp.CHATS_LIST_STORAGE,response)
//            Log.d("CHATS_LIST_STORAGE_777",Gson().toJson()

    }
    override fun onRequestIsFailure(error: String) {
        progressPar?.visibility=View.GONE
        Log.d("!response error",error)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CreateNewChatFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CreateNewChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }


}