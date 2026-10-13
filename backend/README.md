# MiraNova backend

The backend is a Python 3.11+ FastAPI service. This foundation intentionally
contains no authentication behavior; the authentication protocol remains
subject to the project's approved architecture decisions.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/` | Minimal service discovery response |
| `GET` | `/health` | Process liveness check |
| `GET` | `/health/ready` | Application readiness check |
| `GET` | `/docs` | OpenAPI UI when documentation is enabled |

Health responses deliberately contain only a status value and no environment,
dependency, credential, or host details.

## Configuration

Settings are loaded from environment variables or a local `.env` file. Shell
environment variables take precedence. Local `.env` files are ignored by Git.

| Variable | Default | Description |
|---|---|---|
| `MIRANOVA_APP_NAME` | `MiraNova Authentication API` | OpenAPI service title |
| `MIRANOVA_ENVIRONMENT` | `local` | `local`, `test`, `staging`, or `production` |
| `MIRANOVA_DEBUG` | `false` | FastAPI debug mode |
| `MIRANOVA_LOG_LEVEL` | `INFO` | Python log level |
| `MIRANOVA_DOCS_ENABLED` | `true` | Enables `/docs`, `/redoc`, and `/openapi.json` |
| `API_PORT` | `8000` | Host port used by Docker Compose only |

For a production-like configuration, explicitly set
`MIRANOVA_DEBUG=false` and `MIRANOVA_DOCS_ENABLED=false`. TLS must be provided
by the approved deployment boundary; the Compose environment is local-only.

## Local commands

From the repository root:

```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
python -m pip install -r backend/requirements-dev.txt
uvicorn app.main:app --app-dir backend --reload
```

Run checks:

```powershell
python -m pytest
python -m ruff check backend tests/backend
```
