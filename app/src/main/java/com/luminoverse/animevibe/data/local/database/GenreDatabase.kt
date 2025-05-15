package com.luminoverse.animevibe.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.luminoverse.animevibe.data.local.dao.GenreDao
import com.luminoverse.animevibe.models.Genre

@Database(entities = [Genre::class], version = 1, exportSchema = false)
abstract class GenreDatabase : RoomDatabase() {

    abstract fun getGenreDao(): GenreDao

    companion object {
        @Volatile
        private var INSTANCE: GenreDatabase? = null

        fun getDatabase(context: Context): GenreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GenreDatabase::class.java,
                    "genre.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}