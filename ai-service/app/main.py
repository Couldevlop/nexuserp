"""NexusAI — Service IA FastAPI."""

import structlog
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from prometheus_client import make_asgi_app

from app.core.config import get_settings
from app.api.v1 import (
    chat_assistant,
    anomaly_detection,
    demand_forecasting,
    compliance_checker,
    invoice_extraction,
    predictive_analytics,
)

logger = structlog.get_logger()
settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("NexusAI starting", version=settings.app_version, ai_enabled=settings.ai_enabled)
    yield
    logger.info("NexusAI shutting down")


app = FastAPI(
    title="NexusERP AI Service",
    description="Service IA NexusERP — Détection d'anomalies, Prévisions, Assistant conversationnel",
    version=settings.app_version,
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "https://*.nexuserp.io"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Prometheus metrics endpoint
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)

# Routers
app.include_router(chat_assistant.router, prefix="/ai/v1", tags=["Chat Assistant"])
app.include_router(anomaly_detection.router, prefix="/ai/v1", tags=["Anomaly Detection"])
app.include_router(demand_forecasting.router, prefix="/ai/v1", tags=["Demand Forecasting"])
app.include_router(compliance_checker.router, prefix="/ai/v1", tags=["Compliance"])
app.include_router(invoice_extraction.router, prefix="/ai/v1", tags=["Invoice Extraction"])
app.include_router(predictive_analytics.router, prefix="/ai/v1", tags=["Predictive Analytics"])


@app.get("/health")
async def health() -> dict:
    return {
        "status": "healthy",
        "service": "nexus-ai",
        "version": settings.app_version,
        "ai_enabled": settings.ai_enabled
    }


@app.get("/ai/v1/status")
async def ai_status() -> dict:
    return {
        "ai_enabled": settings.ai_enabled,
        "model": settings.anthropic_model if settings.ai_enabled else None,
        "capabilities": [
            "chat_assistant",
            "anomaly_detection",
            "demand_forecasting",
            "compliance_checker"
        ] if settings.ai_enabled else []
    }
