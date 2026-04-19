from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
import shutil
import subprocess

from app.core.config import settings
from app.services.media.safety import (
    allowed_video_mime_prefixes,
    download_source_url_to_temp,
    validate_local_source_path,
)


@dataclass(frozen=True)
class FilterPreset:
    id: str
    name: str
    description: str
    ffmpeg_filter: str


FILTER_PRESETS: dict[str, FilterPreset] = {
    "clean-pop": FilterPreset(
        id="clean-pop",
        name="Clean Pop",
        description="Crisp, bright, saturated look used for short-form creator clips.",
        ffmpeg_filter="eq=contrast=1.08:saturation=1.16:brightness=0.02,unsharp=5:5:0.6:5:5:0.0",
    ),
    "cinematic-teal-orange": FilterPreset(
        id="cinematic-teal-orange",
        name="Cinematic Teal/Orange",
        description="Film-inspired split tones with boosted contrast.",
        ffmpeg_filter="eq=contrast=1.1:saturation=1.05,colorbalance=rs=0.04:gs=0.01:bs=-0.05:rm=0.02:gm=0.00:bm=0.03",
    ),
    "warm-lifestyle": FilterPreset(
        id="warm-lifestyle",
        name="Warm Lifestyle",
        description="Warm social aesthetic for people, food, and lifestyle content.",
        ffmpeg_filter="eq=contrast=1.04:saturation=1.12:brightness=0.03,colorbalance=rs=0.05:gs=0.01:bs=-0.03",
    ),
    "cool-crisp": FilterPreset(
        id="cool-crisp",
        name="Cool Crisp",
        description="Cooler highlights and clean edges for tech/product shots.",
        ffmpeg_filter="eq=contrast=1.08:saturation=1.04,colorbalance=rs=-0.03:gs=0.00:bs=0.05",
    ),
    "mono-fade": FilterPreset(
        id="mono-fade",
        name="Mono Fade",
        description="Low-drama monochrome with subtle fade.",
        ffmpeg_filter="hue=s=0,eq=contrast=1.02:brightness=0.04",
    ),
    "vintage-soft": FilterPreset(
        id="vintage-soft",
        name="Vintage Soft",
        description="Softer contrast and muted tones for retro-style edits.",
        ffmpeg_filter="eq=contrast=0.95:saturation=0.9:brightness=0.03,colorbalance=rs=0.04:gs=0.01:bs=-0.02",
    ),
}

_ALLOWED_CURVES_PRESETS = {
    "color_negative",
    "cross_process",
    "darker",
    "increase_contrast",
    "linear_contrast",
    "medium_contrast",
    "negative",
    "strong_contrast",
    "vintage",
}


@dataclass
class ApplyVideoFilterResult:
    success: bool
    message: str
    ffmpeg_available: bool
    preset_id: str
    source_path_used: str | None = None
    output_path: str | None = None


def ffmpeg_binary() -> str:
    return (settings.ffmpeg_binary or "ffmpeg").strip() or "ffmpeg"


def ffmpeg_is_available() -> bool:
    binary = ffmpeg_binary()
    binary_path = Path(binary)
    if binary_path.is_absolute():
        return binary_path.exists() and binary_path.is_file()
    return shutil.which(binary) is not None


def list_presets() -> list[FilterPreset]:
    return list(FILTER_PRESETS.values())


def _build_output_path(source_path: Path, preset_id: str, output_name: str | None) -> Path:
    output_dir = Path(settings.media_output_dir).expanduser()
    if not output_dir.is_absolute():
        output_dir = Path.cwd() / output_dir
    output_dir.mkdir(parents=True, exist_ok=True)

    if output_name:
        output_file_name = output_name.strip()
        if not output_file_name.lower().endswith(".mp4"):
            output_file_name = f"{output_file_name}.mp4"
    else:
        stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        output_file_name = f"{source_path.stem}_{preset_id}_{stamp}.mp4"
    return output_dir / output_file_name


def _resolve_local_or_url(
    path: str | None,
    url: str | None,
    fallback_suffix: str,
    allowed_mime_prefixes: list[str],
) -> tuple[Path | None, bool]:
    if path:
        source = validate_local_source_path(path, allowed_mime_prefixes=allowed_mime_prefixes)
        return source, False
    if url:
        source = download_source_url_to_temp(
            url,
            fallback_suffix=fallback_suffix,
            allowed_mime_prefixes=allowed_mime_prefixes,
        )
        return source, True
    return None, False


def _escape_drawtext_text(text: str) -> str:
    escaped = text.replace("\\", "\\\\")
    escaped = escaped.replace(":", "\\:")
    escaped = escaped.replace("'", "\\'")
    return escaped


def _escape_filter_path(path: Path) -> str:
    value = path.as_posix()
    value = value.replace("\\", "\\\\")
    value = value.replace(":", "\\:")
    value = value.replace("'", "\\'")
    return value


def _atempo_chain(speed_factor: float) -> list[str]:
    if abs(speed_factor - 1.0) < 0.001:
        return []
    if speed_factor <= 0:
        return []
    target = speed_factor
    filters: list[str] = []
    while target > 2.0:
        filters.append("atempo=2.0")
        target /= 2.0
    while target < 0.5:
        filters.append("atempo=0.5")
        target *= 2.0
    filters.append(f"atempo={target:.6f}")
    return filters


def apply_video_filter(
    *,
    preset_id: str,
    source_path: str | None,
    source_url: str | None,
    output_name: str | None,
    sepia: bool = False,
    black_white: bool = False,
    saturation_factor: float = 1.0,
    brightness_delta: float = 0.0,
    overlay_path: str | None = None,
    overlay_url: str | None = None,
    overlay_x: int = 12,
    overlay_y: int = 12,
    overlay_opacity: float = 0.6,
    text_overlay: str | None = None,
    text_x: int = 16,
    text_y: int = 16,
    text_size: int = 28,
    text_color: str = "white",
    subtitle_path: str | None = None,
    subtitle_url: str | None = None,
    burn_subtitles: bool = False,
    speed_factor: float = 1.0,
    stabilize: bool = False,
    curves_preset: str | None = None,
    lut3d_path: str | None = None,
    lut3d_url: str | None = None,
    sound_path: str | None = None,
    sound_url: str | None = None,
    sound_volume: float = 1.0,
    audio_denoise: bool = False,
    audio_eq_low_gain: float = 0.0,
    audio_eq_mid_gain: float = 0.0,
    audio_eq_high_gain: float = 0.0,
    audio_compressor: bool = False,
    audio_gate: bool = False,
    audio_ducking: bool = False,
    loudness_normalization: bool = False,
    audio_limiter: bool = False,
) -> ApplyVideoFilterResult:
    preset = FILTER_PRESETS.get(preset_id)
    if preset is None:
        return ApplyVideoFilterResult(
            success=False,
            message=f"Unknown preset_id '{preset_id}'",
            ffmpeg_available=ffmpeg_is_available(),
            preset_id=preset_id,
        )

    if not ffmpeg_is_available():
        return ApplyVideoFilterResult(
            success=False,
            message=f"'{ffmpeg_binary()}' was not found. Install FFmpeg and configure FFMPEG_BINARY if needed.",
            ffmpeg_available=False,
            preset_id=preset_id,
        )

    if not source_path and not source_url:
        return ApplyVideoFilterResult(
            success=False,
            message="Provide either source_path or source_url",
            ffmpeg_available=True,
            preset_id=preset_id,
        )

    working_source: Path | None = None
    downloaded_source = False
    working_overlay: Path | None = None
    downloaded_overlay = False
    working_sound: Path | None = None
    downloaded_sound = False
    working_subtitle: Path | None = None
    downloaded_subtitle = False
    working_lut: Path | None = None
    downloaded_lut = False
    try:
        if source_path:
            working_source = validate_local_source_path(
                source_path,
                allowed_mime_prefixes=allowed_video_mime_prefixes(),
            )
        else:
            assert source_url is not None
            working_source = download_source_url_to_temp(
                source_url,
                fallback_suffix=".mp4",
                allowed_mime_prefixes=allowed_video_mime_prefixes(),
            )
            downloaded_source = True

        if overlay_path or overlay_url:
            working_overlay, downloaded_overlay = _resolve_local_or_url(
                overlay_path,
                overlay_url,
                ".png",
                allowed_mime_prefixes=["image/"],
            )
        if sound_path or sound_url:
            working_sound, downloaded_sound = _resolve_local_or_url(
                sound_path,
                sound_url,
                ".mp3",
                allowed_mime_prefixes=["audio/"],
            )
        if subtitle_path or subtitle_url:
            working_subtitle, downloaded_subtitle = _resolve_local_or_url(
                subtitle_path,
                subtitle_url,
                ".srt",
                allowed_mime_prefixes=["text/", "application/"],
            )
        if lut3d_path or lut3d_url:
            working_lut, downloaded_lut = _resolve_local_or_url(
                lut3d_path,
                lut3d_url,
                ".cube",
                allowed_mime_prefixes=["text/", "application/"],
            )

        output_path = _build_output_path(working_source, preset_id, output_name)

        video_filters = [preset.ffmpeg_filter]
        if sepia:
            video_filters.append("colorchannelmixer=.393:.769:.189:.349:.686:.168:.272:.534:.131")
        if black_white:
            video_filters.append("hue=s=0")
        if abs(saturation_factor - 1.0) > 0.001 or abs(brightness_delta) > 0.001:
            video_filters.append(
                f"eq=saturation={max(0.0, saturation_factor):.3f}:brightness={max(-1.0, min(1.0, brightness_delta)):.3f}"
            )
        if stabilize:
            video_filters.append("deshake")
        if curves_preset:
            preset_name = curves_preset.strip().lower()
            if preset_name in _ALLOWED_CURVES_PRESETS:
                video_filters.append(f"curves=preset={preset_name}")
        if working_lut is not None:
            video_filters.append(f"lut3d=file='{_escape_filter_path(working_lut)}'")
        if burn_subtitles and working_subtitle is not None:
            video_filters.append(f"subtitles='{_escape_filter_path(working_subtitle)}'")
        if text_overlay:
            font_file = Path("C:/Windows/Fonts/arial.ttf")
            drawtext = (
                f"drawtext=text='{_escape_drawtext_text(text_overlay)}':"
                f"x={max(0, text_x)}:y={max(0, text_y)}:"
                f"fontsize={max(10, text_size)}:fontcolor={text_color or 'white'}"
            )
            if font_file.exists():
                drawtext += f":fontfile='{font_file.as_posix()}'"
            video_filters.append(drawtext)
        if abs(speed_factor - 1.0) > 0.001:
            clamped_speed = max(0.10, min(4.0, speed_factor))
            video_filters.append(f"setpts=PTS/{clamped_speed:.6f}")

        audio_filters: list[str] = []
        if audio_denoise:
            audio_filters.append("afftdn")
        if abs(audio_eq_low_gain) > 0.001:
            audio_filters.append(f"equalizer=f=120:t=h:w=200:g={audio_eq_low_gain:.2f}")
        if abs(audio_eq_mid_gain) > 0.001:
            audio_filters.append(f"equalizer=f=1000:t=h:w=800:g={audio_eq_mid_gain:.2f}")
        if abs(audio_eq_high_gain) > 0.001:
            audio_filters.append(f"equalizer=f=5000:t=h:w=2500:g={audio_eq_high_gain:.2f}")
        if audio_compressor:
            audio_filters.append("acompressor=threshold=-18dB:ratio=3:attack=20:release=250:makeup=2")
        if audio_gate:
            audio_filters.append("agate=threshold=0.03:ratio=2:attack=20:release=200")
        if loudness_normalization:
            audio_filters.append("loudnorm=I=-14:TP=-1.5:LRA=11")
        if audio_limiter:
            audio_filters.append("alimiter=limit=0.95:level=disabled")
        audio_filters.extend(_atempo_chain(max(0.10, min(4.0, speed_factor))))

        cmd = [ffmpeg_binary(), "-y", "-hide_banner", "-i", str(working_source)]

        has_overlay = working_overlay is not None
        has_sound = working_sound is not None

        if has_overlay:
            assert working_overlay is not None
            if working_overlay.suffix.lower() in {".png", ".jpg", ".jpeg", ".webp", ".bmp"}:
                cmd.extend(["-loop", "1"])
            cmd.extend(["-i", str(working_overlay)])

        if has_sound:
            assert working_sound is not None
            cmd.extend(["-i", str(working_sound)])

        filter_complex_blocks: list[str] = []
        if has_overlay:
            base_chain = ",".join(video_filters) if video_filters else "null"
            opacity = max(0.0, min(1.0, overlay_opacity))
            filter_complex_blocks.append(
                f"[0:v]{base_chain}[base];"
                f"[1:v]format=rgba,colorchannelmixer=aa={opacity:.3f}[ovr];"
                f"[base][ovr]overlay={max(0, overlay_x)}:{max(0, overlay_y)}[vout]"
            )

        use_ducking = has_sound and audio_ducking
        if use_ducking:
            sound_index = 2 if has_overlay else 1
            duck_gain = max(0.0, sound_volume)
            combined_audio_filters = list(audio_filters)
            suffix = f",{','.join(combined_audio_filters)}" if combined_audio_filters else ""
            filter_complex_blocks.append(
                f"[{sound_index}:a]volume={duck_gain:.3f}[bg];"
                f"[bg][0:a]sidechaincompress=threshold=0.03:ratio=8:attack=20:release=250[ducked];"
                f"[0:a][ducked]amix=inputs=2:duration=first:dropout_transition=2{suffix}[aout]"
            )

        if filter_complex_blocks:
            cmd.extend(["-filter_complex", ";".join(filter_complex_blocks)])

        if has_overlay:
            cmd.extend(["-map", "[vout]"])
        else:
            if video_filters:
                cmd.extend(["-vf", ",".join(video_filters)])
            cmd.extend(["-map", "0:v:0"])

        if use_ducking:
            cmd.extend(["-map", "[aout]"])
        elif has_sound:
            sound_index = 2 if has_overlay else 1
            cmd.extend(["-map", f"{sound_index}:a:0"])
            local_audio_filters = [f"volume={max(0.0, sound_volume):.3f}"] + audio_filters
            if local_audio_filters:
                cmd.extend(["-filter:a", ",".join(local_audio_filters)])
            cmd.extend(["-shortest"])
        else:
            cmd.extend(["-map", "0:a?"])
            if audio_filters:
                cmd.extend(["-filter:a", ",".join(audio_filters)])

        cmd.extend(
            [
                "-c:v",
                "libx264",
                "-preset",
                "medium",
                "-crf",
                "20",
                "-movflags",
                "+faststart",
                "-c:a",
                "aac",
                str(output_path),
            ]
        )
        completed = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            check=False,
            timeout=max(1.0, settings.ffmpeg_timeout_seconds),
        )
        if completed.returncode != 0:
            stderr_tail = (completed.stderr or "").strip()[-500:]
            return ApplyVideoFilterResult(
                success=False,
                message=f"FFmpeg failed: {stderr_tail or 'unknown ffmpeg error'}",
                ffmpeg_available=True,
                preset_id=preset_id,
                source_path_used=str(working_source),
            )

        return ApplyVideoFilterResult(
            success=True,
            message=f"Applied preset '{preset_id}' successfully",
            ffmpeg_available=True,
            preset_id=preset_id,
            source_path_used=str(working_source),
            output_path=str(output_path),
        )
    except subprocess.TimeoutExpired:
        return ApplyVideoFilterResult(
            success=False,
            message="FFmpeg processing timed out.",
            ffmpeg_available=ffmpeg_is_available(),
            preset_id=preset_id,
            source_path_used=str(working_source) if working_source else None,
        )
    except ValueError as exc:
        return ApplyVideoFilterResult(
            success=False,
            message=str(exc),
            ffmpeg_available=ffmpeg_is_available(),
            preset_id=preset_id,
            source_path_used=str(working_source) if working_source else None,
        )
    except Exception as exc:
        return ApplyVideoFilterResult(
            success=False,
            message=f"Video filter processing failed: {exc}",
            ffmpeg_available=ffmpeg_is_available(),
            preset_id=preset_id,
            source_path_used=str(working_source) if working_source else None,
        )
    finally:
        if downloaded_source and working_source is not None:
            try:
                working_source.unlink(missing_ok=True)
            except Exception:
                pass
        if downloaded_overlay and working_overlay is not None:
            try:
                working_overlay.unlink(missing_ok=True)
            except Exception:
                pass
        if downloaded_sound and working_sound is not None:
            try:
                working_sound.unlink(missing_ok=True)
            except Exception:
                pass
        if downloaded_subtitle and working_subtitle is not None:
            try:
                working_subtitle.unlink(missing_ok=True)
            except Exception:
                pass
        if downloaded_lut and working_lut is not None:
            try:
                working_lut.unlink(missing_ok=True)
            except Exception:
                pass
