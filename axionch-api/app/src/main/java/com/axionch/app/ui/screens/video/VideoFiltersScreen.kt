package com.axionch.app.ui.screens.video

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun VideoFiltersScreen(
    navController: NavController,
    viewModel: VideoFiltersViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPresets()
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Media Filters", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                Text(
                    if (uiState.ffmpegAvailable) {
                        "FFmpeg detected: ${uiState.ffmpegBinary}"
                    } else {
                        "FFmpeg unavailable: ${uiState.ffmpegBinary}"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Video Filter")
                        OutlinedTextField(
                            value = uiState.videoSourcePath,
                            onValueChange = viewModel::updateVideoSourcePath,
                            label = { Text("Video Source Path (local)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoSourceUrl,
                            onValueChange = viewModel::updateVideoSourceUrl,
                            label = { Text("Video Source URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoOutputName,
                            onValueChange = viewModel::updateVideoOutputName,
                            label = { Text("Video Output Name (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoOverlayPath,
                            onValueChange = viewModel::updateVideoOverlayPath,
                            label = { Text("Overlay Path (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoOverlayUrl,
                            onValueChange = viewModel::updateVideoOverlayUrl,
                            label = { Text("Overlay URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoTextOverlay,
                            onValueChange = viewModel::updateVideoTextOverlay,
                            label = { Text("Text Overlay (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoSoundPath,
                            onValueChange = viewModel::updateVideoSoundPath,
                            label = { Text("Sound Path (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoSoundUrl,
                            onValueChange = viewModel::updateVideoSoundUrl,
                            label = { Text("Sound URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoSoundVolume,
                            onValueChange = viewModel::updateVideoSoundVolume,
                            label = { Text("Sound Volume (1.0 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoSaturationFactor,
                            onValueChange = viewModel::updateVideoSaturationFactor,
                            label = { Text("Saturation Factor (1.0 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoBrightnessDelta,
                            onValueChange = viewModel::updateVideoBrightnessDelta,
                            label = { Text("Brightness Delta (-1..1)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::toggleVideoSepia, enabled = !uiState.isWorking) {
                                Text(if (uiState.videoSepia) "Sepia*" else "Sepia")
                            }
                            Button(onClick = viewModel::toggleVideoBlackWhite, enabled = !uiState.isWorking) {
                                Text(if (uiState.videoBlackWhite) "B&W*" else "B&W")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::loadPresets, enabled = !uiState.isWorking) {
                                Text("Refresh Presets")
                            }
                            Button(onClick = viewModel::applySelectedVideoFilter, enabled = !uiState.isWorking) {
                                Text("Apply Video Filter")
                            }
                        }
                        if (uiState.videoStatusMessage.isNotBlank()) {
                            Text(uiState.videoStatusMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.videoOutputPath.isNotBlank()) {
                            Text("Video output: ${uiState.videoOutputPath}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Image Filter")
                        OutlinedTextField(
                            value = uiState.imageSourcePath,
                            onValueChange = viewModel::updateImageSourcePath,
                            label = { Text("Image Source Path (local)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageSourceUrl,
                            onValueChange = viewModel::updateImageSourceUrl,
                            label = { Text("Image Source URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageOutputName,
                            onValueChange = viewModel::updateImageOutputName,
                            label = { Text("Image Output Name (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageOverlayPath,
                            onValueChange = viewModel::updateImageOverlayPath,
                            label = { Text("Overlay Path (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageOverlayUrl,
                            onValueChange = viewModel::updateImageOverlayUrl,
                            label = { Text("Overlay URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageTextOverlay,
                            onValueChange = viewModel::updateImageTextOverlay,
                            label = { Text("Text Overlay (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageSaturationFactor,
                            onValueChange = viewModel::updateImageSaturationFactor,
                            label = { Text("Saturation Factor (1.0 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageBrightnessFactor,
                            onValueChange = viewModel::updateImageBrightnessFactor,
                            label = { Text("Brightness Factor (1.0 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageContrastFactor,
                            onValueChange = viewModel::updateImageContrastFactor,
                            label = { Text("Contrast Factor (1.0 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::toggleImageSepia, enabled = !uiState.isWorking) {
                                Text(if (uiState.imageSepia) "Sepia*" else "Sepia")
                            }
                            Button(onClick = viewModel::toggleImageBlackWhite, enabled = !uiState.isWorking) {
                                Text(if (uiState.imageBlackWhite) "B&W*" else "B&W")
                            }
                            Button(onClick = viewModel::toggleImageCartoonify, enabled = !uiState.isWorking) {
                                Text(if (uiState.imageCartoonify) "Cartoon*" else "Cartoon")
                            }
                            Button(onClick = viewModel::toggleImageCaricature, enabled = !uiState.isWorking) {
                                Text(if (uiState.imageCaricature) "Caricature*" else "Caricature")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::applySelectedImageFilter, enabled = !uiState.isWorking) {
                                Text("Apply Image Filter")
                            }
                        }
                        if (uiState.imageStatusMessage.isNotBlank()) {
                            Text(uiState.imageStatusMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.imageOutputPath.isNotBlank()) {
                            Text("Image output: ${uiState.imageOutputPath}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (uiState.isWorking) {
                item {
                    CircularProgressIndicator()
                }
            }

            item {
                Text("Video Presets", style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.videoPresets) { preset ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(preset.name, style = MaterialTheme.typography.titleSmall)
                        Text(preset.description, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { viewModel.selectVideoPreset(preset.id) },
                            enabled = !uiState.isWorking
                        ) {
                            Text(if (uiState.selectedVideoPresetId == preset.id) "Selected" else "Select")
                        }
                    }
                }
            }

            item {
                Text("Image Presets", style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.imagePresets) { preset ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(preset.name, style = MaterialTheme.typography.titleSmall)
                        Text(preset.description, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { viewModel.selectImagePreset(preset.id) },
                            enabled = !uiState.isWorking
                        ) {
                            Text(if (uiState.selectedImagePresetId == preset.id) "Selected" else "Select")
                        }
                    }
                }
            }
        }
    }
}
