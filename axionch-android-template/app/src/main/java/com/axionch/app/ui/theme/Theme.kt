package com.axionch.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AxionColorScheme = darkColorScheme()

@Composable
fun AxionCHTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AxionColorScheme,
        content = content
    )
}
