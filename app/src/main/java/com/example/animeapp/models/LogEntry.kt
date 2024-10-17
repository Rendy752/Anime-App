package com.example.animeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LogEntry(
    val requestUrl: String,
    val responseCode: Int,
    val responseBody: String
) : Parcelable