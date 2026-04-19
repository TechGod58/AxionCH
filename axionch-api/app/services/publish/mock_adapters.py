from datetime import datetime

from app.db.models import Platform
from app.services.publish.base import PublishAdapter, PublishResult
from app.services.publish.credential_checks import credential_check_tracker


class MockXAdapter(PublishAdapter):
    platform = Platform.X

    def publish(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        if len(body) > 280:
            return PublishResult(platform=self.platform, success=False, error_message="Body exceeds X length limit")
        return PublishResult(
            platform=self.platform,
            success=True,
            remote_post_id=f"x_{int(datetime.utcnow().timestamp())}",
        )

    def dry_run(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        if len(body) > 280:
            error = "Body exceeds X length limit"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(platform=self.platform, success=False, error_message=error)
        error = "Using mock adapter; real X credentials are not configured"
        credential_check_tracker.record(self.platform, success=False, error=error)
        return PublishResult(
            platform=self.platform,
            success=False,
            error_message=error,
        )


class MockLinkedInAdapter(PublishAdapter):
    platform = Platform.LINKEDIN

    def publish(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        return PublishResult(
            platform=self.platform,
            success=True,
            remote_post_id=f"li_{int(datetime.utcnow().timestamp())}",
        )

    def dry_run(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        error = "Using mock adapter; real LinkedIn credentials are not configured"
        credential_check_tracker.record(self.platform, success=False, error=error)
        return PublishResult(
            platform=self.platform,
            success=False,
            error_message=error,
        )


class MockInstagramAdapter(PublishAdapter):
    platform = Platform.INSTAGRAM

    def publish(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        if not image_url:
            return PublishResult(platform=self.platform, success=False, error_message="Instagram requires an image in this starter")
        return PublishResult(
            platform=self.platform,
            success=True,
            remote_post_id=f"ig_{int(datetime.utcnow().timestamp())}",
        )

    def dry_run(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        if not image_url:
            error = "Instagram requires an image in this starter"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(platform=self.platform, success=False, error_message=error)
        error = "Using mock adapter; real Instagram credentials are not configured"
        credential_check_tracker.record(self.platform, success=False, error=error)
        return PublishResult(
            platform=self.platform,
            success=False,
            error_message=error,
        )
