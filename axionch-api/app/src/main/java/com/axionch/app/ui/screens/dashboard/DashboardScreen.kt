package com.axionch.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.axionch.app.R
import com.axionch.app.data.api.AppClientConfigStore
import com.axionch.app.domain.model.NavRoute
import com.axionch.app.ui.theme.SkinPalettes
import com.axionch.app.ui.theme.skinPaletteById

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val config by AppClientConfigStore.config.collectAsState()
    val selectedSkin = skinPaletteById(config.skin)

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
            Image(
                painter = painterResource(id = R.drawable.axion_clean_logo),
                contentDescription = "Creator's Hub brand",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                contentScale = ContentScale.Fit
            )
            Text("Creator's Hub", style = MaterialTheme.typography.headlineMedium)
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backend status")
                    Text(uiState.healthText)
                }
            }
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Skin Color")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SkinPalettes) { skin ->
                            Button(
                                onClick = { AppClientConfigStore.updateSkin(context, skin.id) },
                                enabled = selectedSkin.id != skin.id
                            ) {
                                Text(if (selectedSkin.id == skin.id) "${skin.label}*" else skin.label)
                            }
                        }
                    }
                    Text(
                        "Active skin: ${selectedSkin.label}",
                        style = MaterialTheme.typography.bodySmall
                    )
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
            Button(onClick = { navController.navigate(NavRoute.VideoFilters.route) }) {
                Text("Video Filters")
            }
            Button(onClick = { navController.navigate(NavRoute.CaptureStudio.route) }) {
                Text("Capture Studio")
            }
        }
    }
}
