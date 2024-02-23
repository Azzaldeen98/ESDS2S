package com.example.esds2s.ApiClient.Controlls

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.example.esds2s.ApiClient.Adapter.ApiClient
import com.example.esds2s.ApiClient.Adapter.RequestMethod
import com.example.esds2s.ApiClient.BuildConfig
import com.example.esds2s.ApiClient.Interface.IAuthServices
import com.example.esds2s.Interface.IAuthServiceEventListener
import com.example.esds2s.Models.ResponseModels.RegisterResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



class AuthControl(private val context: Context):BaseControl(context) {

       fun  register(callBack: IAuthServiceEventListener?) {

            val client: IAuthServices? = ApiClient.getClient(context, RequestMethod.POST,
                    BuildConfig.BASE_URL)?.create(IAuthServices::class.java)

                    try {
                        val call: Call<RegisterResponse?> by lazy { client?.RegisterUser()!! }
                        call?.enqueue(object : Callback<RegisterResponse?> {
                            override fun onResponse(
                                call: Call<RegisterResponse?>,
                                response: Response<RegisterResponse?>
                            ) {
                                if (response!!.isSuccessful) {
                                    val responseData: RegisterResponse? = response.body()
                                    Log.d("response", Gson().toJson(responseData));
                                    if (callBack != null) {
                                        callBack.onRequestIsSuccess(responseData!!)
                                    }
                                } else
                                    if (callBack != null)
                                        callBack.onRequestIsFailure(response?.message()!!)
                            }

                            override fun onFailure(call: Call<RegisterResponse?>, t: Throwable) {
                                // التعامل مع الأخطاء هنا
                                if (callBack != null)
                                    callBack?.onRequestIsFailure(t.message!!)
                            }
                        })
                    }catch (e:java.lang.Exception){
                        callBack?.onRequestIsFailure(e?.message?.toString()!!)
                    }
                }


    }




