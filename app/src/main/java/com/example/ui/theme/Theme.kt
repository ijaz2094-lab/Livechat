package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PopLiveDarkColorScheme = darkColorScheme(
    primary = CoralPink,
    onPrimary = Color.White,
    primaryContainer = CoralContainer,
    onPrimaryContainer = CoralPinkLight,
    secondary = ElectricCyan,
    onSecondary = CosmicObsidian,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = ElectricCyan,
    tertiary = WarmGold,
    onTertiary = CosmicObsidian,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = WarmGold,
    background = CosmicObsidian,
    onBackground = TextPrimaryDark,
    surface = VelvetSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = VelvetSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    outline = VelvetOutline,
    error = Color(0xFFFF5370),
    onError = Color.White,
)

private val PopLiveLightColorScheme = lightColorScheme(
    primary = CoralPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCE8),
    onPrimaryContainer = CoralPinkDark,
    secondary = ElectricCyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F7FF),
    onSecondaryContainer = Color(0xFF003642),
    tertiary = WarmGoldDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF0C2),
    onTertiaryContainer = Color(0xFF3E2C07),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline,
    error = Color(0xFFD32F2F),
    onError = Color.White,
)

val PopLiveShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PopLiveTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) PopLiveDarkColorScheme else PopLiveLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PopLiveTypography,
        shapes = PopLiveShapes,
        content = content,
    )
}
