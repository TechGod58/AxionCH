from pydantic import BaseModel, Field


class CreateApiKeyRequest(BaseModel):
    user_email: str
    label: str = Field(min_length=1, max_length=120)


class ApiKeyResponse(BaseModel):
    key_id: str
    user_email: str
    label: str
    raw_api_key: str
    created_at: str


class ApiKeyListItem(BaseModel):
    key_id: str
    label: str
    is_active: bool
    created_at: str
    last_used_at: str | None = None
    revoked_at: str | None = None


class ApiKeyListResponse(BaseModel):
    user_email: str
    keys: list[ApiKeyListItem]


class ApiKeyRevokeResponse(BaseModel):
    user_email: str
    key_id: str
    revoked: bool
