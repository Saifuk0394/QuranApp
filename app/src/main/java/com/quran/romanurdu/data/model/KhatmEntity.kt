package com.quran.romanurdu.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks which surahs the user has read (for Khatm progress).
 * One row per surah once the user marks it read.
 */
@Entity(tableName = "surah_read_log")
data class SurahReadEntity(
    @PrimaryKey val surahNumber: Int,
    val firstReadAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = System.currentTimeMillis()
)

/**
 * Records of completed Quran readings (Khatm completions).
 */
@Entity(tableName = "khatm_completions")
data class KhatmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val completedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)
