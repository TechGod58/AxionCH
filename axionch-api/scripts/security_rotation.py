from __future__ import annotations

import argparse
from dataclasses import dataclass

from sqlalchemy import select

from app.core.config import settings
from app.db.models import SocialAccount, VaultEntry
from app.db.session import SessionLocal
from app.services.oauth.token_crypto import decrypt_token_with_key, encrypt_token_value, encrypt_token_with_key
from app.services.vault.crypto import decrypt_secret_with_key, encrypt_secret_with_key


@dataclass
class RotationSummary:
    scanned: int = 0
    updated: int = 0
    skipped: int = 0
    errors: int = 0


def _print_summary(title: str, summary: RotationSummary, *, apply: bool) -> None:
    print(f"\n{title}")
    print("-" * len(title))
    print(f"Mode: {'apply' if apply else 'dry-run'}")
    print(f"Scanned: {summary.scanned}")
    print(f"Updated: {summary.updated}")
    print(f"Skipped: {summary.skipped}")
    print(f"Errors: {summary.errors}")


def _decrypt_token_with_any_key(value: str, old_keys: list[str]) -> str:
    last_error: Exception | None = None
    for key in old_keys:
        try:
            return decrypt_token_with_key(value, raw_key=key)
        except Exception as exc:
            last_error = exc
    if last_error is None:
        raise ValueError("No old key candidates were supplied.")
    raise ValueError(str(last_error))


def _decrypt_vault_with_any_key(value: str, old_keys: list[str]) -> str:
    last_error: Exception | None = None
    for key in old_keys:
        try:
            return decrypt_secret_with_key(value, raw_key=key)
        except Exception as exc:
            last_error = exc
    if last_error is None:
        raise ValueError("No old key candidates were supplied.")
    raise ValueError(str(last_error))


def migrate_plaintext_social_tokens(*, apply: bool) -> int:
    summary = RotationSummary()
    db = SessionLocal()
    try:
        accounts = db.execute(select(SocialAccount)).scalars().all()
        for account in accounts:
            summary.scanned += 1
            changed = False

            for field_name in ("access_token", "refresh_token"):
                current = getattr(account, field_name)
                if not current:
                    summary.skipped += 1
                    continue
                if is_encrypted_token_value(current):
                    summary.skipped += 1
                    continue

                encrypted = encrypt_token_value(current)
                if encrypted is None:
                    summary.skipped += 1
                    continue
                setattr(account, field_name, encrypted)
                changed = True
                summary.updated += 1

            if apply and changed:
                db.add(account)

        if apply:
            db.commit()
        else:
            db.rollback()
    finally:
        db.close()

    _print_summary("Plaintext Social Token Migration", summary, apply=apply)
    return 0 if summary.errors == 0 else 1


def rotate_social_token_key(*, old_keys: list[str], new_key: str, apply: bool) -> int:
    summary = RotationSummary()
    db = SessionLocal()
    try:
        accounts = db.execute(select(SocialAccount)).scalars().all()
        for account in accounts:
            summary.scanned += 1
            changed = False

            for field_name in ("access_token", "refresh_token"):
                current = getattr(account, field_name)
                if not current:
                    summary.skipped += 1
                    continue
                try:
                    plaintext = _decrypt_token_with_any_key(current, old_keys)
                    reencrypted = encrypt_token_with_key(plaintext, raw_key=new_key)
                except Exception as exc:
                    summary.errors += 1
                    print(f"[ERROR] account={account.id} field={field_name}: {exc}")
                    continue

                if reencrypted is None:
                    summary.skipped += 1
                    continue
                setattr(account, field_name, reencrypted)
                changed = True
                summary.updated += 1

            if apply and changed:
                db.add(account)

        if apply:
            db.commit()
        else:
            db.rollback()
    finally:
        db.close()

    _print_summary("Social Token Key Rotation", summary, apply=apply)
    return 0 if summary.errors == 0 else 1


def rotate_vault_key(*, old_keys: list[str], new_key: str, apply: bool) -> int:
    summary = RotationSummary()
    db = SessionLocal()
    try:
        entries = db.execute(select(VaultEntry)).scalars().all()
        for entry in entries:
            summary.scanned += 1
            changed = False
            field_values = {
                "username_encrypted": entry.username_encrypted,
                "password_encrypted": entry.password_encrypted,
                "notes_encrypted": entry.notes_encrypted,
            }
            for field_name, ciphertext in field_values.items():
                if not ciphertext:
                    summary.skipped += 1
                    continue
                try:
                    plaintext = _decrypt_vault_with_any_key(ciphertext, old_keys)
                    reencrypted = encrypt_secret_with_key(plaintext, raw_key=new_key)
                except Exception as exc:
                    summary.errors += 1
                    print(f"[ERROR] vault_entry={entry.id} field={field_name}: {exc}")
                    continue
                setattr(entry, field_name, reencrypted)
                changed = True
                summary.updated += 1

            if apply and changed:
                db.add(entry)

        if apply:
            db.commit()
        else:
            db.rollback()
    finally:
        db.close()

    _print_summary("Vault Master Key Rotation", summary, apply=apply)
    return 0 if summary.errors == 0 else 1


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Security rotation and migration utilities.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    migrate_parser = subparsers.add_parser(
        "migrate-plaintext-social-tokens",
        help="Encrypt any legacy plaintext social access/refresh tokens at rest.",
    )
    migrate_parser.add_argument(
        "--apply",
        action="store_true",
        help="Persist changes. Without this flag, runs in dry-run mode.",
    )

    rotate_token_parser = subparsers.add_parser(
        "rotate-token-encryption-key",
        help="Re-encrypt social tokens from old TOKEN_ENCRYPTION_KEY to new key.",
    )
    rotate_token_parser.add_argument(
        "--old-key",
        action="append",
        required=True,
        help="Old token key candidate. Pass multiple times to support historical key sets.",
    )
    rotate_token_parser.add_argument("--new-key", required=True)
    rotate_token_parser.add_argument("--apply", action="store_true")

    rotate_vault_parser = subparsers.add_parser(
        "rotate-vault-master-key",
        help="Re-encrypt vault records from old VAULT_ENCRYPTION_KEY to new key.",
    )
    rotate_vault_parser.add_argument(
        "--old-key",
        action="append",
        required=True,
        help="Old vault key candidate. Pass multiple times to support historical key sets.",
    )
    rotate_vault_parser.add_argument("--new-key", required=True)
    rotate_vault_parser.add_argument("--apply", action="store_true")

    return parser


def main() -> int:
    parser = _build_parser()
    args = parser.parse_args()

    print(f"Database target: {settings.database_url}")

    if args.command == "migrate-plaintext-social-tokens":
        return migrate_plaintext_social_tokens(apply=bool(args.apply))

    if args.command == "rotate-token-encryption-key":
        return rotate_social_token_key(old_keys=list(args.old_key), new_key=args.new_key, apply=bool(args.apply))

    if args.command == "rotate-vault-master-key":
        return rotate_vault_key(old_keys=list(args.old_key), new_key=args.new_key, apply=bool(args.apply))

    parser.print_help()
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
