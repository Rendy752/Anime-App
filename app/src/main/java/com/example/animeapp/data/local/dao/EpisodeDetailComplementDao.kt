package com.example.animeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.OnConflictStrategy
import com.example.animeapp.models.EpisodeDetailComplement

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

    @Update
    suspend fun updateEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement)

    @Query(
        """
            SELECT * FROM episode_detail_complement
            WHERE lastWatched IS NOT NULL AND lastTimestamp IS NOT NULL
            AND (:isFavorite IS NULL OR isFavorite = :isFavorite)
            AND (:searchQuery = '' OR animeTitle LIKE '%' || :searchQuery || '%' OR episodeTitle LIKE '%' || :searchQuery || '%')
            ORDER BY
                CASE
                    WHEN :isAscending THEN
                        CASE :sortBy
                            WHEN 'LastWatched' THEN lastWatched
                            WHEN 'AnimeTitle' THEN animeTitle
                            WHEN 'EpisodeTitle' THEN episodeTitle
                        END
                    ELSE
                        CASE :sortBy
                            WHEN 'LastWatched' THEN ''
                            WHEN 'AnimeTitle' THEN ''
                            WHEN 'EpisodeTitle' THEN ''
                        END
                END ASC,
                CASE
                    WHEN NOT :isAscending THEN
                        CASE :sortBy
                            WHEN 'LastWatched' THEN lastWatched
                            WHEN 'AnimeTitle' THEN animeTitle
                            WHEN 'EpisodeTitle' THEN episodeTitle
                        END
                    ELSE
                        CASE :sortBy
                            WHEN 'LastWatched' THEN ''
                            WHEN 'AnimeTitle' THEN ''
                            WHEN 'EpisodeTitle' THEN ''
                        END
                END DESC,
                episodeTitle ASC
            LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getPaginatedEpisodeHistory(
        searchQuery: String,
        isFavorite: Boolean?,
        sortBy: String,
        isAscending: Boolean,
        limit: Int,
        offset: Int
    ): List<EpisodeDetailComplement>

    @Query(
        """
            SELECT COUNT(*) FROM episode_detail_complement
            WHERE lastWatched IS NOT NULL AND lastTimestamp IS NOT NULL
            AND (:isFavorite IS NULL OR isFavorite = :isFavorite)
            AND (:searchQuery = '' OR animeTitle LIKE '%' || :searchQuery || '%' OR episodeTitle LIKE '%' || :searchQuery || '%')
        """
    )
    suspend fun getEpisodeHistoryCount(
        searchQuery: String,
        isFavorite: Boolean?
    ): Int
}