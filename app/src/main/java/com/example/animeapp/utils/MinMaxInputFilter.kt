package com.example.animeapp.utils

import android.text.InputFilter
import android.text.Spanned

object MinMaxInputFilter {
    fun createInt(min: Int, max: Int): InputFilter {
        return object : InputFilter {
            override fun filter(
                source: CharSequence,
                start: Int,
                end: Int,
                dest: Spanned,
                dstart: Int,
                dend: Int,
            ): CharSequence? {
                val inputString = (dest.subSequence(0, dstart).toString() +
                        source.subSequence(start, end) +
                        dest.subSequence(dend, dest.length))

                if (inputString.isBlank()) {
                    return null
                }

                try {
                    val input = inputString.toInt()

                    return if (input in min..max) {
                        null
                    } else {
                        ""
                    }
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
                return ""
            }
        }
    }
}