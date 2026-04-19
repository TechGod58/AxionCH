package com.axionch.app.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axionch.app.data.repo.AxionRepository
import com.axionch.shared.api.DeadLetterItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ResultsUiState(
    val deadLetters: List<DeadLetterItem> = emptyList(),
    val selectedDeadLetterId: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val isRequeueing: Boolean = false,
)

class ResultsViewModel : ViewModel() {
    private val repository = AxionRepository()

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState

    init {
        refreshDeadLetters()
    }

    fun refreshDeadLetters() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { repository.getDeadLetters(limit = 50) }
                .onSuccess { response ->
                    val previousSelected = _uiState.value.selectedDeadLetterId
                    val selectedId = if (response.items.any { it.id == previousSelected }) {
                        previousSelected
                    } else {
                        response.items.firstOrNull()?.id
                    }
                    _uiState.value = _uiState.value.copy(
                        deadLetters = response.items,
                        selectedDeadLetterId = selectedId,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load dead letters"
                    )
                }
        }
    }

    fun selectDeadLetter(deadLetterId: Int) {
        _uiState.value = _uiState.value.copy(selectedDeadLetterId = deadLetterId)
    }

    fun requeueSelectedDeadLetter() {
        val selectedId = _uiState.value.selectedDeadLetterId ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = "Select a dead-letter entry first")
            return
        }

        _uiState.value = _uiState.value.copy(isRequeueing = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { repository.requeueDeadLetter(selectedId) }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isRequeueing = false,
                        actionMessage = "Requeued dead letter ${result.dead_letter_id} as job ${result.new_job_id}",
                        errorMessage = null
                    )
                    refreshDeadLetters()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isRequeueing = false,
                        errorMessage = error.message ?: "Failed to requeue dead letter"
                    )
                }
        }
    }

    fun setActionMessage(message: String?) {
        _uiState.value = _uiState.value.copy(actionMessage = message)
    }
}
