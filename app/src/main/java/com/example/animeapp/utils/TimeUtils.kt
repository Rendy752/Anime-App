package com.example.animeapp.utils

import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object TimeUtils {
    fun formatDateToAgo(dateString: String): String {
        val prettyTime = PrettyTime(Locale.getDefault())
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val date: Date? = sdf.parse(dateString)
        return prettyTime.format(date)
    }

    fun isEpisodeAreUpToDate(
        broadcastTime: String?,
        broadcastTimezone: String?,
        broadcastDay: String?,
        lastEpisodeUpdatedAt: Long?
    ): Boolean {
        return false
    }

    fun formatTimestamp(timestamp: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timestamp)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timestamp) % TimeUnit.HOURS.toMinutes(1)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timestamp) % TimeUnit.MINUTES.toSeconds(1)

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}