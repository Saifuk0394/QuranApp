package com.quran.romanurdu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quran.romanurdu.data.PreferencesRepository
import com.quran.romanurdu.data.QuranRepository
import com.quran.romanurdu.data.Surah
import com.quran.romanurdu.data.SurahData
import com.quran.romanurdu.data.model.AyahEntity
import com.quran.romanurdu.data.model.KhatmEntity
import com.quran.romanurdu.data.model.KhatmDao
import com.quran.romanurdu.data.model.QuranDatabase
import com.quran.romanurdu.data.model.SurahReadEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.quran.romanurdu.ui.screens.SearchLanguage

enum class DisplayLanguage(val label: String) {
    ALL("All"),
    ARABIC_ONLY("Arabic"),
    HINDI("Hindi"),
    URDU("Urdu"),
    TRANSLITERATION("Roman")
}

@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    val repo = QuranRepository(application)
    private val khatmDao: KhatmDao = QuranDatabase.getInstance(application).khatmDao()

    // ── Navigation state ──────────────────────────────────────────────────

    private val _currentSurahNumber = MutableStateFlow(1)
    val currentSurahNumber: StateFlow<Int> = _currentSurahNumber

    private val _currentAyahIndex = MutableStateFlow(0)  // scroll target

    // ── UI preferences ────────────────────────────────────────────────────

    val darkMode: StateFlow<Boolean> = prefs.darkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val bookmarks: StateFlow<Set<Int>> = prefs.bookmarks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _fontSize = MutableStateFlow(20f)
    val fontSize: StateFlow<Float> = _fontSize

    private val _displayLanguage = MutableStateFlow(DisplayLanguage.ALL)
    val displayLanguage: StateFlow<DisplayLanguage> = _displayLanguage

    // ── Ayah loading ──────────────────────────────────────────────────────

    private val _loadingState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadingState: StateFlow<LoadState> = _loadingState

    val ayahs: StateFlow<List<AyahEntity>> = _currentSurahNumber
        .flatMapLatest { surahNum -> repo.getAyahsForSurah(surahNum) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Surah list search ─────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredSurahs: StateFlow<List<Surah>> = _searchQuery
        .map { q ->
            if (q.isBlank()) SurahData.surahs
            else SurahData.surahs.filter {
                it.nameRoman.contains(q, ignoreCase = true) ||
                it.nameArabic.contains(q) ||
                it.number.toString() == q.trim()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SurahData.surahs)

    // ── Ayah-text search results ──────────────────────────────────────────

    private val _ayahSearchResults = MutableStateFlow<List<AyahEntity>>(emptyList())
    val ayahSearchResults: StateFlow<List<AyahEntity>> = _ayahSearchResults
    val searchResults: StateFlow<List<AyahEntity>> = _ayahSearchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // ── First-launch bulk download ─────────────────────────────────────────

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError

    val isSetupComplete: StateFlow<Boolean> = prefs.isSetupComplete
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── v6: Last read ─────────────────────────────────────────────────────

    val lastRead: StateFlow<PreferencesRepository.LastRead?> = prefs.lastRead
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── v6: Khatm / reading progress ─────────────────────────────────────

    val readSurahs: StateFlow<Set<Int>> = khatmDao.getReadSurahs()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val readSurahCount: StateFlow<Int> = khatmDao.getReadCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val khatmCompletions: StateFlow<List<KhatmEntity>> = khatmDao.getKhatms()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val khatmCount: StateFlow<Int> = khatmDao.getKhatmCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Init ──────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            prefs.currentPage.collect { savedSurahNum ->
                if (savedSurahNum in 1..114) {
                    _currentSurahNumber.value = savedSurahNum
                    loadSurah(savedSurahNum)
                    return@collect
                }
            }
        }
    }

    // ── Public actions ────────────────────────────────────────────────────

    fun navigateToSurah(surahNumber: Int) {
        viewModelScope.launch {
            _currentSurahNumber.value = surahNumber
            prefs.saveCurrentPage(surahNumber)
            loadSurah(surahNumber)
        }
    }

    private suspend fun loadSurah(surahNumber: Int) {
        _loadingState.value = LoadState.Loading
        try {
            repo.ensureSurahCached(surahNumber)
            _loadingState.value = LoadState.Success
        } catch (e: Exception) {
            _loadingState.value = if (ayahs.value.isEmpty())
                LoadState.Error("Could not load Surah $surahNumber.\nCheck your internet connection.")
            else
                LoadState.Success
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch { prefs.setDarkMode(!darkMode.value) }
    }

    fun toggleBookmark(globalAyahNumber: Int) {
        viewModelScope.launch { prefs.toggleBookmark(globalAyahNumber) }
    }

    fun isBookmarked(globalAyahNumber: Int) = bookmarks.value.contains(globalAyahNumber)

    fun increaseFontSize() { _fontSize.value = (_fontSize.value + 2f).coerceAtMost(36f) }
    fun decreaseFontSize() { _fontSize.value = (_fontSize.value - 2f).coerceAtLeast(14f) }

    fun setDisplayLanguage(lang: DisplayLanguage) { _displayLanguage.value = lang }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun searchAyahText(query: String) {
        viewModelScope.launch {
            _ayahSearchResults.value = if (query.isBlank()) emptyList()
                                       else repo.searchAyahs(query)
        }
    }

    fun searchAyahs(query: String, lang: SearchLanguage) {
        if (query.isBlank()) { _ayahSearchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                _ayahSearchResults.value = when (lang) {
                    SearchLanguage.ROMAN_ENGLISH -> repo.searchByTransliteration(query)
                    SearchLanguage.ARABIC        -> repo.searchByArabic(query)
                    SearchLanguage.HINDI         -> repo.searchByHindi(query)
                    SearchLanguage.URDU          -> repo.searchByUrdu(query)
                }
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun selectSurah(surahNumber: Int) = navigateToSurah(surahNumber)

    fun getSurahMeta(number: Int): Surah? = SurahData.surahs.getOrNull(number - 1)

    // ── v6: Save last read position ───────────────────────────────────────

    fun saveLastRead(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            val surahName = SurahData.surahs.getOrNull(surahNumber - 1)?.nameRoman ?: ""
            prefs.saveLastRead(surahNumber, ayahNumber, surahName)
        }
    }

    // ── v6: Khatm tracking ────────────────────────────────────────────────

    /** Call when user opens a surah (marks it as read). */
    fun markSurahRead(surahNumber: Int) {
        viewModelScope.launch {
            val exists = khatmDao.isSurahRead(surahNumber)
            if (exists) {
                khatmDao.updateLastRead(surahNumber, System.currentTimeMillis())
            } else {
                khatmDao.markSurahRead(SurahReadEntity(surahNumber))
            }
            // If all 114 surahs read, record a Khatm automatically
            val count = readSurahCount.value
            if (count == 114) {
                recordKhatm()
            }
        }
    }

    fun recordKhatm(notes: String = "") {
        viewModelScope.launch {
            khatmDao.recordKhatm(KhatmEntity(notes = notes))
            // Reset read log so user can start a new cycle
            // (we keep completions record; just don't reset log here — user can track multiple cycles)
        }
    }

    // ── Bulk download ──────────────────────────────────────────────────────

    fun downloadAllSurahs() {
        _downloadProgress.value = 0
        _downloadError.value = null
        viewModelScope.launch {
            for (surahNum in 1..114) {
                try {
                    repo.ensureSurahCached(surahNum)
                    _downloadProgress.value = surahNum
                } catch (e: Exception) {
                    _downloadError.value = "Failed at Surah $surahNum: ${e.message ?: "Unknown error"}"
                    return@launch
                }
            }
            prefs.markSetupComplete()
        }
    }

    fun retryDownload() { _downloadError.value = null; downloadAllSurahs() }

    fun preloadNextSurah() {
        val next = _currentSurahNumber.value + 1
        if (next <= 114) viewModelScope.launch {
            try { repo.ensureSurahCached(next) } catch (_: Exception) {}
        }
    }

    fun goToSurah(surah: Surah) = navigateToSurah(surah.number)
}

sealed class LoadState {
    data object Idle : LoadState()
    data object Loading : LoadState()
    data object Success : LoadState()
    data class Error(val message: String) : LoadState()
}
