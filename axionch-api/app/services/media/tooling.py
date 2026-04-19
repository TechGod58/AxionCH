from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
import json
import shutil
import subprocess
import tempfile
import threading
import uuid

from app.core.config import settings
from app.services.media.safety import (
    allowed_audio_mime_prefixes,
    allowed_image_mime_prefixes,
    allowed_video_mime_prefixes,
    download_source_url_to_temp,
    validate_local_source_path,
)
from app.services.media.video_filters import ffmpeg_binary, ffmpeg_is_available


@dataclass(frozen=True)
class CreatorTemplateModel:
    id: str
    name: str
    media_type: str
    platforms: list[str]
    width: int
    height: int
    fps: int | None
    description: str


CREATOR_TEMPLATES: list[CreatorTemplateModel] = [
    CreatorTemplateModel(
        id="reels-1080x1920",
        name="Reels Vertical 9:16",
        media_type="video",
        platforms=["instagram", "facebook"],
        width=1080,
        height=1920,
        fps=30,
        description="Vertical full-screen format for Reels.",
    ),
    CreatorTemplateModel(
        id="shorts-1080x1920",
        name="YouTube Shorts 9:16",
        media_type="video",
        platforms=["youtube"],
        width=1080,
        height=1920,
        fps=30,
        description="Vertical short-form format for YouTube Shorts.",
    ),
    CreatorTemplateModel(
        id="tiktok-1080x1920",
        name="TikTok Vertical 9:16",
        media_type="video",
        platforms=["tiktok"],
        width=1080,
        height=1920,
        fps=30,
        description="Vertical format optimized for TikTok uploads.",
    ),
    CreatorTemplateModel(
        id="feed-1080x1350",
        name="Instagram Feed 4:5",
        media_type="image",
        platforms=["instagram"],
        width=1080,
        height=1350,
        fps=None,
        description="Portrait feed image format for Instagram.",
    ),
    CreatorTemplateModel(
        id="landscape-1920x1080",
        name="Landscape 16:9",
        media_type="video",
        platforms=["youtube", "x", "linkedin"],
        width=1920,
        height=1080,
        fps=30,
        description="Standard landscape format for long-form and feed video.",
    ),
]


@dataclass(frozen=True)
class ExportPresetModel:
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


EXPORT_PRESETS: dict[str, ExportPresetModel] = {
    "reels-h264-1080x1920": ExportPresetModel(
        id="reels-h264-1080x1920",
        name="Instagram Reels H.264",
        platform="instagram",
        media_type="video",
        width=1080,
        height=1920,
        fps=30,
        video_bitrate="8M",
        audio_bitrate="192k",
        container="mp4",
    ),
    "shorts-h264-1080x1920": ExportPresetModel(
        id="shorts-h264-1080x1920",
        name="YouTube Shorts H.264",
        platform="youtube",
        media_type="video",
        width=1080,
        height=1920,
        fps=30,
        video_bitrate="10M",
        audio_bitrate="192k",
        container="mp4",
    ),
    "tiktok-h264-1080x1920": ExportPresetModel(
        id="tiktok-h264-1080x1920",
        name="TikTok H.264",
        platform="tiktok",
        media_type="video",
        width=1080,
        height=1920,
        fps=30,
        video_bitrate="8M",
        audio_bitrate="192k",
        container="mp4",
    ),
    "linkedin-h264-1920x1080": ExportPresetModel(
        id="linkedin-h264-1920x1080",
        name="LinkedIn Landscape H.264",
        platform="linkedin",
        media_type="video",
        width=1920,
        height=1080,
        fps=30,
        video_bitrate="8M",
        audio_bitrate="192k",
        container="mp4",
    ),
}


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _iso_now() -> str:
    return _utc_now().isoformat()


def list_creator_templates(platform: str | None = None, media_type: str | None = None) -> list[CreatorTemplateModel]:
    output = CREATOR_TEMPLATES
    if platform:
        target = platform.strip().lower()
        output = [item for item in output if target in [p.lower() for p in item.platforms]]
    if media_type:
        target_type = media_type.strip().lower()
        output = [item for item in output if item.media_type.lower() == target_type]
    return output


def list_export_presets() -> list[ExportPresetModel]:
    return list(EXPORT_PRESETS.values())


def _output_root() -> Path:
    root = Path(settings.media_output_dir).expanduser()
    if not root.is_absolute():
        root = Path.cwd() / root
    root.mkdir(parents=True, exist_ok=True)
    return root


def _export_output_dir() -> Path:
    target = _output_root() / "exports"
    target.mkdir(parents=True, exist_ok=True)
    return target


def _timeline_output_dir() -> Path:
    target = _output_root() / "timelines"
    target.mkdir(parents=True, exist_ok=True)
    return target


def _captions_output_dir() -> Path:
    target = _output_root() / "captions"
    target.mkdir(parents=True, exist_ok=True)
    return target


def _assets_db_path() -> Path:
    target = _output_root() / "assets"
    target.mkdir(parents=True, exist_ok=True)
    return target / "library.json"


def _sanitize_output_prefix(prefix: str | None) -> str:
    if not prefix:
        return "export"
    cleaned = "".join(ch for ch in prefix if ch.isalnum() or ch in {"-", "_"})
    return cleaned or "export"


def _sanitize_name(name: str | None, fallback: str) -> str:
    raw = (name or "").strip()
    if not raw:
        return fallback
    cleaned = "".join(ch for ch in raw if ch.isalnum() or ch in {"-", "_", "."})
    return cleaned or fallback


_ENCODER_CACHE_LOCK = threading.Lock()
_ENCODER_CACHE: dict[str, bool] = {}


def _ffmpeg_encoder_available(encoder_name: str) -> bool:
    with _ENCODER_CACHE_LOCK:
        if encoder_name in _ENCODER_CACHE:
            return _ENCODER_CACHE[encoder_name]
    if not ffmpeg_is_available():
        with _ENCODER_CACHE_LOCK:
            _ENCODER_CACHE[encoder_name] = False
        return False
    cmd = [ffmpeg_binary(), "-hide_banner", "-encoders"]
    completed = subprocess.run(cmd, capture_output=True, text=True, check=False, timeout=30)
    available = completed.returncode == 0 and encoder_name in (completed.stdout or "")
    with _ENCODER_CACHE_LOCK:
        _ENCODER_CACHE[encoder_name] = available
    return available


def _select_export_encoder(hardware_acceleration: bool) -> tuple[str, list[str], str]:
    if hardware_acceleration:
        candidates: list[tuple[str, list[str], str]] = [
            ("h264_nvenc", ["-preset", "p4", "-rc", "vbr", "-cq", "23"], "NVIDIA NVENC"),
            ("h264_qsv", ["-preset", "faster"], "Intel Quick Sync"),
            ("h264_amf", ["-quality", "speed"], "AMD AMF"),
        ]
        for encoder, args, label in candidates:
            if _ffmpeg_encoder_available(encoder):
                return encoder, args, label
        return "libx264", ["-preset", "veryfast"], "CPU fallback (no hardware encoder available)"
    return "libx264", ["-preset", "medium"], "CPU libx264"


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


@dataclass
class ExportJobItemState:
    index: int
    source: str
    output_path: str | None = None
    status: str = "queued"
    message: str | None = None


@dataclass
class ExportJobState:
    job_id: str
    preset_id: str
    status: str
    created_at: str
    completed_at: str | None
    queued_items: int
    completed_items: int
    failed_items: int
    items: list[ExportJobItemState] = field(default_factory=list)


class ExportQueueService:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._jobs: dict[str, ExportJobState] = {}
        self._executor = ThreadPoolExecutor(max_workers=2, thread_name_prefix="axionch-export")

    def queue_job(
        self,
        *,
        preset_id: str,
        source_paths: list[str],
        source_urls: list[str],
        output_prefix: str | None,
        hardware_acceleration: bool,
    ) -> ExportJobState:
        preset = EXPORT_PRESETS.get(preset_id)
        if preset is None:
            raise ValueError(f"Unknown preset_id '{preset_id}'")
        sources = [path.strip() for path in source_paths if path and path.strip()]
        urls = [url.strip() for url in source_urls if url and url.strip()]
        if not sources and not urls:
            raise ValueError("Provide at least one source_path or source_url")

        source_descriptors: list[tuple[str, str]] = []
        source_descriptors.extend(("path", value) for value in sources)
        source_descriptors.extend(("url", value) for value in urls)

        items = [
            ExportJobItemState(index=index, source=value)
            for index, (_, value) in enumerate(source_descriptors, start=1)
        ]
        job = ExportJobState(
            job_id=f"exp_{uuid.uuid4().hex[:12]}",
            preset_id=preset_id,
            status="queued",
            created_at=_iso_now(),
            completed_at=None,
            queued_items=len(items),
            completed_items=0,
            failed_items=0,
            items=items,
        )
        with self._lock:
            self._jobs[job.job_id] = job

        self._executor.submit(
            self._run_job,
            job.job_id,
            source_descriptors,
            preset,
            _sanitize_output_prefix(output_prefix),
            hardware_acceleration,
        )
        return self.get_job(job.job_id)

    def get_job(self, job_id: str) -> ExportJobState:
        with self._lock:
            job = self._jobs.get(job_id)
            if job is None:
                raise ValueError(f"Export job '{job_id}' was not found")
            return ExportJobState(
                job_id=job.job_id,
                preset_id=job.preset_id,
                status=job.status,
                created_at=job.created_at,
                completed_at=job.completed_at,
                queued_items=job.queued_items,
                completed_items=job.completed_items,
                failed_items=job.failed_items,
                items=[ExportJobItemState(**item.__dict__) for item in job.items],
            )

    def _set_job_status(self, job_id: str, status: str) -> None:
        with self._lock:
            job = self._jobs[job_id]
            job.status = status
            if status in {"completed", "failed", "completed_with_errors"}:
                job.completed_at = _iso_now()

    def _update_item(self, job_id: str, index: int, *, status: str, output_path: str | None = None, message: str | None = None) -> None:
        with self._lock:
            job = self._jobs[job_id]
            item = job.items[index - 1]
            item.status = status
            item.output_path = output_path
            item.message = message
            job.completed_items = sum(1 for value in job.items if value.status == "completed")
            job.failed_items = sum(1 for value in job.items if value.status == "failed")

    def _run_job(
        self,
        job_id: str,
        source_descriptors: list[tuple[str, str]],
        preset: ExportPresetModel,
        output_prefix: str,
        hardware_acceleration: bool,
    ) -> None:
        self._set_job_status(job_id, "running")
        used_temp_sources: list[Path] = []
        try:
            for index, (source_type, source_value) in enumerate(source_descriptors, start=1):
                try:
                    if source_type == "path":
                        source_path = validate_local_source_path(
                            source_value,
                            allowed_mime_prefixes=allowed_video_mime_prefixes() + allowed_image_mime_prefixes(),
                        )
                    else:
                        source_path = download_source_url_to_temp(
                            source_value,
                            fallback_suffix=".mp4",
                            allowed_mime_prefixes=allowed_video_mime_prefixes() + allowed_image_mime_prefixes(),
                        )
                        used_temp_sources.append(source_path)

                    stamp = _utc_now().strftime("%Y%m%dT%H%M%SZ")
                    output_name = f"{output_prefix}_{index:03d}_{preset.platform}_{stamp}.{preset.container}"
                    output_path = _export_output_dir() / output_name

                    encoder, encoder_args, encoder_label = _select_export_encoder(hardware_acceleration)

                    vf = (
                        f"scale={preset.width}:{preset.height}:force_original_aspect_ratio=decrease,"
                        f"pad={preset.width}:{preset.height}:(ow-iw)/2:(oh-ih)/2:black,"
                        f"fps={preset.fps}"
                    )
                    cmd = [
                        ffmpeg_binary(),
                        "-y",
                        "-hide_banner",
                        "-i",
                        str(source_path),
                        "-vf",
                        vf,
                        "-map",
                        "0:v:0",
                        "-map",
                        "0:a?",
                        "-c:v",
                        encoder,
                        *encoder_args,
                        "-b:v",
                        preset.video_bitrate,
                        "-c:a",
                        "aac",
                        "-b:a",
                        preset.audio_bitrate,
                        "-movflags",
                        "+faststart",
                        str(output_path),
                    ]
                    completed = subprocess.run(
                        cmd,
                        capture_output=True,
                        text=True,
                        check=False,
                        timeout=max(1.0, settings.ffmpeg_timeout_seconds),
                    )
                    if completed.returncode != 0:
                        stderr_tail = (completed.stderr or "").strip()[-500:]
                        self._update_item(
                            job_id,
                            index,
                            status="failed",
                            message=f"FFmpeg failed: {stderr_tail or 'unknown ffmpeg error'}",
                        )
                    else:
                        self._update_item(
                            job_id,
                            index,
                            status="completed",
                            output_path=str(output_path),
                            message=f"Export completed ({encoder_label})",
                        )
                except subprocess.TimeoutExpired:
                    self._update_item(job_id, index, status="failed", message="Export timed out")
                except Exception as exc:
                    self._update_item(job_id, index, status="failed", message=str(exc))

            summary = self.get_job(job_id)
            if summary.failed_items == 0:
                self._set_job_status(job_id, "completed")
            elif summary.completed_items == 0:
                self._set_job_status(job_id, "failed")
            else:
                self._set_job_status(job_id, "completed_with_errors")
        finally:
            for temp_file in used_temp_sources:
                try:
                    temp_file.unlink(missing_ok=True)
                except Exception:
                    pass


export_queue_service = ExportQueueService()


def _format_srt_timestamp(seconds: float) -> str:
    total_ms = max(0, int(round(seconds * 1000)))
    hours = total_ms // 3_600_000
    minutes = (total_ms % 3_600_000) // 60_000
    secs = (total_ms % 60_000) // 1000
    millis = total_ms % 1000
    return f"{hours:02d}:{minutes:02d}:{secs:02d},{millis:03d}"


def _split_caption_text(text: str, max_chars: int) -> str:
    words = text.strip().split()
    if not words:
        return ""
    lines: list[str] = []
    line = ""
    for word in words:
        candidate = f"{line} {word}".strip()
        if len(candidate) <= max_chars or not line:
            line = candidate
            continue
        lines.append(line)
        line = word
    if line:
        lines.append(line)
    return "\n".join(lines)


def _write_srt_file(cues: list[dict[str, object]], output_name: str | None) -> Path:
    stem = _sanitize_name(output_name, f"captions_{_utc_now().strftime('%Y%m%dT%H%M%SZ')}")
    if not stem.lower().endswith(".srt"):
        stem = f"{stem}.srt"
    output_path = _captions_output_dir() / stem

    lines: list[str] = []
    for index, cue in enumerate(cues, start=1):
        start = float(cue.get("start_seconds") or 0.0)
        end = float(cue.get("end_seconds") or 0.0)
        text = str(cue.get("text") or "").strip()
        if not text:
            continue
        if end <= start:
            end = start + 1.0
        lines.append(str(index))
        lines.append(f"{_format_srt_timestamp(start)} --> {_format_srt_timestamp(end)}")
        lines.append(text)
        lines.append("")

    output_path.write_text("\n".join(lines), encoding="utf-8")
    return output_path


def build_srt_from_cues(*, cues: list[dict[str, object]], output_name: str | None) -> tuple[bool, str, str | None, int]:
    if not cues:
        return False, "Provide at least one subtitle cue", None, 0
    output_path = _write_srt_file(cues, output_name)
    return True, "SRT built successfully", str(output_path), len(cues)


def transcribe_captions(
    *,
    source_path: str | None,
    source_url: str | None,
    language: str | None,
    output_name: str | None,
    max_chars_per_line: int,
) -> tuple[bool, str, str, int, str | None]:
    if not source_path and not source_url:
        return False, "Provide source_path or source_url", "none", 0, None

    downloaded = False
    working_source: Path | None = None
    try:
        if source_path:
            working_source = validate_local_source_path(
                source_path,
                allowed_mime_prefixes=allowed_video_mime_prefixes() + allowed_audio_mime_prefixes(),
            )
        else:
            assert source_url is not None
            working_source = download_source_url_to_temp(
                source_url,
                fallback_suffix=".mp4",
                allowed_mime_prefixes=allowed_video_mime_prefixes() + allowed_audio_mime_prefixes(),
            )
            downloaded = True

        cues: list[dict[str, object]] = []
        engine = "none"

        try:
            from faster_whisper import WhisperModel  # type: ignore

            model = WhisperModel("base", device="cpu", compute_type="int8")
            segments, _ = model.transcribe(str(working_source), language=(language or None), vad_filter=True)
            for segment in segments:
                text = _split_caption_text(str(getattr(segment, "text", "")), max(18, max_chars_per_line))
                if not text:
                    continue
                cues.append(
                    {
                        "start_seconds": float(getattr(segment, "start", 0.0) or 0.0),
                        "end_seconds": float(getattr(segment, "end", 0.0) or 0.0),
                        "text": text,
                    }
                )
            engine = "faster_whisper"
        except Exception:
            try:
                import whisper as openai_whisper  # type: ignore

                model = openai_whisper.load_model("base")
                result = model.transcribe(str(working_source), language=(language or None))
                for segment in result.get("segments", []):
                    text = _split_caption_text(str(segment.get("text") or ""), max(18, max_chars_per_line))
                    if not text:
                        continue
                    cues.append(
                        {
                            "start_seconds": float(segment.get("start") or 0.0),
                            "end_seconds": float(segment.get("end") or 0.0),
                            "text": text,
                        }
                    )
                engine = "openai_whisper"
            except Exception:
                return (
                    False,
                    "No transcription engine available. Install faster-whisper or openai-whisper in API runtime.",
                    "none",
                    0,
                    None,
                )

        if not cues:
            return False, "Transcription finished but no subtitle cues were produced", engine, 0, None

        output_path = _write_srt_file(cues, output_name)
        return True, "Transcription complete", engine, len(cues), str(output_path)
    except Exception as exc:
        return False, f"Transcription failed: {exc}", "none", 0, None
    finally:
        if downloaded and working_source is not None:
            try:
                working_source.unlink(missing_ok=True)
            except Exception:
                pass


def _read_assets_db() -> list[dict[str, object]]:
    db_path = _assets_db_path()
    if not db_path.exists():
        return []
    try:
        payload = json.loads(db_path.read_text(encoding="utf-8"))
        if isinstance(payload, list):
            return [item for item in payload if isinstance(item, dict)]
    except Exception:
        pass
    return []


def _write_assets_db(items: list[dict[str, object]]) -> None:
    db_path = _assets_db_path()
    db_path.write_text(json.dumps(items, indent=2, ensure_ascii=True), encoding="utf-8")


def list_assets(*, kind: str | None = None, tag: str | None = None) -> list[dict[str, object]]:
    items = _read_assets_db()
    if kind:
        target_kind = kind.strip().lower()
        items = [item for item in items if str(item.get("kind") or "").strip().lower() == target_kind]
    if tag:
        target_tag = tag.strip().lower()
        items = [
            item
            for item in items
            if target_tag in [str(value).strip().lower() for value in list(item.get("tags") or [])]
        ]
    return items


def add_asset(*, payload: dict[str, object]) -> dict[str, object]:
    items = _read_assets_db()
    now = _iso_now()
    entry = {
        "id": f"ast_{uuid.uuid4().hex[:12]}",
        "kind": str(payload.get("kind") or "").strip().lower(),
        "title": str(payload.get("title") or "").strip(),
        "source": str(payload.get("source") or "").strip(),
        "license_name": str(payload.get("license_name") or "").strip(),
        "license_url": str(payload.get("license_url") or "").strip() or None,
        "attribution_required": bool(payload.get("attribution_required") or False),
        "attribution_text": str(payload.get("attribution_text") or "").strip() or None,
        "tags": [str(value).strip().lower() for value in list(payload.get("tags") or []) if str(value).strip()],
        "local_path": str(payload.get("local_path") or "").strip() or None,
        "remote_url": str(payload.get("remote_url") or "").strip() or None,
        "created_at": now,
        "updated_at": now,
    }
    if not entry["kind"]:
        raise ValueError("Asset kind is required")
    if not entry["title"]:
        raise ValueError("Asset title is required")
    if not entry["source"]:
        raise ValueError("Asset source is required")
    if not entry["license_name"]:
        raise ValueError("Asset license_name is required")
    items.insert(0, entry)
    _write_assets_db(items)
    return entry


def delete_asset(*, asset_id: str) -> bool:
    target = asset_id.strip()
    items = _read_assets_db()
    updated = [item for item in items if str(item.get("id") or "") != target]
    if len(updated) == len(items):
        return False
    _write_assets_db(updated)
    return True


def _probe_media_duration_seconds(source_path: Path) -> float | None:
    cmd = [
        "ffprobe",
        "-v",
        "error",
        "-show_entries",
        "format=duration",
        "-of",
        "default=noprint_wrappers=1:nokey=1",
        str(source_path),
    ]
    completed = subprocess.run(cmd, capture_output=True, text=True, check=False, timeout=20)
    if completed.returncode != 0:
        return None
    value = (completed.stdout or "").strip()
    try:
        duration = float(value)
    except Exception:
        return None
    if duration <= 0:
        return None
    return duration


def _expand_split_segments(
    *,
    start_seconds: float,
    end_seconds: float | None,
    split_points: list[float],
) -> list[tuple[float, float | None]]:
    start_value = max(0.0, float(start_seconds))
    valid_splits: list[float] = []
    for point in split_points:
        value = float(point)
        if value <= start_value:
            continue
        if end_seconds is not None and value >= float(end_seconds):
            continue
        valid_splits.append(value)
    valid_splits = sorted(set(valid_splits))
    if not valid_splits:
        return [(start_value, float(end_seconds) if end_seconds is not None else None)]

    segments: list[tuple[float, float | None]] = []
    boundaries = [start_value] + valid_splits
    if end_seconds is not None:
        boundaries.append(float(end_seconds))
        for index in range(len(boundaries) - 1):
            segments.append((boundaries[index], boundaries[index + 1]))
    else:
        for index in range(len(boundaries) - 1):
            segments.append((boundaries[index], boundaries[index + 1]))
        segments.append((boundaries[-1], None))
    return segments


def _keyframe_expression(default_value: float, points: list[tuple[float, float]]) -> str:
    if not points:
        return f"{default_value:.6f}"
    sorted_points = sorted(points, key=lambda item: item[0])
    if sorted_points[0][0] > 0.0:
        sorted_points = [(0.0, default_value)] + sorted_points
    expr = f"{sorted_points[-1][1]:.6f}"
    for idx in range(len(sorted_points) - 1, 0, -1):
        t0, v0 = sorted_points[idx - 1]
        t1, v1 = sorted_points[idx]
        span = max(0.001, t1 - t0)
        seg = f"({v0:.6f}+({(v1 - v0):.6f})*(t-{t0:.6f})/{span:.6f})"
        expr = f"if(lt(t,{t1:.6f}),{seg},{expr})"
    return expr


def _build_keyframe_video_filter(
    keyframes: list[dict[str, object]] | None,
    warnings: list[str],
) -> str | None:
    if not keyframes:
        return None
    values_by_property: dict[str, list[tuple[float, float]]] = {
        "brightness": [],
        "contrast": [],
        "saturation": [],
    }
    for item in keyframes:
        property_name = str(item.get("property") or "").strip().lower()
        if property_name not in values_by_property:
            warnings.append(f"Unsupported keyframe property '{property_name}' skipped")
            continue
        try:
            at_seconds = max(0.0, float(item.get("at_seconds") or 0.0))
            value = float(item.get("value") or 0.0)
        except Exception:
            warnings.append(f"Invalid keyframe value for property '{property_name}' skipped")
            continue
        values_by_property[property_name].append((at_seconds, value))

    if not any(values_by_property.values()):
        return None

    brightness_expr = _keyframe_expression(0.0, values_by_property["brightness"])
    contrast_expr = _keyframe_expression(1.0, values_by_property["contrast"])
    saturation_expr = _keyframe_expression(1.0, values_by_property["saturation"])
    return f"eq=brightness='{brightness_expr}':contrast='{contrast_expr}':saturation='{saturation_expr}'"


def compose_timeline(
    *,
    clips: list[dict[str, object]],
    audio_tracks: list[dict[str, object]] | None,
    keyframes: list[dict[str, object]] | None,
    transition_default: str | None,
    normalize_loudness: bool,
    output_name: str | None,
    background_audio_path: str | None,
    background_audio_url: str | None,
    background_audio_volume: float,
) -> tuple[bool, str, str | None, list[str]]:
    warnings: list[str] = []
    if not ffmpeg_is_available():
        return False, f"'{ffmpeg_binary()}' was not found. Install FFmpeg and configure FFMPEG_BINARY if needed.", None, warnings
    if not clips:
        return False, "Provide at least one timeline clip", None, warnings

    expanded_clips: list[dict[str, object]] = []
    for clip in clips:
        source_path_value = str(clip.get("source_path") or "").strip()
        source_url_value = str(clip.get("source_url") or "").strip()
        if not source_path_value and not source_url_value:
            return False, "Each clip requires source_path or source_url", None, warnings
        start_seconds = max(0.0, float(clip.get("start_seconds") or 0.0))
        end_seconds_raw = clip.get("end_seconds")
        end_seconds = float(end_seconds_raw) if end_seconds_raw is not None else None
        split_points = [float(value) for value in list(clip.get("split_points") or [])]
        segments = _expand_split_segments(
            start_seconds=start_seconds,
            end_seconds=end_seconds,
            split_points=split_points,
        )
        for seg_start, seg_end in segments:
            expanded_clips.append(
                {
                    "source_path": source_path_value or None,
                    "source_url": source_url_value or None,
                    "start_seconds": seg_start,
                    "end_seconds": seg_end,
                    "speed_factor": float(clip.get("speed_factor") or 1.0),
                    "volume": float(clip.get("volume") or 1.0),
                    "transition_to_next": clip.get("transition_to_next"),
                    "transition_duration": float(clip.get("transition_duration") or 0.35),
                }
            )
    if len(expanded_clips) > len(clips):
        warnings.append(f"Expanded {len(clips)} input clip(s) into {len(expanded_clips)} timeline segment(s) using split points")

    tmp_dir = Path(tempfile.mkdtemp(prefix="axionch_timeline_"))
    downloaded_sources: list[Path] = []
    trimmed_outputs: list[Path] = []
    downloaded_audio: list[Path] = []
    concat_list_file = tmp_dir / "concat.txt"
    try:
        clip_count = len(expanded_clips)
        for index, clip in enumerate(expanded_clips, start=1):
            source_path_value = str(clip.get("source_path") or "").strip()
            source_url_value = str(clip.get("source_url") or "").strip()
            if source_path_value:
                source_path = validate_local_source_path(source_path_value, allowed_mime_prefixes=allowed_video_mime_prefixes())
            elif source_url_value:
                source_path = download_source_url_to_temp(
                    source_url_value,
                    fallback_suffix=".mp4",
                    allowed_mime_prefixes=allowed_video_mime_prefixes(),
                )
                downloaded_sources.append(source_path)
            else:
                return False, f"Clip #{index} requires source_path or source_url", None, warnings

            start_seconds = float(clip.get("start_seconds") or 0.0)
            end_seconds = clip.get("end_seconds")
            end_seconds_float = float(end_seconds) if end_seconds is not None else None
            speed_factor = float(clip.get("speed_factor") or 1.0)
            speed_factor = max(0.10, min(4.0, speed_factor))
            clip_volume = max(0.0, float(clip.get("volume") or 1.0))
            transition = str(clip.get("transition_to_next") or transition_default or "none").strip().lower()
            transition_duration = max(0.05, min(2.0, float(clip.get("transition_duration") or 0.35)))

            clip_duration: float | None = None
            if end_seconds_float is not None:
                raw_duration = end_seconds_float - start_seconds
                if raw_duration > 0.05:
                    clip_duration = raw_duration / speed_factor
            else:
                probed = _probe_media_duration_seconds(source_path)
                if probed is not None and probed > start_seconds:
                    clip_duration = (probed - start_seconds) / speed_factor

            trimmed_path = tmp_dir / f"clip_{index:03d}.mp4"
            cmd = [ffmpeg_binary(), "-y", "-hide_banner"]
            if start_seconds > 0:
                cmd.extend(["-ss", str(start_seconds)])
            cmd.extend(["-i", str(source_path)])
            if end_seconds_float is not None:
                duration = max(0.05, end_seconds_float - start_seconds)
                cmd.extend(["-t", str(duration)])

            vf: list[str] = []
            af: list[str] = []
            if abs(speed_factor - 1.0) > 0.001:
                vf.append(f"setpts=PTS/{speed_factor:.6f}")
                af.extend(_atempo_chain(speed_factor))
            if abs(clip_volume - 1.0) > 0.001:
                af.insert(0, f"volume={clip_volume:.3f}")

            if transition != "none" and clip_duration and clip_duration > (transition_duration + 0.08):
                if index > 1:
                    vf.append(f"fade=t=in:st=0:d={transition_duration:.3f}")
                    af.append(f"afade=t=in:st=0:d={transition_duration:.3f}")
                if index < clip_count:
                    fade_out_start = max(0.0, clip_duration - transition_duration)
                    vf.append(f"fade=t=out:st={fade_out_start:.3f}:d={transition_duration:.3f}")
                    af.append(f"afade=t=out:st={fade_out_start:.3f}:d={transition_duration:.3f}")

            if vf:
                cmd.extend(["-vf", ",".join(vf)])
            if af:
                cmd.extend(["-af", ",".join(af)])

            cmd.extend(
                [
                    "-c:v",
                    "libx264",
                    "-preset",
                    "veryfast",
                    "-crf",
                    "21",
                    "-c:a",
                    "aac",
                    str(trimmed_path),
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
                return False, f"Timeline trim failed on clip #{index}: {stderr_tail or 'unknown ffmpeg error'}", None, warnings
            trimmed_outputs.append(trimmed_path)

        concat_lines: list[str] = []
        for item in trimmed_outputs:
            escaped_path = item.as_posix().replace("'", "\\'")
            concat_lines.append(f"file '{escaped_path}'")
        concat_list_file.write_text("\n".join(concat_lines), encoding="utf-8")

        output_file_name = _sanitize_name(output_name, "timeline_output")
        if not output_file_name.lower().endswith(".mp4"):
            output_file_name = f"{output_file_name}.mp4"
        output_path = _timeline_output_dir() / output_file_name
        base_timeline_path = tmp_dir / "timeline_base.mp4"

        concat_cmd = [
            ffmpeg_binary(),
            "-y",
            "-hide_banner",
            "-f",
            "concat",
            "-safe",
            "0",
            "-i",
            str(concat_list_file),
            "-c:v",
            "libx264",
            "-preset",
            "medium",
            "-crf",
            "20",
            "-c:a",
            "aac",
            str(base_timeline_path),
        ]
        concat_completed = subprocess.run(
            concat_cmd,
            capture_output=True,
            text=True,
            check=False,
            timeout=max(1.0, settings.ffmpeg_timeout_seconds),
        )
        if concat_completed.returncode != 0:
            stderr_tail = (concat_completed.stderr or "").strip()[-500:]
            return False, f"Timeline compose failed: {stderr_tail or 'unknown ffmpeg error'}", None, warnings

        mix_tracks = list(audio_tracks or [])
        if background_audio_path or background_audio_url:
            mix_tracks.append(
                {
                    "source_path": background_audio_path,
                    "source_url": background_audio_url,
                    "start_seconds": 0.0,
                    "volume": max(0.0, float(background_audio_volume)),
                }
            )

        if mix_tracks:
            resolved_tracks: list[tuple[Path, float, float | None, float]] = []
            for idx, track in enumerate(mix_tracks, start=1):
                source_path_value = str(track.get("source_path") or "").strip()
                source_url_value = str(track.get("source_url") or "").strip()
                if source_path_value:
                    audio_path = validate_local_source_path(source_path_value, allowed_mime_prefixes=allowed_audio_mime_prefixes())
                elif source_url_value:
                    audio_path = download_source_url_to_temp(
                        source_url_value,
                        fallback_suffix=".mp3",
                        allowed_mime_prefixes=allowed_audio_mime_prefixes(),
                    )
                    downloaded_audio.append(audio_path)
                else:
                    warnings.append(f"Skipped audio track #{idx}: missing source")
                    continue
                resolved_tracks.append(
                    (
                        audio_path,
                        max(0.0, float(track.get("start_seconds") or 0.0)),
                        float(track.get("end_seconds")) if track.get("end_seconds") is not None else None,
                        max(0.0, float(track.get("volume") or 1.0)),
                    )
                )

            if resolved_tracks:
                mix_cmd = [ffmpeg_binary(), "-y", "-hide_banner", "-i", str(base_timeline_path)]
                for track_path, _, _, _ in resolved_tracks:
                    mix_cmd.extend(["-i", str(track_path)])

                filter_parts: list[str] = ["[0:a]anull[basea]"]
                mix_inputs = "[basea]"
                for idx, (_, start_at, end_at, volume) in enumerate(resolved_tracks, start=1):
                    label = f"trk{idx}"
                    chain = [f"[{idx}:a]"]
                    if start_at > 0:
                        delay_ms = int(start_at * 1000)
                        chain.append(f"adelay={delay_ms}|{delay_ms}")
                    if end_at is not None and end_at > 0:
                        chain.append(f"atrim=0:{end_at:.3f}")
                    chain.append(f"volume={volume:.3f}")
                    filter_parts.append(",".join(chain) + f"[{label}]")
                    mix_inputs += f"[{label}]"

                mix_count = len(resolved_tracks) + 1
                filter_parts.append(f"{mix_inputs}amix=inputs={mix_count}:duration=first:dropout_transition=2[mixa]")
                if normalize_loudness:
                    filter_parts.append("[mixa]loudnorm=I=-14:TP=-1.5:LRA=11[aout]")
                else:
                    filter_parts.append("[mixa]anull[aout]")

                mix_cmd.extend(
                    [
                        "-filter_complex",
                        ";".join(filter_parts),
                        "-map",
                        "0:v:0",
                        "-map",
                        "[aout]",
                        "-c:v",
                        "copy",
                        "-c:a",
                        "aac",
                        str(output_path),
                    ]
                )
                mixed = subprocess.run(
                    mix_cmd,
                    capture_output=True,
                    text=True,
                    check=False,
                    timeout=max(1.0, settings.ffmpeg_timeout_seconds),
                )
                if mixed.returncode != 0:
                    stderr_tail = (mixed.stderr or "").strip()[-500:]
                    return False, f"Timeline audio mix failed: {stderr_tail or 'unknown ffmpeg error'}", None, warnings
            else:
                shutil.move(str(base_timeline_path), str(output_path))
        elif normalize_loudness:
            loudnorm_cmd = [
                ffmpeg_binary(),
                "-y",
                "-hide_banner",
                "-i",
                str(base_timeline_path),
                "-map",
                "0:v:0",
                "-map",
                "0:a?",
                "-c:v",
                "copy",
                "-filter:a",
                "loudnorm=I=-14:TP=-1.5:LRA=11",
                "-c:a",
                "aac",
                str(output_path),
            ]
            loudnorm = subprocess.run(
                loudnorm_cmd,
                capture_output=True,
                text=True,
                check=False,
                timeout=max(1.0, settings.ffmpeg_timeout_seconds),
            )
            if loudnorm.returncode != 0:
                stderr_tail = (loudnorm.stderr or "").strip()[-500:]
                return False, f"Timeline loudness normalization failed: {stderr_tail or 'unknown ffmpeg error'}", None, warnings
        else:
            shutil.move(str(base_timeline_path), str(output_path))

        keyframe_vf = _build_keyframe_video_filter(keyframes, warnings)
        if keyframe_vf:
            keyframed_path = tmp_dir / "timeline_keyframed.mp4"
            keyframe_cmd = [
                ffmpeg_binary(),
                "-y",
                "-hide_banner",
                "-i",
                str(output_path),
                "-vf",
                keyframe_vf,
                "-map",
                "0:v:0",
                "-map",
                "0:a?",
                "-c:v",
                "libx264",
                "-preset",
                "medium",
                "-crf",
                "20",
                "-c:a",
                "copy",
                str(keyframed_path),
            ]
            keyed = subprocess.run(
                keyframe_cmd,
                capture_output=True,
                text=True,
                check=False,
                timeout=max(1.0, settings.ffmpeg_timeout_seconds),
            )
            if keyed.returncode != 0:
                stderr_tail = (keyed.stderr or "").strip()[-500:]
                return False, f"Timeline keyframe render failed: {stderr_tail or 'unknown ffmpeg error'}", None, warnings
            shutil.move(str(keyframed_path), str(output_path))

        return True, "Timeline composed successfully", str(output_path), warnings
    except subprocess.TimeoutExpired:
        return False, "Timeline processing timed out", None, warnings
    except Exception as exc:
        return False, f"Timeline processing failed: {exc}", None, warnings
    finally:
        for item in downloaded_sources:
            try:
                item.unlink(missing_ok=True)
            except Exception:
                pass
        for audio_file in downloaded_audio:
            try:
                audio_file.unlink(missing_ok=True)
            except Exception:
                pass
        shutil.rmtree(tmp_dir, ignore_errors=True)
