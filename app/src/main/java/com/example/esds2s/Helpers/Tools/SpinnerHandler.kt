package com.example.esds2s.Helpers.Tools

import android.R
import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

class SpinnerHandler(private val activity: Activity, private val spinner: Spinner, private val callBackListener: AdapterView.OnItemSelectedListener): AdapterView.OnItemSelectedListener {

    private lateinit var adapter: ArrayAdapter<String>

    init {
    }

    fun setItemSelected(index:Int){
        if(index<0) return
        spinner?.setSelection(index)
    }

    fun initialize(items:List<String>,
                   simple_spinner_item:Int?=R.layout.simple_spinner_item,
                   simple_spinner_dropdown_item:Int?=R.layout.simple_spinner_dropdown_item){

        if(items==null || spinner==null) return
         adapter = ArrayAdapter<String>(activity?.applicationContext!!, simple_spinner_item!! , items?.toList()!!)
        adapter.setDropDownViewResource(simple_spinner_dropdown_item!!)
        spinner.setAdapter(adapter)
        spinner.setOnItemSelectedListener(this)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        callBackListener.onItemSelected(parent,view,position,id);
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        callBackListener.onNothingSelected(parent);
    }
}