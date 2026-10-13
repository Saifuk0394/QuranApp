package com.quran.romanurdu.data.model

import androidx.room.*

/**
 * One "rich ayah" row = all 4 text layers for a single ayah.
 * Cached locally so the app works offline after first load.
 */
@Entity(tableName = "ayahs", primaryKeys = ["surahNumber", "ayahNumber"])
data class AyahEntity(
    val surahNumber: Int,
    val ayahNumber: Int,       // numberInSurah (1-based)
    val globalNumber: Int,
    val arabic: String,        // quran-uthmani
    val transliteration: String, // en.transliteration
    val hindi: String,         // hi.hindi
    val urdu: String,          // ur.junagarhi
    val juz: Int,
    val page: Int,
    val sajda: Boolean
)

/** Surah metadata cache */
@Entity(tableName = "surahs", primaryKeys = ["number"])
data class SurahEntity(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val englishMeaning: String,
    val revelationType: String,
    val totalAyahs: Int
)
