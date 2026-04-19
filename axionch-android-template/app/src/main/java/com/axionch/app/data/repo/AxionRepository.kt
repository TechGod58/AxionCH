package com.axionch.app.data.repo

import com.axionch.app.data.api.ApiModule
import com.axionch.shared.api.AccountResponse
import com.axionch.shared.api.AssetDeleteResponse
import com.axionch.shared.api.AssetListResponse
import com.axionch.shared.api.AssetUpsertRequest
import com.axionch.shared.api.AssetUpsertResponse
import com.axionch.shared.api.CaptionSrtBuildRequest
import com.axionch.shared.api.CaptionSrtBuildResponse
import com.axionch.shared.api.CaptionTranscribeRequest
import com.axionch.shared.api.CaptionTranscribeResponse
import com.axionch.shared.api.ConfigCheckResponse
import com.axionch.shared.api.ConfigSecurityResponse
import com.axionch.shared.api.ConfigStatusResponse
import com.axionch.shared.api.CreateApiKeyRequest
import com.axionch.shared.api.CreateAccountRequest
import com.axionch.shared.api.CreatePostRequest
import com.axionch.shared.api.DeadLetterListResponse
import com.axionch.shared.api.DryRunHistoryClearResponse
import com.axionch.shared.api.DryRunHistoryResponse
import com.axionch.shared.api.ExportJobStatusResponse
import com.axionch.shared.api.ExportPresetListResponse
import com.axionch.shared.api.ExportQueueRequest
import com.axionch.shared.api.ExportQueueResponse
import com.axionch.shared.api.HealthResponse
import com.axionch.shared.api.ImageEraseRegion
import com.axionch.shared.api.ImageHealRequest
import com.axionch.shared.api.ImageHealResponse
import com.axionch.shared.api.LiveMulticastSessionListResponse
import com.axionch.shared.api.LiveMulticastSessionResponse
import com.axionch.shared.api.LiveMulticastStartRequest
import com.axionch.shared.api.LiveMulticastStopResponse
import com.axionch.shared.api.MotionTrackRequest
import com.axionch.shared.api.MotionTrackResponse
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
import com.axionch.shared.api.CreatorTemplateListResponse
import com.axionch.shared.api.TimelineComposeRequest
import com.axionch.shared.api.TimelineComposeResponse
import com.axionch.shared.api.VaultDeleteResponse
import com.axionch.shared.api.VaultEntryCreateRequest
import com.axionch.shared.api.VaultEntryResponse
import com.axionch.shared.api.VaultEntryUpdateRequest
import com.axionch.shared.api.VaultListResponse

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

    suspend fun getCreatorTemplates(
        platform: String? = null,
        mediaType: String? = null
    ): CreatorTemplateListResponse = ApiModule.api().getCreatorTemplates(
        platform = platform,
        mediaType = mediaType
    )

    suspend fun getExportPresets(): ExportPresetListResponse =
        ApiModule.api().getExportPresets()

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

    suspend fun composeTimeline(
        request: TimelineComposeRequest
    ): TimelineComposeResponse = ApiModule.api().composeTimeline(request)

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

    suspend fun getAssets(kind: String? = null, tag: String? = null): AssetListResponse =
        ApiModule.api().getAssets(kind = kind, tag = tag)

    suspend fun addAsset(request: AssetUpsertRequest): AssetUpsertResponse =
        ApiModule.api().addAsset(request)

    suspend fun deleteAsset(assetId: String): AssetDeleteResponse =
        ApiModule.api().deleteAsset(assetId)

    suspend fun startLiveMulticast(request: LiveMulticastStartRequest): LiveMulticastSessionResponse =
        ApiModule.api().startLiveMulticast(request)

    suspend fun listLiveMulticastSessions(): LiveMulticastSessionListResponse =
        ApiModule.api().listLiveMulticastSessions()

    suspend fun getLiveMulticastSession(sessionId: String): LiveMulticastSessionResponse =
        ApiModule.api().getLiveMulticastSession(sessionId)

    suspend fun stopLiveMulticastSession(sessionId: String): LiveMulticastStopResponse =
        ApiModule.api().stopLiveMulticastSession(sessionId)

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
}


