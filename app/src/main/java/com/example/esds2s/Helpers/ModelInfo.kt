package com.example.esds2s.Helpers

import android.content.Context
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.Enums.AvailableLanguages
import com.example.esds2s.Helpers.Enums.GenderType
import com.example.esds2s.Models.ResponseModels.BaseChatResponse
import com.google.gson.Gson

class ModelInfo(private  val context: Context) {

 private  var modelData : BaseChatResponse?=null

    init {
           modelData=getInfo()
    }

    fun getInfo(): BaseChatResponse? {

        val currentModelinfoString = ExternalStorage.getValue(context, ContentApp.CURRENT_MODEL_INFO)?.toString()
        if (currentModelinfoString?.isNullOrEmpty() == false) {
            return Gson().fromJson(currentModelinfoString, BaseChatResponse::class.java)
        }
        return null
    }

    fun getLanguage(): AvailableLanguages? {

        val lang=LanguageInfo.getStorageSelcetedLanguage(context)
        if(lang?.index!!>-1 && lang?.index!! < AvailableLanguages.values().size){
           return AvailableLanguages.values()?.get(lang?.index!!)
        }
        return AvailableLanguages.ARABIC
    }

    fun getGender(): GenderType? {
        return if(modelData?.modeldescription=="Male") GenderType.MALE else GenderType.FEMALE
    }
}