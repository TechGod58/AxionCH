from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
import ipaddress
from pathlib import Path
import socket
import subprocess
import threading
import urllib.parse
import uuid

from app.core.config import settings
from app.services.media.safety import (
    allowed_video_mime_prefixes,
    validate_local_source_path,
    validate_source_url,
)
from app.services.media.video_filters import ffmpeg_binary, ffmpeg_is_available


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _iso_now() -> str:
    return _utc_now().isoformat()


def _parse_csv(raw: str | None) -> list[str]:
    return [item.strip() for item in (raw or "").split(",") if item.strip()]


def _blocked_hosts() -> set[str]:
    return {item.lower() for item in _parse_csv(settings.media_blocked_source_hosts)}


def _resolve_host_addresses(hostname: str) -> set[str]:
    infos = socket.getaddrinfo(hostname, None, proto=socket.IPPROTO_TCP)
    addresses: set[str] = set()
    for info in infos:
        sockaddr = info[4]
        if not sockaddr:
            continue
        addresses.add(sockaddr[0])
    return addresses


def _address_is_private_or_local(address: str) -> bool:
    ip = ipaddress.ip_address(address)
    return (
        ip.is_private
        or ip.is_loopback
        or ip.is_link_local
        or ip.is_multicast
        or ip.is_reserved
        or ip.is_unspecified
    )


def _validate_rtmp_like_url(url_value: str, *, field_name: str) -> urllib.parse.ParseResult:
    parsed = urllib.parse.urlparse(url_value)
    if parsed.scheme not in {"rtmp", "rtmps"}:
        raise ValueError(f"{field_name} must use rtmp:// or rtmps://")
    hostname = (parsed.hostname or "").strip().lower()
    if not hostname:
        raise ValueError(f"{field_name} hostname is required")
    if hostname in _blocked_hosts():
        raise ValueError(f"{field_name} host '{hostname}' is blocked")
    if settings.media_block_private_networks:
        try:
            addresses = _resolve_host_addresses(hostname)
        except Exception as exc:
            raise ValueError(f"Unable to resolve {field_name} host '{hostname}': {exc}") from exc
        if any(_address_is_private_or_local(address) for address in addresses):
            raise ValueError(f"{field_name} host '{hostname}' resolved to a private or local network address")
    return parsed


def _validate_live_input_source(*, source_path: str | None, source_url: str | None) -> tuple[str, str]:
    source_path_value = (source_path or "").strip()
    source_url_value = (source_url or "").strip()
    if source_path_value and source_url_value:
        raise ValueError("Provide source_path or source_url, not both")
    if not source_path_value and not source_url_value:
        raise ValueError("Provide source_path or source_url for live multicast input")

    if source_path_value:
        validated_path = validate_local_source_path(
            source_path_value,
            allowed_mime_prefixes=allowed_video_mime_prefixes(),
        )
        return "path", str(validated_path)

    parsed = urllib.parse.urlparse(source_url_value)
    scheme = parsed.scheme.strip().lower()
    if scheme in {"http", "https"}:
        validate_source_url(source_url_value)
        return "url", source_url_value
    if scheme in {"rtmp", "rtmps"}:
        _validate_rtmp_like_url(source_url_value, field_name="source_url")
        return "url", source_url_value
    raise ValueError("source_url must use http(s):// or rtmp(s)://")


def _compose_destination_target(ingest_url: str, stream_key: str | None) -> str:
    base = ingest_url.strip()
    key = (stream_key or "").strip()
    if not key:
        return base
    if "{stream_key}" in base:
        return base.replace("{stream_key}", key)
    if base.endswith("/"):
        return f"{base}{key}"
    if base.endswith("="):
        return f"{base}{key}"
    return f"{base}/{key}"


def _mask_url(url_value: str) -> str:
    parsed = urllib.parse.urlparse(url_value)
    host = parsed.hostname or ""
    if not parsed.scheme or not host:
        return "***"
    return f"{parsed.scheme}://{host}/***"


def _normalize_session_name(raw: str | None) -> str:
    value = (raw or "").strip()
    if not value:
        return f"live-{_utc_now().strftime('%Y%m%d-%H%M%S')}"
    return value[:120]


@dataclass
class LiveDestinationRuntime:
    destination_id: str
    platform: str
    label: str | None
    ingest_url_masked: str
    target_url: str
    target_host: str
    stream_key_configured: bool
    status: str = "ready"
    last_error: str | None = None


@dataclass
class LiveMulticastSessionRuntime:
    session_id: str
    session_name: str
    status: str
    message: str
    created_at: str
    source_kind: str
    source_value: str
    dry_run: bool
    copy_video: bool
    video_bitrate: str
    audio_bitrate: str
    fps: int
    gop_seconds: float
    destinations: list[LiveDestinationRuntime]
    started_at: str | None = None
    stopped_at: str | None = None
    ffmpeg_pid: int | None = None
    ffmpeg_exit_code: int | None = None
    log_tail: list[str] = field(default_factory=list)
    stop_requested: bool = False
    process: subprocess.Popen[str] | None = field(default=None, repr=False)
    monitor_thread: threading.Thread | None = field(default=None, repr=False)


class LiveMulticastService:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._sessions: dict[str, LiveMulticastSessionRuntime] = {}
        self._max_sessions = 6
        self._max_destinations = 8
        self._max_log_tail = 120

    def start_session(
        self,
        *,
        session_name: str | None,
        source_path: str | None,
        source_url: str | None,
        destinations: list[dict[str, object]],
        dry_run: bool,
        copy_video: bool,
        video_bitrate: str,
        audio_bitrate: str,
        fps: int,
        gop_seconds: float,
    ) -> dict[str, object]:
        enabled = [item for item in destinations if bool(item.get("enabled", True))]
        if not enabled:
            raise ValueError("Provide at least one enabled destination")
        if len(enabled) > self._max_destinations:
            raise ValueError(f"Too many destinations. Maximum is {self._max_destinations}.")

        source_kind, source_value = _validate_live_input_source(source_path=source_path, source_url=source_url)
        fps_value = max(1, min(60, int(fps or 30)))
        gop_value = max(0.5, min(10.0, float(gop_seconds or 2.0)))
        video_bitrate_value = (video_bitrate or "4500k").strip() or "4500k"
        audio_bitrate_value = (audio_bitrate or "160k").strip() or "160k"

        runtime_destinations: list[LiveDestinationRuntime] = []
        for index, item in enumerate(enabled, start=1):
            platform = str(item.get("platform") or "custom").strip().lower() or "custom"
            label_value = str(item.get("label") or "").strip() or None
            ingest_url = str(item.get("ingest_url") or "").strip()
            if not ingest_url:
                raise ValueError(f"Destination #{index} is missing ingest_url")
            stream_key = str(item.get("stream_key") or "").strip() or None
            target = _compose_destination_target(ingest_url, stream_key)
            parsed = _validate_rtmp_like_url(target, field_name=f"destination #{index}")
            runtime_destinations.append(
                LiveDestinationRuntime(
                    destination_id=f"dst_{index:02d}",
                    platform=platform,
                    label=label_value,
                    ingest_url_masked=_mask_url(target),
                    target_url=target,
                    target_host=(parsed.hostname or "").lower(),
                    stream_key_configured=stream_key is not None,
                )
            )

        session = LiveMulticastSessionRuntime(
            session_id=f"live_{uuid.uuid4().hex[:12]}",
            session_name=_normalize_session_name(session_name),
            status="starting",
            message="Preparing live multicast session...",
            created_at=_iso_now(),
            source_kind=source_kind,
            source_value=source_value,
            dry_run=dry_run,
            copy_video=copy_video,
            video_bitrate=video_bitrate_value,
            audio_bitrate=audio_bitrate_value,
            fps=fps_value,
            gop_seconds=gop_value,
            destinations=runtime_destinations,
        )

        with self._lock:
            active_sessions = sum(
                1
                for value in self._sessions.values()
                if value.status in {"starting", "live", "stopping", "live_simulated"}
            )
            if active_sessions >= self._max_sessions:
                raise ValueError(f"Too many active live sessions. Maximum is {self._max_sessions}.")
            self._sessions[session.session_id] = session

        if dry_run:
            with self._lock:
                current = self._sessions[session.session_id]
                current.status = "live_simulated"
                current.message = "Dry-run multicast session started (no ffmpeg process)."
                current.started_at = _iso_now()
                for destination in current.destinations:
                    destination.status = "live"
            return self.get_session(session.session_id)

        if not ffmpeg_is_available():
            with self._lock:
                self._sessions.pop(session.session_id, None)
            raise ValueError(f"'{ffmpeg_binary()}' was not found. Install FFmpeg to start multicast live streaming.")

        command = self._build_ffmpeg_command(session)
        try:
            process = subprocess.Popen(
                command,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.PIPE,
                text=True,
                bufsize=1,
            )
        except Exception as exc:
            with self._lock:
                self._sessions.pop(session.session_id, None)
            raise ValueError(f"Unable to start ffmpeg live multicast process: {exc}") from exc

        with self._lock:
            current = self._sessions[session.session_id]
            current.process = process
            current.ffmpeg_pid = process.pid
            current.started_at = _iso_now()
            current.status = "starting"
            current.message = "Live multicast process started. Waiting for stream handshake..."
            monitor = threading.Thread(
                target=self._monitor_session,
                args=(session.session_id,),
                name=f"axionch-live-{session.session_id}",
                daemon=True,
            )
            current.monitor_thread = monitor
            monitor.start()

        return self.get_session(session.session_id)

    def get_session(self, session_id: str) -> dict[str, object]:
        with self._lock:
            session = self._sessions.get(session_id)
            if session is None:
                raise ValueError(f"Live session '{session_id}' was not found")
            return self._snapshot_locked(session)

    def list_sessions(self) -> list[dict[str, object]]:
        with self._lock:
            sessions = list(self._sessions.values())
            sessions.sort(key=lambda item: item.created_at, reverse=True)
            return [self._snapshot_locked(item) for item in sessions]

    def stop_session(self, session_id: str) -> dict[str, object]:
        with self._lock:
            session = self._sessions.get(session_id)
            if session is None:
                raise ValueError(f"Live session '{session_id}' was not found")
            if session.status in {"stopped", "failed"}:
                return self._snapshot_locked(session)
            if session.dry_run or session.process is None:
                session.status = "stopped"
                session.message = "Dry-run live session stopped."
                session.stopped_at = _iso_now()
                for destination in session.destinations:
                    if destination.status != "failed":
                        destination.status = "stopped"
                return self._snapshot_locked(session)

            process = session.process
            session.stop_requested = True
            session.status = "stopping"
            session.message = "Stopping live multicast..."

        try:
            process.terminate()
        except Exception:
            pass
        try:
            process.wait(timeout=8)
        except Exception:
            try:
                process.kill()
            except Exception:
                pass

        with self._lock:
            session = self._sessions.get(session_id)
            if session is None:
                raise ValueError(f"Live session '{session_id}' was not found")
            if session.status == "stopping":
                session.status = "stopped"
                session.message = "Live multicast stopped."
                session.stopped_at = _iso_now()
                for destination in session.destinations:
                    if destination.status != "failed":
                        destination.status = "stopped"
            return self._snapshot_locked(session)

    def shutdown(self) -> None:
        for snapshot in self.list_sessions():
            status = str(snapshot.get("status") or "")
            if status in {"starting", "live", "stopping", "live_simulated"}:
                try:
                    self.stop_session(str(snapshot["session_id"]))
                except Exception:
                    pass

    def _build_ffmpeg_command(self, session: LiveMulticastSessionRuntime) -> list[str]:
        tee_outputs = "|".join(f"[f=flv:onfail=ignore]{destination.target_url}" for destination in session.destinations)
        gop_frames = max(1, int(session.fps * session.gop_seconds))
        command = [ffmpeg_binary(), "-hide_banner", "-loglevel", "info", "-nostats"]
        if session.source_kind == "path":
            command.extend(["-re", "-i", session.source_value])
        else:
            command.extend(["-i", session.source_value])
        command.extend(["-map", "0:v:0", "-map", "0:a?"])
        if session.copy_video:
            command.extend(["-c:v", "copy"])
        else:
            command.extend(
                [
                    "-c:v",
                    "libx264",
                    "-preset",
                    "veryfast",
                    "-tune",
                    "zerolatency",
                    "-pix_fmt",
                    "yuv420p",
                    "-r",
                    str(session.fps),
                    "-g",
                    str(gop_frames),
                    "-keyint_min",
                    str(gop_frames),
                    "-b:v",
                    session.video_bitrate,
                    "-maxrate",
                    session.video_bitrate,
                    "-bufsize",
                    session.video_bitrate,
                ]
            )
        command.extend(
            [
                "-c:a",
                "aac",
                "-b:a",
                session.audio_bitrate,
                "-ar",
                "44100",
                "-ac",
                "2",
                "-f",
                "tee",
                tee_outputs,
            ]
        )
        return command

    def _monitor_session(self, session_id: str) -> None:
        with self._lock:
            session = self._sessions.get(session_id)
            if session is None or session.process is None:
                return
            process = session.process

        try:
            if process.stderr is not None:
                for raw_line in process.stderr:
                    line = raw_line.strip()
                    if not line:
                        continue
                    with self._lock:
                        current = self._sessions.get(session_id)
                        if current is None:
                            return
                        self._append_log_locked(current, line)
                        lower_line = line.lower()
                        if current.status == "starting" and ("frame=" in lower_line or "press [q]" in lower_line):
                            current.status = "live"
                            current.message = "Live multicast is running."
                            for destination in current.destinations:
                                if destination.status == "ready":
                                    destination.status = "live"
                        self._update_destination_errors_locked(current, line)
            exit_code = process.wait()
        except Exception as exc:
            with self._lock:
                current = self._sessions.get(session_id)
                if current is not None:
                    current.status = "failed"
                    current.message = f"Live multicast monitor failed: {exc}"
                    current.stopped_at = _iso_now()
                    current.ffmpeg_exit_code = None
            return

        with self._lock:
            current = self._sessions.get(session_id)
            if current is None:
                return
            current.ffmpeg_exit_code = int(exit_code)
            current.ffmpeg_pid = None
            current.process = None
            current.stopped_at = _iso_now()
            if current.stop_requested:
                current.status = "stopped"
                current.message = "Live multicast stopped."
                for destination in current.destinations:
                    if destination.status != "failed":
                        destination.status = "stopped"
            elif exit_code == 0:
                current.status = "stopped"
                current.message = "Live multicast ended."
                for destination in current.destinations:
                    if destination.status != "failed":
                        destination.status = "stopped"
            else:
                current.status = "failed"
                current.message = f"Live multicast failed (ffmpeg exit code {exit_code})."
                for destination in current.destinations:
                    if destination.status not in {"failed", "stopped"}:
                        destination.status = "failed"
                        if not destination.last_error:
                            destination.last_error = current.message

    def _append_log_locked(self, session: LiveMulticastSessionRuntime, line: str) -> None:
        session.log_tail.append(line)
        if len(session.log_tail) > self._max_log_tail:
            del session.log_tail[:-self._max_log_tail]

    def _update_destination_errors_locked(self, session: LiveMulticastSessionRuntime, line: str) -> None:
        lower_line = line.lower()
        error_signatures = ["error", "failed", "refused", "timed out", "forbidden", "denied"]
        if not any(token in lower_line for token in error_signatures):
            return
        for destination in session.destinations:
            if destination.target_host and destination.target_host in lower_line:
                destination.status = "failed"
                destination.last_error = line[-300:]

    def _snapshot_locked(self, session: LiveMulticastSessionRuntime) -> dict[str, object]:
        active_destinations = sum(1 for item in session.destinations if item.status in {"ready", "live"})
        failed_destinations = sum(1 for item in session.destinations if item.status == "failed")
        return {
            "session_id": session.session_id,
            "session_name": session.session_name,
            "status": session.status,
            "message": session.message,
            "created_at": session.created_at,
            "started_at": session.started_at,
            "stopped_at": session.stopped_at,
            "source_kind": session.source_kind,
            "source_value": session.source_value,
            "ffmpeg_pid": session.ffmpeg_pid,
            "ffmpeg_exit_code": session.ffmpeg_exit_code,
            "dry_run": session.dry_run,
            "active_destinations": active_destinations,
            "failed_destinations": failed_destinations,
            "destinations": [
                {
                    "destination_id": destination.destination_id,
                    "platform": destination.platform,
                    "label": destination.label,
                    "ingest_url_masked": destination.ingest_url_masked,
                    "stream_key_configured": destination.stream_key_configured,
                    "status": destination.status,
                    "last_error": destination.last_error,
                }
                for destination in session.destinations
            ],
            "log_tail": list(session.log_tail),
        }


live_multicast_service = LiveMulticastService()

