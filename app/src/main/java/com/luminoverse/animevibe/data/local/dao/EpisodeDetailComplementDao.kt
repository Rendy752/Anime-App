package com.luminoverse.animevibe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.OnConflictStrategy
import com.luminoverse.animevibe.models.EpisodeDetailComplement

@Dao
interface EpisodeDetailComplementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement)

    @Query("SELECT * FROM episode_detail_complement WHERE id = :id")
    suspend fun getEpisodeDetailComplementById(id: String): EpisodeDetailComplement?

    @Query(
        """
            SELECT * FROM episode_detail_complement
            WHERE malId = :malId
            ORDER BY
                CASE
                    WHEN lastWatched IS NOT NULL AND lastTimestamp IS NOT NULL THEN 0
                    ELSE 1
                END,
                lastWatched DESC
            LIMIT 1
        """
    )
    suspend fun getDefaultEpisodeDetailComplementByMalId(malId: Int): EpisodeDetailComplement

    @Query("SELECT * FROM episode_detail_complement WHERE lastWatched IS NOT NULL AND lastTimestamp IS NOT NULL ORDER BY lastWatched DESC LIMIT 1")
    suspend fun getLatestWatchedEpisodeDetailComplement(): EpisodeDetailComplement?

    @Delete
    suspend fun deleteEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement)

    @Query("DELETE FROM episode_detail_complement WHERE malId = :malId")
    suspend fun deleteEpisodeDetailComplementByMalId(malId: Int)

    @Update
    suspend fun updateEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement)

    @Query(
        """
            SELECT * FROM episode_detail_complement
            WHERE lastWatched IS NOT NULL AND lastTimestamp IS NOT NULL
            AND (:isFavorite IS NULL OR isFavorite = :isFavorite)
            ORDER BY
                CASE :sortBy
                    WHEN 'NewestFirst' THEN lastWatched
                    WHEN 'AnimeTitle' THEN animeTitle
                    WHEN 'EpisodeTitle' THEN episodeTitle
                    WHEN 'EpisodeNumber' THEN number
                END DESC,
                episodeTitle ASC
            LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getPaginatedEpisodeHistory(
        isFavorite: Boolean?,
        sortBy: String,
        limit: Int,
        offset: Int
    ): List<EpisodeDetailComplement>

    @Query(
        """
            SELECT * FROM episode_detail_complement
            WHERE lastWatched IS NOT NULL AND lastTimestamp IS NOT NULL
            AND (:isFavorite IS NULL OR isFavorite = :isFavorite)
            ORDER BY
                CASE :sortBy
                    WHEN 'NewestFirst' THEN lastWatched
                    WHEN 'AnimeTitle' THEN animeTitle
                    WHEN 'EpisodeTitle' THEN episodeTitle
                    WHEN 'EpisodeNumber' THEN number
                END DESC,
                episodeTitle ASC
        """
    )
    suspend fun getAllEpisodeHistory(
        isFavorite: Boolean?,
        sortBy: String
    ): List<EpisodeDetailComplement>

    @Query(
        """
            SELECT COUNT(*) FROM episode_detail_complement
            WHERE lastWatched IS NOT NULL AND lastTimestamp IS NOT NULL
            AND (:isFavorite IS NULL OR isFavorite = :isFavorite)
        """
    )
    suspend fun getEpisodeHistoryCount(
        isFavorite: Boolean?
    ): Int
}