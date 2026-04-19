from pydantic import BaseModel

from app.db.models import Platform


class OAuthStartResponse(BaseModel):
    platform: Platform
    auth_url: str
    state: str
    redirect_uri: str
    scopes: list[str]


class OAuthCallbackResponse(BaseModel):
    platform: Platform
    state_valid: bool
    account_id: int | None = None
    message: str
