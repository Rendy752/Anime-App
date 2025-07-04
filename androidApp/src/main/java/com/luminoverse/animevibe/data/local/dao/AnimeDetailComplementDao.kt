package com.luminoverse.animevibe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.luminoverse.animevibe.models.AnimeDetailComplement

@Dao
interface AnimeDetailComplementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement)

    @Query("SELECT * FROM anime_detail_complement WHERE id = :id")
    suspend fun getAnimeDetailComplementById(id: String): AnimeDetailComplement?

    @Query("SELECT * FROM anime_detail_complement WHERE malId = :malId")
    suspend fun getAnimeDetailComplementByMalId(malId: Int): AnimeDetailComplement?

    @Delete
    suspend fun deleteAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement)

    @Update
    suspend fun updateAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement)

    @Query("SELECT * FROM anime_detail_complement WHERE isFavorite = 1")
    suspend fun getAllFavoriteAnimeComplements(): List<AnimeDetailComplement>

    @Query("SELECT * FROM anime_detail_complement WHERE lastEpisodeWatchedId IS NOT NULL")
    suspend fun getAllAnimeDetailsWithWatchedEpisodes(): List<AnimeDetailComplement>
}