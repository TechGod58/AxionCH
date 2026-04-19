from __future__ import annotations

from dataclasses import dataclass, field

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.models import VaultEntry, VaultEntryQuarantine
from app.services.vault.crypto import decrypt_secret, decrypt_secret_with_key


@dataclass
class VaultAuditIssue:
    entry_id: int
    service_name: str
    failing_fields: list[str] = field(default_factory=list)
    reason: str = "undecryptable_with_available_keys"


@dataclass
class VaultAuditReport:
    scanned_rows: int = 0
    decrypt_ok_fields: int = 0
    decrypt_bad_fields: int = 0
    undecryptable_rows: int = 0
    issues: list[VaultAuditIssue] = field(default_factory=list)


@dataclass
class VaultQuarantineReport:
    scanned_rows: int = 0
    quarantined_rows: int = 0
    deleted_rows: int = 0
    already_quarantined_rows: int = 0
    skipped_rows: int = 0
    errors: int = 0


def _decrypt_with_fallback(ciphertext: str, historical_keys: list[str]) -> str:
    try:
        return decrypt_secret(ciphertext)
    except Exception as current_exc:
        last_exc: Exception = current_exc
        for old_key in historical_keys:
            try:
                return decrypt_secret_with_key(ciphertext, raw_key=old_key)
            except Exception as exc:
                last_exc = exc
        raise ValueError(str(last_exc))


def audit_vault_entries(
    db: Session,
    *,
    historical_old_keys: list[str] | None = None,
) -> VaultAuditReport:
    keys = [item.strip() for item in (historical_old_keys or []) if item and item.strip()]
    report = VaultAuditReport()
    rows = db.execute(select(VaultEntry).order_by(VaultEntry.id.asc())).scalars().all()
    report.scanned_rows = len(rows)

    for row in rows:
        issue = VaultAuditIssue(entry_id=row.id, service_name=row.service_name)
        values = {
            "username_encrypted": row.username_encrypted,
            "password_encrypted": row.password_encrypted,
            "notes_encrypted": row.notes_encrypted,
        }
        for field_name, ciphertext in values.items():
            if not ciphertext:
                continue
            try:
                _decrypt_with_fallback(ciphertext, keys)
                report.decrypt_ok_fields += 1
            except Exception:
                report.decrypt_bad_fields += 1
                issue.failing_fields.append(field_name)

        if issue.failing_fields:
            report.undecryptable_rows += 1
            report.issues.append(issue)

    return report


def quarantine_undecryptable_entries(
    db: Session,
    *,
    historical_old_keys: list[str] | None = None,
    apply: bool = False,
) -> VaultQuarantineReport:
    report = VaultQuarantineReport()
    audit = audit_vault_entries(db, historical_old_keys=historical_old_keys)
    report.scanned_rows = audit.scanned_rows

    issue_by_id = {item.entry_id: item for item in audit.issues}
    if not issue_by_id:
        return report

    rows = db.execute(
        select(VaultEntry)
        .where(VaultEntry.id.in_(list(issue_by_id.keys())))
        .order_by(VaultEntry.id.asc())
    ).scalars().all()

    for row in rows:
        issue = issue_by_id[row.id]
        existing_quarantine = db.execute(
            select(VaultEntryQuarantine).where(
                VaultEntryQuarantine.original_vault_entry_id == row.id
            )
        ).scalar_one_or_none()
        if existing_quarantine is not None:
            report.already_quarantined_rows += 1
            if apply:
                db.delete(row)
                report.deleted_rows += 1
            continue

        quarantine_reason = f"{issue.reason}: {','.join(issue.failing_fields)}"
        quarantine_row = VaultEntryQuarantine(
            original_vault_entry_id=row.id,
            user_id=row.user_id,
            service_name=row.service_name,
            username_encrypted=row.username_encrypted,
            password_encrypted=row.password_encrypted,
            notes_encrypted=row.notes_encrypted,
            quarantine_reason=quarantine_reason,
        )
        if apply:
            db.add(quarantine_row)
            db.delete(row)
            report.deleted_rows += 1
        report.quarantined_rows += 1

    if apply:
        db.commit()
    else:
        db.rollback()
    return report
