from datetime import datetime, timedelta
from threading import Event, Thread
from time import sleep

from sqlalchemy import and_, or_, select

from app.core.config import settings
from app.db.models import ConnectionStatus, Platform, SocialAccount
from app.db.session import SessionLocal
from app.services.oauth.exchange import OAuthExchangeError, refresh_oauth_token
from app.services.oauth.token_crypto import decrypt_token_value, store_account_tokens


class OAuthRefreshWorkerService:
    def __init__(self) -> None:
        self._thread: Thread | None = None
        self._stop_event = Event()
        self._refreshed_total = 0
        self._failed_total = 0
        self._scanned_total = 0
        self._next_retry_by_account: dict[int, datetime] = {}

    def start(self) -> None:
        if not settings.oauth_refresh_enabled:
            return
        if self._thread and self._thread.is_alive():
            return
        self._stop_event.clear()
        self._thread = Thread(target=self._worker_loop, daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._stop_event.set()
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=2.0)

    def metrics(self) -> dict[str, int]:
        return {
            "scanned_total": self._scanned_total,
            "refreshed_total": self._refreshed_total,
            "failed_total": self._failed_total,
        }

    def run_once(self) -> None:
        if not settings.oauth_refresh_enabled:
            return

        db = SessionLocal()
        try:
            now = datetime.utcnow()
            refresh_before = now + timedelta(seconds=max(0.0, settings.oauth_refresh_ahead_seconds))

            due_accounts = db.execute(
                select(SocialAccount).where(
                    SocialAccount.token_expires_at.is_not(None),
                    SocialAccount.token_expires_at <= refresh_before,
                    SocialAccount.status == ConnectionStatus.CONNECTED,
                    or_(
                        SocialAccount.refresh_token.is_not(None),
                        and_(
                            SocialAccount.platform == Platform.INSTAGRAM,
                            SocialAccount.access_token.is_not(None),
                        ),
                    ),
                )
            ).scalars().all()

            self._scanned_total += len(due_accounts)

            for account in due_accounts:
                next_retry = self._next_retry_by_account.get(account.id)
                if next_retry is not None and next_retry > now:
                    continue

                try:
                    refresh_token = (decrypt_token_value(account.refresh_token) or "").strip()
                    access_token = (decrypt_token_value(account.access_token) or "").strip()
                    refresh_credential = refresh_token
                    if not refresh_credential and account.platform == Platform.INSTAGRAM:
                        refresh_credential = access_token

                    bundle = refresh_oauth_token(account.platform, refresh_credential)
                    store_account_tokens(
                        account=account,
                        access_token=bundle.access_token,
                        refresh_token=bundle.refresh_token or refresh_token,
                        token_type=bundle.token_type or account.token_type,
                        token_scope=bundle.scope or account.token_scope,
                        token_expires_at=bundle.expires_at or account.token_expires_at,
                    )
                    account.status = ConnectionStatus.CONNECTED
                    self._refreshed_total += 1
                    self._next_retry_by_account.pop(account.id, None)
                except OAuthExchangeError as exc:
                    self._failed_total += 1
                    if not exc.retryable:
                        account.status = ConnectionStatus.EXPIRED
                        self._next_retry_by_account.pop(account.id, None)
                    else:
                        self._next_retry_by_account[account.id] = now + timedelta(
                            seconds=max(5.0, settings.oauth_refresh_failure_backoff_seconds)
                        )
                except Exception:
                    self._failed_total += 1
                    self._next_retry_by_account[account.id] = now + timedelta(
                        seconds=max(5.0, settings.oauth_refresh_failure_backoff_seconds)
                    )

            db.commit()
        finally:
            db.close()

    def _worker_loop(self) -> None:
        while not self._stop_event.is_set():
            self.run_once()
            sleep(max(1.0, settings.oauth_refresh_interval_seconds))


oauth_refresh_worker_service = OAuthRefreshWorkerService()
