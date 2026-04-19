package com.axionch.app.ui.screens.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

@Composable
fun ResultsScreen(
    navController: NavController,
    viewModel: ResultsViewModel = viewModel()
) {
    val result by ResultsStore.liveResult.collectAsState()
    val error by ResultsStore.liveError.collectAsState()
    val deadLetterState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Live Publish Results", style = MaterialTheme.typography.headlineMedium)

            if (error != null) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Error", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text(error!!)
                    }
                }
            }

            if (result != null) {
                val currentResult = result!!
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Mode: Live Publish")
                        if (currentResult.post_id != null) {
                            Text("Post ID: ${currentResult.post_id}")
                        }
                        Text("Overall status: ${currentResult.status}")
                    }
                }

                currentResult.results.forEach { item ->
                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Platform: ${item.platform}", style = MaterialTheme.typography.titleSmall)
                            Text("Status: ${item.status}")
                            if (item.remote_post_id != null) {
                                Text("Remote ID: ${item.remote_post_id}")
                            }
                            if (item.error_message != null) {
                                Text("Error: ${item.error_message}", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            } else if (error == null) {
                Text("No live publish results yet.")
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dead Letter Drilldown", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { viewModel.refreshDeadLetters() },
                        enabled = !deadLetterState.isLoading && !deadLetterState.isRequeueing
                    ) {
                        Text("Refresh Dead Letters")
                    }
                    Button(
                        onClick = { viewModel.requeueSelectedDeadLetter() },
                        enabled = deadLetterState.selectedDeadLetterId != null && !deadLetterState.isLoading && !deadLetterState.isRequeueing
                    ) {
                        Text(if (deadLetterState.isRequeueing) "Requeueing..." else "Requeue Selected")
                    }
                    if (deadLetterState.actionMessage != null) {
                        Text(deadLetterState.actionMessage!!, style = MaterialTheme.typography.bodySmall)
                    }
                    if (deadLetterState.errorMessage != null) {
                        Text(
                            text = deadLetterState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (deadLetterState.deadLetters.isEmpty()) {
                        Text("No dead-letter entries yet.")
                    } else {
                        deadLetterState.deadLetters.forEach { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Dead Letter #${item.id}", style = MaterialTheme.typography.titleSmall)
                                    Text("Reason: ${item.reason}")
                                    Text("Post ID: ${item.post_id}")
                                    Button(onClick = { viewModel.selectDeadLetter(item.id) }) {
                                        Text(
                                            if (deadLetterState.selectedDeadLetterId == item.id) {
                                                "Selected"
                                            } else {
                                                "View Payload"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val selected = deadLetterState.deadLetters.firstOrNull {
                        it.id == deadLetterState.selectedDeadLetterId
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Selected Payload", style = MaterialTheme.typography.titleSmall)
                            if (selected == null) {
                                Text("Select a dead-letter entry to view payload details.")
                            } else {
                                val rawPayload = selected.payload_json.orEmpty()
                                val prettyPayload = prettifyJson(rawPayload)
                                Text("ID: ${selected.id}")
                                Text("Reason: ${selected.reason}")
                                Text(prettyPayload)
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(rawPayload))
                                        viewModel.setActionMessage("Copied raw payload for dead letter ${selected.id}")
                                    },
                                    enabled = rawPayload.isNotBlank()
                                ) {
                                    Text("Copy Raw Payload")
                                }
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(prettyPayload))
                                        viewModel.setActionMessage("Copied pretty payload for dead letter ${selected.id}")
                                    },
                                    enabled = rawPayload.isNotBlank()
                                ) {
                                    Text("Copy Pretty Payload")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun prettifyJson(raw: String): String {
    if (raw.isBlank()) {
        return "No payload JSON recorded."
    }
    return runCatching {
        val parsed = JsonParser.parseString(raw)
        GsonBuilder().setPrettyPrinting().create().toJson(parsed)
    }.getOrElse { raw }
}
