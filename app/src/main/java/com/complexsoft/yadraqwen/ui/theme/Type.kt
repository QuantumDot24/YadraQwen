package com.complexsoft.yadraqwen.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Monospace para respuestas de la IA — feel terminal / computacional
val MonoFamily = FontFamily.Monospace

val NeuralTypography = Typography(
    // Títulos de la app
    headlineLarge = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.W700,
        fontSize    = 28.sp,
        lineHeight  = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.W600,
        fontSize    = 20.sp,
        lineHeight  = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    // Texto body estándar (mensajes usuario)
    bodyLarge = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.W400,
        fontSize    = 15.sp,
        lineHeight  = 23.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.W400,
        fontSize    = 13.sp,
        lineHeight  = 20.sp,
    ),
    // Labels UI
    labelLarge = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.W600,
        fontSize    = 12.sp,
        letterSpacing = 1.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily  = FontFamily.Default,
        fontWeight  = FontWeight.W400,
        fontSize    = 10.sp,
        letterSpacing = 0.5.sp
    ),
)