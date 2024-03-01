package com.example.esds2s.Services

import android.content.Context
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.Enums.GenderType
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Models.ResponseModels.BaseChatResponse
import com.example.esds2s.R
import com.google.gson.Gson


class ModelLanguages(private  val context: Context) {

    private var languagesCode:MutableList<String> = mutableListOf()
    private var languagesName:MutableList<String> = mutableListOf()

    fun getLanguagesCode(): List<String>? {
        return languagesCode?.toList()
    }

    fun getLanguagesName(): List<String>? {
        return languagesName?.toList()
    }

    fun getGenderLanguages(gender: GenderType):ModelLanguages{

        val codes = context?.resources?.getStringArray(R.array.language_codes)
        val names = context?.resources?.getStringArray(R.array.language_names)

        if(codes!=null && names!=null){
            var i:Int = 0
            while(i<names.size){
                if(i%2==gender.ordinal){
                    languagesCode.add(codes.get(i))
                    languagesName.add(names.get(i))
                }
                i++
            }
        }
        return  this
    }



}