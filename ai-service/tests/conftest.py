"""Pytest configuration and shared fixtures."""
import pytest
import asyncio
from typing import AsyncGenerator


@pytest.fixture(scope="session")
def event_loop():
    """Create a single event loop for the entire test session."""
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(scope="session", autouse=True)
def set_test_env(monkeypatch_session):
    """Set test environment variables."""
    monkeypatch_session.setenv("ANTHROPIC_API_KEY", "test-key-xxxx")
    monkeypatch_session.setenv("AI_ENABLED", "true")
    monkeypatch_session.setenv("KEYCLOAK_URL", "http://localhost:8180/realms/nexuserp")
    monkeypatch_session.setenv("KEYCLOAK_CLIENT_ID", "nexuserp-frontend")


@pytest.fixture(scope="session")
def monkeypatch_session(request):
    """Session-scoped monkeypatch."""
    from _pytest.monkeypatch import MonkeyPatch
    mp = MonkeyPatch()
    yield mp
    mp.undo()


@pytest.fixture
def tenant_fr():
    return {
        "tenant_id": "fr-test",
        "country": "FR",
        "currency": "EUR",
        "accounting_standard": "PCG",
    }


@pytest.fixture
def tenant_ci():
    return {
        "tenant_id": "ci-test",
        "country": "CI",
        "currency": "XOF",
        "accounting_standard": "SYSCOHADA",
    }
