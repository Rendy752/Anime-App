package com.luminoverse.animevibe.utils.workers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import com.luminoverse.animevibe.android.R
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.repository.AnimeEpisodeDetailRepository
import com.luminoverse.animevibe.utils.TimeUtils
import com.luminoverse.animevibe.utils.factories.ChildWorkerFactory
import com.luminoverse.animevibe.utils.receivers.WidgetRefreshReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.math.roundToInt

class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: AnimeEpisodeDetailRepository
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory : ChildWorkerFactory {
        override fun create(appContext: Context, params: WorkerParameters): WidgetUpdateWorker
    }

    companion object {
        const val KEY_APP_WIDGET_IDS = "app_widget_ids"
        const val ACTION_REFRESH = "com.luminoverse.animevibe.REFRESH_WIDGET"

        private fun log(message: String) {
            Log.d("WidgetUpdateWorker", message)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val appWidgetIds = inputData.getIntArray(KEY_APP_WIDGET_IDS)
            if (appWidgetIds == null || appWidgetIds.isEmpty()) {
                log("No appWidgetIds provided")
                return@withContext Result.failure()
            }

            log("Updating widgets: ${appWidgetIds.joinToString()}")
            updateWidgets(appWidgetIds)
            Result.success()
        } catch (e: Exception) {
            log("Error updating widgets: ${e.message}")
            Result.failure()
        }
    }

    private suspend fun updateWidgets(appWidgetIds: IntArray) {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val views = RemoteViews(applicationContext.packageName, R.layout.widget_latest_watched)

        val refreshIntent = Intent(applicationContext, WidgetRefreshReceiver::class.java).apply {
            action = ACTION_REFRESH
            putExtra(KEY_APP_WIDGET_IDS, appWidgetIds)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            1,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val episode = repository.getCachedLatestWatchedEpisodeDetailComplement()
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
                    if (timestamp < duration) (timestamp.toFloat() / duration).coerceIn(0f, 1f) else 1f
                } ?: 0f
                val percentage = (progress * 100).roundToInt()
                views.setProgressBar(R.id.widget_progress_bar, 100, percentage, false)
                views.setTextViewText(R.id.widget_progress_text, "$percentage%")

                val bitmap = loadEpisodeImage(episode)
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widget_episode_image, bitmap)
                } else {
                    views.setImageViewResource(R.id.widget_episode_image, R.drawable.ic_video_black_24dp)
                }

                val backgroundRes = if (episode.isFiller) {
                    log("Using filler gradient background")
                    R.drawable.widget_background_filler
                } else {
                    log("Using default gradient background")
                    R.drawable.widget_background_default
                }
                views.setInt(R.id.widget_root_layout, "setBackgroundResource", backgroundRes)

                val clickIntent = Intent(Intent.ACTION_VIEW).apply {
                    val encodedMalId = URLEncoder.encode(episode.malId.toString(), "UTF-8")
                    val encodedEpisodeId = URLEncoder.encode(episode.id, "UTF-8")
                    data = "animevibe://anime/watch/${encodedMalId}/${encodedEpisodeId}".toUri()
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
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
            log("Error updating widget: ${e.message}")
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
                log("Failed to decode Base64 screenshot: ${e.message}")
            }
        }

        episode.imageUrl?.let { url ->
            try {
                val imageLoader = ImageLoader.Builder(applicationContext).build()
                val request = ImageRequest.Builder(applicationContext)
                    .data(url)
                    .size(80, 45)
                    .build()
                val result = imageLoader.execute(request)
                return result.drawable?.toBitmap()
            } catch (e: Exception) {
                log("Failed to load image with Coil: ${e.message}")
            }
        }

        return null
    }

    private fun setErrorWidgetLayout(
        views: RemoteViews,
        episodeTitle: String,
        refreshPendingIntent: PendingIntent
    ) {
        views.setTextViewText(R.id.widget_episode_title, episodeTitle)
        views.setViewVisibility(R.id.widget_timestamp_layout, View.GONE)
        views.setTextViewText(R.id.widget_timestamp, "")
        views.setTextViewText(R.id.widget_last_watched, "")
        views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
        views.setTextViewText(R.id.widget_progress_text, "0%")
        views.setImageViewResource(R.id.widget_episode_image, R.drawable.ic_video_black_24dp)
        views.setInt(R.id.widget_root_layout, "setBackgroundResource", R.drawable.widget_background_default)
        views.setOnClickPendingIntent(R.id.widget_root_layout, refreshPendingIntent)
    }
}