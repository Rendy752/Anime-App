package com.luminoverse.animevibe.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.luminoverse.animevibe.utils.workers.WidgetUpdateWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LatestWatchedWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(
            "LatestWatchedWidgetProvider",
            "onUpdate: Updating widgets ${appWidgetIds.joinToString()}"
        )
        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setInputData(workDataOf(WidgetUpdateWorker.KEY_APP_WIDGET_IDS to appWidgetIds))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    override fun onEnabled(context: Context?) {
        Log.d("LatestWatchedWidgetProvider", "onEnabled: First widget created")
    }

    override fun onDisabled(context: Context?) {
        Log.d("LatestWatchedWidgetProvider", "onDisabled: Last widget removed")
    }
}