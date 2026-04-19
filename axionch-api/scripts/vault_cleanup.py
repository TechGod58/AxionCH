from __future__ import annotations

import argparse
from datetime import datetime, timezone
from pathlib import Path
import shutil

from app.core.config import settings
from app.db.session import SessionLocal, init_db
from app.services.vault.hygiene import (
    VaultAuditReport,
    VaultQuarantineReport,
    audit_vault_entries,
    quarantine_undecryptable_entries,
)


def _print_audit(report: VaultAuditReport) -> None:
    print("\nVault decryptability audit")
    print("--------------------------")
    print(f"Scanned rows: {report.scanned_rows}")
    print(f"Decryptable fields: {report.decrypt_ok_fields}")
    print(f"Undecryptable fields: {report.decrypt_bad_fields}")
    print(f"Undecryptable rows: {report.undecryptable_rows}")
    if report.issues:
        print("Sample issues:")
        for item in report.issues[:20]:
            print(f"- entry_id={item.entry_id} service={item.service_name} fields={','.join(item.failing_fields)}")


def _print_quarantine(report: VaultQuarantineReport, *, apply: bool) -> None:
    print("\nVault quarantine run")
    print("--------------------")
    print(f"Mode: {'apply' if apply else 'dry-run'}")
    print(f"Scanned rows: {report.scanned_rows}")
    print(f"Rows marked for quarantine: {report.quarantined_rows}")
    print(f"Rows already quarantined: {report.already_quarantined_rows}")
    print(f"Rows deleted from vault_entries: {report.deleted_rows}")
    print(f"Errors: {report.errors}")


def _backup_sqlite_if_needed(*, force: bool = False) -> str | None:
    if not settings.database_url.startswith("sqlite:///"):
        return None
    db_path = settings.database_url.replace("sqlite:///", "", 1).strip()
    if not db_path:
        return None
    source = Path(db_path).resolve()
    if not source.exists():
        return None
    backup_dir = source.parent / "backups"
    backup_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    target = backup_dir / f"{source.stem}_vault_cleanup_{timestamp}{source.suffix}"
    if target.exists() and not force:
        return str(target)
    shutil.copy2(source, target)
    return str(target)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Vault decryptability audit and quarantine utility.")
    parser.add_argument(
        "--old-key",
        action="append",
        default=[],
        help="Historical vault key candidate. Repeat multiple times if needed.",
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Persist quarantine/delete changes. Default mode is dry-run.",
    )
    parser.add_argument(
        "--skip-backup",
        action="store_true",
        help="Skip automatic sqlite backup in apply mode.",
    )
    parser.add_argument(
        "--audit-only",
        action="store_true",
        help="Run audit only and do not quarantine.",
    )
    return parser


def main() -> int:
    args = _build_parser().parse_args()

    init_db()
    print(f"Database target: {settings.database_url}")
    keys = [item.strip() for item in args.old_key if item and item.strip()]

    db = SessionLocal()
    try:
        audit = audit_vault_entries(db, historical_old_keys=keys)
        _print_audit(audit)
        if args.audit_only:
            return 0

        if args.apply and not args.skip_backup:
            backup_path = _backup_sqlite_if_needed()
            if backup_path:
                print(f"SQLite backup created: {backup_path}")

        quarantine = quarantine_undecryptable_entries(
            db,
            historical_old_keys=keys,
            apply=bool(args.apply),
        )
        _print_quarantine(quarantine, apply=bool(args.apply))
        return 0
    finally:
        db.close()


if __name__ == "__main__":
    raise SystemExit(main())
