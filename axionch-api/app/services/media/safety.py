from __future__ import annotations

from pathlib import Path
import ipaddress
import mimetypes
import socket
import tempfile
import urllib.parse

import httpx

from app.core.config import settings


def _parse_csv(value: str | None) -> list[str]:
    return [item.strip() for item in (value or "").split(",") if item.strip()]


def _allowed_domains() -> list[str]:
    return [item.lower() for item in _parse_csv(settings.media_allowed_source_domains)]


def _blocked_hosts() -> set[str]:
    return {item.lower() for item in _parse_csv(settings.media_blocked_source_hosts)}


def _mime_prefixes(raw: str | None) -> list[str]:
    return [item.lower() for item in _parse_csv(raw)]


def allowed_video_mime_prefixes() -> list[str]:
    return _mime_prefixes(settings.media_allowed_video_mime_prefixes)


def allowed_image_mime_prefixes() -> list[str]:
    return _mime_prefixes(settings.media_allowed_image_mime_prefixes)


def _hostname_allowed(hostname: str, allowed_domains: list[str]) -> bool:
    if not allowed_domains:
        return True
    for domain in allowed_domains:
        if hostname == domain or hostname.endswith(f".{domain}"):
            return True
    return False


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


def _resolve_host_addresses(hostname: str) -> set[str]:
    infos = socket.getaddrinfo(hostname, None, proto=socket.IPPROTO_TCP)
    addresses: set[str] = set()
    for info in infos:
        sockaddr = info[4]
        if not sockaddr:
            continue
        addresses.add(sockaddr[0])
    return addresses


def validate_source_url(url: str) -> urllib.parse.ParseResult:
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme not in {"https", "http"}:
        raise ValueError("Only http/https source_url values are supported")
    if parsed.scheme == "http" and not settings.media_allow_http_source_urls:
        raise ValueError("HTTP source URLs are disabled. Use HTTPS or set MEDIA_ALLOW_HTTP_SOURCE_URLS=true.")

    hostname = (parsed.hostname or "").strip().lower()
    if not hostname:
        raise ValueError("URL hostname is required")

    if hostname in _blocked_hosts():
        raise ValueError(f"Blocked source host: {hostname}")

    allowed_domains = _allowed_domains()
    if not _hostname_allowed(hostname, allowed_domains):
        raise ValueError(f"Source host '{hostname}' is not in MEDIA_ALLOWED_SOURCE_DOMAINS allowlist")

    if settings.media_block_private_networks:
        try:
            addresses = _resolve_host_addresses(hostname)
        except Exception as exc:
            raise ValueError(f"Unable to resolve source host '{hostname}': {exc}") from exc
        if any(_address_is_private_or_local(address) for address in addresses):
            raise ValueError(f"Source host '{hostname}' resolved to a private or local network address")

    return parsed


def _is_allowed_mime(content_type: str | None, allowed_prefixes: list[str]) -> bool:
    if not allowed_prefixes:
        return True
    if not content_type:
        return False
    candidate = content_type.split(";")[0].strip().lower()
    return any(candidate.startswith(prefix) for prefix in allowed_prefixes)


def validate_local_source_path(path_value: str, *, allowed_mime_prefixes: list[str]) -> Path:
    source = Path(path_value).expanduser()
    if not source.is_absolute():
        source = Path.cwd() / source
    if not source.exists() or not source.is_file():
        raise ValueError(f"source_path not found: {source}")
    if source.stat().st_size > max(1, settings.media_max_download_bytes):
        raise ValueError("source_path exceeds MEDIA_MAX_DOWNLOAD_BYTES limit")

    guessed_mime, _ = mimetypes.guess_type(str(source))
    if guessed_mime and not _is_allowed_mime(guessed_mime, allowed_mime_prefixes):
        raise ValueError(f"source_path MIME '{guessed_mime}' is not allowed")
    return source


def download_source_url_to_temp(
    url: str,
    *,
    fallback_suffix: str,
    allowed_mime_prefixes: list[str],
) -> Path:
    parsed = validate_source_url(url)
    suffix = Path(parsed.path).suffix or fallback_suffix
    max_bytes = max(1, settings.media_max_download_bytes)
    timeout = max(1.0, settings.media_download_timeout_seconds)

    with httpx.Client(timeout=timeout, follow_redirects=True) as client:
        with client.stream("GET", url) as response:
            response.raise_for_status()
            content_type = (response.headers.get("content-type") or "").strip()
            if not _is_allowed_mime(content_type, allowed_mime_prefixes):
                raise ValueError(
                    f"source_url content-type '{content_type or 'unknown'}' is not allowed for this endpoint"
                )

            temp = tempfile.NamedTemporaryFile(prefix="axionch_media_", suffix=suffix, delete=False)
            total = 0
            try:
                for chunk in response.iter_bytes(chunk_size=65536):
                    if not chunk:
                        continue
                    total += len(chunk)
                    if total > max_bytes:
                        raise ValueError("source_url download exceeds MEDIA_MAX_DOWNLOAD_BYTES limit")
                    temp.write(chunk)
                temp.flush()
            finally:
                temp.close()
    return Path(temp.name)
