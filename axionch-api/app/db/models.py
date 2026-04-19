from datetime import datetime
from enum import Enum

from sqlalchemy import DateTime, Enum as SqlEnum, ForeignKey, Integer, String, Text
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


class Base(DeclarativeBase):
    pass


class Platform(str, Enum):
    X = "x"
    LINKEDIN = "linkedin"
    INSTAGRAM = "instagram"


class ConnectionStatus(str, Enum):
    CONNECTED = "connected"
    EXPIRED = "expired"
    DISCONNECTED = "disconnected"


class PublishStatus(str, Enum):
    DRAFT = "draft"
    READY = "ready"
    SUCCESS = "success"
    FAILED = "failed"
    PARTIAL = "partial"


class PublishJobStatus(str, Enum):
    QUEUED = "queued"
    PROCESSING = "processing"
    RETRYING = "retrying"
    SUCCESS = "success"
    FAILED = "failed"
    PARTIAL = "partial"


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    accounts: Mapped[list["SocialAccount"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    posts: Mapped[list["Post"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    api_credentials: Mapped[list["ApiCredential"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    vault_entries: Mapped[list["VaultEntry"]] = relationship(back_populates="user", cascade="all, delete-orphan")


class SocialAccount(Base):
    __tablename__ = "social_accounts"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    platform: Mapped[Platform] = mapped_column(SqlEnum(Platform), index=True)
    handle: Mapped[str] = mapped_column(String(120))
    access_token: Mapped[str] = mapped_column(Text)
    refresh_token: Mapped[str | None] = mapped_column(Text, nullable=True)
    token_type: Mapped[str | None] = mapped_column(String(40), nullable=True)
    token_scope: Mapped[str | None] = mapped_column(Text, nullable=True)
    token_expires_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    status: Mapped[ConnectionStatus] = mapped_column(SqlEnum(ConnectionStatus), default=ConnectionStatus.CONNECTED)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="accounts")
    platform_posts: Mapped[list["PlatformPost"]] = relationship(back_populates="account", cascade="all, delete-orphan")


class Post(Base):
    __tablename__ = "posts"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    body: Mapped[str] = mapped_column(Text)
    image_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    status: Mapped[PublishStatus] = mapped_column(SqlEnum(PublishStatus), default=PublishStatus.DRAFT)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="posts")
    platform_posts: Mapped[list["PlatformPost"]] = relationship(back_populates="post", cascade="all, delete-orphan")
    publish_jobs: Mapped[list["PublishJob"]] = relationship(back_populates="post", cascade="all, delete-orphan")


class PlatformPost(Base):
    __tablename__ = "platform_posts"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    post_id: Mapped[int] = mapped_column(ForeignKey("posts.id"), index=True)
    social_account_id: Mapped[int] = mapped_column(ForeignKey("social_accounts.id"), index=True)
    platform: Mapped[Platform] = mapped_column(SqlEnum(Platform), index=True)
    transformed_body: Mapped[str] = mapped_column(Text)
    publish_status: Mapped[PublishStatus] = mapped_column(SqlEnum(PublishStatus), default=PublishStatus.DRAFT)
    remote_post_id: Mapped[str | None] = mapped_column(String(255), nullable=True)
    error_message: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    post: Mapped["Post"] = relationship(back_populates="platform_posts")
    account: Mapped["SocialAccount"] = relationship(back_populates="platform_posts")


class DryRunHistory(Base):
    __tablename__ = "dry_run_history"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_email: Mapped[str] = mapped_column(String(255), index=True)
    body_preview: Mapped[str] = mapped_column(String(120))
    image_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    status: Mapped[PublishStatus] = mapped_column(SqlEnum(PublishStatus), index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)

    results: Mapped[list["DryRunHistoryResult"]] = relationship(
        back_populates="history",
        cascade="all, delete-orphan",
    )


class DryRunHistoryResult(Base):
    __tablename__ = "dry_run_history_results"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    history_id: Mapped[int] = mapped_column(ForeignKey("dry_run_history.id"), index=True)
    account_id: Mapped[int] = mapped_column(Integer, index=True)
    platform: Mapped[Platform] = mapped_column(SqlEnum(Platform), index=True)
    status: Mapped[PublishStatus] = mapped_column(SqlEnum(PublishStatus), index=True)
    remote_post_id: Mapped[str | None] = mapped_column(String(255), nullable=True)
    error_message: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    history: Mapped["DryRunHistory"] = relationship(back_populates="results")


class PublishJob(Base):
    __tablename__ = "publish_jobs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    post_id: Mapped[int] = mapped_column(ForeignKey("posts.id"), index=True)
    status: Mapped[PublishJobStatus] = mapped_column(SqlEnum(PublishJobStatus), index=True, default=PublishJobStatus.QUEUED)
    attempt_count: Mapped[int] = mapped_column(Integer, default=0)
    max_attempts: Mapped[int] = mapped_column(Integer, default=3)
    last_error: Mapped[str | None] = mapped_column(Text, nullable=True)
    next_run_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    post: Mapped["Post"] = relationship(back_populates="publish_jobs")


class PublishDeadLetter(Base):
    __tablename__ = "publish_dead_letters"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    publish_job_id: Mapped[int | None] = mapped_column(ForeignKey("publish_jobs.id"), nullable=True, index=True)
    post_id: Mapped[int] = mapped_column(ForeignKey("posts.id"), index=True)
    reason: Mapped[str] = mapped_column(String(255))
    payload_json: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)


class ApiCredential(Base):
    __tablename__ = "api_credentials"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    key_id: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    key_hash: Mapped[str] = mapped_column(String(128), index=True)
    label: Mapped[str] = mapped_column(String(120))
    is_active: Mapped[bool] = mapped_column(default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)
    last_used_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)

    user: Mapped["User"] = relationship(back_populates="api_credentials")


class VaultEntry(Base):
    __tablename__ = "vault_entries"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    service_name: Mapped[str] = mapped_column(String(120), index=True)
    username_encrypted: Mapped[str] = mapped_column(Text)
    password_encrypted: Mapped[str] = mapped_column(Text)
    notes_encrypted: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="vault_entries")


class VaultEntryQuarantine(Base):
    __tablename__ = "vault_entry_quarantine"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    original_vault_entry_id: Mapped[int] = mapped_column(Integer, index=True)
    user_id: Mapped[int | None] = mapped_column(ForeignKey("users.id"), nullable=True, index=True)
    service_name: Mapped[str] = mapped_column(String(120), index=True)
    username_encrypted: Mapped[str] = mapped_column(Text)
    password_encrypted: Mapped[str] = mapped_column(Text)
    notes_encrypted: Mapped[str | None] = mapped_column(Text, nullable=True)
    quarantine_reason: Mapped[str] = mapped_column(Text)
    quarantined_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)
