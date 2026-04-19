package com.axionch.app.data.repo

import com.axionch.app.data.api.ApiModule
import com.axionch.shared.api.AccountResponse
import com.axionch.shared.api.ConfigCheckResponse
import com.axionch.shared.api.ConfigSecurityResponse
import com.axionch.shared.api.ConfigStatusResponse
import com.axionch.shared.api.CreatorTemplateListResponse
import com.axionch.shared.api.CreateApiKeyRequest
import com.axionch.shared.api.CreateAccountRequest
import com.axionch.shared.api.CreatePostRequest
import com.axionch.shared.api.CaptionSrtBuildRequest
import com.axionch.shared.api.CaptionSrtBuildResponse
import com.axionch.shared.api.CaptionTranscribeRequest
import com.axionch.shared.api.CaptionTranscribeResponse
import com.axionch.shared.api.DeadLetterListResponse
import com.axionch.shared.api.DryRunHistoryClearResponse
import com.axionch.shared.api.DryRunHistoryResponse
import com.axionch.shared.api.HealthResponse
import com.axionch.shared.api.PublishMetricsResponse
import com.axionch.shared.api.OAuthCallbackResponse
import com.axionch.shared.api.OAuthStartResponse
import com.axionch.shared.api.PublishJobStatusResponse
import com.axionch.shared.api.QueuedPostResponse
import com.axionch.shared.api.ApiKeyListResponse
import com.axionch.shared.api.ApiKeyResponse
import com.axionch.shared.api.ApiKeyRevokeResponse
import com.axionch.shared.api.RequeueDeadLetterResponse
import com.axionch.shared.api.PostResponse
import com.axionch.shared.api.VaultDeleteResponse
import com.axionch.shared.api.VaultEntryCreateRequest
import com.axionch.shared.api.VaultEntryResponse
import com.axionch.shared.api.VaultEntryUpdateRequest
import com.axionch.shared.api.VaultListResponse
import com.axionch.shared.api.VideoFilterApplyRequest
import com.axionch.shared.api.VideoFilterApplyResponse
import com.axionch.shared.api.VideoFilterListResponse
import com.axionch.shared.api.ImageFilterApplyRequest
import com.axionch.shared.api.ImageFilterApplyResponse
import com.axionch.shared.api.ImageFilterListResponse
import com.axionch.shared.api.ImageHealRequest
import com.axionch.shared.api.ImageHealResponse
import com.axionch.shared.api.ImageEraseRegion
import com.axionch.shared.api.ImageLayer
import com.axionch.shared.api.ExportPresetListResponse
import com.axionch.shared.api.ExportQueueRequest
import com.axionch.shared.api.ExportQueueResponse
import com.axionch.shared.api.ExportJobStatusResponse
import com.axionch.shared.api.AssetDeleteResponse
import com.axionch.shared.api.AssetListResponse
import com.axionch.shared.api.AssetUpsertRequest
import com.axionch.shared.api.AssetUpsertResponse
import com.axionch.shared.api.MotionTrackRequest
import com.axionch.shared.api.MotionTrackResponse
import com.axionch.shared.api.TimelineAudioTrack
import com.axionch.shared.api.TimelineClip
import com.axionch.shared.api.TimelineComposeRequest
import com.axionch.shared.api.TimelineComposeResponse
import com.axionch.shared.api.TimelineKeyframe

class AxionRepository {
    suspend fun getHealth(): HealthResponse = ApiModule.api().health()
    suspend fun getConfigStatus(): ConfigStatusResponse = ApiModule.api().getConfigStatus()
    suspend fun runConfigCheck(): ConfigCheckResponse = ApiModule.api().runConfigCheck()
    suspend fun getConfigSecurity(): ConfigSecurityResponse = ApiModule.api().getConfigSecurity()
    suspend fun getDryRunHistory(
        limit: Int = 25,
        platform: String? = null,
        successOnly: Boolean? = null
    ): DryRunHistoryResponse = ApiModule.api().getDryRunHistory(
        limit = limit,
        platform = platform,
        successOnly = successOnly
    )
    suspend fun clearDryRunHistory(platform: String? = null): DryRunHistoryClearResponse =
        ApiModule.api().clearDryRunHistory(platform = platform)

    suspend fun queuePost(
        email: String,
        body: String,
        imageUrl: String?,
        accountIds: List<Int>
    ): QueuedPostResponse {
        return ApiModule.api().queuePost(
            CreatePostRequest(
                user_email = email,
                body = body,
                image_url = imageUrl,
                account_ids = accountIds
            )
        )
    }

    suspend fun getPublishJob(jobId: Int): PublishJobStatusResponse = ApiModule.api().getPublishJob(jobId)
    suspend fun getPublishMetrics(): PublishMetricsResponse = ApiModule.api().getPublishMetrics()
    suspend fun getDeadLetters(limit: Int = 50): DeadLetterListResponse = ApiModule.api().getDeadLetters(limit)
    suspend fun requeueDeadLetter(deadLetterId: Int): RequeueDeadLetterResponse =
        ApiModule.api().requeueDeadLetter(deadLetterId)

    suspend fun oauthStart(platform: String, email: String): OAuthStartResponse =
        ApiModule.api().oauthStart(platform = platform, userEmail = email)

    suspend fun oauthCallback(platform: String, code: String, state: String): OAuthCallbackResponse =
        ApiModule.api().oauthCallback(platform = platform, code = code, state = state)

    suspend fun createUserApiKey(userEmail: String, label: String): ApiKeyResponse =
        ApiModule.api().createUserApiKey(CreateApiKeyRequest(user_email = userEmail, label = label))

    suspend fun listUserApiKeys(userEmail: String): ApiKeyListResponse =
        ApiModule.api().listUserApiKeys(userEmail)

    suspend fun revokeUserApiKey(userEmail: String, keyId: String): ApiKeyRevokeResponse =
        ApiModule.api().revokeUserApiKey(userEmail, keyId)

    suspend fun getAccounts(): List<AccountResponse> = ApiModule.api().getAccounts()

    suspend fun createMockAccount(
        email: String,
        platform: String,
        handle: String
    ): AccountResponse {
        return ApiModule.api().createAccount(
            CreateAccountRequest(
                user_email = email,
                platform = platform,
                handle = handle,
                access_token = "mock-token-$platform"
            )
        )
    }

    suspend fun deleteAccount(id: Int) {
        ApiModule.api().deleteAccount(id)
    }

    suspend fun createPost(
        email: String,
        body: String,
        imageUrl: String?,
        accountIds: List<Int>
    ): PostResponse {
        return ApiModule.api().createPost(
            CreatePostRequest(
                user_email = email,
                body = body,
                image_url = imageUrl,
                account_ids = accountIds
            )
        )
    }

    suspend fun dryRunPost(
        email: String,
        body: String,
        imageUrl: String?,
        accountIds: List<Int>
    ): PostResponse {
        return ApiModule.api().dryRunPost(
            CreatePostRequest(
                user_email = email,
                body = body,
                image_url = imageUrl,
                account_ids = accountIds
            )
        )
    }

    suspend fun createVaultEntry(
        userEmail: String,
        serviceName: String,
        username: String,
        password: String,
        notes: String?
    ): VaultEntryResponse = ApiModule.api().createVaultEntry(
        VaultEntryCreateRequest(
            user_email = userEmail,
            service_name = serviceName,
            username = username,
            password = password,
            notes = notes
        )
    )

    suspend fun listVaultEntries(userEmail: String): VaultListResponse =
        ApiModule.api().listVaultEntries(userEmail)

    suspend fun getVaultEntry(entryId: Int, userEmail: String): VaultEntryResponse =
        ApiModule.api().getVaultEntry(entryId = entryId, userEmail = userEmail)

    suspend fun updateVaultEntry(
        entryId: Int,
        userEmail: String,
        serviceName: String? = null,
        username: String? = null,
        password: String? = null,
        notes: String? = null
    ): VaultEntryResponse = ApiModule.api().updateVaultEntry(
        entryId = entryId,
        userEmail = userEmail,
        requestBody = VaultEntryUpdateRequest(
            service_name = serviceName,
            username = username,
            password = password,
            notes = notes
        )
    )

    suspend fun deleteVaultEntry(entryId: Int, userEmail: String): VaultDeleteResponse =
        ApiModule.api().deleteVaultEntry(entryId = entryId, userEmail = userEmail)

    suspend fun getVideoFilters(): VideoFilterListResponse = ApiModule.api().getVideoFilters()

    suspend fun applyVideoFilter(
        sourcePath: String?,
        sourceUrl: String?,
        presetId: String,
        outputName: String?,
        sepia: Boolean = false,
        blackWhite: Boolean = false,
        saturationFactor: Double = 1.0,
        brightnessDelta: Double = 0.0,
        overlayPath: String? = null,
        overlayUrl: String? = null,
        textOverlay: String? = null,
        subtitlePath: String? = null,
        subtitleUrl: String? = null,
        burnSubtitles: Boolean = false,
        speedFactor: Double = 1.0,
        stabilize: Boolean = false,
        curvesPreset: String? = null,
        lut3dPath: String? = null,
        lut3dUrl: String? = null,
        soundPath: String? = null,
        soundUrl: String? = null,
        soundVolume: Double = 1.0,
        audioDenoise: Boolean = false,
        audioEqLowGain: Double = 0.0,
        audioEqMidGain: Double = 0.0,
        audioEqHighGain: Double = 0.0,
        audioCompressor: Boolean = false,
        audioGate: Boolean = false,
        audioDucking: Boolean = false,
        loudnessNormalization: Boolean = false,
        audioLimiter: Boolean = false
    ): VideoFilterApplyResponse = ApiModule.api().applyVideoFilter(
        VideoFilterApplyRequest(
            source_path = sourcePath,
            source_url = sourceUrl,
            preset_id = presetId,
            output_name = outputName,
            sepia = sepia,
            black_white = blackWhite,
            saturation_factor = saturationFactor,
            brightness_delta = brightnessDelta,
            overlay_path = overlayPath,
            overlay_url = overlayUrl,
            text_overlay = textOverlay,
            subtitle_path = subtitlePath,
            subtitle_url = subtitleUrl,
            burn_subtitles = burnSubtitles,
            speed_factor = speedFactor,
            stabilize = stabilize,
            curves_preset = curvesPreset,
            lut3d_path = lut3dPath,
            lut3d_url = lut3dUrl,
            sound_path = soundPath,
            sound_url = soundUrl,
            sound_volume = soundVolume,
            audio_denoise = audioDenoise,
            audio_eq_low_gain = audioEqLowGain,
            audio_eq_mid_gain = audioEqMidGain,
            audio_eq_high_gain = audioEqHighGain,
            audio_compressor = audioCompressor,
            audio_gate = audioGate,
            audio_ducking = audioDucking,
            loudness_normalization = loudnessNormalization,
            audio_limiter = audioLimiter
        )
    )

    suspend fun getImageFilters(): ImageFilterListResponse = ApiModule.api().getImageFilters()

    suspend fun getCreatorTemplates(
        platform: String? = null,
        mediaType: String? = null
    ): CreatorTemplateListResponse = ApiModule.api().getCreatorTemplates(
        platform = platform,
        mediaType = mediaType
    )

    suspend fun getExportPresets(): ExportPresetListResponse = ApiModule.api().getExportPresets()

    suspend fun queueExportJob(
        sourcePaths: List<String>,
        sourceUrls: List<String>,
        presetId: String,
        outputPrefix: String? = null,
        hardwareAcceleration: Boolean = false
    ): ExportQueueResponse = ApiModule.api().queueExportJob(
        ExportQueueRequest(
            source_paths = sourcePaths,
            source_urls = sourceUrls,
            preset_id = presetId,
            output_prefix = outputPrefix,
            hardware_acceleration = hardwareAcceleration
        )
    )

    suspend fun getExportJobStatus(jobId: String): ExportJobStatusResponse =
        ApiModule.api().getExportJobStatus(jobId)

    suspend fun getAssets(
        kind: String? = null,
        tag: String? = null
    ): AssetListResponse = ApiModule.api().getAssets(kind = kind, tag = tag)

    suspend fun addAsset(
        kind: String,
        title: String,
        source: String,
        licenseName: String,
        licenseUrl: String? = null,
        attributionRequired: Boolean = false,
        attributionText: String? = null,
        tags: List<String> = emptyList(),
        localPath: String? = null,
        remoteUrl: String? = null
    ): AssetUpsertResponse = ApiModule.api().addAsset(
        AssetUpsertRequest(
            kind = kind,
            title = title,
            source = source,
            license_name = licenseName,
            license_url = licenseUrl,
            attribution_required = attributionRequired,
            attribution_text = attributionText,
            tags = tags,
            local_path = localPath,
            remote_url = remoteUrl
        )
    )

    suspend fun deleteAsset(assetId: String): AssetDeleteResponse =
        ApiModule.api().deleteAsset(assetId)

    suspend fun applyImageFilter(
        sourcePath: String?,
        sourceUrl: String?,
        presetId: String,
        outputName: String?,
        sepia: Boolean = false,
        blackWhite: Boolean = false,
        cartoonify: Boolean = false,
        caricature: Boolean = false,
        saturationFactor: Double = 1.0,
        brightnessFactor: Double = 1.0,
        contrastFactor: Double = 1.0,
        overlayPath: String? = null,
        overlayUrl: String? = null,
        textOverlay: String? = null,
        rawEnhance: Boolean = false,
        selectiveX: Int? = null,
        selectiveY: Int? = null,
        selectiveWidth: Int? = null,
        selectiveHeight: Int? = null,
        selectiveBrightnessFactor: Double = 1.0,
        selectiveContrastFactor: Double = 1.0,
        selectiveSaturationFactor: Double = 1.0,
        eraseRegions: List<ImageEraseRegion> = emptyList(),
        layers: List<ImageLayer> = emptyList(),
        backgroundKeyColor: String? = null,
        backgroundKeyTolerance: Int = 24
    ): ImageFilterApplyResponse = ApiModule.api().applyImageFilter(
        ImageFilterApplyRequest(
            source_path = sourcePath,
            source_url = sourceUrl,
            preset_id = presetId,
            output_name = outputName,
            sepia = sepia,
            black_white = blackWhite,
            cartoonify = cartoonify,
            caricature = caricature,
            saturation_factor = saturationFactor,
            brightness_factor = brightnessFactor,
            contrast_factor = contrastFactor,
            overlay_path = overlayPath,
            overlay_url = overlayUrl,
            text_overlay = textOverlay,
            raw_enhance = rawEnhance,
            selective_x = selectiveX,
            selective_y = selectiveY,
            selective_width = selectiveWidth,
            selective_height = selectiveHeight,
            selective_brightness_factor = selectiveBrightnessFactor,
            selective_contrast_factor = selectiveContrastFactor,
            selective_saturation_factor = selectiveSaturationFactor,
            erase_regions = eraseRegions,
            layers = layers,
            background_key_color = backgroundKeyColor,
            background_key_tolerance = backgroundKeyTolerance
        )
    )

    suspend fun healImage(
        sourcePath: String?,
        sourceUrl: String?,
        outputName: String? = null,
        maskPath: String? = null,
        maskUrl: String? = null,
        eraseRegions: List<ImageEraseRegion> = emptyList(),
        inpaintRadius: Double = 3.0,
        method: String = "telea",
        fillStrategy: String = "inpaint",
        featherRadius: Int = 3,
        preserveEdges: Boolean = true,
        edgeBlend: Double = 0.55,
        denoiseStrength: Double = 0.0
    ): ImageHealResponse = ApiModule.api().healImage(
        ImageHealRequest(
            source_path = sourcePath,
            source_url = sourceUrl,
            output_name = outputName,
            mask_path = maskPath,
            mask_url = maskUrl,
            erase_regions = eraseRegions,
            inpaint_radius = inpaintRadius,
            method = method,
            fill_strategy = fillStrategy,
            feather_radius = featherRadius,
            preserve_edges = preserveEdges,
            edge_blend = edgeBlend,
            denoise_strength = denoiseStrength
        )
    )

    suspend fun composeTimeline(
        clips: List<TimelineClip>,
        audioTracks: List<TimelineAudioTrack> = emptyList(),
        keyframes: List<TimelineKeyframe> = emptyList(),
        transitionDefault: String? = null,
        normalizeLoudness: Boolean = false,
        outputName: String? = null,
        backgroundAudioPath: String? = null,
        backgroundAudioUrl: String? = null,
        backgroundAudioVolume: Double = 0.35
    ): TimelineComposeResponse = ApiModule.api().composeTimeline(
        TimelineComposeRequest(
            clips = clips,
            audio_tracks = audioTracks,
            keyframes = keyframes,
            transition_default = transitionDefault,
            normalize_loudness = normalizeLoudness,
            output_name = outputName,
            background_audio_path = backgroundAudioPath,
            background_audio_url = backgroundAudioUrl,
            background_audio_volume = backgroundAudioVolume
        )
    )

    suspend fun transcribeCaptions(
        sourcePath: String?,
        sourceUrl: String?,
        language: String? = null,
        outputName: String? = null,
        maxCharsPerLine: Int = 42
    ): CaptionTranscribeResponse = ApiModule.api().transcribeCaptions(
        CaptionTranscribeRequest(
            source_path = sourcePath,
            source_url = sourceUrl,
            language = language,
            output_name = outputName,
            max_chars_per_line = maxCharsPerLine
        )
    )

    suspend fun buildSrt(request: CaptionSrtBuildRequest): CaptionSrtBuildResponse =
        ApiModule.api().buildSrt(request)

    suspend fun trackVideoMotion(
        sourcePath: String?,
        sourceUrl: String?,
        startSeconds: Double = 0.0,
        endSeconds: Double? = null,
        maxCorners: Int = 150,
        qualityLevel: Double = 0.2,
        minDistance: Double = 8.0,
        sampleEveryNFrames: Int = 2,
        roiX: Int? = null,
        roiY: Int? = null,
        roiWidth: Int? = null,
        roiHeight: Int? = null,
        smoothingWindow: Int = 4,
        minConfidence: Double = 0.05,
        outputOverlayName: String? = null
    ): MotionTrackResponse = ApiModule.api().trackVideoMotion(
        MotionTrackRequest(
            source_path = sourcePath,
            source_url = sourceUrl,
            start_seconds = startSeconds,
            end_seconds = endSeconds,
            max_corners = maxCorners,
            quality_level = qualityLevel,
            min_distance = minDistance,
            sample_every_n_frames = sampleEveryNFrames,
            roi_x = roiX,
            roi_y = roiY,
            roi_width = roiWidth,
            roi_height = roiHeight,
            smoothing_window = smoothingWindow,
            min_confidence = minConfidence,
            output_overlay_name = outputOverlayName
        )
    )
}


