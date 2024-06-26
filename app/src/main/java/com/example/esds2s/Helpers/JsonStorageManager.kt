package com.example.esds2s.Helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.esds2s.ContentApp.ContentApp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class JsonStorageManager(private var context: Context?,private var prefsKey:String?=null) {

    private lateinit var sharedPreferences: SharedPreferences
    private val gson: Gson

    init {
        if (context != null) {
            if(prefsKey==null)
                prefsKey=ContentApp.API_TEMP_STORAGE
            sharedPreferences = context?.getSharedPreferences(prefsKey, Context.MODE_PRIVATE)!!
        }
        gson = Gson()
    }


    private  fun preferenceIsNotNull():Boolean{
        if (sharedPreferences == null) {
            Log.e("Error!","sharedPreferences is null")
            return false;
        }
        return true;
    }
    fun <T> saveList(key: String?, list: List<T>?) {
        if(!preferenceIsNotNull()) return

        val json = gson.toJson(list)
        val editor = sharedPreferences.edit()
        editor.putString(key, json)
        editor.commit()
    }

    fun  exists(key: String?):Boolean {
        if(!preferenceIsNotNull()) false
      return sharedPreferences.contains(key)

    }

    fun <T> getList(key: String?, clazz: Class<T>?): List<T>? {

        if(!preferenceIsNotNull())  return null;

        val json = sharedPreferences.getString(key, null)
        val type = TypeToken.getParameterized(
            MutableList::class.java, clazz
        ).type
        return gson.fromJson(json, type)
    }

    fun  delete(key: String?):Boolean? {
        if(key==null || !exists(key))
            return false
        val editor = sharedPreferences?.edit()
        editor?.remove(key!!)
        Log.e("delete "," delete Json Storage")
       return editor?.commit()
    }
}
