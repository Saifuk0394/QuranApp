package com.quran.romanurdu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quran.romanurdu.data.AsmaData
import com.quran.romanurdu.data.AsmaName
import com.quran.romanurdu.ui.theme.QuranGold
import com.quran.romanurdu.ui.theme.QuranGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsmaScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredNames = remember(searchQuery) {
        if (searchQuery.isBlank()) AsmaData.names
        else AsmaData.names.filter { name ->
            name.transliteration.contains(searchQuery, ignoreCase = true) ||
            name.meaning.contains(searchQuery, ignoreCase = true) ||
            name.arabic.contains(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Asma ul-Husna", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("99 Names of Allah", fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f))
                    }
                },
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
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or meaning…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QuranGreen,
                    cursorColor = QuranGreen
                )
            )

            // Bismillah header
            if (searchQuery.isBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = QuranGreen.copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "وَلِلَّهِ الْأَسْمَاءُ الْحُسْنَىٰ فَادْعُوهُ بِهَا",
                            fontSize = 18.sp,
                            color = QuranGreen,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "\"And to Allah belong the best names, so invoke Him by them.\" (7:180)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredNames, key = { it.number }) { name ->
                    AsmaCard(name)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AsmaCard(name: AsmaName) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(QuranGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.number.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            // Transliteration + meaning (left column)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.transliteration,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QuranGreen
                )
                Text(
                    text = name.meaning,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            // Arabic (right, RTL)
            Text(
                text = name.arabic,
                fontSize = 22.sp,
                color = QuranGold,
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
