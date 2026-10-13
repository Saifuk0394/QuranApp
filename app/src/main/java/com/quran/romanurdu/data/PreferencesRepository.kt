package com.quran.romanurdu.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quran_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        val CURRENT_PAGE        = intPreferencesKey("current_page")
        val DARK_MODE           = booleanPreferencesKey("dark_mode")
        val BOOKMARKS           = stringPreferencesKey("bookmarks") // comma-separated ayah global numbers
        val FONT_SCALE          = floatPreferencesKey("font_scale")
        val SETUP_COMPLETE      = booleanPreferencesKey("setup_complete")
        // v6: last read tracking
        val LAST_READ_SURAH     = intPreferencesKey("last_read_surah")
        val LAST_READ_AYAH      = intPreferencesKey("last_read_ayah")
        val LAST_READ_SURAH_NAME = stringPreferencesKey("last_read_surah_name")
    }

    val currentPage: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[CURRENT_PAGE] ?: 1
    }

    val isSetupComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SETUP_COMPLETE] ?: false
    }

    suspend fun markSetupComplete() {
        context.dataStore.edit { prefs -> prefs[SETUP_COMPLETE] = true }
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE] ?: false
    }

    val bookmarks: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[BOOKMARKS] ?: ""
        if (raw.isBlank()) emptySet()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    suspend fun saveCurrentPage(page: Int) {
        context.dataStore.edit { prefs -> prefs[CURRENT_PAGE] = page }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE] = enabled }
    }

    suspend fun toggleBookmark(page: Int) {
        context.dataStore.edit { prefs ->
            val raw = prefs[BOOKMARKS] ?: ""
            val current = if (raw.isBlank()) mutableSetOf()
                          else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
            if (current.contains(page)) current.remove(page) else current.add(page)
            prefs[BOOKMARKS] = current.joinToString(",")
        }
    }

    // ── v6: Last read position ────────────────────────────────────────────────

    data class LastRead(val surahNumber: Int, val ayahNumber: Int, val surahName: String)

    val lastRead: Flow<LastRead?> = context.dataStore.data.map { prefs ->
        val surah = prefs[LAST_READ_SURAH] ?: return@map null
        val ayah  = prefs[LAST_READ_AYAH]  ?: 1
        val name  = prefs[LAST_READ_SURAH_NAME] ?: ""
        LastRead(surah, ayah, name)
    }

    suspend fun saveLastRead(surahNumber: Int, ayahNumber: Int, surahName: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_READ_SURAH]      = surahNumber
            prefs[LAST_READ_AYAH]       = ayahNumber
            prefs[LAST_READ_SURAH_NAME] = surahName
        }
    }
}
