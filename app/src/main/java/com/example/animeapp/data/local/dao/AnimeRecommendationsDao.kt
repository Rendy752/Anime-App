package com.example.animeapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.animeapp.models.AnimeRecommendation

@Dao
interface AnimeRecommendationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeRecommendation(animeRecommendations: List<AnimeRecommendation>): List<Long>

    @Query("SELECT * FROM anime_recommendations")
    fun getAllAnimeRecommendations(): LiveData<List<AnimeRecommendation>>

    @Delete
    suspend fun deleteAnimeRecommendation(animeRecommendation: AnimeRecommendation)
}