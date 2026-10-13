package com.quran.romanurdu.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quran.romanurdu.data.model.AyahEntity
import com.quran.romanurdu.utils.ShareUtils
import com.quran.romanurdu.viewmodel.AudioState
import com.quran.romanurdu.viewmodel.AudioViewModel
import com.quran.romanurdu.viewmodel.DisplayLanguage
import com.quran.romanurdu.viewmodel.LoadState
import com.quran.romanurdu.viewmodel.QuranViewModel
import com.quran.romanurdu.ui.theme.QuranGold
import com.quran.romanurdu.ui.theme.QuranGreen
import com.quran.romanurdu.utils.HijriUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: QuranViewModel,
    navController: NavController,
    audioViewModel: AudioViewModel
) {
    val surahNumber by viewModel.currentSurahNumber.collectAsState()
    val ayahs by viewModel.ayahs.collectAsState()
    val loadState by viewModel.loadingState.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val displayLanguage by viewModel.displayLanguage.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    // Audio state
    val audioState by audioViewModel.audioState.collectAsState()
    val currentAyahIndex by audioViewModel.currentAyahIndex.collectAsState()
    val isAudioPlaying = audioState is AudioState.Playing
    val isAudioLoading = audioState is AudioState.Loading

    val surahMeta = viewModel.getSurahMeta(surahNumber)
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // v6: mark surah as read when it loads, show Hijri date
    val hijriDate = remember { HijriUtils.today() }
    LaunchedEffect(surahNumber) {
        viewModel.markSurahRead(surahNumber)
    }

    // v6: save last-read position as user scrolls
    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleIndex) {
        val ayahOffset = (firstVisibleIndex - 2).coerceAtLeast(0) // -2 for header + bismillah
        if (ayahs.isNotEmpty() && ayahOffset < ayahs.size) {
            val ayah = ayahs[ayahOffset]
            viewModel.saveLastRead(surahNumber, ayah.ayahNumber)
        }
    }

    // Auto-scroll to highlighted ayah when audio advances
    LaunchedEffect(currentAyahIndex) {
        if (currentAyahIndex >= 0 && isAudioPlaying) {
            listState.animateScrollToItem(
                index = currentAyahIndex + 2, // +2 for header + bismillah items
                scrollOffset = -200
            )
        }
    }

    // Preload next surah once this one is visible
    LaunchedEffect(surahNumber) { viewModel.preloadNextSurah() }

    // Stop audio when navigating away from a different surah
    DisposableEffect(surahNumber) {
        onDispose { /* keep playing across surah switches */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = surahMeta?.let { "${it.number}. ${it.nameRoman}" } ?: "Qur'an",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = surahMeta?.nameArabic ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "· ${hijriDate.toShortString()}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("surah_list") }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, "Surah list")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.decreaseFontSize() }) {
                        Icon(Icons.Filled.TextDecrease, "Smaller text")
                    }
                    IconButton(onClick = { viewModel.increaseFontSize() }) {
                        Icon(Icons.Filled.TextIncrease, "Larger text")
                    }
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            if (darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            "Toggle theme"
                        )
                    }
                    IconButton(onClick = { navController.navigate("bookmarks") }) {
                        Icon(Icons.Filled.CollectionsBookmark, "Bookmarks")
                    }
                    // ── New v5 navigation icons ──
                    IconButton(onClick = { navController.navigate("search") }) {
                        Icon(Icons.Filled.Search, "Search Quran")
                    }
                    IconButton(onClick = { navController.navigate("hadith") }) {
                        Icon(Icons.Filled.ImportContacts, "Hadith")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Column {
                // Audio player bar (shown when audio is active)
                AnimatedVisibility(
                    visible = audioState !is AudioState.Idle,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    AudioPlayerBar(
                        audioState = audioState,
                        currentAyahIndex = currentAyahIndex,
                        totalAyahs = ayahs.size,
                        surahName = surahMeta?.nameRoman ?: "",
                        isLoading = isAudioLoading,
                        onPlayPause = { audioViewModel.togglePlayPause() },
                        onPrevious = { audioViewModel.previousAyah() },
                        onNext = { audioViewModel.nextAyah() },
                        onStop = { audioViewModel.stop() }
                    )
                }
                // Language filter chips
                LanguageFilterBar(
                    selected = displayLanguage,
                    onSelect = { viewModel.setDisplayLanguage(it) }
                )
            }
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (surahNumber > 1) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.navigateToSurah(surahNumber - 1) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) { Icon(Icons.Filled.KeyboardArrowUp, "Previous surah") }
                }
                // Play / stop surah audio
                FloatingActionButton(
                    onClick = {
                        if (isAudioPlaying) audioViewModel.togglePlayPause()
                        else audioViewModel.playSurah(surahNumber)
                    },
                    containerColor = QuranGreen
                ) {
                    if (isAudioLoading)
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else
                        Icon(
                            if (isAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play / Pause",
                            tint = Color.White
                        )
                }
                if (surahNumber < 114) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.navigateToSurah(surahNumber + 1) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) { Icon(Icons.Filled.KeyboardArrowDown, "Next surah") }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (loadState) {
                is LoadState.Loading -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading Surah $surahNumber…", fontSize = 15.sp)
                    }
                }
                is LoadState.Error -> {
                    Column(
                        Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.WifiOff, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Text((loadState as LoadState.Error).message,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.navigateToSurah(surahNumber) }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    if (ayahs.isEmpty() && loadState == LoadState.Idle) {
                        LaunchedEffect(surahNumber) { viewModel.navigateToSurah(surahNumber) }
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            SurahHeader(
                                surahNumber = surahNumber,
                                nameRoman = surahMeta?.nameRoman ?: "",
                                nameArabic = surahMeta?.nameArabic ?: "",
                                meaning = surahMeta?.let { "${it.ayahs} verses" } ?: ""
                            )
                        }

                        if (surahNumber != 1 && surahNumber != 9) {
                            item { BismillahBanner() }
                        }

                        itemsIndexed(ayahs, key = { _, ayah -> ayah.globalNumber }) { index, ayah ->
                            val isHighlighted = isAudioPlaying && index == currentAyahIndex
                            AyahCard(
                                ayah = ayah,
                                fontSize = fontSize,
                                displayLanguage = displayLanguage,
                                isBookmarked = bookmarks.contains(ayah.globalNumber),
                                onToggleBookmark = { viewModel.toggleBookmark(ayah.globalNumber) },
                                darkMode = darkMode,
                                isHighlighted = isHighlighted,
                                onPlayFromHere = { audioViewModel.playFromAyah(index) },
                                onShare = {
                                    ShareUtils.shareAyah(
                                        context = context,
                                        ayah = ayah,
                                        surahName = surahMeta?.nameRoman ?: "Unknown"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Audio Player Bar ─────────────────────────────────────────────────────────

@Composable
private fun AudioPlayerBar(
    audioState: AudioState,
    currentAyahIndex: Int,
    totalAyahs: Int,
    surahName: String,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        color = QuranGreen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surahName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (currentAyahIndex >= 0) {
                    Text(
                        text = "Ayah ${currentAyahIndex + 1} of $totalAyahs",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
            }

            // Prev
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.SkipPrevious, "Previous ayah", tint = Color.White)
            }

            // Play/Pause
            IconButton(onClick = onPlayPause) {
                if (isLoading)
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                else
                    Icon(
                        if (audioState is AudioState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
            }

            // Next
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, "Next ayah", tint = Color.White)
            }

            // Stop/close
            IconButton(onClick = onStop) {
                Icon(Icons.Filled.Close, "Stop", tint = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

// ── Surah Header ─────────────────────────────────────────────────────────────

@Composable
private fun SurahHeader(
    surahNumber: Int,
    nameRoman: String,
    nameArabic: String,
    meaning: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = nameArabic,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Surah $surahNumber — $nameRoman",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = meaning,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Bismillah Banner ─────────────────────────────────────────────────────────

@Composable
private fun BismillahBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Text(
            text = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِیمِ",
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp)
        )
    }
}

// ── Language Filter Bottom Bar ────────────────────────────────────────────────

@Composable
private fun LanguageFilterBar(
    selected: DisplayLanguage,
    onSelect: (DisplayLanguage) -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DisplayLanguage.entries.forEach { lang ->
                FilterChip(
                    selected = selected == lang,
                    onClick = { onSelect(lang) },
                    label = { Text(lang.label, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

// ── Ayah Card ─────────────────────────────────────────────────────────────────

@Composable
fun AyahCard(
    ayah: AyahEntity,
    fontSize: Float,
    displayLanguage: DisplayLanguage,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    darkMode: Boolean,
    isHighlighted: Boolean = false,
    onPlayFromHere: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val cardBg = when {
        isHighlighted -> QuranGreen.copy(alpha = 0.12f)
        ayah.ayahNumber % 2 == 0 -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    // Gold left border when highlighted (currently playing)
    val borderModifier = if (isHighlighted)
        Modifier.border(
            width = 3.dp,
            color = QuranGold,
            shape = RoundedCornerShape(0.dp)
        )
    else Modifier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .background(cardBg)
    ) {
        // ── Ayah number row with action buttons ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Numbered badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isHighlighted) QuranGold.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            ) {
                Text(
                    text = ayah.ayahNumber.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) QuranGreen
                            else MaterialTheme.colorScheme.primary
                )
            }

            if (ayah.sajda) {
                Spacer(Modifier.width(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("Sajda", fontSize = 10.sp) },
                    modifier = Modifier.height(24.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }

            Spacer(Modifier.weight(1f))

            Text("Juz ${ayah.juz}", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))

            // Play from this ayah
            onPlayFromHere?.let { play ->
                IconButton(onClick = play, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.PlayCircle,
                        "Play from here",
                        tint = if (isHighlighted) QuranGreen
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Share
            onShare?.let { share ->
                IconButton(onClick = share, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Share, "Share ayah",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }

            // Bookmark
            IconButton(onClick = onToggleBookmark, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    "Bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.secondary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Arabic ──
        if (displayLanguage == DisplayLanguage.ALL || displayLanguage == DisplayLanguage.ARABIC_ONLY) {
            Text(
                text = ayah.arabic,
                fontSize = (fontSize + 8).sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                lineHeight = (fontSize + 20).sp,
                textAlign = TextAlign.End,
                style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                color = if (isHighlighted) QuranGreen
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(4.dp))
        }

        // ── Transliteration ──
        if (displayLanguage == DisplayLanguage.ALL || displayLanguage == DisplayLanguage.TRANSLITERATION) {
            Text(
                text = ayah.transliteration,
                fontSize = fontSize.sp,
                lineHeight = (fontSize + 8).sp,
                color = if (isHighlighted) QuranGreen.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        // ── Hindi ──
        if (displayLanguage == DisplayLanguage.ALL || displayLanguage == DisplayLanguage.HINDI) {
            if (ayah.hindi.isNotBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Top) {
                    Surface(
                        color = Color(0xFFFF6B35).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                    ) {
                        Text("HI", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFFCC4400),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                    Text(text = ayah.hindi, fontSize = (fontSize - 1).sp,
                        lineHeight = (fontSize + 6).sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f))
                }
            }
        }

        // ── Urdu ──
        if (displayLanguage == DisplayLanguage.ALL || displayLanguage == DisplayLanguage.URDU) {
            if (ayah.urdu.isNotBlank()) {
                if (displayLanguage == DisplayLanguage.ALL) Spacer(Modifier.height(4.dp))
                else HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Top) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                    ) {
                        Text("UR", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                    Text(text = ayah.urdu, fontSize = (fontSize - 1).sp,
                        lineHeight = (fontSize + 6).sp,
                        textAlign = TextAlign.End,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
