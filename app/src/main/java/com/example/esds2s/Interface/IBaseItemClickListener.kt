package com.example.esds2s.Interface

interface IBaseItemClickListener<T> {

    fun onItemClick(item:T){ throw RuntimeException("Stub!") }
    fun onSelectedItem(item:T){ throw RuntimeException("Stub!") }
}