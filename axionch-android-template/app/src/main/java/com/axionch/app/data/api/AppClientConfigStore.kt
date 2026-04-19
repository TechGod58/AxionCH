package com.axionch.app.data.api

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

data class AppClientConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val userEmail: String = "",
    val apiKey: String = ""
) {
    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8010/"
    }
}

object AppClientConfigStore {
    private const val PREFS_NAME = "axionch_client_config"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_API_KEY = "api_key"

    private val initialized = AtomicBoolean(false)
    private val _config = MutableStateFlow(AppClientConfig())
    val config: StateFlow<AppClientConfig> = _config

    fun initialize(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            _config.value = readFromPrefs(context)
        }
    }

    fun current(): AppClientConfig = _config.value

    fun update(
        context: Context,
        baseUrl: String,
        userEmail: String,
        apiKey: String
    ) {
        val next = AppClientConfig(
            baseUrl = normalizeBaseUrl(baseUrl),
            userEmail = userEmail.trim(),
            apiKey = apiKey.trim()
        )
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_BASE_URL, next.baseUrl)
            .putString(KEY_USER_EMAIL, next.userEmail)
            .putString(KEY_API_KEY, next.apiKey)
            .apply()
        _config.value = next
    }

    private fun readFromPrefs(context: Context): AppClientConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppClientConfig(
            baseUrl = normalizeBaseUrl(prefs.getString(KEY_BASE_URL, AppClientConfig.DEFAULT_BASE_URL).orEmpty()),
            userEmail = prefs.getString(KEY_USER_EMAIL, "").orEmpty().trim(),
            apiKey = prefs.getString(KEY_API_KEY, "").orEmpty().trim()
        )
    }

    private fun normalizeBaseUrl(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return AppClientConfig.DEFAULT_BASE_URL
        }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
