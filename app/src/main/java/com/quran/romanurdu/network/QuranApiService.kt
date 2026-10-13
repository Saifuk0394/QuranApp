package com.quran.romanurdu.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

// ---------- API response models ----------

data class ApiResponse<T>(
    val code: Int,
    val status: String,
    val data: T
)

data class SurahEditionWrapper(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val numberOfAyahs: Int,
    val ayahs: List<ApiAyah>,
    val edition: ApiEdition
)

data class ApiAyah(
    val number: Int,           // global ayah number
    val text: String,
    val numberInSurah: Int,
    val juz: Int,
    val page: Int,
    val sajda: Boolean = false,
    val audio: String? = null  // MP3 URL for audio editions
)

data class ApiEdition(
    val identifier: String,
    val language: String,
    val name: String,
    val englishName: String,
    val format: String,
    val type: String,
    val direction: String?
)

// ---------- Retrofit service ----------

interface QuranApiService {

    /**
     * Fetch one surah in multiple editions simultaneously.
     * editions = comma-separated identifiers, e.g.
     *   "quran-uthmani,en.transliteration,hi.hindi,ur.junagarhi"
     */
    @GET("surah/{number}/editions/{editions}")
    suspend fun getSurahEditions(
        @Path("number") surahNumber: Int,
        @Path("editions") editions: String
    ): ApiResponse<List<SurahEditionWrapper>>

    /** Full Quran in multiple editions (heavy — use only for full offline download) */
    @GET("quran/editions/{editions}")
    suspend fun getFullQuranEditions(
        @Path("editions") editions: String
    ): ApiResponse<List<FullQuranEdition>>

    /**
     * Get audio URLs for a surah from a specific reciter.
     * Returns a list of ayahs each with an `audio` field (MP3 URL).
     * Edition example: "ar.alafasy"
     */
    @GET("surah/{number}/editions/{edition}")
    suspend fun getSurahAudio(
        @Path("number") surahNumber: Int,
        @Path("edition") edition: String
    ): ApiResponse<List<SurahEditionWrapper>>
}

data class FullQuranEdition(
    val identifier: String,
    val language: String,
    val name: String,
    val englishName: String,
    val format: String,
    val type: String,
    val direction: String?,
    val ayahs: List<ApiAyah>
)
