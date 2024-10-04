package com.example.animeapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.animeapp.models.AnimeRecommendation

@Database(
    entities = [AnimeRecommendation::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AnimeRecommendationsDatabase : RoomDatabase() {
    abstract fun getAnimeRecommendationsDao(): AnimeRecommendationsDao

    companion object {
        @Volatile
        private var instance: AnimeRecommendationsDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: createDatabase(context).also { instance = it }
        }

        private fun createDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AnimeRecommendationsDatabase::class.java,
                "anime_recommendations_db.db"
            ).build()
    }
}