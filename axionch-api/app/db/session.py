from sqlalchemy import create_engine, inspect, text
from sqlalchemy.orm import sessionmaker

from app.core.config import settings
from app.db.models import Base

engine_kwargs = {"future": True}
if settings.database_url.startswith("sqlite"):
    engine_kwargs["connect_args"] = {"check_same_thread": False, "timeout": 30}

engine = create_engine(settings.database_url, **engine_kwargs)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)


def _ensure_column_sql(table_name: str, column_name: str, type_sql: str) -> str:
    return f"ALTER TABLE {table_name} ADD COLUMN {column_name} {type_sql}"


def _ensure_runtime_compatibility() -> None:
    inspector = inspect(engine)
    dialect = engine.dialect.name

    missing_columns: list[tuple[str, str, str]] = []
    existing_tables = set(inspector.get_table_names())

    if "social_accounts" in existing_tables:
        social_account_columns = {col["name"] for col in inspector.get_columns("social_accounts")}
        if "refresh_token" not in social_account_columns:
            missing_columns.append(("social_accounts", "refresh_token", "TEXT"))
        if "token_type" not in social_account_columns:
            missing_columns.append(("social_accounts", "token_type", "VARCHAR(40)"))
        if "token_scope" not in social_account_columns:
            missing_columns.append(("social_accounts", "token_scope", "TEXT"))
        if "token_expires_at" not in social_account_columns:
            missing_columns.append(("social_accounts", "token_expires_at", "DATETIME"))

    with engine.begin() as conn:
        for table_name, column_name, type_sql in missing_columns:
            statement = _ensure_column_sql(table_name, column_name, type_sql)
            if dialect == "postgresql":
                statement = f"ALTER TABLE {table_name} ADD COLUMN IF NOT EXISTS {column_name} {type_sql}"
            try:
                conn.execute(text(statement))
            except Exception:
                # Ignore duplicate-column races or unsupported ALTER variations.
                pass


def init_db() -> None:
    Base.metadata.create_all(bind=engine)
    _ensure_runtime_compatibility()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
