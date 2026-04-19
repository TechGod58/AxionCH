from collections import deque
from datetime import datetime, timezone
from threading import Lock
from typing import Any
from uuid import uuid4

from app.core.config import settings
from app.db.models import Platform, PublishStatus


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


class DryRunHistoryStore:
    def __init__(self, max_items: int = 100) -> None:
        self._lock = Lock()
        self._items: deque[dict[str, Any]] = deque(maxlen=max_items)

    def add(
        self,
        *,
        user_email: str,
        body: str,
        image_url: str | None,
        account_ids: list[int],
        status: PublishStatus,
        results: list[dict[str, Any]],
    ) -> dict[str, Any]:
        item = {
            "id": str(uuid4()),
            "created_at": _now_iso(),
            "user_email": user_email,
            "body_preview": body[:120],
            "image_url": image_url,
            "account_ids": account_ids,
            "status": status.value,
            "results": results,
        }
        with self._lock:
            self._items.appendleft(item)
        return dict(item)

    @staticmethod
    def _matches_filters(item: dict[str, Any], platform: Platform | None, success_only: bool | None) -> bool:
        if platform is not None:
            if not any(result.get("platform") == platform.value for result in item.get("results", [])):
                return False
        if success_only is not None:
            status = str(item.get("status", ""))
            if success_only and status != PublishStatus.SUCCESS.value:
                return False
            if not success_only and status == PublishStatus.SUCCESS.value:
                return False
        return True

    def list_items(
        self,
        *,
        limit: int | None = None,
        platform: Platform | None = None,
        success_only: bool | None = None,
    ) -> list[dict[str, Any]]:
        with self._lock:
            matched = [
                dict(item)
                for item in self._items
                if self._matches_filters(item, platform=platform, success_only=success_only)
            ]
        if limit is None:
            return matched
        return matched[:max(limit, 0)]

    def clear(self, *, platform: Platform | None = None) -> int:
        with self._lock:
            if platform is None:
                count = len(self._items)
                self._items.clear()
                return count

            retained = deque(
                [item for item in self._items if not any(result.get("platform") == platform.value for result in item.get("results", []))],
                maxlen=self._items.maxlen,
            )
            removed_count = len(self._items) - len(retained)
            self._items = retained
            return removed_count


dry_run_history_store = DryRunHistoryStore(max_items=max(1, settings.dry_run_history_max_items))
