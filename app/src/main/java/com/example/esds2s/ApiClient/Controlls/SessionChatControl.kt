package com.example.esds2s.ApiClient.Controlls

import android.content.Context
import android.util.Log
import com.example.esds2s.ApiClient.Adapter.ApiClient
import com.example.esds2s.ApiClient.Adapter.RequestMethod
import com.example.esds2s.ApiClient.BuildConfig
import com.example.esds2s.ApiClient.Interface.IChatServices
import com.example.esds2s.ApiClient.Interface.ISessionChatServices
import com.example.esds2s.Interface.IBaseServiceEventListener
import com.example.esds2s.Models.RequestModels.CustomerChatRequest
import com.example.esds2s.Models.ResponseModels.BaseChatResponse
import com.example.esds2s.Models.ResponseModels.CustomerChatResponse
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.google.gson.Gson
import retrofit2.Call

class SessionChatControl(private val context: Context): BaseControl(context) {

    suspend fun  getAllChats(callBack: IBaseServiceEventListener<ArrayList<BaseChatResponse>>) {


        val client: IChatServices by lazy {
            ApiClient.getClient(context, RequestMethod.GET,
                BuildConfig.BASE_URL_TEXT_TO_GENERATOR)
                ?.create(IChatServices::class.java)!! }


        val call: Call<ArrayList<BaseChatResponse>?> by lazy { client?.getAllChats()!! }
        call?.enqueue(object : retrofit2.Callback<ArrayList<BaseChatResponse>?> {
            override fun onResponse(
                call: Call<ArrayList<BaseChatResponse>?>,
                response: retrofit2.Response<ArrayList<BaseChatResponse>?>
            ) {
                if (response!!.isSuccessful) {
                    val responseData: ArrayList<BaseChatResponse>? = response.body()
                    Log.d("response", Gson().toJson(responseData));
                    if (callBack != null) {
                        callBack.onRequestIsSuccess(responseData!!)
                    }
                } else
                    if (callBack != null)
                        callBack.onRequestIsFailure(response?.message()!!)
            }

            override fun onFailure(call: Call<ArrayList<BaseChatResponse>?>, t: Throwable) {
                // التعامل مع الأخطاء هنا
                if (callBack != null)
                    callBack?.onRequestIsFailure(t.message!!)

            }
        })
    }
    suspend fun  createSessionChat(body: CustomerChatRequest, callBack: IBaseServiceEventListener<CustomerChatResponse>) {

        val client: ISessionChatServices by lazy {
            ApiClient.getClient(context, RequestMethod.POST,
                BuildConfig.BASE_URL_TEXT_TO_GENERATOR)
                ?.create(ISessionChatServices::class.java)!! }

        val call: Call<CustomerChatResponse?> by lazy { client?.createSessionChat(body)!! }
        call?.enqueue(object : retrofit2.Callback<CustomerChatResponse?> {
            override fun onResponse(call: Call<CustomerChatResponse?>,
                response: retrofit2.Response<CustomerChatResponse?>
            ) {
                if (response!!.isSuccessful) {
                    val responseData: CustomerChatResponse? = response.body()
                    Log.d("response", Gson().toJson(responseData));
                    if (callBack != null) {
                        callBack.onRequestIsSuccess(responseData!!)
                    }
                } else
                    if (callBack != null)
                        callBack.onRequestIsFailure(response?.message()!!)
            }

            override fun onFailure(call: Call<CustomerChatResponse?>, t: Throwable) {
                if (callBack != null)
                    callBack?.onRequestIsFailure(t.message!!)
            }

        })
    }
}