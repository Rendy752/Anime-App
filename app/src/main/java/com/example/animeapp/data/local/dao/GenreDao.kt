package com.example.animeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.animeapp.models.Genre

@Dao
interface GenreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenre(genre: Genre)

    @Query("SELECT * FROM genre WHERE mal_id = :id")
    suspend fun getGenreById(id: Int): Genre?

    @Query("SELECT * FROM genre")
    suspend fun getGenres(): List<Genre>

    @Update
    suspend fun updateGenre(genre: Genre)

    @Delete
    suspend fun deleteGenre(genre: Genre)
}