package com.luminoverse.animevibe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.luminoverse.animevibe.models.AnimeDetail

@Dao
interface AnimeDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeDetail(animeDetail: AnimeDetail)

    @Query("SELECT * FROM anime_detail WHERE mal_id = :id")
    suspend fun getAnimeDetailById(id: Int): AnimeDetail?

    @Update
    suspend fun updateAnimeDetail(animeDetail: AnimeDetail)

    @Query("DELETE FROM anime_detail WHERE mal_id = :id")
    suspend fun deleteAnimeDetailById(id: Int)
}