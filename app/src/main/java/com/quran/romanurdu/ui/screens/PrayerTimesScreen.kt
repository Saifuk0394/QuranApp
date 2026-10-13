package com.quran.romanurdu.ui.screens

import android.content.Context
import android.location.LocationManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quran.romanurdu.ui.theme.QuranGold
import com.quran.romanurdu.ui.theme.QuranGreen
import com.quran.romanurdu.utils.HijriUtils
import com.quran.romanurdu.utils.PrayerTimesUtils
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("MissingPermission")
@Composable
fun PrayerTimesScreen(navController: NavController) {
    val context = LocalContext.current

    var prayerTimes by remember { mutableStateOf<PrayerTimesUtils.PrayerTimes?>(null) }
    var locationName by remember { mutableStateOf("Calculating…") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedMethod by remember { mutableStateOf(PrayerTimesUtils.CalculationMethod.KARACHI) }

    val hijri = remember { HijriUtils.today() }
    val today = remember {
        Calendar.getInstance().let {
            Triple(
                it.get(Calendar.YEAR),
                it.get(Calendar.MONTH) + 1,
                it.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    fun loadPrayerTimes() {
        isLoading = true
        errorMessage = null
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )
            val loc = providers.firstNotNullOfOrNull { p ->
                try { lm.getLastKnownLocation(p) } catch (_: Exception) { null }
            }
            if (loc == null) {
                // Fallback: default to Mumbai coordinates
                val lat = 19.076; val lng = 72.877
                val tz = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0
                prayerTimes = PrayerTimesUtils.calculate(lat, lng, tz, today, selectedMethod)
                locationName = "Mumbai, India (default)"
            } else {
                val tz = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0
                prayerTimes = PrayerTimesUtils.calculate(loc.latitude, loc.longitude, tz, today, selectedMethod)
                locationName = "%.4f°N, %.4f°E".format(loc.latitude, loc.longitude)
            }
        } catch (e: Exception) {
            errorMessage = "Could not calculate prayer times:\n${e.message}"
        }
        isLoading = false
    }

    LaunchedEffect(selectedMethod) { loadPrayerTimes() }

    val prayers = prayerTimes?.let {
        listOf(
            PrayerEntry("Fajr", "الفجر", it.fajr, "🌙", QuranGreen.copy(alpha = 0.15f)),
            PrayerEntry("Sunrise", "الشروق", it.sunrise, "🌅", Color.Transparent),
            PrayerEntry("Dhuhr", "الظهر", it.dhuhr, "☀️", QuranGreen.copy(alpha = 0.15f)),
            PrayerEntry("Asr", "العصر", it.asr, "🌤️", Color.Transparent),
            PrayerEntry("Maghrib", "المغرب", it.maghrib, "🌇", QuranGreen.copy(alpha = 0.15f)),
            PrayerEntry("Isha", "العشاء", it.isha, "🌙", Color.Transparent),
        )
    } ?: emptyList()

    // Find next prayer
    val nowMin = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
    val nextPrayerIndex = prayerTimes?.let {
        val mins = listOf(it.fajrMin, it.sunriseMin, it.dhuhrMin, it.asrMin, it.maghribMin, it.ishaMin)
        mins.indexOfFirst { m -> m > nowMin }.takeIf { it >= 0 } ?: 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prayer Times", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadPrayerTimes() }) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = Color.White)
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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Date header card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(QuranGreen, QuranGreen.copy(alpha = 0.75f))
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            hijri.toArabicString(),
                            fontSize = 22.sp,
                            color = QuranGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            hijri.toEnglishString(),
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.LocationOn, null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(locationName, fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Method selector
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Calculation Method", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp))
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedMethod.label,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QuranGreen,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.5f)
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            PrayerTimesUtils.CalculationMethod.entries.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.label, fontSize = 13.sp) },
                                    onClick = {
                                        selectedMethod = method
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = QuranGreen)
                    }
                }
            } else if (errorMessage != null) {
                item {
                    Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Prayer time rows
                items(prayers.indices.toList()) { i ->
                    val prayer = prayers[i]
                    val isNext = i == nextPrayerIndex

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNext)
                                QuranGreen.copy(alpha = 0.12f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = if (isNext) CardDefaults.outlinedCardBorder().copy(
                            // highlight border handled by color
                        ) else null,
                        elevation = CardDefaults.cardElevation(if (isNext) 3.dp else 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(prayer.emoji, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        prayer.nameEn,
                                        fontSize = 16.sp,
                                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isNext) QuranGreen
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isNext) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = QuranGold
                                        ) {
                                            Text(
                                                "NEXT",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1A1A1A),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    prayer.nameAr,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                prayer.time,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNext) QuranGreen else QuranGold
                            )
                        }
                    }
                }
            }

            // Upcoming Islamic events
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Upcoming Islamic Events",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val events = HijriUtils.upcomingEvents(hijri.year).take(6)
            items(events) { (date, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = QuranGreen.copy(alpha = 0.1f),
                        modifier = Modifier.width(52.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Text(date.day.toString(), fontSize = 16.sp,
                                fontWeight = FontWeight.Bold, color = QuranGreen)
                            Text(date.monthName.take(3), fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("${date.monthName} ${date.day}, ${date.year} AH",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

private data class PrayerEntry(
    val nameEn: String,
    val nameAr: String,
    val time: String,
    val emoji: String,
    val bgColor: Color
)
