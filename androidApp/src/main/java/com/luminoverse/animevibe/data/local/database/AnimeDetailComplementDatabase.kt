package com.luminoverse.animevibe.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.luminoverse.animevibe.data.local.dao.AnimeDetailComplementDao
import com.luminoverse.animevibe.data.local.entities.AnimeDetailComplementConverter
import com.luminoverse.animevibe.models.AnimeDetailComplement

@Database(entities = [AnimeDetailComplement::class], version = 5, exportSchema = false)
@TypeConverters(AnimeDetailComplementConverter::class)
abstract class AnimeDetailComplementDatabase : RoomDatabase() {

    abstract fun getAnimeDetailComplementDao(): AnimeDetailComplementDao

    companion object {
        @Volatile
        private var INSTANCE: AnimeDetailComplementDatabase? = null

        fun getDatabase(context: Context): AnimeDetailComplementDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnimeDetailComplementDatabase::class.java,
                    "anime_detail_complement.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}