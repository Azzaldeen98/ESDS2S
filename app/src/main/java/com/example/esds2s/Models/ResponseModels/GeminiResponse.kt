package com.example.esds2s.Models.ResponseModels

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponse(
    @SerializedName("description")
    @Expose
    val description:String
)
