package com.example.esds2s.Interface

interface IBaseCallbackListener<T> {
    fun onCallBackExecuted(item:T?=null)
}