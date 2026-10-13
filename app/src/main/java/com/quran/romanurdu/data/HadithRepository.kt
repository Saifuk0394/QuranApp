package com.quran.romanurdu.data

import android.content.Context
import com.quran.romanurdu.data.model.HadithCollectionEntity
import com.quran.romanurdu.data.model.HadithDao
import com.quran.romanurdu.data.model.HadithEntity
import com.quran.romanurdu.data.model.QuranDatabase
import com.quran.romanurdu.network.HadithRetrofitClient
import kotlinx.coroutines.flow.Flow

// ── Collection metadata ────────────────────────────────────────────────────

data class HadithCollection(
    val id: String,          // e.g. "bukhari"
    val nameEnglish: String,
    val nameArabic: String,
    val nameUrdu: String,
    val approxCount: Int     // approximate — actual count comes from API
)

/** Languages we show in the UI, with display labels */
enum class HadithLanguage(val code: String, val label: String, val isRtl: Boolean) {
    ARABIC("ara", "العربية", true),
    URDU("urd", "اردو", true),
    ENGLISH("eng", "English", false)
}

val HADITH_COLLECTIONS = listOf(
    HadithCollection("bukhari",  "Sahih al-Bukhari",  "صحيح البخاري",  "صحیح بخاری",  7563),
    HadithCollection("muslim",   "Sahih Muslim",      "صحيح مسلم",     "صحیح مسلم",   3033),
    HadithCollection("abudawud", "Sunan Abu Dawud",   "سنن أبي داود",   "سنن ابوداؤد",  5274),
    HadithCollection("tirmidhi", "Jami at-Tirmidhi",  "جامع الترمذي",   "جامع ترمذی",   3956),
    HadithCollection("nasai",    "Sunan an-Nasai",    "سنن النسائي",    "سنن نسائی",    5758),
    HadithCollection("ibnmajah", "Sunan Ibn Majah",   "سنن ابن ماجه",   "سنن ابن ماجہ", 4342),
    HadithCollection("malik",    "Muwatta Malik",     "موطأ مالك",      "موطا مالک",    1852),
    HadithCollection("nawawi",   "40 Hadith Nawawi",  "الأربعون النووية","چالیس حدیث",   42),
    HadithCollection("qudsi",    "40 Hadith Qudsi",   "الأربعون القدسية","قدسی حدیث",    40)
)

// ── Repository ─────────────────────────────────────────────────────────────

class HadithRepository(context: Context) {

    private val dao: HadithDao = QuranDatabase.getInstance(context).hadithDao()
    private val api = HadithRetrofitClient.api

    /** Returns all cached hadiths for a collection+language as a Flow. */
    fun getHadiths(collection: String, language: String): Flow<List<HadithEntity>> =
        dao.getHadiths(collection, language)

    /** True if this collection+language is already downloaded. */
    suspend fun isCached(collection: String, language: String): Boolean =
        dao.countHadiths(collection, language) > 0

    /**
     * Download and cache an entire collection in the given language.
     * Safe to call multiple times — skips if already cached.
     */
    suspend fun ensureCached(collection: String, language: String) {
        if (isCached(collection, language)) return

        val edition = "$language-$collection"
        val response = api.getCollection(edition)

        val entities = response.hadiths.map { h ->
            HadithEntity(
                collection       = collection,
                language         = language,
                hadithNumber     = h.hadithnumber,
                arabicNumber     = h.arabicnumber,
                text             = h.text,
                bookNumber       = h.reference?.book ?: 0,
                bookHadithNumber = h.reference?.hadith ?: 0,
                grade            = h.grades?.firstOrNull()?.grade ?: ""
            )
        }

        dao.insertHadiths(entities)
        dao.insertCollectionMeta(
            HadithCollectionEntity(
                collection    = collection,
                language      = language,
                totalHadiths  = entities.size
            )
        )
    }

    suspend fun countHadiths(collection: String, language: String): Int =
        dao.countHadiths(collection, language)

    suspend fun searchHadiths(
        collection: String,
        language: String,
        query: String
    ) = dao.searchHadiths(collection, language, query)
}
