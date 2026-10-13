"""Shared backend API test fixtures."""

import pytest
from app.core.config import Settings
from app.main import create_app
from fastapi.testclient import TestClient


@pytest.fixture
def settings() -> Settings:
    """Return deterministic settings independent of a developer's `.env`."""

    return Settings(
        _env_file=None,
        environment="test",
        debug=False,
        docs_enabled=True,
    )


@pytest.fixture
def client(settings: Settings) -> TestClient:
    """Return a client that runs the application's lifespan hooks."""

    with TestClient(create_app(settings)) as test_client:
        yield test_client
