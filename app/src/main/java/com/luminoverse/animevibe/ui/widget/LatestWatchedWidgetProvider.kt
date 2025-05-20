package com.luminoverse.animevibe.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LatestWatchedWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("LatestWatchedWidgetProvider", "onUpdate: Updating widgets ${appWidgetIds.joinToString()}")
        val intent = Intent(context, WidgetUpdateService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        context.startService(intent)
    }

    override fun onEnabled(context: Context?) {
        Log.d("LatestWatchedWidgetProvider", "onEnabled: First widget created")
    }

    override fun onDisabled(context: Context?) {
        Log.d("LatestWatchedWidgetProvider", "onDisabled: Last widget removed")
        val intent = Intent(context, WidgetUpdateService::class.java)
        context?.stopService(intent)
    }
}