package com.example.esds2s.ApiClient.Controlls

import android.content.Context
import android.util.Log
import com.example.esds2s.ApiClient.Adapter.ApiClient
import com.example.esds2s.ApiClient.Adapter.RequestMethod
import com.example.esds2s.ApiClient.BuildConfig
import com.example.esds2s.ApiClient.Interface.IChatServices
import com.example.esds2s.ApiClient.Interface.ISessionChatServices
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.Helper
import com.example.esds2s.Helpers.JsonStorageManager
import com.example.esds2s.Helpers.LanguageInfo
import com.example.esds2s.Interface.IBaseServiceEventListener
import com.example.esds2s.Models.RequestModels.CustomerChatRequest
import com.example.esds2s.Models.RequestModels.GeminiRequest
import com.example.esds2s.Models.RequestModels.GeminiRequestMessage
import com.example.esds2s.Models.ResponseModels.BaseChatResponse
import com.example.esds2s.Models.ResponseModels.CustomerChatResponse
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import com.example.esds2s.Services.SessionManagement
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Call

class SessionChatControl(private val context: Context): BaseControl(context) {


   suspend  fun  getAllChats(callBack: IBaseServiceEventListener<ArrayList<BaseChatResponse>>) {


        val client: IChatServices by lazy {
            ApiClient.getClient(context, RequestMethod.GET,
                BuildConfig.BASE_URL)
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

    suspend  fun  getTime() : GeminiResponse? = withContext(Dispatchers.IO){


        val client: IChatServices by lazy {
            ApiClient.getClient(context, RequestMethod.GET,
                BuildConfig.BASE_URL)
                ?.create(IChatServices::class.java)!! }


        val call: Call<GeminiResponse?> by lazy { client?.getTime()!! }
        val response = call.execute()
        if (response.isSuccessful) {
//            ExternalStorage.storage(context,ContentApp.CURRENT_SESSION_TOKEN,response?.body()?.token)
            if(response!=null ){
                val responseData: GeminiResponse? = response.body()
                if(responseData!=null){
                    ExternalStorage.storage(context,ContentApp.TIME,responseData.description)
                    Log.d("CustomerChatResponse",Gson().toJson(response.body()))
                }
            }

            return@withContext response.body()
        } else {
            Log.e("CustomerChatResponse",response.message())
            return@withContext null
        }
    }

    suspend fun  createSessionChat(body: CustomerChatRequest) : CustomerChatResponse? = withContext(Dispatchers.IO){

//        ChatAIProvider().load()
        val client: ISessionChatServices by lazy {
            ApiClient.getClient(context, RequestMethod.POST,
                BuildConfig.BASE_URL)
                ?.create(ISessionChatServices::class.java)!! }

        Log.d("createSessionChat", Gson().toJson(body))
        val call: Call<CustomerChatResponse?> by lazy { client?.createSessionChat(body)!! }
        val response = call.execute()
            if (response.isSuccessful) {
                ExternalStorage.storage(context,ContentApp.CURRENT_SESSION_TOKEN,response?.body()?.token)
                Log.d("CustomerChatResponse",Gson().toJson(response.body()))
                return@withContext response.body()
            } else {
                Log.e("CustomerChatResponse",response.message())
                return@withContext null
            }

//        call?.enqueue(object : retrofit2.Callback<CustomerChatResponse?> {
//            override fun onResponse(
//                call: Call<CustomerChatResponse?>,
//                response: retrofit2.Response<CustomerChatResponse?>
//            ) {
//                if (response!!.isSuccessful) {
//                    val responseData: CustomerChatResponse? = response.body()
//                    if(responseData!=null && responseData.token!=null)
//                         ExternalStorage.storage(context,ContentApp.CURRENT_CHAT_TOKEN,responseData.token)
//                       Log.d("response", Gson().toJson(responseData));
//
//                    }
//                }
//
//
//            override fun onFailure(call: Call<CustomerChatResponse?>, t: Throwable) {
//                // التعامل مع الأخطاء هنا
//
//
//            }
//        })
    }

    suspend fun  removeSession() : GeminiResponse? = withContext(Dispatchers.IO) {

        try {


            val client: ISessionChatServices by lazy {
                ApiClient.getClient(
                    context, RequestMethod.POST,
                    BuildConfig.BASE_URL
                )
                    ?.create(ISessionChatServices::class.java)!!
            }

            var customer_token =
                ExternalStorage.getValue(context, ContentApp.CURRENT_SESSION_TOKEN) as String?
            Log.d("customer_token", customer_token!!)
            if (customer_token == null) {
                Log.d("Error", "Token is null")
                return@withContext null
            }

            val body = GeminiRequest(_content = customer_token!!)
            val call: Call<GeminiResponse?> by lazy { client?.removeSession(body)!! }
            val response = call.execute()

            SessionManagement.OnLogOutFromSession(context)

            if (response.isSuccessful) {
                Log.d("responseEndSession", Gson().toJson(response.body()))
//            ExternalStorage.remove(context,ContentApp.CURRENT_SESSION_TOKEN)
                return@withContext response.body()
            } else {
                Log.e("removeSession", response.message())
                return@withContext throw java.lang.Exception(response.message())
            }

        }catch (e:Exception){
            return@withContext throw java.lang.Exception(e.message)
        }
    }
}


