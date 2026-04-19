import base64
import hashlib
import threading

from cryptography.fernet import Fernet, InvalidToken

from app.core.config import settings


def _derive_fernet_key(raw_key: str, *, enforce_strength: bool = True) -> bytes:
    cleaned = (raw_key or "").strip()
    if not cleaned:
        raise ValueError("VAULT_ENCRYPTION_KEY is not configured.")

    lowered = cleaned.lower()
    if enforce_strength and (len(cleaned) < 32 or "replace-me" in lowered or "changeme" in lowered):
        raise ValueError(
            "VAULT_ENCRYPTION_KEY is weak. Set a strong value (>=32 chars) before using /vault."
        )

    encoded = cleaned.encode("utf-8")
    try:
        # If this succeeds, the provided value is already a valid Fernet key.
        Fernet(encoded)
        return encoded
    except Exception:
        digest = hashlib.sha256(encoded).digest()
        return base64.urlsafe_b64encode(digest)


_LOCK = threading.Lock()
_CACHED_RAW_KEY: str | None = None
_CACHED_FERNET: Fernet | None = None


def build_fernet_for_key(raw_key: str, *, enforce_strength: bool = True) -> Fernet:
    return Fernet(_derive_fernet_key(raw_key, enforce_strength=enforce_strength))


def _get_fernet() -> Fernet:
    global _CACHED_RAW_KEY, _CACHED_FERNET

    raw_key = (settings.vault_encryption_key or "").strip()
    with _LOCK:
        if _CACHED_FERNET is not None and _CACHED_RAW_KEY == raw_key:
            return _CACHED_FERNET

        derived = _derive_fernet_key(raw_key)
        _CACHED_FERNET = Fernet(derived)
        _CACHED_RAW_KEY = raw_key
        return _CACHED_FERNET


def vault_crypto_status() -> tuple[bool, str]:
    try:
        _derive_fernet_key(settings.vault_encryption_key)
        return True, "Vault encryption key is configured."
    except ValueError as exc:
        return False, str(exc)


def encrypt_secret(plaintext: str) -> str:
    return _get_fernet().encrypt(plaintext.encode("utf-8")).decode("utf-8")


def decrypt_secret(ciphertext: str) -> str:
    try:
        return _get_fernet().decrypt(ciphertext.encode("utf-8")).decode("utf-8")
    except InvalidToken as exc:
        raise ValueError("Unable to decrypt vault secret with configured vault key.") from exc


def encrypt_secret_with_key(plaintext: str, *, raw_key: str) -> str:
    return build_fernet_for_key(raw_key).encrypt(plaintext.encode("utf-8")).decode("utf-8")


def decrypt_secret_with_key(ciphertext: str, *, raw_key: str) -> str:
    try:
        return build_fernet_for_key(raw_key, enforce_strength=False).decrypt(ciphertext.encode("utf-8")).decode("utf-8")
    except InvalidToken as exc:
        raise ValueError("Unable to decrypt vault secret with supplied vault key.") from exc
