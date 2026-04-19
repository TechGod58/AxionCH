from pydantic import BaseModel, Field


class VaultEntryCreateRequest(BaseModel):
    user_email: str
    service_name: str = Field(min_length=1, max_length=120)
    username: str = Field(min_length=1, max_length=255)
    password: str = Field(min_length=1, max_length=4096)
    notes: str | None = Field(default=None, max_length=4096)


class VaultEntryUpdateRequest(BaseModel):
    service_name: str | None = Field(default=None, min_length=1, max_length=120)
    username: str | None = Field(default=None, min_length=1, max_length=255)
    password: str | None = Field(default=None, min_length=1, max_length=4096)
    notes: str | None = Field(default=None, max_length=4096)


class VaultEntrySummary(BaseModel):
    id: int
    user_email: str
    service_name: str
    username: str
    password_mask: str
    created_at: str
    updated_at: str


class VaultListResponse(BaseModel):
    entries: list[VaultEntrySummary]
    total_returned: int


class VaultEntryResponse(BaseModel):
    id: int
    user_email: str
    service_name: str
    username: str
    password: str
    notes: str | None = None
    created_at: str
    updated_at: str


class VaultDeleteResponse(BaseModel):
    deleted: bool
    entry_id: int


class VaultCryptoStatusResponse(BaseModel):
    ready: bool
    message: str
