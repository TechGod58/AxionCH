from pydantic import BaseModel, Field
from app.db.models import Platform, PublishJobStatus, PublishStatus


class CreatePostRequest(BaseModel):
    user_email: str
    body: str = Field(min_length=1, max_length=5000)
    image_url: str | None = None
    account_ids: list[int] = Field(default_factory=list)


class PlatformPublishResult(BaseModel):
    account_id: int
    platform: Platform
    status: PublishStatus
    remote_post_id: str | None = None
    error_message: str | None = None


class PostResponse(BaseModel):
    post_id: int | None = None
    status: PublishStatus
    results: list[PlatformPublishResult]
    dry_run: bool = False


class DryRunHistoryItem(BaseModel):
    id: str
    created_at: str
    user_email: str
    body_preview: str
    image_url: str | None = None
    account_ids: list[int]
    status: PublishStatus
    results: list[PlatformPublishResult]


class DryRunHistoryResponse(BaseModel):
    items: list[DryRunHistoryItem]
    total_returned: int
    limit: int
    platform: Platform | None = None
    success_only: bool | None = None


class DryRunHistoryClearResponse(BaseModel):
    cleared_count: int
    remaining_count: int
    platform: Platform | None = None


class QueuedPostResponse(BaseModel):
    job_id: int
    post_id: int
    status: PublishJobStatus
    attempts: int
    max_attempts: int
    next_run_at: str | None = None
    message: str


class PublishJobStatusResponse(BaseModel):
    job_id: int
    post_id: int
    status: PublishJobStatus
    attempts: int
    max_attempts: int
    next_run_at: str | None = None
    last_error: str | None = None
    post_status: PublishStatus
    results: list[PlatformPublishResult]


class DeadLetterItem(BaseModel):
    id: int
    publish_job_id: int | None = None
    post_id: int
    reason: str
    payload_json: str | None = None
    created_at: str


class DeadLetterListResponse(BaseModel):
    items: list[DeadLetterItem]
    total_returned: int
    limit: int


class RequeueDeadLetterResponse(BaseModel):
    dead_letter_id: int
    new_job_id: int
    post_id: int
    status: PublishJobStatus
    message: str


class PublishMetricsResponse(BaseModel):
    queue_depth: int
    active_jobs: int
    jobs_by_status: dict[str, int]
    dead_letter_count: int
    worker_processed_total: int
    worker_success_total: int
    worker_failure_total: int
