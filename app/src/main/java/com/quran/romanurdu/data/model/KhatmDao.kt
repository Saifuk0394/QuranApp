package com.quran.romanurdu.data.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KhatmDao {

    // ── Surah read log ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSurahRead(entry: SurahReadEntity)

    @Query("UPDATE surah_read_log SET lastReadAt = :timestamp WHERE surahNumber = :surahNumber")
    suspend fun updateLastRead(surahNumber: Int, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT surahNumber FROM surah_read_log")
    fun getReadSurahs(): Flow<List<Int>>

    @Query("SELECT COUNT(*) FROM surah_read_log")
    fun getReadCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM surah_read_log WHERE surahNumber = :surahNumber)")
    suspend fun isSurahRead(surahNumber: Int): Boolean

    // ── Khatm completions ──────────────────────────────────────────────────

    @Insert
    suspend fun recordKhatm(khatm: KhatmEntity)

    @Query("SELECT * FROM khatm_completions ORDER BY completedAt DESC")
    fun getKhatms(): Flow<List<KhatmEntity>>

    @Query("SELECT COUNT(*) FROM khatm_completions")
    fun getKhatmCount(): Flow<Int>

    // ── Global cross-table search (Quran + Hadith) ─────────────────────────

    @Query("""
        SELECT * FROM ayahs
        WHERE transliteration LIKE '%' || :query || '%'
           OR arabic LIKE '%' || :query || '%'
           OR hindi LIKE '%' || :query || '%'
           OR urdu LIKE '%' || :query || '%'
        LIMIT 50
    """)
    suspend fun searchAllAyahs(query: String): List<AyahEntity>
}
