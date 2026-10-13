"""Tests for externalized application configuration."""

import pytest
from app.core.config import Settings
from app.main import create_app
from pydantic import ValidationError


def test_settings_load_prefixed_environment_variables(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setenv("MIRANOVA_ENVIRONMENT", "staging")
    monkeypatch.setenv("MIRANOVA_DEBUG", "true")
    monkeypatch.setenv("MIRANOVA_LOG_LEVEL", "WARNING")

    settings = Settings(_env_file=None)

    assert settings.environment == "staging"
    assert settings.debug is True
    assert settings.log_level == "WARNING"


def test_invalid_environment_is_rejected() -> None:
    with pytest.raises(ValidationError):
        Settings(_env_file=None, environment="unknown")


def test_api_documentation_can_be_disabled() -> None:
    app = create_app(Settings(_env_file=None, environment="test", docs_enabled=False))

    assert app.docs_url is None
    assert app.redoc_url is None
    assert app.openapi_url is None
