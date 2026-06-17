package com.momobridge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MomoDarkScheme = darkColorScheme(
    primary = MomoColors.Gold,
    onPrimary = MomoColors.OnGold,
    primaryContainer = MomoColors.GoldDim,
    secondary = MomoColors.GoldVivid,
    tertiary = MomoColors.StatusConfirmed,
    background = MomoColors.GroundDark,
    surface = MomoColors.GroundMedium,
    surfaceVariant = MomoColors.GroundLight,
    onBackground = MomoColors.TextPrimary,
    onSurface = MomoColors.TextPrimary,
    onSurfaceVariant = MomoColors.TextSecondary,
    error = MomoColors.StatusFailed,
    onError = Color.White,
    outline = MomoColors.BorderSubtle,
    outlineVariant = MomoColors.BorderSubtle.copy(alpha = 0.5f)
)

@Composable
fun MomoBridgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MomoDarkScheme,
        typography = MomoMaterialTypography,
        content = content
    )
}
