package com.luminoverse.animevibe.repository

import com.luminoverse.animevibe.data.local.dao.NotificationDao
import com.luminoverse.animevibe.models.Notification
import java.time.Instant
import javax.inject.Inject
import kotlin.math.abs

class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {
    companion object {
        private const val NOTIFICATION_DUPLICATE_WINDOW_SECONDS = 1 * 60 * 60
        private const val NOTIFICATION_CLEANUP_DAYS = 7L
    }

    suspend fun saveNotification(notification: Notification): Long {
        try {
            val id = notificationDao.insertNotification(notification)
            log("Inserted notification: id=$id, accessId=${notification.accessId}, type=${notification.type}")
            return id
        } catch (e: Exception) {
            log("Failed to insert notification: accessId=${notification.accessId}, type=${notification.type}, error=${e.message}")
            throw e
        }
    }

    suspend fun checkDuplicateNotification(accessId: String, type: String): Boolean {
        val notifications = notificationDao.getNotificationsByAccessIdAndType(accessId, type)
        log("Checking duplicates for accessId=$accessId, type=$type, found=${notifications.size}")
        notifications.forEach { log("Notification: id=${it.id}, createdAt=${it.createdAt}, sentAt=${it.sentAt}") }

        val now = Instant.now().epochSecond
        val hasRecent = notifications.any {
            abs(now - it.createdAt) <= NOTIFICATION_DUPLICATE_WINDOW_SECONDS
        }

        if (hasRecent) {
            log("Found recent notification: accessId=$accessId, type=$type")
            return true
        }

        log("No recent notifications for accessId=$accessId, type=$type, allowing new notification")
        return false
    }

    suspend fun markNotificationAsSent(id: Long) {
        try {
            notificationDao.updateNotificationSentTime(id, Instant.now().epochSecond)
            log("Marked notification as sent: id=$id")
        } catch (e: Exception) {
            log("Failed to mark notification as sent: id=$id, error=${e.message}")
        }
    }

    suspend fun cleanOldNotifications() {
        val cutoff = Instant.now().minusSeconds(NOTIFICATION_CLEANUP_DAYS * 24 * 60 * 60).epochSecond
        val deleted = notificationDao.deleteOldNotifications(cutoff)
        log("Deleted $deleted notifications older than $NOTIFICATION_CLEANUP_DAYS days")
    }

    private fun log(message: String) {
        println("NotificationRepository: $message")
    }
}