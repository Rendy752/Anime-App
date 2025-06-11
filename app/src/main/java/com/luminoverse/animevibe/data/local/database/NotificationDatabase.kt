package com.luminoverse.animevibe.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.luminoverse.animevibe.data.local.dao.NotificationDao
import com.luminoverse.animevibe.models.Notification

@Database(entities = [Notification::class], version = 2, exportSchema = false)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun getNotificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: NotificationDatabase? = null

        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notification.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}