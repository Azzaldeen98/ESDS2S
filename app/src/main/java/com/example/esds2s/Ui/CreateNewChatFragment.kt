package com.example.esds2s.Ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.esds2s.ApiClient.Controlls.SessionChatControl
import com.example.esds2s.ApiClient.Controlls.SpeechChatControl
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.Enums.TypeChat
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Interface.IBaseServiceEventListener
import com.example.esds2s.Models.ResponseModels.BaseChatResponse
import com.example.esds2s.R
import com.example.esds2s.RecordAudioActivity
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

    private var binding: FragmentCreateNewChatBinding?=null
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var selectedLanguage: String? = null
    private var selectedChat: BaseChatResponse? = null
    private var typeChat: TypeChat? = null
    private var  progressPar: RelativeLayout? = null
    private var  dropdownChats: TextInputLayout? = null
    private var arrayAdapterLanguage: ArrayAdapter<String>? =null
    private var arrayAdapterChats: ArrayAdapter<BaseChatResponse>? =null
    private var sessionChatControl: SessionChatControl? =null

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

            // Use the data as needed
        }
        return binding?.getRoot()
    }

    override fun onStart() {
        super.onStart()

        sessionChatControl= SessionChatControl(this.context!!)
        progressPar=activity?.findViewById(R.id.progressPar1)
        dropdownChats=activity?.findViewById(R.id.DropDownListChat)

        progressPar?.visibility=View.VISIBLE


        if(typeChat!=null && typeChat?.ordinal!! > TypeChat.NEWCHAT.ordinal) {
            dropdownChats?.isEnabled=false
            binding?.InputChatDescription?.isEnabled=false
            binding?.InputChatNameData?.visibility=View.GONE
        }

        laoudAllChats()
        insilizationLanguagesList()
        binding?.btnSubmitChatInfo?.setOnClickListener{submitChatInfo() }
        binding?.btnChatInfoBack?.setOnClickListener {
            Helper.LoadFragment(MainHomeFragment(),
                activity?.supportFragmentManager,
                R.id.main_frame_layout)
        }
    }


    private  fun laoudAllChats(){
        GlobalScope.launch {

                withContext(Dispatchers.IO) {
                    sessionChatControl?.getAllChats(this@CreateNewChatFragment)
                }

        }
    }
    private  fun insilizationChatsList(chats: ArrayList<BaseChatResponse>){

        progressPar?.visibility=View.GONE
       var arrayAdapterChats = ArrayAdapter<String>(this?.context!!, R.layout.dropdown_item, chats.map { it.scope })
        val autocompleteTV = activity?.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextViewChat)
        autocompleteTV?.setAdapter(arrayAdapterChats)
        autocompleteTV?.setOnItemClickListener { parent, view, position, id ->

         var selectedChat=arrayAdapterChats?.getItem(position)

            Toast.makeText(requireContext(), "Selected: ", Toast.LENGTH_SHORT).show()
        }
    }
    private  fun insilizationLanguagesList(){

        val languages = resources.getStringArray(R.array.language_names)
        arrayAdapterLanguage = ArrayAdapter<String>(this?.context!!, R.layout.dropdown_item, languages)
        val autocompleteTV = activity?.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextViewLanguage)
        autocompleteTV?.setAdapter(arrayAdapterLanguage)
        autocompleteTV?.setOnItemClickListener { parent, view, position, id ->
            val languagesCode =  resources.getStringArray(R.array.language_codes)
            selectedLanguage = languagesCode?.get(position) as String
            ExternalStorage.storage(this?.context,"Lang",selectedLanguage)
            Toast.makeText(requireContext(), "Selected: $selectedLanguage", Toast.LENGTH_SHORT).show()
        }
    }

    private  fun checkInputData():Boolean{

        if(typeChat?.ordinal!! > TypeChat.NEWCHAT.ordinal)
            return  true;

        var countEmpty=0
        if(binding?.InputChatNameData?.text.isNullOrEmpty()) {
            countEmpty++
            Helper.setEditTextError(binding?.InputChatNameData,getString(R.string.input_is_empty))
        }
        if(binding?.InputChatDescriptionData?.text.isNullOrEmpty()) {
            countEmpty++
            Helper.setEditTextError(binding?.InputChatDescriptionData,getString(R.string.input_is_empty))
        }

//        if(binding?.InputChatLanguageData?.text.isNullOrEmpty()) {
//            countEmpty++
//            Helper.setEditTextError(binding?.InputChatLanguageData,getString(R.string.input_is_empty))
//        }

        Toast.makeText(this.context, countEmpty?.toString(), Toast.LENGTH_SHORT).show()
        return countEmpty==0
    }
    private  fun submitChatInfo(){

        if(!checkInputData())
            return;

        val  intent = Intent(this@CreateNewChatFragment.context, RecordAudioActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

//        val newFragment=MainHomeFragment()
//        val bundle = Bundle()
//        bundle.putString("chatName",binding?.InputChatNameData?.text.toString() )
//        bundle.putString("chatDescript", binding?.InputChatDescriptionData?.text.toString())
//        bundle.putString("chatLang", binding?.InputChatLanguageData?.text.toString())
//        newFragment.setArguments(bundle)
//        Helper.LoadFragment(newFragment,activity?.supportFragmentManager, R.id.main_frame_layout)


    }

    override fun onRequestIsSuccess(response: ArrayList<BaseChatResponse>) {
        Log.d("response",Gson().toJson(response))

        if(response!=null)
            insilizationChatsList(response!!)
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