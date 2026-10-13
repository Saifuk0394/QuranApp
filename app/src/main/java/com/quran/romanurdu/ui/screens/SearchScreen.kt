package com.quran.romanurdu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quran.romanurdu.data.HADITH_COLLECTIONS
import com.quran.romanurdu.data.model.AyahEntity
import com.quran.romanurdu.data.model.HadithEntity
import com.quran.romanurdu.viewmodel.HadithViewModel
import com.quran.romanurdu.viewmodel.QuranViewModel
import com.quran.romanurdu.ui.theme.QuranGold
import com.quran.romanurdu.ui.theme.QuranGreen

enum class SearchLanguage(val label: String, val isRtl: Boolean) {
    ROMAN_ENGLISH("English (Roman)", false),
    ARABIC("Arabic", true),
    HINDI("Hindi", false),
    URDU("Urdu", true)
}

/** Which type of content to search */
enum class SearchScope(val label: String) {
    QURAN("Quran"),
    HADITH("Hadith"),
    BOTH("Both")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: QuranViewModel,
    hadithViewModel: HadithViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf(SearchLanguage.ROMAN_ENGLISH) }
    var selectedScope by remember { mutableStateOf(SearchScope.BOTH) }

    val ayahResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val hadithResults by hadithViewModel.globalHadithResults.collectAsState()
    val isHadithSearching by hadithViewModel.isGlobalSearching.collectAsState()

    val isBusy = isSearching || isHadithSearching
    val totalResults = ayahResults.size + hadithResults.size

    fun doSearch(q: String) {
        if (selectedScope == SearchScope.QURAN || selectedScope == SearchScope.BOTH) {
            viewModel.searchAyahs(q, selectedLang)
        }
        if (selectedScope == SearchScope.HADITH || selectedScope == SearchScope.BOTH) {
            val langCode = when (selectedLang) {
                SearchLanguage.ARABIC -> "ara"
                SearchLanguage.URDU   -> "urd"
                else                  -> "eng"
            }
            hadithViewModel.searchAllHadiths(q, langCode)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = QuranGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = { q ->
                    query = q
                    doSearch(q)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search Quran & Hadith…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = QuranGreen) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QuranGreen,
                    cursorColor = QuranGreen
                )
            )

            // Scope tabs: Quran | Hadith | Both
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchScope.entries.forEach { scope ->
                    FilterChip(
                        selected = selectedScope == scope,
                        onClick = {
                            selectedScope = scope
                            if (query.isNotBlank()) doSearch(query)
                        },
                        label = { Text(scope.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QuranGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Language filter chips (only relevant when Quran scope active)
            if (selectedScope != SearchScope.HADITH) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = {
                                selectedLang = lang
                                if (query.isNotBlank()) doSearch(query)
                            },
                            label = { Text(lang.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = QuranGold,
                                selectedLabelColor = Color(0xFF1A1A1A)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            when {
                isBusy -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = QuranGreen)
                    }
                }
                query.isBlank() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search, null,
                                modifier = Modifier.size(64.dp),
                                tint = QuranGreen.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Search Quran ayahs and Hadith",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "in Arabic, English, Hindi, or Urdu",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                totalResults == 0 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No results for \"$query\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    Text(
                        "$totalResults result${if (totalResults != 1) "s" else ""} for \"$query\"",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ── Quran results ──────────────────────────────────
                        if (ayahResults.isNotEmpty() &&
                            selectedScope != SearchScope.HADITH) {
                            item {
                                SectionHeader("📖 Quran", ayahResults.size)
                            }
                            items(ayahResults, key = { "ayah_${it.globalNumber}" }) { ayah ->
                                SearchResultCard(
                                    ayah = ayah,
                                    query = query,
                                    searchLang = selectedLang,
                                    onClick = {
                                        viewModel.selectSurah(ayah.surahNumber)
                                        navController.navigate("reader") {
                                            popUpTo("reader") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }

                        // ── Hadith results ─────────────────────────────────
                        if (hadithResults.isNotEmpty() &&
                            selectedScope != SearchScope.QURAN) {
                            item {
                                SectionHeader("📜 Hadith", hadithResults.size)
                            }
                            items(hadithResults, key = { "hadith_${it.collection}_${it.hadithNumber}" }) { hadith ->
                                HadithSearchResultCard(
                                    hadith = hadith,
                                    query = query,
                                    onClick = {
                                        navController.navigate("hadith") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

// ── Section Header ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = QuranGreen.copy(alpha = 0.15f)
        ) {
            Text(
                count.toString(),
                fontSize = 11.sp,
                color = QuranGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// ── Quran Ayah Result Card ──────────────────────────────────────────────────

@Composable
private fun SearchResultCard(
    ayah: AyahEntity,
    query: String,
    searchLang: SearchLanguage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Surah ${ayah.surahNumber}:${ayah.ayahNumber}",
                    fontSize = 11.sp, color = QuranGreen, fontWeight = FontWeight.SemiBold
                )
                Text("Juz ${ayah.juz}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = ayah.arabic,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            val searchText = when (searchLang) {
                SearchLanguage.ROMAN_ENGLISH -> ayah.transliteration
                SearchLanguage.ARABIC        -> ayah.arabic
                SearchLanguage.HINDI         -> ayah.hindi
                SearchLanguage.URDU          -> ayah.urdu
            }
            if (searchLang != SearchLanguage.ARABIC && searchText.isNotBlank()) {
                HighlightedText(text = searchText, highlight = query, isRtl = searchLang.isRtl)
            }
        }
    }
}

// ── Hadith Result Card ──────────────────────────────────────────────────────

@Composable
private fun HadithSearchResultCard(
    hadith: HadithEntity,
    query: String,
    onClick: () -> Unit
) {
    val collectionName = HADITH_COLLECTIONS.find { it.id == hadith.collection }?.nameEnglish
        ?: hadith.collection
    val isRtl = hadith.language == "ara" || hadith.language == "urd"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = QuranGold.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    collectionName.replace("Sunan ", "").replace("Sahih ", ""),
                    fontSize = 11.sp, color = QuranGold, fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Hadith #${hadith.hadithNumber}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            HighlightedText(
                text = hadith.text,
                highlight = query,
                isRtl = isRtl
            )
        }
    }
}

// ── Highlighted Text ────────────────────────────────────────────────────────

@Composable
fun HighlightedText(text: String, highlight: String, isRtl: Boolean) {
    val annotated = buildAnnotatedString {
        if (highlight.isBlank()) { append(text); return@buildAnnotatedString }
        var start = 0
        val lower = text.lowercase()
        val lowerHighlight = highlight.lowercase()
        while (true) {
            val idx = lower.indexOf(lowerHighlight, start)
            if (idx < 0) { append(text.substring(start)); break }
            append(text.substring(start, idx))
            withStyle(SpanStyle(
                background = QuranGold.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )) {
                append(text.substring(idx, idx + highlight.length))
            }
            start = idx + highlight.length
        }
    }
    Text(
        text = annotated,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
        style = LocalTextStyle.current.copy(textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}
