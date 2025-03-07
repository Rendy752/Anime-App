package com.example.animeapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.entities.AnimeDetailComplementConverter
import com.example.animeapp.models.AnimeDetailComplement

@Database(entities = [AnimeDetailComplement::class], version = 1, exportSchema = false)
@TypeConverters(AnimeDetailComplementConverter::class)
abstract class AnimeDetailComplementDatabase : RoomDatabase() {

    abstract fun getAnimeDetailComplementDao(): AnimeDetailComplementDao

    companion object {
        @Volatile
        private var INSTANCE: AnimeDetailComplementDatabase? = null

        fun getDatabase(context: Context): AnimeDetailComplementDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnimeDetailComplementDatabase::class.java,
                    "anime_detail_complement.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}