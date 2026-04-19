package com.axionch.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.axionch.app.data.api.AppClientConfigStore

data class SkinPalette(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onDark: Color,
    val backdropStart: Color,
    val backdropEnd: Color
)

val SkinPalettes = listOf(
    SkinPalette(
        id = "blue",
        label = "Blue",
        primary = Color(0xFF43B3FF),
        secondary = Color(0xFF86D8FF),
        background = Color(0xFF07101E),
        surface = Color(0xFF112138),
        surfaceVariant = Color(0xFF1B3151),
        onDark = Color(0xFFEAF6FF),
        backdropStart = Color(0xD4040E1D),
        backdropEnd = Color(0xE1082A52)
    ),
    SkinPalette(
        id = "red",
        label = "Red",
        primary = Color(0xFFFF4D4D),
        secondary = Color(0xFFFF9A9A),
        background = Color(0xFF1A0808),
        surface = Color(0xFF2A1010),
        surfaceVariant = Color(0xFF472020),
        onDark = Color(0xFFFFEEEE),
        backdropStart = Color(0xD4180606),
        backdropEnd = Color(0xE1461212)
    ),
    SkinPalette(
        id = "green",
        label = "Green",
        primary = Color(0xFF39D98A),
        secondary = Color(0xFF89F5C0),
        background = Color(0xFF06160F),
        surface = Color(0xFF0F271B),
        surfaceVariant = Color(0xFF1D422F),
        onDark = Color(0xFFE8FFF4),
        backdropStart = Color(0xD406120C),
        backdropEnd = Color(0xE10A3A25)
    ),
    SkinPalette(
        id = "pink",
        label = "Pink",
        primary = Color(0xFFFF5FB8),
        secondary = Color(0xFFFFAAD8),
        background = Color(0xFF1A0813),
        surface = Color(0xFF291122),
        surfaceVariant = Color(0xFF472039),
        onDark = Color(0xFFFFECF7),
        backdropStart = Color(0xD4120710),
        backdropEnd = Color(0xE13D1332)
    ),
    SkinPalette(
        id = "purple",
        label = "Purple",
        primary = Color(0xFFB06BFF),
        secondary = Color(0xFFD6AFFF),
        background = Color(0xFF12091E),
        surface = Color(0xFF201235),
        surfaceVariant = Color(0xFF372157),
        onDark = Color(0xFFF4ECFF),
        backdropStart = Color(0xD40E0819),
        backdropEnd = Color(0xE12A1450)
    ),
    SkinPalette(
        id = "gold",
        label = "Gold",
        primary = Color(0xFFFFC74A),
        secondary = Color(0xFFFFE09C),
        background = Color(0xFF1D1404),
        surface = Color(0xFF322208),
        surfaceVariant = Color(0xFF574017),
        onDark = Color(0xFFFFF7E2),
        backdropStart = Color(0xD4181103),
        backdropEnd = Color(0xE1463408)
    )
)

fun skinPaletteById(id: String?): SkinPalette {
    return SkinPalettes.firstOrNull { it.id == id } ?: SkinPalettes.first()
}

val LocalSkinPalette = staticCompositionLocalOf { SkinPalettes.first() }

@Composable
fun AxionCHTheme(content: @Composable () -> Unit) {
    val config by AppClientConfigStore.config.collectAsState()
    val palette = skinPaletteById(config.skin)
    val colorScheme = darkColorScheme(
        primary = palette.primary,
        secondary = palette.secondary,
        background = palette.background,
        surface = palette.surface,
        surfaceVariant = palette.surfaceVariant,
        onPrimary = palette.background,
        onSecondary = palette.background,
        onBackground = palette.onDark,
        onSurface = palette.onDark
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalSkinPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
