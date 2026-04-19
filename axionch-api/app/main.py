from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.auth import require_api_key
from app.core.config import settings
from app.core.cors import has_wildcard, parse_csv, validate_cors_for_runtime
from app.core.security_checks import security_warnings
from app.db.session import init_db
from app.services.oauth.refresh_worker import oauth_refresh_worker_service
from app.routes import accounts, auth, config, health, media, oauth, posts, vault
from app.services.publish.queue_service import publish_queue_service

app = FastAPI(
    title=settings.app_name,
    debug=settings.debug,
)

cors_origins = parse_csv(settings.cors_allowed_origins)
validate_cors_for_runtime(cors_origins, runtime_environment=settings.runtime_environment)
allow_all_origins = has_wildcard(cors_origins)
if not cors_origins:
    cors_origins = ["http://127.0.0.1:3000", "http://localhost:3000"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"] if allow_all_origins else cors_origins,
    allow_credentials=settings.cors_allow_credentials and not allow_all_origins,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(accounts.router, prefix="/accounts", tags=["accounts"], dependencies=[Depends(require_api_key)])
app.include_router(auth.router, prefix="/auth", tags=["auth"], dependencies=[Depends(require_api_key)])
app.include_router(posts.router, prefix="/posts", tags=["posts"], dependencies=[Depends(require_api_key)])
app.include_router(config.router, prefix="/config", tags=["config"], dependencies=[Depends(require_api_key)])
app.include_router(oauth.router, prefix="/oauth", tags=["oauth"], dependencies=[Depends(require_api_key)])
app.include_router(vault.router, prefix="/vault", tags=["vault"], dependencies=[Depends(require_api_key)])
app.include_router(media.router, prefix="/media", tags=["media"], dependencies=[Depends(require_api_key)])


@app.on_event("startup")
def on_startup() -> None:
    init_db()
    publish_queue_service.start()
    oauth_refresh_worker_service.start()
    for warning in security_warnings():
        print(f"[SECURITY WARNING] {warning}")


@app.on_event("shutdown")
def on_shutdown() -> None:
    publish_queue_service.stop()
    oauth_refresh_worker_service.stop()
