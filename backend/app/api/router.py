"""Top-level API router."""

from fastapi import APIRouter

from app.api.routes import health
from app.schemas.health import ServiceResponse

api_router = APIRouter()
api_router.include_router(health.router)


@api_router.get(
    "/",
    response_model=ServiceResponse,
    response_model_exclude_none=True,
    tags=["service"],
    summary="Discover the API service",
)
async def service_root() -> ServiceResponse:
    """Return a stable, non-sensitive service identifier."""

    return ServiceResponse(service="miranova-api")
