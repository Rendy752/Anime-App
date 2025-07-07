package com.luminoverse.animevibe.utils.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.utils.ComplementUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AnimeEpisodeDetailRepository

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra("notification_id")?.toIntOrNull()
            ?: intent.getIntExtra("notification_id", -1)

        val accessId = intent.getStringExtra("access_id")
        val pendingResult = goAsync()

        Log.d(
            "NotificationReceiver",
            "Received background action: ${intent.action} for notificationId: $notificationId"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    "ACTION_UNFAVORITE_ANIME" -> {
                        if (accessId != null) {
                            val malId = accessId.toIntOrNull()
                            if (malId != null) {
                                val complement = ComplementUtils.toggleAnimeFavorite(
                                    repository,
                                    null,
                                    malId,
                                    false
                                )
                                if (complement != null) {
                                    repository.updateCachedAnimeDetailComplement(complement)
                                    Log.d("NotificationReceiver", "Unfavorited anime $malId")
                                }
                            }
                        }
                    }

                    "ACTION_CLOSE_NOTIFICATION" -> {
                        Log.d(
                            "NotificationReceiver",
                            "Close action received for notification $notificationId"
                        )
                    }
                }
            } finally {
                if (notificationId != -1) {
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                    Log.d(
                        "NotificationReceiver",
                        "Closed notification $notificationId due to background action: ${intent.action}"
                    )
                }
                pendingResult.finish()
            }
        }
    }
}