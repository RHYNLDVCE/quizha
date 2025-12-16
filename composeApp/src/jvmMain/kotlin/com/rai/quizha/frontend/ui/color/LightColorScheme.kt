package com.rai.quizha.frontend.ui.color

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- Light Color Scheme ---
private val LightColorScheme = lightColorScheme(
    primary = MaroonLight,          // Key color for interactive elements (buttons, primary icons)
    onPrimary = Color.White,        // Text/Icon color on top of primary
    primaryContainer = Color(0xFFFDDCE3), // Lighter background for primary-related elements
    onPrimaryContainer = MaroonDark,

    secondary = SecondaryColor,
    onSecondary = Color.White,

    background = NeutralLight,      // Main background color (e.g., the window area)
    onBackground = NeutralDark,     // Text color on background

    surface = Color.White,          // Color for cards, sheets, or distinct containers
    onSurface = NeutralDark,        // Text color on surface

    error = Color(0xFFB00020),
    onError = Color.White
)

// --- Dark Color Scheme ---
private val DarkColorScheme = darkColorScheme(
    primary = MaroonLight,          // Keeping a vibrant color for primary in dark mode
    onPrimary = Color.Black,
    primaryContainer = MaroonDark,
    onPrimaryContainer = Color.White,

    secondary = SecondaryColor,
    onSecondary = Color.White,

    background = NeutralDark,
    onBackground = NeutralLight,

    surface = Color(0xFF2D2C31),
    onSurface = NeutralLight,

    error = Color(0xFFCF6679),
    onError = Color.Black
)

/**
 * The main Quizha application theme.
 * Uses a Light Maroon as the primary color.
 *
 * @param darkTheme Automatically defaults to system preference, but can be forced.
 * @param content The composable content to apply the theme to.
 */
@Composable
fun QuizhaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(), // Uses Material3 standard typography
        content = content
    )
}