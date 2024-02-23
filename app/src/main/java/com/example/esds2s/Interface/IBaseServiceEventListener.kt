package com.example.esds2s.Interface

interface IBaseServiceEventListener<T> {
    fun onRequestIsSuccess(response: T)
    fun onRequestIsFailure(error: String)
}