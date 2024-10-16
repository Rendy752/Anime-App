package com.example.animeapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.animeapp.models.AnimeDetail

@Database(
    entities = [AnimeDetail::class],
    version = 3
)
@TypeConverters(Converters::class)
abstract class AnimeDetailDatabase : RoomDatabase() {
    abstract fun getAnimeDetailDao(): AnimeDetailDao

    companion object {
        @Volatile
        private var instance: AnimeDetailDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: createDatabase(context).also { instance = it }
        }

        private fun createDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AnimeDetailDatabase::class.java,
                "anime_detail_db.db"
            ).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL(
//                    """
//                CREATE TABLE anime_detail_new (
//                    mal_id INTEGER NOT NULL,
//                    title TEXT,
//                    url TEXT,
//                    images TEXT,
//                    trailer TEXT,
//                    approved INTEGER,
//                    titles TEXT,
//                    title_english TEXT,
//                    title_japanese TEXT,
//                    title_synonyms TEXT,
//                    type TEXT,
//                    source TEXT,
//                    episodes INTEGER,
//                    status TEXT,
//                    airing INTEGER,
//                    aired TEXT,
//                    duration TEXT,
//                    rating TEXT,
//                    score REAL,
//                    scored_by INTEGER,
//                    rank INTEGER,
//                    popularity INTEGER,
//                    members INTEGER,
//                    favorites INTEGER,
//                    synopsis TEXT,
//                    background TEXT,
//                    season TEXT,
//                    year INTEGER,
//                    broadcast TEXT
//                    producers TEXT,
//                    licensors TEXT,
//                    studios TEXT,
//                    genres TEXT,
//                    explicit_genres TEXT,
//                    themes TEXT,
//                    demographics TEXT,
//                    relations TEXT,
//                    theme TEXT,
//                    external TEXT,
//                    streaming TEXT,
//                    PRIMARY KEY(mal_id)
//                )
//            """
//                )
            }
        }
    }
}