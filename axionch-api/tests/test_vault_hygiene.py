from __future__ import annotations

from uuid import uuid4

from sqlalchemy import select

from app.db.models import User, VaultEntry, VaultEntryQuarantine
from app.db.session import SessionLocal
from app.services.vault.crypto import encrypt_secret
from app.services.vault.hygiene import audit_vault_entries, quarantine_undecryptable_entries


def test_vault_hygiene_audit_and_quarantine_flow() -> None:
    db = SessionLocal()
    user_email = f"vault-hygiene-{uuid4().hex[:10]}@example.com"
    user = User(email=user_email)
    db.add(user)
    db.flush()

    good = VaultEntry(
        user_id=user.id,
        service_name="vault-hygiene-good",
        username_encrypted=encrypt_secret("good-user"),
        password_encrypted=encrypt_secret("good-pass"),
        notes_encrypted=encrypt_secret("good-notes"),
    )
    bad = VaultEntry(
        user_id=user.id,
        service_name="vault-hygiene-bad",
        username_encrypted="invalid-ciphertext",
        password_encrypted="invalid-ciphertext",
        notes_encrypted="invalid-ciphertext",
    )
    db.add_all([good, bad])
    db.commit()
    db.refresh(good)
    db.refresh(bad)

    try:
        audit = audit_vault_entries(db)
        assert audit.scanned_rows >= 2
        assert audit.undecryptable_rows >= 1
        bad_issue = next((item for item in audit.issues if item.entry_id == bad.id), None)
        assert bad_issue is not None
        assert set(bad_issue.failing_fields) >= {
            "username_encrypted",
            "password_encrypted",
            "notes_encrypted",
        }

        dry_run = quarantine_undecryptable_entries(db, apply=False)
        assert (dry_run.quarantined_rows + dry_run.already_quarantined_rows) >= 1
        remaining_bad = db.execute(select(VaultEntry).where(VaultEntry.id == bad.id)).scalar_one_or_none()
        assert remaining_bad is not None

        applied = quarantine_undecryptable_entries(db, apply=True)
        assert (applied.quarantined_rows + applied.already_quarantined_rows) >= 1
        assert applied.deleted_rows >= 1

        remaining_bad = db.execute(select(VaultEntry).where(VaultEntry.id == bad.id)).scalar_one_or_none()
        assert remaining_bad is None
        quarantine_row = db.execute(
            select(VaultEntryQuarantine).where(VaultEntryQuarantine.original_vault_entry_id == bad.id)
        ).scalar_one_or_none()
        assert quarantine_row is not None
    finally:
        db.query(VaultEntryQuarantine).filter(
            VaultEntryQuarantine.original_vault_entry_id.in_([good.id, bad.id])
        ).delete(synchronize_session=False)
        db.query(VaultEntry).filter(VaultEntry.id.in_([good.id, bad.id])).delete(synchronize_session=False)
        db.query(User).filter(User.id == user.id).delete(synchronize_session=False)
        db.commit()
        db.close()
