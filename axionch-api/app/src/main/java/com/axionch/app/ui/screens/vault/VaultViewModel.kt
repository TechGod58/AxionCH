package com.axionch.app.ui.screens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axionch.app.data.api.AppClientConfigStore
import com.axionch.app.data.repo.AxionRepository
import com.axionch.shared.api.VaultEntryResponse
import com.axionch.shared.api.VaultEntrySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VaultUiState(
    val entries: List<VaultEntrySummary> = emptyList(),
    val selectedEntry: VaultEntryResponse? = null,
    val serviceName: String = "",
    val username: String = "",
    val password: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
    val message: String = ""
)

class VaultViewModel : ViewModel() {
    private val repository = AxionRepository()

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState

    fun updateServiceName(value: String) {
        _uiState.value = _uiState.value.copy(serviceName = value)
    }

    fun updateUsername(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun loadVault() {
        val email = AppClientConfigStore.current().userEmail
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Set your user email in Dashboard config first.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "")
            runCatching { repository.listVaultEntries(email) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        entries = it.entries,
                        isLoading = false,
                        message = "Loaded ${it.total_returned} vault entries."
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Load vault failed: ${it.message}"
                    )
                }
        }
    }

    fun createEntry() {
        val email = AppClientConfigStore.current().userEmail
        val state = _uiState.value
        if (email.isBlank()) {
            _uiState.value = state.copy(message = "Set your user email in Dashboard config first.")
            return
        }
        if (state.serviceName.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(message = "Service, username, and password are required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "")
            runCatching {
                repository.createVaultEntry(
                    userEmail = email,
                    serviceName = state.serviceName,
                    username = state.username,
                    password = state.password,
                    notes = state.notes.takeIf { it.isNotBlank() }
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    serviceName = "",
                    username = "",
                    password = "",
                    notes = "",
                    message = "Vault entry saved.",
                    selectedEntry = it
                )
                loadVault()
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Create vault entry failed: ${it.message}"
                )
            }
        }
    }

    fun selectEntry(entryId: Int) {
        val email = AppClientConfigStore.current().userEmail
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Set your user email in Dashboard config first.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "")
            runCatching { repository.getVaultEntry(entryId = entryId, userEmail = email) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        selectedEntry = it,
                        isLoading = false,
                        message = "Loaded vault entry details."
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Load vault detail failed: ${it.message}"
                    )
                }
        }
    }

    fun deleteEntry(entryId: Int) {
        val email = AppClientConfigStore.current().userEmail
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Set your user email in Dashboard config first.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "")
            runCatching { repository.deleteVaultEntry(entryId = entryId, userEmail = email) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedEntry = if (_uiState.value.selectedEntry?.id == entryId) null else _uiState.value.selectedEntry,
                        message = "Vault entry deleted."
                    )
                    loadVault()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Delete vault entry failed: ${it.message}"
                    )
                }
        }
    }
}
