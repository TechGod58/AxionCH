package com.axionch.app.ui.screens.composer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axionch.app.data.api.AppClientConfigStore
import com.axionch.app.data.repo.AxionRepository
import com.axionch.app.ui.screens.results.ResultsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ComposerUiState(
    val body: String = "Hello from AxionCH",
    val imageUrl: String = "https://example.com/image.jpg",
    val selectedAccountIds: List<Int> = emptyList(),
    val isPublishing: Boolean = false,
    val isDryRunning: Boolean = false,
    val message: String = ""
)

class ComposerViewModel : ViewModel() {
    private val repository = AxionRepository()

    private val _uiState = MutableStateFlow(ComposerUiState())
    val uiState: StateFlow<ComposerUiState> = _uiState

    fun updateBody(value: String) {
        _uiState.value = _uiState.value.copy(body = value)
    }

    fun updateImageUrl(value: String) {
        _uiState.value = _uiState.value.copy(imageUrl = value)
    }

    fun loadAccountsForSelection() {
        viewModelScope.launch {
            runCatching { repository.getAccounts() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        selectedAccountIds = it.map { item -> item.id }
                    )
                }
        }
    }

    fun publish(onComplete: () -> Unit) {
        val email = AppClientConfigStore.current().userEmail
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Set your user email in Dashboard config before publishing.")
            return
        }
        _uiState.value = _uiState.value.copy(isPublishing = true, isDryRunning = false)
        ResultsStore.clearLive()
        
        viewModelScope.launch {
            try {
                val result = repository.createPost(
                    email = email,
                    body = _uiState.value.body,
                    imageUrl = _uiState.value.imageUrl.takeIf { it.isNotBlank() },
                    accountIds = _uiState.value.selectedAccountIds
                )
                ResultsStore.updateLiveResult(result)
                _uiState.value = _uiState.value.copy(message = "Live publish request completed.")
            } catch (e: Exception) {
                ResultsStore.updateLiveError(e.message ?: "Unknown error")
                _uiState.value = _uiState.value.copy(message = "Live publish failed: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isPublishing = false)
                onComplete()
            }
        }
    }

    fun dryRun(onComplete: () -> Unit) {
        val email = AppClientConfigStore.current().userEmail
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Set your user email in Dashboard config before dry run.")
            return
        }
        _uiState.value = _uiState.value.copy(isPublishing = false, isDryRunning = true)
        ResultsStore.clearDryRun()

        viewModelScope.launch {
            try {
                val result = repository.dryRunPost(
                    email = email,
                    body = _uiState.value.body,
                    imageUrl = _uiState.value.imageUrl.takeIf { it.isNotBlank() },
                    accountIds = _uiState.value.selectedAccountIds
                )
                ResultsStore.updateDryRunResult(result)
                _uiState.value = _uiState.value.copy(message = "Dry run completed.")
            } catch (e: Exception) {
                ResultsStore.updateDryRunError(e.message ?: "Unknown error")
                _uiState.value = _uiState.value.copy(message = "Dry run failed: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isDryRunning = false)
                onComplete()
            }
        }
    }
}
