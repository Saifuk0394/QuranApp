package com.quran.romanurdu.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Islamic green palette
val QuranGreen = Color(0xFF1B6B3A)
val QuranGreenDark = Color(0xFF0D4A25)
val QuranGold = Color(0xFFD4AF37)
val QuranGoldLight = Color(0xFFEDD97A)
val QuranCream = Color(0xFFFFFBF2)
val QuranDarkBg = Color(0xFF121212)
val QuranDarkSurface = Color(0xFF1E1E1E)

private val LightColors = lightColorScheme(
    primary = QuranGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F0C8),
    onPrimaryContainer = Color(0xFF002111),
    secondary = QuranGold,
    onSecondary = Color.Black,
    secondaryContainer = QuranGoldLight,
    background = QuranCream,
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0EDE6),
    onSurfaceVariant = Color(0xFF44483D),
)

private val DarkColors = darkColorScheme(
    primary = QuranGold,
    onPrimary = Color(0xFF3A2E00),
    primaryContainer = QuranGreenDark,
    onPrimaryContainer = QuranGoldLight,
    secondary = QuranGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B4A2E),
    background = QuranDarkBg,
    onBackground = Color(0xFFE6E1D5),
    surface = QuranDarkSurface,
    onSurface = Color(0xFFE6E1D5),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFCAC5B8),
)

@Composable
fun QuranAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
