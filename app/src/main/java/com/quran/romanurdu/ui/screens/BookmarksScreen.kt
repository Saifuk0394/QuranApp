package com.quran.romanurdu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quran.romanurdu.data.SurahData
import com.quran.romanurdu.viewmodel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    viewModel: QuranViewModel,
    navController: NavController
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val sorted = bookmarks.sorted()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarked Ayahs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (sorted.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.BookmarkBorder,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No bookmarks yet", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Tap the bookmark icon on any ayah to save it",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(sorted, key = { it }) { globalNumber ->
                    // Find which surah this ayah belongs to (using global numbering)
                    val surahForAyah = SurahData.surahs.lastOrNull {
                        // rough mapping: sum of ayahs up to this surah ≥ globalNumber
                        var cumulative = 0
                        for (s in SurahData.surahs) {
                            cumulative += s.ayahs
                            if (cumulative >= globalNumber) return@lastOrNull true
                        }
                        false
                    }

                    BookmarkRow(
                        globalAyahNumber = globalNumber,
                        surahName = surahForAyah?.let { "${it.number}. ${it.nameRoman}" } ?: "Ayah",
                        arabicName = surahForAyah?.nameArabic ?: "",
                        onClick = {
                            surahForAyah?.let { viewModel.navigateToSurah(it.number) }
                            navController.popBackStack()
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    globalAyahNumber: Int,
    surahName: String,
    arabicName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Bookmark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(surahName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Ayah #$globalAyahNumber", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(arabicName, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
    }
}
