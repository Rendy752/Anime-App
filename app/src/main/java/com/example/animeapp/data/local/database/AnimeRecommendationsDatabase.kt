package com.example.animeapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.animeapp.data.local.dao.AnimeRecommendationsDao
import com.example.animeapp.data.local.entities.Converters
import com.example.animeapp.models.AnimeRecommendation

@Database(entities = [AnimeRecommendation::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AnimeRecommendationsDatabase : RoomDatabase() {

    abstract fun getAnimeRecommendationsDao(): AnimeRecommendationsDao

    companion object {
        @Volatile
        private var INSTANCE: AnimeRecommendationsDatabase? = null

        fun getDatabase(context: Context): AnimeRecommendationsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnimeRecommendationsDatabase::class.java,
                    "anime_recommendations_db.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}