package com.quran.romanurdu.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.quran.romanurdu.data.SurahData
import com.quran.romanurdu.ui.theme.QuranGold
import com.quran.romanurdu.ui.theme.QuranGreen
import com.quran.romanurdu.viewmodel.QuranViewModel

@Composable
fun SetupScreen(
    viewModel: QuranViewModel,
    navController: NavController
) {
    val progress by viewModel.downloadProgress.collectAsState()
    val error by viewModel.downloadError.collectAsState()
    val isSetupComplete by viewModel.isSetupComplete.collectAsState()

    // Navigate to reader as soon as setup is complete
    LaunchedEffect(isSetupComplete) {
        if (isSetupComplete) {
            navController.navigate("reader") {
                popUpTo("setup") { inclusive = true }
            }
        }
    }

    val isDownloading = progress > 0 && progress < 114 && error == null
    val isDone = progress == 114
    val currentSurahName = if (progress in 1..114)
        SurahData.surahs.getOrNull(progress - 1)?.nameRoman ?: ""
    else ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        QuranGreen,
                        Color(0xFF0D3D21),
                        Color(0xFF000000)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // Crescent + app title
            ArabicLogoHeader()

            Spacer(modifier = Modifier.height(8.dp))

            if (!isDownloading && !isDone && error == null) {
                // Initial state — ready to download
                DownloadReadyContent(onDownload = { viewModel.downloadAllSurahs() })
            } else if (isDownloading) {
                // Downloading
                DownloadingContent(
                    progress = progress,
                    currentSurahName = currentSurahName
                )
            } else if (error != null) {
                // Error state
                DownloadErrorContent(
                    errorMessage = error!!,
                    progress = progress,
                    onRetry = { viewModel.retryDownload() }
                )
            }
            // isDone handled by LaunchedEffect navigation
        }
    }
}

@Composable
private fun ArabicLogoHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Crescent moon symbol
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(QuranGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "☽",
                fontSize = 52.sp,
                color = QuranGold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
            fontSize = 22.sp,
            color = QuranGold,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Al-Quran",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp
        )

        Text(
            text = "Arabic • Roman • Hindi • Urdu",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun DownloadReadyContent(onDownload: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📥  Download the Full Quran",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Download all 114 Surahs once for completely offline reading.\n\nThis takes 1–2 minutes on Wi-Fi.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // Feature bullets
        FeatureBullet("📖", "All 114 Surahs — 6,236 Ayahs")
        FeatureBullet("🔤", "Arabic • Roman • Hindi • Urdu")
        FeatureBullet("✈️", "Fully offline after download")
        FeatureBullet("🔖", "Bookmarks & dark mode")

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDownload,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = QuranGold,
                contentColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Download Now",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FeatureBullet(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun DownloadingContent(progress: Int, currentSurahName: String) {
    val fraction = progress / 114f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Animated progress ring
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.size(120.dp),
                color = QuranGold,
                trackColor = Color.White.copy(alpha = 0.15f),
                strokeWidth = 8.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$progress",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "of 114",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Text(
            text = "Downloading…",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        if (currentSurahName.isNotBlank()) {
            Text(
                text = "Surah $progress · $currentSurahName",
                fontSize = 14.sp,
                color = QuranGold,
                textAlign = TextAlign.Center
            )
        }

        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = QuranGold,
            trackColor = Color.White.copy(alpha = 0.15f)
        )

        Text(
            text = "Please keep the app open",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun DownloadErrorContent(
    errorMessage: String,
    progress: Int,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )

        Text(
            text = "Download interrupted",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (progress > 0) {
            Text(
                text = "$progress of 114 surahs downloaded",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFB00020).copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "Make sure you have a working internet connection and try again.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = QuranGold,
                contentColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (progress > 0) "Resume Download" else "Try Again",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
