package com.quran.romanurdu.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// ---------- Response models ----------

data class HadithApiResponse(
    val metadata: HadithMetadata,
    val hadiths: List<HadithApiItem>
)

data class HadithMetadata(
    val name: String,
    val section: Map<String, String>?,
    @SerializedName("section_detail")
    val sectionDetail: Map<String, SectionDetail>?
)

data class SectionDetail(
    @SerializedName("hadithnumber_first") val hadithNumberFirst: Int,
    @SerializedName("hadithnumber_last")  val hadithNumberLast: Int
)

data class HadithApiItem(
    val hadithnumber: Int,
    val arabicnumber: Int,
    val text: String,
    val grades: List<HadithGrade>?,
    val reference: HadithReference?
)

data class HadithGrade(
    val name: String,
    val grade: String
)

data class HadithReference(
    val book: Int?,
    val hadith: Int?
)

// ---------- Retrofit interface ----------

interface HadithApiService {
    /**
     * Returns the full collection for a given edition.
     * Edition format: "{lang}-{book}"  e.g. "ara-bukhari", "urd-muslim", "eng-abudawud"
     */
    @GET("editions/{edition}.min.json")
    suspend fun getCollection(
        @Path("edition") edition: String
    ): HadithApiResponse
}

// ---------- Retrofit singleton ----------

object HadithRetrofitClient {
    // jsDelivr CDN — serves static JSON files from the GitHub repo
    private const val BASE_URL =
        "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)   // collections can be large
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val api: HadithApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(HadithApiService::class.java)
}
