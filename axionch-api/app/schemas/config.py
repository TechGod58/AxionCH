from pydantic import BaseModel, Field


class PlatformConfigStatus(BaseModel):
    mode: str
    configured: bool
    required_fields: list[str]
    configured_fields: list[str]
    source_by_field: dict[str, str] = Field(default_factory=dict)
    last_checked_at: str | None = None
    last_check_success: bool | None = None
    last_check_error: str | None = None
    check_count: int = 0


class ConfigStatusResponse(BaseModel):
    x: PlatformConfigStatus
    linkedin: PlatformConfigStatus
    instagram: PlatformConfigStatus


class ConfigCheckResult(BaseModel):
    platform: str
    success: bool
    error_message: str | None = None
    last_checked_at: str | None = None
    check_count: int = 0


class ConfigCheckResponse(BaseModel):
    checked_at: str
    results: list[ConfigCheckResult]


class SecretCheckStatus(BaseModel):
    name: str
    configured: bool
    strong: bool
    message: str


class ConfigSecurityResponse(BaseModel):
    production_ready: bool
    warnings: list[str]
    checks: list[SecretCheckStatus]


class PlatformSecretWriteRequest(BaseModel):
    user_email: str
    secrets: dict[str, str] = Field(default_factory=dict)
    overwrite_existing: bool = True


class PlatformSecretMigrationRequest(BaseModel):
    user_email: str
    overwrite_existing: bool = False


class PlatformSecretWriteResult(BaseModel):
    platform: str
    user_email: str
    updated_fields: list[str]
    skipped_fields: list[str]
    status: PlatformConfigStatus


class PlatformSecretMigrationResponse(BaseModel):
    user_email: str
    results: list[PlatformSecretWriteResult]
