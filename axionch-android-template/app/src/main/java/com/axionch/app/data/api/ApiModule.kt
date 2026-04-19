package com.axionch.app.data.api

import com.axionch.app.BuildConfig
import com.axionch.shared.api.AxionApiClient

object ApiModule {
    @Volatile
    private var cachedConfig: AppClientConfig? = null

    @Volatile
    private var cachedClient: AxionApiClient? = null

    fun api(): AxionApiClient {
        val currentConfig = AppClientConfigStore.current()
        val existingClient = cachedClient
        val existingConfig = cachedConfig
        if (existingClient != null && existingConfig == currentConfig) {
            return existingClient
        }
        return synchronized(this) {
            if (cachedClient != null && cachedConfig == currentConfig) {
                cachedClient!!
            } else {
                AxionApiClient(
                    baseUrl = currentConfig.baseUrl,
                    apiKey = currentConfig.apiKey.ifBlank { null },
                    userEmail = currentConfig.userEmail.ifBlank { null },
                    enableHttpLogging = BuildConfig.DEBUG
                ).also { client ->
                    cachedClient = client
                    cachedConfig = currentConfig
                }
            }
        }
    }
}
