package com.quran.romanurdu.data.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AyahEntity::class,
        SurahEntity::class,
        HadithEntity::class,
        HadithCollectionEntity::class,
        SurahReadEntity::class,
        KhatmEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao
    abstract fun hadithDao(): HadithDao
    abstract fun khatmDao(): KhatmDao

    companion object {
        @Volatile private var INSTANCE: QuranDatabase? = null

        // Migration from v1 (Quran only) to v2 (+ Hadith tables)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS hadiths (
                        collection TEXT NOT NULL,
                        language TEXT NOT NULL,
                        hadithNumber INTEGER NOT NULL,
                        arabicNumber INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        bookNumber INTEGER NOT NULL,
                        bookHadithNumber INTEGER NOT NULL,
                        grade TEXT NOT NULL,
                        PRIMARY KEY (collection, language, hadithNumber)
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS hadith_collections (
                        collection TEXT NOT NULL,
                        language TEXT NOT NULL,
                        totalHadiths INTEGER NOT NULL,
                        cachedAt INTEGER NOT NULL,
                        PRIMARY KEY (collection, language)
                    )
                """)
            }
        }

        // Migration from v2 to v3 (+ Khatm tracker tables)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS surah_read_log (
                        surahNumber INTEGER NOT NULL PRIMARY KEY,
                        firstReadAt INTEGER NOT NULL DEFAULT 0,
                        lastReadAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS khatm_completions (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        completedAt INTEGER NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT ''
                    )
                """)
            }
        }

        fun getInstance(context: Context): QuranDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
            }
    }
}
