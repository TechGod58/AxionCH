package com.axionch.app.ui.screens.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.axionch.app.domain.model.NavRoute

@Composable
fun MediaStudioScreen(
    navController: NavController,
    viewModel: MediaStudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("video", "timeline", "captions", "pro", "live", "assets")
    val tabIndex = tabs.indexOf(uiState.selectedTab).takeIf { it >= 0 } ?: 0

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Media Studio", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                Text("Templates: ${uiState.templatesSummary}", style = MaterialTheme.typography.bodySmall)
            }
            item {
                Text("Export presets: ${uiState.presetsSummary}", style = MaterialTheme.typography.bodySmall)
            }
            item {
                PrimaryTabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { viewModel.setTab(tab) },
                            text = { Text(tab.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.sourcePath,
                    onValueChange = viewModel::updateSourcePath,
                    label = { Text("Source Path") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.sourceUrl,
                    onValueChange = viewModel::updateSourceUrl,
                    label = { Text("Source URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (uiState.selectedTab) {
                "video" -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::loadMediaCatalog, enabled = !uiState.isWorking) {
                                Text("Load Catalog")
                            }
                            Button(onClick = viewModel::queueExport, enabled = !uiState.isWorking) {
                                Text("Queue Export")
                            }
                            Button(
                                onClick = { navController.navigate(NavRoute.RealtimeCapture.route) },
                                enabled = !uiState.isWorking
                            ) {
                                Text("Live Capture")
                            }
                        }
                    }
                    if (uiState.exportJobId.isNotBlank()) {
                        item { Text("Export Job ID: ${uiState.exportJobId}") }
                        item { Text("Export Job Status: ${uiState.exportJobStatus}") }
                        item {
                            Button(onClick = viewModel::refreshExportJob, enabled = !uiState.isWorking) {
                                Text("Refresh Export Status")
                            }
                        }
                    }
                }

                "timeline" -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::addTimelineClip, enabled = !uiState.isWorking) {
                                Text("Add Clip")
                            }
                            Button(onClick = viewModel::composeTimelineAdvanced, enabled = !uiState.isWorking) {
                                Text("Compose Timeline")
                            }
                        }
                    }
                    items(uiState.timelineClips, key = { it.id }) { clip ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Clip #${clip.id}", style = MaterialTheme.typography.titleSmall)
                                OutlinedTextField(
                                    value = clip.sourcePath,
                                    onValueChange = { viewModel.updateTimelineClipSourcePath(clip.id, it) },
                                    label = { Text("Clip Source Path") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = clip.sourceUrl,
                                    onValueChange = { viewModel.updateTimelineClipSourceUrl(clip.id, it) },
                                    label = { Text("Clip Source URL") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = clip.startSeconds,
                                        onValueChange = { viewModel.updateTimelineClipStartSeconds(clip.id, it) },
                                        label = { Text("Start (sec)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = clip.endSeconds,
                                        onValueChange = { viewModel.updateTimelineClipEndSeconds(clip.id, it) },
                                        label = { Text("End (sec)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = clip.speedFactor,
                                        onValueChange = { viewModel.updateTimelineClipSpeedFactor(clip.id, it) },
                                        label = { Text("Speed") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = clip.volume,
                                        onValueChange = { viewModel.updateTimelineClipVolume(clip.id, it) },
                                        label = { Text("Volume") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = clip.splitPointsCsv,
                                    onValueChange = { viewModel.updateTimelineClipSplitPointsCsv(clip.id, it) },
                                    label = { Text("Split points CSV (sec)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = clip.transitionToNext,
                                        onValueChange = { viewModel.updateTimelineClipTransitionToNext(clip.id, it) },
                                        label = { Text("Transition to next") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = clip.transitionDuration,
                                        onValueChange = { viewModel.updateTimelineClipTransitionDuration(clip.id, it) },
                                        label = { Text("Transition dur (sec)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.duplicateTimelineClip(clip.id) }, enabled = !uiState.isWorking) {
                                        Text("Duplicate")
                                    }
                                    Button(onClick = { viewModel.removeTimelineClip(clip.id) }, enabled = !uiState.isWorking) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.audioTrackPath,
                            onValueChange = viewModel::updateAudioTrackPath,
                            label = { Text("Audio Track Path (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.timelineOutputName,
                            onValueChange = viewModel::updateTimelineOutputName,
                            label = { Text("Timeline Output Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.brightnessKeyframesCsv,
                            onValueChange = viewModel::updateBrightnessKeyframesCsv,
                            label = { Text("Brightness keyframes: t:v; t:v") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.contrastKeyframesCsv,
                            onValueChange = viewModel::updateContrastKeyframesCsv,
                            label = { Text("Contrast keyframes: t:v; t:v") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.saturationKeyframesCsv,
                            onValueChange = viewModel::updateSaturationKeyframesCsv,
                            label = { Text("Saturation keyframes: t:v; t:v") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                "captions" -> {
                    item {
                        OutlinedTextField(
                            value = uiState.captionLanguage,
                            onValueChange = viewModel::updateCaptionLanguage,
                            label = { Text("Caption Language (en)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.captionSrtOutputName,
                            onValueChange = viewModel::updateCaptionOutputName,
                            label = { Text("Subtitle Output Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::transcribeCaptions, enabled = !uiState.isWorking) {
                                Text("Auto-Transcribe")
                            }
                            Button(onClick = viewModel::buildSampleSrt, enabled = !uiState.isWorking) {
                                Text("Build Sample SRT")
                            }
                        }
                    }
                    item {
                        Text(uiState.captionResultText, style = MaterialTheme.typography.bodySmall)
                    }
                }

                "pro" -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::runMotionTrack, enabled = !uiState.isWorking) {
                                Text("Run Motion Tracking")
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.motionRoiX,
                                onValueChange = viewModel::updateMotionRoiX,
                                label = { Text("ROI X") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.motionRoiY,
                                onValueChange = viewModel::updateMotionRoiY,
                                label = { Text("ROI Y") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.motionRoiWidth,
                                onValueChange = viewModel::updateMotionRoiWidth,
                                label = { Text("ROI W") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.motionRoiHeight,
                                onValueChange = viewModel::updateMotionRoiHeight,
                                label = { Text("ROI H") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.motionSmoothingWindow,
                                onValueChange = viewModel::updateMotionSmoothingWindow,
                                label = { Text("Smoothing Window") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.motionMinConfidence,
                                onValueChange = viewModel::updateMotionMinConfidence,
                                label = { Text("Min Confidence") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.motionOverlayOutputName,
                            onValueChange = viewModel::updateMotionOverlayOutputName,
                            label = { Text("Overlay video output name (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (uiState.motionTrackSummary.isNotBlank()) {
                        item {
                            Text(uiState.motionTrackSummary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.healMaskPath,
                            onValueChange = viewModel::updateHealMaskPath,
                            label = { Text("Heal Mask Path (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.healRegionsCsv,
                            onValueChange = viewModel::updateHealRegionsCsv,
                            label = { Text("Heal Regions x,y,w,h; x,y,w,h") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.healOutputName,
                            onValueChange = viewModel::updateHealOutputName,
                            label = { Text("Heal Output Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.healFillStrategy,
                                onValueChange = viewModel::updateHealFillStrategy,
                                label = { Text("Fill Strategy (inpaint/hybrid)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.healEdgeBlend,
                                onValueChange = viewModel::updateHealEdgeBlend,
                                label = { Text("Edge Blend") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.healDenoiseStrength,
                            onValueChange = viewModel::updateHealDenoiseStrength,
                            label = { Text("Denoise Strength (0..20)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Button(onClick = viewModel::runImageHeal, enabled = !uiState.isWorking) {
                            Text("Run Image Heal")
                        }
                    }
                    if (uiState.healSummary.isNotBlank()) {
                        item {
                            Text(uiState.healSummary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                "live" -> {
                    item {
                        Text(
                            "Multicast one live source to multiple social platforms at the same time.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.liveSessionName,
                            onValueChange = viewModel::updateLiveSessionName,
                            label = { Text("Session Name (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.liveSourcePath,
                            onValueChange = viewModel::updateLiveSourcePath,
                            label = { Text("Live Source Path (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.liveSourceUrl,
                            onValueChange = viewModel::updateLiveSourceUrl,
                            label = { Text("Live Source URL (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.liveXIngestUrl,
                            onValueChange = viewModel::updateLiveXIngestUrl,
                            label = { Text("X Ingest URL (rtmp/rtmps)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.liveXStreamKey,
                            onValueChange = viewModel::updateLiveXStreamKey,
                            label = { Text("X Stream Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.liveLinkedinIngestUrl,
                            onValueChange = viewModel::updateLiveLinkedinIngestUrl,
                            label = { Text("LinkedIn Ingest URL (rtmp/rtmps)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.liveLinkedinStreamKey,
                            onValueChange = viewModel::updateLiveLinkedinStreamKey,
                            label = { Text("LinkedIn Stream Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.liveInstagramIngestUrl,
                            onValueChange = viewModel::updateLiveInstagramIngestUrl,
                            label = { Text("Instagram Ingest URL (rtmp/rtmps)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.liveInstagramStreamKey,
                            onValueChange = viewModel::updateLiveInstagramStreamKey,
                            label = { Text("Instagram Stream Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.liveVideoBitrate,
                                onValueChange = viewModel::updateLiveVideoBitrate,
                                label = { Text("Video Bitrate") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.liveAudioBitrate,
                                onValueChange = viewModel::updateLiveAudioBitrate,
                                label = { Text("Audio Bitrate") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.liveFps,
                                onValueChange = viewModel::updateLiveFps,
                                label = { Text("FPS") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.liveGopSeconds,
                                onValueChange = viewModel::updateLiveGopSeconds,
                                label = { Text("GOP Seconds") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::toggleLiveDryRun, enabled = !uiState.isWorking) {
                                Text(if (uiState.liveDryRun) "Dry Run: On" else "Dry Run: Off")
                            }
                            Button(onClick = viewModel::toggleLiveCopyVideo, enabled = !uiState.isWorking) {
                                Text(if (uiState.liveCopyVideo) "Copy Video: On" else "Copy Video: Off")
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::startLiveMulticast, enabled = !uiState.isWorking) {
                                Text("Start Multicast")
                            }
                            Button(onClick = viewModel::refreshLiveSessions, enabled = !uiState.isWorking) {
                                Text("Refresh Sessions")
                            }
                            Button(
                                onClick = { viewModel.stopLiveMulticast() },
                                enabled = !uiState.isWorking && uiState.liveCurrentSessionId.isNotBlank()
                            ) {
                                Text("Stop Current")
                            }
                        }
                    }
                    if (uiState.liveStatusSummary.isNotBlank()) {
                        item {
                            Text(uiState.liveStatusSummary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (uiState.liveSessions.isEmpty()) {
                        item {
                            Text("No live multicast sessions yet.", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        items(uiState.liveSessions, key = { it.session_id }) { session ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(session.session_name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Status=${session.status} | dryRun=${session.dry_run} | active=${session.active_destinations} failed=${session.failed_destinations}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text("Source: ${session.source_kind} -> ${session.source_value}", style = MaterialTheme.typography.bodySmall)
                                    session.destinations.forEach { dest ->
                                        Text(
                                            "- ${dest.platform}: ${dest.status} (${dest.ingest_url_masked})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.stopLiveMulticast(session.session_id) },
                                        enabled = !uiState.isWorking && session.status !in listOf("stopped", "failed")
                                    ) {
                                        Text("Stop Session")
                                    }
                                }
                            }
                        }
                    }
                }

                "assets" -> {
                    item {
                        OutlinedTextField(
                            value = uiState.assetKind,
                            onValueChange = viewModel::updateAssetKind,
                            label = { Text("Asset Kind (audio/overlay/font)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.assetTitle,
                            onValueChange = viewModel::updateAssetTitle,
                            label = { Text("Asset Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.assetSource,
                            onValueChange = viewModel::updateAssetSource,
                            label = { Text("Asset Source") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.assetLicense,
                            onValueChange = viewModel::updateAssetLicense,
                            label = { Text("License Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.assetUrlOrPath,
                            onValueChange = viewModel::updateAssetUrlOrPath,
                            label = { Text("Local Path or URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::saveAsset, enabled = !uiState.isWorking) {
                                Text("Save Asset")
                            }
                            Button(onClick = viewModel::loadAssets, enabled = !uiState.isWorking) {
                                Text("Load Assets")
                            }
                        }
                    }
                    item {
                        Text(uiState.assetSummary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (uiState.isWorking) {
                item {
                    CircularProgressIndicator()
                }
            }
            if (uiState.message.isNotBlank()) {
                item {
                    Text(uiState.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
