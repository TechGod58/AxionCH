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
    val subtitle_path: String? = null,
    val subtitle_url: String? = null,
    val burn_subtitles: Boolean = false,
    val speed_factor: Double = 1.0,
    val stabilize: Boolean = false,
    val curves_preset: String? = null,
    val lut3d_path: String? = null,
    val lut3d_url: String? = null,
    val sound_path: String? = null,
    val sound_url: String? = null,
    val sound_volume: Double = 1.0,
    val audio_denoise: Boolean = false,
    val audio_eq_low_gain: Double = 0.0,
    val audio_eq_mid_gain: Double = 0.0,
    val audio_eq_high_gain: Double = 0.0,
    val audio_compressor: Boolean = false,
    val audio_gate: Boolean = false,
    val audio_ducking: Boolean = false,
    val loudness_normalization: Boolean = false,
    val audio_limiter: Boolean = false
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

data class ImageEraseRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class ImageLayer(
    val layer_path: String? = null,
    val layer_url: String? = null,
    val mask_path: String? = null,
    val mask_url: String? = null,
    val opacity: Double = 1.0,
    val blend_mode: String = "normal",
    val x: Int = 0,
    val y: Int = 0
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
    val text_color: String = "#FFFFFF",
    val raw_enhance: Boolean = false,
    val selective_x: Int? = null,
    val selective_y: Int? = null,
    val selective_width: Int? = null,
    val selective_height: Int? = null,
    val selective_brightness_factor: Double = 1.0,
    val selective_contrast_factor: Double = 1.0,
    val selective_saturation_factor: Double = 1.0,
    val erase_regions: List<ImageEraseRegion> = emptyList(),
    val layers: List<ImageLayer> = emptyList(),
    val background_key_color: String? = null,
    val background_key_tolerance: Int = 24
)

data class ImageFilterApplyResponse(
    val success: Boolean,
    val message: String,
    val pillow_available: Boolean,
    val preset_id: String,
    val source_path_used: String? = null,
    val output_path: String? = null
)

data class ImageHealRequest(
    val source_path: String? = null,
    val source_url: String? = null,
    val output_name: String? = null,
    val mask_path: String? = null,
    val mask_url: String? = null,
    val erase_regions: List<ImageEraseRegion> = emptyList(),
    val inpaint_radius: Double = 3.0,
    val method: String = "telea",
    val fill_strategy: String = "inpaint",
    val feather_radius: Int = 3,
    val preserve_edges: Boolean = true,
    val edge_blend: Double = 0.55,
    val denoise_strength: Double = 0.0
)

data class ImageHealResponse(
    val success: Boolean,
    val message: String,
    val source_path_used: String? = null,
    val output_path: String? = null
)

data class CreatorTemplate(
    val id: String,
    val name: String,
    val media_type: String,
    val platforms: List<String>,
    val width: Int,
    val height: Int,
    val fps: Int? = null,
    val description: String
)

data class CreatorTemplateListResponse(
    val templates: List<CreatorTemplate>,
    val total_returned: Int,
    val platform: String? = null,
    val media_type: String? = null
)

data class ExportPreset(
    val id: String,
    val name: String,
    val platform: String,
    val media_type: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val video_bitrate: String,
    val audio_bitrate: String,
    val container: String
)

data class ExportPresetListResponse(
    val presets: List<ExportPreset>,
    val total_returned: Int
)

data class ExportQueueRequest(
    val source_paths: List<String> = emptyList(),
    val source_urls: List<String> = emptyList(),
    val preset_id: String,
    val output_prefix: String? = null,
    val hardware_acceleration: Boolean = false
)

data class ExportQueueResponse(
    val job_id: String,
    val status: String,
    val queued_items: Int,
    val message: String
)

data class ExportJobItem(
    val index: Int,
    val source: String,
    val output_path: String? = null,
    val status: String,
    val message: String? = null
)

data class ExportJobStatusResponse(
    val job_id: String,
    val preset_id: String,
    val status: String,
    val created_at: String,
    val completed_at: String? = null,
    val queued_items: Int,
    val completed_items: Int,
    val failed_items: Int,
    val items: List<ExportJobItem>
)

data class TimelineClip(
    val source_path: String? = null,
    val source_url: String? = null,
    val start_seconds: Double? = null,
    val end_seconds: Double? = null,
    val speed_factor: Double = 1.0,
    val volume: Double = 1.0,
    val split_points: List<Double> = emptyList(),
    val transition_to_next: String? = null,
    val transition_duration: Double = 0.35
)

data class TimelineAudioTrack(
    val source_path: String? = null,
    val source_url: String? = null,
    val start_seconds: Double = 0.0,
    val end_seconds: Double? = null,
    val volume: Double = 1.0
)

data class TimelineKeyframe(
    val at_seconds: Double,
    val property: String,
    val value: String
)

data class TimelineComposeRequest(
    val clips: List<TimelineClip>,
    val audio_tracks: List<TimelineAudioTrack> = emptyList(),
    val keyframes: List<TimelineKeyframe> = emptyList(),
    val transition_default: String? = null,
    val normalize_loudness: Boolean = false,
    val output_name: String? = null,
    val background_audio_path: String? = null,
    val background_audio_url: String? = null,
    val background_audio_volume: Double = 0.35
)

data class TimelineComposeResponse(
    val success: Boolean,
    val message: String,
    val clip_count: Int,
    val output_path: String? = null,
    val warnings: List<String> = emptyList()
)

data class MotionTrackPoint(
    val at_seconds: Double,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val confidence: Double,
    val point_count: Int
)

data class MotionTrackRequest(
    val source_path: String? = null,
    val source_url: String? = null,
    val start_seconds: Double = 0.0,
    val end_seconds: Double? = null,
    val max_corners: Int = 150,
    val quality_level: Double = 0.2,
    val min_distance: Double = 8.0,
    val sample_every_n_frames: Int = 2,
    val roi_x: Int? = null,
    val roi_y: Int? = null,
    val roi_width: Int? = null,
    val roi_height: Int? = null,
    val smoothing_window: Int = 4,
    val min_confidence: Double = 0.05,
    val output_overlay_name: String? = null
)

data class MotionTrackResponse(
    val success: Boolean,
    val message: String,
    val source_path_used: String? = null,
    val fps: Double = 0.0,
    val frame_count: Int = 0,
    val average_confidence: Double = 0.0,
    val overlay_path: String? = null,
    val track_points: List<MotionTrackPoint> = emptyList()
)

data class CaptionCue(
    val start_seconds: Double,
    val end_seconds: Double,
    val text: String
)

data class CaptionTranscribeRequest(
    val source_path: String? = null,
    val source_url: String? = null,
    val language: String? = null,
    val output_name: String? = null,
    val max_chars_per_line: Int = 42
)

data class CaptionTranscribeResponse(
    val success: Boolean,
    val message: String,
    val engine: String,
    val cue_count: Int,
    val srt_path: String? = null
)

data class CaptionSrtBuildRequest(
    val cues: List<CaptionCue>,
    val output_name: String? = null
)

data class CaptionSrtBuildResponse(
    val success: Boolean,
    val message: String,
    val cue_count: Int,
    val srt_path: String? = null
)

data class AssetItem(
    val id: String,
    val kind: String,
    val title: String,
    val source: String,
    val license_name: String,
    val license_url: String? = null,
    val attribution_required: Boolean = false,
    val attribution_text: String? = null,
    val tags: List<String> = emptyList(),
    val local_path: String? = null,
    val remote_url: String? = null,
    val created_at: String,
    val updated_at: String
)

data class AssetListResponse(
    val items: List<AssetItem>,
    val total_returned: Int,
    val kind: String? = null,
    val tag: String? = null
)

data class AssetUpsertRequest(
    val kind: String,
    val title: String,
    val source: String,
    val license_name: String,
    val license_url: String? = null,
    val attribution_required: Boolean = false,
    val attribution_text: String? = null,
    val tags: List<String> = emptyList(),
    val local_path: String? = null,
    val remote_url: String? = null
)

data class AssetUpsertResponse(
    val success: Boolean,
    val message: String,
    val asset: AssetItem
)

data class AssetDeleteResponse(
    val deleted: Boolean,
    val asset_id: String
)

data class LiveDestinationConfig(
    val platform: String,
    val label: String? = null,
    val ingest_url: String,
    val stream_key: String? = null,
    val enabled: Boolean = true
)

data class LiveMulticastStartRequest(
    val session_name: String? = null,
    val source_path: String? = null,
    val source_url: String? = null,
    val dry_run: Boolean = false,
    val copy_video: Boolean = false,
    val video_bitrate: String = "4500k",
    val audio_bitrate: String = "160k",
    val fps: Int = 30,
    val gop_seconds: Double = 2.0,
    val destinations: List<LiveDestinationConfig> = emptyList()
)

data class LiveDestinationStatus(
    val destination_id: String,
    val platform: String,
    val label: String? = null,
    val ingest_url_masked: String,
    val stream_key_configured: Boolean,
    val status: String,
    val last_error: String? = null
)

data class LiveMulticastSessionResponse(
    val session_id: String,
    val session_name: String,
    val status: String,
    val message: String,
    val created_at: String,
    val started_at: String? = null,
    val stopped_at: String? = null,
    val source_kind: String,
    val source_value: String,
    val ffmpeg_pid: Int? = null,
    val ffmpeg_exit_code: Int? = null,
    val dry_run: Boolean = false,
    val active_destinations: Int = 0,
    val failed_destinations: Int = 0,
    val destinations: List<LiveDestinationStatus> = emptyList(),
    val log_tail: List<String> = emptyList()
)

data class LiveMulticastSessionListResponse(
    val sessions: List<LiveMulticastSessionResponse>,
    val total_returned: Int
)

data class LiveMulticastStopResponse(
    val session_id: String,
    val status: String,
    val stopped: Boolean,
    val message: String
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
