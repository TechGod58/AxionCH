package com.axionch.shared.api

data class HealthResponse(
    val status: String,
    val service: String
)

data class CreateAccountRequest(
    val user_email: String,
    val platform: String,
    val handle: String,
    val access_token: String
)

data class AccountResponse(
    val id: Int,
    val platform: String,
    val handle: String,
    val status: String
)

data class CreatePostRequest(
    val user_email: String,
    val body: String,
    val image_url: String?,
    val account_ids: List<Int>
)

data class PublishResultItem(
    val account_id: Int,
    val platform: String,
    val status: String,
    val remote_post_id: String?,
    val error_message: String?
)

data class PostResponse(
    val post_id: Int?,
    val status: String,
    val results: List<PublishResultItem>,
    val dry_run: Boolean = false
)

data class PlatformConfigStatus(
    val mode: String,
    val configured: Boolean,
    val required_fields: List<String>,
    val configured_fields: List<String>,
    val source_by_field: Map<String, String> = emptyMap(),
    val last_checked_at: String? = null,
    val last_check_success: Boolean? = null,
    val last_check_error: String? = null,
    val check_count: Int = 0
)

data class ConfigStatusResponse(
    val x: PlatformConfigStatus,
    val linkedin: PlatformConfigStatus,
    val instagram: PlatformConfigStatus
)

data class ConfigCheckResult(
    val platform: String,
    val success: Boolean,
    val error_message: String?,
    val last_checked_at: String? = null,
    val check_count: Int = 0
)

data class ConfigCheckResponse(
    val checked_at: String,
    val results: List<ConfigCheckResult>
)

data class SecretCheckStatus(
    val name: String,
    val configured: Boolean,
    val strong: Boolean,
    val message: String
)

data class ConfigSecurityResponse(
    val production_ready: Boolean,
    val warnings: List<String>,
    val checks: List<SecretCheckStatus>
)

data class VideoFilterPreset(
    val id: String,
    val name: String,
    val description: String,
    val requires_video: Boolean = true
)

data class VideoFilterListResponse(
    val ffmpeg_available: Boolean,
    val ffmpeg_binary: String,
    val presets: List<VideoFilterPreset>
)

data class VideoFilterApplyRequest(
    val source_path: String? = null,
    val source_url: String? = null,
    val preset_id: String,
    val output_name: String? = null,
    val sepia: Boolean = false,
    val black_white: Boolean = false,
    val saturation_factor: Double = 1.0,
    val brightness_delta: Double = 0.0,
    val overlay_path: String? = null,
    val overlay_url: String? = null,
    val overlay_x: Int = 12,
    val overlay_y: Int = 12,
    val overlay_opacity: Double = 0.6,
    val text_overlay: String? = null,
    val text_x: Int = 16,
    val text_y: Int = 16,
    val text_size: Int = 28,
    val text_color: String = "white",
    val sound_path: String? = null,
    val sound_url: String? = null,
    val sound_volume: Double = 1.0
)

data class VideoFilterApplyResponse(
    val success: Boolean,
    val message: String,
    val ffmpeg_available: Boolean,
    val preset_id: String,
    val source_path_used: String? = null,
    val output_path: String? = null
)

data class ImageFilterPreset(
    val id: String,
    val name: String,
    val description: String,
    val requires_image: Boolean = true
)

data class ImageFilterListResponse(
    val pillow_available: Boolean,
    val presets: List<ImageFilterPreset>
)

data class ImageFilterApplyRequest(
    val source_path: String? = null,
    val source_url: String? = null,
    val preset_id: String,
    val output_name: String? = null,
    val sepia: Boolean = false,
    val black_white: Boolean = false,
    val cartoonify: Boolean = false,
    val caricature: Boolean = false,
    val saturation_factor: Double = 1.0,
    val brightness_factor: Double = 1.0,
    val contrast_factor: Double = 1.0,
    val overlay_path: String? = null,
    val overlay_url: String? = null,
    val overlay_x: Int = 12,
    val overlay_y: Int = 12,
    val overlay_opacity: Double = 0.6,
    val text_overlay: String? = null,
    val text_x: Int = 16,
    val text_y: Int = 16,
    val text_size: Int = 28,
    val text_color: String = "#FFFFFF"
)

data class ImageFilterApplyResponse(
    val success: Boolean,
    val message: String,
    val pillow_available: Boolean,
    val preset_id: String,
    val source_path_used: String? = null,
    val output_path: String? = null
)

data class DryRunHistoryItem(
    val id: String,
    val created_at: String,
    val user_email: String,
    val body_preview: String,
    val image_url: String?,
    val account_ids: List<Int>,
    val status: String,
    val results: List<PublishResultItem>
)

data class DryRunHistoryResponse(
    val items: List<DryRunHistoryItem>,
    val total_returned: Int,
    val limit: Int,
    val platform: String? = null,
    val success_only: Boolean? = null
)

data class DryRunHistoryClearResponse(
    val cleared_count: Int,
    val remaining_count: Int,
    val platform: String? = null
)

data class QueuedPostResponse(
    val job_id: Int,
    val post_id: Int,
    val status: String,
    val attempts: Int,
    val max_attempts: Int,
    val next_run_at: String?,
    val message: String
)

data class PublishJobStatusResponse(
    val job_id: Int,
    val post_id: Int,
    val status: String,
    val attempts: Int,
    val max_attempts: Int,
    val next_run_at: String?,
    val last_error: String?,
    val post_status: String,
    val results: List<PublishResultItem>
)

data class OAuthStartResponse(
    val platform: String,
    val auth_url: String,
    val state: String,
    val redirect_uri: String,
    val scopes: List<String>
)

data class OAuthCallbackResponse(
    val platform: String,
    val state_valid: Boolean,
    val account_id: Int?,
    val message: String
)

data class PublishMetricsResponse(
    val queue_depth: Int,
    val active_jobs: Int,
    val jobs_by_status: Map<String, Int>,
    val dead_letter_count: Int,
    val worker_processed_total: Int,
    val worker_success_total: Int,
    val worker_failure_total: Int
)

data class DeadLetterItem(
    val id: Int,
    val publish_job_id: Int?,
    val post_id: Int,
    val reason: String,
    val payload_json: String?,
    val created_at: String
)

data class DeadLetterListResponse(
    val items: List<DeadLetterItem>,
    val total_returned: Int,
    val limit: Int
)

data class RequeueDeadLetterResponse(
    val dead_letter_id: Int,
    val new_job_id: Int,
    val post_id: Int,
    val status: String,
    val message: String
)

data class CreateApiKeyRequest(
    val user_email: String,
    val label: String
)

data class ApiKeyResponse(
    val key_id: String,
    val user_email: String,
    val label: String,
    val raw_api_key: String,
    val created_at: String
)

data class ApiKeyListItem(
    val key_id: String,
    val label: String,
    val is_active: Boolean,
    val created_at: String,
    val last_used_at: String?,
    val revoked_at: String?
)

data class ApiKeyListResponse(
    val user_email: String,
    val keys: List<ApiKeyListItem>
)

data class ApiKeyRevokeResponse(
    val user_email: String,
    val key_id: String,
    val revoked: Boolean
)

data class VaultEntryCreateRequest(
    val user_email: String,
    val service_name: String,
    val username: String,
    val password: String,
    val notes: String? = null
)

data class VaultEntryUpdateRequest(
    val service_name: String? = null,
    val username: String? = null,
    val password: String? = null,
    val notes: String? = null
)

data class VaultEntrySummary(
    val id: Int,
    val user_email: String,
    val service_name: String,
    val username: String,
    val password_mask: String,
    val created_at: String,
    val updated_at: String
)

data class VaultListResponse(
    val entries: List<VaultEntrySummary>,
    val total_returned: Int
)

data class VaultEntryResponse(
    val id: Int,
    val user_email: String,
    val service_name: String,
    val username: String,
    val password: String,
    val notes: String? = null,
    val created_at: String,
    val updated_at: String
)

data class VaultDeleteResponse(
    val deleted: Boolean,
    val entry_id: Int
)
