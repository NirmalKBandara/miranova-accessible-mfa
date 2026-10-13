"""Non-sensitive service health endpoints."""

from fastapi import APIRouter, status

from app.schemas.health import HealthResponse

router = APIRouter(prefix="/health", tags=["health"])


@router.get(
    "",
    response_model=HealthResponse,
    status_code=status.HTTP_200_OK,
    summary="Check service liveness",
)
async def health_check() -> HealthResponse:
    """Confirm that the API process can serve requests."""

    return HealthResponse(status="ok")


@router.get(
    "/ready",
    response_model=HealthResponse,
    status_code=status.HTTP_200_OK,
    summary="Check service readiness",
)
async def readiness_check() -> HealthResponse:
    """Confirm readiness; dependency checks can be added as they are introduced."""

    return HealthResponse(status="ready")
