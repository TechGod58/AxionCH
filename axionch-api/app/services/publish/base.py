from abc import ABC, abstractmethod
from dataclasses import dataclass

from app.db.models import Platform


@dataclass
class PublishResult:
    platform: Platform
    success: bool
    remote_post_id: str | None = None
    error_message: str | None = None


class PublishAdapter(ABC):
    platform: Platform

    @abstractmethod
    def publish(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        raise NotImplementedError

    @abstractmethod
    def dry_run(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        raise NotImplementedError
