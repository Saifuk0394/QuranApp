package com.quran.romanurdu.data.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    // ── Ayahs ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC")
    fun getAyahsForSurah(surahNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT COUNT(*) FROM ayahs WHERE surahNumber = :surahNumber")
    suspend fun countAyahsForSurah(surahNumber: Int): Int

    @Query("SELECT * FROM ayahs WHERE arabic LIKE '%' || :query || '%' OR transliteration LIKE '%' || :query || '%' OR hindi LIKE '%' || :query || '%' OR urdu LIKE '%' || :query || '%' LIMIT 100")
    suspend fun searchAyahs(query: String): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE transliteration LIKE '%' || :query || '%' LIMIT 100")
    suspend fun searchByTransliteration(query: String): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE arabic LIKE '%' || :query || '%' LIMIT 100")
    suspend fun searchByArabic(query: String): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE hindi LIKE '%' || :query || '%' LIMIT 100")
    suspend fun searchByHindi(query: String): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE urdu LIKE '%' || :query || '%' LIMIT 100")
    suspend fun searchByUrdu(query: String): List<AyahEntity>

    // ── Surahs ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurah(surah: SurahEntity)

    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :number")
    suspend fun getSurah(number: Int): SurahEntity?
}
