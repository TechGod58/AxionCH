from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

from PIL import Image, ImageEnhance, ImageFilter, ImageOps, ImageDraw, ImageFont, ImageChops

from app.core.config import settings
from app.services.media.safety import (
    allowed_image_mime_prefixes,
    download_source_url_to_temp,
    validate_local_source_path,
)


@dataclass(frozen=True)
class ImagePreset:
    id: str
    name: str
    description: str


IMAGE_PRESETS: dict[str, ImagePreset] = {
    "clean-bright": ImagePreset(
        id="clean-bright",
        name="Clean Bright",
        description="Slightly brighter and sharper for polished creator photos.",
    ),
    "portrait-pop": ImagePreset(
        id="portrait-pop",
        name="Portrait Pop",
        description="Warm portrait vibe with gentle contrast and color boost.",
    ),
    "cinematic-cool": ImagePreset(
        id="cinematic-cool",
        name="Cinematic Cool",
        description="Cooler tones with film-style contrast.",
    ),
    "vintage-soft": ImagePreset(
        id="vintage-soft",
        name="Vintage Soft",
        description="Muted, retro-inspired softness.",
    ),
    "mono-crisp": ImagePreset(
        id="mono-crisp",
        name="Mono Crisp",
        description="Monochrome with crisp details for dramatic posts.",
    ),
}


@dataclass
class ApplyImageFilterResult:
    success: bool
    message: str
    pillow_available: bool
    preset_id: str
    source_path_used: str | None = None
    output_path: str | None = None


def pillow_is_available() -> bool:
    return True


def list_image_presets() -> list[ImagePreset]:
    return list(IMAGE_PRESETS.values())


def _output_dir() -> Path:
    output_dir = Path(settings.image_output_dir).expanduser()
    if not output_dir.is_absolute():
        output_dir = Path.cwd() / output_dir
    output_dir.mkdir(parents=True, exist_ok=True)
    return output_dir


def _output_path(source: Path, preset_id: str, output_name: str | None) -> Path:
    if output_name:
        file_name = output_name.strip()
        if not file_name.lower().endswith((".jpg", ".jpeg", ".png", ".webp")):
            file_name = f"{file_name}.jpg"
    else:
        stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        file_name = f"{source.stem}_{preset_id}_{stamp}.jpg"
    return _output_dir() / file_name


def _load_optional_image(path: str | None, url: str | None) -> tuple[Path | None, bool]:
    if path:
        source = validate_local_source_path(path, allowed_mime_prefixes=["image/"])
        return source, False
    if url:
        source = download_source_url_to_temp(url, fallback_suffix=".jpg", allowed_mime_prefixes=["image/"])
        return source, True
    return None, False


def _apply_sepia_matrix(image: Image.Image) -> Image.Image:
    src = image.convert("RGB")
    pixels = src.load()
    width, height = src.size
    for y in range(height):
        for x in range(width):
            r, g, b = pixels[x, y]
            nr = min(255, int(0.393 * r + 0.769 * g + 0.189 * b))
            ng = min(255, int(0.349 * r + 0.686 * g + 0.168 * b))
            nb = min(255, int(0.272 * r + 0.534 * g + 0.131 * b))
            pixels[x, y] = (nr, ng, nb)
    return src


def _cartoonify(image: Image.Image) -> Image.Image:
    smooth = image.filter(ImageFilter.MedianFilter(size=5))
    flat = smooth.quantize(colors=20).convert("RGB")
    edges = image.convert("L").filter(ImageFilter.FIND_EDGES)
    edges = edges.point(lambda p: 0 if p > 40 else 255)
    edge_mask = edges.convert("1")
    outlined = flat.copy()
    draw = ImageDraw.Draw(outlined)
    width, height = outlined.size
    for y in range(height):
        for x in range(width):
            if edge_mask.getpixel((x, y)) == 0:
                draw.point((x, y), fill=(0, 0, 0))
    return outlined


def _caricature_style(image: Image.Image) -> Image.Image:
    work = ImageEnhance.Contrast(image).enhance(1.24)
    work = ImageEnhance.Color(work).enhance(1.35)
    work = ImageEnhance.Sharpness(work).enhance(1.45)
    work = ImageOps.posterize(work.convert("RGB"), bits=5)
    return work


def _apply_raw_enhance(image: Image.Image) -> Image.Image:
    # RAW-like finishing pass for highlight/shadow balance and detail pop.
    work = ImageEnhance.Contrast(image).enhance(1.08)
    work = ImageEnhance.Sharpness(work).enhance(1.2)
    work = ImageEnhance.Color(work).enhance(1.05)
    return work


def _apply_overlay(
    base: Image.Image,
    *,
    overlay_path: str | None,
    overlay_url: str | None,
    overlay_x: int,
    overlay_y: int,
    overlay_opacity: float,
) -> Image.Image:
    overlay_source, downloaded = _load_optional_image(overlay_path, overlay_url)
    if overlay_source is None:
        return base
    try:
        with Image.open(overlay_source) as overlay_img:
            overlay = overlay_img.convert("RGBA")
            if overlay.size[0] > base.size[0] or overlay.size[1] > base.size[1]:
                overlay.thumbnail((base.size[0], base.size[1]))
            opacity = max(0.0, min(1.0, overlay_opacity))
            if opacity < 1.0:
                alpha = overlay.split()[3].point(lambda a: int(a * opacity))
                overlay.putalpha(alpha)
            result = base.convert("RGBA")
            result.alpha_composite(overlay, dest=(max(0, overlay_x), max(0, overlay_y)))
            return result.convert("RGB")
    finally:
        if downloaded and overlay_source is not None:
            try:
                overlay_source.unlink(missing_ok=True)
            except Exception:
                pass


def _blend_with_mode(base_rgba: Image.Image, layer_rgba: Image.Image, blend_mode: str) -> Image.Image:
    mode = (blend_mode or "normal").strip().lower()
    if mode == "multiply":
        blended = ImageChops.multiply(base_rgba.convert("RGB"), layer_rgba.convert("RGB")).convert("RGBA")
        blended.putalpha(base_rgba.split()[3])
        return blended
    if mode == "screen":
        inv = ImageChops.multiply(
            ImageChops.invert(base_rgba.convert("RGB")),
            ImageChops.invert(layer_rgba.convert("RGB")),
        )
        blended = ImageChops.invert(inv).convert("RGBA")
        blended.putalpha(base_rgba.split()[3])
        return blended
    return Image.alpha_composite(base_rgba, layer_rgba)


def _apply_layers(base: Image.Image, *, layers: list[dict[str, object]]) -> Image.Image:
    if not layers:
        return base
    output = base.convert("RGBA")
    for layer in layers:
        layer_source, layer_downloaded = _load_optional_image(
            str(layer.get("layer_path") or "").strip() or None,
            str(layer.get("layer_url") or "").strip() or None,
        )
        if layer_source is None:
            continue
        mask_source, mask_downloaded = _load_optional_image(
            str(layer.get("mask_path") or "").strip() or None,
            str(layer.get("mask_url") or "").strip() or None,
        )
        try:
            with Image.open(layer_source) as layer_img:
                layer_rgba = layer_img.convert("RGBA")
                opacity = max(0.0, min(1.0, float(layer.get("opacity") or 1.0)))
                alpha = layer_rgba.split()[3].point(lambda a: int(a * opacity))
                layer_rgba.putalpha(alpha)

                if mask_source is not None:
                    with Image.open(mask_source) as mask_img:
                        mask = mask_img.convert("L").resize(layer_rgba.size)
                        mask = mask.point(lambda a: int(a * opacity))
                        layer_rgba.putalpha(mask)

                x = max(0, int(layer.get("x") or 0))
                y = max(0, int(layer.get("y") or 0))
                canvas = Image.new("RGBA", output.size, (0, 0, 0, 0))
                canvas.alpha_composite(layer_rgba, dest=(x, y))
                output = _blend_with_mode(output, canvas, str(layer.get("blend_mode") or "normal"))
        finally:
            if layer_downloaded:
                try:
                    layer_source.unlink(missing_ok=True)
                except Exception:
                    pass
            if mask_downloaded and mask_source is not None:
                try:
                    mask_source.unlink(missing_ok=True)
                except Exception:
                    pass
    return output.convert("RGB")


def _draw_text(
    image: Image.Image,
    *,
    text_overlay: str | None,
    text_x: int,
    text_y: int,
    text_size: int,
    text_color: str,
) -> Image.Image:
    if not text_overlay:
        return image
    draw = ImageDraw.Draw(image)
    try:
        font_path = Path("C:/Windows/Fonts/arial.ttf")
        if font_path.exists():
            font = ImageFont.truetype(str(font_path), size=max(12, text_size))
        else:
            font = ImageFont.load_default()
    except Exception:
        font = ImageFont.load_default()
    draw.text((max(0, text_x), max(0, text_y)), text_overlay, fill=text_color or "#FFFFFF", font=font)
    return image


def _apply_selective_adjustment(
    image: Image.Image,
    *,
    x: int | None,
    y: int | None,
    width: int | None,
    height: int | None,
    brightness: float,
    contrast: float,
    saturation: float,
) -> Image.Image:
    if x is None or y is None or width is None or height is None:
        return image
    if width <= 0 or height <= 0:
        return image
    left = max(0, x)
    top = max(0, y)
    right = min(image.width, left + width)
    bottom = min(image.height, top + height)
    if right <= left or bottom <= top:
        return image
    crop = image.crop((left, top, right, bottom))
    crop = ImageEnhance.Brightness(crop).enhance(max(0.0, brightness))
    crop = ImageEnhance.Contrast(crop).enhance(max(0.0, contrast))
    crop = ImageEnhance.Color(crop).enhance(max(0.0, saturation))
    output = image.copy()
    output.paste(crop, (left, top))
    return output


def _apply_erase_regions(image: Image.Image, *, regions: list[dict[str, int]]) -> Image.Image:
    if not regions:
        return image
    output = image.copy()
    blurred = output.filter(ImageFilter.GaussianBlur(radius=10))
    for region in regions:
        x = max(0, int(region.get("x", 0)))
        y = max(0, int(region.get("y", 0)))
        width = max(0, int(region.get("width", 0)))
        height = max(0, int(region.get("height", 0)))
        if width <= 0 or height <= 0:
            continue
        box = (x, y, min(output.width, x + width), min(output.height, y + height))
        if box[2] <= box[0] or box[3] <= box[1]:
            continue
        patch = blurred.crop(box)
        output.paste(patch, box[:2])
    return output


def _parse_hex_color(hex_color: str) -> tuple[int, int, int] | None:
    value = (hex_color or "").strip().lstrip("#")
    if len(value) == 3:
        value = "".join(ch * 2 for ch in value)
    if len(value) != 6:
        return None
    try:
        return int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16)
    except ValueError:
        return None


def _apply_background_key(image: Image.Image, *, key_color: str | None, tolerance: int) -> tuple[Image.Image, bool]:
    if not key_color:
        return image, False
    color = _parse_hex_color(key_color)
    if color is None:
        return image, False
    tol = max(0, min(255, int(tolerance)))
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    removed = False
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if abs(r - color[0]) <= tol and abs(g - color[1]) <= tol and abs(b - color[2]) <= tol:
                pixels[x, y] = (r, g, b, 0)
                removed = True
    return rgba, removed


def _apply_preset(image: Image.Image, preset_id: str) -> Image.Image:
    if preset_id == "clean-bright":
        image = ImageEnhance.Brightness(image).enhance(1.06)
        image = ImageEnhance.Contrast(image).enhance(1.08)
        image = ImageEnhance.Sharpness(image).enhance(1.2)
        return image
    if preset_id == "portrait-pop":
        image = ImageEnhance.Color(image).enhance(1.13)
        image = ImageEnhance.Brightness(image).enhance(1.04)
        image = ImageEnhance.Contrast(image).enhance(1.06)
        return image
    if preset_id == "cinematic-cool":
        image = ImageEnhance.Contrast(image).enhance(1.1)
        image = ImageEnhance.Color(image).enhance(0.96)
        r, g, b = image.split() if image.mode == "RGB" else image.convert("RGB").split()
        image = Image.merge("RGB", (r.point(lambda v: max(0, v - 8)), g, b.point(lambda v: min(255, v + 10))))
        return image
    if preset_id == "vintage-soft":
        image = ImageEnhance.Color(image).enhance(0.86)
        image = ImageEnhance.Contrast(image).enhance(0.94)
        image = ImageEnhance.Brightness(image).enhance(1.03)
        image = image.filter(ImageFilter.GaussianBlur(radius=0.4))
        return image
    if preset_id == "mono-crisp":
        image = ImageOps.grayscale(image)
        image = ImageEnhance.Contrast(image).enhance(1.14)
        image = ImageEnhance.Sharpness(image).enhance(1.3)
        return image.convert("RGB")
    return image


def apply_image_filter(
    *,
    preset_id: str,
    source_path: str | None,
    source_url: str | None,
    output_name: str | None,
    sepia: bool = False,
    black_white: bool = False,
    cartoonify: bool = False,
    caricature: bool = False,
    saturation_factor: float = 1.0,
    brightness_factor: float = 1.0,
    contrast_factor: float = 1.0,
    overlay_path: str | None = None,
    overlay_url: str | None = None,
    overlay_x: int = 12,
    overlay_y: int = 12,
    overlay_opacity: float = 0.6,
    text_overlay: str | None = None,
    text_x: int = 16,
    text_y: int = 16,
    text_size: int = 28,
    text_color: str = "#FFFFFF",
    raw_enhance: bool = False,
    selective_x: int | None = None,
    selective_y: int | None = None,
    selective_width: int | None = None,
    selective_height: int | None = None,
    selective_brightness_factor: float = 1.0,
    selective_contrast_factor: float = 1.0,
    selective_saturation_factor: float = 1.0,
    erase_regions: list[dict[str, int]] | None = None,
    layers: list[dict[str, object]] | None = None,
    background_key_color: str | None = None,
    background_key_tolerance: int = 24,
) -> ApplyImageFilterResult:
    preset = IMAGE_PRESETS.get(preset_id)
    if preset is None:
        return ApplyImageFilterResult(
            success=False,
            message=f"Unknown preset_id '{preset_id}'",
            pillow_available=pillow_is_available(),
            preset_id=preset_id,
        )
    if not source_path and not source_url:
        return ApplyImageFilterResult(
            success=False,
            message="Provide either source_path or source_url",
            pillow_available=pillow_is_available(),
            preset_id=preset_id,
        )

    working_source: Path | None = None
    downloaded_source = False
    try:
        if source_path:
            working_source = validate_local_source_path(
                source_path,
                allowed_mime_prefixes=allowed_image_mime_prefixes(),
            )
        else:
            assert source_url is not None
            working_source = download_source_url_to_temp(
                source_url,
                fallback_suffix=".jpg",
                allowed_mime_prefixes=allowed_image_mime_prefixes(),
            )
            downloaded_source = True

        output = _output_path(working_source, preset_id, output_name)
        with Image.open(working_source) as img:
            working_img = img.convert("RGB")
            filtered = _apply_preset(working_img, preset_id)
            filtered = ImageEnhance.Color(filtered).enhance(max(0.0, saturation_factor))
            filtered = ImageEnhance.Brightness(filtered).enhance(max(0.0, brightness_factor))
            filtered = ImageEnhance.Contrast(filtered).enhance(max(0.0, contrast_factor))
            if raw_enhance:
                filtered = _apply_raw_enhance(filtered)
            if sepia:
                filtered = _apply_sepia_matrix(filtered)
            if black_white:
                filtered = ImageOps.grayscale(filtered).convert("RGB")
            if cartoonify:
                filtered = _cartoonify(filtered)
            if caricature:
                filtered = _caricature_style(filtered)
            filtered = _apply_selective_adjustment(
                filtered,
                x=selective_x,
                y=selective_y,
                width=selective_width,
                height=selective_height,
                brightness=selective_brightness_factor,
                contrast=selective_contrast_factor,
                saturation=selective_saturation_factor,
            )
            filtered = _apply_erase_regions(filtered, regions=erase_regions or [])
            filtered = _apply_layers(filtered, layers=layers or [])
            filtered = _apply_overlay(
                filtered,
                overlay_path=overlay_path,
                overlay_url=overlay_url,
                overlay_x=overlay_x,
                overlay_y=overlay_y,
                overlay_opacity=overlay_opacity,
            )
            filtered = _draw_text(
                filtered,
                text_overlay=text_overlay,
                text_x=text_x,
                text_y=text_y,
                text_size=text_size,
                text_color=text_color,
            )
            filtered_with_alpha, removed_bg = _apply_background_key(
                filtered,
                key_color=background_key_color,
                tolerance=background_key_tolerance,
            )
            if removed_bg and output.suffix.lower() not in {".png", ".webp"}:
                output = output.with_suffix(".png")

            save_format = "JPEG"
            final_image: Image.Image = filtered_with_alpha if removed_bg else filtered
            if output.suffix.lower() == ".png":
                save_format = "PNG"
            elif output.suffix.lower() == ".webp":
                save_format = "WEBP"
            if save_format == "JPEG" and final_image.mode == "RGBA":
                final_image = final_image.convert("RGB")
            final_image.save(output, format=save_format, quality=95)

        return ApplyImageFilterResult(
            success=True,
            message=f"Applied image preset '{preset_id}' successfully",
            pillow_available=True,
            preset_id=preset_id,
            source_path_used=str(working_source),
            output_path=str(output),
        )
    except ValueError as exc:
        return ApplyImageFilterResult(
            success=False,
            message=str(exc),
            pillow_available=pillow_is_available(),
            preset_id=preset_id,
            source_path_used=str(working_source) if working_source else None,
        )
    except Exception as exc:
        return ApplyImageFilterResult(
            success=False,
            message=f"Image filter processing failed: {exc}",
            pillow_available=pillow_is_available(),
            preset_id=preset_id,
            source_path_used=str(working_source) if working_source else None,
        )
    finally:
        if downloaded_source and working_source is not None:
            try:
                working_source.unlink(missing_ok=True)
            except Exception:
                pass
