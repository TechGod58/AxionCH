package com.axionch.app.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

data class AppClientConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val userEmail: String = "",
    val apiKey: String = "",
    val skin: String = DEFAULT_SKIN
) {
    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8010/"
        const val DEFAULT_SKIN = "blue"
    }
}

object AppClientConfigStore {
    private const val PREFS_NAME = "axionch_client_config"
    private const val SECURE_PREFS_NAME = "axionch_client_config_secure"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_SKIN = "skin"

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
        apiKey: String,
        skin: String = _config.value.skin
    ) {
        val next = AppClientConfig(
            baseUrl = normalizeBaseUrl(baseUrl),
            userEmail = userEmail.trim(),
            apiKey = apiKey.trim(),
            skin = skin.trim().ifBlank { AppClientConfig.DEFAULT_SKIN }
        )
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val securePrefs = securePrefsOrNull(context)
        prefs.edit()
            .putString(KEY_BASE_URL, next.baseUrl)
            .putString(KEY_USER_EMAIL, next.userEmail)
            .putString(KEY_SKIN, next.skin)
            .apply()
        if (securePrefs != null) {
            securePrefs.edit().putString(KEY_API_KEY, next.apiKey).apply()
            prefs.edit().remove(KEY_API_KEY).apply()
        } else {
            prefs.edit().putString(KEY_API_KEY, next.apiKey).apply()
        }
        _config.value = next
    }

    fun updateSkin(context: Context, skin: String) {
        val current = _config.value
        update(
            context = context,
            baseUrl = current.baseUrl,
            userEmail = current.userEmail,
            apiKey = current.apiKey,
            skin = skin
        )
    }

    private fun readFromPrefs(context: Context): AppClientConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val securePrefs = securePrefsOrNull(context)
        val secureApiKey = securePrefs?.getString(KEY_API_KEY, null)?.trim().orEmpty()
        val fallbackApiKey = prefs.getString(KEY_API_KEY, "").orEmpty().trim()
        return AppClientConfig(
            baseUrl = normalizeBaseUrl(prefs.getString(KEY_BASE_URL, AppClientConfig.DEFAULT_BASE_URL).orEmpty()),
            userEmail = prefs.getString(KEY_USER_EMAIL, "").orEmpty().trim(),
            apiKey = if (secureApiKey.isNotBlank()) secureApiKey else fallbackApiKey,
            skin = prefs.getString(KEY_SKIN, AppClientConfig.DEFAULT_SKIN).orEmpty()
        )
    }

    private fun securePrefsOrNull(context: Context): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeBaseUrl(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return AppClientConfig.DEFAULT_BASE_URL
        }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
