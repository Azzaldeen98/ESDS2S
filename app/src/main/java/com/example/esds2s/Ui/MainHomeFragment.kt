package com.example.esds2s.Ui

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.Enums.TypeChat
import com.example.esds2s.databinding.FragmentMainHomeBinding
import com.google.rpc.Help


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MainHomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainHomeFragment : Fragment() {
    private var binding: FragmentMainHomeBinding?=null

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

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
        binding?.btnMainChat1?.setOnClickListener({v-> openSelectedChat(TypeChat.CHAT1) })
        binding?.btnMainChat2?.setOnClickListener({v-> openSelectedChat(TypeChat.CHAT2) })
        binding?.btnMainChat3?.setOnClickListener({v-> openSelectedChat(TypeChat.CHAT3) })
        binding?.btnMainChat4?.setOnClickListener({v-> openSelectedChat(TypeChat.CHAT4) })

       Helper.setAnimateAlphaForTool(binding?.mainContinerChatButtons)
//        setAnimateAlphaForTool(binding?.btnMainChat1)
//        setAnimateAlphaForTool(binding?.btnMainChat2)
//        setAnimateAlphaForTool(binding?.btnMainChat3)
//        setAnimateAlphaForTool(binding?.btnMainChat4)





    }


    fun openSelectedChat(typeChat: TypeChat) {

        val newFragment=CreateNewChatFragment()
        val bundle = Bundle()
        bundle.putInt("typeChat", typeChat.ordinal)
        newFragment.setArguments(bundle)
        Helper.LoadFragment(newFragment,activity?.supportFragmentManager, com.example.esds2s.R.id.main_frame_layout)
        Toast.makeText(this.context,"Chat: "+ typeChat?.toString(), Toast.LENGTH_SHORT).show()
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