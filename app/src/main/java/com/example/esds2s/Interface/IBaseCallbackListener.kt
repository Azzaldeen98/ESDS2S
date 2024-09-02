package com.example.esds2s.Interface

interface IBaseCallbackListener<T> {
    fun onCallBackExecuted(item:T?=null)

}
interface IListenerStream<T> {
    fun onStreamReader(response:T?=null,lastFlow:Boolean=false){}
    fun onStreamReader(response:T?=null,flowIndex:Int=0,lastFlowIndex:Int=0){}
    fun onStreamComplete(lastFlowIndex:Int=0)
    fun onStreamError(message:Throwable)
}
interface IAcceptOrCancelListener<T> {
    fun onAccept(item:T?=null)
    fun onCancel(item:T?=null)

}