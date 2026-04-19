package com.axionch.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.axionch.app.ui.navigation.AxionNavHost

@Composable
fun AxionCHApp() {
    val navController = rememberNavController()
    AxionNavHost(navController = navController)
}