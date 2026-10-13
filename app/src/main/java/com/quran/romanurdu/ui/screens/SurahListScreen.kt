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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quran.romanurdu.data.PreferencesRepository
import com.quran.romanurdu.data.Surah
import com.quran.romanurdu.viewmodel.QuranViewModel
import com.quran.romanurdu.ui.theme.QuranGold
import com.quran.romanurdu.ui.theme.QuranGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    viewModel: QuranViewModel,
    navController: NavController
) {
    val surahs by viewModel.filteredSurahs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    // v6: read progress + last read
    val readSurahs by viewModel.readSurahs.collectAsState()
    val readCount by viewModel.readSurahCount.collectAsState()
    val lastRead by viewModel.lastRead.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Surah Index",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            if (readCount > 0) {
                                Text(
                                    "$readCount / 114 surahs read",
                                    fontSize = 12.sp,
                                    color = QuranGreen
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        // Khatm tracker shortcut
                        TextButton(onClick = { navController.navigate("khatm") }) {
                            Text("Progress", color = QuranGreen, fontSize = 12.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Progress bar
                if (readCount > 0) {
                    LinearProgressIndicator(
                        progress = { readCount / 114f },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = QuranGreen,
                        trackColor = QuranGreen.copy(alpha = 0.15f)
                    )
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search surah…") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // ── Last Read Banner ──────────────────────────────────────────
            lastRead?.let { lr ->
                item {
                    LastReadBanner(
                        lastRead = lr,
                        onClick = {
                            viewModel.selectSurah(lr.surahNumber)
                            navController.popBackStack()
                        }
                    )
                }
            }

            items(surahs, key = { it.number }) { surah ->
                SurahRow(
                    surah = surah,
                    isRead = surah.number in readSurahs,
                    onClick = {
                        viewModel.goToSurah(surah)
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

@Composable
private fun LastReadBanner(
    lastRead: PreferencesRepository.LastRead,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(QuranGreen, QuranGreen.copy(alpha = 0.75f))
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.PlayArrow, null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Continue Reading",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${lastRead.surahName} · Ayah ${lastRead.ayahNumber}",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text("›", fontSize = 24.sp, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun SurahRow(surah: Surah, isRead: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Surah number badge — green if read, else normal
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isRead) QuranGreen.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.primaryContainer
                )
        ) {
            if (isRead) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Read",
                    tint = QuranGreen,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = surah.number.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // Roman name + info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = surah.nameRoman,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${surah.ayahs} verses",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Juz ${surah.part}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Revelation type badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (surah.type == "Meccan")
                        Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = surah.type,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (surah.type == "Meccan")
                            Color(0xFFE65100) else Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Arabic name
        Text(
            text = surah.nameArabic,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = if (isRead) QuranGreen else MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 80.dp)
        )
    }
}
