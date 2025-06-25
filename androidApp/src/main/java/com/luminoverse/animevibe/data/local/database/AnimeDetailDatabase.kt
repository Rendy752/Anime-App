package com.luminoverse.animevibe.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.luminoverse.animevibe.data.local.dao.AnimeDetailDao
import com.luminoverse.animevibe.data.local.entities.AnimeDetailConverter
import com.luminoverse.animevibe.models.AnimeDetail

@Database(entities = [AnimeDetail::class], version = 16, exportSchema = false)
@TypeConverters(AnimeDetailConverter::class)
abstract class AnimeDetailDatabase : RoomDatabase() {

    abstract fun getAnimeDetailDao(): AnimeDetailDao

    companion object {
        @Volatile
        private var INSTANCE: AnimeDetailDatabase? = null

        fun getDatabase(context: Context): AnimeDetailDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnimeDetailDatabase::class.java,
                    "anime_detail.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}