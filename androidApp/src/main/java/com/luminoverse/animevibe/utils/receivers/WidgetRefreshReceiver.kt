package com.luminoverse.animevibe.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.luminoverse.animevibe.utils.workers.WidgetUpdateWorker

class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == WidgetUpdateWorker.ACTION_REFRESH) {
            val appWidgetIds = intent.getIntArrayExtra(WidgetUpdateWorker.KEY_APP_WIDGET_IDS)
            if (appWidgetIds == null || appWidgetIds.isEmpty()) {
                Log.w("WidgetRefreshReceiver", "No appWidgetIds provided")
                return
            }
            Log.d("WidgetRefreshReceiver", "Received refresh request for widgets: ${appWidgetIds.joinToString()}")
            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInputData(workDataOf(WidgetUpdateWorker.KEY_APP_WIDGET_IDS to appWidgetIds))
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}