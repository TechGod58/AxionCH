package com.axionch.app.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axionch.app.data.api.AppClientConfigStore
import com.axionch.app.data.repo.AxionRepository
import com.axionch.shared.api.AccountResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AccountsUiState(
    val accounts: List<AccountResponse> = emptyList(),
    val isLoading: Boolean = false,
    val message: String = ""
)

class AccountsViewModel : ViewModel() {
    private val repository = AxionRepository()

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching { repository.getAccounts() }
                .onSuccess { _uiState.value = AccountsUiState(accounts = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false) }
        }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch {
            runCatching { repository.deleteAccount(id) }
                .onSuccess { loadAccounts() }
        }
    }

    fun createSeedAccounts() {
        viewModelScope.launch {
            val email = AppClientConfigStore.current().userEmail
            if (email.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    message = "Set your user email in Dashboard config before creating accounts."
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching {
                repository.createMockAccount(email, "x", "@axion_x")
                repository.createMockAccount(email, "linkedin", "axion-linkedin")
                repository.createMockAccount(email, "instagram", "@axion_ig")
            }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(message = "Mock accounts created.")
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(message = "Create mock accounts failed: ${it.message}")
                }
            loadAccounts()
        }
    }
}
