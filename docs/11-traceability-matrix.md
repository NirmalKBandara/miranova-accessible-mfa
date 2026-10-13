# Requirements traceability matrix

This matrix links requirements to implementation and executable evidence. A
row marked **Partial** records only the part delivered in the referenced issue;
it must not be interpreted as evidence that the full cross-component
requirement is complete.

| Requirement | Issue | Implementation | Verification evidence | Status |
|---|---|---|---|---|
| FR-01 — runnable client and authentication API | #40 | `backend/app/main.py`, `backend/Dockerfile` | `tests/backend/test_health.py`; native API smoke test | Partial — backend only |
| FR-03 — externalized development configuration, no committed secrets | #40 | `backend/app/core/config.py`, `.env.example`, `.gitignore` | `tests/backend/test_config.py` | Implemented for backend foundation |
| FR-04 — non-sensitive `GET /health` | #40 | `backend/app/api/routes/health.py`, `backend/app/schemas/health.py` | `tests/backend/test_health.py::test_health_endpoint_returns_only_safe_status` | Implemented |
| FR-05 — repeatable local setup and automated smoke tests | #40 | `docker-compose.yml`, `backend/Dockerfile`, `README.md`, `backend/README.md`, `pytest.ini` | `docker compose config`; backend test suite; container health check | Implemented for backend foundation |
| ASM-11 — local server components can run | #40 | `docker-compose.yml`, `backend/Dockerfile` | Docker image build and healthy Compose container | Validated for API; database is future scope |

## Issue #40 verification record

- Backend tests: `python -m pytest`
- Static checks: `python -m ruff check backend tests/backend`
- Formatting: `python -m ruff format --check backend tests/backend`
- Compose validation: `docker compose config`
- Runtime smoke test: `GET /health`, `GET /health/ready`, and
  `GET /openapi.json`

Record CI links and reviewer evidence in this section when a pull request is
opened.
