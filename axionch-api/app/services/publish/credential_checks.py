from datetime import datetime, timezone
from threading import Lock
from typing import Any

from app.db.models import Platform


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


class CredentialCheckTracker:
    def __init__(self) -> None:
        self._lock = Lock()
        self._state: dict[str, dict[str, Any]] = {
            Platform.X.value: self._empty_state(),
            Platform.LINKEDIN.value: self._empty_state(),
            Platform.INSTAGRAM.value: self._empty_state(),
        }

    @staticmethod
    def _empty_state() -> dict[str, Any]:
        return {
            "last_checked_at": None,
            "last_check_success": None,
            "last_check_error": None,
            "check_count": 0,
        }

    def record(self, platform: Platform, success: bool, error: str | None = None) -> None:
        key = platform.value
        with self._lock:
            entry = self._state[key]
            entry["last_checked_at"] = _now_iso()
            entry["last_check_success"] = success
            entry["last_check_error"] = error
            entry["check_count"] = int(entry["check_count"]) + 1

    def get(self, platform: Platform) -> dict[str, Any]:
        key = platform.value
        with self._lock:
            return dict(self._state[key])


credential_check_tracker = CredentialCheckTracker()
