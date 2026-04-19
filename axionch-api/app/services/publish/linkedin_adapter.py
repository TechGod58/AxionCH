import httpx

from app.core.config import settings
from app.db.models import Platform
from app.services.publish.base import PublishAdapter, PublishResult
from app.services.publish.credential_checks import credential_check_tracker


class LinkedInAdapter(PublishAdapter):
    platform = Platform.LINKEDIN

    @staticmethod
    def _resolved_credentials(secrets: dict[str, str | None] | None) -> tuple[str | None, str | None]:
        if secrets is not None:
            return secrets.get("LINKEDIN_ACCESS_TOKEN"), secrets.get("LINKEDIN_AUTHOR_URN")
        return settings.linkedin_access_token, settings.linkedin_author_urn

    def publish(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        access_token, author_urn = self._resolved_credentials(secrets)
        if not access_token or not author_urn:
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message="LinkedIn API credentials not configured",
            )

        payload = {
            "author": author_urn,
            "lifecycleState": "PUBLISHED",
            "specificContent": {
                "com.linkedin.ugc.ShareContent": {
                    "shareCommentary": {"text": body},
                    "shareMediaCategory": "NONE",
                }
            },
            "visibility": {"com.linkedin.ugc.MemberNetworkVisibility": "PUBLIC"},
        }

        headers = {
            "Authorization": f"Bearer {access_token}",
            "Content-Type": "application/json",
            "X-Restli-Protocol-Version": "2.0.0",
        }

        try:
            response = httpx.post(
                "https://api.linkedin.com/v2/ugcPosts",
                headers=headers,
                json=payload,
                timeout=30,
            )
            if response.is_success:
                data = response.json() if response.content else {}
                remote_id = str(data.get("id") or response.headers.get("x-restli-id") or "")
                return PublishResult(
                    platform=self.platform,
                    success=True,
                    remote_post_id=remote_id or None,
                )
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=f"LinkedIn API error {response.status_code}: {response.text[:300]}",
            )
        except Exception as exc:
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=f"LinkedIn publish failed: {exc}",
            )

    def dry_run(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        access_token, _ = self._resolved_credentials(secrets)
        if not access_token:
            error = "LinkedIn API credentials not configured"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error,
            )

        headers = {
            "Authorization": f"Bearer {access_token}",
            "X-Restli-Protocol-Version": "2.0.0",
        }
        try:
            response = httpx.get(
                "https://api.linkedin.com/v2/me",
                headers=headers,
                timeout=30,
            )
            if response.is_success:
                data = response.json() if response.content else {}
                remote_id = str(data.get("id") or "dryrun_li")
                credential_check_tracker.record(self.platform, success=True, error=None)
                return PublishResult(
                    platform=self.platform,
                    success=True,
                    remote_post_id=remote_id,
                )
            error = f"LinkedIn dry-run error {response.status_code}: {response.text[:300]}"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error,
            )
        except Exception as exc:
            error = f"LinkedIn dry-run failed: {exc}"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error,
            )
