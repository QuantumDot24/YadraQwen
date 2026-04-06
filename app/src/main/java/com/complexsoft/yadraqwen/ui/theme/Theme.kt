package com.complexsoft.yadraqwen.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val YadraDarkScheme = darkColorScheme(
    primary              = VulkanRed,
    onPrimary            = Void,
    primaryContainer     = VulkanRedDim,
    onPrimaryContainer   = VulkanRed,
    secondary            = QwenPurple,
    onSecondary          = Void,
    secondaryContainer   = QwenPurpleDim,
    onSecondaryContainer = QwenPurple,
    tertiary             = ThinkAmber,
    tertiaryContainer    = ThinkAmberDim,
    background           = Void,
    onBackground         = TextPrimary,
    surface              = Surface,
    onSurface            = TextPrimary,
    surfaceVariant       = Elevated,
    onSurfaceVariant     = TextSecondary,
    outline              = Border,
    error                = StatusError,
)

@Composable
fun YadraQwenTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Void.toArgb()
            window.navigationBarColor = Void.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = YadraDarkScheme,
        typography  = NeuralTypography,
        content     = content
    )
}