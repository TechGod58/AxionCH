from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "AxionCH API"
    runtime_environment: str = "development"
    debug: bool = False
    database_url: str = "postgresql://axionch:axionch@localhost:5432/axionch"
    api_host: str = "127.0.0.1"
    api_port: int = 8010
    token_encryption_key: str = "axionch-dev-token-encryption-key-rotate-before-prod-2026"
    vault_encryption_key: str = "replace-me-vault-key"
    api_key: str | None = None
    api_key_hash_salt: str = "axionch-dev-api-key-hash-salt-rotate-before-prod-2026"
    enforce_api_auth: bool = True
    allow_bootstrap_without_api_key: bool = False
    cors_allowed_origins: str = "http://127.0.0.1:3000,http://localhost:3000,http://10.0.2.2:3000"
    cors_allow_credentials: bool = True

    # X (Twitter) Credentials
    x_api_key: str | None = None
    x_api_secret: str | None = None
    x_access_token: str | None = None
    x_access_token_secret: str | None = None
    x_bearer_token: str | None = None

    # LinkedIn Credentials
    linkedin_client_id: str | None = None
    linkedin_client_secret: str | None = None
    linkedin_access_token: str | None = None
    linkedin_author_urn: str | None = None

    # Instagram / Meta Credentials
    instagram_app_id: str | None = None
    instagram_app_secret: str | None = None
    instagram_access_token: str | None = None
    instagram_business_account_id: str | None = None

    # In-memory dry-run history limits
    dry_run_history_max_items: int = 200

    # Retry and queue behavior
    publish_retry_max_attempts: int = 3
    publish_retry_backoff_seconds: float = 1.0
    publish_queue_max_attempts: int = 3
    publish_queue_base_backoff_seconds: float = 2.0

    # Platform secret resolution
    platform_secrets_env_fallback: bool = True

    # Local video filter processing
    ffmpeg_binary: str = "ffmpeg"
    media_output_dir: str = "media_outputs"
    image_output_dir: str = "media_outputs/images"
    ffmpeg_timeout_seconds: float = 240.0
    media_download_timeout_seconds: float = 60.0
    media_max_download_bytes: int = 50_000_000
    media_allow_http_source_urls: bool = False
    media_allowed_source_domains: str | None = None
    media_blocked_source_hosts: str = "localhost,127.0.0.1,0.0.0.0,169.254.169.254,metadata.google.internal"
    media_block_private_networks: bool = True
    media_allowed_video_mime_prefixes: str = "video/"
    media_allowed_image_mime_prefixes: str = "image/"
    media_allowed_audio_mime_prefixes: str = "audio/"

    # OAuth scaffold settings
    oauth_state_ttl_seconds: int = 600
    oauth_exchange_timeout_seconds: float = 20.0
    oauth_refresh_enabled: bool = True
    oauth_refresh_interval_seconds: float = 60.0
    oauth_refresh_ahead_seconds: int = 300
    oauth_refresh_failure_backoff_seconds: float = 300.0
    x_client_id: str | None = None
    x_redirect_uri: str | None = None
    x_oauth_token_url: str = "https://api.twitter.com/2/oauth2/token"
    linkedin_redirect_uri: str | None = None
    linkedin_oauth_token_url: str = "https://www.linkedin.com/oauth/v2/accessToken"
    instagram_redirect_uri: str | None = None
    instagram_oauth_token_url: str = "https://graph.facebook.com/v20.0/oauth/access_token"
    instagram_oauth_refresh_url: str = "https://graph.instagram.com/refresh_access_token"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore"
    )


settings = Settings()
