package com.example.esds2s.Ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.Enums.TypeChat
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.R
import com.example.esds2s.RecordAudioActivity
import com.example.esds2s.RegisterActivity
import com.example.esds2s.databinding.FragmentCreateNewChatBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CreateNewChatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateNewChatFragment : Fragment() {

    private var binding: FragmentCreateNewChatBinding?=null
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var selectedLanguage: String? = null
    private var typeChat: TypeChat? = null
    private var arrayAdapter: ArrayAdapter<String>? =null

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

        if(typeChat!=null && typeChat?.ordinal!! > TypeChat.NEWCHAT.ordinal) {
            binding?.InputChatName?.isEnabled=false
            binding?.InputChatDescription?.isEnabled=false
            laodChatInformation(typeChat)
        }
        insilizationLanguagesList()
        binding?.btnSubmitChatInfo?.setOnClickListener{submitChatInfo() }
        binding?.btnChatInfoBack?.setOnClickListener {
            Helper.LoadFragment(MainHomeFragment(),
                activity?.supportFragmentManager,
                R.id.main_frame_layout)


        }
    }

    private  fun insilizationLanguagesList(){

        val languages = resources.getStringArray(R.array.language_names)
        // create an array adapter and pass the required parameter
        // in our case pass the context, drop down layout , and array.
        arrayAdapter = ArrayAdapter<String>(this?.context!!, R.layout.dropdown_item, languages)
        // get reference to the autocomplete text view
        val autocompleteTV = activity?.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)
        // set adapter to the autocomplete tv to the arrayAdapter
        autocompleteTV?.setAdapter(arrayAdapter)
        autocompleteTV?.setOnItemClickListener { parent, view, position, id ->
            val languagesCode =  resources.getStringArray(R.array.language_codes)
            selectedLanguage = languagesCode?.get(position) as String
            ExternalStorage.storage(this?.context,"Lang",selectedLanguage)
            // Do something with the selected item, for example, show a toast
            Toast.makeText(requireContext(), "Selected: $selectedLanguage", Toast.LENGTH_SHORT).show()
        }
    }

    private  fun laodChatInformation(typeChat: TypeChat?){
        binding?.InputChatNameData?.setText("Chat Name")
        binding?.InputChatDescriptionData?.setText("Chat Description")
    }

    private  fun checkInputData():Boolean{

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