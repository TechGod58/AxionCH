package com.axionch.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.axionch.shared.api.AccountResponse
import com.axionch.shared.api.AxionApiClient
import com.axionch.shared.api.CreateAccountRequest
import com.axionch.shared.api.CreatePostRequest
import com.axionch.shared.api.DeadLetterItem
import com.axionch.shared.api.DryRunHistoryItem
import com.axionch.shared.api.PlatformConfigStatus
import com.axionch.shared.api.PostResponse
import com.axionch.shared.api.PublishResultItem
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private data class DesktopUiState(
    val healthText: String = "Not checked",
    val xConfigText: String = "Not checked",
    val linkedinConfigText: String = "Not checked",
    val instagramConfigText: String = "Not checked",
    val xMissingFieldsText: String = "",
    val linkedinMissingFieldsText: String = "",
    val instagramMissingFieldsText: String = "",
    val xCredentialCheckText: String = "Last check: never",
    val linkedinCredentialCheckText: String = "Last check: never",
    val instagramCredentialCheckText: String = "Last check: never",
    val configCheckSummaryText: String = "",
    val accounts: List<AccountResponse> = emptyList(),
    val selectedAccountIds: List<Int> = emptyList(),
    val body: String = "Hello from AxionCH desktop",
    val imageUrl: String = "https://example.com/image.jpg",
    val livePostResult: PostResponse? = null,
    val dryRunPreview: PostResponse? = null,
    val dryRunHistory: List<DryRunHistoryItem> = emptyList(),
    val selectedDryRunPlatform: String? = null,
    val dryRunHistoryLimit: Int = 50,
    val dryRunHistoryInfoText: String = "",
    val deadLetters: List<DeadLetterItem> = emptyList(),
    val selectedDeadLetterId: Int? = null,
    val deadLetterInfoText: String = "",
    val deadLetterActionText: String = "",
    val errorText: String? = null,
    val isWorking: Boolean = false
)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AxionCH Desktop"
    ) {
        MaterialTheme {
            AxionDesktopApp()
        }
    }
}

@Composable
private fun AxionDesktopApp() {
    val api = remember { AxionApiClient.fromEnvironment(defaultBaseUrl = "http://127.0.0.1:8010/") }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(DesktopUiState()) }

    fun setError(message: String?) {
        state = state.copy(errorText = message)
    }

    fun refreshHealth() {
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.health() }
                .onSuccess {
                    state = state.copy(
                        healthText = "${it.status} (${it.service})",
                        isWorking = false,
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Health check failed: ${it.message}"
                    )
                }
        }
    }

    fun refreshConfigStatus() {
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.getConfigStatus() }
                .onSuccess {
                    state = state.copy(
                        xConfigText = formatPlatformStatus("X", it.x.mode, it.x.configured),
                        linkedinConfigText = formatPlatformStatus("LinkedIn", it.linkedin.mode, it.linkedin.configured),
                        instagramConfigText = formatPlatformStatus("Instagram", it.instagram.mode, it.instagram.configured),
                        xMissingFieldsText = formatMissingFields(it.x.required_fields, it.x.configured_fields),
                        linkedinMissingFieldsText = formatMissingFields(it.linkedin.required_fields, it.linkedin.configured_fields),
                        instagramMissingFieldsText = formatMissingFields(it.instagram.required_fields, it.instagram.configured_fields),
                        xCredentialCheckText = formatCredentialCheck(it.x),
                        linkedinCredentialCheckText = formatCredentialCheck(it.linkedin),
                        instagramCredentialCheckText = formatCredentialCheck(it.instagram),
                        isWorking = false,
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Config status failed: ${it.message}"
                    )
                }
        }
    }

    fun runCredentialCheck() {
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.runConfigCheck() }
                .onSuccess {
                    val passed = it.results.count { item -> item.success }
                    val total = it.results.size
                    state = state.copy(
                        isWorking = false,
                        errorText = null,
                        configCheckSummaryText = "Credential check at ${it.checked_at}: $passed/$total passed"
                    )
                    refreshConfigStatus()
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Credential check failed: ${it.message}"
                    )
                }
        }
    }

    fun refreshAccounts() {
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.getAccounts() }
                .onSuccess { accounts ->
                    state = state.copy(
                        accounts = accounts,
                        selectedAccountIds = accounts.map { it.id },
                        isWorking = false,
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Loading accounts failed: ${it.message}"
                    )
                }
        }
    }

    fun createMockAccounts() {
        val email = "you@example.com"
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching {
                api.createAccount(CreateAccountRequest(email, "x", "@axion_x", "mock-token-x"))
                api.createAccount(CreateAccountRequest(email, "linkedin", "axion-linkedin", "mock-token-linkedin"))
                api.createAccount(CreateAccountRequest(email, "instagram", "@axion_ig", "mock-token-instagram"))
            }.onSuccess {
                setError(null)
                refreshAccounts()
            }.onFailure {
                state = state.copy(
                    isWorking = false,
                    errorText = "Creating mock accounts failed: ${it.message}"
                )
            }
        }
    }

    fun refreshDryRunHistory() {
        val platform = state.selectedDryRunPlatform
        val limit = state.dryRunHistoryLimit
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.getDryRunHistory(limit = limit, platform = platform) }
                .onSuccess { history ->
                    val filterLabel = platform ?: "all"
                    state = state.copy(
                        dryRunHistory = history.items,
                        dryRunHistoryInfoText = "History entries: ${history.total_returned} (filter=$filterLabel, limit=${history.limit})",
                        isWorking = false,
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Loading dry-run history failed: ${it.message}"
                    )
                }
        }
    }

    fun setDryRunPlatform(platform: String?) {
        state = state.copy(selectedDryRunPlatform = platform)
        refreshDryRunHistory()
    }

    fun clearDryRunHistory() {
        val platform = state.selectedDryRunPlatform
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.clearDryRunHistory(platform = platform) }
                .onSuccess { clear ->
                    state = state.copy(
                        isWorking = false,
                        errorText = null,
                        dryRunHistoryInfoText = "Cleared ${clear.cleared_count} item(s), remaining=${clear.remaining_count}"
                    )
                    refreshDryRunHistory()
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Clearing dry-run history failed: ${it.message}"
                    )
                }
        }
    }

    fun refreshDeadLetters() {
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.getDeadLetters(limit = 50) }
                .onSuccess { response ->
                    val selectedId = state.selectedDeadLetterId
                    val selectedStillExists = response.items.any { it.id == selectedId }
                    state = state.copy(
                        deadLetters = response.items,
                        selectedDeadLetterId = if (selectedStillExists) selectedId else response.items.firstOrNull()?.id,
                        deadLetterInfoText = "Dead letters loaded: ${response.total_returned} (limit=${response.limit})",
                        isWorking = false,
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Loading dead letters failed: ${it.message}"
                    )
                }
        }
    }

    fun selectDeadLetter(deadLetterId: Int) {
        state = state.copy(selectedDeadLetterId = deadLetterId)
    }

    fun requeueSelectedDeadLetter() {
        val selectedId = state.selectedDeadLetterId
        if (selectedId == null) {
            setError("Select a dead-letter entry before requeue.")
            return
        }

        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.requeueDeadLetter(selectedId) }
                .onSuccess { response ->
                    state = state.copy(
                        isWorking = false,
                        errorText = null,
                        deadLetterActionText = "Requeued dead letter ${response.dead_letter_id} as job ${response.new_job_id}"
                    )
                    refreshDeadLetters()
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Requeue failed: ${it.message}"
                    )
                }
        }
    }

    fun publish() {
        if (state.selectedAccountIds.isEmpty()) {
            setError("No selected account IDs. Click Load Account IDs first.")
            return
        }

        val request = CreatePostRequest(
            user_email = "you@example.com",
            body = state.body,
            image_url = state.imageUrl.takeIf { it.isNotBlank() },
            account_ids = state.selectedAccountIds
        )

        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.createPost(request) }
                .onSuccess {
                    state = state.copy(
                        livePostResult = it,
                        isWorking = false,
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Publish failed: ${it.message}"
                    )
                }
        }
    }

    fun dryRun() {
        if (state.selectedAccountIds.isEmpty()) {
            setError("No selected account IDs. Click Load Account IDs first.")
            return
        }

        val request = CreatePostRequest(
            user_email = "you@example.com",
            body = state.body,
            image_url = state.imageUrl.takeIf { it.isNotBlank() },
            account_ids = state.selectedAccountIds
        )

        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.dryRunPost(request) }
                .onSuccess { result ->
                    runCatching {
                        api.getDryRunHistory(
                            limit = state.dryRunHistoryLimit,
                            platform = state.selectedDryRunPlatform
                        )
                    }
                        .onSuccess { history ->
                            state = state.copy(
                                dryRunPreview = result,
                                dryRunHistory = history.items,
                                dryRunHistoryInfoText = "History entries: ${history.total_returned} (filter=${state.selectedDryRunPlatform ?: "all"}, limit=${history.limit})",
                                isWorking = false,
                                errorText = null
                            )
                        }
                        .onFailure {
                            state = state.copy(
                                dryRunPreview = result,
                                isWorking = false,
                                errorText = "Dry run succeeded, but history refresh failed: ${it.message}"
                            )
                        }
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Dry run failed: ${it.message}"
                    )
                }
        }
    }

    LaunchedEffect(Unit) {
        refreshHealth()
        refreshConfigStatus()
        refreshDryRunHistory()
        refreshDeadLetters()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("AxionCH Desktop", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Backend")
                    Text("Health: ${state.healthText}")
                    Text(state.xConfigText)
                    Text(state.xMissingFieldsText, style = MaterialTheme.typography.bodySmall)
                    Text(state.xCredentialCheckText, style = MaterialTheme.typography.bodySmall)
                    Text(state.linkedinConfigText)
                    Text(state.linkedinMissingFieldsText, style = MaterialTheme.typography.bodySmall)
                    Text(state.linkedinCredentialCheckText, style = MaterialTheme.typography.bodySmall)
                    Text(state.instagramConfigText)
                    Text(state.instagramMissingFieldsText, style = MaterialTheme.typography.bodySmall)
                    Text(state.instagramCredentialCheckText, style = MaterialTheme.typography.bodySmall)
                    if (state.configCheckSummaryText.isNotBlank()) {
                        Text(state.configCheckSummaryText, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::refreshHealth, enabled = !state.isWorking) { Text("Check Health") }
                        Button(onClick = ::refreshConfigStatus, enabled = !state.isWorking) { Text("Refresh Config") }
                        Button(onClick = ::runCredentialCheck, enabled = !state.isWorking) { Text("Run Credential Check") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::refreshAccounts, enabled = !state.isWorking) { Text("Load Account IDs") }
                        Button(onClick = ::createMockAccounts, enabled = !state.isWorking) { Text("Create Mock Accounts") }
                        Button(onClick = ::refreshDryRunHistory, enabled = !state.isWorking) { Text("Refresh Dry-Run History") }
                        Button(onClick = ::clearDryRunHistory, enabled = !state.isWorking) { Text("Clear Filtered History") }
                        Button(onClick = ::refreshDeadLetters, enabled = !state.isWorking) { Text("Load Dead Letters") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { setDryRunPlatform(null) }, enabled = !state.isWorking) {
                            Text(if (state.selectedDryRunPlatform == null) "All*" else "All")
                        }
                        Button(onClick = { setDryRunPlatform("x") }, enabled = !state.isWorking) {
                            Text(if (state.selectedDryRunPlatform == "x") "X*" else "X")
                        }
                        Button(onClick = { setDryRunPlatform("linkedin") }, enabled = !state.isWorking) {
                            Text(if (state.selectedDryRunPlatform == "linkedin") "LinkedIn*" else "LinkedIn")
                        }
                        Button(onClick = { setDryRunPlatform("instagram") }, enabled = !state.isWorking) {
                            Text(if (state.selectedDryRunPlatform == "instagram") "Instagram*" else "Instagram")
                        }
                    }
                    Text(
                        "Selected IDs: ${
                            if (state.selectedAccountIds.isEmpty()) "None" else state.selectedAccountIds.joinToString()
                        }"
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Compose")
                    OutlinedTextField(
                        value = state.body,
                        onValueChange = { state = state.copy(body = it) },
                        label = { Text("Post text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.imageUrl,
                        onValueChange = { state = state.copy(imageUrl = it) },
                        label = { Text("Image URL (needed for Instagram)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::dryRun, enabled = !state.isWorking) { Text("Dry Run") }
                        Button(onClick = ::publish, enabled = !state.isWorking) { Text("Publish") }
                    }
                }
            }
        }

        if (state.isWorking) {
            item { CircularProgressIndicator(modifier = Modifier.width(36.dp)) }
        }

        state.errorText?.let { message ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        state.livePostResult?.let { result ->
            item {
                ResultSummaryCard(title = "Live Publish Result", result = result)
            }
            items(result.results) { item ->
                PublishResultCard(item)
            }
        }

        state.dryRunPreview?.let { result ->
            item {
                ResultSummaryCard(title = "Latest Dry Run Preview", result = result)
            }
            items(result.results) { item ->
                PublishResultCard(item)
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Dry Run History", style = MaterialTheme.typography.titleMedium)
                    Text("Entries: ${state.dryRunHistory.size}")
                    if (state.dryRunHistoryInfoText.isNotBlank()) {
                        Text(state.dryRunHistoryInfoText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (state.dryRunHistory.isEmpty()) {
            item { Text("No dry-run history entries yet.") }
        } else {
            items(state.dryRunHistory) { historyItem ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("At: ${historyItem.created_at}", style = MaterialTheme.typography.titleSmall)
                        Text("Status: ${historyItem.status}")
                        Text("Body: ${historyItem.body_preview}")
                        Text("Accounts: ${historyItem.account_ids.joinToString()}")
                        historyItem.results.forEach { result ->
                            val remote = result.remote_post_id?.let { " remote_id=$it" } ?: ""
                            val error = result.error_message?.let { " error=$it" } ?: ""
                            Text("- ${result.platform}: ${result.status}$remote$error")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Dead Letter Drilldown", style = MaterialTheme.typography.titleMedium)
                    Text("Entries: ${state.deadLetters.size}")
                    if (state.deadLetterInfoText.isNotBlank()) {
                        Text(state.deadLetterInfoText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.deadLetterActionText.isNotBlank()) {
                        Text(state.deadLetterActionText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (state.deadLetters.isEmpty()) {
            item { Text("No dead-letter entries yet.") }
        } else {
            items(state.deadLetters) { deadLetter ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Dead Letter #${deadLetter.id}", style = MaterialTheme.typography.titleSmall)
                        Text("Post: ${deadLetter.post_id}")
                        deadLetter.publish_job_id?.let { Text("Job: $it") }
                        Text("Reason: ${deadLetter.reason}")
                        Text("At: ${deadLetter.created_at}")
                        Button(
                            onClick = { selectDeadLetter(deadLetter.id) },
                            enabled = !state.isWorking
                        ) {
                            Text(if (state.selectedDeadLetterId == deadLetter.id) "Selected" else "View Payload")
                        }
                    }
                }
            }

            val selected = state.deadLetters.firstOrNull { it.id == state.selectedDeadLetterId }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Selected Dead Letter Payload", style = MaterialTheme.typography.titleMedium)
                        if (selected == null) {
                            Text("Select a dead-letter entry to view payload details.")
                        } else {
                            val prettyPayload = prettifyJson(selected.payload_json)
                            Text("Entry ID: ${selected.id}")
                            Text("Reason: ${selected.reason}")
                            Text(prettyPayload)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val copied = copyToClipboard(selected.payload_json ?: "")
                                        state = state.copy(
                                            deadLetterActionText = if (copied) {
                                                "Copied raw payload for dead letter ${selected.id}"
                                            } else {
                                                "Copy raw payload failed"
                                            }
                                        )
                                    },
                                    enabled = !state.isWorking && !selected.payload_json.isNullOrBlank()
                                ) {
                                    Text("Copy Raw Payload")
                                }
                                Button(
                                    onClick = {
                                        val copied = copyToClipboard(prettyPayload)
                                        state = state.copy(
                                            deadLetterActionText = if (copied) {
                                                "Copied pretty payload for dead letter ${selected.id}"
                                            } else {
                                                "Copy pretty payload failed"
                                            }
                                        )
                                    },
                                    enabled = !state.isWorking && !selected.payload_json.isNullOrBlank()
                                ) {
                                    Text("Copy Pretty Payload")
                                }
                            }
                            Button(
                                onClick = ::requeueSelectedDeadLetter,
                                enabled = !state.isWorking
                            ) {
                                Text("Requeue Selected")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSummaryCard(title: String, result: PostResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title)
            Text("Mode: ${if (result.dry_run) "Dry Run" else "Live Publish"}")
            if (result.post_id != null) {
                Text("Post ID: ${result.post_id}")
            }
            Text("Status: ${result.status}")
        }
    }
}

@Composable
private fun PublishResultCard(item: PublishResultItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Platform: ${item.platform}")
            Text("Status: ${item.status}")
            if (!item.remote_post_id.isNullOrBlank()) {
                Text("Remote ID: ${item.remote_post_id}")
            }
            if (!item.error_message.isNullOrBlank()) {
                Text("Error: ${item.error_message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
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

private fun formatCredentialCheck(platform: PlatformConfigStatus): String {
    val checkState = when (platform.last_check_success) {
        true -> "success"
        false -> "failed"
        null -> "never"
    }
    val base = if (platform.last_checked_at.isNullOrBlank()) {
        "Last check: never"
    } else {
        "Last check: ${platform.last_checked_at} ($checkState)"
    }
    val withCount = "$base | count=${platform.check_count}"
    return if (platform.last_check_error.isNullOrBlank()) withCount else "$withCount | error=${platform.last_check_error}"
}

private fun prettifyJson(raw: String?): String {
    if (raw.isNullOrBlank()) {
        return "No payload JSON recorded."
    }
    return runCatching {
        val parsed = JsonParser.parseString(raw)
        GsonBuilder().setPrettyPrinting().create().toJson(parsed)
    }.getOrElse { raw }
}

private fun copyToClipboard(text: String): Boolean {
    if (text.isBlank()) {
        return false
    }
    return runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }.isSuccess
}
