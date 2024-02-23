package com.example.esds2s

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.example.esds2s.ApiClient.Controlls.AuthControll
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Interface.IAuthServiceEventListener
import com.example.esds2s.Models.ResponseModels.AuthResponse

class RegisterActivity : AppCompatActivity() ,IAuthServiceEventListener{

    private  var logo_content: LinearLayout?=null
    private  var btn_register: Button?=null
    private  var authControll: AuthControll?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        authControll=AuthControll(this)
        logo_content=findViewById(R.id.first_logo_content)
        btn_register=findViewById(R.id.btnRegisterAccount)
        btn_register?.setOnClickListener{v-> createRegister() }

    }

    private  fun createRegister(){

        if(!ExternalStorage.existing(this,"Token") && authControll!=null) {

            authControll?.register(this)
        }

    }

    override fun onRequestIsSuccess(response: AuthResponse) {
        TODO("Not yet implemented")
    }

    override fun onRequestIsFailure(error: String) {
        TODO("Not yet implemented")
    }
}