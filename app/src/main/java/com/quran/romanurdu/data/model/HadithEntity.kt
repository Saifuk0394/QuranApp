package com.quran.romanurdu.data.model

import androidx.room.Entity

/**
 * Cached hadith row.
 * Primary key = (collection, language, hadithNumber)
 * e.g. ("bukhari", "ara", 1) and ("bukhari", "urd", 1) are separate rows.
 */
@Entity(
    tableName = "hadiths",
    primaryKeys = ["collection", "language", "hadithNumber"]
)
data class HadithEntity(
    val collection: String,     // e.g. "bukhari", "muslim"
    val language: String,       // "ara", "urd", "eng"
    val hadithNumber: Int,
    val arabicNumber: Int,
    val text: String,
    val bookNumber: Int,
    val bookHadithNumber: Int,
    val grade: String           // e.g. "Sahih", "" if none
)

@Entity(
    tableName = "hadith_collections",
    primaryKeys = ["collection", "language"]
)
data class HadithCollectionEntity(
    val collection: String,
    val language: String,
    val totalHadiths: Int,
    val cachedAt: Long = System.currentTimeMillis()
)
