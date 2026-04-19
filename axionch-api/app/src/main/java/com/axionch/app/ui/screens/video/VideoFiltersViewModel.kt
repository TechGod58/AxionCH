package com.axionch.app.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axionch.app.data.repo.AxionRepository
import com.axionch.shared.api.ImageFilterPreset
import com.axionch.shared.api.VideoFilterPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    val videoSepia: Boolean = false,
    val videoBlackWhite: Boolean = false,
    val videoSaturationFactor: String = "1.0",
    val videoBrightnessDelta: String = "0.0",
    val videoOutputPath: String = "",
    val videoStatusMessage: String = "",
    val imagePresets: List<ImageFilterPreset> = emptyList(),
    val selectedImagePresetId: String = "",
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
    val imageOutputPath: String = "",
    val imageStatusMessage: String = "",
    val isWorking: Boolean = false,
)

class VideoFiltersViewModel : ViewModel() {
    private val repository = AxionRepository()

    private val _uiState = MutableStateFlow(VideoFiltersUiState())
    val uiState: StateFlow<VideoFiltersUiState> = _uiState

    fun updateVideoSourcePath(value: String) {
        _uiState.value = _uiState.value.copy(videoSourcePath = value)
    }

    fun updateVideoSourceUrl(value: String) {
        _uiState.value = _uiState.value.copy(videoSourceUrl = value)
    }

    fun updateVideoOutputName(value: String) {
        _uiState.value = _uiState.value.copy(videoOutputName = value)
    }

    fun updateVideoOverlayPath(value: String) {
        _uiState.value = _uiState.value.copy(videoOverlayPath = value)
    }

    fun updateVideoOverlayUrl(value: String) {
        _uiState.value = _uiState.value.copy(videoOverlayUrl = value)
    }

    fun updateVideoTextOverlay(value: String) {
        _uiState.value = _uiState.value.copy(videoTextOverlay = value)
    }

    fun updateVideoSoundPath(value: String) {
        _uiState.value = _uiState.value.copy(videoSoundPath = value)
    }

    fun updateVideoSoundUrl(value: String) {
        _uiState.value = _uiState.value.copy(videoSoundUrl = value)
    }

    fun updateVideoSoundVolume(value: String) {
        _uiState.value = _uiState.value.copy(videoSoundVolume = value)
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
        _uiState.value = _uiState.value.copy(imageSourcePath = value)
    }

    fun updateImageSourceUrl(value: String) {
        _uiState.value = _uiState.value.copy(imageSourceUrl = value)
    }

    fun updateImageOutputName(value: String) {
        _uiState.value = _uiState.value.copy(imageOutputName = value)
    }

    fun updateImageOverlayPath(value: String) {
        _uiState.value = _uiState.value.copy(imageOverlayPath = value)
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

    fun loadPresets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                videoStatusMessage = "",
                imageStatusMessage = ""
            )

            val videoResult = runCatching { repository.getVideoFilters() }
            val imageResult = runCatching { repository.getImageFilters() }

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
                }
            )
        }
    }

    fun applySelectedVideoFilter() {
        val state = _uiState.value
        if (state.selectedVideoPresetId.isBlank()) {
            _uiState.value = state.copy(videoStatusMessage = "Select a video preset first.")
            return
        }
        if (state.videoSourcePath.isBlank() && state.videoSourceUrl.isBlank()) {
            _uiState.value = state.copy(videoStatusMessage = "Provide video source path or source URL.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, videoStatusMessage = "")
            runCatching {
                repository.applyVideoFilter(
                    sourcePath = state.videoSourcePath.trim().ifBlank { null },
                    sourceUrl = state.videoSourceUrl.trim().ifBlank { null },
                    presetId = state.selectedVideoPresetId,
                    outputName = state.videoOutputName.trim().ifBlank { null },
                    sepia = state.videoSepia,
                    blackWhite = state.videoBlackWhite,
                    saturationFactor = state.videoSaturationFactor.toDoubleOrNull() ?: 1.0,
                    brightnessDelta = state.videoBrightnessDelta.toDoubleOrNull() ?: 0.0,
                    overlayPath = state.videoOverlayPath.trim().ifBlank { null },
                    overlayUrl = state.videoOverlayUrl.trim().ifBlank { null },
                    textOverlay = state.videoTextOverlay.trim().ifBlank { null },
                    soundPath = state.videoSoundPath.trim().ifBlank { null },
                    soundUrl = state.videoSoundUrl.trim().ifBlank { null },
                    soundVolume = state.videoSoundVolume.toDoubleOrNull() ?: 1.0
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    ffmpegAvailable = response.ffmpeg_available,
                    videoOutputPath = response.output_path.orEmpty(),
                    videoStatusMessage = response.message
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    videoStatusMessage = "Video filter apply failed: ${it.message}"
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
                    textOverlay = state.imageTextOverlay.trim().ifBlank { null }
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
}
