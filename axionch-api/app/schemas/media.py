from pydantic import BaseModel, Field


class VideoFilterPreset(BaseModel):
    id: str
    name: str
    description: str
    requires_video: bool = True


class VideoFilterListResponse(BaseModel):
    ffmpeg_available: bool
    ffmpeg_binary: str
    presets: list[VideoFilterPreset]


class VideoFilterApplyRequest(BaseModel):
    source_path: str | None = Field(default=None, max_length=4096)
    source_url: str | None = Field(default=None, max_length=4096)
    preset_id: str = Field(min_length=2, max_length=120)
    output_name: str | None = Field(default=None, max_length=240)
    sepia: bool = False
    black_white: bool = False
    saturation_factor: float = 1.0
    brightness_delta: float = 0.0
    overlay_path: str | None = Field(default=None, max_length=4096)
    overlay_url: str | None = Field(default=None, max_length=4096)
    overlay_x: int = 12
    overlay_y: int = 12
    overlay_opacity: float = 0.6
    text_overlay: str | None = Field(default=None, max_length=240)
    text_x: int = 16
    text_y: int = 16
    text_size: int = 28
    text_color: str = "white"
    subtitle_path: str | None = Field(default=None, max_length=4096)
    subtitle_url: str | None = Field(default=None, max_length=4096)
    burn_subtitles: bool = False
    speed_factor: float = 1.0
    stabilize: bool = False
    curves_preset: str | None = Field(default=None, max_length=80)
    lut3d_path: str | None = Field(default=None, max_length=4096)
    lut3d_url: str | None = Field(default=None, max_length=4096)
    sound_path: str | None = Field(default=None, max_length=4096)
    sound_url: str | None = Field(default=None, max_length=4096)
    sound_volume: float = 1.0
    audio_denoise: bool = False
    audio_eq_low_gain: float = 0.0
    audio_eq_mid_gain: float = 0.0
    audio_eq_high_gain: float = 0.0
    audio_compressor: bool = False
    audio_gate: bool = False
    audio_ducking: bool = False
    loudness_normalization: bool = False
    audio_limiter: bool = False


class VideoFilterApplyResponse(BaseModel):
    success: bool
    message: str
    ffmpeg_available: bool
    preset_id: str
    source_path_used: str | None = None
    output_path: str | None = None


class ImageFilterPreset(BaseModel):
    id: str
    name: str
    description: str
    requires_image: bool = True


class ImageFilterListResponse(BaseModel):
    pillow_available: bool
    presets: list[ImageFilterPreset]


class ImageEraseRegion(BaseModel):
    x: int
    y: int
    width: int
    height: int


class ImageLayer(BaseModel):
    layer_path: str | None = Field(default=None, max_length=4096)
    layer_url: str | None = Field(default=None, max_length=4096)
    mask_path: str | None = Field(default=None, max_length=4096)
    mask_url: str | None = Field(default=None, max_length=4096)
    opacity: float = 1.0
    blend_mode: str = Field(default="normal", max_length=24)
    x: int = 0
    y: int = 0


class ImageFilterApplyRequest(BaseModel):
    source_path: str | None = Field(default=None, max_length=4096)
    source_url: str | None = Field(default=None, max_length=4096)
    preset_id: str = Field(min_length=2, max_length=120)
    output_name: str | None = Field(default=None, max_length=240)
    sepia: bool = False
    black_white: bool = False
    cartoonify: bool = False
    caricature: bool = False
    saturation_factor: float = 1.0
    brightness_factor: float = 1.0
    contrast_factor: float = 1.0
    overlay_path: str | None = Field(default=None, max_length=4096)
    overlay_url: str | None = Field(default=None, max_length=4096)
    overlay_x: int = 12
    overlay_y: int = 12
    overlay_opacity: float = 0.6
    text_overlay: str | None = Field(default=None, max_length=240)
    text_x: int = 16
    text_y: int = 16
    text_size: int = 28
    text_color: str = "#FFFFFF"
    raw_enhance: bool = False
    selective_x: int | None = None
    selective_y: int | None = None
    selective_width: int | None = None
    selective_height: int | None = None
    selective_brightness_factor: float = 1.0
    selective_contrast_factor: float = 1.0
    selective_saturation_factor: float = 1.0
    erase_regions: list[ImageEraseRegion] = Field(default_factory=list)
    layers: list[ImageLayer] = Field(default_factory=list)
    background_key_color: str | None = Field(default=None, max_length=16)
    background_key_tolerance: int = 24


class ImageFilterApplyResponse(BaseModel):
    success: bool
    message: str
    pillow_available: bool
    preset_id: str
    source_path_used: str | None = None
    output_path: str | None = None


class ImageHealRequest(BaseModel):
    source_path: str | None = Field(default=None, max_length=4096)
    source_url: str | None = Field(default=None, max_length=4096)
    output_name: str | None = Field(default=None, max_length=240)
    mask_path: str | None = Field(default=None, max_length=4096)
    mask_url: str | None = Field(default=None, max_length=4096)
    erase_regions: list[ImageEraseRegion] = Field(default_factory=list)
    inpaint_radius: float = 3.0
    method: str = Field(default="telea", max_length=24)
    fill_strategy: str = Field(default="inpaint", max_length=24)
    feather_radius: int = 3
    preserve_edges: bool = True
    edge_blend: float = 0.55
    denoise_strength: float = 0.0


class ImageHealResponse(BaseModel):
    success: bool
    message: str
    source_path_used: str | None = None
    output_path: str | None = None


class CreatorTemplate(BaseModel):
    id: str
    name: str
    media_type: str
    platforms: list[str]
    width: int
    height: int
    fps: int | None = None
    description: str


class CreatorTemplateListResponse(BaseModel):
    templates: list[CreatorTemplate]
    total_returned: int
    platform: str | None = None
    media_type: str | None = None


class ExportPreset(BaseModel):
    id: str
    name: str
    platform: str
    media_type: str
    width: int
    height: int
    fps: int
    video_bitrate: str
    audio_bitrate: str
    container: str


class ExportPresetListResponse(BaseModel):
    presets: list[ExportPreset]
    total_returned: int


class ExportQueueRequest(BaseModel):
    source_paths: list[str] = Field(default_factory=list)
    source_urls: list[str] = Field(default_factory=list)
    preset_id: str = Field(min_length=2, max_length=120)
    output_prefix: str | None = Field(default=None, max_length=120)
    hardware_acceleration: bool = False


class ExportQueueResponse(BaseModel):
    job_id: str
    status: str
    queued_items: int
    message: str


class ExportJobItem(BaseModel):
    index: int
    source: str
    output_path: str | None = None
    status: str
    message: str | None = None


class ExportJobStatusResponse(BaseModel):
    job_id: str
    preset_id: str
    status: str
    created_at: str
    completed_at: str | None = None
    queued_items: int
    completed_items: int
    failed_items: int
    items: list[ExportJobItem]


class TimelineClip(BaseModel):
    source_path: str | None = Field(default=None, max_length=4096)
    source_url: str | None = Field(default=None, max_length=4096)
    start_seconds: float | None = None
    end_seconds: float | None = None
    speed_factor: float = 1.0
    volume: float = 1.0
    split_points: list[float] = Field(default_factory=list)
    transition_to_next: str | None = Field(default=None, max_length=40)
    transition_duration: float = 0.35


class TimelineAudioTrack(BaseModel):
    source_path: str | None = Field(default=None, max_length=4096)
    source_url: str | None = Field(default=None, max_length=4096)
    start_seconds: float = 0.0
    end_seconds: float | None = None
    volume: float = 1.0


class TimelineKeyframe(BaseModel):
    at_seconds: float
    property: str = Field(min_length=2, max_length=80)
    value: str = Field(min_length=1, max_length=240)


class TimelineComposeRequest(BaseModel):
    clips: list[TimelineClip] = Field(default_factory=list)
    audio_tracks: list[TimelineAudioTrack] = Field(default_factory=list)
    keyframes: list[TimelineKeyframe] = Field(default_factory=list)
    transition_default: str | None = Field(default=None, max_length=40)
    normalize_loudness: bool = False
    output_name: str | None = Field(default=None, max_length=240)
    background_audio_path: str | None = Field(default=None, max_length=4096)
    background_audio_url: str | None = Field(default=None, max_length=4096)
    background_audio_volume: float = 0.35


class TimelineComposeResponse(BaseModel):
    success: bool
    message: str
    clip_count: int
    output_path: str | None = None
    warnings: list[str] = Field(default_factory=list)


class MotionTrackPoint(BaseModel):
    at_seconds: float
    x: int
    y: int
    width: int
    height: int
    confidence: float
    point_count: int


class MotionTrackRequest(BaseModel):
    source_path: str | None = Field(default=None, max_length=4096)
    source_url: str | None = Field(default=None, max_length=4096)
    start_seconds: float = 0.0
    end_seconds: float | None = None
    max_corners: int = 150
    quality_level: float = 0.2
    min_distance: float = 8.0
    sample_every_n_frames: int = 2
    roi_x: int | None = None
    roi_y: int | None = None
    roi_width: int | None = None
    roi_height: int | None = None
    smoothing_window: int = 4
    min_confidence: float = 0.05
    output_overlay_name: str | None = Field(default=None, max_length=240)


class MotionTrackResponse(BaseModel):
    success: bool
    message: str
    source_path_used: str | None = None
    fps: float = 0.0
    frame_count: int = 0
    average_confidence: float = 0.0
    overlay_path: str | None = None
    track_points: list[MotionTrackPoint] = Field(default_factory=list)


class CaptionCue(BaseModel):
    start_seconds: float
    end_seconds: float
    text: str = Field(min_length=1, max_length=2000)


class CaptionTranscribeRequest(BaseModel):
    source_path: str | None = Field(default=None, max_length=4096)
    source_url: str | None = Field(default=None, max_length=4096)
    language: str | None = Field(default=None, max_length=24)
    output_name: str | None = Field(default=None, max_length=240)
    max_chars_per_line: int = 42


class CaptionTranscribeResponse(BaseModel):
    success: bool
    message: str
    engine: str
    cue_count: int
    srt_path: str | None = None


class CaptionSrtBuildRequest(BaseModel):
    cues: list[CaptionCue] = Field(default_factory=list)
    output_name: str | None = Field(default=None, max_length=240)


class CaptionSrtBuildResponse(BaseModel):
    success: bool
    message: str
    cue_count: int
    srt_path: str | None = None


class AssetItem(BaseModel):
    id: str
    kind: str
    title: str
    source: str
    license_name: str
    license_url: str | None = None
    attribution_required: bool = False
    attribution_text: str | None = None
    tags: list[str] = Field(default_factory=list)
    local_path: str | None = None
    remote_url: str | None = None
    created_at: str
    updated_at: str


class AssetListResponse(BaseModel):
    items: list[AssetItem]
    total_returned: int
    kind: str | None = None
    tag: str | None = None


class AssetUpsertRequest(BaseModel):
    kind: str = Field(min_length=2, max_length=40)
    title: str = Field(min_length=2, max_length=160)
    source: str = Field(min_length=2, max_length=160)
    license_name: str = Field(min_length=2, max_length=120)
    license_url: str | None = Field(default=None, max_length=4096)
    attribution_required: bool = False
    attribution_text: str | None = Field(default=None, max_length=1000)
    tags: list[str] = Field(default_factory=list)
    local_path: str | None = Field(default=None, max_length=4096)
    remote_url: str | None = Field(default=None, max_length=4096)


class AssetUpsertResponse(BaseModel):
    success: bool
    message: str
    asset: AssetItem


class AssetDeleteResponse(BaseModel):
    deleted: bool
    asset_id: str


class LiveDestinationConfig(BaseModel):
    platform: str = Field(min_length=1, max_length=40)
    label: str | None = Field(default=None, max_length=80)
    ingest_url: str = Field(min_length=8, max_length=4096)
    stream_key: str | None = Field(default=None, max_length=512)
    enabled: bool = True


class LiveMulticastStartRequest(BaseModel):
    session_name: str | None = Field(default=None, max_length=120)
    source_path: str | None = Field(default=None, max_length=4096)
    source_url: str | None = Field(default=None, max_length=4096)
    dry_run: bool = False
    copy_video: bool = False
    video_bitrate: str = Field(default="4500k", max_length=32)
    audio_bitrate: str = Field(default="160k", max_length=32)
    fps: int = 30
    gop_seconds: float = 2.0
    destinations: list[LiveDestinationConfig] = Field(default_factory=list)


class LiveDestinationStatus(BaseModel):
    destination_id: str
    platform: str
    label: str | None = None
    ingest_url_masked: str
    stream_key_configured: bool
    status: str
    last_error: str | None = None


class LiveMulticastSessionResponse(BaseModel):
    session_id: str
    session_name: str
    status: str
    message: str
    created_at: str
    started_at: str | None = None
    stopped_at: str | None = None
    source_kind: str
    source_value: str
    ffmpeg_pid: int | None = None
    ffmpeg_exit_code: int | None = None
    dry_run: bool = False
    active_destinations: int = 0
    failed_destinations: int = 0
    destinations: list[LiveDestinationStatus] = Field(default_factory=list)
    log_tail: list[str] = Field(default_factory=list)


class LiveMulticastSessionListResponse(BaseModel):
    sessions: list[LiveMulticastSessionResponse]
    total_returned: int


class LiveMulticastStopResponse(BaseModel):
    session_id: str
    status: str
    stopped: bool
    message: str
