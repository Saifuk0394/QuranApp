package com.quran.romanurdu.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quran.romanurdu.data.model.KhatmEntity
import com.quran.romanurdu.viewmodel.QuranViewModel
import com.quran.romanurdu.ui.theme.QuranGold
import com.quran.romanurdu.ui.theme.QuranGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatmScreen(
    viewModel: QuranViewModel,
    navController: NavController
) {
    val readCount by viewModel.readSurahCount.collectAsState()
    val khatmCompletions by viewModel.khatmCompletions.collectAsState()
    val khatmCount by viewModel.khatmCount.collectAsState()

    var showKhatmDialog by remember { mutableStateOf(false) }
    var khatmNotes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Khatm Tracker", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Progress ring ──────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Reading Progress",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(20.dp))

                        // Circular ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(160.dp)
                        ) {
                            val fraction = readCount / 114f
                            val green = QuranGreen
                            val track = QuranGreen.copy(alpha = 0.1f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 18.dp.toPx()
                                val inset = stroke / 2f
                                drawArc(
                                    color = track,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - stroke, size.height - stroke),
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = green,
                                    startAngle = -90f,
                                    sweepAngle = 360f * fraction,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - stroke, size.height - stroke),
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$readCount",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QuranGreen
                                )
                                Text(
                                    text = "of 114",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = when {
                                readCount == 114 -> "Mashallah! All 114 surahs read 🎉"
                                readCount == 0   -> "Start reading to track progress"
                                readCount < 30   -> "${114 - readCount} surahs remaining"
                                readCount < 60   -> "Great progress! Keep going"
                                readCount < 90   -> "Over halfway — almost there!"
                                else             -> "So close! Only ${114 - readCount} left"
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { showKhatmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = QuranGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.EmojiEvents, null,
                                tint = Color(0xFF1A1A1A), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Record Khatm", color = Color(0xFF1A1A1A), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Khatm completions ──────────────────────────────────────────
            if (khatmCompletions.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Completed Khatms",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = QuranGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "$khatmCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = QuranGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                items(khatmCompletions.sortedByDescending { it.completedAt },
                    key = { it.id }) { khatm ->
                    KhatmCompletionCard(khatm)
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🌙", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No khatms recorded yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Record one after completing all 114 surahs",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ── Record Khatm dialog ────────────────────────────────────────────────
    if (showKhatmDialog) {
        AlertDialog(
            onDismissRequest = { showKhatmDialog = false; khatmNotes = "" },
            icon = { Icon(Icons.Filled.EmojiEvents, null, tint = QuranGold) },
            title = { Text("Record Khatm", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Alhamdulillah! Record this completion of the Qur'an.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = khatmNotes,
                        onValueChange = { khatmNotes = it },
                        placeholder = { Text("Notes (optional)…") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recordKhatm(khatmNotes)
                        showKhatmDialog = false
                        khatmNotes = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QuranGreen)
                ) {
                    Text("Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKhatmDialog = false; khatmNotes = "" }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun KhatmCompletionCard(khatm: KhatmEntity) {
    val dateStr = remember(khatm.completedAt) {
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(khatm.completedAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = QuranGold.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏆", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Khatm Completed",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    dateStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (khatm.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        khatm.notes,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
