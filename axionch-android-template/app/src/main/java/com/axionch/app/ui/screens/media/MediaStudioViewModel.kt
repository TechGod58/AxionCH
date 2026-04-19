package com.axionch.app.ui.screens.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axionch.app.data.repo.AxionRepository
import com.axionch.shared.api.AssetUpsertRequest
import com.axionch.shared.api.CaptionCue
import com.axionch.shared.api.CaptionSrtBuildRequest
import com.axionch.shared.api.ImageEraseRegion
import com.axionch.shared.api.LiveDestinationConfig
import com.axionch.shared.api.LiveMulticastSessionResponse
import com.axionch.shared.api.LiveMulticastStartRequest
import com.axionch.shared.api.TimelineAudioTrack
import com.axionch.shared.api.TimelineClip
import com.axionch.shared.api.TimelineComposeRequest
import com.axionch.shared.api.TimelineKeyframe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimelineClipDraft(
    val id: Int,
    val sourcePath: String = "",
    val sourceUrl: String = "",
    val startSeconds: String = "0.0",
    val endSeconds: String = "",
    val speedFactor: String = "1.0",
    val volume: String = "1.0",
    val splitPointsCsv: String = "",
    val transitionToNext: String = "fade",
    val transitionDuration: String = "0.35"
)

data class MediaStudioUiState(
    val selectedTab: String = "video",
    val templatesSummary: String = "Not loaded",
    val presetsSummary: String = "Not loaded",
    val selectedPresetId: String = "",
    val sourcePath: String = "",
    val sourceUrl: String = "",
    val audioTrackPath: String = "",
    val timelineOutputName: String = "timeline_output",
    val timelineClips: List<TimelineClipDraft> = listOf(TimelineClipDraft(id = 1)),
    val nextTimelineClipId: Int = 2,
    val brightnessKeyframesCsv: String = "",
    val contrastKeyframesCsv: String = "",
    val saturationKeyframesCsv: String = "",
    val exportJobId: String = "",
    val exportJobStatus: String = "",
    val captionLanguage: String = "en",
    val captionSrtOutputName: String = "captions_auto",
    val captionResultText: String = "",
    val motionTrackSummary: String = "",
    val motionRoiX: String = "",
    val motionRoiY: String = "",
    val motionRoiWidth: String = "",
    val motionRoiHeight: String = "",
    val motionSmoothingWindow: String = "4",
    val motionMinConfidence: String = "0.05",
    val motionOverlayOutputName: String = "",
    val healMaskPath: String = "",
    val healRegionsCsv: String = "",
    val healOutputName: String = "healed_image",
    val healFillStrategy: String = "hybrid",
    val healEdgeBlend: String = "0.55",
    val healDenoiseStrength: String = "0.0",
    val healSummary: String = "",
    val liveSessionName: String = "",
    val liveSourcePath: String = "",
    val liveSourceUrl: String = "",
    val liveXIngestUrl: String = "",
    val liveXStreamKey: String = "",
    val liveLinkedinIngestUrl: String = "",
    val liveLinkedinStreamKey: String = "",
    val liveInstagramIngestUrl: String = "",
    val liveInstagramStreamKey: String = "",
    val liveDryRun: Boolean = true,
    val liveCopyVideo: Boolean = false,
    val liveVideoBitrate: String = "4500k",
    val liveAudioBitrate: String = "160k",
    val liveFps: String = "30",
    val liveGopSeconds: String = "2.0",
    val liveCurrentSessionId: String = "",
    val liveStatusSummary: String = "",
    val liveSessions: List<LiveMulticastSessionResponse> = emptyList(),
    val assetKind: String = "audio",
    val assetTitle: String = "",
    val assetSource: String = "",
    val assetLicense: String = "",
    val assetUrlOrPath: String = "",
    val assetSummary: String = "No assets loaded",
    val message: String = "",
    val isWorking: Boolean = false
)

private fun parseSplitPoints(raw: String): List<Double> {
    return raw.split(",")
        .mapNotNull { token -> token.trim().toDoubleOrNull() }
        .filter { it > 0.0 }
}

private fun parseKeyframes(property: String, raw: String): List<TimelineKeyframe> {
    return raw.split(";")
        .mapNotNull { segment ->
            val parts = segment.split(":")
            if (parts.size != 2) {
                null
            } else {
                val at = parts[0].trim().toDoubleOrNull()
                val value = parts[1].trim().toDoubleOrNull()
                if (at == null || value == null) {
                    null
                } else {
                    TimelineKeyframe(
                        at_seconds = at,
                        property = property,
                        value = value.toString()
                    )
                }
            }
        }
}

private fun parseHealRegions(raw: String): List<ImageEraseRegion> {
    // "x,y,w,h; x,y,w,h"
    return raw.split(";")
        .mapNotNull { segment ->
            val parts = segment.split(",").map { it.trim() }
            if (parts.size != 4) {
                null
            } else {
                val x = parts[0].toIntOrNull()
                val y = parts[1].toIntOrNull()
                val w = parts[2].toIntOrNull()
                val h = parts[3].toIntOrNull()
                if (x == null || y == null || w == null || h == null) null else ImageEraseRegion(x, y, w, h)
            }
        }
}

private fun parseTimelineClip(draft: TimelineClipDraft): TimelineClip? {
    val sourcePath = draft.sourcePath.trim().ifBlank { null }
    val sourceUrl = draft.sourceUrl.trim().ifBlank { null }
    if (sourcePath == null && sourceUrl == null) {
        return null
    }
    val startSeconds = draft.startSeconds.trim().toDoubleOrNull() ?: 0.0
    val endSeconds = draft.endSeconds.trim().toDoubleOrNull()
    val speed = (draft.speedFactor.trim().toDoubleOrNull() ?: 1.0).coerceIn(0.1, 4.0)
    val volume = (draft.volume.trim().toDoubleOrNull() ?: 1.0).coerceAtLeast(0.0)
    val transitionDuration = (draft.transitionDuration.trim().toDoubleOrNull() ?: 0.35).coerceIn(0.05, 2.0)
    return TimelineClip(
        source_path = sourcePath,
        source_url = sourceUrl,
        start_seconds = startSeconds,
        end_seconds = endSeconds,
        speed_factor = speed,
        volume = volume,
        split_points = parseSplitPoints(draft.splitPointsCsv),
        transition_to_next = draft.transitionToNext.trim().ifBlank { null },
        transition_duration = transitionDuration
    )
}

class MediaStudioViewModel(
    private val repository: AxionRepository = AxionRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MediaStudioUiState())
    val uiState: StateFlow<MediaStudioUiState> = _uiState.asStateFlow()

    fun setTab(tab: String) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateSourcePath(value: String) = _uiState.update { it.copy(sourcePath = value) }
    fun updateSourceUrl(value: String) = _uiState.update { it.copy(sourceUrl = value) }
    fun updateAudioTrackPath(value: String) = _uiState.update { it.copy(audioTrackPath = value) }
    fun updateTimelineOutputName(value: String) = _uiState.update { it.copy(timelineOutputName = value) }
    fun updateBrightnessKeyframesCsv(value: String) = _uiState.update { it.copy(brightnessKeyframesCsv = value) }
    fun updateContrastKeyframesCsv(value: String) = _uiState.update { it.copy(contrastKeyframesCsv = value) }
    fun updateSaturationKeyframesCsv(value: String) = _uiState.update { it.copy(saturationKeyframesCsv = value) }
    fun updateCaptionLanguage(value: String) = _uiState.update { it.copy(captionLanguage = value) }
    fun updateCaptionOutputName(value: String) = _uiState.update { it.copy(captionSrtOutputName = value) }
    fun updateHealMaskPath(value: String) = _uiState.update { it.copy(healMaskPath = value) }
    fun updateHealRegionsCsv(value: String) = _uiState.update { it.copy(healRegionsCsv = value) }
    fun updateHealOutputName(value: String) = _uiState.update { it.copy(healOutputName = value) }
    fun updateHealFillStrategy(value: String) = _uiState.update { it.copy(healFillStrategy = value) }
    fun updateHealEdgeBlend(value: String) = _uiState.update { it.copy(healEdgeBlend = value) }
    fun updateHealDenoiseStrength(value: String) = _uiState.update { it.copy(healDenoiseStrength = value) }
    fun updateMotionRoiX(value: String) = _uiState.update { it.copy(motionRoiX = value) }
    fun updateMotionRoiY(value: String) = _uiState.update { it.copy(motionRoiY = value) }
    fun updateMotionRoiWidth(value: String) = _uiState.update { it.copy(motionRoiWidth = value) }
    fun updateMotionRoiHeight(value: String) = _uiState.update { it.copy(motionRoiHeight = value) }
    fun updateMotionSmoothingWindow(value: String) = _uiState.update { it.copy(motionSmoothingWindow = value) }
    fun updateMotionMinConfidence(value: String) = _uiState.update { it.copy(motionMinConfidence = value) }
    fun updateMotionOverlayOutputName(value: String) = _uiState.update { it.copy(motionOverlayOutputName = value) }
    fun updateLiveSessionName(value: String) = _uiState.update { it.copy(liveSessionName = value) }
    fun updateLiveSourcePath(value: String) = _uiState.update { it.copy(liveSourcePath = value) }
    fun updateLiveSourceUrl(value: String) = _uiState.update { it.copy(liveSourceUrl = value) }
    fun updateLiveXIngestUrl(value: String) = _uiState.update { it.copy(liveXIngestUrl = value) }
    fun updateLiveXStreamKey(value: String) = _uiState.update { it.copy(liveXStreamKey = value) }
    fun updateLiveLinkedinIngestUrl(value: String) = _uiState.update { it.copy(liveLinkedinIngestUrl = value) }
    fun updateLiveLinkedinStreamKey(value: String) = _uiState.update { it.copy(liveLinkedinStreamKey = value) }
    fun updateLiveInstagramIngestUrl(value: String) = _uiState.update { it.copy(liveInstagramIngestUrl = value) }
    fun updateLiveInstagramStreamKey(value: String) = _uiState.update { it.copy(liveInstagramStreamKey = value) }
    fun updateLiveVideoBitrate(value: String) = _uiState.update { it.copy(liveVideoBitrate = value) }
    fun updateLiveAudioBitrate(value: String) = _uiState.update { it.copy(liveAudioBitrate = value) }
    fun updateLiveFps(value: String) = _uiState.update { it.copy(liveFps = value) }
    fun updateLiveGopSeconds(value: String) = _uiState.update { it.copy(liveGopSeconds = value) }
    fun toggleLiveDryRun() = _uiState.update { it.copy(liveDryRun = !it.liveDryRun) }
    fun toggleLiveCopyVideo() = _uiState.update { it.copy(liveCopyVideo = !it.liveCopyVideo) }
    fun updateAssetKind(value: String) = _uiState.update { it.copy(assetKind = value) }
    fun updateAssetTitle(value: String) = _uiState.update { it.copy(assetTitle = value) }
    fun updateAssetSource(value: String) = _uiState.update { it.copy(assetSource = value) }
    fun updateAssetLicense(value: String) = _uiState.update { it.copy(assetLicense = value) }
    fun updateAssetUrlOrPath(value: String) = _uiState.update { it.copy(assetUrlOrPath = value) }

    fun addTimelineClip() {
        _uiState.update { state ->
            val newId = state.nextTimelineClipId
            state.copy(
                timelineClips = state.timelineClips + TimelineClipDraft(
                    id = newId,
                    sourcePath = state.sourcePath,
                    sourceUrl = state.sourceUrl
                ),
                nextTimelineClipId = newId + 1
            )
        }
    }

    fun duplicateTimelineClip(clipId: Int) {
        _uiState.update { state ->
            val source = state.timelineClips.firstOrNull { it.id == clipId } ?: return@update state
            val newId = state.nextTimelineClipId
            state.copy(
                timelineClips = state.timelineClips + source.copy(id = newId),
                nextTimelineClipId = newId + 1
            )
        }
    }

    fun removeTimelineClip(clipId: Int) {
        _uiState.update { state ->
            val remaining = state.timelineClips.filterNot { it.id == clipId }
            if (remaining.isEmpty()) {
                state.copy(
                    timelineClips = listOf(TimelineClipDraft(id = state.nextTimelineClipId)),
                    nextTimelineClipId = state.nextTimelineClipId + 1
                )
            } else {
                state.copy(timelineClips = remaining)
            }
        }
    }

    private fun updateTimelineClip(clipId: Int, transform: (TimelineClipDraft) -> TimelineClipDraft) {
        _uiState.update { state ->
            state.copy(
                timelineClips = state.timelineClips.map { clip ->
                    if (clip.id == clipId) transform(clip) else clip
                }
            )
        }
    }

    fun updateTimelineClipSourcePath(clipId: Int, value: String) =
        updateTimelineClip(clipId) { it.copy(sourcePath = value) }

    fun updateTimelineClipSourceUrl(clipId: Int, value: String) =
        updateTimelineClip(clipId) { it.copy(sourceUrl = value) }

    fun updateTimelineClipStartSeconds(clipId: Int, value: String) =
        updateTimelineClip(clipId) { it.copy(startSeconds = value) }

    fun updateTimelineClipEndSeconds(clipId: Int, value: String) =
        updateTimelineClip(clipId) { it.copy(endSeconds = value) }

    fun updateTimelineClipSpeedFactor(clipId: Int, value: String) =
        updateTimelineClip(clipId) { it.copy(speedFactor = value) }

    fun updateTimelineClipVolume(clipId: Int, value: String) =
        updateTimelineClip(clipId) { it.copy(volume = value) }

    fun updateTimelineClipSplitPointsCsv(clipId: Int, value: String) =
        updateTimelineClip(clipId) { it.copy(splitPointsCsv = value) }

    fun updateTimelineClipTransitionToNext(clipId: Int, value: String) =
        updateTimelineClip(clipId) { it.copy(transitionToNext = value) }

    fun updateTimelineClipTransitionDuration(clipId: Int, value: String) =
        updateTimelineClip(clipId) { it.copy(transitionDuration = value) }

    fun loadMediaCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching {
                val templates = repository.getCreatorTemplates(mediaType = "video")
                val presets = repository.getExportPresets()
                templates to presets
            }.onSuccess { (templates, presets) ->
                val templateNames = templates.templates.take(4).joinToString { it.name }
                val presetNames = presets.presets.take(4).joinToString { it.name }
                _uiState.update {
                    it.copy(
                        templatesSummary = "${templates.total_returned} templates: $templateNames",
                        presetsSummary = "${presets.total_returned} presets: $presetNames",
                        selectedPresetId = it.selectedPresetId.ifBlank { presets.presets.firstOrNull()?.id.orEmpty() },
                        isWorking = false,
                        message = "Catalog loaded"
                    )
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isWorking = false, message = "Catalog load failed: ${it.message}") }
            }
        }
    }

    fun queueExport() {
        val snapshot = _uiState.value
        val sourcePath = snapshot.sourcePath.trim()
        val sourceUrl = snapshot.sourceUrl.trim()
        val presetId = snapshot.selectedPresetId.trim()
        if (presetId.isBlank()) {
            _uiState.update { it.copy(message = "Load catalog and select an export preset first") }
            return
        }
        if (sourcePath.isBlank() && sourceUrl.isBlank()) {
            _uiState.update { it.copy(message = "Provide source path or source URL") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching {
                repository.queueExportJob(
                    sourcePaths = if (sourcePath.isBlank()) emptyList() else listOf(sourcePath),
                    sourceUrls = if (sourceUrl.isBlank()) emptyList() else listOf(sourceUrl),
                    presetId = presetId,
                    outputPrefix = "creatorhub",
                    hardwareAcceleration = true
                )
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        exportJobId = response.job_id,
                        exportJobStatus = response.status,
                        isWorking = false,
                        message = "Queued export job ${response.job_id}"
                    )
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isWorking = false, message = "Queue export failed: ${it.message}") }
            }
        }
    }

    fun refreshExportJob() {
        val jobId = _uiState.value.exportJobId.trim()
        if (jobId.isBlank()) {
            _uiState.update { it.copy(message = "No export job ID available yet") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching { repository.getExportJobStatus(jobId) }
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            exportJobStatus = "${response.status} | completed=${response.completed_items} failed=${response.failed_items}",
                            isWorking = false,
                            message = "Export job refreshed"
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state -> state.copy(isWorking = false, message = "Export status failed: ${it.message}") }
                }
        }
    }

    fun composeTimelineAdvanced() {
        val snapshot = _uiState.value
        val audioPath = snapshot.audioTrackPath.trim()
        val parsedClips = snapshot.timelineClips.mapNotNull(::parseTimelineClip)
        val fallbackClip = if (parsedClips.isEmpty()) {
            parseTimelineClip(
                TimelineClipDraft(
                    id = 0,
                    sourcePath = snapshot.sourcePath,
                    sourceUrl = snapshot.sourceUrl
                )
            )
        } else {
            null
        }
        val clips = if (fallbackClip == null) parsedClips else listOf(fallbackClip)
        if (clips.isEmpty()) {
            _uiState.update { it.copy(message = "Provide at least one timeline clip source path/url") }
            return
        }

        val keyframes = buildList {
            addAll(parseKeyframes("brightness", snapshot.brightnessKeyframesCsv))
            addAll(parseKeyframes("contrast", snapshot.contrastKeyframesCsv))
            addAll(parseKeyframes("saturation", snapshot.saturationKeyframesCsv))
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching {
                repository.composeTimeline(
                    TimelineComposeRequest(
                        clips = clips,
                        audio_tracks = if (audioPath.isBlank()) {
                            emptyList()
                        } else {
                            listOf(
                                TimelineAudioTrack(
                                    source_path = audioPath,
                                    start_seconds = 0.0,
                                    volume = 0.35
                                )
                            )
                        },
                        keyframes = keyframes,
                        normalize_loudness = true,
                        output_name = snapshot.timelineOutputName.trim().ifBlank { "timeline_output" }
                    )
                )
            }.onSuccess { response ->
                val warnings = if (response.warnings.isNotEmpty()) " | warnings=${response.warnings.joinToString()}" else ""
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        message = if (response.success) {
                            "Timeline output: ${response.output_path}$warnings"
                        } else {
                            "Timeline compose failed: ${response.message}"
                        }
                    )
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isWorking = false, message = "Timeline compose failed: ${it.message}") }
            }
        }
    }

    fun transcribeCaptions() {
        val snapshot = _uiState.value
        val sourcePath = snapshot.sourcePath.trim().ifBlank { null }
        val sourceUrl = snapshot.sourceUrl.trim().ifBlank { null }
        if (sourcePath == null && sourceUrl == null) {
            _uiState.update { it.copy(message = "Provide source path or source URL to transcribe captions") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching {
                repository.transcribeCaptions(
                    sourcePath = sourcePath,
                    sourceUrl = sourceUrl,
                    language = snapshot.captionLanguage.trim().ifBlank { null },
                    outputName = snapshot.captionSrtOutputName.trim().ifBlank { null },
                    maxCharsPerLine = 42
                )
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        captionResultText = "${response.engine}: cues=${response.cue_count}, srt=${response.srt_path ?: "n/a"}",
                        message = response.message
                    )
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isWorking = false, message = "Caption transcription failed: ${it.message}") }
            }
        }
    }

    fun buildSampleSrt() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching {
                repository.buildSrt(
                    CaptionSrtBuildRequest(
                        cues = listOf(
                            CaptionCue(start_seconds = 0.0, end_seconds = 2.5, text = "Welcome to Creator's Hub."),
                            CaptionCue(start_seconds = 2.5, end_seconds = 5.0, text = "Edit once, publish everywhere.")
                        ),
                        output_name = _uiState.value.captionSrtOutputName.trim().ifBlank { "captions_manual" }
                    )
                )
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        captionResultText = "manual cues=${response.cue_count}, srt=${response.srt_path ?: "n/a"}",
                        message = response.message
                    )
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isWorking = false, message = "SRT build failed: ${it.message}") }
            }
        }
    }

    fun runMotionTrack() {
        val snapshot = _uiState.value
        val sourcePath = snapshot.sourcePath.trim().ifBlank { null }
        val sourceUrl = snapshot.sourceUrl.trim().ifBlank { null }
        if (sourcePath == null && sourceUrl == null) {
            _uiState.update { it.copy(message = "Provide source path or URL for motion tracking") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching {
                repository.trackVideoMotion(
                    sourcePath = sourcePath,
                    sourceUrl = sourceUrl,
                    startSeconds = 0.0,
                    endSeconds = null,
                    maxCorners = 220,
                    qualityLevel = 0.18,
                    minDistance = 6.0,
                    sampleEveryNFrames = 2,
                    roiX = snapshot.motionRoiX.trim().toIntOrNull(),
                    roiY = snapshot.motionRoiY.trim().toIntOrNull(),
                    roiWidth = snapshot.motionRoiWidth.trim().toIntOrNull(),
                    roiHeight = snapshot.motionRoiHeight.trim().toIntOrNull(),
                    smoothingWindow = snapshot.motionSmoothingWindow.trim().toIntOrNull() ?: 4,
                    minConfidence = snapshot.motionMinConfidence.trim().toDoubleOrNull() ?: 0.05,
                    outputOverlayName = snapshot.motionOverlayOutputName.trim().ifBlank { null }
                )
            }.onSuccess { response ->
                val sample = response.track_points.take(3)
                    .joinToString { "t=${"%.2f".format(it.at_seconds)} (${it.x},${it.y},${it.width}x${it.height})" }
                val overlayPart = response.overlay_path?.let { " | overlay=$it" } ?: ""
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        motionTrackSummary = if (response.success) {
                            "points=${response.track_points.size}, fps=${"%.2f".format(response.fps)}, conf=${"%.2f".format(response.average_confidence)}$overlayPart | $sample"
                        } else {
                            response.message
                        },
                        message = response.message
                    )
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isWorking = false, message = "Motion track failed: ${it.message}") }
            }
        }
    }

    fun runImageHeal() {
        val snapshot = _uiState.value
        val sourcePath = snapshot.sourcePath.trim().ifBlank { null }
        val sourceUrl = snapshot.sourceUrl.trim().ifBlank { null }
        if (sourcePath == null && sourceUrl == null) {
            _uiState.update { it.copy(message = "Provide source image path or URL for heal") }
            return
        }
        val eraseRegions = parseHealRegions(snapshot.healRegionsCsv)
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching {
                repository.healImage(
                    sourcePath = sourcePath,
                    sourceUrl = sourceUrl,
                    outputName = snapshot.healOutputName.trim().ifBlank { "healed_image" },
                    maskPath = snapshot.healMaskPath.trim().ifBlank { null },
                    eraseRegions = eraseRegions,
                    inpaintRadius = 3.0,
                    method = "telea",
                    fillStrategy = snapshot.healFillStrategy.trim().ifBlank { "hybrid" },
                    featherRadius = 3,
                    preserveEdges = true,
                    edgeBlend = snapshot.healEdgeBlend.trim().toDoubleOrNull() ?: 0.55,
                    denoiseStrength = snapshot.healDenoiseStrength.trim().toDoubleOrNull() ?: 0.0
                )
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        healSummary = if (response.success) {
                            "Healed image: ${response.output_path ?: "n/a"}"
                        } else {
                            response.message
                        },
                        message = response.message
                    )
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isWorking = false, message = "Image heal failed: ${it.message}") }
            }
        }
    }

    private fun buildLiveDestinations(snapshot: MediaStudioUiState): List<LiveDestinationConfig> {
        fun destinationOrNull(
            platform: String,
            ingestUrl: String,
            streamKey: String
        ): LiveDestinationConfig? {
            val ingest = ingestUrl.trim()
            if (ingest.isBlank()) {
                return null
            }
            val key = streamKey.trim().ifBlank { null }
            return LiveDestinationConfig(
                platform = platform,
                label = platform.uppercase(),
                ingest_url = ingest,
                stream_key = key,
                enabled = true
            )
        }

        return listOfNotNull(
            destinationOrNull("x", snapshot.liveXIngestUrl, snapshot.liveXStreamKey),
            destinationOrNull("linkedin", snapshot.liveLinkedinIngestUrl, snapshot.liveLinkedinStreamKey),
            destinationOrNull("instagram", snapshot.liveInstagramIngestUrl, snapshot.liveInstagramStreamKey)
        )
    }

    fun refreshLiveSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching { repository.listLiveMulticastSessions() }
                .onSuccess { response ->
                    val current = _uiState.value.liveCurrentSessionId
                    val active = response.sessions.firstOrNull { it.session_id == current } ?: response.sessions.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            liveSessions = response.sessions,
                            liveCurrentSessionId = active?.session_id.orEmpty(),
                            liveStatusSummary = active?.let { session ->
                                "${session.session_name}: ${session.status} | active=${session.active_destinations} failed=${session.failed_destinations}"
                            }.orEmpty(),
                            message = "Live sessions refreshed"
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            isWorking = false,
                            message = "Live session refresh failed: ${it.message}"
                        )
                    }
                }
        }
    }

    fun startLiveMulticast() {
        val snapshot = _uiState.value
        val sourcePath = snapshot.liveSourcePath.trim().ifBlank { snapshot.sourcePath.trim().ifBlank { null } }
        val sourceUrl = snapshot.liveSourceUrl.trim().ifBlank { snapshot.sourceUrl.trim().ifBlank { null } }
        if (sourcePath == null && sourceUrl == null) {
            _uiState.update { it.copy(message = "Provide a live source path or URL") }
            return
        }
        val destinations = buildLiveDestinations(snapshot)
        if (destinations.isEmpty()) {
            _uiState.update { it.copy(message = "Provide at least one ingest URL (X/LinkedIn/Instagram)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching {
                repository.startLiveMulticast(
                    LiveMulticastStartRequest(
                        session_name = snapshot.liveSessionName.trim().ifBlank { null },
                        source_path = sourcePath,
                        source_url = sourceUrl,
                        dry_run = snapshot.liveDryRun,
                        copy_video = snapshot.liveCopyVideo,
                        video_bitrate = snapshot.liveVideoBitrate.trim().ifBlank { "4500k" },
                        audio_bitrate = snapshot.liveAudioBitrate.trim().ifBlank { "160k" },
                        fps = snapshot.liveFps.trim().toIntOrNull() ?: 30,
                        gop_seconds = snapshot.liveGopSeconds.trim().toDoubleOrNull() ?: 2.0,
                        destinations = destinations
                    )
                )
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        liveCurrentSessionId = response.session_id,
                        liveStatusSummary = "${response.session_name}: ${response.status} | active=${response.active_destinations} failed=${response.failed_destinations}",
                        message = response.message
                    )
                }
                refreshLiveSessions()
            }.onFailure {
                _uiState.update { state ->
                    state.copy(isWorking = false, message = "Live multicast start failed: ${it.message}")
                }
            }
        }
    }

    fun stopLiveMulticast(sessionId: String? = null) {
        val id = sessionId?.trim().orEmpty().ifBlank { _uiState.value.liveCurrentSessionId.trim() }
        if (id.isBlank()) {
            _uiState.update { it.copy(message = "Select or start a live session first") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching { repository.stopLiveMulticastSession(id) }
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            liveStatusSummary = "${response.session_id}: ${response.status}",
                            message = response.message
                        )
                    }
                    refreshLiveSessions()
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(isWorking = false, message = "Live multicast stop failed: ${it.message}")
                    }
                }
        }
    }

    fun loadAssets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching { repository.getAssets(kind = _uiState.value.assetKind.trim().ifBlank { null }) }
                .onSuccess { response ->
                    val summary = if (response.items.isEmpty()) {
                        "No assets for current filter"
                    } else {
                        response.items.take(4).joinToString { "${it.title} (${it.license_name})" }
                    }
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            assetSummary = "${response.total_returned} assets: $summary",
                            message = "Assets loaded"
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state -> state.copy(isWorking = false, message = "Asset load failed: ${it.message}") }
                }
        }
    }

    fun saveAsset() {
        val snapshot = _uiState.value
        val kind = snapshot.assetKind.trim()
        val title = snapshot.assetTitle.trim()
        val source = snapshot.assetSource.trim()
        val license = snapshot.assetLicense.trim()
        val urlOrPath = snapshot.assetUrlOrPath.trim()
        if (kind.isBlank() || title.isBlank() || source.isBlank() || license.isBlank()) {
            _uiState.update { it.copy(message = "Kind/title/source/license are required for assets") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = "") }
            runCatching {
                repository.addAsset(
                    AssetUpsertRequest(
                        kind = kind,
                        title = title,
                        source = source,
                        license_name = license,
                        remote_url = if (urlOrPath.startsWith("http")) urlOrPath else null,
                        local_path = if (urlOrPath.startsWith("http")) null else urlOrPath.ifBlank { null }
                    )
                )
            }.onSuccess { response ->
                _uiState.update { state -> state.copy(isWorking = false, message = "Saved asset '${response.asset.title}'") }
                loadAssets()
            }.onFailure {
                _uiState.update { state -> state.copy(isWorking = false, message = "Asset save failed: ${it.message}") }
            }
        }
    }
}
