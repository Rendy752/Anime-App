package com.example.animeapp.utils

import android.content.res.Resources

object ViewUtils {
    fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
    fun Int.toDp(): Int = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
}