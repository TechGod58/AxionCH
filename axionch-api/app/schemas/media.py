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
    sound_path: str | None = Field(default=None, max_length=4096)
    sound_url: str | None = Field(default=None, max_length=4096)
    sound_volume: float = 1.0


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


class ImageFilterApplyResponse(BaseModel):
    success: bool
    message: str
    pillow_available: bool
    preset_id: str
    source_path_used: str | None = None
    output_path: str | None = None
