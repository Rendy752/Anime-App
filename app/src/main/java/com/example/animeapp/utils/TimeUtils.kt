package com.example.animeapp.utils

import com.example.animeapp.models.Broadcast
import org.ocpsoft.prettytime.PrettyTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun getCurrentDayOfWeek(): String {
        val currentDay = LocalDate.now().dayOfWeek
        return currentDay.name.lowercase(Locale.ENGLISH)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
    }

    fun getDayOfWeekList(): List<String> {
        val currentDay = LocalDate.now().dayOfWeek
        val daysOfWeek = DayOfWeek.entries

        val orderedList = mutableListOf<String>()

        orderedList.add(
            currentDay.getDisplayName(TextStyle.FULL_STANDALONE, Locale.ENGLISH).lowercase()
                .replaceFirstChar { it.titlecase(Locale.ENGLISH) })

        daysOfWeek.drop(currentDay.ordinal + 1).forEach {
            orderedList.add(
                it.getDisplayName(TextStyle.FULL_STANDALONE, Locale.ENGLISH).lowercase()
                    .replaceFirstChar { it.titlecase(Locale.ENGLISH) })
        }

        daysOfWeek.take(currentDay.ordinal).forEach {
            orderedList.add(
                it.getDisplayName(TextStyle.FULL_STANDALONE, Locale.ENGLISH).lowercase()
                    .replaceFirstChar { it.titlecase(Locale.ENGLISH) })
        }

        return orderedList
    }

    fun formatDateToAgo(dateString: String): String {
        return try {
            val prettyTime = PrettyTime(Locale.getDefault())
            val dateTimeFormatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val offsetDateTime = OffsetDateTime.parse(dateString, dateTimeFormatter)
            val date = Date.from(offsetDateTime.toInstant())
            prettyTime.format(date)
        } catch (e: Exception) {
            println("Error parsing date: ${e.message}")
            ""
        }
    }

    fun getBroadcastDateTimeThisWeek(
        broadcastTime: String,
        broadcastTimezone: String,
        broadcastDay: String
    ): ZonedDateTime {
        val broadcastLocalTime = LocalTime.parse(broadcastTime, timeFormatter)
        val broadcastZone = ZoneId.of(broadcastTimezone)
        val todayInBroadcastZone = LocalDate.now(broadcastZone)
        val firstDayOfWeek =
            todayInBroadcastZone.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dayOfWeek = DayOfWeek.valueOf(broadcastDay.removeSuffix("s").uppercase(Locale.ENGLISH))
        val broadcastDateThisWeek = firstDayOfWeek.with(TemporalAdjusters.nextOrSame(dayOfWeek))
        return ZonedDateTime.of(broadcastDateThisWeek, broadcastLocalTime, broadcastZone)
    }

    fun isNowWithinBroadcastWindow(
        broadcastTime: String?,
        broadcastTimezone: String?,
        broadcastDay: String?
    ): Boolean {
        if (broadcastTime == null || broadcastTimezone == null || broadcastDay == null) {
            return false
        }
        return try {
            val thisWeekBroadcastDateTimeInUserZone =
                getBroadcastDateTimeThisWeek(broadcastTime, broadcastTimezone, broadcastDay)
                    .withZoneSameInstant(ZoneId.systemDefault())

            val currentTimeInUserZone = ZonedDateTime.now(ZoneId.systemDefault())

            val lastDayOfWeekInBroadcastZone =
                LocalDate.now(ZoneId.of(broadcastTimezone))
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .atTime(LocalTime.MAX)
                    .atZone(ZoneId.of(broadcastTimezone))
            val lastDayOfWeekInUserZone =
                lastDayOfWeekInBroadcastZone.withZoneSameInstant(ZoneId.systemDefault())

            currentTimeInUserZone.isAfter(thisWeekBroadcastDateTimeInUserZone) && currentTimeInUserZone.isBefore(
                lastDayOfWeekInUserZone
            )
        } catch (e: Exception) {
            println("Error parsing broadcast time: ${e.message}")
            false
        }
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
        return try {
            val thisWeekBroadcastDateTimeInUserZone =
                getBroadcastDateTimeThisWeek(broadcastTime, broadcastTimezone, broadcastDay)
                    .withZoneSameInstant(ZoneId.systemDefault())
            val lastUpdateDateTime =
                Instant.ofEpochSecond(lastEpisodeUpdatedAt).atZone(ZoneId.systemDefault())
            val isUpdatedAfterBroadcast =
                lastUpdateDateTime.isAfter(thisWeekBroadcastDateTimeInUserZone)
            val isCurrentlyBroadcasting =
                isNowWithinBroadcastWindow(broadcastTime, broadcastTimezone, broadcastDay)
            println("thisWeekBroadcastDateTimeInUserZone: $thisWeekBroadcastDateTimeInUserZone")
            println("lastUpdateDateTime: $lastUpdateDateTime")
            println("isUpdatedAfterBroadcast: $isUpdatedAfterBroadcast")
            println("isCurrentlyBroadcasting: $isCurrentlyBroadcasting")
            isUpdatedAfterBroadcast || !isCurrentlyBroadcasting
        } catch (e: Exception) {
            println("Error parsing broadcast time: ${e.message}")
            false
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(timestamp)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun calculateRemainingTime(broadcast: Broadcast): String {
        return try {
            val broadcastZoneId = ZoneId.of(broadcast.timezone)
            val currentZoneId = ZoneId.systemDefault()
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val nowAligned = ZonedDateTime.now(currentZoneId)
            val nowBroadcast = nowAligned.withZoneSameInstant(broadcastZoneId)

            if (isNowWithinBroadcastWindow(broadcast.time, broadcast.timezone, broadcast.day)) {
                "On Air"
            } else {
                val broadcastLocalTime = LocalTime.parse(broadcast.time, timeFormatter)
                val broadcastDateTimeThisWeek = getBroadcastDateTimeThisWeek(
                    broadcast.time!!,
                    broadcast.timezone!!,
                    broadcast.day!!
                )
                var nextBroadcast = broadcastDateTimeThisWeek
                    .with(
                        TemporalAdjusters.nextOrSame(
                            DayOfWeek.valueOf(
                                broadcast.day.removeSuffix("s").uppercase(Locale.ENGLISH)
                            )
                        )
                    )
                    .withHour(broadcastLocalTime.hour)
                    .withMinute(broadcastLocalTime.minute)
                    .withSecond(0)
                    .withNano(0)

                if (!nowBroadcast.isBefore(nextBroadcast)) {
                    nextBroadcast = nextBroadcast.plusWeeks(1)
                }

                val timeDifference = ChronoUnit.MINUTES.between(nowBroadcast, nextBroadcast)
                val days = timeDifference / (24 * 60)
                val hours = (timeDifference % (24 * 60)) / 60
                val minutes = timeDifference % 60

                when {
                    days > 0 -> "${days}d ${hours}h ${minutes}m"
                    hours > 0 -> "${hours}h ${minutes}m"
                    minutes >= 0 -> "${minutes}m"
                    else -> "On Air"
                }
            }
        } catch (_: Exception) {
            ""
        }
    }
}