package com.tecnozoni.reproductor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Esquema fijo negro/blanco. Sin color dinámico ni tema claro: la app es siempre oscura.
private val MonoColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = White,
    onSecondary = Black,
    tertiary = Gray,
    background = Black,
    onBackground = OffWhite,
    surface = Black,
    onSurface = OffWhite,
    surfaceVariant = Surface2,
    onSurfaceVariant = Gray,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    outline = Outline,
    outlineVariant = Outline,
)

@Composable
fun ReproductorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonoColorScheme,
        typography = Typography,
        content = content,
    )
}
