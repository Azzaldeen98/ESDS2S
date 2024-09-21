package com.example.esds2s.Interface

import com.example.esds2s.Models.ResponseModels.GeminiResponse

interface IGeminiServiceEventListener:IBaseServiceEventListener<GeminiResponse> {
}

interface IWasmServiceEventListener:IBaseServiceEventListener<String> {
    fun onRequestIsSuccess2(callBack:IBaseCallbackListener<Any?>?){}

}