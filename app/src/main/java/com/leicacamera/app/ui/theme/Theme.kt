package com.leicacamera.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LeicaBlack = Color(0xFF0A0A0A)
val LeicaAnthracite = Color(0xFF1A1A1A)
val LeicaDarkGray = Color(0xFF2C2C2E)
val LeicaWhite = Color(0xFFF5F5F5)
val LeicaSilver = Color(0xFFA0A0A5)
val LeicaRed = Color(0xFFE20612)
val LeicaAmber = Color(0xFFE5B84B)

private val LeicaColorScheme = darkColorScheme(
    primary = LeicaWhite,
    onPrimary = LeicaBlack,
    secondary = LeicaRed,
    onSecondary = LeicaWhite,
    background = LeicaBlack,
    onBackground = LeicaWhite,
    surface = LeicaAnthracite,
    onSurface = LeicaWhite
)

@Composable
fun LeicaCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LeicaColorScheme,
        content = content
    )
}