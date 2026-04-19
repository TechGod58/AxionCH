package com.axionch.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.axionch.app.domain.model.NavRoute

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("AxionCH", style = MaterialTheme.typography.headlineMedium)
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backend status")
                    Text(uiState.healthText)
                }
            }
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Client Config")
                    OutlinedTextField(
                        value = uiState.clientBaseUrl,
                        onValueChange = viewModel::updateClientBaseUrl,
                        label = { Text("Base URL") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.clientUserEmail,
                        onValueChange = viewModel::updateClientUserEmail,
                        label = { Text("User Email") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.clientApiKey,
                        onValueChange = viewModel::updateClientApiKey,
                        label = { Text("API Key (optional)") },
                        singleLine = true
                    )
                    Button(onClick = { viewModel.saveClientConfig(context) }) {
                        Text("Save Client Config")
                    }
                    if (uiState.clientConfigMessage.isNotBlank()) {
                        Text(uiState.clientConfigMessage, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Platform Config Status")
                    Text(uiState.xConfigText)
                    Text(uiState.xMissingFieldsText, style = MaterialTheme.typography.bodySmall)
                    Text(uiState.xCredentialCheckText, style = MaterialTheme.typography.bodySmall)
                    Text(uiState.linkedinConfigText)
                    Text(uiState.linkedinMissingFieldsText, style = MaterialTheme.typography.bodySmall)
                    Text(uiState.linkedinCredentialCheckText, style = MaterialTheme.typography.bodySmall)
                    Text(uiState.instagramConfigText)
                    Text(uiState.instagramMissingFieldsText, style = MaterialTheme.typography.bodySmall)
                    Text(uiState.instagramCredentialCheckText, style = MaterialTheme.typography.bodySmall)
                    if (uiState.checkSummaryText.isNotBlank()) {
                        Text(uiState.checkSummaryText, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { viewModel.loadConfigStatus() }) {
                        Text("Refresh Config Status")
                    }
                    Button(onClick = { viewModel.runCredentialChecks() }) {
                        Text("Run Credential Check")
                    }
                }
            }
            Button(onClick = { navController.navigate(NavRoute.Accounts.route) }) {
                Text("Connected Accounts")
            }
            Button(onClick = { navController.navigate(NavRoute.Composer.route) }) {
                Text("Compose Post")
            }
            Button(onClick = { navController.navigate(NavRoute.Results.route) }) {
                Text("Live Publish Results")
            }
            Button(onClick = { navController.navigate(NavRoute.DryRunHistory.route) }) {
                Text("Dry Run History")
            }
            Button(onClick = { navController.navigate(NavRoute.Vault.route) }) {
                Text("Password Vault")
            }
        }
    }
}
