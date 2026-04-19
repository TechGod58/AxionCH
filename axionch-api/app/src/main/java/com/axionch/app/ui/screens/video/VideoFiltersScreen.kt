package com.axionch.app.ui.screens.video

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.axionch.app.media.LocalMediaFileManager
import com.axionch.app.media.LocalMediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface TimelineImportTarget {
    data class Clip(val index: Int) : TimelineImportTarget
    data class AudioTrack(val index: Int) : TimelineImportTarget
    data object BackgroundAudio : TimelineImportTarget
}

private enum class MediaEditingTab(val label: String) {
    TEMPLATES("Templates"),
    ASSETS("Assets"),
    EXPORT("Export"),
    VIDEO("Video"),
    IMAGE("Photo"),
    TIMELINE("Timeline"),
    CAPTIONS("Captions"),
    PRESETS("Presets")
}

@Composable
fun VideoFiltersScreen(
    navController: NavController,
    viewModel: VideoFiltersViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedMediaTab by remember { mutableStateOf(MediaEditingTab.VIDEO) }
    val mediaTabs = remember { MediaEditingTab.entries.toList() }

    val importVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LocalMediaFileManager.importToAppStorage(context, uri, LocalMediaKind.VIDEO)
                }
            }.onSuccess { imported ->
                viewModel.onVideoImported(imported.localPath, imported.displayName)
            }.onFailure {
                viewModel.onVideoLocalMediaError("Video import failed: ${it.message}")
            }
        }
    }

    val importVideoOverlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LocalMediaFileManager.importToAppStorage(context, uri, LocalMediaKind.IMAGE)
                }
            }.onSuccess { imported ->
                viewModel.onVideoOverlayImported(imported.localPath, imported.displayName)
            }.onFailure {
                viewModel.onVideoLocalMediaError("Overlay import failed: ${it.message}")
            }
        }
    }

    val importVideoSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LocalMediaFileManager.importToAppStorage(context, uri, LocalMediaKind.AUDIO)
                }
            }.onSuccess { imported ->
                viewModel.onVideoSoundImported(imported.localPath, imported.displayName)
            }.onFailure {
                viewModel.onVideoLocalMediaError("Audio import failed: ${it.message}")
            }
        }
    }

    val importVideoLutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LocalMediaFileManager.importToAppStorage(context, uri, LocalMediaKind.VIDEO)
                }
            }.onSuccess { imported ->
                viewModel.onVideoLutImported(imported.localPath, imported.displayName)
            }.onFailure {
                viewModel.onVideoLocalMediaError("LUT import failed: ${it.message}")
            }
        }
    }

    val importImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LocalMediaFileManager.importToAppStorage(context, uri, LocalMediaKind.IMAGE)
                }
            }.onSuccess { imported ->
                viewModel.onImageImported(imported.localPath, imported.displayName)
            }.onFailure {
                viewModel.onImageLocalMediaError("Image import failed: ${it.message}")
            }
        }
    }

    val importImageOverlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LocalMediaFileManager.importToAppStorage(context, uri, LocalMediaKind.IMAGE)
                }
            }.onSuccess { imported ->
                viewModel.onImageOverlayImported(imported.localPath, imported.displayName)
            }.onFailure {
                viewModel.onImageLocalMediaError("Image overlay import failed: ${it.message}")
            }
        }
    }

    val importCaptionSourceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LocalMediaFileManager.importToAppStorage(context, uri, LocalMediaKind.VIDEO)
                }
            }.onSuccess { imported ->
                viewModel.onCaptionSourceImported(imported.localPath, imported.displayName)
            }.onFailure {
                viewModel.onCaptionLocalMediaError("Caption source import failed: ${it.message}")
            }
        }
    }

    val importSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LocalMediaFileManager.importToAppStorage(context, uri, LocalMediaKind.VIDEO)
                }
            }.onSuccess { imported ->
                viewModel.onCaptionSubtitleImported(imported.localPath, imported.displayName)
            }.onFailure {
                viewModel.onCaptionLocalMediaError("Subtitle import failed: ${it.message}")
            }
        }
    }

    var pendingTimelineImport by remember { mutableStateOf<TimelineImportTarget?>(null) }
    val importTimelineMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = pendingTimelineImport
        pendingTimelineImport = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    when (target) {
                        is TimelineImportTarget.Clip ->
                            LocalMediaFileManager.importToAppStorage(context, uri, LocalMediaKind.VIDEO)
                        is TimelineImportTarget.AudioTrack,
                        TimelineImportTarget.BackgroundAudio ->
                            LocalMediaFileManager.importToAppStorage(context, uri, LocalMediaKind.AUDIO)
                    }
                }
            }.onSuccess { imported ->
                when (target) {
                    is TimelineImportTarget.Clip ->
                        viewModel.onTimelineClipImported(target.index, imported.localPath, imported.displayName)
                    is TimelineImportTarget.AudioTrack ->
                        viewModel.onTimelineAudioTrackImported(target.index, imported.localPath, imported.displayName)
                    TimelineImportTarget.BackgroundAudio ->
                        viewModel.onTimelineBackgroundAudioImported(imported.localPath, imported.displayName)
                }
            }.onFailure {
                viewModel.onTimelineLocalMediaError("Timeline import failed: ${it.message}")
            }
        }
    }

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
                TabRow(selectedTabIndex = mediaTabs.indexOf(selectedMediaTab).coerceAtLeast(0)) {
                    mediaTabs.forEach { tab ->
                        Tab(
                            selected = selectedMediaTab == tab,
                            onClick = { selectedMediaTab = tab },
                            text = { Text(tab.label) }
                        )
                    }
                }
            }

            item {
                if (selectedMediaTab == MediaEditingTab.TEMPLATES) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        Text("Creator Templates", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Reusable presets by platform (Reels / Shorts / TikTok and more).",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = uiState.templatePlatformFilter,
                            onValueChange = viewModel::updateTemplatePlatformFilter,
                            label = { Text("Platform Filter (instagram/youtube/tiktok)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.templateMediaTypeFilter,
                            onValueChange = viewModel::updateTemplateMediaTypeFilter,
                            label = { Text("Media Type Filter (video/image)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.applyTemplateQuickFilter("reels") }, enabled = !uiState.isWorking) {
                                Text("Reels")
                            }
                            Button(onClick = { viewModel.applyTemplateQuickFilter("shorts") }, enabled = !uiState.isWorking) {
                                Text("Shorts")
                            }
                            Button(onClick = { viewModel.applyTemplateQuickFilter("tiktok") }, enabled = !uiState.isWorking) {
                                Text("TikTok")
                            }
                            Button(
                                onClick = {
                                    viewModel.clearTemplateFilters()
                                    viewModel.loadCreatorTemplates()
                                },
                                enabled = !uiState.isWorking
                            ) {
                                Text("Clear Filters")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::loadCreatorTemplates, enabled = !uiState.isWorking) {
                                Text("Refresh Templates")
                            }
                            Button(onClick = viewModel::applySelectedCreatorTemplate, enabled = !uiState.isWorking) {
                                Text("Apply Selected")
                            }
                        }
                        if (uiState.templateStatusMessage.isNotBlank()) {
                            Text(uiState.templateStatusMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.activeCreatorTemplateText.isNotBlank()) {
                            Text(uiState.activeCreatorTemplateText, style = MaterialTheme.typography.bodySmall)
                        }
                        uiState.creatorTemplates.forEach { template ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(template.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${template.media_type} | ${template.width}x${template.height}${template.fps?.let { " @${it}fps" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text("Platforms: ${template.platforms.joinToString()}", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.selectCreatorTemplate(template.id) },
                                            enabled = !uiState.isWorking
                                        ) {
                                            Text(if (uiState.selectedCreatorTemplateId == template.id) "Selected" else "Select")
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }

            item {
                if (selectedMediaTab == MediaEditingTab.ASSETS) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        Text("Asset Library", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Stock audio + overlay management with licensing and attribution metadata.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = uiState.assetKindFilter,
                            onValueChange = viewModel::updateAssetKindFilter,
                            label = { Text("Kind Filter (audio/overlay)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.assetTagFilter,
                            onValueChange = viewModel::updateAssetTagFilter,
                            label = { Text("Tag Filter (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.applyAssetQuickFilter("audio") }, enabled = !uiState.isWorking) {
                                Text("Audio")
                            }
                            Button(onClick = { viewModel.applyAssetQuickFilter("overlay") }, enabled = !uiState.isWorking) {
                                Text("Overlay")
                            }
                            Button(onClick = { viewModel.applyAssetQuickFilter(null) }, enabled = !uiState.isWorking) {
                                Text("All")
                            }
                            Button(onClick = viewModel::loadAssets, enabled = !uiState.isWorking) {
                                Text("Refresh")
                            }
                        }
                        OutlinedTextField(
                            value = uiState.assetTitleInput,
                            onValueChange = viewModel::updateAssetTitleInput,
                            label = { Text("Asset Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.assetSourceInput,
                            onValueChange = viewModel::updateAssetSourceInput,
                            label = { Text("Source (vendor/library)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.assetLicenseNameInput,
                            onValueChange = viewModel::updateAssetLicenseNameInput,
                            label = { Text("License Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.assetLicenseUrlInput,
                            onValueChange = viewModel::updateAssetLicenseUrlInput,
                            label = { Text("License URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = viewModel::toggleAssetAttributionRequired, enabled = !uiState.isWorking) {
                            Text(if (uiState.assetAttributionRequired) "Attribution Required: On" else "Attribution Required: Off")
                        }
                        OutlinedTextField(
                            value = uiState.assetAttributionTextInput,
                            onValueChange = viewModel::updateAssetAttributionTextInput,
                            label = { Text("Attribution Text (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.assetTagsInput,
                            onValueChange = viewModel::updateAssetTagsInput,
                            label = { Text("Tags (comma/newline)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.assetLocalPathInput,
                            onValueChange = viewModel::updateAssetLocalPathInput,
                            label = { Text("Local Path (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.assetRemoteUrlInput,
                            onValueChange = viewModel::updateAssetRemoteUrlInput,
                            label = { Text("Remote URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::saveAsset, enabled = !uiState.isWorking) {
                                Text("Save Asset")
                            }
                        }
                        if (uiState.assetStatusMessage.isNotBlank()) {
                            Text(uiState.assetStatusMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        uiState.assets.forEach { asset ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("${asset.title} (${asset.kind})", style = MaterialTheme.typography.titleSmall)
                                    Text("Source: ${asset.source}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "License: ${asset.license_name}${asset.license_url?.let { " | $it" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (asset.attribution_required || !asset.attribution_text.isNullOrBlank()) {
                                        Text(
                                            "Attribution: ${if (asset.attribution_required) "required" else "optional"}${asset.attribution_text?.let { " | $it" } ?: ""}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (asset.tags.isNotEmpty()) {
                                        Text("Tags: ${asset.tags.joinToString()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        "Local: ${asset.local_path ?: "-"} | Remote: ${asset.remote_url ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (asset.kind.equals("audio", ignoreCase = true)) {
                                            Button(onClick = { viewModel.applyAssetToVideoAudio(asset.id) }, enabled = !uiState.isWorking) {
                                                Text("Use In Video Audio")
                                            }
                                            Button(onClick = { viewModel.applyAssetToTimelineBackgroundAudio(asset.id) }, enabled = !uiState.isWorking) {
                                                Text("Use In Timeline")
                                            }
                                        } else {
                                            Button(onClick = { viewModel.applyAssetToVideoOverlay(asset.id) }, enabled = !uiState.isWorking) {
                                                Text("Use In Video Overlay")
                                            }
                                            Button(onClick = { viewModel.applyAssetToImageOverlay(asset.id) }, enabled = !uiState.isWorking) {
                                                Text("Use In Image Overlay")
                                            }
                                        }
                                        Button(onClick = { viewModel.deleteAsset(asset.id) }, enabled = !uiState.isWorking) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }

            item {
                if (selectedMediaTab == MediaEditingTab.EXPORT) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        Text("Export Pipeline", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Platform-specific presets, batch export queue, and optional hardware-accelerated rendering.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = uiState.exportSourcePathsInput,
                            onValueChange = viewModel::updateExportSourcePathsInput,
                            label = { Text("Source Paths (one per line, local files)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.exportSourceUrlsInput,
                            onValueChange = viewModel::updateExportSourceUrlsInput,
                            label = { Text("Source URLs (one per line)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.exportOutputPrefix,
                            onValueChange = viewModel::updateExportOutputPrefix,
                            label = { Text("Output Prefix (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::toggleExportUseHardwareAcceleration, enabled = !uiState.isWorking) {
                                Text(
                                    if (uiState.exportUseHardwareAcceleration) {
                                        "HW Acceleration: On"
                                    } else {
                                        "HW Acceleration: Off"
                                    }
                                )
                            }
                            Button(onClick = viewModel::queueBatchExport, enabled = !uiState.isWorking) {
                                Text("Queue Batch Export")
                            }
                            Button(onClick = viewModel::refreshExportJobStatus, enabled = !uiState.isWorking) {
                                Text("Refresh Job")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (uiState.videoOutputPath.isNotBlank()) {
                                Button(onClick = { viewModel.appendExportSourcePath(uiState.videoOutputPath) }, enabled = !uiState.isWorking) {
                                    Text("Add Video Output")
                                }
                            }
                            if (uiState.timelineOutputPath.isNotBlank()) {
                                Button(onClick = { viewModel.appendExportSourcePath(uiState.timelineOutputPath) }, enabled = !uiState.isWorking) {
                                    Text("Add Timeline Output")
                                }
                            }
                            if (uiState.captionBurnOutputPath.isNotBlank()) {
                                Button(onClick = { viewModel.appendExportSourcePath(uiState.captionBurnOutputPath) }, enabled = !uiState.isWorking) {
                                    Text("Add Burned Caption Output")
                                }
                            }
                            if (uiState.imageOutputPath.isNotBlank()) {
                                Button(onClick = { viewModel.appendExportSourcePath(uiState.imageOutputPath) }, enabled = !uiState.isWorking) {
                                    Text("Add Image Output")
                                }
                            }
                        }
                        if (uiState.exportStatusMessage.isNotBlank()) {
                            Text(uiState.exportStatusMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.exportQueuedJobId.isNotBlank()) {
                            Text(
                                "Job ${uiState.exportQueuedJobId} | status=${uiState.exportLastStatus} | queued=${uiState.exportQueuedItems} completed=${uiState.exportCompletedItems} failed=${uiState.exportFailedItems}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text("Export Presets", style = MaterialTheme.typography.titleSmall)
                        uiState.exportPresets.forEach { preset ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(preset.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${preset.platform} | ${preset.media_type} | ${preset.width}x${preset.height} @${preset.fps}fps",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Video ${preset.video_bitrate} / Audio ${preset.audio_bitrate} / ${preset.container}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Button(
                                        onClick = { viewModel.selectExportPreset(preset.id) },
                                        enabled = !uiState.isWorking
                                    ) {
                                        Text(if (uiState.selectedExportPresetId == preset.id) "Selected" else "Select")
                                    }
                                }
                            }
                        }
                        if (uiState.exportItemSummaries.isNotEmpty()) {
                            Text("Batch Items", style = MaterialTheme.typography.titleSmall)
                            uiState.exportItemSummaries.forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        }
                    }
                }
            }

            item {
                if (selectedMediaTab == MediaEditingTab.VIDEO) {
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
                        Button(
                            onClick = { importVideoLauncher.launch(arrayOf("video/*")) },
                            enabled = !uiState.isWorking
                        ) {
                            Text("Import Local Video")
                        }
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
                        Button(
                            onClick = { importVideoOverlayLauncher.launch(arrayOf("image/*")) },
                            enabled = !uiState.isWorking
                        ) {
                            Text("Import Local Overlay Image")
                        }
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
                        Button(
                            onClick = { importVideoSoundLauncher.launch(arrayOf("audio/*")) },
                            enabled = !uiState.isWorking
                        ) {
                            Text("Import Local Sound")
                        }
                        OutlinedTextField(
                            value = uiState.videoSoundVolume,
                            onValueChange = viewModel::updateVideoSoundVolume,
                            label = { Text("Sound Volume (1.0 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Audio Finishing", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::toggleVideoAudioDenoise, enabled = !uiState.isWorking) {
                                Text(if (uiState.videoAudioDenoise) "Denoise*" else "Denoise")
                            }
                            Button(onClick = viewModel::toggleVideoAudioCompressor, enabled = !uiState.isWorking) {
                                Text(if (uiState.videoAudioCompressor) "Compressor*" else "Compressor")
                            }
                            Button(onClick = viewModel::toggleVideoAudioLimiter, enabled = !uiState.isWorking) {
                                Text(if (uiState.videoAudioLimiter) "Limiter*" else "Limiter")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::toggleVideoAudioGate, enabled = !uiState.isWorking) {
                                Text(if (uiState.videoAudioGate) "Gate*" else "Gate")
                            }
                            Button(onClick = viewModel::toggleVideoAudioDucking, enabled = !uiState.isWorking) {
                                Text(if (uiState.videoAudioDucking) "Ducking*" else "Ducking")
                            }
                            Button(onClick = viewModel::toggleVideoLoudnessNormalization, enabled = !uiState.isWorking) {
                                Text(if (uiState.videoLoudnessNormalization) "Loudness*" else "Loudness")
                            }
                        }
                        OutlinedTextField(
                            value = uiState.videoAudioEqLowGain,
                            onValueChange = viewModel::updateVideoAudioEqLowGain,
                            label = { Text("EQ Low Gain dB (-24..24)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoAudioEqMidGain,
                            onValueChange = viewModel::updateVideoAudioEqMidGain,
                            label = { Text("EQ Mid Gain dB (-24..24)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoAudioEqHighGain,
                            onValueChange = viewModel::updateVideoAudioEqHighGain,
                            label = { Text("EQ High Gain dB (-24..24)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Pro Video Controls", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = uiState.videoSpeedFactor,
                            onValueChange = viewModel::updateVideoSpeedFactor,
                            label = { Text("Speed Ramp Factor (0.10..4.0)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = viewModel::toggleVideoStabilize, enabled = !uiState.isWorking) {
                            Text(if (uiState.videoStabilize) "Stabilization: On" else "Stabilization: Off")
                        }
                        OutlinedTextField(
                            value = uiState.videoCurvesPreset,
                            onValueChange = viewModel::updateVideoCurvesPreset,
                            label = { Text("Curves Preset (e.g. medium_contrast)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.updateVideoCurvesPreset("medium_contrast") }, enabled = !uiState.isWorking) {
                                Text("Medium Contrast")
                            }
                            Button(onClick = { viewModel.updateVideoCurvesPreset("vintage") }, enabled = !uiState.isWorking) {
                                Text("Vintage")
                            }
                            Button(onClick = { viewModel.updateVideoCurvesPreset("strong_contrast") }, enabled = !uiState.isWorking) {
                                Text("Strong Contrast")
                            }
                        }
                        OutlinedTextField(
                            value = uiState.videoLut3dPath,
                            onValueChange = viewModel::updateVideoLut3dPath,
                            label = { Text("LUT 3D Path (.cube)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.videoLut3dUrl,
                            onValueChange = viewModel::updateVideoLut3dUrl,
                            label = { Text("LUT 3D URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { importVideoLutLauncher.launch(arrayOf("*/*")) },
                            enabled = !uiState.isWorking
                        ) {
                            Text("Import LUT File")
                        }
                        Text("Motion Tracking", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.videoMotionStartSeconds,
                                onValueChange = viewModel::updateVideoMotionStartSeconds,
                                label = { Text("Start Sec") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = uiState.videoMotionEndSeconds,
                                onValueChange = viewModel::updateVideoMotionEndSeconds,
                                label = { Text("End Sec (optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.videoMotionRoiX,
                                onValueChange = viewModel::updateVideoMotionRoiX,
                                label = { Text("ROI X") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = uiState.videoMotionRoiY,
                                onValueChange = viewModel::updateVideoMotionRoiY,
                                label = { Text("ROI Y") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.videoMotionRoiWidth,
                                onValueChange = viewModel::updateVideoMotionRoiWidth,
                                label = { Text("ROI W") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = uiState.videoMotionRoiHeight,
                                onValueChange = viewModel::updateVideoMotionRoiHeight,
                                label = { Text("ROI H") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.videoMotionSmoothingWindow,
                                onValueChange = viewModel::updateVideoMotionSmoothingWindow,
                                label = { Text("Smoothing") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = uiState.videoMotionMinConfidence,
                                onValueChange = viewModel::updateVideoMotionMinConfidence,
                                label = { Text("Min Conf") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = uiState.videoMotionOverlayOutputName,
                            onValueChange = viewModel::updateVideoMotionOverlayOutputName,
                            label = { Text("Motion Overlay Output (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = viewModel::trackVideoMotion, enabled = !uiState.isWorking) {
                            Text("Run Motion Tracking")
                        }
                        if (uiState.videoMotionSummary.isNotBlank()) {
                            Text(uiState.videoMotionSummary, style = MaterialTheme.typography.bodySmall)
                        }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::applyVideoAudioFinishing, enabled = !uiState.isWorking) {
                                Text("Run Audio Finishing")
                            }
                            Button(onClick = viewModel::applyVideoProControls, enabled = !uiState.isWorking) {
                                Text("Apply Pro Video")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (uiState.videoOutputPath.isBlank()) {
                                        viewModel.onVideoLocalMediaError("No local video output path to export yet.")
                                    } else {
                                        scope.launch {
                                            runCatching {
                                                withContext(Dispatchers.IO) {
                                                    LocalMediaFileManager.exportLocalFileToMediaStore(
                                                        context = context,
                                                        sourcePath = uiState.videoOutputPath,
                                                        kind = LocalMediaKind.VIDEO,
                                                        preferredDisplayName = uiState.videoOutputName
                                                    )
                                                }
                                            }.onSuccess { uri ->
                                                viewModel.onVideoExported(uri.toString())
                                            }.onFailure {
                                                viewModel.onVideoLocalMediaError("Video export failed: ${it.message}")
                                            }
                                        }
                                    }
                                },
                                enabled = !uiState.isWorking
                            ) {
                                Text("Export Video To Gallery")
                            }
                        }
                        if (uiState.videoStatusMessage.isNotBlank()) {
                            Text(uiState.videoStatusMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.videoLocalMediaMessage.isNotBlank()) {
                            Text(uiState.videoLocalMediaMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.videoOutputPath.isNotBlank()) {
                            Text("Video output: ${uiState.videoOutputPath}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.videoExportUri.isNotBlank()) {
                            Text("Video gallery URI: ${uiState.videoExportUri}", style = MaterialTheme.typography.bodySmall)
                        }
                        }
                    }
                }
            }

            item {
                if (selectedMediaTab == MediaEditingTab.IMAGE) {
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
                        Button(
                            onClick = { importImageLauncher.launch(arrayOf("image/*")) },
                            enabled = !uiState.isWorking
                        ) {
                            Text("Import Local Image")
                        }
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
                        Button(
                            onClick = { importImageOverlayLauncher.launch(arrayOf("image/*")) },
                            enabled = !uiState.isWorking
                        ) {
                            Text("Import Local Image Overlay")
                        }
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
                        Button(onClick = viewModel::toggleImageRawEnhance, enabled = !uiState.isWorking) {
                            Text(if (uiState.imageRawEnhance) "RAW Enhance: On" else "RAW Enhance: Off")
                        }
                        Text("Selective Edits", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = uiState.imageSelectiveX,
                            onValueChange = viewModel::updateImageSelectiveX,
                            label = { Text("Selective X (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageSelectiveY,
                            onValueChange = viewModel::updateImageSelectiveY,
                            label = { Text("Selective Y (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageSelectiveWidth,
                            onValueChange = viewModel::updateImageSelectiveWidth,
                            label = { Text("Selective Width (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageSelectiveHeight,
                            onValueChange = viewModel::updateImageSelectiveHeight,
                            label = { Text("Selective Height (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageSelectiveBrightnessFactor,
                            onValueChange = viewModel::updateImageSelectiveBrightnessFactor,
                            label = { Text("Selective Brightness (1.0 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageSelectiveContrastFactor,
                            onValueChange = viewModel::updateImageSelectiveContrastFactor,
                            label = { Text("Selective Contrast (1.0 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageSelectiveSaturationFactor,
                            onValueChange = viewModel::updateImageSelectiveSaturationFactor,
                            label = { Text("Selective Saturation (1.0 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Background Removal", style = MaterialTheme.typography.titleSmall)
                        Button(onClick = viewModel::toggleImageBackgroundRemove, enabled = !uiState.isWorking) {
                            Text(if (uiState.imageBackgroundRemove) "Background Remove: On" else "Background Remove: Off")
                        }
                        OutlinedTextField(
                            value = uiState.imageBackgroundKeyColor,
                            onValueChange = viewModel::updateImageBackgroundKeyColor,
                            label = { Text("Key Color (#00FF00 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageBackgroundKeyTolerance,
                            onValueChange = viewModel::updateImageBackgroundKeyTolerance,
                            label = { Text("Key Tolerance (0..255)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Object Erase Regions (${uiState.imageEraseRegions.size})", style = MaterialTheme.typography.titleSmall)
                        Button(onClick = viewModel::addImageEraseRegion, enabled = !uiState.isWorking) {
                            Text("Add Erase Region")
                        }
                        uiState.imageEraseRegions.forEachIndexed { index, region ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Region ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = region.x,
                                        onValueChange = { viewModel.updateImageEraseRegionX(index, it) },
                                        label = { Text("X") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = region.y,
                                        onValueChange = { viewModel.updateImageEraseRegionY(index, it) },
                                        label = { Text("Y") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = region.width,
                                        onValueChange = { viewModel.updateImageEraseRegionWidth(index, it) },
                                        label = { Text("Width") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = region.height,
                                        onValueChange = { viewModel.updateImageEraseRegionHeight(index, it) },
                                        label = { Text("Height") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(onClick = { viewModel.removeImageEraseRegion(index) }, enabled = !uiState.isWorking) {
                                        Text("Remove Region")
                                    }
                                }
                            }
                        }
                        Text("Layers + Masks (${uiState.imageLayers.size})", style = MaterialTheme.typography.titleSmall)
                        Button(onClick = viewModel::addImageLayer, enabled = !uiState.isWorking) {
                            Text("Add Layer")
                        }
                        uiState.imageLayers.forEachIndexed { index, layer ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Layer ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = layer.layerPath,
                                        onValueChange = { viewModel.updateImageLayerPath(index, it) },
                                        label = { Text("Layer Path (local)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = layer.layerUrl,
                                        onValueChange = { viewModel.updateImageLayerUrl(index, it) },
                                        label = { Text("Layer URL (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = layer.maskPath,
                                        onValueChange = { viewModel.updateImageLayerMaskPath(index, it) },
                                        label = { Text("Mask Path (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = layer.maskUrl,
                                        onValueChange = { viewModel.updateImageLayerMaskUrl(index, it) },
                                        label = { Text("Mask URL (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = layer.opacity,
                                        onValueChange = { viewModel.updateImageLayerOpacity(index, it) },
                                        label = { Text("Opacity (0..1)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = layer.blendMode,
                                        onValueChange = { viewModel.updateImageLayerBlendMode(index, it) },
                                        label = { Text("Blend Mode (normal/multiply/screen/overlay)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = layer.x,
                                        onValueChange = { viewModel.updateImageLayerX(index, it) },
                                        label = { Text("Layer X") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = layer.y,
                                        onValueChange = { viewModel.updateImageLayerY(index, it) },
                                        label = { Text("Layer Y") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(onClick = { viewModel.removeImageLayer(index) }, enabled = !uiState.isWorking) {
                                        Text("Remove Layer")
                                    }
                                }
                            }
                        }
                        Text("Heal / Object Removal", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = uiState.imageHealMaskPath,
                            onValueChange = viewModel::updateImageHealMaskPath,
                            label = { Text("Heal Mask Path (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageHealMaskUrl,
                            onValueChange = viewModel::updateImageHealMaskUrl,
                            label = { Text("Heal Mask URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageHealOutputName,
                            onValueChange = viewModel::updateImageHealOutputName,
                            label = { Text("Heal Output Name (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageHealInpaintRadius,
                            onValueChange = viewModel::updateImageHealInpaintRadius,
                            label = { Text("Inpaint Radius") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageHealMethod,
                            onValueChange = viewModel::updateImageHealMethod,
                            label = { Text("Heal Method (telea/ns)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageHealFillStrategy,
                            onValueChange = viewModel::updateImageHealFillStrategy,
                            label = { Text("Fill Strategy (inpaint/median/blur)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageHealFeatherRadius,
                            onValueChange = viewModel::updateImageHealFeatherRadius,
                            label = { Text("Feather Radius") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = viewModel::toggleImageHealPreserveEdges, enabled = !uiState.isWorking) {
                            Text(if (uiState.imageHealPreserveEdges) "Preserve Edges: On" else "Preserve Edges: Off")
                        }
                        OutlinedTextField(
                            value = uiState.imageHealEdgeBlend,
                            onValueChange = viewModel::updateImageHealEdgeBlend,
                            label = { Text("Edge Blend (0..1)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.imageHealDenoiseStrength,
                            onValueChange = viewModel::updateImageHealDenoiseStrength,
                            label = { Text("Denoise Strength (0..1)") },
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
                            Button(onClick = viewModel::healSelectedImage, enabled = !uiState.isWorking) {
                                Text("Run Heal / Remove")
                            }
                            Button(
                                onClick = {
                                    if (uiState.imageOutputPath.isBlank()) {
                                        viewModel.onImageLocalMediaError("No local image output path to export yet.")
                                    } else {
                                        scope.launch {
                                            runCatching {
                                                withContext(Dispatchers.IO) {
                                                    LocalMediaFileManager.exportLocalFileToMediaStore(
                                                        context = context,
                                                        sourcePath = uiState.imageOutputPath,
                                                        kind = LocalMediaKind.IMAGE,
                                                        preferredDisplayName = uiState.imageOutputName
                                                    )
                                                }
                                            }.onSuccess { uri ->
                                                viewModel.onImageExported(uri.toString())
                                            }.onFailure {
                                                viewModel.onImageLocalMediaError("Image export failed: ${it.message}")
                                            }
                                        }
                                    }
                                },
                                enabled = !uiState.isWorking
                            ) {
                                Text("Export Image To Gallery")
                            }
                        }
                        if (uiState.imageStatusMessage.isNotBlank()) {
                            Text(uiState.imageStatusMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.imageLocalMediaMessage.isNotBlank()) {
                            Text(uiState.imageLocalMediaMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.imageOutputPath.isNotBlank()) {
                            Text("Image output: ${uiState.imageOutputPath}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.imageHealOutputPath.isNotBlank()) {
                            Text("Image heal output: ${uiState.imageHealOutputPath}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.imageExportUri.isNotBlank()) {
                            Text("Image gallery URI: ${uiState.imageExportUri}", style = MaterialTheme.typography.bodySmall)
                        }
                        }
                    }
                }
            }

            item {
                if (selectedMediaTab == MediaEditingTab.TIMELINE) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        Text("Timeline Editor", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Multiclip timeline + multitrack audio with trim/split/transitions/keyframes.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = uiState.timelineOutputName,
                            onValueChange = viewModel::updateTimelineOutputName,
                            label = { Text("Output Name (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.timelineTransitionDefault,
                            onValueChange = viewModel::updateTimelineTransitionDefault,
                            label = { Text("Default Transition (none/fade, optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = viewModel::toggleTimelineNormalizeLoudness, enabled = !uiState.isWorking) {
                            Text(if (uiState.timelineNormalizeLoudness) "Normalize Loudness: On" else "Normalize Loudness: Off")
                        }
                        OutlinedTextField(
                            value = uiState.timelineBackgroundAudioPath,
                            onValueChange = viewModel::updateTimelineBackgroundAudioPath,
                            label = { Text("Background Audio Path (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.timelineBackgroundAudioUrl,
                            onValueChange = viewModel::updateTimelineBackgroundAudioUrl,
                            label = { Text("Background Audio URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.timelineBackgroundAudioVolume,
                            onValueChange = viewModel::updateTimelineBackgroundAudioVolume,
                            label = { Text("Background Audio Volume (0.35 default)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                pendingTimelineImport = TimelineImportTarget.BackgroundAudio
                                importTimelineMediaLauncher.launch(arrayOf("audio/*"))
                            },
                            enabled = !uiState.isWorking
                        ) {
                            Text("Import Background Audio")
                        }

                        Text("Video Clips (${uiState.timelineClips.size})", style = MaterialTheme.typography.titleSmall)
                        Button(onClick = viewModel::addTimelineClip, enabled = !uiState.isWorking) {
                            Text("Add Clip")
                        }
                        uiState.timelineClips.forEachIndexed { index, clip ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Clip ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = clip.sourcePath,
                                        onValueChange = { viewModel.updateTimelineClipSourcePath(index, it) },
                                        label = { Text("Source Path (local)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = clip.sourceUrl,
                                        onValueChange = { viewModel.updateTimelineClipSourceUrl(index, it) },
                                        label = { Text("Source URL (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            pendingTimelineImport = TimelineImportTarget.Clip(index)
                                            importTimelineMediaLauncher.launch(arrayOf("video/*"))
                                        },
                                        enabled = !uiState.isWorking
                                    ) {
                                        Text("Import Clip")
                                    }
                                    OutlinedTextField(
                                        value = clip.startSeconds,
                                        onValueChange = { viewModel.updateTimelineClipStartSeconds(index, it) },
                                        label = { Text("Trim Start Seconds") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = clip.endSeconds,
                                        onValueChange = { viewModel.updateTimelineClipEndSeconds(index, it) },
                                        label = { Text("Trim End Seconds (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = clip.splitPointsCsv,
                                        onValueChange = { viewModel.updateTimelineClipSplitPoints(index, it) },
                                        label = { Text("Split Points CSV (e.g. 3.2,7.9)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = clip.speedFactor,
                                        onValueChange = { viewModel.updateTimelineClipSpeedFactor(index, it) },
                                        label = { Text("Speed Factor") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = clip.volume,
                                        onValueChange = { viewModel.updateTimelineClipVolume(index, it) },
                                        label = { Text("Clip Volume") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = clip.transitionToNext,
                                        onValueChange = { viewModel.updateTimelineClipTransition(index, it) },
                                        label = { Text("Transition To Next (none/fade)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = clip.transitionDuration,
                                        onValueChange = { viewModel.updateTimelineClipTransitionDuration(index, it) },
                                        label = { Text("Transition Duration Seconds") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { viewModel.updateTimelineClipTransition(index, "none") }, enabled = !uiState.isWorking) {
                                            Text("None")
                                        }
                                        Button(onClick = { viewModel.updateTimelineClipTransition(index, "fade") }, enabled = !uiState.isWorking) {
                                            Text("Fade")
                                        }
                                        Button(onClick = { viewModel.removeTimelineClip(index) }, enabled = !uiState.isWorking) {
                                            Text("Remove Clip")
                                        }
                                    }
                                }
                            }
                        }

                        Text("Audio Tracks (${uiState.timelineAudioTracks.size})", style = MaterialTheme.typography.titleSmall)
                        Button(onClick = viewModel::addTimelineAudioTrack, enabled = !uiState.isWorking) {
                            Text("Add Audio Track")
                        }
                        uiState.timelineAudioTracks.forEachIndexed { index, track ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Track ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = track.sourcePath,
                                        onValueChange = { viewModel.updateTimelineAudioTrackSourcePath(index, it) },
                                        label = { Text("Audio Path (local)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = track.sourceUrl,
                                        onValueChange = { viewModel.updateTimelineAudioTrackSourceUrl(index, it) },
                                        label = { Text("Audio URL (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            pendingTimelineImport = TimelineImportTarget.AudioTrack(index)
                                            importTimelineMediaLauncher.launch(arrayOf("audio/*"))
                                        },
                                        enabled = !uiState.isWorking
                                    ) {
                                        Text("Import Audio Track")
                                    }
                                    OutlinedTextField(
                                        value = track.startSeconds,
                                        onValueChange = { viewModel.updateTimelineAudioTrackStartSeconds(index, it) },
                                        label = { Text("Start Seconds") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = track.endSeconds,
                                        onValueChange = { viewModel.updateTimelineAudioTrackEndSeconds(index, it) },
                                        label = { Text("End Seconds (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = track.volume,
                                        onValueChange = { viewModel.updateTimelineAudioTrackVolume(index, it) },
                                        label = { Text("Track Volume") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(onClick = { viewModel.removeTimelineAudioTrack(index) }, enabled = !uiState.isWorking) {
                                        Text("Remove Track")
                                    }
                                }
                            }
                        }

                        Text("Keyframes (${uiState.timelineKeyframes.size})", style = MaterialTheme.typography.titleSmall)
                        Text("Supported properties: brightness, contrast, saturation", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = viewModel::addTimelineKeyframe, enabled = !uiState.isWorking) {
                            Text("Add Keyframe")
                        }
                        uiState.timelineKeyframes.forEachIndexed { index, keyframe ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Keyframe ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = keyframe.atSeconds,
                                        onValueChange = { viewModel.updateTimelineKeyframeAt(index, it) },
                                        label = { Text("At Seconds") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = keyframe.property,
                                        onValueChange = { viewModel.updateTimelineKeyframeProperty(index, it) },
                                        label = { Text("Property") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { viewModel.updateTimelineKeyframeProperty(index, "brightness") }, enabled = !uiState.isWorking) {
                                            Text("Brightness")
                                        }
                                        Button(onClick = { viewModel.updateTimelineKeyframeProperty(index, "contrast") }, enabled = !uiState.isWorking) {
                                            Text("Contrast")
                                        }
                                        Button(onClick = { viewModel.updateTimelineKeyframeProperty(index, "saturation") }, enabled = !uiState.isWorking) {
                                            Text("Saturation")
                                        }
                                    }
                                    OutlinedTextField(
                                        value = keyframe.value,
                                        onValueChange = { viewModel.updateTimelineKeyframeValue(index, it) },
                                        label = { Text("Value") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(onClick = { viewModel.removeTimelineKeyframe(index) }, enabled = !uiState.isWorking) {
                                        Text("Remove Keyframe")
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::composeTimeline, enabled = !uiState.isWorking) {
                                Text("Compose Timeline")
                            }
                            Button(
                                onClick = {
                                    if (uiState.timelineOutputPath.isBlank()) {
                                        viewModel.onTimelineLocalMediaError("No timeline output path to export yet.")
                                    } else {
                                        scope.launch {
                                            runCatching {
                                                withContext(Dispatchers.IO) {
                                                    LocalMediaFileManager.exportLocalFileToMediaStore(
                                                        context = context,
                                                        sourcePath = uiState.timelineOutputPath,
                                                        kind = LocalMediaKind.VIDEO,
                                                        preferredDisplayName = uiState.timelineOutputName
                                                    )
                                                }
                                            }.onSuccess { uri ->
                                                viewModel.onTimelineExported(uri.toString())
                                            }.onFailure {
                                                viewModel.onTimelineLocalMediaError("Timeline export failed: ${it.message}")
                                            }
                                        }
                                    }
                                },
                                enabled = !uiState.isWorking
                            ) {
                                Text("Export Timeline To Gallery")
                            }
                        }

                        if (uiState.timelineStatusMessage.isNotBlank()) {
                            Text(uiState.timelineStatusMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.timelineLocalMediaMessage.isNotBlank()) {
                            Text(uiState.timelineLocalMediaMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.timelineOutputPath.isNotBlank()) {
                            Text("Timeline output: ${uiState.timelineOutputPath}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.timelineExportUri.isNotBlank()) {
                            Text("Timeline gallery URI: ${uiState.timelineExportUri}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.timelineWarnings.isNotEmpty()) {
                            Text("Timeline warnings:", style = MaterialTheme.typography.bodySmall)
                            uiState.timelineWarnings.forEach { warning ->
                                Text("- $warning", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        }
                    }
                }
            }

            item {
                if (selectedMediaTab == MediaEditingTab.CAPTIONS) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        Text("Captions / Subtitles", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Auto-transcribe, edit subtitle cues, build SRT, then burn subtitles into output video.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = uiState.captionSourcePath,
                            onValueChange = viewModel::updateCaptionSourcePath,
                            label = { Text("Caption Source Path (optional; falls back to video source)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.captionSourceUrl,
                            onValueChange = viewModel::updateCaptionSourceUrl,
                            label = { Text("Caption Source URL (optional; falls back to video URL)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { importCaptionSourceLauncher.launch(arrayOf("video/*")) },
                            enabled = !uiState.isWorking
                        ) {
                            Text("Import Caption Source Video")
                        }
                        OutlinedTextField(
                            value = uiState.captionLanguage,
                            onValueChange = viewModel::updateCaptionLanguage,
                            label = { Text("Language (e.g., en)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.captionOutputName,
                            onValueChange = viewModel::updateCaptionOutputName,
                            label = { Text("Subtitle Output Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::transcribeCaptions, enabled = !uiState.isWorking) {
                                Text("Auto-Transcribe")
                            }
                            Button(onClick = viewModel::buildSrtFromCues, enabled = !uiState.isWorking) {
                                Text("Build SRT From Cues")
                            }
                        }

                        Text("Subtitle Cues (${uiState.captionCues.size})", style = MaterialTheme.typography.titleSmall)
                        Button(onClick = viewModel::addCaptionCue, enabled = !uiState.isWorking) {
                            Text("Add Cue")
                        }
                        uiState.captionCues.forEachIndexed { index, cue ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Cue ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = cue.startSeconds,
                                        onValueChange = { viewModel.updateCaptionCueStart(index, it) },
                                        label = { Text("Start Seconds") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = cue.endSeconds,
                                        onValueChange = { viewModel.updateCaptionCueEnd(index, it) },
                                        label = { Text("End Seconds") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = cue.text,
                                        onValueChange = { viewModel.updateCaptionCueText(index, it) },
                                        label = { Text("Subtitle Text") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(onClick = { viewModel.removeCaptionCue(index) }, enabled = !uiState.isWorking) {
                                        Text("Remove Cue")
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.captionSubtitlePath,
                            onValueChange = viewModel::updateCaptionSubtitlePath,
                            label = { Text("Subtitle Path For Burn-In (SRT)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.captionSubtitleUrl,
                            onValueChange = viewModel::updateCaptionSubtitleUrl,
                            label = { Text("Subtitle URL For Burn-In (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { importSubtitleLauncher.launch(arrayOf("*/*")) },
                            enabled = !uiState.isWorking
                        ) {
                            Text("Import Subtitle File (.srt)")
                        }
                        OutlinedTextField(
                            value = uiState.captionBurnOutputName,
                            onValueChange = viewModel::updateCaptionBurnOutputName,
                            label = { Text("Burned Video Output Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::burnSubtitles, enabled = !uiState.isWorking) {
                                Text("Burn Subtitles Into Video")
                            }
                            Button(
                                onClick = {
                                    if (uiState.captionBurnOutputPath.isBlank()) {
                                        viewModel.onCaptionLocalMediaError("No burned subtitle video path to export yet.")
                                    } else {
                                        scope.launch {
                                            runCatching {
                                                withContext(Dispatchers.IO) {
                                                    LocalMediaFileManager.exportLocalFileToMediaStore(
                                                        context = context,
                                                        sourcePath = uiState.captionBurnOutputPath,
                                                        kind = LocalMediaKind.VIDEO,
                                                        preferredDisplayName = uiState.captionBurnOutputName
                                                    )
                                                }
                                            }.onSuccess { uri ->
                                                viewModel.onCaptionBurnExported(uri.toString())
                                            }.onFailure {
                                                viewModel.onCaptionLocalMediaError("Subtitle burn export failed: ${it.message}")
                                            }
                                        }
                                    }
                                },
                                enabled = !uiState.isWorking
                            ) {
                                Text("Export Burned Video")
                            }
                        }

                        if (uiState.captionStatusMessage.isNotBlank()) {
                            Text(uiState.captionStatusMessage, style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.captionEngine.isNotBlank()) {
                            Text("Engine: ${uiState.captionEngine}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.captionCueCount > 0) {
                            Text("Cue count: ${uiState.captionCueCount}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.captionGeneratedSrtPath.isNotBlank()) {
                            Text("Generated SRT: ${uiState.captionGeneratedSrtPath}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.captionBurnOutputPath.isNotBlank()) {
                            Text("Burned video output: ${uiState.captionBurnOutputPath}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.captionBurnExportUri.isNotBlank()) {
                            Text("Burned video gallery URI: ${uiState.captionBurnExportUri}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (uiState.captionLocalMediaMessage.isNotBlank()) {
                            Text(uiState.captionLocalMediaMessage, style = MaterialTheme.typography.bodySmall)
                        }
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
                if (selectedMediaTab == MediaEditingTab.PRESETS) {
                    Text("Video Presets", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (selectedMediaTab == MediaEditingTab.PRESETS) {
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
            }

            item {
                if (selectedMediaTab == MediaEditingTab.PRESETS) {
                    Text("Image Presets", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (selectedMediaTab == MediaEditingTab.PRESETS) {
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
}
