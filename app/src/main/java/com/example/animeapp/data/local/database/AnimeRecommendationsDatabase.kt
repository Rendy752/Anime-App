package com.example.animeapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.animeapp.data.local.dao.AnimeRecommendationsDao
import com.example.animeapp.data.local.entities.AnimeRecommendationsConverter
import com.example.animeapp.models.AnimeRecommendation

@Database(entities = [AnimeRecommendation::class], version = 3, exportSchema = false)
@TypeConverters(AnimeRecommendationsConverter::class)
abstract class AnimeRecommendationsDatabase : RoomDatabase() {
    abstract fun getAnimeRecommendationsDao(): AnimeRecommendationsDao
}