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
        if (broadcastTime == null || broadcastTimezone == null || broadcastDay == null || lastEpisodeUpdatedAt == null) {
            return false
        }

        try {
            val broadcastLocalTime = LocalTime.parse(broadcastTime)
            val broadcastZone = ZoneId.of(broadcastTimezone)
            val userZone = ZoneId.systemDefault()

            val singularDay = broadcastDay.removeSuffix("s").uppercase(Locale.ENGLISH)
            val dayOfWeek = DayOfWeek.valueOf(singularDay)

            val todayInBroadcastZone = LocalDate.now(broadcastZone)
            val firstDayOfWeek =
                todayInBroadcastZone.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val broadcastDateThisWeek = firstDayOfWeek.with(TemporalAdjusters.nextOrSame(dayOfWeek))

            val broadcastDateTime =
                ZonedDateTime.of(broadcastDateThisWeek, broadcastLocalTime, broadcastZone)
            val thisWeekBroadcastDateTime = broadcastDateTime.withZoneSameInstant(userZone)

            val lastUpdateDateTime = Instant.ofEpochSecond(lastEpisodeUpdatedAt).atZone(userZone)
            val currentTime = ZonedDateTime.now(userZone)

            println("lastUpdateDateTime: $lastUpdateDateTime")
            println("thisWeekBroadcastDateTime: $thisWeekBroadcastDateTime")
            println("currentTime: $currentTime")

            val lastDayOfWeek =
                firstDayOfWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .atTime(LocalTime.MAX).atZone(broadcastZone).withZoneSameInstant(userZone)
            println("lastDayOfWeek: $lastDayOfWeek")

            val isUpdatedAfterBroadcast = lastUpdateDateTime.isAfter(thisWeekBroadcastDateTime)
            val isCurrentTimeWithinBroadcastWindow =
                currentTime.isAfter(thisWeekBroadcastDateTime) && currentTime.isBefore(lastDayOfWeek)

            println("isUpdatedAfterBroadcast: $isUpdatedAfterBroadcast")
            println("isCurrentTimeWithinBroadcastWindow: $isCurrentTimeWithinBroadcastWindow")

            return if (isUpdatedAfterBroadcast) true
            else !isCurrentTimeWithinBroadcastWindow

        } catch (e: Exception) {
            println("Error parsing broadcast time: ${e.message}")
            return false
        }
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