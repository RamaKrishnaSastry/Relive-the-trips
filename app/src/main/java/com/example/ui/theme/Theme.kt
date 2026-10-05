package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MarigoldGold,
    onPrimary = Color(0xFF431407),
    primaryContainer = TerracottaDark,
    onPrimaryContainer = TerracottaLight,
    secondary = TerracottaPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = SageForest,
    onTertiary = Color.White,
    background = TempleNightCanvas,
    onBackground = InkPrimaryDark,
    surface = TempleNightCard,
    onSurface = InkPrimaryDark,
    surfaceVariant = TempleNightCardMuted,
    onSurfaceVariant = InkSecondaryDark,
    outline = TempleNightBorder
)

private val LightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaLight,
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = MarigoldGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = AntiqueGold,
    tertiary = SageForest,
    onTertiary = Color.White,
    background = ParchmentCanvas,
    onBackground = InkPrimaryLight,
    surface = ParchmentCard,
    onSurface = InkPrimaryLight,
    surfaceVariant = ParchmentCardMuted,
    onSurfaceVariant = InkSecondaryLight,
    outline = ParchmentBorder
)

@Composable
fun TravelTempleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
