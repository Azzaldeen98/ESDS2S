package com.example.esds2s.Services

import com.example.esds2s.Interface.IExecuteRequestAsync
import com.example.esds2s.Models.ResponseModels.GeminiResponse
import kotlinx.coroutines.*

class SendRequestAsync<T>(private var callBack: IExecuteRequestAsync) {

        // Declare a property using lazy and Deferred
        private val someDeferredProperty: Deferred<T> by lazy {
            // Use Dispatchers.IO or any appropriate coroutine context
            GlobalScope.async(Dispatchers.IO) {
                callBack?.onExecuteRequestAsync()
                // Perform some asynchronous operation
                "Deferred result" as T
            }
        }

        // Example function to retrieve the result
      suspend  fun getResult(): T = runBlocking {
            someDeferredProperty.await()
        }


//    val response=SendRequestAsync<GeminiResponse>(object : IExecuteRequestAsync{
//        override fun onExecuteRequestAsync(){
//            GlobalScope.async {
//            }
//        }
//    } }).getResult()}

//    fun createRequest(callBack:IExecuteRequestAsync){
//        this.callBack=callBack
//        someDeferredProperty.get
//    }

//    var str=object : IExecuteRequestAsync{
//                    override fun
//                }
//                SendRequestAsync<GeminiResponse>
    }


