from datetime import datetime, timedelta
import json
from queue import Empty, Queue
from threading import Event, Thread, Timer

from sqlalchemy import select

from app.core.config import settings
from app.db.models import PlatformPost, Post, PublishDeadLetter, PublishJob, PublishJobStatus, PublishStatus, User
from app.db.session import SessionLocal
from app.services.publish.platform_secrets import resolve_platform_secrets
from app.services.publish.registry import registry
from app.services.publish.retry import publish_with_retry


def _status_from_success_count(success_count: int, total: int) -> PublishStatus:
    if success_count == total:
        return PublishStatus.SUCCESS
    if success_count == 0:
        return PublishStatus.FAILED
    return PublishStatus.PARTIAL


class PublishQueueService:
    def __init__(self) -> None:
        self._queue: Queue[int] = Queue()
        self._stop_event = Event()
        self._thread: Thread | None = None
        self._processed_total = 0
        self._success_total = 0
        self._failure_total = 0

    def start(self) -> None:
        if self._thread and self._thread.is_alive():
            return
        self._stop_event.clear()
        self._thread = Thread(target=self._worker_loop, daemon=True)
        self._thread.start()
        self.enqueue_due_jobs()

    def stop(self) -> None:
        self._stop_event.set()
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=2.0)

    def enqueue(self, job_id: int) -> None:
        self._queue.put(job_id)

    def queue_depth(self) -> int:
        return self._queue.qsize()

    def worker_counters(self) -> dict[str, int]:
        return {
            "processed_total": self._processed_total,
            "success_total": self._success_total,
            "failure_total": self._failure_total,
        }

    def enqueue_due_jobs(self) -> None:
        db = SessionLocal()
        try:
            now = datetime.utcnow()
            due_job_ids = db.execute(
                select(PublishJob.id).where(
                    PublishJob.status.in_([PublishJobStatus.QUEUED, PublishJobStatus.RETRYING]),
                    (PublishJob.next_run_at.is_(None)) | (PublishJob.next_run_at <= now),
                )
            ).scalars().all()
        finally:
            db.close()

        for job_id in due_job_ids:
            self.enqueue(int(job_id))

    def _worker_loop(self) -> None:
        while not self._stop_event.is_set():
            try:
                job_id = self._queue.get(timeout=0.5)
            except Empty:
                self.enqueue_due_jobs()
                continue

            try:
                self._process_job(job_id)
            finally:
                self._queue.task_done()

    def _schedule_retry(self, job_id: int, delay_seconds: float) -> None:
        timer = Timer(delay_seconds, lambda: self.enqueue(job_id))
        timer.daemon = True
        timer.start()

    def _process_job(self, job_id: int) -> None:
        db = SessionLocal()
        try:
            self._processed_total += 1
            job = db.get(PublishJob, job_id)
            if job is None:
                self._failure_total += 1
                return
            if job.status in [PublishJobStatus.SUCCESS, PublishJobStatus.FAILED, PublishJobStatus.PARTIAL]:
                return
            if job.next_run_at and job.next_run_at > datetime.utcnow():
                delay = max((job.next_run_at - datetime.utcnow()).total_seconds(), 0.1)
                self._schedule_retry(job.id, delay)
                return

            post = db.get(Post, job.post_id)
            if post is None:
                job.status = PublishJobStatus.FAILED
                job.last_error = "Post not found for queued job"
                self._create_dead_letter(db, job=job, reason=job.last_error, post_id=job.post_id)
                self._failure_total += 1
                db.commit()
                return

            platform_posts = db.execute(
                select(PlatformPost).where(PlatformPost.post_id == post.id).order_by(PlatformPost.id.asc())
            ).scalars().all()
            if not platform_posts:
                job.status = PublishJobStatus.FAILED
                job.last_error = "No platform targets found for queued post"
                self._create_dead_letter(db, job=job, reason=job.last_error, post_id=post.id)
                self._failure_total += 1
                db.commit()
                return

            owner = db.get(User, post.user_id)
            owner_email = owner.email if owner is not None else None
            secret_cache = {}

            job.status = PublishJobStatus.PROCESSING
            job.attempt_count += 1
            db.flush()

            failures: list[str] = []
            for platform_post in platform_posts:
                if platform_post.publish_status == PublishStatus.SUCCESS:
                    continue

                resolution = secret_cache.get(platform_post.platform)
                if resolution is None:
                    resolution = resolve_platform_secrets(
                        db,
                        platform=platform_post.platform,
                        user_email=owner_email,
                    )
                    secret_cache[platform_post.platform] = resolution

                adapter = registry.get(platform_post.platform, prefer_mock=not resolution.configured)
                publish_result = publish_with_retry(
                    adapter,
                    post.body,
                    post.image_url,
                    secrets=resolution.values if resolution.configured else None,
                )

                platform_post.publish_status = PublishStatus.SUCCESS if publish_result.success else PublishStatus.FAILED
                platform_post.remote_post_id = publish_result.remote_post_id
                platform_post.error_message = publish_result.error_message

                if not publish_result.success and publish_result.error_message:
                    failures.append(f"{platform_post.platform.value}: {publish_result.error_message}")

            success_count = sum(1 for item in platform_posts if item.publish_status == PublishStatus.SUCCESS)
            post.status = _status_from_success_count(success_count, len(platform_posts))

            if post.status == PublishStatus.SUCCESS:
                job.status = PublishJobStatus.SUCCESS
                job.next_run_at = None
                job.last_error = None
                self._success_total += 1
                db.commit()
                return

            if job.attempt_count >= job.max_attempts:
                job.status = PublishJobStatus.PARTIAL if post.status == PublishStatus.PARTIAL else PublishJobStatus.FAILED
                job.next_run_at = None
                job.last_error = "; ".join(failures)[:1000] if failures else "Publish failed"
                self._create_dead_letter(
                    db,
                    job=job,
                    reason=job.last_error or "Publish failed",
                    post_id=post.id,
                )
                self._failure_total += 1
                db.commit()
                return

            delay_seconds = max(0.1, settings.publish_queue_base_backoff_seconds) * (2 ** (job.attempt_count - 1))
            job.status = PublishJobStatus.RETRYING
            job.next_run_at = datetime.utcnow() + timedelta(seconds=delay_seconds)
            job.last_error = "; ".join(failures)[:1000] if failures else "Retrying failed publish targets"
            db.commit()

            self._schedule_retry(job.id, delay_seconds)
        finally:
            db.close()

    @staticmethod
    def _create_dead_letter(db, *, job: PublishJob, reason: str, post_id: int) -> None:
        payload = {
            "job_id": job.id,
            "attempt_count": job.attempt_count,
            "max_attempts": job.max_attempts,
            "next_run_at": job.next_run_at.isoformat() if job.next_run_at else None,
            "status": job.status.value,
        }
        db.add(
            PublishDeadLetter(
                publish_job_id=job.id,
                post_id=post_id,
                reason=reason[:255],
                payload_json=json.dumps(payload),
            )
        )


publish_queue_service = PublishQueueService()
