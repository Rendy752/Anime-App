package com.luminoverse.animevibe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.luminoverse.animevibe.models.Notification

@Dao
interface NotificationDao {
    @Insert
    suspend fun insertNotification(notification: Notification): Long

    @Query("SELECT * FROM notifications WHERE accessId = :accessId AND type = :type")
    suspend fun getNotificationsByAccessIdAndType(accessId: String, type: String): List<Notification>

    @Query("UPDATE notifications SET sentAt = :sentAt WHERE id = :id")
    suspend fun updateNotificationSentTime(id: Long, sentAt: Long)

    @Query("DELETE FROM notifications WHERE createdAt < :cutoff")
    suspend fun deleteOldNotifications(cutoff: Long): Int

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}