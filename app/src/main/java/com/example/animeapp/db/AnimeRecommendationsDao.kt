package com.example.animeapp.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.animeapp.models.AnimeRecommendation

@Dao
interface AnimeRecommendationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeRecommendation(animeRecommendations: List<AnimeRecommendation>): List<Long>

    @Query("SELECT * FROM anime_recommendations")
    suspend fun getAllAnimeRecommendations(): LiveData<List<AnimeRecommendation>>

    @Delete
    suspend fun deleteAnimeRecommendation(animeRecommendation: AnimeRecommendation)
}