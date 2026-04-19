import httpx

from app.core.config import settings
from app.db.models import Platform
from app.services.publish.base import PublishAdapter, PublishResult
from app.services.publish.credential_checks import credential_check_tracker


class InstagramAdapter(PublishAdapter):
    platform = Platform.INSTAGRAM

    @staticmethod
    def _resolved_credentials(secrets: dict[str, str | None] | None) -> tuple[str | None, str | None]:
        if secrets is not None:
            return secrets.get("INSTAGRAM_ACCESS_TOKEN"), secrets.get("INSTAGRAM_BUSINESS_ACCOUNT_ID")
        return settings.instagram_access_token, settings.instagram_business_account_id

    def publish(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        access_token, business_account_id = self._resolved_credentials(secrets)
        if not access_token or not business_account_id:
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message="Instagram API credentials not configured",
            )
        if not image_url:
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message="Instagram requires an image URL",
            )

        base = f"https://graph.facebook.com/v20.0/{business_account_id}"
        try:
            create_response = httpx.post(
                f"{base}/media",
                data={
                    "image_url": image_url,
                    "caption": body,
                    "access_token": access_token,
                },
                timeout=30,
            )
            if not create_response.is_success:
                return PublishResult(
                    platform=self.platform,
                    success=False,
                    error_message=f"Instagram create media error {create_response.status_code}: {create_response.text[:300]}",
                )

            creation_id = (create_response.json() or {}).get("id")
            if not creation_id:
                return PublishResult(
                    platform=self.platform,
                    success=False,
                    error_message="Instagram create media returned no creation id",
                )

            publish_response = httpx.post(
                f"{base}/media_publish",
                data={
                    "creation_id": creation_id,
                    "access_token": access_token,
                },
                timeout=30,
            )
            if publish_response.is_success:
                remote_id = str((publish_response.json() or {}).get("id") or "")
                return PublishResult(
                    platform=self.platform,
                    success=True,
                    remote_post_id=remote_id or None,
                )
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=f"Instagram publish error {publish_response.status_code}: {publish_response.text[:300]}",
            )
        except Exception as exc:
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=f"Instagram publish failed: {exc}",
            )

    def dry_run(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        access_token, business_account_id = self._resolved_credentials(secrets)
        if not access_token or not business_account_id:
            error = "Instagram API credentials not configured"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error,
            )
        if not image_url:
            error = "Instagram requires an image URL"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error,
            )

        try:
            response = httpx.get(
                f"https://graph.facebook.com/v20.0/{business_account_id}",
                params={
                    "fields": "id,username",
                    "access_token": access_token,
                },
                timeout=30,
            )
            if response.is_success:
                data = response.json() if response.content else {}
                remote_id = str(data.get("id") or "dryrun_ig")
                credential_check_tracker.record(self.platform, success=True, error=None)
                return PublishResult(
                    platform=self.platform,
                    success=True,
                    remote_post_id=remote_id,
                )
            error = f"Instagram dry-run error {response.status_code}: {response.text[:300]}"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error,
            )
        except Exception as exc:
            error = f"Instagram dry-run failed: {exc}"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error,
            )
