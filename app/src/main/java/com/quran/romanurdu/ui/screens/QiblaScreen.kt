package com.quran.romanurdu.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quran.romanurdu.ui.theme.QuranGold
import com.quran.romanurdu.ui.theme.QuranGreen
import kotlin.math.*

// Mecca coordinates
private const val MECCA_LAT = 21.3891
private const val MECCA_LNG = 39.8579

/**
 * Calculate bearing from (lat1, lng1) to Mecca in degrees (0–360, clockwise from North).
 */
fun calculateQiblaBearing(lat: Double, lng: Double): Double {
    val lat1 = Math.toRadians(lat)
    val lat2 = Math.toRadians(MECCA_LAT)
    val deltaLng = Math.toRadians(MECCA_LNG - lng)

    val x = sin(deltaLng) * cos(lat2)
    val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng)
    val bearing = Math.toDegrees(atan2(x, y))
    return (bearing + 360) % 360
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("MissingPermission")
@Composable
fun QiblaScreen(navController: NavController) {
    val context = LocalContext.current

    // Compass heading (degrees from magnetic north, clockwise)
    var compassHeading by remember { mutableFloatStateOf(0f) }
    // User's GPS location
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Qibla bearing from user's location (null until we have GPS)
    val qiblaBearing = remember(userLocation) {
        userLocation?.let { calculateQiblaBearing(it.latitude, it.longitude) }
    }

    // Needle rotation = qibla bearing relative to current compass heading
    // needle points toward Mecca = qiblaBearing - compassHeading
    val needleAngle = remember(qiblaBearing, compassHeading) {
        qiblaBearing?.let { ((it - compassHeading + 360) % 360).toFloat() } ?: 0f
    }

    // Smooth animation for the needle
    val animatedNeedle by animateFloatAsState(
        targetValue = needleAngle,
        animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
        label = "needle"
    )

    // ── Sensor listener (compass) ─────────────────────────────────────────
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> gravity.also { g ->
                        // Low-pass filter
                        val alpha = 0.8f
                        g[0] = alpha * g[0] + (1 - alpha) * event.values[0]
                        g[1] = alpha * g[1] + (1 - alpha) * event.values[1]
                        g[2] = alpha * g[2] + (1 - alpha) * event.values[2]
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic.also { gm ->
                        event.values.copyInto(gm)
                    }
                }
                if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    compassHeading = (azimuth + 360) % 360
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sm.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sm.unregisterListener(listener)
        }
    }

    // ── Location (last known, no continuous updates needed) ───────────────
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect
        try {
            @Suppress("MissingPermission")
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val providers = listOf(
                android.location.LocationManager.GPS_PROVIDER,
                android.location.LocationManager.NETWORK_PROVIDER
            )
            val loc = providers.firstNotNullOfOrNull { provider ->
                try { lm.getLastKnownLocation(provider) } catch (_: Exception) { null }
            }
            userLocation = loc
            if (loc == null) locationError = "Could not get location. Try stepping outside."
        } catch (e: Exception) {
            locationError = e.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Qibla Direction", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Permission check — no accompanist needed, use ActivityResultContracts
            LocationPermissionRequest(
                onGranted = { hasLocationPermission = true }
            )

            Spacer(Modifier.height(16.dp))

            // Compass rose + needle
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Compass rose (always oriented to North = up, rotated by device heading)
                CompassRose(
                    modifier = Modifier.fillMaxSize(),
                    rotation = -compassHeading
                )
                // Qibla needle
                if (qiblaBearing != null) {
                    QiblaNeedle(
                        modifier = Modifier
                            .size(220.dp)
                            .rotate(animatedNeedle),
                    )
                }
            }

            // Distance to Mecca
            userLocation?.let { loc ->
                val distKm = calculateDistanceToMecca(loc.latitude, loc.longitude)
                Text(
                    text = "%.0f km to Mecca".format(distKm),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Bearing: %.1f°".format(qiblaBearing ?: 0.0),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            locationError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            if (!hasLocationPermission) {
                Text(
                    "Location permission is needed to calculate the Qibla direction accurately.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = QuranGreen.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("الْكَعْبَةُ الْمُشَرَّفَة", fontSize = 22.sp, color = QuranGold)
                    Text("Al-Ka'bah Al-Musharrafah", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Mecca, Saudi Arabia", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CompassRose(modifier: Modifier, rotation: Float) {
    val color = MaterialTheme.colorScheme.onSurface
    val northColor = Color(0xFFD32F2F)

    Canvas(modifier = modifier.rotate(rotation)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.width / 2 - 8.dp.toPx()

        // Outer ring
        drawCircle(color = color.copy(alpha = 0.15f), radius = radius, style = Stroke(2.dp.toPx()))

        // Tick marks every 30°
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val inner = if (i % 3 == 0) radius * 0.82f else radius * 0.88f
            drawLine(
                color = color.copy(alpha = 0.4f),
                start = Offset((cx + inner * cos(angle)).toFloat(), (cy + inner * sin(angle)).toFloat()),
                end = Offset((cx + radius * cos(angle)).toFloat(), (cy + radius * sin(angle)).toFloat()),
                strokeWidth = if (i % 3 == 0) 2.dp.toPx() else 1.dp.toPx()
            )
        }

        // N marker (red triangle pointing up)
        val nAngle = Math.toRadians(-90.0)
        val nTip = Offset(
            (cx + (radius * 0.72f) * cos(nAngle)).toFloat(),
            (cy + (radius * 0.72f) * sin(nAngle)).toFloat()
        )
        drawCircle(northColor, radius = 12.dp.toPx(), center = Offset(
            (cx + radius * 0.88f * cos(nAngle)).toFloat(),
            (cy + radius * 0.88f * sin(nAngle)).toFloat()
        ))
    }
}

@Composable
private fun QiblaNeedle(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val len = size.height / 2 * 0.75f

        // Crescent at tip
        drawCircle(QuranGold, radius = 14.dp.toPx(),
            center = Offset(cx, cy - len + 14.dp.toPx()))

        // Needle shaft — green pointing toward Mecca (up = Qibla direction)
        drawLine(
            color = QuranGreen,
            start = Offset(cx, cy + len * 0.3f),
            end = Offset(cx, cy - len + 28.dp.toPx()),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Tail
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(cx, cy),
            end = Offset(cx, cy + len * 0.3f),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Center pivot
        drawCircle(QuranGold, radius = 8.dp.toPx(), center = Offset(cx, cy))
    }
}

@Composable
private fun LocationPermissionRequest(onGranted: () -> Unit) {
    val permissionState = rememberLocationPermission(onGranted)
    if (!permissionState) {
        Button(
            onClick = { /* handled inside */ },
            colors = ButtonDefaults.buttonColors(containerColor = QuranGreen)
        ) {
            Text("Allow Location", color = Color.White)
        }
    }
}

@Composable
private fun rememberLocationPermission(onGranted: () -> Unit): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val fineGranted = pm.checkPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            context.packageName
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = pm.checkPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            context.packageName
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            granted = true
            onGranted()
        }
    }

    // Runtime permission request using Activity
    val activity = context as? android.app.Activity
    if (!granted && activity != null) {
        LaunchedEffect(Unit) {
            activity.requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
        }
    }

    return granted
}

private fun calculateDistanceToMecca(lat: Double, lng: Double): Double {
    val R = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(MECCA_LAT - lat)
    val dLng = Math.toRadians(MECCA_LNG - lng)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(MECCA_LAT)) * sin(dLng / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
