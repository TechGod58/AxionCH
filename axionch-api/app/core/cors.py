from __future__ import annotations

import ipaddress


LOCAL_HOSTNAMES = {"localhost"}


def parse_csv(value: str | None) -> list[str]:
    return [item.strip() for item in (value or "").split(",") if item.strip()]


def has_wildcard(origins: list[str]) -> bool:
    return "*" in origins


def _origin_host(origin: str) -> str:
    candidate = origin.strip().lower()
    if "://" in candidate:
        candidate = candidate.split("://", 1)[1]
    candidate = candidate.split("/", 1)[0]
    candidate = candidate.split("@", 1)[-1]
    if ":" in candidate:
        candidate = candidate.rsplit(":", 1)[0]
    return candidate.strip().lower()


def _host_is_local_or_private(host: str) -> bool:
    if not host:
        return True
    if host in LOCAL_HOSTNAMES:
        return True
    if host.endswith(".local"):
        return True
    try:
        ip = ipaddress.ip_address(host)
    except ValueError:
        return False
    return (
        ip.is_private
        or ip.is_loopback
        or ip.is_link_local
        or ip.is_multicast
        or ip.is_reserved
        or ip.is_unspecified
    )


def validate_cors_for_runtime(
    origins: list[str],
    *,
    runtime_environment: str,
) -> None:
    is_production = (runtime_environment or "").strip().lower() == "production"
    if not is_production:
        return

    if not origins:
        raise RuntimeError("CORS_ALLOWED_ORIGINS must be configured in production.")

    if has_wildcard(origins):
        raise RuntimeError("Wildcard CORS origins are not allowed in production.")

    for origin in origins:
        host = _origin_host(origin)
        if _host_is_local_or_private(host):
            raise RuntimeError(
                f"Production CORS origin '{origin}' resolves to local/private host '{host}'. "
                "Only deployed public frontend origins are allowed."
            )
