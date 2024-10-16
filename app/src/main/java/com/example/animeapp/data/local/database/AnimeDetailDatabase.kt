package com.example.animeapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.entities.Converters
import com.example.animeapp.models.AnimeDetail

@Database(entities = [AnimeDetail::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AnimeDetailDatabase : RoomDatabase() {

    abstract fun getAnimeDetailDao(): AnimeDetailDao

    companion object {
        @Volatile
        private var INSTANCE: AnimeDetailDatabase? = null

        fun getDatabase(context: Context): AnimeDetailDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnimeDetailDatabase::class.java,
                    "anime_detail_db.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}