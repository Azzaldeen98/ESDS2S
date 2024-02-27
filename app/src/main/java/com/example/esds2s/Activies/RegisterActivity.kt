package com.example.esds2s.Activies

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.example.esds2s.ApiClient.Controlls.AuthControl
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Interface.IAuthServiceEventListener
import com.example.esds2s.Models.ResponseModels.AuthResponse
import com.example.esds2s.R

class RegisterActivity : AppCompatActivity() ,IAuthServiceEventListener{

    private  var logo_content: LinearLayout?=null
    private  var btn_register: Button?=null
    private  var authControl: AuthControl?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        authControl=AuthControl(this)
        logo_content=findViewById(R.id.first_logo_content)
        btn_register=findViewById(R.id.btnRegisterAccount)
        btn_register?.setOnClickListener{v-> createRegister() }

    }

    private  fun createRegister(){

        if(!ExternalStorage.existing(this,"Token") && authControl!=null) {

            authControl?.register(this)
        }

    }

    override fun onRequestIsSuccess(response: AuthResponse) {
        TODO("Not yet implemented")
    }

    override fun onRequestIsFailure(error: String) {
        TODO("Not yet implemented")
    }
}