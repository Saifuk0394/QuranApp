package com.quran.romanurdu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quran.romanurdu.data.HADITH_COLLECTIONS
import com.quran.romanurdu.data.HadithLanguage
import com.quran.romanurdu.data.model.HadithEntity
import com.quran.romanurdu.ui.theme.QuranGold
import com.quran.romanurdu.ui.theme.QuranGreen
import com.quran.romanurdu.viewmodel.HadithLoadState
import com.quran.romanurdu.viewmodel.HadithViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithScreen(
    navController: NavController,
    hadithViewModel: HadithViewModel = viewModel()
) {
    val selectedCollection by hadithViewModel.selectedCollection.collectAsState()
    val selectedLanguage   by hadithViewModel.selectedLanguage.collectAsState()
    val loadState          by hadithViewModel.loadState.collectAsState()
    val hadiths            by hadithViewModel.hadiths.collectAsState()
    val searchQuery        by hadithViewModel.searchQuery.collectAsState()
    val searchResults      by hadithViewModel.searchResults.collectAsState()

    val displayList = if (searchQuery.isBlank()) hadiths else searchResults

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                selectedCollection.nameEnglish,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                selectedCollection.nameArabic,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("asma") }) {
                            Icon(Icons.Filled.AutoAwesome, "99 Names", tint = Color.White)
                        }
                        IconButton(onClick = { navController.navigate("qibla") }) {
                            Icon(Icons.Filled.Explore, "Qibla", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = QuranGreen,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
                // Collection chips
                CollectionChipsRow(
                    selectedId = selectedCollection.id,
                    onSelect = { hadithViewModel.selectCollection(it) }
                )
                // Language tabs
                LanguageTabRow(
                    selected = selectedLanguage,
                    onSelect = { hadithViewModel.selectLanguage(it) }
                )
                // Search bar
                HadithSearchBar(
                    query = searchQuery,
                    onQueryChange = { hadithViewModel.setSearchQuery(it) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                loadState is HadithLoadState.Loading && hadiths.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = QuranGreen, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Downloading ${selectedCollection.nameEnglish}…",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "This only happens once",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                loadState is HadithLoadState.Error && hadiths.isEmpty() -> {
                    ErrorContent(
                        message = (loadState as HadithLoadState.Error).message,
                        onRetry = { hadithViewModel.retry() }
                    )
                }

                displayList.isEmpty() && searchQuery.isNotBlank() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No hadiths matching \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (hadiths.isNotEmpty() && searchQuery.isBlank()) {
                            item {
                                HadithCountHeader(
                                    count = hadiths.size,
                                    collectionName = selectedCollection.nameEnglish
                                )
                            }
                        }
                        itemsIndexed(displayList, key = { _, h -> "${h.collection}_${h.language}_${h.hadithNumber}" }) { _, hadith ->
                            HadithCard(
                                hadith = hadith,
                                isRtl = selectedLanguage.isRtl
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun CollectionChipsRow(
    selectedId: String,
    onSelect: (com.quran.romanurdu.data.HadithCollection) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(QuranGreen.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HADITH_COLLECTIONS, key = { it.id }) { col ->
            val selected = col.id == selectedId
            FilterChip(
                selected = selected,
                onClick = { onSelect(col) },
                label = {
                    Text(
                        col.nameEnglish.replace("Sunan ", "").replace("Sahih ", "").replace("Muwatta ", ""),
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = QuranGold,
                    selectedLabelColor = Color(0xFF1A1A1A),
                    containerColor = Color.White.copy(alpha = 0.15f),
                    labelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun LanguageTabRow(
    selected: HadithLanguage,
    onSelect: (HadithLanguage) -> Unit
) {
    TabRow(
        selectedTabIndex = HadithLanguage.entries.indexOf(selected),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = QuranGreen,
        indicator = { tabPositions ->
            val index = HadithLanguage.entries.indexOf(selected)
            if (index >= 0 && index < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[index], matchContentSize = false),
                    color = QuranGold
                )
            }
        }
    ) {
        HadithLanguage.entries.forEach { lang ->
            Tab(
                selected = lang == selected,
                onClick = { onSelect(lang) },
                text = { Text(lang.label, fontSize = 13.sp) }
            )
        }
    }
}

@Composable
private fun HadithSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        placeholder = { Text("Search hadiths…", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = QuranGreen) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = QuranGreen,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
    )
}

@Composable
private fun HadithCountHeader(count: Int, collectionName: String) {
    Text(
        text = "$count hadiths · $collectionName",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun HadithCard(
    hadith: HadithEntity,
    isRtl: Boolean
) {
    val textDir = if (isRtl) TextDirection.Rtl else TextDirection.Ltr

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Number badge + grade
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hadith number circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(QuranGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = hadith.hadithNumber.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Hadith #${hadith.hadithNumber}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (hadith.bookNumber > 0) {
                        Text(
                            "Book ${hadith.bookNumber}, Hadith ${hadith.bookHadithNumber}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Grade badge
                if (hadith.grade.isNotBlank()) {
                    val gradeColor = when (hadith.grade.lowercase()) {
                        "sahih", "صحيح" -> Color(0xFF2E7D32)
                        "hasan", "حسن"  -> Color(0xFF1565C0)
                        "daif", "ضعيف"  -> Color(0xFFC62828)
                        else             -> MaterialTheme.colorScheme.tertiary
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = gradeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = hadith.grade,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = gradeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Hadith text
            Text(
                text = hadith.text,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
                style = LocalTextStyle.current.copy(textDirection = textDir),
                textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = QuranGreen)
        ) {
            Text("Try Again", color = Color.White)
        }
    }
}

