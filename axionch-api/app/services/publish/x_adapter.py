import tweepy

from app.core.config import settings
from app.db.models import Platform
from app.services.publish.base import PublishAdapter, PublishResult
from app.services.publish.credential_checks import credential_check_tracker


class XAdapter(PublishAdapter):
    platform = Platform.X

    @staticmethod
    def _resolved_credentials(secrets: dict[str, str | None] | None) -> dict[str, str | None]:
        if secrets is not None:
            return {
                "X_API_KEY": secrets.get("X_API_KEY"),
                "X_API_SECRET": secrets.get("X_API_SECRET"),
                "X_ACCESS_TOKEN": secrets.get("X_ACCESS_TOKEN"),
                "X_ACCESS_TOKEN_SECRET": secrets.get("X_ACCESS_TOKEN_SECRET"),
            }
        return {
            "X_API_KEY": settings.x_api_key,
            "X_API_SECRET": settings.x_api_secret,
            "X_ACCESS_TOKEN": settings.x_access_token,
            "X_ACCESS_TOKEN_SECRET": settings.x_access_token_secret,
        }

    def _build_client(self, secrets: dict[str, str | None] | None) -> tweepy.Client | None:
        creds = self._resolved_credentials(secrets)
        if not all([creds["X_API_KEY"], creds["X_API_SECRET"], creds["X_ACCESS_TOKEN"], creds["X_ACCESS_TOKEN_SECRET"]]):
            return None
        return tweepy.Client(
            consumer_key=creds["X_API_KEY"],
            consumer_secret=creds["X_API_SECRET"],
            access_token=creds["X_ACCESS_TOKEN"],
            access_token_secret=creds["X_ACCESS_TOKEN_SECRET"],
        )

    def publish(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        client = self._build_client(secrets)
        if not client:
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message="X API credentials not configured"
            )

        try:
            # v2 create_tweet call
            response = client.create_tweet(text=body)

            # The response contains the data for the newly created tweet
            if response and response.data:
                tweet_id = response.data.get('id')
                return PublishResult(
                    platform=self.platform,
                    success=True,
                    remote_post_id=str(tweet_id)
                )
            else:
                return PublishResult(
                    platform=self.platform,
                    success=False,
                    error_message="Received empty response from X API"
                )

        except tweepy.TweepyException as e:
            # Handle specific tweepy exceptions
            error_msg = str(e)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=f"X API error: {error_msg}"
            )
        except Exception as e:
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=f"Unexpected error publishing to X: {str(e)}"
            )

    def dry_run(
        self,
        body: str,
        image_url: str | None,
        secrets: dict[str, str | None] | None = None,
    ) -> PublishResult:
        client = self._build_client(secrets)
        if not client:
            error = "X API credentials not configured"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error
            )
        if len(body) > 280:
            error = "Body exceeds X length limit"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error
            )

        try:
            response = client.get_me(user_auth=True)
            if response and response.data:
                account_id = str(response.data.get("id") or "")
                credential_check_tracker.record(self.platform, success=True, error=None)
                return PublishResult(
                    platform=self.platform,
                    success=True,
                    remote_post_id=account_id or "dryrun_x",
                )
            error = "X credential check returned empty profile response"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error
            )
        except tweepy.TweepyException as e:
            error = f"X dry-run error: {str(e)}"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error
            )
        except Exception as e:
            error = f"Unexpected X dry-run error: {str(e)}"
            credential_check_tracker.record(self.platform, success=False, error=error)
            return PublishResult(
                platform=self.platform,
                success=False,
                error_message=error
            )
