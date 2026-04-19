from fastapi import APIRouter, HTTPException

from app.schemas.media import (
    AssetDeleteResponse,
    AssetItem,
    AssetListResponse,
    AssetUpsertRequest,
    AssetUpsertResponse,
    CaptionSrtBuildRequest,
    CaptionSrtBuildResponse,
    CaptionTranscribeRequest,
    CaptionTranscribeResponse,
    CreatorTemplate,
    CreatorTemplateListResponse,
    ExportJobItem,
    ExportJobStatusResponse,
    ExportPreset,
    ExportPresetListResponse,
    ExportQueueRequest,
    ExportQueueResponse,
    ImageHealRequest,
    ImageHealResponse,
    ImageFilterApplyRequest,
    ImageFilterApplyResponse,
    ImageFilterListResponse,
    ImageFilterPreset,
    LiveMulticastSessionListResponse,
    LiveMulticastSessionResponse,
    LiveMulticastStartRequest,
    LiveMulticastStopResponse,
    MotionTrackPoint,
    MotionTrackRequest,
    MotionTrackResponse,
    TimelineComposeRequest,
    TimelineComposeResponse,
    VideoFilterApplyRequest,
    VideoFilterApplyResponse,
    VideoFilterListResponse,
    VideoFilterPreset,
)
from app.services.media.live_multicast import live_multicast_service
from app.services.media.image_filters import apply_image_filter, list_image_presets, pillow_is_available
from app.services.media.tooling import (
    add_asset,
    build_srt_from_cues,
    delete_asset,
    export_queue_service,
    compose_timeline,
    list_assets,
    list_creator_templates,
    list_export_presets,
    transcribe_captions,
)
from app.services.media.video_filters import apply_video_filter, ffmpeg_binary, ffmpeg_is_available, list_presets
from app.services.media.vision import heal_image_with_inpaint, track_video_motion

router = APIRouter()


@router.get("/filters", response_model=VideoFilterListResponse)
def get_video_filter_presets() -> VideoFilterListResponse:
    presets = [
        VideoFilterPreset(
            id=item.id,
            name=item.name,
            description=item.description,
            requires_video=True,
        )
        for item in list_presets()
    ]
    return VideoFilterListResponse(
        ffmpeg_available=ffmpeg_is_available(),
        ffmpeg_binary=ffmpeg_binary(),
        presets=presets,
    )


@router.post("/filters/apply", response_model=VideoFilterApplyResponse)
def apply_filter(payload: VideoFilterApplyRequest) -> VideoFilterApplyResponse:
    result = apply_video_filter(
        preset_id=payload.preset_id,
        source_path=payload.source_path,
        source_url=payload.source_url,
        output_name=payload.output_name,
        sepia=payload.sepia,
        black_white=payload.black_white,
        saturation_factor=payload.saturation_factor,
        brightness_delta=payload.brightness_delta,
        overlay_path=payload.overlay_path,
        overlay_url=payload.overlay_url,
        overlay_x=payload.overlay_x,
        overlay_y=payload.overlay_y,
        overlay_opacity=payload.overlay_opacity,
        text_overlay=payload.text_overlay,
        text_x=payload.text_x,
        text_y=payload.text_y,
        text_size=payload.text_size,
        text_color=payload.text_color,
        subtitle_path=payload.subtitle_path,
        subtitle_url=payload.subtitle_url,
        burn_subtitles=payload.burn_subtitles,
        speed_factor=payload.speed_factor,
        stabilize=payload.stabilize,
        curves_preset=payload.curves_preset,
        lut3d_path=payload.lut3d_path,
        lut3d_url=payload.lut3d_url,
        sound_path=payload.sound_path,
        sound_url=payload.sound_url,
        sound_volume=payload.sound_volume,
        audio_denoise=payload.audio_denoise,
        audio_eq_low_gain=payload.audio_eq_low_gain,
        audio_eq_mid_gain=payload.audio_eq_mid_gain,
        audio_eq_high_gain=payload.audio_eq_high_gain,
        audio_compressor=payload.audio_compressor,
        audio_gate=payload.audio_gate,
        audio_ducking=payload.audio_ducking,
        loudness_normalization=payload.loudness_normalization,
        audio_limiter=payload.audio_limiter,
    )
    return VideoFilterApplyResponse(
        success=result.success,
        message=result.message,
        ffmpeg_available=result.ffmpeg_available,
        preset_id=result.preset_id,
        source_path_used=result.source_path_used,
        output_path=result.output_path,
    )


@router.get("/image-filters", response_model=ImageFilterListResponse)
def get_image_filter_presets() -> ImageFilterListResponse:
    presets = [
        ImageFilterPreset(
            id=item.id,
            name=item.name,
            description=item.description,
            requires_image=True,
        )
        for item in list_image_presets()
    ]
    return ImageFilterListResponse(
        pillow_available=pillow_is_available(),
        presets=presets,
    )


@router.post("/image-filters/apply", response_model=ImageFilterApplyResponse)
def apply_image_preset(payload: ImageFilterApplyRequest) -> ImageFilterApplyResponse:
    result = apply_image_filter(
        preset_id=payload.preset_id,
        source_path=payload.source_path,
        source_url=payload.source_url,
        output_name=payload.output_name,
        sepia=payload.sepia,
        black_white=payload.black_white,
        cartoonify=payload.cartoonify,
        caricature=payload.caricature,
        saturation_factor=payload.saturation_factor,
        brightness_factor=payload.brightness_factor,
        contrast_factor=payload.contrast_factor,
        overlay_path=payload.overlay_path,
        overlay_url=payload.overlay_url,
        overlay_x=payload.overlay_x,
        overlay_y=payload.overlay_y,
        overlay_opacity=payload.overlay_opacity,
        text_overlay=payload.text_overlay,
        text_x=payload.text_x,
        text_y=payload.text_y,
        text_size=payload.text_size,
        text_color=payload.text_color,
        raw_enhance=payload.raw_enhance,
        selective_x=payload.selective_x,
        selective_y=payload.selective_y,
        selective_width=payload.selective_width,
        selective_height=payload.selective_height,
        selective_brightness_factor=payload.selective_brightness_factor,
        selective_contrast_factor=payload.selective_contrast_factor,
        selective_saturation_factor=payload.selective_saturation_factor,
        erase_regions=[region.model_dump() for region in payload.erase_regions],
        layers=[layer.model_dump() for layer in payload.layers],
        background_key_color=payload.background_key_color,
        background_key_tolerance=payload.background_key_tolerance,
    )
    return ImageFilterApplyResponse(
        success=result.success,
        message=result.message,
        pillow_available=result.pillow_available,
        preset_id=result.preset_id,
        source_path_used=result.source_path_used,
        output_path=result.output_path,
    )


@router.post("/image/heal", response_model=ImageHealResponse)
def heal_image_route(payload: ImageHealRequest) -> ImageHealResponse:
    result = heal_image_with_inpaint(
        source_path=payload.source_path,
        source_url=payload.source_url,
        output_name=payload.output_name,
        mask_path=payload.mask_path,
        mask_url=payload.mask_url,
        erase_regions=[region.model_dump() for region in payload.erase_regions],
        inpaint_radius=payload.inpaint_radius,
        method=payload.method,
        fill_strategy=payload.fill_strategy,
        feather_radius=payload.feather_radius,
        preserve_edges=payload.preserve_edges,
        edge_blend=payload.edge_blend,
        denoise_strength=payload.denoise_strength,
    )
    return ImageHealResponse(
        success=result.success,
        message=result.message,
        source_path_used=result.source_path_used,
        output_path=result.output_path,
    )


@router.get("/templates", response_model=CreatorTemplateListResponse)
def get_creator_templates(platform: str | None = None, media_type: str | None = None) -> CreatorTemplateListResponse:
    templates = list_creator_templates(platform=platform, media_type=media_type)
    mapped = [
        CreatorTemplate(
            id=item.id,
            name=item.name,
            media_type=item.media_type,
            platforms=item.platforms,
            width=item.width,
            height=item.height,
            fps=item.fps,
            description=item.description,
        )
        for item in templates
    ]
    return CreatorTemplateListResponse(
        templates=mapped,
        total_returned=len(mapped),
        platform=platform,
        media_type=media_type,
    )


@router.get("/exports/presets", response_model=ExportPresetListResponse)
def get_export_presets() -> ExportPresetListResponse:
    presets = list_export_presets()
    mapped = [
        ExportPreset(
            id=item.id,
            name=item.name,
            platform=item.platform,
            media_type=item.media_type,
            width=item.width,
            height=item.height,
            fps=item.fps,
            video_bitrate=item.video_bitrate,
            audio_bitrate=item.audio_bitrate,
            container=item.container,
        )
        for item in presets
    ]
    return ExportPresetListResponse(presets=mapped, total_returned=len(mapped))


@router.post("/exports/queue", response_model=ExportQueueResponse)
def queue_export_job(payload: ExportQueueRequest) -> ExportQueueResponse:
    try:
        job = export_queue_service.queue_job(
            preset_id=payload.preset_id,
            source_paths=payload.source_paths,
            source_urls=payload.source_urls,
            output_prefix=payload.output_prefix,
            hardware_acceleration=payload.hardware_acceleration,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return ExportQueueResponse(
        job_id=job.job_id,
        status=job.status,
        queued_items=job.queued_items,
        message="Export job queued",
    )


@router.get("/exports/jobs/{job_id}", response_model=ExportJobStatusResponse)
def get_export_job_status(job_id: str) -> ExportJobStatusResponse:
    try:
        job = export_queue_service.get_job(job_id)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    return ExportJobStatusResponse(
        job_id=job.job_id,
        preset_id=job.preset_id,
        status=job.status,
        created_at=job.created_at,
        completed_at=job.completed_at,
        queued_items=job.queued_items,
        completed_items=job.completed_items,
        failed_items=job.failed_items,
        items=[
            ExportJobItem(
                index=item.index,
                source=item.source,
                output_path=item.output_path,
                status=item.status,
                message=item.message,
            )
            for item in job.items
        ],
    )


@router.post("/live/multicast/start", response_model=LiveMulticastSessionResponse)
def start_live_multicast(payload: LiveMulticastStartRequest) -> LiveMulticastSessionResponse:
    try:
        snapshot = live_multicast_service.start_session(
            session_name=payload.session_name,
            source_path=payload.source_path,
            source_url=payload.source_url,
            destinations=[item.model_dump() for item in payload.destinations],
            dry_run=payload.dry_run,
            copy_video=payload.copy_video,
            video_bitrate=payload.video_bitrate,
            audio_bitrate=payload.audio_bitrate,
            fps=payload.fps,
            gop_seconds=payload.gop_seconds,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return LiveMulticastSessionResponse(**snapshot)


@router.get("/live/multicast/sessions", response_model=LiveMulticastSessionListResponse)
def list_live_multicast_sessions() -> LiveMulticastSessionListResponse:
    snapshots = live_multicast_service.list_sessions()
    mapped = [LiveMulticastSessionResponse(**item) for item in snapshots]
    return LiveMulticastSessionListResponse(sessions=mapped, total_returned=len(mapped))


@router.get("/live/multicast/sessions/{session_id}", response_model=LiveMulticastSessionResponse)
def get_live_multicast_session(session_id: str) -> LiveMulticastSessionResponse:
    try:
        snapshot = live_multicast_service.get_session(session_id)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    return LiveMulticastSessionResponse(**snapshot)


@router.post("/live/multicast/sessions/{session_id}/stop", response_model=LiveMulticastStopResponse)
def stop_live_multicast_session(session_id: str) -> LiveMulticastStopResponse:
    try:
        snapshot = live_multicast_service.stop_session(session_id)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    status = str(snapshot.get("status") or "stopped")
    message = str(snapshot.get("message") or "Live multicast session stopped")
    return LiveMulticastStopResponse(
        session_id=session_id,
        status=status,
        stopped=status in {"stopped", "failed"},
        message=message,
    )


@router.post("/timeline/compose", response_model=TimelineComposeResponse)
def compose_timeline_route(payload: TimelineComposeRequest) -> TimelineComposeResponse:
    success, message, output_path, warnings = compose_timeline(
        clips=[clip.model_dump() for clip in payload.clips],
        audio_tracks=[track.model_dump() for track in payload.audio_tracks],
        keyframes=[keyframe.model_dump() for keyframe in payload.keyframes],
        transition_default=payload.transition_default,
        normalize_loudness=payload.normalize_loudness,
        output_name=payload.output_name,
        background_audio_path=payload.background_audio_path,
        background_audio_url=payload.background_audio_url,
        background_audio_volume=payload.background_audio_volume,
    )
    return TimelineComposeResponse(
        success=success,
        message=message,
        clip_count=len(payload.clips),
        output_path=output_path,
        warnings=warnings,
    )


@router.post("/video/motion-track", response_model=MotionTrackResponse)
def motion_track_route(payload: MotionTrackRequest) -> MotionTrackResponse:
    result = track_video_motion(
        source_path=payload.source_path,
        source_url=payload.source_url,
        start_seconds=payload.start_seconds,
        end_seconds=payload.end_seconds,
        max_corners=payload.max_corners,
        quality_level=payload.quality_level,
        min_distance=payload.min_distance,
        sample_every_n_frames=payload.sample_every_n_frames,
        roi_x=payload.roi_x,
        roi_y=payload.roi_y,
        roi_width=payload.roi_width,
        roi_height=payload.roi_height,
        smoothing_window=payload.smoothing_window,
        min_confidence=payload.min_confidence,
        output_overlay_name=payload.output_overlay_name,
    )
    return MotionTrackResponse(
        success=result.success,
        message=result.message,
        source_path_used=result.source_path_used,
        fps=result.fps,
        frame_count=result.frame_count,
        average_confidence=result.average_confidence,
        overlay_path=result.overlay_path,
        track_points=[
            MotionTrackPoint(
                at_seconds=item.at_seconds,
                x=item.x,
                y=item.y,
                width=item.width,
                height=item.height,
                confidence=item.confidence,
                point_count=item.point_count,
            )
            for item in (result.track_points or [])
        ],
    )


@router.post("/captions/transcribe", response_model=CaptionTranscribeResponse)
def transcribe_captions_route(payload: CaptionTranscribeRequest) -> CaptionTranscribeResponse:
    success, message, engine, cue_count, srt_path = transcribe_captions(
        source_path=payload.source_path,
        source_url=payload.source_url,
        language=payload.language,
        output_name=payload.output_name,
        max_chars_per_line=payload.max_chars_per_line,
    )
    return CaptionTranscribeResponse(
        success=success,
        message=message,
        engine=engine,
        cue_count=cue_count,
        srt_path=srt_path,
    )


@router.post("/captions/srt", response_model=CaptionSrtBuildResponse)
def build_srt_route(payload: CaptionSrtBuildRequest) -> CaptionSrtBuildResponse:
    success, message, srt_path, cue_count = build_srt_from_cues(
        cues=[cue.model_dump() for cue in payload.cues],
        output_name=payload.output_name,
    )
    return CaptionSrtBuildResponse(
        success=success,
        message=message,
        cue_count=cue_count,
        srt_path=srt_path,
    )


@router.get("/assets", response_model=AssetListResponse)
def get_assets(kind: str | None = None, tag: str | None = None) -> AssetListResponse:
    rows = list_assets(kind=kind, tag=tag)
    mapped = [
        AssetItem(
            id=str(item["id"]),
            kind=str(item["kind"]),
            title=str(item["title"]),
            source=str(item["source"]),
            license_name=str(item["license_name"]),
            license_url=item.get("license_url"),
            attribution_required=bool(item.get("attribution_required") or False),
            attribution_text=item.get("attribution_text"),
            tags=[str(value) for value in list(item.get("tags") or [])],
            local_path=item.get("local_path"),
            remote_url=item.get("remote_url"),
            created_at=str(item["created_at"]),
            updated_at=str(item["updated_at"]),
        )
        for item in rows
    ]
    return AssetListResponse(
        items=mapped,
        total_returned=len(mapped),
        kind=kind,
        tag=tag,
    )


@router.post("/assets", response_model=AssetUpsertResponse)
def upsert_asset_route(payload: AssetUpsertRequest) -> AssetUpsertResponse:
    try:
        row = add_asset(payload=payload.model_dump())
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    mapped = AssetItem(
        id=str(row["id"]),
        kind=str(row["kind"]),
        title=str(row["title"]),
        source=str(row["source"]),
        license_name=str(row["license_name"]),
        license_url=row.get("license_url"),
        attribution_required=bool(row.get("attribution_required") or False),
        attribution_text=row.get("attribution_text"),
        tags=[str(value) for value in list(row.get("tags") or [])],
        local_path=row.get("local_path"),
        remote_url=row.get("remote_url"),
        created_at=str(row["created_at"]),
        updated_at=str(row["updated_at"]),
    )
    return AssetUpsertResponse(success=True, message="Asset saved", asset=mapped)


@router.delete("/assets/{asset_id}", response_model=AssetDeleteResponse)
def delete_asset_route(asset_id: str) -> AssetDeleteResponse:
    deleted = delete_asset(asset_id=asset_id)
    if not deleted:
        raise HTTPException(status_code=404, detail=f"Asset '{asset_id}' was not found")
    return AssetDeleteResponse(deleted=True, asset_id=asset_id)
