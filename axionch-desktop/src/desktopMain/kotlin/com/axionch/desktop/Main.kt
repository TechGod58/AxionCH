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
import com.axionch.shared.api.AssetItem
import com.axionch.shared.api.AssetUpsertRequest
import com.axionch.shared.api.AxionApiClient
import com.axionch.shared.api.CaptionCue
import com.axionch.shared.api.CaptionSrtBuildRequest
import com.axionch.shared.api.CaptionTranscribeRequest
import com.axionch.shared.api.CreateAccountRequest
import com.axionch.shared.api.CreatorTemplate
import com.axionch.shared.api.CreatePostRequest
import com.axionch.shared.api.DeadLetterItem
import com.axionch.shared.api.DryRunHistoryItem
import com.axionch.shared.api.ExportPreset
import com.axionch.shared.api.ExportQueueRequest
import com.axionch.shared.api.ImageEraseRegion
import com.axionch.shared.api.ImageFilterApplyRequest
import com.axionch.shared.api.ImageHealRequest
import com.axionch.shared.api.ImageLayer
import com.axionch.shared.api.LiveDestinationConfig
import com.axionch.shared.api.LiveMulticastSessionResponse
import com.axionch.shared.api.LiveMulticastStartRequest
import com.axionch.shared.api.MotionTrackRequest
import com.axionch.shared.api.PlatformConfigStatus
import com.axionch.shared.api.PostResponse
import com.axionch.shared.api.PublishResultItem
import com.axionch.shared.api.TimelineAudioTrack
import com.axionch.shared.api.TimelineClip
import com.axionch.shared.api.TimelineComposeRequest
import com.axionch.shared.api.TimelineComposeResponse
import com.axionch.shared.api.TimelineKeyframe
import com.axionch.shared.api.VideoFilterApplyRequest
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private data class TimelineClipDraft(
    val sourcePath: String = "",
    val sourceUrl: String = "",
    val startSeconds: String = "0.0",
    val endSeconds: String = "",
    val speedFactor: String = "1.0",
    val volume: String = "1.0",
    val splitPointsCsv: String = "",
    val transitionToNext: String = "",
    val transitionDuration: String = "0.35"
)

private data class TimelineAudioTrackDraft(
    val sourcePath: String = "",
    val sourceUrl: String = "",
    val startSeconds: String = "0.0",
    val endSeconds: String = "",
    val volume: String = "1.0"
)

private data class TimelineKeyframeDraft(
    val atSeconds: String = "",
    val property: String = "",
    val value: String = ""
)

private data class CaptionCueDraft(
    val startSeconds: String = "",
    val endSeconds: String = "",
    val text: String = ""
)

private data class ImageEraseRegionDraft(
    val x: String = "",
    val y: String = "",
    val width: String = "",
    val height: String = ""
)

private data class ImageLayerDraft(
    val layerPath: String = "",
    val layerUrl: String = "",
    val maskPath: String = "",
    val maskUrl: String = "",
    val opacity: String = "1.0",
    val blendMode: String = "normal",
    val x: String = "0",
    val y: String = "0"
)

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
    val creatorTemplates: List<CreatorTemplate> = emptyList(),
    val templatePlatformFilter: String = "",
    val templateMediaTypeFilter: String = "",
    val selectedCreatorTemplateId: String = "",
    val activeCreatorTemplateText: String = "",
    val templateStatusText: String = "",
    val exportPresets: List<ExportPreset> = emptyList(),
    val selectedExportPresetId: String = "",
    val exportSourcePathsInput: String = "",
    val exportSourceUrlsInput: String = "",
    val exportOutputPrefix: String = "",
    val exportUseHardwareAcceleration: Boolean = true,
    val exportQueuedJobId: String = "",
    val exportLastStatus: String = "",
    val exportQueuedItems: Int = 0,
    val exportCompletedItems: Int = 0,
    val exportFailedItems: Int = 0,
    val exportItemSummaries: List<String> = emptyList(),
    val exportStatusText: String = "",
    val assets: List<AssetItem> = emptyList(),
    val assetKindFilter: String = "",
    val assetTagFilter: String = "",
    val assetTitleInput: String = "",
    val assetSourceInput: String = "",
    val assetLicenseNameInput: String = "",
    val assetLicenseUrlInput: String = "",
    val assetAttributionRequired: Boolean = false,
    val assetAttributionTextInput: String = "",
    val assetTagsInput: String = "",
    val assetLocalPathInput: String = "",
    val assetRemoteUrlInput: String = "",
    val assetStatusText: String = "",
    val timelineClips: List<TimelineClipDraft> = listOf(TimelineClipDraft()),
    val timelineAudioTracks: List<TimelineAudioTrackDraft> = emptyList(),
    val timelineKeyframes: List<TimelineKeyframeDraft> = emptyList(),
    val timelineTransitionDefault: String = "",
    val timelineNormalizeLoudness: Boolean = false,
    val timelineOutputName: String = "",
    val timelineBackgroundAudioPath: String = "",
    val timelineBackgroundAudioUrl: String = "",
    val timelineBackgroundAudioVolume: String = "0.35",
    val timelineStatusText: String = "",
    val timelineOutputPath: String = "",
    val timelineWarnings: List<String> = emptyList(),
    val captionSourcePath: String = "",
    val captionSourceUrl: String = "",
    val captionLanguage: String = "en",
    val captionOutputName: String = "captions_auto",
    val captionCues: List<CaptionCueDraft> = listOf(CaptionCueDraft()),
    val captionGeneratedSrtPath: String = "",
    val captionSubtitlePath: String = "",
    val captionSubtitleUrl: String = "",
    val captionBurnOutputName: String = "caption_burned",
    val captionBurnOutputPath: String = "",
    val captionStatusText: String = "",
    val captionEngine: String = "",
    val captionCueCount: Int = 0,
    val audioFinSourcePath: String = "",
    val audioFinSourceUrl: String = "",
    val audioFinOutputName: String = "audio_finished",
    val audioFinSoundPath: String = "",
    val audioFinSoundUrl: String = "",
    val audioFinSoundVolume: String = "1.0",
    val audioFinDenoise: Boolean = false,
    val audioFinEqLowGain: String = "0.0",
    val audioFinEqMidGain: String = "0.0",
    val audioFinEqHighGain: String = "0.0",
    val audioFinCompressor: Boolean = false,
    val audioFinLimiter: Boolean = false,
    val audioFinGate: Boolean = false,
    val audioFinDucking: Boolean = false,
    val audioFinLoudnessNormalization: Boolean = false,
    val audioFinStatusText: String = "",
    val audioFinOutputPath: String = "",
    val proVideoSourcePath: String = "",
    val proVideoSourceUrl: String = "",
    val proVideoOutputName: String = "pro_video",
    val proVideoSpeedFactor: String = "1.0",
    val proVideoStabilize: Boolean = false,
    val proVideoCurvesPreset: String = "",
    val proVideoLutPath: String = "",
    val proVideoLutUrl: String = "",
    val proVideoOverlayPath: String = "",
    val proVideoOverlayUrl: String = "",
    val proVideoTextOverlay: String = "",
    val proVideoStatusText: String = "",
    val proVideoOutputPath: String = "",
    val proMotionStartSeconds: String = "0.0",
    val proMotionEndSeconds: String = "",
    val proMotionRoiX: String = "",
    val proMotionRoiY: String = "",
    val proMotionRoiWidth: String = "",
    val proMotionRoiHeight: String = "",
    val proMotionSmoothingWindow: String = "4",
    val proMotionMinConfidence: String = "0.05",
    val proMotionOverlayOutputName: String = "",
    val proMotionSummary: String = "",
    val photoSourcePath: String = "",
    val photoSourceUrl: String = "",
    val photoOutputName: String = "photo_pro",
    val photoPresetId: String = "",
    val photoOverlayPath: String = "",
    val photoOverlayUrl: String = "",
    val photoTextOverlay: String = "",
    val photoSepia: Boolean = false,
    val photoBlackWhite: Boolean = false,
    val photoCartoonify: Boolean = false,
    val photoCaricature: Boolean = false,
    val photoSaturationFactor: String = "1.0",
    val photoBrightnessFactor: String = "1.0",
    val photoContrastFactor: String = "1.0",
    val photoRawEnhance: Boolean = false,
    val photoSelectiveX: String = "",
    val photoSelectiveY: String = "",
    val photoSelectiveWidth: String = "",
    val photoSelectiveHeight: String = "",
    val photoSelectiveBrightnessFactor: String = "1.0",
    val photoSelectiveContrastFactor: String = "1.0",
    val photoSelectiveSaturationFactor: String = "1.0",
    val photoBackgroundRemove: Boolean = false,
    val photoBackgroundKeyColor: String = "#00FF00",
    val photoBackgroundKeyTolerance: String = "24",
    val photoEraseRegions: List<ImageEraseRegionDraft> = emptyList(),
    val photoLayers: List<ImageLayerDraft> = emptyList(),
    val photoHealMaskPath: String = "",
    val photoHealMaskUrl: String = "",
    val photoHealOutputName: String = "photo_heal",
    val photoHealInpaintRadius: String = "3.0",
    val photoHealMethod: String = "telea",
    val photoHealFillStrategy: String = "inpaint",
    val photoHealFeatherRadius: String = "3",
    val photoHealPreserveEdges: Boolean = true,
    val photoHealEdgeBlend: String = "0.55",
    val photoHealDenoiseStrength: String = "0.0",
    val photoStatusText: String = "",
    val photoOutputPath: String = "",
    val photoHealOutputPath: String = "",
    val liveMulticastSessionName: String = "",
    val liveMulticastSourcePath: String = "",
    val liveMulticastSourceUrl: String = "",
    val liveMulticastXIngestUrl: String = "",
    val liveMulticastXStreamKey: String = "",
    val liveMulticastLinkedinIngestUrl: String = "",
    val liveMulticastLinkedinStreamKey: String = "",
    val liveMulticastInstagramIngestUrl: String = "",
    val liveMulticastInstagramStreamKey: String = "",
    val liveMulticastDryRun: Boolean = true,
    val liveMulticastCopyVideo: Boolean = false,
    val liveMulticastVideoBitrate: String = "4500k",
    val liveMulticastAudioBitrate: String = "160k",
    val liveMulticastFps: String = "30",
    val liveMulticastGopSeconds: String = "2.0",
    val liveMulticastCurrentSessionId: String = "",
    val liveMulticastStatusText: String = "",
    val liveMulticastSessions: List<LiveMulticastSessionResponse> = emptyList(),
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

    fun refreshCreatorTemplates() {
        val platform = state.templatePlatformFilter.trim().ifBlank { null }
        val mediaType = state.templateMediaTypeFilter.trim().ifBlank { null }
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.getCreatorTemplates(platform = platform, mediaType = mediaType) }
                .onSuccess { response ->
                    state = state.copy(
                        creatorTemplates = response.templates,
                        selectedCreatorTemplateId = state.selectedCreatorTemplateId.ifBlank {
                            response.templates.firstOrNull()?.id.orEmpty()
                        },
                        templateStatusText = "Loaded ${response.total_returned} templates.",
                        isWorking = false,
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Loading creator templates failed: ${it.message}"
                    )
                }
        }
    }

    fun applyTemplateQuickFilter(key: String) {
        state = when (key.lowercase()) {
            "reels" -> state.copy(templatePlatformFilter = "instagram", templateMediaTypeFilter = "video")
            "shorts" -> state.copy(templatePlatformFilter = "youtube", templateMediaTypeFilter = "video")
            "tiktok" -> state.copy(templatePlatformFilter = "tiktok", templateMediaTypeFilter = "video")
            else -> state.copy(templatePlatformFilter = "", templateMediaTypeFilter = "")
        }
        refreshCreatorTemplates()
    }

    fun applySelectedCreatorTemplate() {
        val template = state.creatorTemplates.firstOrNull { it.id == state.selectedCreatorTemplateId }
        if (template == null) {
            setError("Select a creator template first.")
            return
        }
        val platformSet = template.platforms.map { it.lowercase() }.toSet()
        val exportPresetId = state.exportPresets.firstOrNull { preset ->
            preset.media_type.equals(template.media_type, ignoreCase = true) &&
                preset.width == template.width &&
                preset.height == template.height &&
                platformSet.contains(preset.platform.lowercase())
        }?.id ?: state.exportPresets.firstOrNull { preset ->
            preset.media_type.equals(template.media_type, ignoreCase = true) &&
                platformSet.contains(preset.platform.lowercase())
        }?.id.orEmpty()
        val resolutionHint = "${template.width}x${template.height}${template.fps?.let { " @${it}fps" } ?: ""}"
        state = when (template.media_type.lowercase()) {
            "video" -> state.copy(
                proVideoOutputName = template.id,
                timelineOutputName = "${template.id}-timeline",
                captionOutputName = "${template.id}-captions",
                captionBurnOutputName = "${template.id}-burned",
                selectedExportPresetId = if (exportPresetId.isNotBlank()) exportPresetId else state.selectedExportPresetId,
                exportOutputPrefix = template.id,
                activeCreatorTemplateText = "Active template: ${template.name} ($resolutionHint) for ${template.platforms.joinToString()}",
                templateStatusText = "Applied ${template.name} to video/timeline defaults."
            )
            else -> state.copy(
                photoOutputName = template.id,
                selectedExportPresetId = if (exportPresetId.isNotBlank()) exportPresetId else state.selectedExportPresetId,
                exportOutputPrefix = template.id,
                activeCreatorTemplateText = "Active template: ${template.name} ($resolutionHint) for ${template.platforms.joinToString()}",
                templateStatusText = "Applied ${template.name} to photo defaults."
            )
        }
    }

    fun parseBulkList(input: String): List<String> =
        input
            .split('\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    fun refreshExportPresets() {
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.getExportPresets() }
                .onSuccess { response ->
                    state = state.copy(
                        exportPresets = response.presets,
                        selectedExportPresetId = state.selectedExportPresetId.ifBlank {
                            response.presets.firstOrNull()?.id.orEmpty()
                        },
                        exportStatusText = "Loaded ${response.total_returned} export presets.",
                        isWorking = false,
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Loading export presets failed: ${it.message}"
                    )
                }
        }
    }

    fun queueBatchExport() {
        if (state.selectedExportPresetId.isBlank()) {
            setError("Select an export preset first.")
            return
        }
        val sourcePaths = parseBulkList(state.exportSourcePathsInput)
        val sourceUrls = parseBulkList(state.exportSourceUrlsInput)
        if (sourcePaths.isEmpty() && sourceUrls.isEmpty()) {
            setError("Provide at least one source path or source URL for batch export.")
            return
        }

        scope.launch {
            state = state.copy(isWorking = true, exportStatusText = "Queueing export job...")
            runCatching {
                api.queueExportJob(
                    ExportQueueRequest(
                        source_paths = sourcePaths,
                        source_urls = sourceUrls,
                        preset_id = state.selectedExportPresetId,
                        output_prefix = state.exportOutputPrefix.trim().ifBlank { null },
                        hardware_acceleration = state.exportUseHardwareAcceleration
                    )
                )
            }.onSuccess { response ->
                state = state.copy(
                    isWorking = false,
                    exportQueuedJobId = response.job_id,
                    exportLastStatus = response.status,
                    exportQueuedItems = response.queued_items,
                    exportCompletedItems = 0,
                    exportFailedItems = 0,
                    exportItemSummaries = emptyList(),
                    exportStatusText = response.message,
                    errorText = null
                )
                runCatching { api.getExportJobStatus(response.job_id) }
                    .onSuccess { job ->
                        val summaries = job.items.map { item ->
                            val outputPart = item.output_path?.let { " -> $it" } ?: ""
                            "${item.index}. ${item.status}: ${item.source}$outputPart${item.message?.let { " (${it})" } ?: ""}"
                        }
                        state = state.copy(
                            exportLastStatus = job.status,
                            exportQueuedItems = job.queued_items,
                            exportCompletedItems = job.completed_items,
                            exportFailedItems = job.failed_items,
                            exportItemSummaries = summaries,
                            exportStatusText = "Export job ${job.job_id}: ${job.status}",
                            errorText = null
                        )
                    }
            }.onFailure {
                state = state.copy(
                    isWorking = false,
                    exportStatusText = "Queue export failed: ${it.message}"
                )
            }
        }
    }

    fun refreshExportJobStatus() {
        val jobId = state.exportQueuedJobId.trim()
        if (jobId.isBlank()) {
            setError("No export job queued yet.")
            return
        }
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.getExportJobStatus(jobId) }
                .onSuccess { response ->
                    val summaries = response.items.map { item ->
                        val outputPart = item.output_path?.let { " -> $it" } ?: ""
                        "${item.index}. ${item.status}: ${item.source}$outputPart${item.message?.let { " (${it})" } ?: ""}"
                    }
                    state = state.copy(
                        isWorking = false,
                        exportLastStatus = response.status,
                        exportQueuedItems = response.queued_items,
                        exportCompletedItems = response.completed_items,
                        exportFailedItems = response.failed_items,
                        exportItemSummaries = summaries,
                        exportStatusText = "Export job ${response.job_id}: ${response.status}",
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        exportStatusText = "Export status check failed: ${it.message}"
                    )
                }
        }
    }

    fun mergeAttributionText(existing: String, asset: AssetItem): String {
        if (!asset.attribution_required) return existing
        val attribution = asset.attribution_text?.trim().orEmpty()
        if (attribution.isBlank()) return existing
        val current = existing.trim()
        if (current.contains(attribution, ignoreCase = true)) {
            return existing
        }
        return if (current.isBlank()) {
            attribution
        } else {
            "$existing\n$attribution"
        }
    }

    fun refreshAssets() {
        val kind = state.assetKindFilter.trim().ifBlank { null }
        val tag = state.assetTagFilter.trim().ifBlank { null }
        scope.launch {
            state = state.copy(isWorking = true, assetStatusText = "Loading asset library...")
            runCatching { api.getAssets(kind = kind, tag = tag) }
                .onSuccess { response ->
                    state = state.copy(
                        isWorking = false,
                        assets = response.items,
                        assetStatusText = "Loaded ${response.total_returned} assets.",
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        assetStatusText = "Loading assets failed: ${it.message}"
                    )
                }
        }
    }

    fun applyAssetQuickFilter(kind: String?) {
        state = state.copy(assetKindFilter = kind.orEmpty(), assetTagFilter = "")
        refreshAssets()
    }

    fun saveAsset() {
        val kind = state.assetKindFilter.trim().lowercase()
        if (kind.isBlank()) {
            setError("Asset kind is required (audio/overlay).")
            return
        }
        if (state.assetTitleInput.trim().isBlank()) {
            setError("Asset title is required.")
            return
        }
        if (state.assetSourceInput.trim().isBlank()) {
            setError("Asset source is required.")
            return
        }
        if (state.assetLicenseNameInput.trim().isBlank()) {
            setError("License name is required.")
            return
        }
        val localPath = state.assetLocalPathInput.trim().ifBlank { null }
        val remoteUrl = state.assetRemoteUrlInput.trim().ifBlank { null }
        if (localPath == null && remoteUrl == null) {
            setError("Provide local path or remote URL for the asset.")
            return
        }

        scope.launch {
            state = state.copy(isWorking = true, assetStatusText = "Saving asset...")
            runCatching {
                api.addAsset(
                    AssetUpsertRequest(
                        kind = kind,
                        title = state.assetTitleInput.trim(),
                        source = state.assetSourceInput.trim(),
                        license_name = state.assetLicenseNameInput.trim(),
                        license_url = state.assetLicenseUrlInput.trim().ifBlank { null },
                        attribution_required = state.assetAttributionRequired,
                        attribution_text = state.assetAttributionTextInput.trim().ifBlank { null },
                        tags = parseBulkList(state.assetTagsInput),
                        local_path = localPath,
                        remote_url = remoteUrl
                    )
                )
            }.onSuccess { response ->
                state = state.copy(
                    isWorking = false,
                    assetTitleInput = "",
                    assetSourceInput = "",
                    assetLicenseNameInput = "",
                    assetLicenseUrlInput = "",
                    assetAttributionRequired = false,
                    assetAttributionTextInput = "",
                    assetTagsInput = "",
                    assetLocalPathInput = "",
                    assetRemoteUrlInput = "",
                    assetStatusText = "Saved asset '${response.asset.title}' (${response.asset.kind}).",
                    errorText = null
                )
                refreshAssets()
            }.onFailure {
                state = state.copy(
                    isWorking = false,
                    assetStatusText = "Saving asset failed: ${it.message}"
                )
            }
        }
    }

    fun deleteAsset(assetId: String) {
        if (assetId.isBlank()) return
        scope.launch {
            state = state.copy(isWorking = true, assetStatusText = "Deleting asset...")
            runCatching { api.deleteAsset(assetId) }
                .onSuccess {
                    state = state.copy(
                        isWorking = false,
                        assetStatusText = "Deleted asset $assetId.",
                        errorText = null
                    )
                    refreshAssets()
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        assetStatusText = "Deleting asset failed: ${it.message}"
                    )
                }
        }
    }

    fun applyAssetToAudioBed(assetId: String) {
        val asset = state.assets.firstOrNull { it.id == assetId } ?: run {
            setError("Asset not found.")
            return
        }
        val path = asset.local_path?.trim().orEmpty().ifBlank { null }
        val url = asset.remote_url?.trim().orEmpty().ifBlank { null }
        if (path == null && url == null) {
            setError("Selected asset has no local path or remote URL.")
            return
        }
        val attributionNote = if (asset.attribution_required && !asset.attribution_text.isNullOrBlank()) {
            " Attribution required: ${asset.attribution_text}"
        } else {
            ""
        }
        state = state.copy(
            audioFinSoundPath = path.orEmpty(),
            audioFinSoundUrl = if (path != null) "" else url.orEmpty(),
            assetStatusText = "Applied '${asset.title}' to audio bed.$attributionNote",
            errorText = null
        )
    }

    fun applyAssetToTimelineBackground(assetId: String) {
        val asset = state.assets.firstOrNull { it.id == assetId } ?: run {
            setError("Asset not found.")
            return
        }
        val path = asset.local_path?.trim().orEmpty().ifBlank { null }
        val url = asset.remote_url?.trim().orEmpty().ifBlank { null }
        if (path == null && url == null) {
            setError("Selected asset has no local path or remote URL.")
            return
        }
        val attributionNote = if (asset.attribution_required && !asset.attribution_text.isNullOrBlank()) {
            " Attribution required: ${asset.attribution_text}"
        } else {
            ""
        }
        state = state.copy(
            timelineBackgroundAudioPath = path.orEmpty(),
            timelineBackgroundAudioUrl = if (path != null) "" else url.orEmpty(),
            assetStatusText = "Applied '${asset.title}' to timeline background audio.$attributionNote",
            errorText = null
        )
    }

    fun applyAssetToProVideoOverlay(assetId: String) {
        val asset = state.assets.firstOrNull { it.id == assetId } ?: run {
            setError("Asset not found.")
            return
        }
        val path = asset.local_path?.trim().orEmpty().ifBlank { null }
        val url = asset.remote_url?.trim().orEmpty().ifBlank { null }
        if (path == null && url == null) {
            setError("Selected asset has no local path or remote URL.")
            return
        }
        state = state.copy(
            proVideoOverlayPath = path.orEmpty(),
            proVideoOverlayUrl = if (path != null) "" else url.orEmpty(),
            proVideoTextOverlay = mergeAttributionText(state.proVideoTextOverlay, asset),
            assetStatusText = "Applied '${asset.title}' to pro-video overlay.",
            errorText = null
        )
    }

    fun applyAssetToPhotoOverlay(assetId: String) {
        val asset = state.assets.firstOrNull { it.id == assetId } ?: run {
            setError("Asset not found.")
            return
        }
        val path = asset.local_path?.trim().orEmpty().ifBlank { null }
        val url = asset.remote_url?.trim().orEmpty().ifBlank { null }
        if (path == null && url == null) {
            setError("Selected asset has no local path or remote URL.")
            return
        }
        state = state.copy(
            photoOverlayPath = path.orEmpty(),
            photoOverlayUrl = if (path != null) "" else url.orEmpty(),
            photoTextOverlay = mergeAttributionText(state.photoTextOverlay, asset),
            assetStatusText = "Applied '${asset.title}' to photo overlay.",
            errorText = null
        )
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

    fun updateTimelineClip(index: Int, transform: (TimelineClipDraft) -> TimelineClipDraft) {
        val clips = state.timelineClips
        if (index !in clips.indices) return
        state = state.copy(
            timelineClips = clips.mapIndexed { i, clip -> if (i == index) transform(clip) else clip }
        )
    }

    fun updateTimelineAudioTrack(index: Int, transform: (TimelineAudioTrackDraft) -> TimelineAudioTrackDraft) {
        val tracks = state.timelineAudioTracks
        if (index !in tracks.indices) return
        state = state.copy(
            timelineAudioTracks = tracks.mapIndexed { i, track -> if (i == index) transform(track) else track }
        )
    }

    fun updateTimelineKeyframe(index: Int, transform: (TimelineKeyframeDraft) -> TimelineKeyframeDraft) {
        val keyframes = state.timelineKeyframes
        if (index !in keyframes.indices) return
        state = state.copy(
            timelineKeyframes = keyframes.mapIndexed { i, keyframe -> if (i == index) transform(keyframe) else keyframe }
        )
    }

    fun updateCaptionCue(index: Int, transform: (CaptionCueDraft) -> CaptionCueDraft) {
        val cues = state.captionCues
        if (index !in cues.indices) return
        state = state.copy(
            captionCues = cues.mapIndexed { i, cue -> if (i == index) transform(cue) else cue }
        )
    }

    fun parseSplitPointsCsv(csv: String): List<Double>? {
        val cleaned = csv.trim()
        if (cleaned.isBlank()) return emptyList()
        val values = mutableListOf<Double>()
        for (token in cleaned.split(",")) {
            val number = token.trim().toDoubleOrNull() ?: return null
            values += number
        }
        return values
    }

    fun parseAudioEqGain(input: String): Double {
        val parsed = input.trim().toDoubleOrNull() ?: 0.0
        return parsed.coerceIn(-24.0, 24.0)
    }

    fun parseSpeedFactor(input: String): Double {
        val parsed = input.trim().toDoubleOrNull() ?: 1.0
        return parsed.coerceIn(0.10, 4.0)
    }

    fun runAudioFinishing(burnSubtitles: Boolean = false, subtitlePath: String? = null, subtitleUrl: String? = null) {
        val sourcePath = state.audioFinSourcePath.trim().ifBlank { null }
        val sourceUrl = state.audioFinSourceUrl.trim().ifBlank { null }
        if (sourcePath == null && sourceUrl == null) {
            setError("Provide audio-finishing source path or URL.")
            return
        }
        if (state.audioFinDucking && state.audioFinSoundPath.trim().isBlank() && state.audioFinSoundUrl.trim().isBlank()) {
            setError("Ducking requires a background sound path or URL.")
            return
        }

        scope.launch {
            state = state.copy(isWorking = true, audioFinStatusText = "Running audio finishing...")
            runCatching {
                val presetId = api.getVideoFilters().presets.firstOrNull()?.id
                    ?: error("No video preset available for audio finishing.")
                api.applyVideoFilter(
                    VideoFilterApplyRequest(
                        source_path = sourcePath,
                        source_url = sourceUrl,
                        preset_id = presetId,
                        output_name = state.audioFinOutputName.trim().ifBlank { "audio_finished" },
                        subtitle_path = subtitlePath,
                        subtitle_url = subtitleUrl,
                        burn_subtitles = burnSubtitles,
                        sound_path = state.audioFinSoundPath.trim().ifBlank { null },
                        sound_url = state.audioFinSoundUrl.trim().ifBlank { null },
                        sound_volume = state.audioFinSoundVolume.trim().toDoubleOrNull() ?: 1.0,
                        audio_denoise = state.audioFinDenoise,
                        audio_eq_low_gain = parseAudioEqGain(state.audioFinEqLowGain),
                        audio_eq_mid_gain = parseAudioEqGain(state.audioFinEqMidGain),
                        audio_eq_high_gain = parseAudioEqGain(state.audioFinEqHighGain),
                        audio_compressor = state.audioFinCompressor,
                        audio_gate = state.audioFinGate,
                        audio_ducking = state.audioFinDucking,
                        loudness_normalization = state.audioFinLoudnessNormalization,
                        audio_limiter = state.audioFinLimiter
                    )
                )
            }.onSuccess { result ->
                state = state.copy(
                    isWorking = false,
                    audioFinStatusText = result.message,
                    audioFinOutputPath = result.output_path.orEmpty(),
                    captionBurnOutputPath = if (burnSubtitles) result.output_path.orEmpty() else state.captionBurnOutputPath,
                    captionStatusText = if (burnSubtitles) result.message else state.captionStatusText,
                    errorText = null
                )
            }.onFailure {
                state = state.copy(
                    isWorking = false,
                    audioFinStatusText = "Audio finishing failed: ${it.message}",
                    captionStatusText = if (burnSubtitles) "Subtitle burn-in failed: ${it.message}" else state.captionStatusText
                )
            }
        }
    }

    fun runProVideoControls() {
        val sourcePath = state.proVideoSourcePath.trim().ifBlank { null }
        val sourceUrl = state.proVideoSourceUrl.trim().ifBlank { null }
        if (sourcePath == null && sourceUrl == null) {
            setError("Provide pro-video source path or URL.")
            return
        }
        if (
            state.audioFinDucking &&
            state.audioFinSoundPath.trim().isBlank() &&
            state.audioFinSoundUrl.trim().isBlank()
        ) {
            setError("If ducking is enabled, provide background sound path or URL.")
            return
        }

        scope.launch {
            state = state.copy(isWorking = true, proVideoStatusText = "Applying pro video controls...")
            runCatching {
                val presetId = api.getVideoFilters().presets.firstOrNull()?.id
                    ?: error("No video preset available.")
                api.applyVideoFilter(
                    VideoFilterApplyRequest(
                        source_path = sourcePath,
                        source_url = sourceUrl,
                        preset_id = presetId,
                        output_name = state.proVideoOutputName.trim().ifBlank { "pro_video" },
                        speed_factor = parseSpeedFactor(state.proVideoSpeedFactor),
                        stabilize = state.proVideoStabilize,
                        curves_preset = state.proVideoCurvesPreset.trim().ifBlank { null },
                        lut3d_path = state.proVideoLutPath.trim().ifBlank { null },
                        lut3d_url = state.proVideoLutUrl.trim().ifBlank { null },
                        overlay_path = state.proVideoOverlayPath.trim().ifBlank { null },
                        overlay_url = state.proVideoOverlayUrl.trim().ifBlank { null },
                        text_overlay = state.proVideoTextOverlay.trim().ifBlank { null },
                        sound_path = state.audioFinSoundPath.trim().ifBlank { null },
                        sound_url = state.audioFinSoundUrl.trim().ifBlank { null },
                        sound_volume = state.audioFinSoundVolume.trim().toDoubleOrNull() ?: 1.0,
                        audio_denoise = state.audioFinDenoise,
                        audio_eq_low_gain = parseAudioEqGain(state.audioFinEqLowGain),
                        audio_eq_mid_gain = parseAudioEqGain(state.audioFinEqMidGain),
                        audio_eq_high_gain = parseAudioEqGain(state.audioFinEqHighGain),
                        audio_compressor = state.audioFinCompressor,
                        audio_gate = state.audioFinGate,
                        audio_ducking = state.audioFinDucking,
                        loudness_normalization = state.audioFinLoudnessNormalization,
                        audio_limiter = state.audioFinLimiter
                    )
                )
            }.onSuccess { result ->
                state = state.copy(
                    isWorking = false,
                    proVideoStatusText = result.message,
                    proVideoOutputPath = result.output_path.orEmpty(),
                    errorText = null
                )
            }.onFailure {
                state = state.copy(
                    isWorking = false,
                    proVideoStatusText = "Pro video processing failed: ${it.message}"
                )
            }
        }
    }

    fun runProMotionTracking() {
        val sourcePath = state.proVideoSourcePath.trim().ifBlank { null }
        val sourceUrl = state.proVideoSourceUrl.trim().ifBlank { null }
        if (sourcePath == null && sourceUrl == null) {
            setError("Provide pro-video source path or URL before motion tracking.")
            return
        }

        scope.launch {
            state = state.copy(isWorking = true, proVideoStatusText = "Running motion tracking...")
            runCatching {
                api.trackVideoMotion(
                    MotionTrackRequest(
                        source_path = sourcePath,
                        source_url = sourceUrl,
                        start_seconds = state.proMotionStartSeconds.trim().toDoubleOrNull() ?: 0.0,
                        end_seconds = state.proMotionEndSeconds.trim().ifBlank { "" }.toDoubleOrNull(),
                        max_corners = 220,
                        quality_level = 0.18,
                        min_distance = 6.0,
                        sample_every_n_frames = 2,
                        roi_x = state.proMotionRoiX.trim().toIntOrNull(),
                        roi_y = state.proMotionRoiY.trim().toIntOrNull(),
                        roi_width = state.proMotionRoiWidth.trim().toIntOrNull(),
                        roi_height = state.proMotionRoiHeight.trim().toIntOrNull(),
                        smoothing_window = state.proMotionSmoothingWindow.trim().toIntOrNull() ?: 4,
                        min_confidence = state.proMotionMinConfidence.trim().toDoubleOrNull() ?: 0.05,
                        output_overlay_name = state.proMotionOverlayOutputName.trim().ifBlank { null }
                    )
                )
            }.onSuccess { response ->
                val sample = response.track_points.take(3).joinToString {
                    "t=${"%.2f".format(it.at_seconds)} (${it.x},${it.y},${it.width}x${it.height})"
                }
                val overlayPart = response.overlay_path?.let { " | overlay=$it" } ?: ""
                state = state.copy(
                    isWorking = false,
                    proVideoStatusText = response.message,
                    proMotionSummary = if (response.success) {
                        "points=${response.track_points.size}, fps=${"%.2f".format(response.fps)}, conf=${"%.2f".format(response.average_confidence)}$overlayPart${if (sample.isBlank()) "" else " | $sample"}"
                    } else {
                        response.message
                    },
                    errorText = null
                )
            }.onFailure {
                state = state.copy(
                    isWorking = false,
                    proVideoStatusText = "Motion tracking failed: ${it.message}"
                )
            }
        }
    }

    fun updatePhotoEraseRegion(index: Int, transform: (ImageEraseRegionDraft) -> ImageEraseRegionDraft) {
        val current = state.photoEraseRegions
        if (index !in current.indices) return
        state = state.copy(
            photoEraseRegions = current.mapIndexed { i, region -> if (i == index) transform(region) else region }
        )
    }

    fun updatePhotoLayer(index: Int, transform: (ImageLayerDraft) -> ImageLayerDraft) {
        val current = state.photoLayers
        if (index !in current.indices) return
        state = state.copy(
            photoLayers = current.mapIndexed { i, layer -> if (i == index) transform(layer) else layer }
        )
    }

    fun parsePhotoEraseRegions(drafts: List<ImageEraseRegionDraft>): List<ImageEraseRegion>? {
        val parsed = mutableListOf<ImageEraseRegion>()
        for (draft in drafts) {
            val x = draft.x.trim()
            val y = draft.y.trim()
            val width = draft.width.trim()
            val height = draft.height.trim()
            if (x.isBlank() && y.isBlank() && width.isBlank() && height.isBlank()) {
                continue
            }
            val xValue = x.toIntOrNull() ?: return null
            val yValue = y.toIntOrNull() ?: return null
            val widthValue = width.toIntOrNull() ?: return null
            val heightValue = height.toIntOrNull() ?: return null
            if (widthValue <= 0 || heightValue <= 0) return null
            parsed += ImageEraseRegion(
                x = xValue,
                y = yValue,
                width = widthValue,
                height = heightValue
            )
        }
        return parsed
    }

    fun parsePhotoLayers(drafts: List<ImageLayerDraft>): List<ImageLayer>? {
        val parsed = mutableListOf<ImageLayer>()
        for (draft in drafts) {
            val layerPath = draft.layerPath.trim()
            val layerUrl = draft.layerUrl.trim()
            if (layerPath.isBlank() && layerUrl.isBlank()) {
                continue
            }
            val opacity = draft.opacity.trim().toDoubleOrNull() ?: return null
            val x = draft.x.trim().toIntOrNull() ?: return null
            val y = draft.y.trim().toIntOrNull() ?: return null
            parsed += ImageLayer(
                layer_path = layerPath.ifBlank { null },
                layer_url = layerUrl.ifBlank { null },
                mask_path = draft.maskPath.trim().ifBlank { null },
                mask_url = draft.maskUrl.trim().ifBlank { null },
                opacity = opacity.coerceIn(0.0, 1.0),
                blend_mode = draft.blendMode.trim().ifBlank { "normal" },
                x = x,
                y = y
            )
        }
        return parsed
    }

    fun runProPhotoFilter() {
        val sourcePath = state.photoSourcePath.trim().ifBlank { null }
        val sourceUrl = state.photoSourceUrl.trim().ifBlank { null }
        if (sourcePath == null && sourceUrl == null) {
            setError("Provide photo source path or URL.")
            return
        }

        val eraseRegions = parsePhotoEraseRegions(state.photoEraseRegions)
        if (eraseRegions == null) {
            setError("Photo erase regions must have valid integer x/y/width/height.")
            return
        }
        val layers = parsePhotoLayers(state.photoLayers)
        if (layers == null) {
            setError("Photo layers need valid opacity and integer x/y.")
            return
        }
        val tolerance = state.photoBackgroundKeyTolerance.trim().toIntOrNull()?.coerceIn(0, 255) ?: 24

        scope.launch {
            state = state.copy(isWorking = true, photoStatusText = "Applying pro photo controls...")
            runCatching {
                val presetId = state.photoPresetId.ifBlank {
                    api.getImageFilters().presets.firstOrNull()?.id ?: error("No image preset available.")
                }
                api.applyImageFilter(
                    ImageFilterApplyRequest(
                        source_path = sourcePath,
                        source_url = sourceUrl,
                        preset_id = presetId,
                        output_name = state.photoOutputName.trim().ifBlank { "photo_pro" },
                        sepia = state.photoSepia,
                        black_white = state.photoBlackWhite,
                        cartoonify = state.photoCartoonify,
                        caricature = state.photoCaricature,
                        saturation_factor = state.photoSaturationFactor.trim().toDoubleOrNull() ?: 1.0,
                        brightness_factor = state.photoBrightnessFactor.trim().toDoubleOrNull() ?: 1.0,
                        contrast_factor = state.photoContrastFactor.trim().toDoubleOrNull() ?: 1.0,
                        overlay_path = state.photoOverlayPath.trim().ifBlank { null },
                        overlay_url = state.photoOverlayUrl.trim().ifBlank { null },
                        text_overlay = state.photoTextOverlay.trim().ifBlank { null },
                        raw_enhance = state.photoRawEnhance,
                        selective_x = state.photoSelectiveX.trim().toIntOrNull(),
                        selective_y = state.photoSelectiveY.trim().toIntOrNull(),
                        selective_width = state.photoSelectiveWidth.trim().toIntOrNull(),
                        selective_height = state.photoSelectiveHeight.trim().toIntOrNull(),
                        selective_brightness_factor = state.photoSelectiveBrightnessFactor.trim().toDoubleOrNull() ?: 1.0,
                        selective_contrast_factor = state.photoSelectiveContrastFactor.trim().toDoubleOrNull() ?: 1.0,
                        selective_saturation_factor = state.photoSelectiveSaturationFactor.trim().toDoubleOrNull() ?: 1.0,
                        erase_regions = eraseRegions,
                        layers = layers,
                        background_key_color = if (state.photoBackgroundRemove) state.photoBackgroundKeyColor.trim().ifBlank { "#00FF00" } else null,
                        background_key_tolerance = tolerance
                    )
                )
            }.onSuccess { result ->
                state = state.copy(
                    isWorking = false,
                    photoStatusText = result.message,
                    photoOutputPath = result.output_path.orEmpty(),
                    photoPresetId = state.photoPresetId.ifBlank { result.preset_id },
                    errorText = null
                )
            }.onFailure {
                state = state.copy(
                    isWorking = false,
                    photoStatusText = "Pro photo apply failed: ${it.message}"
                )
            }
        }
    }

    fun runProPhotoHeal() {
        val sourcePath = state.photoSourcePath.trim().ifBlank { null }
        val sourceUrl = state.photoSourceUrl.trim().ifBlank { null }
        if (sourcePath == null && sourceUrl == null) {
            setError("Provide photo source path or URL before heal/remove.")
            return
        }

        val eraseRegions = parsePhotoEraseRegions(state.photoEraseRegions)
        if (eraseRegions == null) {
            setError("Photo erase regions must have valid integer x/y/width/height.")
            return
        }

        scope.launch {
            state = state.copy(isWorking = true, photoStatusText = "Running photo heal/object removal...")
            runCatching {
                api.healImage(
                    ImageHealRequest(
                        source_path = sourcePath,
                        source_url = sourceUrl,
                        output_name = state.photoHealOutputName.trim().ifBlank { "photo_heal" },
                        mask_path = state.photoHealMaskPath.trim().ifBlank { null },
                        mask_url = state.photoHealMaskUrl.trim().ifBlank { null },
                        erase_regions = eraseRegions,
                        inpaint_radius = state.photoHealInpaintRadius.trim().toDoubleOrNull() ?: 3.0,
                        method = state.photoHealMethod.trim().ifBlank { "telea" },
                        fill_strategy = state.photoHealFillStrategy.trim().ifBlank { "inpaint" },
                        feather_radius = state.photoHealFeatherRadius.trim().toIntOrNull() ?: 3,
                        preserve_edges = state.photoHealPreserveEdges,
                        edge_blend = state.photoHealEdgeBlend.trim().toDoubleOrNull() ?: 0.55,
                        denoise_strength = state.photoHealDenoiseStrength.trim().toDoubleOrNull() ?: 0.0
                    )
                )
            }.onSuccess { response ->
                state = state.copy(
                    isWorking = false,
                    photoStatusText = response.message,
                    photoHealOutputPath = response.output_path.orEmpty(),
                    photoOutputPath = response.output_path.orEmpty().ifBlank { state.photoOutputPath },
                    errorText = null
                )
            }.onFailure {
                state = state.copy(
                    isWorking = false,
                    photoStatusText = "Photo heal/remove failed: ${it.message}"
                )
            }
        }
    }

    fun transcribeCaptions() {
        val sourcePath = state.captionSourcePath.trim().ifBlank { state.timelineClips.firstOrNull()?.sourcePath?.trim().orEmpty().ifBlank { null } }
        val sourceUrl = state.captionSourceUrl.trim().ifBlank { state.timelineClips.firstOrNull()?.sourceUrl?.trim().orEmpty().ifBlank { null } }
        if (sourcePath == null && sourceUrl == null) {
            setError("Provide caption source path/url (or set first timeline clip source).")
            return
        }

        scope.launch {
            state = state.copy(
                isWorking = true,
                captionStatusText = "Transcribing captions..."
            )
            runCatching {
                api.transcribeCaptions(
                    CaptionTranscribeRequest(
                        source_path = sourcePath,
                        source_url = sourceUrl,
                        language = state.captionLanguage.trim().ifBlank { null },
                        output_name = state.captionOutputName.trim().ifBlank { null },
                        max_chars_per_line = 42
                    )
                )
            }.onSuccess { result ->
                state = state.copy(
                    isWorking = false,
                    captionStatusText = result.message,
                    captionEngine = result.engine,
                    captionCueCount = result.cue_count,
                    captionGeneratedSrtPath = result.srt_path.orEmpty(),
                    captionSubtitlePath = result.srt_path.orEmpty().ifBlank { state.captionSubtitlePath },
                    errorText = null
                )
            }.onFailure {
                state = state.copy(
                    isWorking = false,
                    captionStatusText = "Caption transcription failed: ${it.message}"
                )
            }
        }
    }

    fun buildSrtFromCues() {
        val cues = mutableListOf<CaptionCue>()
        for ((index, draft) in state.captionCues.withIndex()) {
            val startToken = draft.startSeconds.trim()
            val endToken = draft.endSeconds.trim()
            val text = draft.text.trim()
            if (startToken.isBlank() && endToken.isBlank() && text.isBlank()) {
                continue
            }
            val startSeconds = startToken.toDoubleOrNull()
            if (startSeconds == null || startSeconds < 0.0) {
                setError("Cue ${index + 1}: invalid start seconds.")
                return
            }
            val endSeconds = endToken.toDoubleOrNull()
            if (endSeconds == null || endSeconds <= startSeconds) {
                setError("Cue ${index + 1}: end seconds must be greater than start.")
                return
            }
            if (text.isBlank()) {
                setError("Cue ${index + 1}: text is required.")
                return
            }
            cues += CaptionCue(
                start_seconds = startSeconds,
                end_seconds = endSeconds,
                text = text
            )
        }

        if (cues.isEmpty()) {
            setError("Add at least one cue before building SRT.")
            return
        }

        scope.launch {
            state = state.copy(isWorking = true, captionStatusText = "Building SRT from cues...")
            runCatching {
                api.buildSrt(
                    CaptionSrtBuildRequest(
                        cues = cues,
                        output_name = state.captionOutputName.trim().ifBlank { "captions_manual" }
                    )
                )
            }.onSuccess { result ->
                state = state.copy(
                    isWorking = false,
                    captionStatusText = result.message,
                    captionCueCount = result.cue_count,
                    captionGeneratedSrtPath = result.srt_path.orEmpty(),
                    captionSubtitlePath = result.srt_path.orEmpty().ifBlank { state.captionSubtitlePath },
                    errorText = null
                )
            }.onFailure {
                state = state.copy(
                    isWorking = false,
                    captionStatusText = "SRT build failed: ${it.message}"
                )
            }
        }
    }

    fun burnSubtitles() {
        val sourcePath = state.captionSourcePath.trim().ifBlank { state.timelineClips.firstOrNull()?.sourcePath?.trim().orEmpty().ifBlank { null } }
        val sourceUrl = state.captionSourceUrl.trim().ifBlank { state.timelineClips.firstOrNull()?.sourceUrl?.trim().orEmpty().ifBlank { null } }
        if (sourcePath == null && sourceUrl == null) {
            setError("Provide source path/url before subtitle burn-in.")
            return
        }

        val subtitlePath = state.captionSubtitlePath.trim().ifBlank { state.captionGeneratedSrtPath.trim().ifBlank { null } }
        val subtitleUrl = state.captionSubtitleUrl.trim().ifBlank { null }
        if (subtitlePath == null && subtitleUrl == null) {
            setError("Provide subtitle path/url or generate/build an SRT first.")
            return
        }

        state = state.copy(
            audioFinSourcePath = sourcePath ?: "",
            audioFinSourceUrl = sourceUrl ?: "",
            audioFinOutputName = state.captionBurnOutputName,
            captionStatusText = "Burning subtitles with current audio-finishing settings..."
        )
        runAudioFinishing(
            burnSubtitles = true,
            subtitlePath = subtitlePath,
            subtitleUrl = subtitleUrl
        )
    }

    fun composeTimeline() {
        val clips = mutableListOf<TimelineClip>()
        for ((index, draft) in state.timelineClips.withIndex()) {
            val sourcePath = draft.sourcePath.trim().ifBlank { null }
            val sourceUrl = draft.sourceUrl.trim().ifBlank { null }
            if (sourcePath == null && sourceUrl == null) {
                setError("Timeline clip ${index + 1}: source path or source URL is required.")
                return
            }
            val startSeconds = draft.startSeconds.toDoubleOrNull()
            if (startSeconds == null) {
                setError("Timeline clip ${index + 1}: invalid start seconds.")
                return
            }
            val endSeconds = draft.endSeconds.trim().ifBlank { "" }.let { token ->
                if (token.isBlank()) null else token.toDoubleOrNull()
            }
            if (draft.endSeconds.isNotBlank() && endSeconds == null) {
                setError("Timeline clip ${index + 1}: invalid end seconds.")
                return
            }
            val speed = draft.speedFactor.toDoubleOrNull()
            if (speed == null || speed <= 0.0) {
                setError("Timeline clip ${index + 1}: speed factor must be > 0.")
                return
            }
            val volume = draft.volume.toDoubleOrNull()
            if (volume == null || volume < 0.0) {
                setError("Timeline clip ${index + 1}: volume must be >= 0.")
                return
            }
            val transitionDuration = draft.transitionDuration.toDoubleOrNull()
            if (transitionDuration == null || transitionDuration <= 0.0) {
                setError("Timeline clip ${index + 1}: transition duration must be > 0.")
                return
            }
            val splitPoints = parseSplitPointsCsv(draft.splitPointsCsv)
            if (splitPoints == null) {
                setError("Timeline clip ${index + 1}: split points must be comma-separated numbers.")
                return
            }
            clips += TimelineClip(
                source_path = sourcePath,
                source_url = sourceUrl,
                start_seconds = startSeconds,
                end_seconds = endSeconds,
                speed_factor = speed,
                volume = volume,
                split_points = splitPoints,
                transition_to_next = draft.transitionToNext.trim().ifBlank { null },
                transition_duration = transitionDuration
            )
        }

        val audioTracks = mutableListOf<TimelineAudioTrack>()
        for ((index, draft) in state.timelineAudioTracks.withIndex()) {
            val sourcePath = draft.sourcePath.trim().ifBlank { null }
            val sourceUrl = draft.sourceUrl.trim().ifBlank { null }
            if (sourcePath == null && sourceUrl == null) continue
            val startSeconds = draft.startSeconds.toDoubleOrNull()
            if (startSeconds == null) {
                setError("Audio track ${index + 1}: invalid start seconds.")
                return
            }
            val endSeconds = draft.endSeconds.trim().ifBlank { "" }.let { token ->
                if (token.isBlank()) null else token.toDoubleOrNull()
            }
            if (draft.endSeconds.isNotBlank() && endSeconds == null) {
                setError("Audio track ${index + 1}: invalid end seconds.")
                return
            }
            val volume = draft.volume.toDoubleOrNull()
            if (volume == null || volume < 0.0) {
                setError("Audio track ${index + 1}: volume must be >= 0.")
                return
            }
            audioTracks += TimelineAudioTrack(
                source_path = sourcePath,
                source_url = sourceUrl,
                start_seconds = startSeconds,
                end_seconds = endSeconds,
                volume = volume
            )
        }

        val keyframes = mutableListOf<TimelineKeyframe>()
        for ((index, draft) in state.timelineKeyframes.withIndex()) {
            val property = draft.property.trim()
            val value = draft.value.trim()
            val atToken = draft.atSeconds.trim()
            if (property.isBlank() && value.isBlank() && atToken.isBlank()) continue
            val atSeconds = atToken.toDoubleOrNull()
            if (atSeconds == null) {
                setError("Keyframe ${index + 1}: invalid at-seconds.")
                return
            }
            if (property.isBlank() || value.isBlank()) {
                setError("Keyframe ${index + 1}: property and value are required.")
                return
            }
            keyframes += TimelineKeyframe(
                at_seconds = atSeconds,
                property = property,
                value = value
            )
        }

        val backgroundAudioVolume = state.timelineBackgroundAudioVolume.toDoubleOrNull() ?: 0.35
        val request = TimelineComposeRequest(
            clips = clips,
            audio_tracks = audioTracks,
            keyframes = keyframes,
            transition_default = state.timelineTransitionDefault.trim().ifBlank { null },
            normalize_loudness = state.timelineNormalizeLoudness,
            output_name = state.timelineOutputName.trim().ifBlank { null },
            background_audio_path = state.timelineBackgroundAudioPath.trim().ifBlank { null },
            background_audio_url = state.timelineBackgroundAudioUrl.trim().ifBlank { null },
            background_audio_volume = backgroundAudioVolume
        )

        scope.launch {
            state = state.copy(isWorking = true, timelineStatusText = "Composing timeline...", timelineWarnings = emptyList())
            runCatching { api.composeTimeline(request) }
                .onSuccess { result: TimelineComposeResponse ->
                    state = state.copy(
                        isWorking = false,
                        timelineStatusText = result.message,
                        timelineOutputPath = result.output_path.orEmpty(),
                        timelineWarnings = result.warnings,
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        timelineStatusText = "Timeline compose failed: ${it.message}"
                    )
                }
        }
    }

    fun buildLiveDestinations(): List<LiveDestinationConfig> {
        fun destinationOrNull(platform: String, ingestUrl: String, streamKey: String): LiveDestinationConfig? {
            val ingest = ingestUrl.trim()
            if (ingest.isBlank()) {
                return null
            }
            return LiveDestinationConfig(
                platform = platform,
                label = platform.uppercase(),
                ingest_url = ingest,
                stream_key = streamKey.trim().ifBlank { null },
                enabled = true
            )
        }
        return listOfNotNull(
            destinationOrNull("x", state.liveMulticastXIngestUrl, state.liveMulticastXStreamKey),
            destinationOrNull("linkedin", state.liveMulticastLinkedinIngestUrl, state.liveMulticastLinkedinStreamKey),
            destinationOrNull("instagram", state.liveMulticastInstagramIngestUrl, state.liveMulticastInstagramStreamKey)
        )
    }

    fun refreshLiveMulticastSessions() {
        scope.launch {
            state = state.copy(isWorking = true)
            runCatching { api.listLiveMulticastSessions() }
                .onSuccess { response ->
                    val current = state.liveMulticastCurrentSessionId
                    val active = response.sessions.firstOrNull { it.session_id == current } ?: response.sessions.firstOrNull()
                    state = state.copy(
                        isWorking = false,
                        liveMulticastSessions = response.sessions,
                        liveMulticastCurrentSessionId = active?.session_id.orEmpty(),
                        liveMulticastStatusText = active?.let { session ->
                            "${session.session_name}: ${session.status} | active=${session.active_destinations} failed=${session.failed_destinations}"
                        }.orEmpty(),
                        errorText = null
                    )
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        errorText = "Loading live multicast sessions failed: ${it.message}"
                    )
                }
        }
    }

    fun startLiveMulticast() {
        val sourcePath = state.liveMulticastSourcePath.trim().ifBlank { null }
        val sourceUrl = state.liveMulticastSourceUrl.trim().ifBlank { null }
        if (sourcePath == null && sourceUrl == null) {
            setError("Live multicast requires source path or source URL.")
            return
        }
        val destinations = buildLiveDestinations()
        if (destinations.isEmpty()) {
            setError("Provide at least one ingest URL for X, LinkedIn, or Instagram.")
            return
        }

        val request = LiveMulticastStartRequest(
            session_name = state.liveMulticastSessionName.trim().ifBlank { null },
            source_path = sourcePath,
            source_url = sourceUrl,
            dry_run = state.liveMulticastDryRun,
            copy_video = state.liveMulticastCopyVideo,
            video_bitrate = state.liveMulticastVideoBitrate.trim().ifBlank { "4500k" },
            audio_bitrate = state.liveMulticastAudioBitrate.trim().ifBlank { "160k" },
            fps = state.liveMulticastFps.toIntOrNull() ?: 30,
            gop_seconds = state.liveMulticastGopSeconds.toDoubleOrNull() ?: 2.0,
            destinations = destinations
        )

        scope.launch {
            state = state.copy(isWorking = true, liveMulticastStatusText = "Starting live multicast...")
            runCatching { api.startLiveMulticast(request) }
                .onSuccess { response ->
                    state = state.copy(
                        isWorking = false,
                        liveMulticastCurrentSessionId = response.session_id,
                        liveMulticastStatusText = "${response.session_name}: ${response.status} | active=${response.active_destinations} failed=${response.failed_destinations}",
                        errorText = null
                    )
                    refreshLiveMulticastSessions()
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        liveMulticastStatusText = "Live multicast start failed: ${it.message}"
                    )
                }
        }
    }

    fun stopLiveMulticast(sessionId: String? = null) {
        val id = sessionId?.trim().orEmpty().ifBlank { state.liveMulticastCurrentSessionId.trim() }
        if (id.isBlank()) {
            setError("Select a live multicast session first.")
            return
        }
        scope.launch {
            state = state.copy(isWorking = true, liveMulticastStatusText = "Stopping live multicast...")
            runCatching { api.stopLiveMulticastSession(id) }
                .onSuccess { response ->
                    state = state.copy(
                        isWorking = false,
                        liveMulticastStatusText = response.message,
                        errorText = null
                    )
                    refreshLiveMulticastSessions()
                }
                .onFailure {
                    state = state.copy(
                        isWorking = false,
                        liveMulticastStatusText = "Live multicast stop failed: ${it.message}"
                    )
                }
        }
    }

    LaunchedEffect(Unit) {
        refreshHealth()
        refreshConfigStatus()
        refreshDryRunHistory()
        refreshDeadLetters()
        refreshCreatorTemplates()
        refreshExportPresets()
        refreshAssets()
        refreshLiveMulticastSessions()
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
                    Text("Creator Templates", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Reusable presets by platform (Reels / Shorts / TikTok and more).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.templatePlatformFilter,
                        onValueChange = { state = state.copy(templatePlatformFilter = it) },
                        label = { Text("Platform Filter (instagram/youtube/tiktok)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.templateMediaTypeFilter,
                        onValueChange = { state = state.copy(templateMediaTypeFilter = it) },
                        label = { Text("Media Type Filter (video/image)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { applyTemplateQuickFilter("reels") }, enabled = !state.isWorking) {
                            Text("Reels")
                        }
                        Button(onClick = { applyTemplateQuickFilter("shorts") }, enabled = !state.isWorking) {
                            Text("Shorts")
                        }
                        Button(onClick = { applyTemplateQuickFilter("tiktok") }, enabled = !state.isWorking) {
                            Text("TikTok")
                        }
                        Button(
                            onClick = {
                                state = state.copy(templatePlatformFilter = "", templateMediaTypeFilter = "")
                                refreshCreatorTemplates()
                            },
                            enabled = !state.isWorking
                        ) {
                            Text("Clear Filters")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::refreshCreatorTemplates, enabled = !state.isWorking) {
                            Text("Refresh Templates")
                        }
                        Button(onClick = ::applySelectedCreatorTemplate, enabled = !state.isWorking) {
                            Text("Apply Selected")
                        }
                    }
                    if (state.templateStatusText.isNotBlank()) {
                        Text(state.templateStatusText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.activeCreatorTemplateText.isNotBlank()) {
                        Text(state.activeCreatorTemplateText, style = MaterialTheme.typography.bodySmall)
                    }
                    state.creatorTemplates.forEach { template ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(template.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${template.media_type} | ${template.width}x${template.height}${template.fps?.let { " @${it}fps" } ?: ""}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text("Platforms: ${template.platforms.joinToString()}", style = MaterialTheme.typography.bodySmall)
                                Button(
                                    onClick = { state = state.copy(selectedCreatorTemplateId = template.id) },
                                    enabled = !state.isWorking
                                ) {
                                    Text(if (state.selectedCreatorTemplateId == template.id) "Selected" else "Select")
                                }
                            }
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
                    Text("Live Multicast", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Stream once and multicast to multiple social platforms simultaneously.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.liveMulticastSessionName,
                        onValueChange = { state = state.copy(liveMulticastSessionName = it) },
                        label = { Text("Session Name (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.liveMulticastSourcePath,
                        onValueChange = { state = state.copy(liveMulticastSourcePath = it) },
                        label = { Text("Source Path (local video/input)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.liveMulticastSourceUrl,
                        onValueChange = { state = state.copy(liveMulticastSourceUrl = it) },
                        label = { Text("Source URL (http/https/rtmp/rtmps)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.liveMulticastXIngestUrl,
                        onValueChange = { state = state.copy(liveMulticastXIngestUrl = it) },
                        label = { Text("X Ingest URL (rtmp/rtmps)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.liveMulticastXStreamKey,
                        onValueChange = { state = state.copy(liveMulticastXStreamKey = it) },
                        label = { Text("X Stream Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.liveMulticastLinkedinIngestUrl,
                        onValueChange = { state = state.copy(liveMulticastLinkedinIngestUrl = it) },
                        label = { Text("LinkedIn Ingest URL (rtmp/rtmps)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.liveMulticastLinkedinStreamKey,
                        onValueChange = { state = state.copy(liveMulticastLinkedinStreamKey = it) },
                        label = { Text("LinkedIn Stream Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.liveMulticastInstagramIngestUrl,
                        onValueChange = { state = state.copy(liveMulticastInstagramIngestUrl = it) },
                        label = { Text("Instagram Ingest URL (rtmp/rtmps)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.liveMulticastInstagramStreamKey,
                        onValueChange = { state = state.copy(liveMulticastInstagramStreamKey = it) },
                        label = { Text("Instagram Stream Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.liveMulticastVideoBitrate,
                            onValueChange = { state = state.copy(liveMulticastVideoBitrate = it) },
                            label = { Text("Video Bitrate") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.liveMulticastAudioBitrate,
                            onValueChange = { state = state.copy(liveMulticastAudioBitrate = it) },
                            label = { Text("Audio Bitrate") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.liveMulticastFps,
                            onValueChange = { state = state.copy(liveMulticastFps = it) },
                            label = { Text("FPS") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.liveMulticastGopSeconds,
                            onValueChange = { state = state.copy(liveMulticastGopSeconds = it) },
                            label = { Text("GOP Seconds") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { state = state.copy(liveMulticastDryRun = !state.liveMulticastDryRun) },
                            enabled = !state.isWorking
                        ) {
                            Text(if (state.liveMulticastDryRun) "Dry Run: On" else "Dry Run: Off")
                        }
                        Button(
                            onClick = { state = state.copy(liveMulticastCopyVideo = !state.liveMulticastCopyVideo) },
                            enabled = !state.isWorking
                        ) {
                            Text(if (state.liveMulticastCopyVideo) "Copy Video: On" else "Copy Video: Off")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::startLiveMulticast, enabled = !state.isWorking) {
                            Text("Start Multicast")
                        }
                        Button(onClick = ::refreshLiveMulticastSessions, enabled = !state.isWorking) {
                            Text("Refresh Sessions")
                        }
                        Button(
                            onClick = { stopLiveMulticast() },
                            enabled = !state.isWorking && state.liveMulticastCurrentSessionId.isNotBlank()
                        ) {
                            Text("Stop Current")
                        }
                    }
                    if (state.liveMulticastStatusText.isNotBlank()) {
                        Text(state.liveMulticastStatusText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.liveMulticastSessions.isEmpty()) {
                        Text("No live multicast sessions yet.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        state.liveMulticastSessions.forEach { session ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(session.session_name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "status=${session.status} | dryRun=${session.dry_run} | active=${session.active_destinations} failed=${session.failed_destinations}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text("Source: ${session.source_kind} -> ${session.source_value}", style = MaterialTheme.typography.bodySmall)
                                    session.destinations.forEach { destination ->
                                        Text(
                                            "- ${destination.platform}: ${destination.status} (${destination.ingest_url_masked})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Button(
                                        onClick = { stopLiveMulticast(session.session_id) },
                                        enabled = !state.isWorking && session.status !in listOf("stopped", "failed")
                                    ) {
                                        Text("Stop Session")
                                    }
                                }
                            }
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
                    Text("Asset Library", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Stock audio + overlay management with licensing and attribution metadata.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.assetKindFilter,
                        onValueChange = { state = state.copy(assetKindFilter = it) },
                        label = { Text("Kind Filter (audio/overlay)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.assetTagFilter,
                        onValueChange = { state = state.copy(assetTagFilter = it) },
                        label = { Text("Tag Filter (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { applyAssetQuickFilter("audio") }, enabled = !state.isWorking) {
                            Text("Audio")
                        }
                        Button(onClick = { applyAssetQuickFilter("overlay") }, enabled = !state.isWorking) {
                            Text("Overlay")
                        }
                        Button(onClick = { applyAssetQuickFilter(null) }, enabled = !state.isWorking) {
                            Text("All")
                        }
                        Button(onClick = ::refreshAssets, enabled = !state.isWorking) {
                            Text("Refresh")
                        }
                    }
                    OutlinedTextField(
                        value = state.assetTitleInput,
                        onValueChange = { state = state.copy(assetTitleInput = it) },
                        label = { Text("Asset Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.assetSourceInput,
                        onValueChange = { state = state.copy(assetSourceInput = it) },
                        label = { Text("Source (vendor/library)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.assetLicenseNameInput,
                        onValueChange = { state = state.copy(assetLicenseNameInput = it) },
                        label = { Text("License Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.assetLicenseUrlInput,
                        onValueChange = { state = state.copy(assetLicenseUrlInput = it) },
                        label = { Text("License URL (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { state = state.copy(assetAttributionRequired = !state.assetAttributionRequired) },
                        enabled = !state.isWorking
                    ) {
                        Text(if (state.assetAttributionRequired) "Attribution Required: On" else "Attribution Required: Off")
                    }
                    OutlinedTextField(
                        value = state.assetAttributionTextInput,
                        onValueChange = { state = state.copy(assetAttributionTextInput = it) },
                        label = { Text("Attribution Text (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.assetTagsInput,
                        onValueChange = { state = state.copy(assetTagsInput = it) },
                        label = { Text("Tags (comma/newline)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.assetLocalPathInput,
                        onValueChange = { state = state.copy(assetLocalPathInput = it) },
                        label = { Text("Local Path (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.assetRemoteUrlInput,
                        onValueChange = { state = state.copy(assetRemoteUrlInput = it) },
                        label = { Text("Remote URL (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = ::saveAsset, enabled = !state.isWorking) {
                        Text("Save Asset")
                    }
                    if (state.assetStatusText.isNotBlank()) {
                        Text(state.assetStatusText, style = MaterialTheme.typography.bodySmall)
                    }
                    state.assets.forEach { asset ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        Button(onClick = { applyAssetToAudioBed(asset.id) }, enabled = !state.isWorking) {
                                            Text("Use In Audio Bed")
                                        }
                                        Button(onClick = { applyAssetToTimelineBackground(asset.id) }, enabled = !state.isWorking) {
                                            Text("Use In Timeline")
                                        }
                                    } else {
                                        Button(onClick = { applyAssetToProVideoOverlay(asset.id) }, enabled = !state.isWorking) {
                                            Text("Use In Pro Video")
                                        }
                                        Button(onClick = { applyAssetToPhotoOverlay(asset.id) }, enabled = !state.isWorking) {
                                            Text("Use In Photo")
                                        }
                                    }
                                    Button(onClick = { deleteAsset(asset.id) }, enabled = !state.isWorking) {
                                        Text("Delete")
                                    }
                                }
                            }
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
                    Text("Export Pipeline", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Platform-specific presets, batch export queue, and hardware-accelerated render option.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.exportSourcePathsInput,
                        onValueChange = { state = state.copy(exportSourcePathsInput = it) },
                        label = { Text("Source Paths (one per line)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.exportSourceUrlsInput,
                        onValueChange = { state = state.copy(exportSourceUrlsInput = it) },
                        label = { Text("Source URLs (one per line)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.exportOutputPrefix,
                        onValueChange = { state = state.copy(exportOutputPrefix = it) },
                        label = { Text("Output Prefix (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { state = state.copy(exportUseHardwareAcceleration = !state.exportUseHardwareAcceleration) },
                            enabled = !state.isWorking
                        ) {
                            Text(
                                if (state.exportUseHardwareAcceleration) {
                                    "HW Acceleration: On"
                                } else {
                                    "HW Acceleration: Off"
                                }
                            )
                        }
                        Button(onClick = ::queueBatchExport, enabled = !state.isWorking) {
                            Text("Queue Batch Export")
                        }
                        Button(onClick = ::refreshExportJobStatus, enabled = !state.isWorking) {
                            Text("Refresh Job")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.proVideoOutputPath.isNotBlank()) {
                            Button(
                                onClick = {
                                    val merged = (parseBulkList(state.exportSourcePathsInput) + state.proVideoOutputPath).distinct()
                                    state = state.copy(exportSourcePathsInput = merged.joinToString("\n"))
                                },
                                enabled = !state.isWorking
                            ) { Text("Add Pro Video Output") }
                        }
                        if (state.timelineOutputPath.isNotBlank()) {
                            Button(
                                onClick = {
                                    val merged = (parseBulkList(state.exportSourcePathsInput) + state.timelineOutputPath).distinct()
                                    state = state.copy(exportSourcePathsInput = merged.joinToString("\n"))
                                },
                                enabled = !state.isWorking
                            ) { Text("Add Timeline Output") }
                        }
                        if (state.captionBurnOutputPath.isNotBlank()) {
                            Button(
                                onClick = {
                                    val merged = (parseBulkList(state.exportSourcePathsInput) + state.captionBurnOutputPath).distinct()
                                    state = state.copy(exportSourcePathsInput = merged.joinToString("\n"))
                                },
                                enabled = !state.isWorking
                            ) { Text("Add Burned Caption Output") }
                        }
                        if (state.photoOutputPath.isNotBlank()) {
                            Button(
                                onClick = {
                                    val merged = (parseBulkList(state.exportSourcePathsInput) + state.photoOutputPath).distinct()
                                    state = state.copy(exportSourcePathsInput = merged.joinToString("\n"))
                                },
                                enabled = !state.isWorking
                            ) { Text("Add Photo Output") }
                        }
                    }
                    if (state.exportStatusText.isNotBlank()) {
                        Text(state.exportStatusText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.exportQueuedJobId.isNotBlank()) {
                        Text(
                            "Job ${state.exportQueuedJobId} | status=${state.exportLastStatus} | queued=${state.exportQueuedItems} completed=${state.exportCompletedItems} failed=${state.exportFailedItems}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Export Presets", style = MaterialTheme.typography.titleSmall)
                    state.exportPresets.forEach { preset ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    onClick = { state = state.copy(selectedExportPresetId = preset.id) },
                                    enabled = !state.isWorking
                                ) {
                                    Text(if (state.selectedExportPresetId == preset.id) "Selected" else "Select")
                                }
                            }
                        }
                    }
                    if (state.exportItemSummaries.isNotEmpty()) {
                        Text("Batch Items", style = MaterialTheme.typography.titleSmall)
                        state.exportItemSummaries.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodySmall)
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
                    Text("Audio Finishing", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Denoise, EQ, compressor/limiter, gate, ducking, and loudness normalization.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.audioFinSourcePath,
                        onValueChange = { state = state.copy(audioFinSourcePath = it) },
                        label = { Text("Source Path") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.audioFinSourceUrl,
                        onValueChange = { state = state.copy(audioFinSourceUrl = it) },
                        label = { Text("Source URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.audioFinOutputName,
                        onValueChange = { state = state.copy(audioFinOutputName = it) },
                        label = { Text("Output Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.audioFinSoundPath,
                        onValueChange = { state = state.copy(audioFinSoundPath = it, audioFinSoundUrl = "") },
                        label = { Text("Background Sound Path (for ducking)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.audioFinSoundUrl,
                        onValueChange = { state = state.copy(audioFinSoundUrl = it) },
                        label = { Text("Background Sound URL (for ducking)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.audioFinSoundVolume,
                        onValueChange = { state = state.copy(audioFinSoundVolume = it) },
                        label = { Text("Background Sound Volume") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { state = state.copy(audioFinDenoise = !state.audioFinDenoise) },
                            enabled = !state.isWorking
                        ) { Text(if (state.audioFinDenoise) "Denoise*" else "Denoise") }
                        Button(
                            onClick = { state = state.copy(audioFinCompressor = !state.audioFinCompressor) },
                            enabled = !state.isWorking
                        ) { Text(if (state.audioFinCompressor) "Compressor*" else "Compressor") }
                        Button(
                            onClick = { state = state.copy(audioFinLimiter = !state.audioFinLimiter) },
                            enabled = !state.isWorking
                        ) { Text(if (state.audioFinLimiter) "Limiter*" else "Limiter") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { state = state.copy(audioFinGate = !state.audioFinGate) },
                            enabled = !state.isWorking
                        ) { Text(if (state.audioFinGate) "Gate*" else "Gate") }
                        Button(
                            onClick = { state = state.copy(audioFinDucking = !state.audioFinDucking) },
                            enabled = !state.isWorking
                        ) { Text(if (state.audioFinDucking) "Ducking*" else "Ducking") }
                        Button(
                            onClick = { state = state.copy(audioFinLoudnessNormalization = !state.audioFinLoudnessNormalization) },
                            enabled = !state.isWorking
                        ) { Text(if (state.audioFinLoudnessNormalization) "Loudness*" else "Loudness") }
                    }
                    OutlinedTextField(
                        value = state.audioFinEqLowGain,
                        onValueChange = { state = state.copy(audioFinEqLowGain = it) },
                        label = { Text("EQ Low Gain dB (-24..24)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.audioFinEqMidGain,
                        onValueChange = { state = state.copy(audioFinEqMidGain = it) },
                        label = { Text("EQ Mid Gain dB (-24..24)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.audioFinEqHighGain,
                        onValueChange = { state = state.copy(audioFinEqHighGain = it) },
                        label = { Text("EQ High Gain dB (-24..24)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { runAudioFinishing() }, enabled = !state.isWorking) {
                        Text("Run Audio Finishing")
                    }
                    if (state.audioFinStatusText.isNotBlank()) {
                        Text(state.audioFinStatusText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.audioFinOutputPath.isNotBlank()) {
                        Text("Audio-finished output: ${state.audioFinOutputPath}", style = MaterialTheme.typography.bodySmall)
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
                    Text("Pro Photo Controls", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Layers/masks, object removal/healing, background removal, selective edits, and RAW-style enhancement.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.photoSourcePath,
                        onValueChange = { state = state.copy(photoSourcePath = it, photoSourceUrl = "") },
                        label = { Text("Photo Source Path (local)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoSourceUrl,
                        onValueChange = { state = state.copy(photoSourceUrl = it) },
                        label = { Text("Photo Source URL (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoOutputName,
                        onValueChange = { state = state.copy(photoOutputName = it) },
                        label = { Text("Photo Output Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoPresetId,
                        onValueChange = { state = state.copy(photoPresetId = it) },
                        label = { Text("Preset ID (optional; auto-load if empty)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoOverlayPath,
                        onValueChange = { state = state.copy(photoOverlayPath = it, photoOverlayUrl = "") },
                        label = { Text("Overlay Path (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoOverlayUrl,
                        onValueChange = { state = state.copy(photoOverlayUrl = it) },
                        label = { Text("Overlay URL (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoTextOverlay,
                        onValueChange = { state = state.copy(photoTextOverlay = it) },
                        label = { Text("Text Overlay (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { state = state.copy(photoSepia = !state.photoSepia) }, enabled = !state.isWorking) {
                            Text(if (state.photoSepia) "Sepia*" else "Sepia")
                        }
                        Button(onClick = { state = state.copy(photoBlackWhite = !state.photoBlackWhite) }, enabled = !state.isWorking) {
                            Text(if (state.photoBlackWhite) "B&W*" else "B&W")
                        }
                        Button(onClick = { state = state.copy(photoCartoonify = !state.photoCartoonify) }, enabled = !state.isWorking) {
                            Text(if (state.photoCartoonify) "Cartoon*" else "Cartoon")
                        }
                        Button(onClick = { state = state.copy(photoCaricature = !state.photoCaricature) }, enabled = !state.isWorking) {
                            Text(if (state.photoCaricature) "Caricature*" else "Caricature")
                        }
                    }
                    OutlinedTextField(
                        value = state.photoSaturationFactor,
                        onValueChange = { state = state.copy(photoSaturationFactor = it) },
                        label = { Text("Saturation Factor (1.0 default)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoBrightnessFactor,
                        onValueChange = { state = state.copy(photoBrightnessFactor = it) },
                        label = { Text("Brightness Factor (1.0 default)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoContrastFactor,
                        onValueChange = { state = state.copy(photoContrastFactor = it) },
                        label = { Text("Contrast Factor (1.0 default)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { state = state.copy(photoRawEnhance = !state.photoRawEnhance) }, enabled = !state.isWorking) {
                        Text(if (state.photoRawEnhance) "RAW Enhance: On" else "RAW Enhance: Off")
                    }
                    Text("Selective Edits", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = state.photoSelectiveX,
                        onValueChange = { state = state.copy(photoSelectiveX = it) },
                        label = { Text("Selective X (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoSelectiveY,
                        onValueChange = { state = state.copy(photoSelectiveY = it) },
                        label = { Text("Selective Y (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoSelectiveWidth,
                        onValueChange = { state = state.copy(photoSelectiveWidth = it) },
                        label = { Text("Selective Width (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoSelectiveHeight,
                        onValueChange = { state = state.copy(photoSelectiveHeight = it) },
                        label = { Text("Selective Height (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoSelectiveBrightnessFactor,
                        onValueChange = { state = state.copy(photoSelectiveBrightnessFactor = it) },
                        label = { Text("Selective Brightness (1.0 default)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoSelectiveContrastFactor,
                        onValueChange = { state = state.copy(photoSelectiveContrastFactor = it) },
                        label = { Text("Selective Contrast (1.0 default)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoSelectiveSaturationFactor,
                        onValueChange = { state = state.copy(photoSelectiveSaturationFactor = it) },
                        label = { Text("Selective Saturation (1.0 default)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Background Removal", style = MaterialTheme.typography.titleSmall)
                    Button(onClick = { state = state.copy(photoBackgroundRemove = !state.photoBackgroundRemove) }, enabled = !state.isWorking) {
                        Text(if (state.photoBackgroundRemove) "Background Remove: On" else "Background Remove: Off")
                    }
                    OutlinedTextField(
                        value = state.photoBackgroundKeyColor,
                        onValueChange = { state = state.copy(photoBackgroundKeyColor = it) },
                        label = { Text("Key Color (#00FF00 default)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoBackgroundKeyTolerance,
                        onValueChange = { state = state.copy(photoBackgroundKeyTolerance = it) },
                        label = { Text("Key Tolerance (0..255)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Object Erase Regions (${state.photoEraseRegions.size})", style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = { state = state.copy(photoEraseRegions = state.photoEraseRegions + ImageEraseRegionDraft()) },
                        enabled = !state.isWorking
                    ) {
                        Text("Add Erase Region")
                    }
                    state.photoEraseRegions.forEachIndexed { index, region ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Region ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = region.x,
                                    onValueChange = { updatePhotoEraseRegion(index) { current -> current.copy(x = it) } },
                                    label = { Text("X") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = region.y,
                                    onValueChange = { updatePhotoEraseRegion(index) { current -> current.copy(y = it) } },
                                    label = { Text("Y") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = region.width,
                                    onValueChange = { updatePhotoEraseRegion(index) { current -> current.copy(width = it) } },
                                    label = { Text("Width") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = region.height,
                                    onValueChange = { updatePhotoEraseRegion(index) { current -> current.copy(height = it) } },
                                    label = { Text("Height") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        state = state.copy(
                                            photoEraseRegions = state.photoEraseRegions.filterIndexed { i, _ -> i != index }
                                        )
                                    },
                                    enabled = !state.isWorking
                                ) {
                                    Text("Remove Region")
                                }
                            }
                        }
                    }
                    Text("Layers + Masks (${state.photoLayers.size})", style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = { state = state.copy(photoLayers = state.photoLayers + ImageLayerDraft()) },
                        enabled = !state.isWorking
                    ) {
                        Text("Add Layer")
                    }
                    state.photoLayers.forEachIndexed { index, layer ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Layer ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = layer.layerPath,
                                    onValueChange = { updatePhotoLayer(index) { current -> current.copy(layerPath = it, layerUrl = "") } },
                                    label = { Text("Layer Path (local)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = layer.layerUrl,
                                    onValueChange = { updatePhotoLayer(index) { current -> current.copy(layerUrl = it) } },
                                    label = { Text("Layer URL (optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = layer.maskPath,
                                    onValueChange = { updatePhotoLayer(index) { current -> current.copy(maskPath = it, maskUrl = "") } },
                                    label = { Text("Mask Path (optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = layer.maskUrl,
                                    onValueChange = { updatePhotoLayer(index) { current -> current.copy(maskUrl = it) } },
                                    label = { Text("Mask URL (optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = layer.opacity,
                                    onValueChange = { updatePhotoLayer(index) { current -> current.copy(opacity = it) } },
                                    label = { Text("Opacity (0..1)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = layer.blendMode,
                                    onValueChange = { updatePhotoLayer(index) { current -> current.copy(blendMode = it) } },
                                    label = { Text("Blend Mode (normal/multiply/screen/overlay)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = layer.x,
                                    onValueChange = { updatePhotoLayer(index) { current -> current.copy(x = it) } },
                                    label = { Text("Layer X") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = layer.y,
                                    onValueChange = { updatePhotoLayer(index) { current -> current.copy(y = it) } },
                                    label = { Text("Layer Y") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        state = state.copy(
                                            photoLayers = state.photoLayers.filterIndexed { i, _ -> i != index }
                                        )
                                    },
                                    enabled = !state.isWorking
                                ) {
                                    Text("Remove Layer")
                                }
                            }
                        }
                    }
                    Text("Heal / Object Removal", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = state.photoHealMaskPath,
                        onValueChange = { state = state.copy(photoHealMaskPath = it, photoHealMaskUrl = "") },
                        label = { Text("Heal Mask Path (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoHealMaskUrl,
                        onValueChange = { state = state.copy(photoHealMaskUrl = it) },
                        label = { Text("Heal Mask URL (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoHealOutputName,
                        onValueChange = { state = state.copy(photoHealOutputName = it) },
                        label = { Text("Heal Output Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoHealInpaintRadius,
                        onValueChange = { state = state.copy(photoHealInpaintRadius = it) },
                        label = { Text("Inpaint Radius") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoHealMethod,
                        onValueChange = { state = state.copy(photoHealMethod = it) },
                        label = { Text("Heal Method (telea/ns)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoHealFillStrategy,
                        onValueChange = { state = state.copy(photoHealFillStrategy = it) },
                        label = { Text("Fill Strategy (inpaint/median/blur)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoHealFeatherRadius,
                        onValueChange = { state = state.copy(photoHealFeatherRadius = it) },
                        label = { Text("Feather Radius") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { state = state.copy(photoHealPreserveEdges = !state.photoHealPreserveEdges) }, enabled = !state.isWorking) {
                        Text(if (state.photoHealPreserveEdges) "Preserve Edges: On" else "Preserve Edges: Off")
                    }
                    OutlinedTextField(
                        value = state.photoHealEdgeBlend,
                        onValueChange = { state = state.copy(photoHealEdgeBlend = it) },
                        label = { Text("Edge Blend (0..1)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.photoHealDenoiseStrength,
                        onValueChange = { state = state.copy(photoHealDenoiseStrength = it) },
                        label = { Text("Denoise Strength (0..1)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::runProPhotoFilter, enabled = !state.isWorking) {
                            Text("Apply Pro Photo")
                        }
                        Button(onClick = ::runProPhotoHeal, enabled = !state.isWorking) {
                            Text("Run Heal / Remove")
                        }
                    }
                    if (state.photoStatusText.isNotBlank()) {
                        Text(state.photoStatusText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.photoOutputPath.isNotBlank()) {
                        Text("Photo output: ${state.photoOutputPath}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.photoHealOutputPath.isNotBlank()) {
                        Text("Photo heal output: ${state.photoHealOutputPath}", style = MaterialTheme.typography.bodySmall)
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
                    Text("Pro Video Controls", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Stabilization, speed ramps, LUT/curve grading, plus motion tracking.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.proVideoSourcePath,
                        onValueChange = { state = state.copy(proVideoSourcePath = it) },
                        label = { Text("Source Path") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proVideoSourceUrl,
                        onValueChange = { state = state.copy(proVideoSourceUrl = it) },
                        label = { Text("Source URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proVideoOutputName,
                        onValueChange = { state = state.copy(proVideoOutputName = it) },
                        label = { Text("Output Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proVideoSpeedFactor,
                        onValueChange = { state = state.copy(proVideoSpeedFactor = it) },
                        label = { Text("Speed Ramp Factor (0.10..4.0)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { state = state.copy(proVideoStabilize = !state.proVideoStabilize) },
                        enabled = !state.isWorking
                    ) {
                        Text(if (state.proVideoStabilize) "Stabilization: On" else "Stabilization: Off")
                    }
                    OutlinedTextField(
                        value = state.proVideoCurvesPreset,
                        onValueChange = { state = state.copy(proVideoCurvesPreset = it) },
                        label = { Text("Curves Preset") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { state = state.copy(proVideoCurvesPreset = "medium_contrast") }, enabled = !state.isWorking) {
                            Text("Medium Contrast")
                        }
                        Button(onClick = { state = state.copy(proVideoCurvesPreset = "vintage") }, enabled = !state.isWorking) {
                            Text("Vintage")
                        }
                        Button(onClick = { state = state.copy(proVideoCurvesPreset = "strong_contrast") }, enabled = !state.isWorking) {
                            Text("Strong Contrast")
                        }
                    }
                    OutlinedTextField(
                        value = state.proVideoLutPath,
                        onValueChange = { state = state.copy(proVideoLutPath = it, proVideoLutUrl = "") },
                        label = { Text("LUT Path (.cube)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proVideoLutUrl,
                        onValueChange = { state = state.copy(proVideoLutUrl = it) },
                        label = { Text("LUT URL (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proVideoOverlayPath,
                        onValueChange = { state = state.copy(proVideoOverlayPath = it, proVideoOverlayUrl = "") },
                        label = { Text("Overlay Path (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proVideoOverlayUrl,
                        onValueChange = { state = state.copy(proVideoOverlayUrl = it) },
                        label = { Text("Overlay URL (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proVideoTextOverlay,
                        onValueChange = { state = state.copy(proVideoTextOverlay = it) },
                        label = { Text("Text Overlay (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::runProVideoControls, enabled = !state.isWorking) {
                            Text("Apply Pro Video")
                        }
                        Button(onClick = ::runProMotionTracking, enabled = !state.isWorking) {
                            Text("Run Motion Tracking")
                        }
                    }
                    Text("Motion Tracking", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = state.proMotionStartSeconds,
                        onValueChange = { state = state.copy(proMotionStartSeconds = it) },
                        label = { Text("Start Seconds") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proMotionEndSeconds,
                        onValueChange = { state = state.copy(proMotionEndSeconds = it) },
                        label = { Text("End Seconds (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proMotionRoiX,
                        onValueChange = { state = state.copy(proMotionRoiX = it) },
                        label = { Text("ROI X (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proMotionRoiY,
                        onValueChange = { state = state.copy(proMotionRoiY = it) },
                        label = { Text("ROI Y (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proMotionRoiWidth,
                        onValueChange = { state = state.copy(proMotionRoiWidth = it) },
                        label = { Text("ROI Width (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proMotionRoiHeight,
                        onValueChange = { state = state.copy(proMotionRoiHeight = it) },
                        label = { Text("ROI Height (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proMotionSmoothingWindow,
                        onValueChange = { state = state.copy(proMotionSmoothingWindow = it) },
                        label = { Text("Smoothing Window") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proMotionMinConfidence,
                        onValueChange = { state = state.copy(proMotionMinConfidence = it) },
                        label = { Text("Min Confidence") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.proMotionOverlayOutputName,
                        onValueChange = { state = state.copy(proMotionOverlayOutputName = it) },
                        label = { Text("Overlay Output Name (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.proVideoStatusText.isNotBlank()) {
                        Text(state.proVideoStatusText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.proVideoOutputPath.isNotBlank()) {
                        Text("Pro video output: ${state.proVideoOutputPath}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.proMotionSummary.isNotBlank()) {
                        Text(state.proMotionSummary, style = MaterialTheme.typography.bodySmall)
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

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Timeline Editor", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Multiclip video timeline + multitrack audio with trim/split/transitions/keyframes.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.timelineOutputName,
                        onValueChange = { state = state.copy(timelineOutputName = it) },
                        label = { Text("Output Name (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.timelineTransitionDefault,
                        onValueChange = { state = state.copy(timelineTransitionDefault = it) },
                        label = { Text("Default Transition (none/fade)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { state = state.copy(timelineNormalizeLoudness = !state.timelineNormalizeLoudness) }, enabled = !state.isWorking) {
                        Text(if (state.timelineNormalizeLoudness) "Normalize Loudness: On" else "Normalize Loudness: Off")
                    }
                    OutlinedTextField(
                        value = state.timelineBackgroundAudioPath,
                        onValueChange = { state = state.copy(timelineBackgroundAudioPath = it, timelineBackgroundAudioUrl = "") },
                        label = { Text("Background Audio Path (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.timelineBackgroundAudioUrl,
                        onValueChange = { state = state.copy(timelineBackgroundAudioUrl = it) },
                        label = { Text("Background Audio URL (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.timelineBackgroundAudioVolume,
                        onValueChange = { state = state.copy(timelineBackgroundAudioVolume = it) },
                        label = { Text("Background Audio Volume") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Video Clips (${state.timelineClips.size})", style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = {
                            state = state.copy(timelineClips = state.timelineClips + TimelineClipDraft())
                        },
                        enabled = !state.isWorking
                    ) {
                        Text("Add Clip")
                    }
                    state.timelineClips.forEachIndexed { index, clip ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Clip ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = clip.sourcePath,
                                    onValueChange = { updateTimelineClip(index) { current -> current.copy(sourcePath = it, sourceUrl = "") } },
                                    label = { Text("Source Path (local)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = clip.sourceUrl,
                                    onValueChange = { updateTimelineClip(index) { current -> current.copy(sourceUrl = it) } },
                                    label = { Text("Source URL (optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = clip.startSeconds,
                                    onValueChange = { updateTimelineClip(index) { current -> current.copy(startSeconds = it) } },
                                    label = { Text("Trim Start Seconds") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = clip.endSeconds,
                                    onValueChange = { updateTimelineClip(index) { current -> current.copy(endSeconds = it) } },
                                    label = { Text("Trim End Seconds (optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = clip.splitPointsCsv,
                                    onValueChange = { updateTimelineClip(index) { current -> current.copy(splitPointsCsv = it) } },
                                    label = { Text("Split Points CSV (e.g. 3.2,7.9)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = clip.speedFactor,
                                    onValueChange = { updateTimelineClip(index) { current -> current.copy(speedFactor = it) } },
                                    label = { Text("Speed Factor") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = clip.volume,
                                    onValueChange = { updateTimelineClip(index) { current -> current.copy(volume = it) } },
                                    label = { Text("Clip Volume") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = clip.transitionToNext,
                                    onValueChange = { updateTimelineClip(index) { current -> current.copy(transitionToNext = it) } },
                                    label = { Text("Transition To Next (none/fade)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = clip.transitionDuration,
                                    onValueChange = { updateTimelineClip(index) { current -> current.copy(transitionDuration = it) } },
                                    label = { Text("Transition Duration Seconds") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { updateTimelineClip(index) { current -> current.copy(transitionToNext = "none") } },
                                        enabled = !state.isWorking
                                    ) { Text("None") }
                                    Button(
                                        onClick = { updateTimelineClip(index) { current -> current.copy(transitionToNext = "fade") } },
                                        enabled = !state.isWorking
                                    ) { Text("Fade") }
                                    Button(
                                        onClick = {
                                            val updated = state.timelineClips.filterIndexed { i, _ -> i != index }
                                            state = state.copy(
                                                timelineClips = if (updated.isEmpty()) listOf(TimelineClipDraft()) else updated
                                            )
                                        },
                                        enabled = !state.isWorking
                                    ) { Text("Remove Clip") }
                                }
                            }
                        }
                    }

                    Text("Audio Tracks (${state.timelineAudioTracks.size})", style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = {
                            state = state.copy(timelineAudioTracks = state.timelineAudioTracks + TimelineAudioTrackDraft())
                        },
                        enabled = !state.isWorking
                    ) {
                        Text("Add Audio Track")
                    }
                    state.timelineAudioTracks.forEachIndexed { index, track ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Track ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = track.sourcePath,
                                    onValueChange = { updateTimelineAudioTrack(index) { current -> current.copy(sourcePath = it, sourceUrl = "") } },
                                    label = { Text("Audio Path (local)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = track.sourceUrl,
                                    onValueChange = { updateTimelineAudioTrack(index) { current -> current.copy(sourceUrl = it) } },
                                    label = { Text("Audio URL (optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = track.startSeconds,
                                    onValueChange = { updateTimelineAudioTrack(index) { current -> current.copy(startSeconds = it) } },
                                    label = { Text("Start Seconds") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = track.endSeconds,
                                    onValueChange = { updateTimelineAudioTrack(index) { current -> current.copy(endSeconds = it) } },
                                    label = { Text("End Seconds (optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = track.volume,
                                    onValueChange = { updateTimelineAudioTrack(index) { current -> current.copy(volume = it) } },
                                    label = { Text("Track Volume") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        state = state.copy(
                                            timelineAudioTracks = state.timelineAudioTracks.filterIndexed { i, _ -> i != index }
                                        )
                                    },
                                    enabled = !state.isWorking
                                ) {
                                    Text("Remove Track")
                                }
                            }
                        }
                    }

                    Text("Keyframes (${state.timelineKeyframes.size})", style = MaterialTheme.typography.titleSmall)
                    Text("Supported properties: brightness, contrast, saturation", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { state = state.copy(timelineKeyframes = state.timelineKeyframes + TimelineKeyframeDraft()) },
                        enabled = !state.isWorking
                    ) {
                        Text("Add Keyframe")
                    }
                    state.timelineKeyframes.forEachIndexed { index, keyframe ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Keyframe ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = keyframe.atSeconds,
                                    onValueChange = { updateTimelineKeyframe(index) { current -> current.copy(atSeconds = it) } },
                                    label = { Text("At Seconds") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = keyframe.property,
                                    onValueChange = { updateTimelineKeyframe(index) { current -> current.copy(property = it) } },
                                    label = { Text("Property") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { updateTimelineKeyframe(index) { current -> current.copy(property = "brightness") } }, enabled = !state.isWorking) {
                                        Text("Brightness")
                                    }
                                    Button(onClick = { updateTimelineKeyframe(index) { current -> current.copy(property = "contrast") } }, enabled = !state.isWorking) {
                                        Text("Contrast")
                                    }
                                    Button(onClick = { updateTimelineKeyframe(index) { current -> current.copy(property = "saturation") } }, enabled = !state.isWorking) {
                                        Text("Saturation")
                                    }
                                }
                                OutlinedTextField(
                                    value = keyframe.value,
                                    onValueChange = { updateTimelineKeyframe(index) { current -> current.copy(value = it) } },
                                    label = { Text("Value") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        state = state.copy(
                                            timelineKeyframes = state.timelineKeyframes.filterIndexed { i, _ -> i != index }
                                        )
                                    },
                                    enabled = !state.isWorking
                                ) {
                                    Text("Remove Keyframe")
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::composeTimeline, enabled = !state.isWorking) {
                            Text("Compose Timeline")
                        }
                    }
                    if (state.timelineStatusText.isNotBlank()) {
                        Text(state.timelineStatusText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.timelineOutputPath.isNotBlank()) {
                        Text("Timeline output: ${state.timelineOutputPath}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.timelineWarnings.isNotEmpty()) {
                        Text("Timeline warnings:", style = MaterialTheme.typography.bodySmall)
                        state.timelineWarnings.forEach { warning ->
                            Text("- $warning", style = MaterialTheme.typography.bodySmall)
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
                    Text("Captions / Subtitles", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Auto-transcribe, edit subtitle cues, generate SRT, then burn subtitles into the video output.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.captionSourcePath,
                        onValueChange = { state = state.copy(captionSourcePath = it) },
                        label = { Text("Caption Source Path (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.captionSourceUrl,
                        onValueChange = { state = state.copy(captionSourceUrl = it) },
                        label = { Text("Caption Source URL (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.captionLanguage,
                        onValueChange = { state = state.copy(captionLanguage = it) },
                        label = { Text("Language (en)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.captionOutputName,
                        onValueChange = { state = state.copy(captionOutputName = it) },
                        label = { Text("Subtitle Output Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::transcribeCaptions, enabled = !state.isWorking) {
                            Text("Auto-Transcribe")
                        }
                        Button(onClick = ::buildSrtFromCues, enabled = !state.isWorking) {
                            Text("Build SRT From Cues")
                        }
                    }

                    Text("Subtitle Cues (${state.captionCues.size})", style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = { state = state.copy(captionCues = state.captionCues + CaptionCueDraft()) },
                        enabled = !state.isWorking
                    ) {
                        Text("Add Cue")
                    }
                    state.captionCues.forEachIndexed { index, cue ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Cue ${index + 1}", style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = cue.startSeconds,
                                    onValueChange = { updateCaptionCue(index) { current -> current.copy(startSeconds = it) } },
                                    label = { Text("Start Seconds") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = cue.endSeconds,
                                    onValueChange = { updateCaptionCue(index) { current -> current.copy(endSeconds = it) } },
                                    label = { Text("End Seconds") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = cue.text,
                                    onValueChange = { updateCaptionCue(index) { current -> current.copy(text = it) } },
                                    label = { Text("Subtitle Text") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        val remaining = state.captionCues.filterIndexed { i, _ -> i != index }
                                        state = state.copy(
                                            captionCues = if (remaining.isEmpty()) listOf(CaptionCueDraft()) else remaining
                                        )
                                    },
                                    enabled = !state.isWorking
                                ) {
                                    Text("Remove Cue")
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.captionSubtitlePath,
                        onValueChange = { state = state.copy(captionSubtitlePath = it, captionSubtitleUrl = "") },
                        label = { Text("Subtitle Path For Burn-In (SRT)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.captionSubtitleUrl,
                        onValueChange = { state = state.copy(captionSubtitleUrl = it) },
                        label = { Text("Subtitle URL For Burn-In (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.captionBurnOutputName,
                        onValueChange = { state = state.copy(captionBurnOutputName = it) },
                        label = { Text("Burned Video Output Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = ::burnSubtitles, enabled = !state.isWorking) {
                        Text("Burn Subtitles Into Video")
                    }
                    if (state.captionStatusText.isNotBlank()) {
                        Text(state.captionStatusText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.captionEngine.isNotBlank()) {
                        Text("Engine: ${state.captionEngine}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.captionCueCount > 0) {
                        Text("Cue count: ${state.captionCueCount}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.captionGeneratedSrtPath.isNotBlank()) {
                        Text("Generated SRT: ${state.captionGeneratedSrtPath}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.captionBurnOutputPath.isNotBlank()) {
                        Text("Burned video output: ${state.captionBurnOutputPath}", style = MaterialTheme.typography.bodySmall)
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
