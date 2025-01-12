package com.example.animeappkotlin.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.animeappkotlin.data.local.dao.AnimeRecommendationsDao
import com.example.animeappkotlin.data.local.entities.AnimeRecommendationsConverter
import com.example.animeappkotlin.models.AnimeRecommendation

@Database(entities = [AnimeRecommendation::class], version = 3, exportSchema = false)
@TypeConverters(AnimeRecommendationsConverter::class)
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
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}