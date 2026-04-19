from fastapi import APIRouter

from app.schemas.media import (
    ImageFilterApplyRequest,
    ImageFilterApplyResponse,
    ImageFilterListResponse,
    ImageFilterPreset,
    VideoFilterApplyRequest,
    VideoFilterApplyResponse,
    VideoFilterListResponse,
    VideoFilterPreset,
)
from app.services.media.image_filters import apply_image_filter, list_image_presets, pillow_is_available
from app.services.media.video_filters import apply_video_filter, ffmpeg_binary, ffmpeg_is_available, list_presets

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
        sound_path=payload.sound_path,
        sound_url=payload.sound_url,
        sound_volume=payload.sound_volume,
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
    )
    return ImageFilterApplyResponse(
        success=result.success,
        message=result.message,
        pillow_available=result.pillow_available,
        preset_id=result.preset_id,
        source_path_used=result.source_path_used,
        output_path=result.output_path,
    )
