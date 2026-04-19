package com.axionch.app.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axionch.app.data.repo.AxionRepository
import com.axionch.shared.api.AssetItem
import com.axionch.shared.api.CaptionCue
import com.axionch.shared.api.CaptionSrtBuildRequest
import com.axionch.shared.api.CreatorTemplate
import com.axionch.shared.api.ExportPreset
import com.axionch.shared.api.ImageEraseRegion
import com.axionch.shared.api.ImageFilterPreset
import com.axionch.shared.api.ImageLayer
import com.axionch.shared.api.TimelineAudioTrack
import com.axionch.shared.api.TimelineClip
import com.axionch.shared.api.TimelineKeyframe
import com.axionch.shared.api.VideoFilterPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TimelineClipDraft(
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

data class TimelineAudioTrackDraft(
    val sourcePath: String = "",
    val sourceUrl: String = "",
    val startSeconds: String = "0.0",
    val endSeconds: String = "",
    val volume: String = "1.0"
)

data class TimelineKeyframeDraft(
    val atSeconds: String = "",
    val property: String = "",
    val value: String = ""
)

data class CaptionCueDraft(
    val startSeconds: String = "",
    val endSeconds: String = "",
    val text: String = ""
)

data class ImageEraseRegionDraft(
    val x: String = "",
    val y: String = "",
    val width: String = "",
    val height: String = ""
)

data class ImageLayerDraft(
    val layerPath: String = "",
    val layerUrl: String = "",
    val maskPath: String = "",
    val maskUrl: String = "",
    val opacity: String = "1.0",
    val blendMode: String = "normal",
    val x: String = "0",
    val y: String = "0"
)

data class VideoFiltersUiState(
    val ffmpegAvailable: Boolean = false,
    val ffmpegBinary: String = "",
    val videoPresets: List<VideoFilterPreset> = emptyList(),
    val selectedVideoPresetId: String = "",
    val videoSourcePath: String = "",
    val videoSourceUrl: String = "",
    val videoOutputName: String = "",
    val videoOverlayPath: String = "",
    val videoOverlayUrl: String = "",
    val videoTextOverlay: String = "",
    val videoSoundPath: String = "",
    val videoSoundUrl: String = "",
    val videoSoundVolume: String = "1.0",
    val videoAudioDenoise: Boolean = false,
    val videoAudioEqLowGain: String = "0.0",
    val videoAudioEqMidGain: String = "0.0",
    val videoAudioEqHighGain: String = "0.0",
    val videoAudioCompressor: Boolean = false,
    val videoAudioGate: Boolean = false,
    val videoAudioDucking: Boolean = false,
    val videoLoudnessNormalization: Boolean = false,
    val videoAudioLimiter: Boolean = false,
    val videoSpeedFactor: String = "1.0",
    val videoStabilize: Boolean = false,
    val videoCurvesPreset: String = "",
    val videoLut3dPath: String = "",
    val videoLut3dUrl: String = "",
    val videoMotionStartSeconds: String = "0.0",
    val videoMotionEndSeconds: String = "",
    val videoMotionRoiX: String = "",
    val videoMotionRoiY: String = "",
    val videoMotionRoiWidth: String = "",
    val videoMotionRoiHeight: String = "",
    val videoMotionSmoothingWindow: String = "4",
    val videoMotionMinConfidence: String = "0.05",
    val videoMotionOverlayOutputName: String = "",
    val videoMotionSummary: String = "",
    val videoSepia: Boolean = false,
    val videoBlackWhite: Boolean = false,
    val videoSaturationFactor: String = "1.0",
    val videoBrightnessDelta: String = "0.0",
    val videoOutputPath: String = "",
    val videoStatusMessage: String = "",
    val videoLocalMediaMessage: String = "",
    val videoExportUri: String = "",
    val imagePresets: List<ImageFilterPreset> = emptyList(),
    val selectedImagePresetId: String = "",
    val creatorTemplates: List<CreatorTemplate> = emptyList(),
    val templatePlatformFilter: String = "",
    val templateMediaTypeFilter: String = "",
    val selectedCreatorTemplateId: String = "",
    val activeCreatorTemplateText: String = "",
    val templateStatusMessage: String = "",
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
    val exportStatusMessage: String = "",
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
    val assetStatusMessage: String = "",
    val imageSourcePath: String = "",
    val imageSourceUrl: String = "",
    val imageOutputName: String = "",
    val imageOverlayPath: String = "",
    val imageOverlayUrl: String = "",
    val imageTextOverlay: String = "",
    val imageSepia: Boolean = false,
    val imageBlackWhite: Boolean = false,
    val imageCartoonify: Boolean = false,
    val imageCaricature: Boolean = false,
    val imageSaturationFactor: String = "1.0",
    val imageBrightnessFactor: String = "1.0",
    val imageContrastFactor: String = "1.0",
    val imageRawEnhance: Boolean = false,
    val imageSelectiveX: String = "",
    val imageSelectiveY: String = "",
    val imageSelectiveWidth: String = "",
    val imageSelectiveHeight: String = "",
    val imageSelectiveBrightnessFactor: String = "1.0",
    val imageSelectiveContrastFactor: String = "1.0",
    val imageSelectiveSaturationFactor: String = "1.0",
    val imageBackgroundRemove: Boolean = false,
    val imageBackgroundKeyColor: String = "#00FF00",
    val imageBackgroundKeyTolerance: String = "24",
    val imageEraseRegions: List<ImageEraseRegionDraft> = emptyList(),
    val imageLayers: List<ImageLayerDraft> = emptyList(),
    val imageHealMaskPath: String = "",
    val imageHealMaskUrl: String = "",
    val imageHealOutputName: String = "",
    val imageHealInpaintRadius: String = "3.0",
    val imageHealMethod: String = "telea",
    val imageHealFillStrategy: String = "inpaint",
    val imageHealFeatherRadius: String = "3",
    val imageHealPreserveEdges: Boolean = true,
    val imageHealEdgeBlend: String = "0.55",
    val imageHealDenoiseStrength: String = "0.0",
    val imageHealOutputPath: String = "",
    val imageOutputPath: String = "",
    val imageStatusMessage: String = "",
    val imageLocalMediaMessage: String = "",
    val imageExportUri: String = "",
    val timelineClips: List<TimelineClipDraft> = listOf(TimelineClipDraft()),
    val timelineAudioTracks: List<TimelineAudioTrackDraft> = emptyList(),
    val timelineKeyframes: List<TimelineKeyframeDraft> = emptyList(),
    val timelineTransitionDefault: String = "",
    val timelineNormalizeLoudness: Boolean = false,
    val timelineOutputName: String = "",
    val timelineBackgroundAudioPath: String = "",
    val timelineBackgroundAudioUrl: String = "",
    val timelineBackgroundAudioVolume: String = "0.35",
    val timelineStatusMessage: String = "",
    val timelineOutputPath: String = "",
    val timelineWarnings: List<String> = emptyList(),
    val timelineLocalMediaMessage: String = "",
    val timelineExportUri: String = "",
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
    val captionBurnExportUri: String = "",
    val captionStatusMessage: String = "",
    val captionEngine: String = "",
    val captionCueCount: Int = 0,
    val captionLocalMediaMessage: String = "",
    val isWorking: Boolean = false,
)

class VideoFiltersViewModel : ViewModel() {
    private val repository = AxionRepository()

    private val _uiState = MutableStateFlow(VideoFiltersUiState())
    val uiState: StateFlow<VideoFiltersUiState> = _uiState

    private enum class VideoApplyMode {
        FULL,
        AUDIO_FINISHING,
        PRO_VIDEO
    }

    fun updateVideoSourcePath(value: String) {
        _uiState.value = _uiState.value.copy(videoSourcePath = value, videoLocalMediaMessage = "")
    }

    fun updateVideoSourceUrl(value: String) {
        _uiState.value = _uiState.value.copy(videoSourceUrl = value)
    }

    fun updateVideoOutputName(value: String) {
        _uiState.value = _uiState.value.copy(videoOutputName = value)
    }

    fun updateVideoOverlayPath(value: String) {
        _uiState.value = _uiState.value.copy(videoOverlayPath = value, videoLocalMediaMessage = "")
    }

    fun updateVideoOverlayUrl(value: String) {
        _uiState.value = _uiState.value.copy(videoOverlayUrl = value)
    }

    fun updateVideoTextOverlay(value: String) {
        _uiState.value = _uiState.value.copy(videoTextOverlay = value)
    }

    fun updateVideoSoundPath(value: String) {
        _uiState.value = _uiState.value.copy(videoSoundPath = value, videoLocalMediaMessage = "")
    }

    fun updateVideoSoundUrl(value: String) {
        _uiState.value = _uiState.value.copy(videoSoundUrl = value)
    }

    fun updateVideoSoundVolume(value: String) {
        _uiState.value = _uiState.value.copy(videoSoundVolume = value)
    }

    fun toggleVideoAudioDenoise() {
        _uiState.value = _uiState.value.copy(videoAudioDenoise = !_uiState.value.videoAudioDenoise)
    }

    fun updateVideoAudioEqLowGain(value: String) {
        _uiState.value = _uiState.value.copy(videoAudioEqLowGain = value)
    }

    fun updateVideoAudioEqMidGain(value: String) {
        _uiState.value = _uiState.value.copy(videoAudioEqMidGain = value)
    }

    fun updateVideoAudioEqHighGain(value: String) {
        _uiState.value = _uiState.value.copy(videoAudioEqHighGain = value)
    }

    fun toggleVideoAudioCompressor() {
        _uiState.value = _uiState.value.copy(videoAudioCompressor = !_uiState.value.videoAudioCompressor)
    }

    fun toggleVideoAudioGate() {
        _uiState.value = _uiState.value.copy(videoAudioGate = !_uiState.value.videoAudioGate)
    }

    fun toggleVideoAudioDucking() {
        _uiState.value = _uiState.value.copy(videoAudioDucking = !_uiState.value.videoAudioDucking)
    }

    fun toggleVideoLoudnessNormalization() {
        _uiState.value = _uiState.value.copy(videoLoudnessNormalization = !_uiState.value.videoLoudnessNormalization)
    }

    fun toggleVideoAudioLimiter() {
        _uiState.value = _uiState.value.copy(videoAudioLimiter = !_uiState.value.videoAudioLimiter)
    }

    fun updateVideoSpeedFactor(value: String) {
        _uiState.value = _uiState.value.copy(videoSpeedFactor = value)
    }

    fun toggleVideoStabilize() {
        _uiState.value = _uiState.value.copy(videoStabilize = !_uiState.value.videoStabilize)
    }

    fun updateVideoCurvesPreset(value: String) {
        _uiState.value = _uiState.value.copy(videoCurvesPreset = value)
    }

    fun updateVideoLut3dPath(value: String) {
        _uiState.value = _uiState.value.copy(videoLut3dPath = value, videoLut3dUrl = "", videoLocalMediaMessage = "")
    }

    fun updateVideoLut3dUrl(value: String) {
        _uiState.value = _uiState.value.copy(videoLut3dUrl = value)
    }

    fun onVideoLutImported(path: String, label: String) {
        _uiState.value = _uiState.value.copy(
            videoLut3dPath = path,
            videoLut3dUrl = "",
            videoLocalMediaMessage = "Imported LUT file: $label"
        )
    }

    fun updateVideoMotionStartSeconds(value: String) {
        _uiState.value = _uiState.value.copy(videoMotionStartSeconds = value)
    }

    fun updateVideoMotionEndSeconds(value: String) {
        _uiState.value = _uiState.value.copy(videoMotionEndSeconds = value)
    }

    fun updateVideoMotionRoiX(value: String) {
        _uiState.value = _uiState.value.copy(videoMotionRoiX = value)
    }

    fun updateVideoMotionRoiY(value: String) {
        _uiState.value = _uiState.value.copy(videoMotionRoiY = value)
    }

    fun updateVideoMotionRoiWidth(value: String) {
        _uiState.value = _uiState.value.copy(videoMotionRoiWidth = value)
    }

    fun updateVideoMotionRoiHeight(value: String) {
        _uiState.value = _uiState.value.copy(videoMotionRoiHeight = value)
    }

    fun updateVideoMotionSmoothingWindow(value: String) {
        _uiState.value = _uiState.value.copy(videoMotionSmoothingWindow = value)
    }

    fun updateVideoMotionMinConfidence(value: String) {
        _uiState.value = _uiState.value.copy(videoMotionMinConfidence = value)
    }

    fun updateVideoMotionOverlayOutputName(value: String) {
        _uiState.value = _uiState.value.copy(videoMotionOverlayOutputName = value)
    }

    fun toggleVideoSepia() {
        _uiState.value = _uiState.value.copy(videoSepia = !_uiState.value.videoSepia)
    }

    fun toggleVideoBlackWhite() {
        _uiState.value = _uiState.value.copy(videoBlackWhite = !_uiState.value.videoBlackWhite)
    }

    fun updateVideoSaturationFactor(value: String) {
        _uiState.value = _uiState.value.copy(videoSaturationFactor = value)
    }

    fun updateVideoBrightnessDelta(value: String) {
        _uiState.value = _uiState.value.copy(videoBrightnessDelta = value)
    }

    fun selectVideoPreset(presetId: String) {
        _uiState.value = _uiState.value.copy(selectedVideoPresetId = presetId)
    }

    fun updateImageSourcePath(value: String) {
        _uiState.value = _uiState.value.copy(imageSourcePath = value, imageLocalMediaMessage = "")
    }

    fun updateImageSourceUrl(value: String) {
        _uiState.value = _uiState.value.copy(imageSourceUrl = value)
    }

    fun updateImageOutputName(value: String) {
        _uiState.value = _uiState.value.copy(imageOutputName = value)
    }

    fun updateImageOverlayPath(value: String) {
        _uiState.value = _uiState.value.copy(imageOverlayPath = value, imageLocalMediaMessage = "")
    }

    fun onVideoImported(path: String, label: String) {
        _uiState.value = _uiState.value.copy(
            videoSourcePath = path,
            videoSourceUrl = "",
            videoLocalMediaMessage = "Imported local video: $label"
        )
    }

    fun onVideoOverlayImported(path: String, label: String) {
        _uiState.value = _uiState.value.copy(
            videoOverlayPath = path,
            videoOverlayUrl = "",
            videoLocalMediaMessage = "Imported overlay image: $label"
        )
    }

    fun onVideoSoundImported(path: String, label: String) {
        _uiState.value = _uiState.value.copy(
            videoSoundPath = path,
            videoSoundUrl = "",
            videoLocalMediaMessage = "Imported sound track: $label"
        )
    }

    fun onImageImported(path: String, label: String) {
        _uiState.value = _uiState.value.copy(
            imageSourcePath = path,
            imageSourceUrl = "",
            imageLocalMediaMessage = "Imported local image: $label"
        )
    }

    fun onImageOverlayImported(path: String, label: String) {
        _uiState.value = _uiState.value.copy(
            imageOverlayPath = path,
            imageOverlayUrl = "",
            imageLocalMediaMessage = "Imported image overlay: $label"
        )
    }

    fun onVideoLocalMediaError(message: String) {
        _uiState.value = _uiState.value.copy(videoLocalMediaMessage = message)
    }

    fun onImageLocalMediaError(message: String) {
        _uiState.value = _uiState.value.copy(imageLocalMediaMessage = message)
    }

    fun onVideoExported(uri: String) {
        _uiState.value = _uiState.value.copy(
            videoExportUri = uri,
            videoLocalMediaMessage = "Exported video to gallery: $uri"
        )
    }

    fun onImageExported(uri: String) {
        _uiState.value = _uiState.value.copy(
            imageExportUri = uri,
            imageLocalMediaMessage = "Exported image to gallery: $uri"
        )
    }

    fun updateImageOverlayUrl(value: String) {
        _uiState.value = _uiState.value.copy(imageOverlayUrl = value)
    }

    fun updateImageTextOverlay(value: String) {
        _uiState.value = _uiState.value.copy(imageTextOverlay = value)
    }

    fun toggleImageSepia() {
        _uiState.value = _uiState.value.copy(imageSepia = !_uiState.value.imageSepia)
    }

    fun toggleImageBlackWhite() {
        _uiState.value = _uiState.value.copy(imageBlackWhite = !_uiState.value.imageBlackWhite)
    }

    fun toggleImageCartoonify() {
        _uiState.value = _uiState.value.copy(imageCartoonify = !_uiState.value.imageCartoonify)
    }

    fun toggleImageCaricature() {
        _uiState.value = _uiState.value.copy(imageCaricature = !_uiState.value.imageCaricature)
    }

    fun updateImageSaturationFactor(value: String) {
        _uiState.value = _uiState.value.copy(imageSaturationFactor = value)
    }

    fun updateImageBrightnessFactor(value: String) {
        _uiState.value = _uiState.value.copy(imageBrightnessFactor = value)
    }

    fun updateImageContrastFactor(value: String) {
        _uiState.value = _uiState.value.copy(imageContrastFactor = value)
    }

    fun selectImagePreset(presetId: String) {
        _uiState.value = _uiState.value.copy(selectedImagePresetId = presetId)
    }

    fun updateTemplatePlatformFilter(value: String) {
        _uiState.value = _uiState.value.copy(templatePlatformFilter = value)
    }

    fun updateTemplateMediaTypeFilter(value: String) {
        _uiState.value = _uiState.value.copy(templateMediaTypeFilter = value)
    }

    fun clearTemplateFilters() {
        _uiState.value = _uiState.value.copy(
            templatePlatformFilter = "",
            templateMediaTypeFilter = ""
        )
    }

    fun applyTemplateQuickFilter(key: String) {
        when (key.lowercase()) {
            "reels" -> _uiState.value = _uiState.value.copy(
                templatePlatformFilter = "instagram",
                templateMediaTypeFilter = "video"
            )
            "shorts" -> _uiState.value = _uiState.value.copy(
                templatePlatformFilter = "youtube",
                templateMediaTypeFilter = "video"
            )
            "tiktok" -> _uiState.value = _uiState.value.copy(
                templatePlatformFilter = "tiktok",
                templateMediaTypeFilter = "video"
            )
            else -> clearTemplateFilters()
        }
        loadCreatorTemplates()
    }

    fun selectCreatorTemplate(templateId: String) {
        _uiState.value = _uiState.value.copy(selectedCreatorTemplateId = templateId)
    }

    fun loadCreatorTemplates() {
        val state = _uiState.value
        val platform = state.templatePlatformFilter.trim().ifBlank { null }
        val mediaType = state.templateMediaTypeFilter.trim().ifBlank { null }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, templateStatusMessage = "")
            runCatching { repository.getCreatorTemplates(platform = platform, mediaType = mediaType) }
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        creatorTemplates = response.templates,
                        selectedCreatorTemplateId = _uiState.value.selectedCreatorTemplateId.ifBlank {
                            response.templates.firstOrNull()?.id.orEmpty()
                        },
                        templateStatusMessage = "Loaded ${response.total_returned} templates."
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        templateStatusMessage = "Loading templates failed: ${it.message}"
                    )
                }
        }
    }

    fun applySelectedCreatorTemplate() {
        val state = _uiState.value
        val template = state.creatorTemplates.firstOrNull { it.id == state.selectedCreatorTemplateId }
        if (template == null) {
            _uiState.value = state.copy(templateStatusMessage = "Select a creator template first.")
            return
        }

        val resolutionHint = "${template.width}x${template.height}${template.fps?.let { " @${it}fps" } ?: ""}"
        val exportPresetId = matchExportPresetForTemplate(state.exportPresets, template)?.id.orEmpty()
        _uiState.value = when (template.media_type.lowercase()) {
            "video" -> state.copy(
                videoOutputName = template.id,
                timelineOutputName = "${template.id}-timeline",
                captionOutputName = "${template.id}-captions",
                captionBurnOutputName = "${template.id}-burned",
                selectedExportPresetId = if (exportPresetId.isNotBlank()) exportPresetId else state.selectedExportPresetId,
                exportOutputPrefix = template.id,
                activeCreatorTemplateText = "Active template: ${template.name} ($resolutionHint) for ${template.platforms.joinToString()}",
                templateStatusMessage = "Applied ${template.name} to video/timeline defaults."
            )
            else -> state.copy(
                imageOutputName = template.id,
                selectedExportPresetId = if (exportPresetId.isNotBlank()) exportPresetId else state.selectedExportPresetId,
                exportOutputPrefix = template.id,
                activeCreatorTemplateText = "Active template: ${template.name} ($resolutionHint) for ${template.platforms.joinToString()}",
                templateStatusMessage = "Applied ${template.name} to image defaults."
            )
        }
    }

    fun updateExportSourcePathsInput(value: String) {
        _uiState.value = _uiState.value.copy(exportSourcePathsInput = value)
    }

    fun updateExportSourceUrlsInput(value: String) {
        _uiState.value = _uiState.value.copy(exportSourceUrlsInput = value)
    }

    fun updateExportOutputPrefix(value: String) {
        _uiState.value = _uiState.value.copy(exportOutputPrefix = value)
    }

    fun toggleExportUseHardwareAcceleration() {
        _uiState.value = _uiState.value.copy(exportUseHardwareAcceleration = !_uiState.value.exportUseHardwareAcceleration)
    }

    fun selectExportPreset(presetId: String) {
        _uiState.value = _uiState.value.copy(selectedExportPresetId = presetId)
    }

    fun appendExportSourcePath(path: String) {
        val existing = parseBulkList(_uiState.value.exportSourcePathsInput).toMutableList()
        if (path.isNotBlank() && path !in existing) {
            existing += path
        }
        _uiState.value = _uiState.value.copy(exportSourcePathsInput = existing.joinToString("\n"))
    }

    fun queueBatchExport() {
        val state = _uiState.value
        if (state.selectedExportPresetId.isBlank()) {
            _uiState.value = state.copy(exportStatusMessage = "Select an export preset first.")
            return
        }
        val sourcePaths = parseBulkList(state.exportSourcePathsInput)
        val sourceUrls = parseBulkList(state.exportSourceUrlsInput)
        if (sourcePaths.isEmpty() && sourceUrls.isEmpty()) {
            _uiState.value = state.copy(exportStatusMessage = "Provide at least one source path or source URL.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, exportStatusMessage = "Queueing export job...")
            runCatching {
                repository.queueExportJob(
                    sourcePaths = sourcePaths,
                    sourceUrls = sourceUrls,
                    presetId = state.selectedExportPresetId,
                    outputPrefix = state.exportOutputPrefix.trim().ifBlank { null },
                    hardwareAcceleration = state.exportUseHardwareAcceleration
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    exportQueuedJobId = response.job_id,
                    exportLastStatus = response.status,
                    exportQueuedItems = response.queued_items,
                    exportCompletedItems = 0,
                    exportFailedItems = 0,
                    exportItemSummaries = emptyList(),
                    exportStatusMessage = response.message
                )
                refreshExportJobStatus()
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    exportStatusMessage = "Queue export failed: ${it.message}"
                )
            }
        }
    }

    fun refreshExportJobStatus() {
        val jobId = _uiState.value.exportQueuedJobId.trim()
        if (jobId.isBlank()) {
            _uiState.value = _uiState.value.copy(exportStatusMessage = "No export job queued yet.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching { repository.getExportJobStatus(jobId) }
                .onSuccess { response ->
                    val summaries = response.items.map { item ->
                        val outputPart = item.output_path?.let { " -> $it" } ?: ""
                        "${item.index}. ${item.status}: ${item.source}$outputPart${item.message?.let { " (${it})" } ?: ""}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        exportLastStatus = response.status,
                        exportQueuedItems = response.queued_items,
                        exportCompletedItems = response.completed_items,
                        exportFailedItems = response.failed_items,
                        exportItemSummaries = summaries,
                        exportStatusMessage = "Export job ${response.job_id}: ${response.status}"
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        exportStatusMessage = "Export status check failed: ${it.message}"
                    )
                }
        }
    }

    fun updateAssetKindFilter(value: String) {
        _uiState.value = _uiState.value.copy(assetKindFilter = value)
    }

    fun updateAssetTagFilter(value: String) {
        _uiState.value = _uiState.value.copy(assetTagFilter = value)
    }

    fun applyAssetQuickFilter(kind: String?) {
        _uiState.value = _uiState.value.copy(assetKindFilter = kind.orEmpty(), assetTagFilter = "")
        loadAssets()
    }

    fun updateAssetTitleInput(value: String) {
        _uiState.value = _uiState.value.copy(assetTitleInput = value)
    }

    fun updateAssetSourceInput(value: String) {
        _uiState.value = _uiState.value.copy(assetSourceInput = value)
    }

    fun updateAssetLicenseNameInput(value: String) {
        _uiState.value = _uiState.value.copy(assetLicenseNameInput = value)
    }

    fun updateAssetLicenseUrlInput(value: String) {
        _uiState.value = _uiState.value.copy(assetLicenseUrlInput = value)
    }

    fun toggleAssetAttributionRequired() {
        _uiState.value = _uiState.value.copy(assetAttributionRequired = !_uiState.value.assetAttributionRequired)
    }

    fun updateAssetAttributionTextInput(value: String) {
        _uiState.value = _uiState.value.copy(assetAttributionTextInput = value)
    }

    fun updateAssetTagsInput(value: String) {
        _uiState.value = _uiState.value.copy(assetTagsInput = value)
    }

    fun updateAssetLocalPathInput(value: String) {
        _uiState.value = _uiState.value.copy(assetLocalPathInput = value)
    }

    fun updateAssetRemoteUrlInput(value: String) {
        _uiState.value = _uiState.value.copy(assetRemoteUrlInput = value)
    }

    fun loadAssets() {
        val snapshot = _uiState.value
        val kind = snapshot.assetKindFilter.trim().ifBlank { null }
        val tag = snapshot.assetTagFilter.trim().ifBlank { null }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, assetStatusMessage = "Loading asset library...")
            runCatching { repository.getAssets(kind = kind, tag = tag) }
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        assets = response.items,
                        assetStatusMessage = "Loaded ${response.total_returned} assets."
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        assetStatusMessage = "Loading assets failed: ${it.message}"
                    )
                }
        }
    }

    fun saveAsset() {
        val state = _uiState.value
        val kind = state.assetKindFilter.trim().lowercase()
        if (kind.isBlank()) {
            _uiState.value = state.copy(assetStatusMessage = "Asset kind is required (audio/overlay).")
            return
        }
        if (state.assetTitleInput.trim().isBlank()) {
            _uiState.value = state.copy(assetStatusMessage = "Asset title is required.")
            return
        }
        if (state.assetSourceInput.trim().isBlank()) {
            _uiState.value = state.copy(assetStatusMessage = "Asset source is required.")
            return
        }
        if (state.assetLicenseNameInput.trim().isBlank()) {
            _uiState.value = state.copy(assetStatusMessage = "License name is required.")
            return
        }
        val localPath = state.assetLocalPathInput.trim().ifBlank { null }
        val remoteUrl = state.assetRemoteUrlInput.trim().ifBlank { null }
        if (localPath == null && remoteUrl == null) {
            _uiState.value = state.copy(assetStatusMessage = "Provide local path or remote URL for the asset.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, assetStatusMessage = "Saving asset...")
            runCatching {
                repository.addAsset(
                    kind = kind,
                    title = state.assetTitleInput.trim(),
                    source = state.assetSourceInput.trim(),
                    licenseName = state.assetLicenseNameInput.trim(),
                    licenseUrl = state.assetLicenseUrlInput.trim().ifBlank { null },
                    attributionRequired = state.assetAttributionRequired,
                    attributionText = state.assetAttributionTextInput.trim().ifBlank { null },
                    tags = parseBulkList(state.assetTagsInput),
                    localPath = localPath,
                    remoteUrl = remoteUrl
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
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
                    assetStatusMessage = "Saved asset '${response.asset.title}' (${response.asset.kind})."
                )
                loadAssets()
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    assetStatusMessage = "Saving asset failed: ${it.message}"
                )
            }
        }
    }

    fun deleteAsset(assetId: String) {
        if (assetId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, assetStatusMessage = "Deleting asset...")
            runCatching { repository.deleteAsset(assetId) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        assetStatusMessage = "Deleted asset $assetId."
                    )
                    loadAssets()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        assetStatusMessage = "Deleting asset failed: ${it.message}"
                    )
                }
        }
    }

    fun applyAssetToVideoOverlay(assetId: String) {
        val state = _uiState.value
        val asset = state.assets.firstOrNull { it.id == assetId } ?: run {
            _uiState.value = state.copy(assetStatusMessage = "Asset not found.")
            return
        }
        val path = asset.local_path?.trim().orEmpty().ifBlank { null }
        val url = asset.remote_url?.trim().orEmpty().ifBlank { null }
        if (path == null && url == null) {
            _uiState.value = state.copy(assetStatusMessage = "Asset '${asset.title}' has no local path or remote URL.")
            return
        }
        _uiState.value = state.copy(
            videoOverlayPath = path.orEmpty(),
            videoOverlayUrl = if (path != null) "" else url.orEmpty(),
            videoTextOverlay = mergeAttributionText(state.videoTextOverlay, asset),
            assetStatusMessage = "Applied '${asset.title}' to video overlay."
        )
    }

    fun applyAssetToImageOverlay(assetId: String) {
        val state = _uiState.value
        val asset = state.assets.firstOrNull { it.id == assetId } ?: run {
            _uiState.value = state.copy(assetStatusMessage = "Asset not found.")
            return
        }
        val path = asset.local_path?.trim().orEmpty().ifBlank { null }
        val url = asset.remote_url?.trim().orEmpty().ifBlank { null }
        if (path == null && url == null) {
            _uiState.value = state.copy(assetStatusMessage = "Asset '${asset.title}' has no local path or remote URL.")
            return
        }
        _uiState.value = state.copy(
            imageOverlayPath = path.orEmpty(),
            imageOverlayUrl = if (path != null) "" else url.orEmpty(),
            imageTextOverlay = mergeAttributionText(state.imageTextOverlay, asset),
            assetStatusMessage = "Applied '${asset.title}' to image overlay."
        )
    }

    fun applyAssetToVideoAudio(assetId: String) {
        val state = _uiState.value
        val asset = state.assets.firstOrNull { it.id == assetId } ?: run {
            _uiState.value = state.copy(assetStatusMessage = "Asset not found.")
            return
        }
        val path = asset.local_path?.trim().orEmpty().ifBlank { null }
        val url = asset.remote_url?.trim().orEmpty().ifBlank { null }
        if (path == null && url == null) {
            _uiState.value = state.copy(assetStatusMessage = "Asset '${asset.title}' has no local path or remote URL.")
            return
        }
        val attributionNote = if (asset.attribution_required && !asset.attribution_text.isNullOrBlank()) {
            " Attribution required: ${asset.attribution_text}"
        } else {
            ""
        }
        _uiState.value = state.copy(
            videoSoundPath = path.orEmpty(),
            videoSoundUrl = if (path != null) "" else url.orEmpty(),
            assetStatusMessage = "Applied '${asset.title}' to video audio.$attributionNote"
        )
    }

    fun applyAssetToTimelineBackgroundAudio(assetId: String) {
        val state = _uiState.value
        val asset = state.assets.firstOrNull { it.id == assetId } ?: run {
            _uiState.value = state.copy(assetStatusMessage = "Asset not found.")
            return
        }
        val path = asset.local_path?.trim().orEmpty().ifBlank { null }
        val url = asset.remote_url?.trim().orEmpty().ifBlank { null }
        if (path == null && url == null) {
            _uiState.value = state.copy(assetStatusMessage = "Asset '${asset.title}' has no local path or remote URL.")
            return
        }
        val attributionNote = if (asset.attribution_required && !asset.attribution_text.isNullOrBlank()) {
            " Attribution required: ${asset.attribution_text}"
        } else {
            ""
        }
        _uiState.value = state.copy(
            timelineBackgroundAudioPath = path.orEmpty(),
            timelineBackgroundAudioUrl = if (path != null) "" else url.orEmpty(),
            assetStatusMessage = "Applied '${asset.title}' to timeline background audio.$attributionNote"
        )
    }

    fun toggleImageRawEnhance() {
        _uiState.value = _uiState.value.copy(imageRawEnhance = !_uiState.value.imageRawEnhance)
    }

    fun updateImageSelectiveX(value: String) {
        _uiState.value = _uiState.value.copy(imageSelectiveX = value)
    }

    fun updateImageSelectiveY(value: String) {
        _uiState.value = _uiState.value.copy(imageSelectiveY = value)
    }

    fun updateImageSelectiveWidth(value: String) {
        _uiState.value = _uiState.value.copy(imageSelectiveWidth = value)
    }

    fun updateImageSelectiveHeight(value: String) {
        _uiState.value = _uiState.value.copy(imageSelectiveHeight = value)
    }

    fun updateImageSelectiveBrightnessFactor(value: String) {
        _uiState.value = _uiState.value.copy(imageSelectiveBrightnessFactor = value)
    }

    fun updateImageSelectiveContrastFactor(value: String) {
        _uiState.value = _uiState.value.copy(imageSelectiveContrastFactor = value)
    }

    fun updateImageSelectiveSaturationFactor(value: String) {
        _uiState.value = _uiState.value.copy(imageSelectiveSaturationFactor = value)
    }

    fun toggleImageBackgroundRemove() {
        _uiState.value = _uiState.value.copy(imageBackgroundRemove = !_uiState.value.imageBackgroundRemove)
    }

    fun updateImageBackgroundKeyColor(value: String) {
        _uiState.value = _uiState.value.copy(imageBackgroundKeyColor = value)
    }

    fun updateImageBackgroundKeyTolerance(value: String) {
        _uiState.value = _uiState.value.copy(imageBackgroundKeyTolerance = value)
    }

    fun addImageEraseRegion() {
        _uiState.value = _uiState.value.copy(imageEraseRegions = _uiState.value.imageEraseRegions + ImageEraseRegionDraft())
    }

    fun removeImageEraseRegion(index: Int) {
        val current = _uiState.value.imageEraseRegions
        if (index !in current.indices) return
        _uiState.value = _uiState.value.copy(imageEraseRegions = current.filterIndexed { i, _ -> i != index })
    }

    fun updateImageEraseRegionX(index: Int, value: String) {
        updateImageEraseRegion(index) { it.copy(x = value) }
    }

    fun updateImageEraseRegionY(index: Int, value: String) {
        updateImageEraseRegion(index) { it.copy(y = value) }
    }

    fun updateImageEraseRegionWidth(index: Int, value: String) {
        updateImageEraseRegion(index) { it.copy(width = value) }
    }

    fun updateImageEraseRegionHeight(index: Int, value: String) {
        updateImageEraseRegion(index) { it.copy(height = value) }
    }

    fun addImageLayer() {
        _uiState.value = _uiState.value.copy(imageLayers = _uiState.value.imageLayers + ImageLayerDraft())
    }

    fun removeImageLayer(index: Int) {
        val current = _uiState.value.imageLayers
        if (index !in current.indices) return
        _uiState.value = _uiState.value.copy(imageLayers = current.filterIndexed { i, _ -> i != index })
    }

    fun updateImageLayerPath(index: Int, value: String) {
        updateImageLayer(index) { it.copy(layerPath = value, layerUrl = "") }
    }

    fun updateImageLayerUrl(index: Int, value: String) {
        updateImageLayer(index) { it.copy(layerUrl = value) }
    }

    fun updateImageLayerMaskPath(index: Int, value: String) {
        updateImageLayer(index) { it.copy(maskPath = value, maskUrl = "") }
    }

    fun updateImageLayerMaskUrl(index: Int, value: String) {
        updateImageLayer(index) { it.copy(maskUrl = value) }
    }

    fun updateImageLayerOpacity(index: Int, value: String) {
        updateImageLayer(index) { it.copy(opacity = value) }
    }

    fun updateImageLayerBlendMode(index: Int, value: String) {
        updateImageLayer(index) { it.copy(blendMode = value) }
    }

    fun updateImageLayerX(index: Int, value: String) {
        updateImageLayer(index) { it.copy(x = value) }
    }

    fun updateImageLayerY(index: Int, value: String) {
        updateImageLayer(index) { it.copy(y = value) }
    }

    fun updateImageHealMaskPath(value: String) {
        _uiState.value = _uiState.value.copy(imageHealMaskPath = value, imageHealMaskUrl = "")
    }

    fun updateImageHealMaskUrl(value: String) {
        _uiState.value = _uiState.value.copy(imageHealMaskUrl = value)
    }

    fun updateImageHealOutputName(value: String) {
        _uiState.value = _uiState.value.copy(imageHealOutputName = value)
    }

    fun updateImageHealInpaintRadius(value: String) {
        _uiState.value = _uiState.value.copy(imageHealInpaintRadius = value)
    }

    fun updateImageHealMethod(value: String) {
        _uiState.value = _uiState.value.copy(imageHealMethod = value)
    }

    fun updateImageHealFillStrategy(value: String) {
        _uiState.value = _uiState.value.copy(imageHealFillStrategy = value)
    }

    fun updateImageHealFeatherRadius(value: String) {
        _uiState.value = _uiState.value.copy(imageHealFeatherRadius = value)
    }

    fun toggleImageHealPreserveEdges() {
        _uiState.value = _uiState.value.copy(imageHealPreserveEdges = !_uiState.value.imageHealPreserveEdges)
    }

    fun updateImageHealEdgeBlend(value: String) {
        _uiState.value = _uiState.value.copy(imageHealEdgeBlend = value)
    }

    fun updateImageHealDenoiseStrength(value: String) {
        _uiState.value = _uiState.value.copy(imageHealDenoiseStrength = value)
    }

    fun addTimelineClip() {
        _uiState.value = _uiState.value.copy(
            timelineClips = _uiState.value.timelineClips + TimelineClipDraft(),
            timelineLocalMediaMessage = ""
        )
    }

    fun removeTimelineClip(index: Int) {
        val current = _uiState.value.timelineClips
        if (index !in current.indices) return
        val updated = current.filterIndexed { i, _ -> i != index }.ifEmpty { listOf(TimelineClipDraft()) }
        _uiState.value = _uiState.value.copy(timelineClips = updated)
    }

    fun updateTimelineClipSourcePath(index: Int, value: String) {
        updateTimelineClip(index) { it.copy(sourcePath = value, sourceUrl = "", splitPointsCsv = it.splitPointsCsv) }
    }

    fun updateTimelineClipSourceUrl(index: Int, value: String) {
        updateTimelineClip(index) { it.copy(sourceUrl = value) }
    }

    fun updateTimelineClipStartSeconds(index: Int, value: String) {
        updateTimelineClip(index) { it.copy(startSeconds = value) }
    }

    fun updateTimelineClipEndSeconds(index: Int, value: String) {
        updateTimelineClip(index) { it.copy(endSeconds = value) }
    }

    fun updateTimelineClipSpeedFactor(index: Int, value: String) {
        updateTimelineClip(index) { it.copy(speedFactor = value) }
    }

    fun updateTimelineClipVolume(index: Int, value: String) {
        updateTimelineClip(index) { it.copy(volume = value) }
    }

    fun updateTimelineClipSplitPoints(index: Int, value: String) {
        updateTimelineClip(index) { it.copy(splitPointsCsv = value) }
    }

    fun updateTimelineClipTransition(index: Int, value: String) {
        updateTimelineClip(index) { it.copy(transitionToNext = value) }
    }

    fun updateTimelineClipTransitionDuration(index: Int, value: String) {
        updateTimelineClip(index) { it.copy(transitionDuration = value) }
    }

    fun onTimelineClipImported(index: Int, path: String, label: String) {
        updateTimelineClip(index) { it.copy(sourcePath = path, sourceUrl = "") }
        _uiState.value = _uiState.value.copy(timelineLocalMediaMessage = "Imported clip ${index + 1}: $label")
    }

    fun addTimelineAudioTrack() {
        _uiState.value = _uiState.value.copy(timelineAudioTracks = _uiState.value.timelineAudioTracks + TimelineAudioTrackDraft())
    }

    fun removeTimelineAudioTrack(index: Int) {
        val current = _uiState.value.timelineAudioTracks
        if (index !in current.indices) return
        _uiState.value = _uiState.value.copy(timelineAudioTracks = current.filterIndexed { i, _ -> i != index })
    }

    fun updateTimelineAudioTrackSourcePath(index: Int, value: String) {
        updateTimelineAudioTrack(index) { it.copy(sourcePath = value, sourceUrl = "") }
    }

    fun updateTimelineAudioTrackSourceUrl(index: Int, value: String) {
        updateTimelineAudioTrack(index) { it.copy(sourceUrl = value) }
    }

    fun updateTimelineAudioTrackStartSeconds(index: Int, value: String) {
        updateTimelineAudioTrack(index) { it.copy(startSeconds = value) }
    }

    fun updateTimelineAudioTrackEndSeconds(index: Int, value: String) {
        updateTimelineAudioTrack(index) { it.copy(endSeconds = value) }
    }

    fun updateTimelineAudioTrackVolume(index: Int, value: String) {
        updateTimelineAudioTrack(index) { it.copy(volume = value) }
    }

    fun onTimelineAudioTrackImported(index: Int, path: String, label: String) {
        updateTimelineAudioTrack(index) { it.copy(sourcePath = path, sourceUrl = "") }
        _uiState.value = _uiState.value.copy(timelineLocalMediaMessage = "Imported audio track ${index + 1}: $label")
    }

    fun updateTimelineTransitionDefault(value: String) {
        _uiState.value = _uiState.value.copy(timelineTransitionDefault = value)
    }

    fun toggleTimelineNormalizeLoudness() {
        _uiState.value = _uiState.value.copy(timelineNormalizeLoudness = !_uiState.value.timelineNormalizeLoudness)
    }

    fun updateTimelineOutputName(value: String) {
        _uiState.value = _uiState.value.copy(timelineOutputName = value)
    }

    fun updateTimelineBackgroundAudioPath(value: String) {
        _uiState.value = _uiState.value.copy(timelineBackgroundAudioPath = value, timelineBackgroundAudioUrl = "")
    }

    fun updateTimelineBackgroundAudioUrl(value: String) {
        _uiState.value = _uiState.value.copy(timelineBackgroundAudioUrl = value)
    }

    fun updateTimelineBackgroundAudioVolume(value: String) {
        _uiState.value = _uiState.value.copy(timelineBackgroundAudioVolume = value)
    }

    fun onTimelineBackgroundAudioImported(path: String, label: String) {
        _uiState.value = _uiState.value.copy(
            timelineBackgroundAudioPath = path,
            timelineBackgroundAudioUrl = "",
            timelineLocalMediaMessage = "Imported background audio: $label"
        )
    }

    fun addTimelineKeyframe() {
        _uiState.value = _uiState.value.copy(timelineKeyframes = _uiState.value.timelineKeyframes + TimelineKeyframeDraft())
    }

    fun removeTimelineKeyframe(index: Int) {
        val current = _uiState.value.timelineKeyframes
        if (index !in current.indices) return
        _uiState.value = _uiState.value.copy(timelineKeyframes = current.filterIndexed { i, _ -> i != index })
    }

    fun updateTimelineKeyframeAt(index: Int, value: String) {
        updateTimelineKeyframe(index) { it.copy(atSeconds = value) }
    }

    fun updateTimelineKeyframeProperty(index: Int, value: String) {
        updateTimelineKeyframe(index) { it.copy(property = value) }
    }

    fun updateTimelineKeyframeValue(index: Int, value: String) {
        updateTimelineKeyframe(index) { it.copy(value = value) }
    }

    fun onTimelineLocalMediaError(message: String) {
        _uiState.value = _uiState.value.copy(timelineLocalMediaMessage = message)
    }

    fun onTimelineExported(uri: String) {
        _uiState.value = _uiState.value.copy(
            timelineExportUri = uri,
            timelineLocalMediaMessage = "Exported timeline video to gallery: $uri"
        )
    }

    fun updateCaptionSourcePath(value: String) {
        _uiState.value = _uiState.value.copy(captionSourcePath = value, captionLocalMediaMessage = "")
    }

    fun updateCaptionSourceUrl(value: String) {
        _uiState.value = _uiState.value.copy(captionSourceUrl = value)
    }

    fun updateCaptionLanguage(value: String) {
        _uiState.value = _uiState.value.copy(captionLanguage = value)
    }

    fun updateCaptionOutputName(value: String) {
        _uiState.value = _uiState.value.copy(captionOutputName = value)
    }

    fun updateCaptionSubtitlePath(value: String) {
        _uiState.value = _uiState.value.copy(captionSubtitlePath = value, captionSubtitleUrl = "", captionLocalMediaMessage = "")
    }

    fun updateCaptionSubtitleUrl(value: String) {
        _uiState.value = _uiState.value.copy(captionSubtitleUrl = value)
    }

    fun updateCaptionBurnOutputName(value: String) {
        _uiState.value = _uiState.value.copy(captionBurnOutputName = value)
    }

    fun addCaptionCue() {
        _uiState.value = _uiState.value.copy(captionCues = _uiState.value.captionCues + CaptionCueDraft())
    }

    fun removeCaptionCue(index: Int) {
        val cues = _uiState.value.captionCues
        if (index !in cues.indices) return
        _uiState.value = _uiState.value.copy(
            captionCues = cues.filterIndexed { i, _ -> i != index }.ifEmpty { listOf(CaptionCueDraft()) }
        )
    }

    fun updateCaptionCueStart(index: Int, value: String) {
        updateCaptionCue(index) { it.copy(startSeconds = value) }
    }

    fun updateCaptionCueEnd(index: Int, value: String) {
        updateCaptionCue(index) { it.copy(endSeconds = value) }
    }

    fun updateCaptionCueText(index: Int, value: String) {
        updateCaptionCue(index) { it.copy(text = value) }
    }

    fun onCaptionSourceImported(path: String, label: String) {
        _uiState.value = _uiState.value.copy(
            captionSourcePath = path,
            captionSourceUrl = "",
            captionLocalMediaMessage = "Imported caption source video: $label"
        )
    }

    fun onCaptionSubtitleImported(path: String, label: String) {
        _uiState.value = _uiState.value.copy(
            captionSubtitlePath = path,
            captionSubtitleUrl = "",
            captionLocalMediaMessage = "Imported subtitle file: $label"
        )
    }

    fun onCaptionLocalMediaError(message: String) {
        _uiState.value = _uiState.value.copy(captionLocalMediaMessage = message)
    }

    fun onCaptionBurnExported(uri: String) {
        _uiState.value = _uiState.value.copy(
            captionBurnExportUri = uri,
            captionLocalMediaMessage = "Exported burned-subtitle video to gallery: $uri"
        )
    }

    fun loadPresets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                videoStatusMessage = "",
                imageStatusMessage = "",
                videoLocalMediaMessage = "",
                imageLocalMediaMessage = ""
            )

            val videoResult = runCatching { repository.getVideoFilters() }
            val imageResult = runCatching { repository.getImageFilters() }
            val templateResult = runCatching { repository.getCreatorTemplates() }
            val exportResult = runCatching { repository.getExportPresets() }
            val assetResult = runCatching { repository.getAssets() }

            _uiState.value = _uiState.value.copy(
                isWorking = false,
                ffmpegAvailable = videoResult.getOrNull()?.ffmpeg_available ?: _uiState.value.ffmpegAvailable,
                ffmpegBinary = videoResult.getOrNull()?.ffmpeg_binary.orEmpty().ifBlank { _uiState.value.ffmpegBinary },
                videoPresets = videoResult.getOrNull()?.presets ?: _uiState.value.videoPresets,
                selectedVideoPresetId = _uiState.value.selectedVideoPresetId.ifBlank {
                    videoResult.getOrNull()?.presets?.firstOrNull()?.id.orEmpty()
                },
                imagePresets = imageResult.getOrNull()?.presets ?: _uiState.value.imagePresets,
                selectedImagePresetId = _uiState.value.selectedImagePresetId.ifBlank {
                    imageResult.getOrNull()?.presets?.firstOrNull()?.id.orEmpty()
                },
                creatorTemplates = templateResult.getOrNull()?.templates ?: _uiState.value.creatorTemplates,
                selectedCreatorTemplateId = _uiState.value.selectedCreatorTemplateId.ifBlank {
                    templateResult.getOrNull()?.templates?.firstOrNull()?.id.orEmpty()
                },
                exportPresets = exportResult.getOrNull()?.presets ?: _uiState.value.exportPresets,
                selectedExportPresetId = _uiState.value.selectedExportPresetId.ifBlank {
                    exportResult.getOrNull()?.presets?.firstOrNull()?.id.orEmpty()
                },
                assets = assetResult.getOrNull()?.items ?: _uiState.value.assets,
                videoStatusMessage = when {
                    videoResult.isSuccess && (videoResult.getOrNull()?.ffmpeg_available == true) ->
                        "Loaded ${videoResult.getOrNull()?.presets?.size ?: 0} video presets."
                    videoResult.isSuccess ->
                        "FFmpeg not found (${videoResult.getOrNull()?.ffmpeg_binary}). Install it to process video."
                    else -> "Loading video presets failed: ${videoResult.exceptionOrNull()?.message}"
                },
                imageStatusMessage = when {
                    imageResult.isSuccess ->
                        "Loaded ${imageResult.getOrNull()?.presets?.size ?: 0} image presets."
                    else -> "Loading image presets failed: ${imageResult.exceptionOrNull()?.message}"
                },
                templateStatusMessage = when {
                    templateResult.isSuccess ->
                        "Loaded ${templateResult.getOrNull()?.total_returned ?: 0} creator templates."
                    else -> "Loading creator templates failed: ${templateResult.exceptionOrNull()?.message}"
                },
                exportStatusMessage = when {
                    exportResult.isSuccess ->
                        "Loaded ${exportResult.getOrNull()?.total_returned ?: 0} export presets."
                    else -> "Loading export presets failed: ${exportResult.exceptionOrNull()?.message}"
                },
                assetStatusMessage = when {
                    assetResult.isSuccess ->
                        "Loaded ${assetResult.getOrNull()?.total_returned ?: 0} assets."
                    else -> "Loading assets failed: ${assetResult.exceptionOrNull()?.message}"
                }
            )
        }
    }

    fun applySelectedVideoFilter() {
        applyVideoFilterByMode(VideoApplyMode.FULL)
    }

    fun applyVideoAudioFinishing() {
        applyVideoFilterByMode(VideoApplyMode.AUDIO_FINISHING)
    }

    fun applyVideoProControls() {
        applyVideoFilterByMode(VideoApplyMode.PRO_VIDEO)
    }

    private fun applyVideoFilterByMode(mode: VideoApplyMode) {
        val state = _uiState.value
        if (state.selectedVideoPresetId.isBlank()) {
            _uiState.value = state.copy(videoStatusMessage = "Select a video preset first.")
            return
        }
        if (state.videoSourcePath.isBlank() && state.videoSourceUrl.isBlank()) {
            _uiState.value = state.copy(videoStatusMessage = "Provide video source path or source URL.")
            return
        }

        val outputName = when (mode) {
            VideoApplyMode.FULL -> state.videoOutputName.trim().ifBlank { null }
            VideoApplyMode.AUDIO_FINISHING -> state.videoOutputName.trim().ifBlank { "audio_finished" }
            VideoApplyMode.PRO_VIDEO -> state.videoOutputName.trim().ifBlank { "pro_video" }
        }
        val includeColorFilters = mode == VideoApplyMode.FULL
        val includeOverlay = mode != VideoApplyMode.AUDIO_FINISHING
        val includeProVideoControls = mode != VideoApplyMode.AUDIO_FINISHING
        val modeLabel = when (mode) {
            VideoApplyMode.FULL -> "video filter"
            VideoApplyMode.AUDIO_FINISHING -> "audio finishing"
            VideoApplyMode.PRO_VIDEO -> "pro video controls"
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, videoStatusMessage = "Applying $modeLabel...")
            runCatching {
                repository.applyVideoFilter(
                    sourcePath = state.videoSourcePath.trim().ifBlank { null },
                    sourceUrl = state.videoSourceUrl.trim().ifBlank { null },
                    presetId = state.selectedVideoPresetId,
                    outputName = outputName,
                    sepia = if (includeColorFilters) state.videoSepia else false,
                    blackWhite = if (includeColorFilters) state.videoBlackWhite else false,
                    saturationFactor = if (includeColorFilters) state.videoSaturationFactor.toDoubleOrNull() ?: 1.0 else 1.0,
                    brightnessDelta = if (includeColorFilters) state.videoBrightnessDelta.toDoubleOrNull() ?: 0.0 else 0.0,
                    overlayPath = if (includeOverlay) state.videoOverlayPath.trim().ifBlank { null } else null,
                    overlayUrl = if (includeOverlay) state.videoOverlayUrl.trim().ifBlank { null } else null,
                    textOverlay = if (includeOverlay) state.videoTextOverlay.trim().ifBlank { null } else null,
                    speedFactor = if (includeProVideoControls) parseSpeedFactor(state.videoSpeedFactor) else 1.0,
                    stabilize = if (includeProVideoControls) state.videoStabilize else false,
                    curvesPreset = if (includeProVideoControls) state.videoCurvesPreset.trim().ifBlank { null } else null,
                    lut3dPath = if (includeProVideoControls) state.videoLut3dPath.trim().ifBlank { null } else null,
                    lut3dUrl = if (includeProVideoControls) state.videoLut3dUrl.trim().ifBlank { null } else null,
                    soundPath = state.videoSoundPath.trim().ifBlank { null },
                    soundUrl = state.videoSoundUrl.trim().ifBlank { null },
                    soundVolume = state.videoSoundVolume.toDoubleOrNull() ?: 1.0,
                    audioDenoise = state.videoAudioDenoise,
                    audioEqLowGain = parseAudioEqGain(state.videoAudioEqLowGain),
                    audioEqMidGain = parseAudioEqGain(state.videoAudioEqMidGain),
                    audioEqHighGain = parseAudioEqGain(state.videoAudioEqHighGain),
                    audioCompressor = state.videoAudioCompressor,
                    audioGate = state.videoAudioGate,
                    audioDucking = state.videoAudioDucking,
                    loudnessNormalization = state.videoLoudnessNormalization,
                    audioLimiter = state.videoAudioLimiter
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    ffmpegAvailable = response.ffmpeg_available,
                    videoOutputPath = response.output_path.orEmpty(),
                    videoStatusMessage = "$modeLabel complete. ${response.message}"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    videoStatusMessage = "$modeLabel failed: ${it.message}"
                )
            }
        }
    }

    fun applySelectedImageFilter() {
        val state = _uiState.value
        if (state.selectedImagePresetId.isBlank()) {
            _uiState.value = state.copy(imageStatusMessage = "Select an image preset first.")
            return
        }
        if (state.imageSourcePath.isBlank() && state.imageSourceUrl.isBlank()) {
            _uiState.value = state.copy(imageStatusMessage = "Provide image source path or source URL.")
            return
        }
        val eraseRegions = parseImageEraseRegions(state.imageEraseRegions)
        if (eraseRegions == null) {
            _uiState.value = state.copy(imageStatusMessage = "Erase regions must use valid integer x/y/width/height.")
            return
        }
        val layers = parseImageLayers(state.imageLayers)
        if (layers == null) {
            _uiState.value = state.copy(imageStatusMessage = "Each layer must include opacity (0..1) and integer x/y coordinates.")
            return
        }
        val backgroundTolerance = state.imageBackgroundKeyTolerance.trim().toIntOrNull()?.coerceIn(0, 255) ?: 24

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, imageStatusMessage = "")
            runCatching {
                repository.applyImageFilter(
                    sourcePath = state.imageSourcePath.trim().ifBlank { null },
                    sourceUrl = state.imageSourceUrl.trim().ifBlank { null },
                    presetId = state.selectedImagePresetId,
                    outputName = state.imageOutputName.trim().ifBlank { null },
                    sepia = state.imageSepia,
                    blackWhite = state.imageBlackWhite,
                    cartoonify = state.imageCartoonify,
                    caricature = state.imageCaricature,
                    saturationFactor = state.imageSaturationFactor.toDoubleOrNull() ?: 1.0,
                    brightnessFactor = state.imageBrightnessFactor.toDoubleOrNull() ?: 1.0,
                    contrastFactor = state.imageContrastFactor.toDoubleOrNull() ?: 1.0,
                    overlayPath = state.imageOverlayPath.trim().ifBlank { null },
                    overlayUrl = state.imageOverlayUrl.trim().ifBlank { null },
                    textOverlay = state.imageTextOverlay.trim().ifBlank { null },
                    rawEnhance = state.imageRawEnhance,
                    selectiveX = state.imageSelectiveX.trim().toIntOrNull(),
                    selectiveY = state.imageSelectiveY.trim().toIntOrNull(),
                    selectiveWidth = state.imageSelectiveWidth.trim().toIntOrNull(),
                    selectiveHeight = state.imageSelectiveHeight.trim().toIntOrNull(),
                    selectiveBrightnessFactor = state.imageSelectiveBrightnessFactor.toDoubleOrNull() ?: 1.0,
                    selectiveContrastFactor = state.imageSelectiveContrastFactor.toDoubleOrNull() ?: 1.0,
                    selectiveSaturationFactor = state.imageSelectiveSaturationFactor.toDoubleOrNull() ?: 1.0,
                    eraseRegions = eraseRegions,
                    layers = layers,
                    backgroundKeyColor = if (state.imageBackgroundRemove) state.imageBackgroundKeyColor.trim().ifBlank { "#00FF00" } else null,
                    backgroundKeyTolerance = backgroundTolerance
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    imageOutputPath = response.output_path.orEmpty(),
                    imageStatusMessage = response.message
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    imageStatusMessage = "Image filter apply failed: ${it.message}"
                )
            }
        }
    }

    fun healSelectedImage() {
        val state = _uiState.value
        if (state.imageSourcePath.isBlank() && state.imageSourceUrl.isBlank()) {
            _uiState.value = state.copy(imageStatusMessage = "Provide image source path or source URL before heal/remove.")
            return
        }
        val eraseRegions = parseImageEraseRegions(state.imageEraseRegions)
        if (eraseRegions == null) {
            _uiState.value = state.copy(imageStatusMessage = "Erase regions must use valid integer x/y/width/height.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, imageStatusMessage = "Running object heal/removal...")
            runCatching {
                repository.healImage(
                    sourcePath = state.imageSourcePath.trim().ifBlank { null },
                    sourceUrl = state.imageSourceUrl.trim().ifBlank { null },
                    outputName = state.imageHealOutputName.trim().ifBlank { null },
                    maskPath = state.imageHealMaskPath.trim().ifBlank { null },
                    maskUrl = state.imageHealMaskUrl.trim().ifBlank { null },
                    eraseRegions = eraseRegions,
                    inpaintRadius = state.imageHealInpaintRadius.trim().toDoubleOrNull() ?: 3.0,
                    method = state.imageHealMethod.trim().ifBlank { "telea" },
                    fillStrategy = state.imageHealFillStrategy.trim().ifBlank { "inpaint" },
                    featherRadius = state.imageHealFeatherRadius.trim().toIntOrNull() ?: 3,
                    preserveEdges = state.imageHealPreserveEdges,
                    edgeBlend = state.imageHealEdgeBlend.trim().toDoubleOrNull() ?: 0.55,
                    denoiseStrength = state.imageHealDenoiseStrength.trim().toDoubleOrNull() ?: 0.0
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    imageHealOutputPath = response.output_path.orEmpty(),
                    imageOutputPath = response.output_path.orEmpty().ifBlank { _uiState.value.imageOutputPath },
                    imageStatusMessage = response.message
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    imageStatusMessage = "Image heal/removal failed: ${it.message}"
                )
            }
        }
    }

    fun composeTimeline() {
        val state = _uiState.value
        val clips = mutableListOf<TimelineClip>()
        for ((index, draft) in state.timelineClips.withIndex()) {
            val sourcePath = draft.sourcePath.trim().ifBlank { null }
            val sourceUrl = draft.sourceUrl.trim().ifBlank { null }
            if (sourcePath == null && sourceUrl == null) {
                _uiState.value = state.copy(timelineStatusMessage = "Clip ${index + 1}: provide source path or source URL.")
                return
            }
            val startSeconds = draft.startSeconds.toDoubleOrNull()
            if (startSeconds == null) {
                _uiState.value = state.copy(timelineStatusMessage = "Clip ${index + 1}: invalid start seconds.")
                return
            }
            val endSeconds = draft.endSeconds.trim().ifBlank { "" }.let { token ->
                if (token.isBlank()) null else token.toDoubleOrNull()
            }
            if (draft.endSeconds.isNotBlank() && endSeconds == null) {
                _uiState.value = state.copy(timelineStatusMessage = "Clip ${index + 1}: invalid end seconds.")
                return
            }
            val speedFactor = draft.speedFactor.toDoubleOrNull()
            if (speedFactor == null || speedFactor <= 0.0) {
                _uiState.value = state.copy(timelineStatusMessage = "Clip ${index + 1}: speed factor must be > 0.")
                return
            }
            val volume = draft.volume.toDoubleOrNull()
            if (volume == null || volume < 0.0) {
                _uiState.value = state.copy(timelineStatusMessage = "Clip ${index + 1}: volume must be >= 0.")
                return
            }
            val transitionDuration = draft.transitionDuration.toDoubleOrNull()
            if (transitionDuration == null || transitionDuration <= 0.0) {
                _uiState.value = state.copy(timelineStatusMessage = "Clip ${index + 1}: transition duration must be > 0.")
                return
            }
            val splitPoints = parseSplitPoints(draft.splitPointsCsv)
            if (splitPoints == null) {
                _uiState.value = state.copy(timelineStatusMessage = "Clip ${index + 1}: split points must be comma-separated numbers.")
                return
            }
            clips += TimelineClip(
                source_path = sourcePath,
                source_url = sourceUrl,
                start_seconds = startSeconds,
                end_seconds = endSeconds,
                speed_factor = speedFactor,
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
            if (sourcePath == null && sourceUrl == null) {
                continue
            }
            val startSeconds = draft.startSeconds.toDoubleOrNull()
            if (startSeconds == null) {
                _uiState.value = state.copy(timelineStatusMessage = "Audio track ${index + 1}: invalid start seconds.")
                return
            }
            val endSeconds = draft.endSeconds.trim().ifBlank { "" }.let { token ->
                if (token.isBlank()) null else token.toDoubleOrNull()
            }
            if (draft.endSeconds.isNotBlank() && endSeconds == null) {
                _uiState.value = state.copy(timelineStatusMessage = "Audio track ${index + 1}: invalid end seconds.")
                return
            }
            val volume = draft.volume.toDoubleOrNull()
            if (volume == null || volume < 0.0) {
                _uiState.value = state.copy(timelineStatusMessage = "Audio track ${index + 1}: volume must be >= 0.")
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
            if (property.isBlank() && value.isBlank() && atToken.isBlank()) {
                continue
            }
            val atSeconds = atToken.toDoubleOrNull()
            if (atSeconds == null) {
                _uiState.value = state.copy(timelineStatusMessage = "Keyframe ${index + 1}: invalid at-seconds value.")
                return
            }
            if (property.isBlank()) {
                _uiState.value = state.copy(timelineStatusMessage = "Keyframe ${index + 1}: property is required.")
                return
            }
            if (value.isBlank()) {
                _uiState.value = state.copy(timelineStatusMessage = "Keyframe ${index + 1}: value is required.")
                return
            }
            keyframes += TimelineKeyframe(
                at_seconds = atSeconds,
                property = property,
                value = value
            )
        }

        val backgroundAudioVolume = state.timelineBackgroundAudioVolume.toDoubleOrNull() ?: 0.35
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                timelineStatusMessage = "Composing timeline...",
                timelineWarnings = emptyList()
            )
            runCatching {
                repository.composeTimeline(
                    clips = clips,
                    audioTracks = audioTracks,
                    keyframes = keyframes,
                    transitionDefault = state.timelineTransitionDefault.trim().ifBlank { null },
                    normalizeLoudness = state.timelineNormalizeLoudness,
                    outputName = state.timelineOutputName.trim().ifBlank { null },
                    backgroundAudioPath = state.timelineBackgroundAudioPath.trim().ifBlank { null },
                    backgroundAudioUrl = state.timelineBackgroundAudioUrl.trim().ifBlank { null },
                    backgroundAudioVolume = backgroundAudioVolume
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    timelineStatusMessage = response.message,
                    timelineOutputPath = response.output_path.orEmpty(),
                    timelineWarnings = response.warnings
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    timelineStatusMessage = "Timeline compose failed: ${it.message}"
                )
            }
        }
    }

    fun transcribeCaptions() {
        val state = _uiState.value
        val sourcePath = state.captionSourcePath.trim().ifBlank { state.videoSourcePath.trim().ifBlank { null } }
        val sourceUrl = state.captionSourceUrl.trim().ifBlank { state.videoSourceUrl.trim().ifBlank { null } }
        if (sourcePath == null && sourceUrl == null) {
            _uiState.value = state.copy(
                captionStatusMessage = "Provide caption source path/URL, or set video source path/URL.",
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                captionStatusMessage = "Transcribing captions..."
            )
            runCatching {
                repository.transcribeCaptions(
                    sourcePath = sourcePath,
                    sourceUrl = sourceUrl,
                    language = state.captionLanguage.trim().ifBlank { null },
                    outputName = state.captionOutputName.trim().ifBlank { null },
                    maxCharsPerLine = 42
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    captionStatusMessage = response.message,
                    captionEngine = response.engine,
                    captionCueCount = response.cue_count,
                    captionGeneratedSrtPath = response.srt_path.orEmpty(),
                    captionSubtitlePath = response.srt_path.orEmpty().ifBlank { _uiState.value.captionSubtitlePath }
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    captionStatusMessage = "Caption transcription failed: ${it.message}"
                )
            }
        }
    }

    fun buildSrtFromCues() {
        val state = _uiState.value
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
                _uiState.value = state.copy(captionStatusMessage = "Cue ${index + 1}: invalid start seconds.")
                return
            }
            val endSeconds = endToken.toDoubleOrNull()
            if (endSeconds == null || endSeconds <= startSeconds) {
                _uiState.value = state.copy(captionStatusMessage = "Cue ${index + 1}: end seconds must be greater than start.")
                return
            }
            if (text.isBlank()) {
                _uiState.value = state.copy(captionStatusMessage = "Cue ${index + 1}: text is required.")
                return
            }
            cues += CaptionCue(
                start_seconds = startSeconds,
                end_seconds = endSeconds,
                text = text
            )
        }
        if (cues.isEmpty()) {
            _uiState.value = state.copy(captionStatusMessage = "Add at least one subtitle cue before building SRT.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                captionStatusMessage = "Building SRT from edited cues..."
            )
            runCatching {
                repository.buildSrt(
                    CaptionSrtBuildRequest(
                        cues = cues,
                        output_name = state.captionOutputName.trim().ifBlank { "captions_manual" }
                    )
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    captionStatusMessage = response.message,
                    captionCueCount = response.cue_count,
                    captionGeneratedSrtPath = response.srt_path.orEmpty(),
                    captionSubtitlePath = response.srt_path.orEmpty().ifBlank { _uiState.value.captionSubtitlePath }
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    captionStatusMessage = "SRT build failed: ${it.message}"
                )
            }
        }
    }

    fun burnSubtitles() {
        val state = _uiState.value
        val presetId = state.selectedVideoPresetId.ifBlank { state.videoPresets.firstOrNull()?.id.orEmpty() }
        if (presetId.isBlank()) {
            _uiState.value = state.copy(captionStatusMessage = "Load and select a video preset before burn-in.")
            return
        }

        val sourcePath = state.captionSourcePath.trim().ifBlank { state.videoSourcePath.trim().ifBlank { null } }
        val sourceUrl = state.captionSourceUrl.trim().ifBlank { state.videoSourceUrl.trim().ifBlank { null } }
        if (sourcePath == null && sourceUrl == null) {
            _uiState.value = state.copy(captionStatusMessage = "Provide video source path/URL for subtitle burn-in.")
            return
        }

        val subtitlePath = state.captionSubtitlePath.trim().ifBlank { state.captionGeneratedSrtPath.trim().ifBlank { null } }
        val subtitleUrl = state.captionSubtitleUrl.trim().ifBlank { null }
        if (subtitlePath == null && subtitleUrl == null) {
            _uiState.value = state.copy(captionStatusMessage = "Provide subtitle path/URL or build/transcribe an SRT first.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                captionStatusMessage = "Burning subtitles into video..."
            )
            runCatching {
                repository.applyVideoFilter(
                    sourcePath = sourcePath,
                    sourceUrl = sourceUrl,
                    presetId = presetId,
                    outputName = state.captionBurnOutputName.trim().ifBlank { "caption_burned" },
                    sepia = state.videoSepia,
                    blackWhite = state.videoBlackWhite,
                    saturationFactor = state.videoSaturationFactor.toDoubleOrNull() ?: 1.0,
                    brightnessDelta = state.videoBrightnessDelta.toDoubleOrNull() ?: 0.0,
                    overlayPath = state.videoOverlayPath.trim().ifBlank { null },
                    overlayUrl = state.videoOverlayUrl.trim().ifBlank { null },
                    textOverlay = state.videoTextOverlay.trim().ifBlank { null },
                    subtitlePath = subtitlePath,
                    subtitleUrl = subtitleUrl,
                    burnSubtitles = true,
                    speedFactor = parseSpeedFactor(state.videoSpeedFactor),
                    stabilize = state.videoStabilize,
                    curvesPreset = state.videoCurvesPreset.trim().ifBlank { null },
                    lut3dPath = state.videoLut3dPath.trim().ifBlank { null },
                    lut3dUrl = state.videoLut3dUrl.trim().ifBlank { null },
                    soundPath = state.videoSoundPath.trim().ifBlank { null },
                    soundUrl = state.videoSoundUrl.trim().ifBlank { null },
                    soundVolume = state.videoSoundVolume.toDoubleOrNull() ?: 1.0,
                    audioDenoise = state.videoAudioDenoise,
                    audioEqLowGain = parseAudioEqGain(state.videoAudioEqLowGain),
                    audioEqMidGain = parseAudioEqGain(state.videoAudioEqMidGain),
                    audioEqHighGain = parseAudioEqGain(state.videoAudioEqHighGain),
                    audioCompressor = state.videoAudioCompressor,
                    audioGate = state.videoAudioGate,
                    audioDucking = state.videoAudioDucking,
                    loudnessNormalization = state.videoLoudnessNormalization,
                    audioLimiter = state.videoAudioLimiter
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    captionStatusMessage = response.message,
                    captionBurnOutputPath = response.output_path.orEmpty(),
                    videoOutputPath = response.output_path.orEmpty()
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    captionStatusMessage = "Subtitle burn-in failed: ${it.message}"
                )
            }
        }
    }

    fun trackVideoMotion() {
        val state = _uiState.value
        val sourcePath = state.videoSourcePath.trim().ifBlank { null }
        val sourceUrl = state.videoSourceUrl.trim().ifBlank { null }
        if (sourcePath == null && sourceUrl == null) {
            _uiState.value = state.copy(videoStatusMessage = "Provide video source path or URL before motion tracking.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                videoStatusMessage = "Running motion tracking..."
            )
            runCatching {
                repository.trackVideoMotion(
                    sourcePath = sourcePath,
                    sourceUrl = sourceUrl,
                    startSeconds = state.videoMotionStartSeconds.trim().toDoubleOrNull() ?: 0.0,
                    endSeconds = state.videoMotionEndSeconds.trim().ifBlank { "" }.toDoubleOrNull(),
                    maxCorners = 220,
                    qualityLevel = 0.18,
                    minDistance = 6.0,
                    sampleEveryNFrames = 2,
                    roiX = state.videoMotionRoiX.trim().toIntOrNull(),
                    roiY = state.videoMotionRoiY.trim().toIntOrNull(),
                    roiWidth = state.videoMotionRoiWidth.trim().toIntOrNull(),
                    roiHeight = state.videoMotionRoiHeight.trim().toIntOrNull(),
                    smoothingWindow = state.videoMotionSmoothingWindow.trim().toIntOrNull() ?: 4,
                    minConfidence = state.videoMotionMinConfidence.trim().toDoubleOrNull() ?: 0.05,
                    outputOverlayName = state.videoMotionOverlayOutputName.trim().ifBlank { null }
                )
            }.onSuccess { response ->
                val sample = response.track_points.take(3).joinToString {
                    "t=${"%.2f".format(it.at_seconds)} (${it.x},${it.y},${it.width}x${it.height})"
                }
                val overlayPart = response.overlay_path?.let { " | overlay=$it" } ?: ""
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    videoStatusMessage = response.message,
                    videoMotionSummary = if (response.success) {
                        "points=${response.track_points.size}, fps=${"%.2f".format(response.fps)}, conf=${"%.2f".format(response.average_confidence)}$overlayPart${if (sample.isBlank()) "" else " | $sample"}"
                    } else {
                        response.message
                    }
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    videoStatusMessage = "Motion tracking failed: ${it.message}"
                )
            }
        }
    }

    private inline fun updateImageEraseRegion(index: Int, transform: (ImageEraseRegionDraft) -> ImageEraseRegionDraft) {
        val current = _uiState.value.imageEraseRegions
        if (index !in current.indices) return
        _uiState.value = _uiState.value.copy(
            imageEraseRegions = current.mapIndexed { i, region -> if (i == index) transform(region) else region }
        )
    }

    private inline fun updateImageLayer(index: Int, transform: (ImageLayerDraft) -> ImageLayerDraft) {
        val current = _uiState.value.imageLayers
        if (index !in current.indices) return
        _uiState.value = _uiState.value.copy(
            imageLayers = current.mapIndexed { i, layer -> if (i == index) transform(layer) else layer }
        )
    }

    private inline fun updateTimelineClip(index: Int, transform: (TimelineClipDraft) -> TimelineClipDraft) {
        val current = _uiState.value.timelineClips
        if (index !in current.indices) return
        _uiState.value = _uiState.value.copy(
            timelineClips = current.mapIndexed { i, clip -> if (i == index) transform(clip) else clip }
        )
    }

    private inline fun updateTimelineAudioTrack(index: Int, transform: (TimelineAudioTrackDraft) -> TimelineAudioTrackDraft) {
        val current = _uiState.value.timelineAudioTracks
        if (index !in current.indices) return
        _uiState.value = _uiState.value.copy(
            timelineAudioTracks = current.mapIndexed { i, track -> if (i == index) transform(track) else track }
        )
    }

    private inline fun updateTimelineKeyframe(index: Int, transform: (TimelineKeyframeDraft) -> TimelineKeyframeDraft) {
        val current = _uiState.value.timelineKeyframes
        if (index !in current.indices) return
        _uiState.value = _uiState.value.copy(
            timelineKeyframes = current.mapIndexed { i, keyframe -> if (i == index) transform(keyframe) else keyframe }
        )
    }

    private inline fun updateCaptionCue(index: Int, transform: (CaptionCueDraft) -> CaptionCueDraft) {
        val current = _uiState.value.captionCues
        if (index !in current.indices) return
        _uiState.value = _uiState.value.copy(
            captionCues = current.mapIndexed { i, cue -> if (i == index) transform(cue) else cue }
        )
    }

    private fun mergeAttributionText(existing: String, asset: AssetItem): String {
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

    private fun parseBulkList(input: String): List<String> =
        input
            .split('\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    private fun matchExportPresetForTemplate(
        presets: List<ExportPreset>,
        template: CreatorTemplate
    ): ExportPreset? {
        val platformSet = template.platforms.map { it.lowercase() }.toSet()
        return presets.firstOrNull { preset ->
            preset.media_type.equals(template.media_type, ignoreCase = true) &&
                preset.width == template.width &&
                preset.height == template.height &&
                platformSet.contains(preset.platform.lowercase())
        } ?: presets.firstOrNull { preset ->
            preset.media_type.equals(template.media_type, ignoreCase = true) &&
                platformSet.contains(preset.platform.lowercase())
        }
    }

    private fun parseSplitPoints(input: String): List<Double>? {
        val cleaned = input.trim()
        if (cleaned.isBlank()) return emptyList()
        val values = mutableListOf<Double>()
        for (token in cleaned.split(",")) {
            val parsed = token.trim().toDoubleOrNull() ?: return null
            values += parsed
        }
        return values
    }

    private fun parseImageEraseRegions(drafts: List<ImageEraseRegionDraft>): List<ImageEraseRegion>? {
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

    private fun parseImageLayers(drafts: List<ImageLayerDraft>): List<ImageLayer>? {
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

    private fun parseAudioEqGain(input: String): Double {
        val parsed = input.trim().toDoubleOrNull() ?: 0.0
        return parsed.coerceIn(-24.0, 24.0)
    }

    private fun parseSpeedFactor(input: String): Double {
        val parsed = input.trim().toDoubleOrNull() ?: 1.0
        return parsed.coerceIn(0.10, 4.0)
    }
}
