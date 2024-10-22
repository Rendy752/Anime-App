package com.example.animeappkotlin.utils

import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatDateToAgo(dateString: String): String {
        val prettyTime = PrettyTime(Locale.getDefault())
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val date: Date = sdf.parse(dateString)
        return prettyTime.format(date)
    }
}