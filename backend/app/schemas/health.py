"""Schemas for service discovery and health responses."""

from typing import Literal

from pydantic import BaseModel, ConfigDict


class HealthResponse(BaseModel):
    """Minimal response that does not expose deployment details."""

    model_config = ConfigDict(extra="forbid")

    status: Literal["ok", "ready"]


class ServiceResponse(BaseModel):
    """Stable service discovery response."""

    model_config = ConfigDict(extra="forbid")

    service: Literal["miranova-api"]
