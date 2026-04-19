package com.axionch.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.rememberNavController
import com.axionch.app.R
import com.axionch.app.ui.navigation.AxionNavHost
import com.axionch.app.ui.theme.LocalSkinPalette

@Composable
fun AxionCHApp() {
    val navController = rememberNavController()
    val skin = LocalSkinPalette.current
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.axion_theme_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.18f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            skin.backdropStart,
                            skin.backdropEnd
                        )
                    )
                )
        )
        AxionNavHost(navController = navController)
    }
}
