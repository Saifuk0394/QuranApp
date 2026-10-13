package com.quran.romanurdu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quran.romanurdu.data.HADITH_COLLECTIONS
import com.quran.romanurdu.data.HadithCollection
import com.quran.romanurdu.data.HadithLanguage
import com.quran.romanurdu.data.HadithRepository
import com.quran.romanurdu.data.model.HadithEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Result type for unified search (used by SearchScreen v6). */
sealed class UnifiedSearchResult {
    data class AyahResult(
        val arabic: String,
        val transliteration: String,
        val hindi: String,
        val urdu: String,
        val surahNumber: Int,
        val ayahNumber: Int,
        val globalNumber: Int,
        val juz: Int
    ) : UnifiedSearchResult()

    data class HadithResult(
        val hadith: HadithEntity,
        val collectionName: String
    ) : UnifiedSearchResult()
}

sealed class HadithLoadState {
    data object Idle : HadithLoadState()
    data object Loading : HadithLoadState()
    data object Success : HadithLoadState()
    data class Error(val message: String) : HadithLoadState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class HadithViewModel(application: Application) : AndroidViewModel(application) {

    val repo = HadithRepository(application)

    // ── Selection state ───────────────────────────────────────────────────

    private val _selectedCollection = MutableStateFlow(HADITH_COLLECTIONS.first())
    val selectedCollection: StateFlow<HadithCollection> = _selectedCollection

    private val _selectedLanguage = MutableStateFlow(HadithLanguage.ARABIC)
    val selectedLanguage: StateFlow<HadithLanguage> = _selectedLanguage

    // ── Load state ────────────────────────────────────────────────────────

    private val _loadState = MutableStateFlow<HadithLoadState>(HadithLoadState.Idle)
    val loadState: StateFlow<HadithLoadState> = _loadState

    // ── Hadiths from Room (reactive) ──────────────────────────────────────

    val hadiths: StateFlow<List<HadithEntity>> = combine(
        _selectedCollection,
        _selectedLanguage
    ) { col, lang -> Pair(col.id, lang.code) }
        .flatMapLatest { (colId, langCode) ->
            repo.getHadiths(colId, langCode)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Search ────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<HadithEntity>>(emptyList())
    val searchResults: StateFlow<List<HadithEntity>> = _searchResults

    // ── Cache status (X / total collections) ─────────────────────────────

    private val _cachedCount = MutableStateFlow(0)
    val cachedCount: StateFlow<Int> = _cachedCount

    init {
        // Auto-load the default collection
        loadCurrentCollection()
    }

    // ── Public actions ────────────────────────────────────────────────────

    fun selectCollection(collection: HadithCollection) {
        _selectedCollection.value = collection
        loadCurrentCollection()
    }

    fun selectLanguage(language: HadithLanguage) {
        _selectedLanguage.value = language
        loadCurrentCollection()
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
        if (q.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = repo.searchHadiths(
                _selectedCollection.value.id,
                _selectedLanguage.value.code,
                q
            )
        }
    }

    // ── v6: Global cross-collection Hadith search ─────────────────────────

    private val _globalHadithResults = MutableStateFlow<List<HadithEntity>>(emptyList())
    val globalHadithResults: StateFlow<List<HadithEntity>> = _globalHadithResults

    private val _isGlobalSearching = MutableStateFlow(false)
    val isGlobalSearching: StateFlow<Boolean> = _isGlobalSearching

    fun searchAllHadiths(query: String, languageCode: String = "eng") {
        if (query.isBlank()) { _globalHadithResults.value = emptyList(); return }
        viewModelScope.launch {
            _isGlobalSearching.value = true
            try {
                // Search across all 9 collections in the given language
                val results = mutableListOf<HadithEntity>()
                for (col in HADITH_COLLECTIONS) {
                    val hits = repo.searchHadiths(col.id, languageCode, query)
                    results.addAll(hits.take(5)) // max 5 per collection to keep list manageable
                }
                _globalHadithResults.value = results.sortedBy { it.hadithNumber }.take(50)
            } finally {
                _isGlobalSearching.value = false
            }
        }
    }

    fun retry() = loadCurrentCollection()

    private fun loadCurrentCollection() {
        val collection = _selectedCollection.value.id
        val language = _selectedLanguage.value.code
        _loadState.value = HadithLoadState.Loading
        viewModelScope.launch {
            try {
                repo.ensureCached(collection, language)
                _loadState.value = HadithLoadState.Success
            } catch (e: Exception) {
                _loadState.value = if (repo.isCached(collection, language))
                    HadithLoadState.Success
                else
                    HadithLoadState.Error(
                        "Could not download ${_selectedCollection.value.nameEnglish}.\n${e.message ?: "Check internet connection."}"
                    )
            }
        }
    }
}
