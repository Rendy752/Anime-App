package com.example.animeappkotlin.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.animeappkotlin.models.AnimeRecommendation

@Dao
interface AnimeRecommendationsDao {
    @Query("SELECT * FROM anime_recommendations")
    fun getAllAnimeRecommendations(): LiveData<List<AnimeRecommendation>>

    @Delete
    suspend fun deleteAnimeRecommendation(animeRecommendation: AnimeRecommendation)
}