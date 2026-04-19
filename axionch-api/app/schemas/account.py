from datetime import datetime

from pydantic import BaseModel
from app.db.models import ConnectionStatus, Platform


class CreateAccountRequest(BaseModel):
    user_email: str
    platform: Platform
    handle: str
    access_token: str


class AccountResponse(BaseModel):
    id: int
    platform: Platform
    handle: str
    status: ConnectionStatus
    token_type: str | None = None
    token_scope: str | None = None
    token_expires_at: datetime | None = None

    class Config:
        from_attributes = True
