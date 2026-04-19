package com.axionch.app.ui.screens.results

import com.axionch.shared.api.PostResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ResultsStore {
    private val _liveResult = MutableStateFlow<PostResponse?>(null)
    val liveResult = _liveResult.asStateFlow()

    private val _liveError = MutableStateFlow<String?>(null)
    val liveError = _liveError.asStateFlow()

    private val _dryRunResult = MutableStateFlow<PostResponse?>(null)
    val dryRunResult = _dryRunResult.asStateFlow()

    private val _dryRunError = MutableStateFlow<String?>(null)
    val dryRunError = _dryRunError.asStateFlow()

    fun updateLiveResult(result: PostResponse?) {
        _liveResult.value = result
        _liveError.value = null
    }

    fun updateLiveError(message: String?) {
        _liveError.value = message
        _liveResult.value = null
    }

    fun updateDryRunResult(result: PostResponse?) {
        _dryRunResult.value = result
        _dryRunError.value = null
    }

    fun updateDryRunError(message: String?) {
        _dryRunError.value = message
        _dryRunResult.value = null
    }

    fun clearLive() {
        _liveResult.value = null
        _liveError.value = null
    }

    fun clearDryRun() {
        _dryRunResult.value = null
        _dryRunError.value = null
    }

    fun clearAll() {
        clearLive()
        clearDryRun()
    }
}
