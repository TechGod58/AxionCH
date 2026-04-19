# HTTP examples

Use this header when `API_KEY` is configured:

```http
X-Axionch-Api-Key: your-api-key
```

Use this additional header when authenticating with a per-user key:

```http
X-Axionch-User-Email: you@example.com
```

When using a per-user key, `user_email` in request payloads/queries must match `X-Axionch-User-Email`.

## Create account
```http
POST /accounts
Content-Type: application/json

{
  "user_email": "you@example.com",
  "platform": "x",
  "handle": "@techgod",
  "access_token": "mock-token"
}
```

## List accounts
```http
GET /accounts
```

## Config status
```http
GET /config/status
```

## Credential check (no live publish)
```http
POST /config/check
```

## Config security checks
```http
GET /config/security
```

## Create and publish post
```http
POST /posts
Content-Type: application/json

{
  "user_email": "you@example.com",
  "body": "Hello from AxionCH",
  "image_url": "https://example.com/image.jpg",
  "account_ids": [1, 2]
}
```

## Queue post for async publish worker
```http
POST /posts/queue
Content-Type: application/json

{
  "user_email": "you@example.com",
  "body": "Queue this post",
  "image_url": "https://example.com/image.jpg",
  "account_ids": [1, 2, 3]
}
```

## Check queued publish job status
```http
GET /posts/jobs/123
```

## Publish queue metrics
```http
GET /posts/metrics
```

## Dead letters list
```http
GET /posts/dead-letters?limit=50
```

## Requeue a dead letter
```http
POST /posts/dead-letters/42/requeue
```

## Dry-run publish (no live post)
```http
POST /posts/dry-run
Content-Type: application/json

{
  "user_email": "you@example.com",
  "body": "Validate credentials only",
  "image_url": "https://example.com/image.jpg",
  "account_ids": [1, 2]
}
```

## Dry-run history
```http
GET /posts/dry-run-history
```

## Dry-run history (filtered)
```http
GET /posts/dry-run-history?limit=50&platform=x&success_only=false
```

## Clear dry-run history
```http
DELETE /posts/dry-run-history
```

## Clear dry-run history for one platform
```http
DELETE /posts/dry-run-history?platform=instagram
```

## OAuth start
```http
GET /oauth/linkedin/start?user_email=you@example.com
```

## OAuth callback (real code exchange + token persistence)
```http
GET /oauth/linkedin/callback?code=AUTH_CODE&state=STATE_VALUE
```

## Create per-user API key
```http
POST /auth/keys
Content-Type: application/json

{
  "user_email": "you@example.com",
  "label": "desktop-local"
}
```

## List per-user API keys
```http
GET /auth/keys?user_email=you@example.com
```

## Revoke per-user API key
```http
DELETE /auth/keys/ak_1234567890abcdef?user_email=you@example.com
```
