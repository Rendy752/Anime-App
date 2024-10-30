package com.example.animeappkotlin.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.animeappkotlin.models.AnimeDetail

@Dao
interface AnimeDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeDetail(animeDetail: AnimeDetail)

    @Query("SELECT * FROM anime_detail WHERE mal_id = :id")
    fun getAnimeDetailById(id: Int): AnimeDetail?

    @Delete
    suspend fun deleteAnimeDetail(animeDetail: AnimeDetail)
}