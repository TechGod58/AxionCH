from __future__ import annotations

from collections import deque
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

try:
    import cv2
    import numpy as np
except Exception:  # pragma: no cover - runtime optional dependency guard
    cv2 = None  # type: ignore[assignment]
    np = None  # type: ignore[assignment]

from app.core.config import settings
from app.services.media.safety import (
    allowed_image_mime_prefixes,
    allowed_video_mime_prefixes,
    download_source_url_to_temp,
    validate_local_source_path,
)


@dataclass
class MotionTrackPoint:
    at_seconds: float
    x: int
    y: int
    width: int
    height: int
    confidence: float
    point_count: int


@dataclass
class MotionTrackResult:
    success: bool
    message: str
    source_path_used: str | None = None
    fps: float = 0.0
    frame_count: int = 0
    average_confidence: float = 0.0
    overlay_path: str | None = None
    track_points: list[MotionTrackPoint] | None = None


@dataclass
class ImageHealResult:
    success: bool
    message: str
    source_path_used: str | None = None
    output_path: str | None = None


def _utc_stamp() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")


def _output_dir() -> Path:
    output_dir = Path(settings.image_output_dir).expanduser()
    if not output_dir.is_absolute():
        output_dir = Path.cwd() / output_dir
    output_dir.mkdir(parents=True, exist_ok=True)
    return output_dir


def _resolve_video_source(path: str | None, url: str | None) -> tuple[Path | None, bool]:
    if path:
        return validate_local_source_path(path, allowed_mime_prefixes=allowed_video_mime_prefixes()), False
    if url:
        return (
            download_source_url_to_temp(
                url,
                fallback_suffix=".mp4",
                allowed_mime_prefixes=allowed_video_mime_prefixes(),
            ),
            True,
        )
    return None, False


def _resolve_image_source(path: str | None, url: str | None) -> tuple[Path | None, bool]:
    if path:
        return validate_local_source_path(path, allowed_mime_prefixes=allowed_image_mime_prefixes()), False
    if url:
        return (
            download_source_url_to_temp(
                url,
                fallback_suffix=".png",
                allowed_mime_prefixes=allowed_image_mime_prefixes(),
            ),
            True,
        )
    return None, False


def track_video_motion(
    *,
    source_path: str | None,
    source_url: str | None,
    start_seconds: float,
    end_seconds: float | None,
    max_corners: int,
    quality_level: float,
    min_distance: float,
    sample_every_n_frames: int,
    roi_x: int | None,
    roi_y: int | None,
    roi_width: int | None,
    roi_height: int | None,
    smoothing_window: int,
    min_confidence: float,
    output_overlay_name: str | None,
) -> MotionTrackResult:
    if cv2 is None or np is None:
        return MotionTrackResult(
            success=False,
            message="OpenCV runtime is not available. Install opencv-python-headless and numpy.",
            track_points=[],
        )
    working_source, downloaded = _resolve_video_source(source_path, source_url)
    if working_source is None:
        return MotionTrackResult(success=False, message="Provide source_path or source_url")

    try:
        capture = cv2.VideoCapture(str(working_source))
        if not capture.isOpened():
            return MotionTrackResult(
                success=False,
                message="Unable to open video for motion tracking",
                source_path_used=str(working_source),
            )

        fps = float(capture.get(cv2.CAP_PROP_FPS) or 0.0)
        if fps <= 0.0:
            fps = 30.0
        frame_count = int(capture.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
        frame_width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH) or 0)
        frame_height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT) or 0)
        start_frame = max(0, int(max(0.0, start_seconds) * fps))
        capture.set(cv2.CAP_PROP_POS_FRAMES, start_frame)

        end_frame = frame_count - 1
        if end_seconds is not None:
            end_frame = min(end_frame, int(max(0.0, end_seconds) * fps))

        prev_gray: np.ndarray | None = None
        prev_points: np.ndarray | None = None
        recent_boxes: deque[tuple[float, float, float, float]] = deque(maxlen=max(1, int(smoothing_window)))
        track_points: list[MotionTrackPoint] = []
        frame_index = start_frame
        sample_rate = max(1, int(sample_every_n_frames))
        confidence_floor = max(0.0, min(1.0, float(min_confidence)))
        confidence_total = 0.0
        confidence_count = 0

        initial_roi: tuple[int, int, int, int] | None = None
        if (
            roi_x is not None
            and roi_y is not None
            and roi_width is not None
            and roi_height is not None
            and roi_width > 0
            and roi_height > 0
        ):
            x = max(0, int(roi_x))
            y = max(0, int(roi_y))
            w = max(1, int(roi_width))
            h = max(1, int(roi_height))
            if frame_width > 0 and frame_height > 0:
                x2 = min(frame_width, x + w)
                y2 = min(frame_height, y + h)
                if x2 > x and y2 > y:
                    initial_roi = (x, y, x2 - x, y2 - y)

        overlay_path: Path | None = None
        overlay_writer: object | None = None
        if output_overlay_name:
            raw_name = output_overlay_name.strip()
            safe_name = "".join(ch for ch in raw_name if ch.isalnum() or ch in {"-", "_", "."}) or f"motion_overlay_{_utc_stamp()}"
            if not safe_name.lower().endswith(".mp4"):
                safe_name = f"{safe_name}.mp4"
            overlay_path = _output_dir() / safe_name
            try:
                fourcc = cv2.VideoWriter_fourcc(*"mp4v")
                writer = cv2.VideoWriter(str(overlay_path), fourcc, fps, (max(1, frame_width), max(1, frame_height)))
                if writer.isOpened():
                    overlay_writer = writer
                else:
                    writer.release()
                    overlay_path = None
            except Exception:
                overlay_path = None
                overlay_writer = None

        last_bbox: tuple[int, int, int, int] | None = None

        while frame_index <= end_frame:
            ok, frame = capture.read()
            if not ok:
                break

            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            if prev_gray is None or prev_points is None or len(prev_points) < 6:
                feature_mask: np.ndarray | None = None
                focus_roi = last_bbox or initial_roi
                if focus_roi is not None:
                    feature_mask = np.zeros_like(gray)
                    x, y, w, h = focus_roi
                    x2 = min(gray.shape[1], x + w)
                    y2 = min(gray.shape[0], y + h)
                    if x2 > x and y2 > y:
                        cv2.rectangle(feature_mask, (x, y), (x2, y2), color=255, thickness=-1)
                prev_points = cv2.goodFeaturesToTrack(
                    gray,
                    maxCorners=max(24, int(max_corners)),
                    qualityLevel=max(0.01, min(0.8, float(quality_level))),
                    minDistance=max(2.0, float(min_distance)),
                    mask=feature_mask,
                )
                prev_gray = gray
                if overlay_writer is not None:
                    overlay_writer.write(frame)
                frame_index += 1
                continue

            next_points, status, _ = cv2.calcOpticalFlowPyrLK(prev_gray, gray, prev_points, None)
            if next_points is None or status is None:
                prev_points = None
                prev_gray = gray
                frame_index += 1
                continue

            valid = next_points[status.flatten() == 1]
            if len(valid) >= 4:
                bbox = cv2.boundingRect(valid.astype(np.float32))
                recent_boxes.append((float(bbox[0]), float(bbox[1]), float(bbox[2]), float(bbox[3])))
                smoothed = (
                    int(round(sum(item[0] for item in recent_boxes) / len(recent_boxes))),
                    int(round(sum(item[1] for item in recent_boxes) / len(recent_boxes))),
                    int(round(sum(item[2] for item in recent_boxes) / len(recent_boxes))),
                    int(round(sum(item[3] for item in recent_boxes) / len(recent_boxes))),
                )
                confidence = min(1.0, float(len(valid)) / float(max(1, max_corners)))
                if confidence >= confidence_floor and frame_index % sample_rate == 0:
                    track_points.append(
                        MotionTrackPoint(
                            at_seconds=float(frame_index) / fps,
                            x=smoothed[0],
                            y=smoothed[1],
                            width=smoothed[2],
                            height=smoothed[3],
                            confidence=confidence,
                            point_count=int(len(valid)),
                        )
                    )
                    confidence_total += confidence
                    confidence_count += 1
                last_bbox = smoothed
                prev_points = valid.reshape(-1, 1, 2)
            else:
                prev_points = None
                last_bbox = None

            prev_gray = gray
            if overlay_writer is not None:
                overlay_frame = frame.copy()
                if last_bbox is not None:
                    x, y, w, h = last_bbox
                    cv2.rectangle(overlay_frame, (x, y), (x + w, y + h), (64, 255, 96), 2)
                    cv2.putText(
                        overlay_frame,
                        f"t={frame_index / fps:.2f}s",
                        (x, max(24, y - 8)),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.55,
                        (64, 255, 96),
                        2,
                        cv2.LINE_AA,
                    )
                overlay_writer.write(overlay_frame)
            frame_index += 1

        capture.release()
        if overlay_writer is not None:
            overlay_writer.release()
        if not track_points:
            return MotionTrackResult(
                success=False,
                message="Tracking completed but no stable motion points were detected",
                source_path_used=str(working_source),
                fps=fps,
                frame_count=frame_count,
                overlay_path=str(overlay_path) if overlay_path else None,
                track_points=[],
            )
        return MotionTrackResult(
            success=True,
            message="Motion tracking completed",
            source_path_used=str(working_source),
            fps=fps,
            frame_count=frame_count,
            average_confidence=(confidence_total / confidence_count) if confidence_count else 0.0,
            overlay_path=str(overlay_path) if overlay_path else None,
            track_points=track_points,
        )
    except Exception as exc:
        return MotionTrackResult(
            success=False,
            message=f"Motion tracking failed: {exc}",
            source_path_used=str(working_source),
            track_points=[],
        )
    finally:
        if downloaded and working_source is not None:
            try:
                working_source.unlink(missing_ok=True)
            except Exception:
                pass


def _load_mask(
    *,
    source_shape: tuple[int, int],
    mask_path: str | None,
    mask_url: str | None,
    erase_regions: list[dict[str, int]],
    feather_radius: int,
) -> tuple[np.ndarray, list[Path]]:
    temp_files: list[Path] = []
    height, width = source_shape
    mask = np.zeros((height, width), dtype=np.uint8)

    if mask_path or mask_url:
        resolved_mask, downloaded = _resolve_image_source(mask_path, mask_url)
        if resolved_mask is not None:
            mask_img = cv2.imread(str(resolved_mask), cv2.IMREAD_GRAYSCALE)
            if mask_img is not None:
                resized = cv2.resize(mask_img, (width, height), interpolation=cv2.INTER_LINEAR)
                _, binary = cv2.threshold(resized, 10, 255, cv2.THRESH_BINARY)
                mask = cv2.bitwise_or(mask, binary)
            if downloaded:
                temp_files.append(resolved_mask)

    for region in erase_regions:
        x = max(0, int(region.get("x", 0)))
        y = max(0, int(region.get("y", 0)))
        w = max(0, int(region.get("width", 0)))
        h = max(0, int(region.get("height", 0)))
        if w <= 0 or h <= 0:
            continue
        x2 = min(width, x + w)
        y2 = min(height, y + h)
        if x2 <= x or y2 <= y:
            continue
        cv2.rectangle(mask, (x, y), (x2, y2), color=255, thickness=-1)

    if feather_radius > 0:
        kernel = max(1, int(feather_radius) * 2 + 1)
        mask = cv2.GaussianBlur(mask, (kernel, kernel), 0)
        _, mask = cv2.threshold(mask, 8, 255, cv2.THRESH_BINARY)

    return mask, temp_files


def heal_image_with_inpaint(
    *,
    source_path: str | None,
    source_url: str | None,
    output_name: str | None,
    mask_path: str | None,
    mask_url: str | None,
    erase_regions: list[dict[str, int]],
    inpaint_radius: float,
    method: str,
    fill_strategy: str,
    feather_radius: int,
    preserve_edges: bool,
    edge_blend: float,
    denoise_strength: float,
) -> ImageHealResult:
    if cv2 is None or np is None:
        return ImageHealResult(
            success=False,
            message="OpenCV runtime is not available. Install opencv-python-headless and numpy.",
        )
    working_source, downloaded_source = _resolve_image_source(source_path, source_url)
    if working_source is None:
        return ImageHealResult(success=False, message="Provide source_path or source_url")

    temp_files: list[Path] = []
    try:
        image = cv2.imread(str(working_source), cv2.IMREAD_COLOR)
        if image is None:
            return ImageHealResult(
                success=False,
                message="Unable to decode source image",
                source_path_used=str(working_source),
            )

        mask, mask_temp = _load_mask(
            source_shape=image.shape[:2],
            mask_path=mask_path,
            mask_url=mask_url,
            erase_regions=erase_regions,
            feather_radius=feather_radius,
        )
        temp_files.extend(mask_temp)

        if int(np.count_nonzero(mask)) == 0:
            return ImageHealResult(
                success=False,
                message="No mask or erase regions were provided",
                source_path_used=str(working_source),
            )

        kernel = np.ones((3, 3), dtype=np.uint8)
        mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel, iterations=1)
        mask = cv2.dilate(mask, kernel, iterations=1)

        inpaint_method = cv2.INPAINT_TELEA if method.strip().lower() != "ns" else cv2.INPAINT_NS
        radius = max(1.0, float(inpaint_radius))
        strategy = (fill_strategy or "inpaint").strip().lower()
        if strategy == "hybrid":
            telea = cv2.inpaint(image, mask, radius, cv2.INPAINT_TELEA)
            ns = cv2.inpaint(image, mask, radius, cv2.INPAINT_NS)
            healed = cv2.addWeighted(telea, 0.65, ns, 0.35, 0.0)
        else:
            healed = cv2.inpaint(image, mask, radius, inpaint_method)

        if preserve_edges:
            edges = cv2.Canny(image, 70, 160)
            edge_mask = cv2.GaussianBlur(edges, (3, 3), 0)
            edge_mask = cv2.threshold(edge_mask, 8, 255, cv2.THRESH_BINARY)[1]
            edge_mask_3 = cv2.cvtColor(edge_mask, cv2.COLOR_GRAY2BGR)
            alpha = max(0.0, min(1.0, float(edge_blend)))
            blended_edges = cv2.addWeighted(healed, 1.0 - alpha, image, alpha, 0.0)
            healed = np.where(edge_mask_3 > 0, blended_edges, healed).astype(np.uint8)

        if denoise_strength > 0:
            strength = max(1.0, min(20.0, float(denoise_strength)))
            healed = cv2.fastNlMeansDenoisingColored(
                healed,
                None,
                h=strength,
                hColor=strength,
                templateWindowSize=7,
                searchWindowSize=21,
            )

        base = (output_name or "").strip() or f"{Path(working_source).stem}_heal_{_utc_stamp()}"
        safe_base = "".join(ch for ch in base if ch.isalnum() or ch in {"-", "_", "."}) or f"heal_{_utc_stamp()}"
        if not safe_base.lower().endswith((".png", ".jpg", ".jpeg", ".webp")):
            safe_base = f"{safe_base}.png"
        output_path = _output_dir() / safe_base
        cv2.imwrite(str(output_path), healed)

        return ImageHealResult(
            success=True,
            message="Image healing completed",
            source_path_used=str(working_source),
            output_path=str(output_path),
        )
    except Exception as exc:
        return ImageHealResult(
            success=False,
            message=f"Image healing failed: {exc}",
            source_path_used=str(working_source),
        )
    finally:
        if downloaded_source and working_source is not None:
            try:
                working_source.unlink(missing_ok=True)
            except Exception:
                pass
        for temp in temp_files:
            try:
                temp.unlink(missing_ok=True)
            except Exception:
                pass
