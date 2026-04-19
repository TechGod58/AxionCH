from __future__ import annotations

from hashlib import sha256

from argon2 import PasswordHasher
from argon2.exceptions import InvalidHashError, VerifyMismatchError

from app.core.config import settings

_ARGON2 = PasswordHasher(
    time_cost=3,
    memory_cost=65536,
    parallelism=2,
    hash_len=32,
    salt_len=16,
)


def _salted_payload(raw_key: str) -> str:
    return f"{settings.api_key_hash_salt}:{raw_key}"


def legacy_sha256_hash(raw_key: str) -> str:
    return sha256(_salted_payload(raw_key).encode("utf-8")).hexdigest()


def hash_api_key(raw_key: str) -> str:
    return _ARGON2.hash(_salted_payload(raw_key))


def is_argon2_hash(stored_hash: str) -> bool:
    return stored_hash.startswith("$argon2")


def verify_api_key_hash(stored_hash: str, raw_key: str) -> bool:
    payload = _salted_payload(raw_key)
    if is_argon2_hash(stored_hash):
        try:
            return bool(_ARGON2.verify(stored_hash, payload))
        except (VerifyMismatchError, InvalidHashError):
            return False
    return stored_hash == legacy_sha256_hash(raw_key)
