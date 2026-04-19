from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import delete, func, select
from sqlalchemy.orm import Session, selectinload

from app.core.auth import AuthIdentity, enforce_user_scope, get_auth_identity, scoped_user_email
from app.core.config import settings
from app.db.models import (
    DryRunHistory,
    DryRunHistoryResult,
    Platform,
    PlatformPost,
    Post,
    PublishDeadLetter,
    PublishJob,
    PublishJobStatus,
    PublishStatus,
    SocialAccount,
    User,
)
from app.db.session import get_db
from app.schemas.post import (
    CreatePostRequest,
    DeadLetterItem,
    DeadLetterListResponse,
    DryRunHistoryClearResponse,
    DryRunHistoryItem,
    DryRunHistoryResponse,
    PlatformPublishResult,
    PostResponse,
    PublishMetricsResponse,
    PublishJobStatusResponse,
    QueuedPostResponse,
    RequeueDeadLetterResponse,
)
from app.services.publish.queue_service import publish_queue_service
from app.services.publish.platform_secrets import resolve_platform_secrets
from app.services.publish.registry import registry
from app.services.publish.retry import publish_with_retry

router = APIRouter()


def _resolve_scoped_user(
    db: Session,
    auth: AuthIdentity,
) -> User | None:
    scoped_email = scoped_user_email(auth)
    if scoped_email is None:
        return None
    return db.execute(select(User).where(User.email == scoped_email)).scalar_one_or_none()


def _resolve_accounts(payload: CreatePostRequest, db: Session, owner_user_id: int | None = None) -> list[SocialAccount]:
    if not payload.account_ids:
        raise HTTPException(status_code=400, detail="At least one account_id is required")

    query = select(SocialAccount).where(SocialAccount.id.in_(payload.account_ids))
    if owner_user_id is not None:
        query = query.where(SocialAccount.user_id == owner_user_id)

    accounts = db.execute(query).scalars().all()

    if owner_user_id is not None and (not accounts or len(accounts) != len(set(payload.account_ids))):
        raise HTTPException(status_code=403, detail="One or more account_ids are not authorized for this user scope")

    if not accounts:
        raise HTTPException(status_code=404, detail="No matching accounts found")
    return accounts


def _ensure_user(email: str, db: Session) -> User:
    user = db.execute(select(User).where(User.email == email)).scalar_one_or_none()
    if user is None:
        user = User(email=email)
        db.add(user)
        db.flush()
    return user


def _status_from_success_count(success_count: int, total: int) -> PublishStatus:
    if success_count == total:
        return PublishStatus.SUCCESS
    if success_count == 0:
        return PublishStatus.FAILED
    return PublishStatus.PARTIAL


def _platform_result(account_id: int, platform: Platform, publish_result) -> PlatformPublishResult:
    publish_status = PublishStatus.SUCCESS if publish_result.success else PublishStatus.FAILED
    return PlatformPublishResult(
        account_id=account_id,
        platform=platform,
        status=publish_status,
        remote_post_id=publish_result.remote_post_id,
        error_message=publish_result.error_message,
    )


def _history_item_from_model(item: DryRunHistory) -> DryRunHistoryItem:
    history_results = [
        PlatformPublishResult(
            account_id=result.account_id,
            platform=result.platform,
            status=result.status,
            remote_post_id=result.remote_post_id,
            error_message=result.error_message,
        )
        for result in item.results
    ]
    account_ids = sorted({result.account_id for result in item.results})
    return DryRunHistoryItem(
        id=str(item.id),
        created_at=item.created_at.isoformat(),
        user_email=item.user_email,
        body_preview=item.body_preview,
        image_url=item.image_url,
        account_ids=account_ids,
        status=item.status,
        results=history_results,
    )


def _trim_dry_run_history(db: Session) -> None:
    max_items = max(1, settings.dry_run_history_max_items)
    total = int(db.execute(select(func.count(DryRunHistory.id))).scalar_one() or 0)
    overflow = total - max_items
    if overflow <= 0:
        return

    oldest_ids = db.execute(
        select(DryRunHistory.id).order_by(DryRunHistory.created_at.asc()).limit(overflow)
    ).scalars().all()
    if not oldest_ids:
        return

    db.execute(delete(DryRunHistoryResult).where(DryRunHistoryResult.history_id.in_(oldest_ids)))
    db.execute(delete(DryRunHistory).where(DryRunHistory.id.in_(oldest_ids)))


def _platform_results_from_rows(rows: list[PlatformPost]) -> list[PlatformPublishResult]:
    return [
        PlatformPublishResult(
            account_id=row.social_account_id,
            platform=row.platform,
            status=row.publish_status,
            remote_post_id=row.remote_post_id,
            error_message=row.error_message,
        )
        for row in rows
    ]


def _serialize_dead_letter(item: PublishDeadLetter) -> DeadLetterItem:
    return DeadLetterItem(
        id=item.id,
        publish_job_id=item.publish_job_id,
        post_id=item.post_id,
        reason=item.reason,
        payload_json=item.payload_json,
        created_at=item.created_at.isoformat(),
    )


@router.post("", response_model=PostResponse)
async def create_and_publish_post(
    payload: CreatePostRequest,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
):
    enforce_user_scope(auth, payload.user_email)
    scoped_email = scoped_user_email(auth)
    scoped_user = _resolve_scoped_user(db, auth)
    effective_email = scoped_email or payload.user_email

    user = _ensure_user(effective_email, db)
    owner_user_id = scoped_user.id if scoped_email is not None and scoped_user is not None else None
    if scoped_email is not None and scoped_user is None:
        owner_user_id = -1
    accounts = _resolve_accounts(payload, db, owner_user_id=owner_user_id)

    post = Post(
        user_id=user.id,
        body=payload.body,
        image_url=payload.image_url,
        status=PublishStatus.READY,
    )
    db.add(post)
    db.flush()

    results: list[PlatformPublishResult] = []
    success_count = 0
    secret_cache = {}

    for account in accounts:
        resolution = secret_cache.get(account.platform)
        if resolution is None:
            resolution = resolve_platform_secrets(
                db,
                platform=account.platform,
                user_email=effective_email,
            )
            secret_cache[account.platform] = resolution

        adapter = registry.get(account.platform, prefer_mock=not resolution.configured)
        publish_result = publish_with_retry(
            adapter,
            payload.body,
            payload.image_url,
            secrets=resolution.values if resolution.configured else None,
        )

        publish_status = PublishStatus.SUCCESS if publish_result.success else PublishStatus.FAILED

        platform_post = PlatformPost(
            post_id=post.id,
            social_account_id=account.id,
            platform=account.platform,
            transformed_body=payload.body,
            publish_status=publish_status,
            remote_post_id=publish_result.remote_post_id,
            error_message=publish_result.error_message,
        )
        db.add(platform_post)

        if publish_result.success:
            success_count += 1

        results.append(_platform_result(account.id, account.platform, publish_result))

    post.status = _status_from_success_count(success_count, len(accounts))

    db.commit()
    db.refresh(post)

    return PostResponse(
        post_id=post.id,
        status=post.status,
        results=results,
        dry_run=False,
    )


@router.post("/queue", response_model=QueuedPostResponse)
async def queue_post_for_publish(
    payload: CreatePostRequest,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
):
    enforce_user_scope(auth, payload.user_email)
    scoped_email = scoped_user_email(auth)
    scoped_user = _resolve_scoped_user(db, auth)
    effective_email = scoped_email or payload.user_email

    user = _ensure_user(effective_email, db)
    owner_user_id = scoped_user.id if scoped_email is not None and scoped_user is not None else None
    if scoped_email is not None and scoped_user is None:
        owner_user_id = -1
    accounts = _resolve_accounts(payload, db, owner_user_id=owner_user_id)

    post = Post(
        user_id=user.id,
        body=payload.body,
        image_url=payload.image_url,
        status=PublishStatus.READY,
    )
    db.add(post)
    db.flush()

    for account in accounts:
        db.add(
            PlatformPost(
                post_id=post.id,
                social_account_id=account.id,
                platform=account.platform,
                transformed_body=payload.body,
                publish_status=PublishStatus.READY,
            )
        )

    job = PublishJob(
        post_id=post.id,
        status=PublishJobStatus.QUEUED,
        attempt_count=0,
        max_attempts=max(1, settings.publish_queue_max_attempts),
        next_run_at=datetime.utcnow(),
    )
    db.add(job)
    db.commit()
    db.refresh(job)

    publish_queue_service.enqueue(job.id)

    return QueuedPostResponse(
        job_id=job.id,
        post_id=post.id,
        status=job.status,
        attempts=job.attempt_count,
        max_attempts=job.max_attempts,
        next_run_at=job.next_run_at.isoformat() if job.next_run_at else None,
        message="Post queued for async publish processing",
    )


@router.get("/jobs/{job_id}", response_model=PublishJobStatusResponse)
def get_publish_job_status(
    job_id: int,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> PublishJobStatusResponse:
    job = db.get(PublishJob, job_id)
    if job is None:
        raise HTTPException(status_code=404, detail="Publish job not found")

    post = db.get(Post, job.post_id)
    if post is None:
        raise HTTPException(status_code=404, detail="Post for publish job was not found")

    scoped_email = scoped_user_email(auth)
    scoped_user = _resolve_scoped_user(db, auth)
    if scoped_email is not None and scoped_user is None:
        raise HTTPException(status_code=403, detail="Publish job is outside authenticated user scope")
    if scoped_user is not None and post.user_id != scoped_user.id:
        raise HTTPException(status_code=403, detail="Publish job is outside authenticated user scope")

    platform_posts = db.execute(
        select(PlatformPost).where(PlatformPost.post_id == post.id).order_by(PlatformPost.id.asc())
    ).scalars().all()

    return PublishJobStatusResponse(
        job_id=job.id,
        post_id=post.id,
        status=job.status,
        attempts=job.attempt_count,
        max_attempts=job.max_attempts,
        next_run_at=job.next_run_at.isoformat() if job.next_run_at else None,
        last_error=job.last_error,
        post_status=post.status,
        results=_platform_results_from_rows(platform_posts),
    )


@router.get("/metrics", response_model=PublishMetricsResponse)
def get_publish_metrics(
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> PublishMetricsResponse:
    scoped_email = scoped_user_email(auth)
    scoped_user = _resolve_scoped_user(db, auth)
    scoped_user_id = scoped_user.id if scoped_user is not None else None
    if scoped_email is not None and scoped_user is None:
        scoped_user_id = -1

    status_counts_query = select(PublishJob.status, func.count(PublishJob.id)).join(Post, Post.id == PublishJob.post_id)
    if scoped_user_id is not None:
        status_counts_query = status_counts_query.where(Post.user_id == scoped_user_id)
    status_counts_rows = db.execute(status_counts_query.group_by(PublishJob.status)).all()
    status_counts = {row[0].value: int(row[1]) for row in status_counts_rows}

    dead_letter_count_query = select(func.count(PublishDeadLetter.id)).join(Post, Post.id == PublishDeadLetter.post_id)
    if scoped_user_id is not None:
        dead_letter_count_query = dead_letter_count_query.where(Post.user_id == scoped_user_id)
    dead_letter_count = int(db.execute(dead_letter_count_query).scalar_one() or 0)

    active_jobs_query = (
        select(func.count(PublishJob.id))
        .join(Post, Post.id == PublishJob.post_id)
        .where(PublishJob.status.in_([PublishJobStatus.QUEUED, PublishJobStatus.PROCESSING, PublishJobStatus.RETRYING]))
    )
    if scoped_user_id is not None:
        active_jobs_query = active_jobs_query.where(Post.user_id == scoped_user_id)
    active_jobs = int(
        db.execute(active_jobs_query).scalar_one()
        or 0
    )
    counters = publish_queue_service.worker_counters()

    return PublishMetricsResponse(
        queue_depth=publish_queue_service.queue_depth(),
        active_jobs=active_jobs,
        jobs_by_status=status_counts,
        dead_letter_count=dead_letter_count,
        worker_processed_total=counters["processed_total"],
        worker_success_total=counters["success_total"],
        worker_failure_total=counters["failure_total"],
    )


@router.get("/dead-letters", response_model=DeadLetterListResponse)
def get_dead_letters(
    limit: int = Query(default=50, ge=1, le=200),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> DeadLetterListResponse:
    scoped_email = scoped_user_email(auth)
    scoped_user = _resolve_scoped_user(db, auth)
    scoped_user_id = scoped_user.id if scoped_user is not None else None
    if scoped_email is not None and scoped_user is None:
        scoped_user_id = -1

    query = select(PublishDeadLetter).join(Post, Post.id == PublishDeadLetter.post_id)
    if scoped_user_id is not None:
        query = query.where(Post.user_id == scoped_user_id)

    items = db.execute(
        query.order_by(PublishDeadLetter.created_at.desc()).limit(limit)
    ).scalars().all()
    return DeadLetterListResponse(
        items=[_serialize_dead_letter(item) for item in items],
        total_returned=len(items),
        limit=limit,
    )


@router.post("/dead-letters/{dead_letter_id}/requeue", response_model=RequeueDeadLetterResponse)
def requeue_dead_letter(
    dead_letter_id: int,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> RequeueDeadLetterResponse:
    dead_letter = db.get(PublishDeadLetter, dead_letter_id)
    if dead_letter is None:
        raise HTTPException(status_code=404, detail="Dead letter not found")

    post = db.get(Post, dead_letter.post_id)
    if post is None:
        raise HTTPException(status_code=404, detail="Post for dead letter not found")

    scoped_email = scoped_user_email(auth)
    scoped_user = _resolve_scoped_user(db, auth)
    if scoped_email is not None and scoped_user is None:
        raise HTTPException(status_code=403, detail="Dead letter is outside authenticated user scope")
    if scoped_user is not None and post.user_id != scoped_user.id:
        raise HTTPException(status_code=403, detail="Dead letter is outside authenticated user scope")

    max_attempts = max(1, settings.publish_queue_max_attempts)
    job = PublishJob(
        post_id=post.id,
        status=PublishJobStatus.QUEUED,
        attempt_count=0,
        max_attempts=max_attempts,
        next_run_at=datetime.utcnow(),
        last_error=None,
    )
    db.add(job)
    db.commit()
    db.refresh(job)

    publish_queue_service.enqueue(job.id)

    return RequeueDeadLetterResponse(
        dead_letter_id=dead_letter.id,
        new_job_id=job.id,
        post_id=post.id,
        status=job.status,
        message="Dead letter post re-queued for processing",
    )


@router.post("/dry-run", response_model=PostResponse)
async def dry_run_publish_post(
    payload: CreatePostRequest,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
):
    enforce_user_scope(auth, payload.user_email)
    scoped_email = scoped_user_email(auth)
    scoped_user = _resolve_scoped_user(db, auth)
    effective_email = scoped_email or payload.user_email
    owner_user_id = scoped_user.id if scoped_email is not None and scoped_user is not None else None
    if scoped_email is not None and scoped_user is None:
        owner_user_id = -1
    accounts = _resolve_accounts(payload, db, owner_user_id=owner_user_id)

    results: list[PlatformPublishResult] = []
    success_count = 0
    secret_cache = {}

    for account in accounts:
        resolution = secret_cache.get(account.platform)
        if resolution is None:
            resolution = resolve_platform_secrets(
                db,
                platform=account.platform,
                user_email=effective_email,
            )
            secret_cache[account.platform] = resolution

        adapter = registry.get(account.platform, prefer_mock=not resolution.configured)
        publish_result = adapter.dry_run(
            payload.body,
            payload.image_url,
            secrets=resolution.values if resolution.configured else None,
        )

        if publish_result.success:
            success_count += 1

        results.append(_platform_result(account.id, account.platform, publish_result))

    response = PostResponse(
        post_id=None,
        status=_status_from_success_count(success_count, len(accounts)),
        results=results,
        dry_run=True,
    )

    history = DryRunHistory(
        user_email=effective_email,
        body_preview=payload.body[:120],
        image_url=payload.image_url,
        status=response.status,
    )
    db.add(history)
    db.flush()

    for result in results:
        db.add(
            DryRunHistoryResult(
                history_id=history.id,
                account_id=result.account_id,
                platform=result.platform,
                status=result.status,
                remote_post_id=result.remote_post_id,
                error_message=result.error_message,
            )
        )

    _trim_dry_run_history(db)
    db.commit()

    return response


@router.get("/dry-run-history", response_model=DryRunHistoryResponse)
async def get_dry_run_history(
    limit: int = Query(default=25, ge=1, le=200),
    platform: Platform | None = Query(default=None),
    success_only: bool | None = Query(default=None),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> DryRunHistoryResponse:
    query = (
        select(DryRunHistory)
        .options(selectinload(DryRunHistory.results))
        .order_by(DryRunHistory.created_at.desc())
    )

    scoped_email = scoped_user_email(auth)
    if scoped_email is not None:
        query = query.where(DryRunHistory.user_email == scoped_email)

    if platform is not None:
        query = query.join(DryRunHistory.results).where(DryRunHistoryResult.platform == platform).distinct()

    if success_only is not None:
        if success_only:
            query = query.where(DryRunHistory.status == PublishStatus.SUCCESS)
        else:
            query = query.where(DryRunHistory.status != PublishStatus.SUCCESS)

    query = query.limit(limit)
    items = db.execute(query).scalars().all()

    return DryRunHistoryResponse(
        items=[_history_item_from_model(item) for item in items],
        total_returned=len(items),
        limit=limit,
        platform=platform,
        success_only=success_only,
    )


@router.delete("/dry-run-history", response_model=DryRunHistoryClearResponse)
async def clear_dry_run_history(
    platform: Platform | None = Query(default=None),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> DryRunHistoryClearResponse:
    scoped_email = scoped_user_email(auth)

    if platform is None:
        query = select(DryRunHistory.id)
        if scoped_email is not None:
            query = query.where(DryRunHistory.user_email == scoped_email)
        history_ids = db.execute(query).scalars().all()
        cleared_count = len(history_ids)
        if history_ids:
            db.execute(delete(DryRunHistoryResult).where(DryRunHistoryResult.history_id.in_(history_ids)))
            db.execute(delete(DryRunHistory).where(DryRunHistory.id.in_(history_ids)))
    else:
        query = (
            select(DryRunHistory.id)
            .join(DryRunHistory.results)
            .where(DryRunHistoryResult.platform == platform)
            .distinct()
        )
        if scoped_email is not None:
            query = query.where(DryRunHistory.user_email == scoped_email)
        history_ids = db.execute(query).scalars().all()
        cleared_count = len(history_ids)
        if history_ids:
            db.execute(delete(DryRunHistoryResult).where(DryRunHistoryResult.history_id.in_(history_ids)))
            db.execute(delete(DryRunHistory).where(DryRunHistory.id.in_(history_ids)))

    db.commit()
    remaining_query = select(func.count(DryRunHistory.id))
    if scoped_email is not None:
        remaining_query = remaining_query.where(DryRunHistory.user_email == scoped_email)
    remaining_count = int(db.execute(remaining_query).scalar_one() or 0)

    return DryRunHistoryClearResponse(
        cleared_count=cleared_count,
        remaining_count=remaining_count,
        platform=platform,
    )
