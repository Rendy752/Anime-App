package com.example.animeapp.utils

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.animeapp.R
import com.example.animeapp.data.remote.api.RetrofitInstance
import com.example.animeapp.models.LogEntry

object LogUtils {
    private const val LOG_DISPLAY_DELAY = 5000L
    private var lastPopupTime = 0L
    private var isToastShowing = false

    fun showLogs(activity: FragmentActivity) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPopupTime > LOG_DISPLAY_DELAY && !isToastShowing) {
            val logsEntries = RetrofitInstance.logCollectorInterceptor.getLogs()

            if (logsEntries.isNotEmpty()) {
                navigateToLoggingFragment(activity, logsEntries)
                RetrofitInstance.logCollectorInterceptor.clearLogs()
            } else {
                showNoLogsToast(activity)
            }

            lastPopupTime = currentTime
        }
    }

    private fun navigateToLoggingFragment(activity: FragmentActivity, logEntries: List<LogEntry>) {
        val navHostFragment =
            activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment

        if (navHostFragment != null) {
            val bundle = Bundle().apply {
                putParcelableArrayList("logEntries", ArrayList(logEntries))
            }
            val navOptions = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()
            navHostFragment.navController.navigate(
                R.id.action_global_loggingFragment,
                bundle,
                navOptions
            )
        } else {
            Log.e("LogUtils", "NavHostFragment is null")
        }
    }

    private fun showNoLogsToast(activity: FragmentActivity) {
        isToastShowing = true
        Toast.makeText(activity, "No logs to display", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ isToastShowing = false }, LOG_DISPLAY_DELAY)
    }
}