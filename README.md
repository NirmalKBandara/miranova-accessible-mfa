# MiraNova Accessible MFA

MiraNova is an academic accessible multi-factor authentication prototype for
visually impaired users. It combines an Android client, device-bound
cryptographic authentication, and a FastAPI backend.

## Backend quick start

### Docker (recommended)

Docker Desktop with Docker Compose is required.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

The API is available at `http://localhost:8000`, with interactive API
documentation at `http://localhost:8000/docs`. Check liveness with:

```powershell
Invoke-RestMethod http://localhost:8000/health
```

Stop the local environment with `docker compose down`.

### Python virtual environment

Python 3.11 or later is required.

```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
python -m pip install -r backend/requirements-dev.txt
uvicorn app.main:app --app-dir backend --reload
```

Run the backend test suite from the repository root:

```powershell
python -m pytest
```

Configuration is read from environment variables prefixed with `MIRANOVA_`.
Copy `.env.example` for local defaults; never commit `.env` files or real
credentials. See [backend/README.md](backend/README.md) for the complete backend
configuration and endpoint reference.
