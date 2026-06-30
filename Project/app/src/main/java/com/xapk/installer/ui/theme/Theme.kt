package com.xapk.installer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Curated premium color palette for XInstaller (Dark Space Theme)
val DarkPurpleBg = Color(0xFF0A0516)      // Very deep obsidian purple background
val CardBg = Color(0xFF160E28)            // Translucent glassmorphic look surface
val PurplePrimary = Color(0xFF8B5CF6)     // Vibrant neon violet
val CyanSecondary = Color(0xFF06B6D4)     // Electric cyan
val MagentaTertiary = Color(0xFFD946EF)   // Bright magenta
val TextPrimary = Color(0xFFF3F4F6)       // Soft white
val TextSecondary = Color(0xFF9CA3AF)     // Cool gray
val ErrorColor = Color(0xFFEF4444)        // Neon red
val SuccessColor = Color(0xFF10B981)      // Emerald green

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    secondary = CyanSecondary,
    tertiary = MagentaTertiary,
    background = DarkPurpleBg,
    surface = CardBg,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorColor
)

@Composable
fun XInstallerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkPurpleBg.toArgb()
            window.navigationBarColor = DarkPurpleBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
