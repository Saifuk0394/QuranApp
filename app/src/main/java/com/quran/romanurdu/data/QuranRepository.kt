package com.quran.romanurdu.data

import android.content.Context
import com.quran.romanurdu.data.model.*
import com.quran.romanurdu.network.RetrofitClient
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth:
 *  1. Check Room cache
 *  2. If empty → fetch from API → store → return
 */
class QuranRepository(context: Context) {

    companion object {
        // The 4 editions we always fetch together
        const val EDITIONS = "quran-uthmani,en.transliteration,hi.hindi,ur.junagarhi"
    }

    private val dao = QuranDatabase.getInstance(context).quranDao()
    private val api = RetrofitClient.api

    /** Returns a Flow from Room; fetches from API if cache is empty. */
    fun getAyahsForSurah(surahNumber: Int): Flow<List<AyahEntity>> =
        dao.getAyahsForSurah(surahNumber)

    /** Call before observing — loads from network if not cached. */
    suspend fun ensureSurahCached(surahNumber: Int) {
        if (dao.countAyahsForSurah(surahNumber) > 0) return   // already cached

        val response = api.getSurahEditions(surahNumber, EDITIONS)
        val editions = response.data          // list of 4 SurahEditionWrapper objects

        // Map editions by identifier for easy lookup
        val byId = editions.associateBy { it.edition.identifier }
        val arabicEd   = byId["quran-uthmani"]         ?: return
        val transEd    = byId["en.transliteration"]    ?: return
        val hindiEd    = byId["hi.hindi"]              ?: return
        val urduEd     = byId["ur.junagarhi"]          ?: return

        // Build AyahEntity list zipping the 4 editions
        val ayahEntities = arabicEd.ayahs.mapIndexed { idx, arabicAyah ->
            AyahEntity(
                surahNumber       = surahNumber,
                ayahNumber        = arabicAyah.numberInSurah,
                globalNumber      = arabicAyah.number,
                arabic            = arabicAyah.text,
                transliteration   = transEd.ayahs.getOrNull(idx)?.text ?: "",
                hindi             = hindiEd.ayahs.getOrNull(idx)?.text ?: "",
                urdu              = urduEd.ayahs.getOrNull(idx)?.text ?: "",
                juz               = arabicAyah.juz,
                page              = arabicAyah.page,
                sajda             = arabicAyah.sajda
            )
        }

        dao.insertAyahs(ayahEntities)

        // Cache surah metadata too
        dao.insertSurah(
            SurahEntity(
                number           = arabicEd.number,
                arabicName       = arabicEd.name,
                englishName      = arabicEd.englishName,
                englishMeaning   = arabicEd.englishNameTranslation,
                revelationType   = arabicEd.revelationType,
                totalAyahs       = arabicEd.numberOfAyahs
            )
        )
    }

    suspend fun searchAyahs(query: String): List<AyahEntity> =
        dao.searchAyahs(query)

    suspend fun searchByTransliteration(query: String): List<AyahEntity> =
        dao.searchByTransliteration(query)

    suspend fun searchByArabic(query: String): List<AyahEntity> =
        dao.searchByArabic(query)

    suspend fun searchByHindi(query: String): List<AyahEntity> =
        dao.searchByHindi(query)

    suspend fun searchByUrdu(query: String): List<AyahEntity> =
        dao.searchByUrdu(query)
}
