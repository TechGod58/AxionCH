package com.axionch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.axionch.app.data.api.AppClientConfigStore
import com.axionch.app.ui.AxionCHApp
import com.axionch.app.ui.theme.AxionCHTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppClientConfigStore.initialize(applicationContext)
        setContent {
            AxionCHTheme {
                AxionCHApp()
            }
        }
    }
}
