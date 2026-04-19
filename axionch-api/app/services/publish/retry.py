from time import sleep

from app.core.config import settings
from app.services.publish.base import PublishAdapter, PublishResult

NON_RETRYABLE_HINTS = (
    "not configured",
    "requires an image",
    "requires an image url",
    "length limit",
    "exceeds",
)


def _is_retryable_failure(result: PublishResult) -> bool:
    if result.success:
        return False
    message = (result.error_message or "").lower()
    if not message:
        return True
    return not any(hint in message for hint in NON_RETRYABLE_HINTS)


def publish_with_retry(
    adapter: PublishAdapter,
    body: str,
    image_url: str | None,
    secrets: dict[str, str | None] | None = None,
) -> PublishResult:
    max_attempts = max(1, settings.publish_retry_max_attempts)
    backoff = max(0.1, settings.publish_retry_backoff_seconds)

    result = adapter.publish(body, image_url, secrets=secrets)
    if max_attempts == 1:
        return result

    for attempt in range(2, max_attempts + 1):
        if not _is_retryable_failure(result):
            break
        sleep(backoff * (2 ** (attempt - 2)))
        result = adapter.publish(body, image_url, secrets=secrets)
    return result
