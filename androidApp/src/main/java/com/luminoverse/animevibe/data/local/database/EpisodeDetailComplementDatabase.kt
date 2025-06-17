package com.luminoverse.animevibe.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.luminoverse.animevibe.data.local.dao.EpisodeDetailComplementDao
import com.luminoverse.animevibe.data.local.entities.EpisodeDetailComplementConverter
import com.luminoverse.animevibe.models.EpisodeDetailComplement

@Database(entities = [EpisodeDetailComplement::class], version = 9, exportSchema = false)
@TypeConverters(EpisodeDetailComplementConverter::class)
abstract class EpisodeDetailComplementDatabase : RoomDatabase() {

    abstract fun getEpisodeDetailComplementDao(): EpisodeDetailComplementDao

    companion object {
        @Volatile
        private var INSTANCE: EpisodeDetailComplementDatabase? = null

        fun getDatabase(context: Context): EpisodeDetailComplementDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EpisodeDetailComplementDatabase::class.java,
                    "episode_detail_complement.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}