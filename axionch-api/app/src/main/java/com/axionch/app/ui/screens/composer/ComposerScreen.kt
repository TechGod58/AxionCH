package com.axionch.app.ui.screens.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.axionch.app.domain.model.NavRoute

@Composable
fun ComposerScreen(
    navController: NavController,
    viewModel: ComposerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBusy = uiState.isPublishing || uiState.isDryRunning

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Compose", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = uiState.body,
                onValueChange = viewModel::updateBody,
                label = { Text("Post text") },
                modifier = Modifier.fillMaxSize().weight(1f, fill = false),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                enabled = !isBusy
            )
            OutlinedTextField(
                value = uiState.imageUrl,
                onValueChange = viewModel::updateImageUrl,
                label = { Text("Image URL (needed for Instagram mock)") },
                enabled = !isBusy
            )
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selected account IDs")
                    Text(uiState.selectedAccountIds.joinToString())
                }
            }
            if (uiState.message.isNotBlank()) {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(
                onClick = { viewModel.loadAccountsForSelection() },
                enabled = !isBusy
            ) {
                Text("Load Account IDs")
            }
            
            if (isBusy) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        viewModel.dryRun {
                            navController.navigate(NavRoute.Results.route)
                        }
                    },
                    enabled = uiState.selectedAccountIds.isNotEmpty()
                ) {
                    Text("Dry Run")
                }
                Button(
                    onClick = {
                        viewModel.publish {
                            navController.navigate(NavRoute.Results.route)
                        }
                    },
                    enabled = uiState.selectedAccountIds.isNotEmpty()
                ) {
                    Text("Publish")
                }
            }
        }
    }
}
