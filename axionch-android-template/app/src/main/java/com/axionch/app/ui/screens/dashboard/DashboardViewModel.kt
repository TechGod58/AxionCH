package com.axionch.app.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axionch.app.data.api.AppClientConfigStore
import com.axionch.app.data.repo.AxionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val healthText: String = "Checking...",
    val xConfigText: String = "Checking...",
    val linkedinConfigText: String = "Checking...",
    val instagramConfigText: String = "Checking...",
    val xMissingFieldsText: String = "",
    val linkedinMissingFieldsText: String = "",
    val instagramMissingFieldsText: String = "",
    val xCredentialCheckText: String = "Last check: never",
    val linkedinCredentialCheckText: String = "Last check: never",
    val instagramCredentialCheckText: String = "Last check: never",
    val checkSummaryText: String = "",
    val clientBaseUrl: String = "",
    val clientUserEmail: String = "",
    val clientApiKey: String = "",
    val clientConfigMessage: String = ""
)

class DashboardViewModel : ViewModel() {
    private val repository = AxionRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    fun loadDashboard() {
        loadClientConfig()
        loadHealth()
        loadConfigStatus()
    }

    fun updateClientBaseUrl(value: String) {
        _uiState.value = _uiState.value.copy(clientBaseUrl = value, clientConfigMessage = "")
    }

    fun updateClientUserEmail(value: String) {
        _uiState.value = _uiState.value.copy(clientUserEmail = value, clientConfigMessage = "")
    }

    fun updateClientApiKey(value: String) {
        _uiState.value = _uiState.value.copy(clientApiKey = value, clientConfigMessage = "")
    }

    fun saveClientConfig(context: Context) {
        val state = _uiState.value
        AppClientConfigStore.update(
            context = context,
            baseUrl = state.clientBaseUrl,
            userEmail = state.clientUserEmail,
            apiKey = state.clientApiKey
        )
        loadClientConfig()
        _uiState.value = _uiState.value.copy(clientConfigMessage = "Client config saved.")
        loadHealth()
        loadConfigStatus()
    }

    fun loadHealth() {
        viewModelScope.launch {
            runCatching { repository.getHealth() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        healthText = "${it.status} (${it.service})"
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        healthText = "Backend unreachable: ${it.message}"
                    )
                }
        }
    }

    fun loadConfigStatus() {
        viewModelScope.launch {
            runCatching { repository.getConfigStatus() }
                .onSuccess { config ->
                    _uiState.value = _uiState.value.copy(
                        xConfigText = formatPlatformStatus("X", config.x.mode, config.x.configured),
                        linkedinConfigText = formatPlatformStatus("LinkedIn", config.linkedin.mode, config.linkedin.configured),
                        instagramConfigText = formatPlatformStatus("Instagram", config.instagram.mode, config.instagram.configured),
                        xMissingFieldsText = formatMissingFields(config.x.required_fields, config.x.configured_fields),
                        linkedinMissingFieldsText = formatMissingFields(config.linkedin.required_fields, config.linkedin.configured_fields),
                        instagramMissingFieldsText = formatMissingFields(config.instagram.required_fields, config.instagram.configured_fields),
                        xCredentialCheckText = formatCredentialCheck(config.x.last_checked_at, config.x.last_check_success, config.x.last_check_error, config.x.check_count),
                        linkedinCredentialCheckText = formatCredentialCheck(config.linkedin.last_checked_at, config.linkedin.last_check_success, config.linkedin.last_check_error, config.linkedin.check_count),
                        instagramCredentialCheckText = formatCredentialCheck(config.instagram.last_checked_at, config.instagram.last_check_success, config.instagram.last_check_error, config.instagram.check_count),
                    )
                }
                .onFailure {
                    val message = "Config status unavailable: ${it.message}"
                    _uiState.value = _uiState.value.copy(
                        xConfigText = message,
                        linkedinConfigText = message,
                        instagramConfigText = message,
                        xMissingFieldsText = "",
                        linkedinMissingFieldsText = "",
                        instagramMissingFieldsText = "",
                        xCredentialCheckText = "Last check: unknown",
                        linkedinCredentialCheckText = "Last check: unknown",
                        instagramCredentialCheckText = "Last check: unknown",
                    )
                }
        }
    }

    fun runCredentialChecks() {
        viewModelScope.launch {
            runCatching { repository.runConfigCheck() }
                .onSuccess { response ->
                    val successCount = response.results.count { it.success }
                    val totalCount = response.results.size
                    _uiState.value = _uiState.value.copy(
                        checkSummaryText = "Credential check at ${response.checked_at}: $successCount/$totalCount passed"
                    )
                    loadConfigStatus()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        checkSummaryText = "Credential check failed: ${it.message}"
                    )
                }
        }
    }

    private fun loadClientConfig() {
        val config = AppClientConfigStore.current()
        _uiState.value = _uiState.value.copy(
            clientBaseUrl = config.baseUrl,
            clientUserEmail = config.userEmail,
            clientApiKey = config.apiKey
        )
    }

    private fun formatPlatformStatus(name: String, mode: String, configured: Boolean): String {
        val status = if (configured) "configured" else "missing keys"
        return "$name: $status ($mode)"
    }

    private fun formatMissingFields(requiredFields: List<String>, configuredFields: List<String>): String {
        val missing = requiredFields.filterNot { it in configuredFields }
        return if (missing.isEmpty()) {
            "Missing fields: none"
        } else {
            "Missing fields: ${missing.joinToString()}"
        }
    }

    private fun formatCredentialCheck(
        lastCheckedAt: String?,
        success: Boolean?,
        error: String?,
        checkCount: Int
    ): String {
        val checkState = when (success) {
            true -> "success"
            false -> "failed"
            null -> "never"
        }
        val base = if (lastCheckedAt.isNullOrBlank()) {
            "Last check: never"
        } else {
            "Last check: $lastCheckedAt ($checkState)"
        }
        val withCount = "$base | count=$checkCount"
        return if (error.isNullOrBlank()) withCount else "$withCount | error=$error"
    }
}
