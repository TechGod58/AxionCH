from __future__ import annotations

import base64
import hashlib
import threading

from cryptography.fernet import Fernet, InvalidToken

from app.core.config import settings

_ENC_PREFIX = "enc::"
_LOCK = threading.Lock()
_CACHED_RAW_KEY: str | None = None
_CACHED_FERNET: Fernet | None = None


def _derive_fernet_key(raw_key: str) -> bytes:
    cleaned = (raw_key or "").strip()
    if not cleaned:
        raise ValueError("TOKEN_ENCRYPTION_KEY is not configured.")

    encoded = cleaned.encode("utf-8")
    try:
        # Already a valid fernet key.
        Fernet(encoded)
        return encoded
    except Exception:
        digest = hashlib.sha256(encoded).digest()
        return base64.urlsafe_b64encode(digest)


def build_fernet_for_key(raw_key: str) -> Fernet:
    return Fernet(_derive_fernet_key(raw_key))


def _get_fernet() -> Fernet:
    global _CACHED_RAW_KEY, _CACHED_FERNET

    raw_key = (settings.token_encryption_key or "").strip()
    with _LOCK:
        if _CACHED_FERNET is not None and _CACHED_RAW_KEY == raw_key:
            return _CACHED_FERNET
        derived = _derive_fernet_key(raw_key)
        _CACHED_FERNET = Fernet(derived)
        _CACHED_RAW_KEY = raw_key
        return _CACHED_FERNET


def token_crypto_status() -> tuple[bool, str]:
    try:
        _derive_fernet_key(settings.token_encryption_key)
        return True, "Token encryption key is configured."
    except ValueError as exc:
        return False, str(exc)


def encrypt_token_value(value: str | None) -> str | None:
    if value is None:
        return None
    token = value.strip()
    if not token:
        return None
    encrypted = _get_fernet().encrypt(token.encode("utf-8")).decode("utf-8")
    return f"{_ENC_PREFIX}{encrypted}"


def is_encrypted_token_value(value: str | None) -> bool:
    candidate = (value or "").strip()
    return bool(candidate) and candidate.startswith(_ENC_PREFIX)


def decrypt_token_value(value: str | None) -> str | None:
    if value is None:
        return None
    token = value.strip()
    if not token:
        return None
    if not token.startswith(_ENC_PREFIX):
        return token
    ciphertext = token[len(_ENC_PREFIX):]
    try:
        return _get_fernet().decrypt(ciphertext.encode("utf-8")).decode("utf-8")
    except InvalidToken as exc:
        raise ValueError("Unable to decrypt social token with configured token key.") from exc


def encrypt_token_with_key(value: str | None, *, raw_key: str) -> str | None:
    if value is None:
        return None
    token = value.strip()
    if not token:
        return None
    encrypted = build_fernet_for_key(raw_key).encrypt(token.encode("utf-8")).decode("utf-8")
    return f"{_ENC_PREFIX}{encrypted}"


def decrypt_token_with_key(value: str | None, *, raw_key: str) -> str | None:
    if value is None:
        return None
    token = value.strip()
    if not token:
        return None
    if not token.startswith(_ENC_PREFIX):
        return token
    ciphertext = token[len(_ENC_PREFIX):]
    try:
        return build_fernet_for_key(raw_key).decrypt(ciphertext.encode("utf-8")).decode("utf-8")
    except InvalidToken as exc:
        raise ValueError("Unable to decrypt social token with supplied token key.") from exc


def store_account_tokens(
    *,
    account,
    access_token: str,
    refresh_token: str | None,
    token_type: str | None,
    token_scope: str | None,
    token_expires_at,
) -> None:
    account.access_token = encrypt_token_value(access_token)
    account.refresh_token = encrypt_token_value(refresh_token)
    account.token_type = token_type
    account.token_scope = token_scope
    account.token_expires_at = token_expires_at


def read_account_tokens(account) -> tuple[str | None, str | None]:
    return decrypt_token_value(account.access_token), decrypt_token_value(account.refresh_token)
