package com.quran.romanurdu.data.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HadithDao {

    // ── Hadiths ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(hadiths: List<HadithEntity>)

    @Query("""
        SELECT * FROM hadiths
        WHERE collection = :collection AND language = :language
        ORDER BY hadithNumber ASC
    """)
    fun getHadiths(collection: String, language: String): Flow<List<HadithEntity>>

    @Query("""
        SELECT * FROM hadiths
        WHERE collection = :collection AND language = :language
        ORDER BY hadithNumber ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getHadithsPaged(
        collection: String,
        language: String,
        limit: Int,
        offset: Int
    ): List<HadithEntity>

    @Query("""
        SELECT COUNT(*) FROM hadiths
        WHERE collection = :collection AND language = :language
    """)
    suspend fun countHadiths(collection: String, language: String): Int

    @Query("""
        SELECT * FROM hadiths
        WHERE collection = :collection AND language = :language
        AND (text LIKE '%' || :query || '%')
        LIMIT 50
    """)
    suspend fun searchHadiths(
        collection: String,
        language: String,
        query: String
    ): List<HadithEntity>

    // ── Collection metadata ────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionMeta(meta: HadithCollectionEntity)

    @Query("SELECT * FROM hadith_collections WHERE collection = :collection AND language = :language")
    suspend fun getCollectionMeta(collection: String, language: String): HadithCollectionEntity?
}
