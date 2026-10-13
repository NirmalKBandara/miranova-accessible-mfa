"""Environment-driven settings for the security tests (see README.md)."""

from __future__ import annotations

import os


def _env_int(name: str, default: int) -> int:
    try:
        return int(os.environ.get(name, default))
    except ValueError:
        return default


BASE_URL = os.environ.get("MIRANOVA_BASE_URL", "http://127.0.0.1:8000")
TIMEOUT = float(os.environ.get("MIRANOVA_TIMEOUT", "10"))
ALLOW_REMOTE = os.environ.get("MIRANOVA_ALLOW_REMOTE") == "1"
LOG_PATH = os.environ.get("MIRANOVA_LOG_PATH")
MAX_FAILED_ATTEMPTS = _env_int("MIRANOVA_MAX_FAILED_ATTEMPTS", 10)
MAX_EXPIRY_WAIT = _env_int("MIRANOVA_MAX_EXPIRY_WAIT", 130)
TEST_JWT_SECRET = os.environ.get("MIRANOVA_TEST_JWT_SECRET")
CONCURRENCY = _env_int("MIRANOVA_CONCURRENCY", 10)

# Hosts considered the team's own local environment (SRS §12, ASM-THR-06).
LOCAL_HOSTS = {"127.0.0.1", "localhost", "::1", "10.0.2.2", "0.0.0.0", "backend"}
