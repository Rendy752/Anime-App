package com.example.animeapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.animeapp.models.AnimeDetail

@Dao
interface AnimeDetailDao {
    @Query("SELECT * FROM anime_detail")
    fun getAllAnimeDetail(): LiveData<List<AnimeDetail>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeDetail(animeDetail: AnimeDetail): Long

    @Delete
    suspend fun deleteAnimeDetail(animeDetail: AnimeDetail)

    @Query("SELECT * FROM anime_detail WHERE mal_id = :id")
    fun getAnimeDetailById(id: Int): LiveData<AnimeDetail>
}