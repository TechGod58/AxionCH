package com.axionch.app.ui.screens.dryrun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axionch.app.data.repo.AxionRepository
import com.axionch.shared.api.DryRunHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DryRunHistoryUiState(
    val isLoading: Boolean = false,
    val items: List<DryRunHistoryItem> = emptyList(),
    val errorText: String? = null,
    val infoText: String = "",
    val selectedPlatform: String? = null,
    val limit: Int = 50
)

class DryRunHistoryViewModel : ViewModel() {
    private val repository = AxionRepository()

    private val _uiState = MutableStateFlow(DryRunHistoryUiState())
    val uiState: StateFlow<DryRunHistoryUiState> = _uiState

    fun setPlatformFilter(platform: String?) {
        _uiState.value = _uiState.value.copy(selectedPlatform = platform)
        loadHistory()
    }

    fun loadHistory() {
        val currentState = _uiState.value
        val platform = currentState.selectedPlatform
        val limit = currentState.limit

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorText = null)
            runCatching {
                repository.getDryRunHistory(
                    limit = limit,
                    platform = platform,
                    successOnly = null
                )
            }
                .onSuccess { response ->
                    val filterLabel = platform ?: "all"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = response.items,
                        errorText = null,
                        infoText = "History entries: ${response.total_returned} (filter=$filterLabel, limit=${response.limit})"
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorText = "Failed loading dry-run history: ${it.message}",
                        infoText = ""
                    )
                }
        }
    }

    fun clearHistory() {
        val platform = _uiState.value.selectedPlatform
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorText = null)
            runCatching { repository.clearDryRunHistory(platform = platform) }
                .onSuccess { clear ->
                    _uiState.value = _uiState.value.copy(
                        infoText = "Cleared ${clear.cleared_count} dry-run item(s), remaining=${clear.remaining_count}"
                    )
                    loadHistory()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorText = "Failed clearing history: ${it.message}",
                        infoText = ""
                    )
                }
        }
    }
}
