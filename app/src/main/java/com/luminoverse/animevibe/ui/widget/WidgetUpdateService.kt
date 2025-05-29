package com.luminoverse.animevibe.ui.widget

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil.ImageLoader
import coil.request.ImageRequest
import com.luminoverse.animevibe.R
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.utils.TimeUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class WidgetUpdateService : Service() {

    @Inject
    lateinit var repository: AnimeEpisodeDetailRepository
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_REFRESH = "com.luminoverse.animevibe.REFRESH_WIDGET"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!::repository.isInitialized) {
            Log.e("WidgetUpdateService", "Repository not injected")
            stopSelf()
            return START_NOT_STICKY
        }

        val appWidgetIds = intent?.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
        if (appWidgetIds != null) {
            coroutineScope.launch {
                Log.d("WidgetUpdateService", "Updating widgets: ${appWidgetIds.joinToString()}")
                updateWidgets(appWidgetIds)
            }
        } else if (intent?.action == ACTION_REFRESH) {
            val widgetIds = AppWidgetManager.getInstance(this).getAppWidgetIds(
                android.content.ComponentName(this, LatestWatchedWidgetProvider::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                coroutineScope.launch {
                    Log.d(
                        "WidgetUpdateService",
                        "Refresh action triggered for widgets: ${widgetIds.joinToString()}"
                    )
                    updateWidgets(widgetIds)
                }
            } else {
                Log.w("WidgetUpdateService", "No widget IDs found for refresh")
                stopSelf()
            }
        } else {
            Log.w("WidgetUpdateService", "No appWidgetIds or valid action provided")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private suspend fun updateWidgets(appWidgetIds: IntArray) {
        Log.d("WidgetUpdateService", "Fetching latest episode")
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val views = RemoteViews(packageName, R.layout.widget_latest_watched)

        val refreshIntent = Intent(this, WidgetUpdateService::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        val refreshPendingIntent = PendingIntent.getService(
            this,
            1,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            val episode = repository.getCachedLatestWatchedEpisodeDetailComplement()
            Log.d("WidgetUpdateService", "Episode fetched: $episode")
            if (episode != null) {
                views.setTextViewText(
                    R.id.widget_episode_title,
                    "Ep ${episode.number}: ${episode.episodeTitle}"
                )

                val timestampText = episode.lastTimestamp?.let { timestamp ->
                    val durationText =
                        episode.duration?.let { " / ${TimeUtils.formatTimestamp(it)}" } ?: ""
                    "${TimeUtils.formatTimestamp(timestamp)}$durationText"
                } ?: ""
                views.setTextViewText(R.id.widget_timestamp, timestampText)
                views.setViewVisibility(R.id.widget_timestamp_layout, View.VISIBLE)

                val lastWatchedText =
                    episode.lastWatched?.let { "~ ${TimeUtils.formatDateToAgo(it)}" } ?: ""
                views.setTextViewText(R.id.widget_last_watched, lastWatchedText)

                val duration = episode.duration?.toFloat() ?: (24 * 60f)
                val progress = episode.lastTimestamp?.let { timestamp ->
                    if (timestamp < duration) (timestamp.toFloat() / duration).coerceIn(
                        0f,
                        1f
                    ) else 1f
                } ?: 0f
                val percentage = (progress * 100).roundToInt()
                views.setProgressBar(R.id.widget_progress_bar, 100, percentage, false)
                views.setTextViewText(R.id.widget_progress_text, "$percentage%")

                val bitmap = loadEpisodeImage(episode)
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widget_episode_image, bitmap)
                } else {
                    views.setImageViewResource(
                        R.id.widget_episode_image,
                        R.drawable.ic_video_black_24dp
                    )
                }

                val backgroundRes = if (episode.isFiller) {
                    Log.d("WidgetUpdateService", "Using filler gradient background")
                    R.drawable.widget_background_filler
                } else {
                    Log.d("WidgetUpdateService", "Using default gradient background")
                    R.drawable.widget_background_default
                }
                views.setInt(R.id.widget_root_layout, "setBackgroundResource", backgroundRes)

                val clickIntent = Intent(Intent.ACTION_VIEW).apply {
                    val encodedMalId = URLEncoder.encode(episode.malId.toString(), "UTF-8")
                    val encodedEpisodeId = URLEncoder.encode(episode.id, "UTF-8")
                    data = "animevibe://anime/watch/${encodedMalId}/${encodedEpisodeId}".toUri()
                }
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root_layout, pendingIntent)
                views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
            } else {
                setErrorWidgetLayout(views, "No recently watched episodes", refreshPendingIntent)
            }
        } catch (e: Exception) {
            Log.e("WidgetUpdateService", "Error updating widget", e)
            setErrorWidgetLayout(views, "Error updating widget", refreshPendingIntent)
        }

        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private suspend fun loadEpisodeImage(episode: EpisodeDetailComplement): Bitmap? {
        episode.screenshot?.let { base64String ->
            try {
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                Log.e("WidgetUpdateService", "Failed to decode Base64 screenshot", e)
            }
        }

        episode.imageUrl?.let { url ->
            try {
                val imageLoader = ImageLoader.Builder(this)
                    .build()
                val request = ImageRequest.Builder(this)
                    .data(url)
                    .size(80, 45)
                    .build()
                val result = imageLoader.execute(request)
                return result.drawable?.toBitmap()
            } catch (e: Exception) {
                Log.e("WidgetUpdateService", "Failed to load image with Coil", e)
            }
        }

        return null
    }

    private fun setErrorWidgetLayout(views: RemoteViews, episodeTitle: String, refreshPendingIntent: PendingIntent) {
        views.setTextViewText(R.id.widget_episode_title, episodeTitle)
        views.setViewVisibility(R.id.widget_timestamp_layout, View.GONE)
        views.setTextViewText(R.id.widget_timestamp, "")
        views.setTextViewText(R.id.widget_last_watched, "")
        views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
        views.setTextViewText(R.id.widget_progress_text, "0%")
        views.setImageViewResource(
            R.id.widget_episode_image,
            R.drawable.ic_video_black_24dp
        )
        views.setInt(
            R.id.widget_root_layout,
            "setBackgroundResource",
            R.drawable.widget_background_default
        )

        views.setOnClickPendingIntent(R.id.widget_root_layout, refreshPendingIntent)
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }
}