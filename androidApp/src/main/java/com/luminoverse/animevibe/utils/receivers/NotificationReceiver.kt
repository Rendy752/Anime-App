package com.luminoverse.animevibe.utils.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.ui.main.MainActivity
import com.luminoverse.animevibe.utils.ComplementUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AnimeEpisodeDetailRepository

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra("notification_id")?.toIntOrNull() ?: -1
        val accessId = intent.getStringExtra("access_id")
        val pendingResult = goAsync()

        Log.d("NotificationReceiver", "Received action: ${intent.action} for notificationId: $notificationId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    "ACTION_UNFAVORITE_ANIME" -> {
                        if (accessId != null) {
                            val malId = accessId.toIntOrNull()
                            if (malId != null) {
                                val complement = ComplementUtils.toggleAnimeFavorite(repository, null, malId, false)
                                if (complement != null) {
                                    repository.updateCachedAnimeDetailComplement(complement)
                                    Log.d("NotificationReceiver", "Unfavorited anime $malId")
                                }
                            }
                        }
                    }

                    "ACTION_OPEN_DETAIL" -> {
                        if (accessId != null) {
                            launchApp(context, "animevibe://anime/detail/$accessId")
                        }
                    }

                    "ACTION_OPEN_EPISODE" -> {
                        if (accessId != null) {
                            val parts = accessId.split("||")
                            if (parts.size == 2) {
                                val encodedMalId = URLEncoder.encode(parts[0], "UTF-8")
                                val encodedEpisodeId = URLEncoder.encode(parts[1], "UTF-8")
                                launchApp(context, "animevibe://anime/watch/$encodedMalId/$encodedEpisodeId")
                            }
                        }
                    }

                    "ACTION_CLOSE_NOTIFICATION" -> {
                        Log.d("NotificationReceiver", "Close action received for notification $notificationId")
                    }
                }
            } finally {
                if (notificationId != -1) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                    Log.d("NotificationReceiver", "Closed notification $notificationId due to action: ${intent.action}")
                }
                pendingResult.finish()
            }
        }
    }

    private fun launchApp(context: Context, deepLink: String) {
        val intent = Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply {
            setClass(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
        Log.d("NotificationReceiver", "Launched app with deep link: $deepLink")
    }
}