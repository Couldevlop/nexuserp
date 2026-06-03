"""Tests for chat assistant endpoint."""
import pytest
from httpx import AsyncClient
from unittest.mock import patch, AsyncMock, MagicMock

from app.main import app


@pytest.fixture
async def client():
    async with AsyncClient(app=app, base_url="http://test") as c:
        yield c


@pytest.fixture
def valid_auth():
    return {"Authorization": "Bearer test-token"}


@pytest.fixture
def mock_user():
    return {
        "sub": "user-001",
        "tenantId": "fr-acme",
        "realm_access": {"roles": ["FINANCE_USER"]},
    }


class TestChatAssistantEndpoint:

    @pytest.mark.asyncio
    async def test_requires_authentication(self, client):
        """Should return 401 without token."""
        response = await client.post(
            "/ai/v1/chat",
            json={"message": "Hello", "tenant_id": "fr-acme", "module": "finance"}
        )
        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_returns_response_for_valid_message(
        self, client, valid_auth, mock_user
    ):
        """Should return AI response for valid message."""
        mock_message = MagicMock()
        mock_message.content = [MagicMock(text="Voici les factures en attente : …")]
        mock_message.usage = MagicMock(input_tokens=100, output_tokens=50)

        with patch("app.api.v1.chat_assistant.verify_token", return_value=mock_user), \
             patch("app.api.v1.chat_assistant.anthropic_client") as mock_client:
            mock_client.messages.create = AsyncMock(return_value=mock_message)

            response = await client.post(
                "/ai/v1/chat",
                headers=valid_auth,
                json={
                    "message": "Montre-moi les factures en attente",
                    "tenant_id": "fr-acme",
                    "module": "finance",
                    "user_role": "FINANCE_USER",
                    "country": "FR",
                }
            )

        assert response.status_code == 200
        data = response.json()
        assert "response" in data
        assert len(data["response"]) > 0

    @pytest.mark.asyncio
    async def test_graceful_fallback_when_ai_disabled(
        self, client, valid_auth, mock_user
    ):
        """Should return fallback message when AI is disabled."""
        with patch("app.api.v1.chat_assistant.verify_token", return_value=mock_user), \
             patch("app.core.config.settings.ai_enabled", False):

            response = await client.post(
                "/ai/v1/chat",
                headers=valid_auth,
                json={
                    "message": "Test message",
                    "tenant_id": "fr-acme",
                    "module": "dashboard",
                    "user_role": "VIEWER",
                    "country": "FR",
                }
            )

        assert response.status_code == 200
        data = response.json()
        assert "response" in data
        assert data.get("ai_disabled") is True

    @pytest.mark.asyncio
    async def test_validates_required_fields(self, client, valid_auth, mock_user):
        """Should return 422 when required fields missing."""
        with patch("app.api.v1.chat_assistant.verify_token", return_value=mock_user):
            response = await client.post(
                "/ai/v1/chat",
                headers=valid_auth,
                json={"module": "finance"}  # missing 'message'
            )
        assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_handles_anthropic_api_error_gracefully(
        self, client, valid_auth, mock_user
    ):
        """Should return 500 with friendly message on Anthropic API failure."""
        with patch("app.api.v1.chat_assistant.verify_token", return_value=mock_user), \
             patch("app.api.v1.chat_assistant.anthropic_client") as mock_client:
            mock_client.messages.create = AsyncMock(
                side_effect=Exception("Anthropic API unavailable")
            )

            response = await client.post(
                "/ai/v1/chat",
                headers=valid_auth,
                json={
                    "message": "Test",
                    "tenant_id": "fr-acme",
                    "module": "finance",
                    "user_role": "FINANCE_USER",
                    "country": "FR",
                }
            )

        assert response.status_code in (500, 503)

    @pytest.mark.asyncio
    async def test_message_length_limit(self, client, valid_auth, mock_user):
        """Should reject messages exceeding maximum length."""
        long_message = "A" * 10_001  # Over 10k chars limit

        with patch("app.api.v1.chat_assistant.verify_token", return_value=mock_user):
            response = await client.post(
                "/ai/v1/chat",
                headers=valid_auth,
                json={
                    "message": long_message,
                    "tenant_id": "fr-acme",
                    "module": "finance",
                    "user_role": "FINANCE_USER",
                    "country": "FR",
                }
            )

        assert response.status_code == 422


class TestHealthEndpoint:

    @pytest.mark.asyncio
    async def test_health_returns_ok(self, client):
        """Health endpoint should always return 200."""
        response = await client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"

    @pytest.mark.asyncio
    async def test_health_includes_version(self, client):
        """Health should include version info."""
        response = await client.get("/health")
        data = response.json()
        assert "version" in data
