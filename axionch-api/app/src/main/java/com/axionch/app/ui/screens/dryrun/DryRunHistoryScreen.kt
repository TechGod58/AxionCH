package com.axionch.app.ui.screens.dryrun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.axionch.app.ui.screens.results.ResultsStore

@Composable
fun DryRunHistoryScreen(
    navController: NavController,
    viewModel: DryRunHistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lastDryRun by ResultsStore.dryRunResult.collectAsState()
    val dryRunError by ResultsStore.dryRunError.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Dry Run History", style = MaterialTheme.typography.headlineMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.loadHistory() }, enabled = !uiState.isLoading) {
                    Text("Refresh")
                }
                Button(onClick = { viewModel.clearHistory() }, enabled = !uiState.isLoading) {
                    Text("Clear Filtered")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.setPlatformFilter(null) }, enabled = !uiState.isLoading) {
                    Text(if (uiState.selectedPlatform == null) "All*" else "All")
                }
                Button(onClick = { viewModel.setPlatformFilter("x") }, enabled = !uiState.isLoading) {
                    Text(if (uiState.selectedPlatform == "x") "X*" else "X")
                }
                Button(onClick = { viewModel.setPlatformFilter("linkedin") }, enabled = !uiState.isLoading) {
                    Text(if (uiState.selectedPlatform == "linkedin") "LinkedIn*" else "LinkedIn")
                }
                Button(onClick = { viewModel.setPlatformFilter("instagram") }, enabled = !uiState.isLoading) {
                    Text(if (uiState.selectedPlatform == "instagram") "Instagram*" else "Instagram")
                }
            }

            if (uiState.infoText.isNotBlank()) {
                Text(uiState.infoText, style = MaterialTheme.typography.bodySmall)
            }

            if (dryRunError != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Latest Dry Run Error", color = MaterialTheme.colorScheme.error)
                        Text(dryRunError ?: "")
                    }
                }
            }

            if (lastDryRun != null) {
                val currentResult = lastDryRun!!
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Latest Dry Run")
                        Text("Status: ${currentResult.status}")
                        Text("Targets: ${currentResult.results.size} account(s)")
                    }
                }
            }

            if (uiState.errorText != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = uiState.errorText ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (uiState.items.isEmpty() && uiState.errorText == null) {
                Text("No dry-run history yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.items) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("At: ${item.created_at}", style = MaterialTheme.typography.titleSmall)
                                Text("Status: ${item.status}")
                                Text("Body: ${item.body_preview}")
                                Text("Account IDs: ${item.account_ids.joinToString()}")
                                item.results.forEach { result ->
                                    val remote = result.remote_post_id?.let { " remote_id=$it" } ?: ""
                                    val error = result.error_message?.let { " error=$it" } ?: ""
                                    Text("- ${result.platform}: ${result.status}$remote$error")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
