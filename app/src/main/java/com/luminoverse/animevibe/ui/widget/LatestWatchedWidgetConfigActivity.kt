package com.luminoverse.animevibe.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.luminoverse.animevibe.utils.workers.WidgetUpdateWorker

class LatestWatchedWidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("WidgetConfigActivity", "onCreate called")

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e("WidgetConfigActivity", "Invalid appWidgetId")
            finish()
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setInputData(workDataOf(WidgetUpdateWorker.KEY_APP_WIDGET_IDS to intArrayOf(appWidgetId)))
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        Log.d("WidgetConfigActivity", "Widget configured, appWidgetId: $appWidgetId")
        finish()
    }
}