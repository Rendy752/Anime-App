package com.example.animeapp.utils

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.animeapp.R
import com.example.animeapp.data.remote.api.RetrofitInstance

object LogUtils {
    private var lastPopupTime: Long = 0L

    fun showLogs(activity: FragmentActivity) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPopupTime > 5000) {
            val logs = RetrofitInstance.logCollectorInterceptor.getLogs()

            val navHostFragment = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment

            val bundle = Bundle().apply {
                putString("logs", logs)
            }
            val navOptions = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .build()

            if (navHostFragment != null) {
                navHostFragment.navController.navigate(R.id.action_global_loggingFragment, bundle, navOptions)
            } else {
                Log.e("LogUtils", "NavHostFragment is null")
            }

            RetrofitInstance.logCollectorInterceptor.clearLogs()
            lastPopupTime = currentTime
        }
    }
}