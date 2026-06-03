"""Configuration NexusAI — chargée depuis variables d'environnement."""

from pydantic_settings import BaseSettings
from functools import lru_cache
from typing import Optional


class Settings(BaseSettings):
    # Application
    app_name: str = "nexus-ai"
    app_version: str = "1.0.0"
    debug: bool = False
    ai_enabled: bool = True

    # Database
    database_url: str = "postgresql+asyncpg://nexus:nexus_dev_secret@localhost:5432/nexuserp"

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group: str = "nexus-ai"

    # Redis
    redis_url: str = "redis://localhost:6379"
    redis_password: Optional[str] = "nexus_redis_secret"

    # Anthropic (Claude API)
    anthropic_api_key: Optional[str] = None
    anthropic_model: str = "claude-sonnet-4-6"
    anthropic_max_tokens: int = 8192

    # Keycloak
    keycloak_url: str = "http://localhost:8180"
    keycloak_realm: str = "nexuserp"

    # Seuils IA
    anomaly_score_threshold: float = 0.7
    forecast_horizon_days: int = 30

    # OpenTelemetry
    otlp_endpoint: str = "http://localhost:4317"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    return Settings()
