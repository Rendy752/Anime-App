package com.luminoverse.animevibe.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Handles the logic when the notification toggle is switched on.
 *
 * It checks for the notification permission on Android 13 (API 33) and newer. If the permission
 * is already granted, it executes the [onPermissionGranted] callback. If not, it determines
 * whether to show a rationale before requesting the permission or to direct the user to the
 * app's settings screen. For Android versions older than 13, it assumes permissions are
 * granted and executes [onPermissionGranted].
 *
 * @param context The current Android context.
 * @param onPermissionGranted A lambda to execute when notification permissions are considered granted.
 * @param permissionLauncher An [ActivityResultLauncher] for requesting the `POST_NOTIFICATIONS` permission.
 * @param settingsLauncher An [ActivityResultLauncher] for opening the app's notification settings screen.
 */
fun onEnableNotifications(
    context: Context,
    onPermissionGranted: () -> Unit,
    permissionLauncher: ActivityResultLauncher<String>,
    settingsLauncher: ActivityResultLauncher<Intent>
) {
    if (Build.VERSION.SDK_INT >= 33) {
        val permission = "android.permission.POST_NOTIFICATIONS"
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            if ((context as? ComponentActivity)?.shouldShowRequestPermissionRationale(permission) == true) {
                permissionLauncher.launch(permission)
            } else {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                settingsLauncher.launch(intent)
            }
        }
    } else {
        onPermissionGranted()
    }
}

/**
 * Handles the logic when the notification toggle is switched off.
 *
 * This function opens the app's notification settings screen, allowing the user to manually
 * disable the notifications for the app from the system settings.
 *
 * @param context The current Android context.
 * @param settingsLauncher An [ActivityResultLauncher] for opening the app's notification settings screen.
 */
fun onDisableNotifications(
    context: Context,
    settingsLauncher: ActivityResultLauncher<Intent>
) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    settingsLauncher.launch(intent)
}