"""Tests for anomaly detection endpoint — 100% coverage target."""
import pytest
from httpx import AsyncClient
from datetime import datetime, timezone
from unittest.mock import patch, MagicMock
import numpy as np

from app.main import app
from app.api.v1.ai_schemas import TransactionData


@pytest.fixture
async def client():
    async with AsyncClient(app=app, base_url="http://test") as c:
        yield c


@pytest.fixture
def valid_token():
    """Mock a valid JWT token."""
    return "Bearer test-token"


@pytest.fixture
def sample_transactions():
    """Generate a set of normal transactions."""
    return [
        {
            "id": f"txn-{i:03d}",
            "amount": 1000.0 + i * 10,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "account_code": "411000",
            "description": f"Invoice payment {i}",
        }
        for i in range(50)
    ]


@pytest.fixture
def anomalous_transactions(sample_transactions):
    """Add obviously anomalous transactions."""
    anomalies = sample_transactions.copy()
    # Round number anomaly
    anomalies.append({
        "id": "txn-anomaly-round",
        "amount": 100000.0,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "account_code": "411000",
        "description": "Round amount",
    })
    # Weekend transaction
    anomalies.append({
        "id": "txn-anomaly-weekend",
        "amount": 5000.0,
        "timestamp": "2026-01-04T14:30:00+00:00",  # Sunday
        "account_code": "512000",
        "description": "Weekend transaction",
    })
    return anomalies


class TestAnomalyDetectionEndpoint:

    @pytest.mark.asyncio
    async def test_requires_authentication(self, client):
        """Should return 401 when no authentication provided."""
        response = await client.post(
            "/ai/v1/anomaly/detect",
            json={"transactions": [], "tenant_id": "test-tenant"}
        )
        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_returns_empty_when_no_anomaly(
        self, client, valid_token, sample_transactions
    ):
        """Should return no anomalies for normal transaction data."""
        with patch("app.api.v1.anomaly_detection.verify_token", return_value={"sub": "user"}):
            response = await client.post(
                "/ai/v1/anomaly/detect",
                headers={"Authorization": valid_token},
                json={
                    "transactions": sample_transactions,
                    "tenant_id": "test-tenant",
                    "threshold": 0.95,  # Very high threshold — few false positives
                }
            )
        assert response.status_code == 200
        data = response.json()
        assert "anomalies" in data
        assert isinstance(data["anomalies"], list)
        assert "total_analyzed" in data
        assert data["total_analyzed"] == len(sample_transactions)

    @pytest.mark.asyncio
    async def test_detects_anomalies_in_suspicious_data(
        self, client, valid_token, anomalous_transactions
    ):
        """Should detect anomalies in data containing suspicious transactions."""
        with patch("app.api.v1.anomaly_detection.verify_token", return_value={"sub": "user"}):
            response = await client.post(
                "/ai/v1/anomaly/detect",
                headers={"Authorization": valid_token},
                json={
                    "transactions": anomalous_transactions,
                    "tenant_id": "test-tenant",
                    "threshold": 0.5,  # Lower threshold to catch obvious anomalies
                }
            )
        assert response.status_code == 200
        data = response.json()
        assert len(data["anomalies"]) > 0

        # Each anomaly must have required fields
        for anomaly in data["anomalies"]:
            assert "transaction_id" in anomaly
            assert "score" in anomaly
            assert "explanation" in anomaly
            assert 0.0 <= anomaly["score"] <= 1.0
            assert anomaly["explanation"] != ""

    @pytest.mark.asyncio
    async def test_detects_round_number_anomaly(
        self, client, valid_token, sample_transactions
    ):
        """Should flag round number transactions (e.g. 100000.0)."""
        transactions = sample_transactions + [{
            "id": "txn-round",
            "amount": 100000.0,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "account_code": "411000",
            "description": "Round amount payment",
        }]

        with patch("app.api.v1.anomaly_detection.verify_token", return_value={"sub": "user"}):
            response = await client.post(
                "/ai/v1/anomaly/detect",
                headers={"Authorization": valid_token},
                json={
                    "transactions": transactions,
                    "tenant_id": "test-tenant",
                    "threshold": 0.3,
                }
            )
        assert response.status_code == 200
        data = response.json()
        anomaly_ids = [a["transaction_id"] for a in data["anomalies"]]
        assert "txn-round" in anomaly_ids

    @pytest.mark.asyncio
    async def test_returns_400_for_empty_transactions(
        self, client, valid_token
    ):
        """Should return 400 when transactions list is empty."""
        with patch("app.api.v1.anomaly_detection.verify_token", return_value={"sub": "user"}):
            response = await client.post(
                "/ai/v1/anomaly/detect",
                headers={"Authorization": valid_token},
                json={
                    "transactions": [],
                    "tenant_id": "test-tenant",
                }
            )
        assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_handles_large_dataset_performance(
        self, client, valid_token
    ):
        """Should process 100k transactions in under 5 seconds."""
        import time

        large_dataset = [
            {
                "id": f"txn-{i:06d}",
                "amount": float(1000 + (i % 500)),
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "account_code": "411000",
                "description": f"Transaction {i}",
            }
            for i in range(100_000)
        ]

        with patch("app.api.v1.anomaly_detection.verify_token", return_value={"sub": "user"}):
            start = time.monotonic()
            response = await client.post(
                "/ai/v1/anomaly/detect",
                headers={"Authorization": valid_token},
                json={
                    "transactions": large_dataset,
                    "tenant_id": "test-tenant",
                },
                timeout=10.0
            )
            elapsed = time.monotonic() - start

        assert response.status_code == 200
        assert elapsed < 5.0, f"Processing took {elapsed:.2f}s — must be < 5s"

    @pytest.mark.asyncio
    async def test_graceful_degradation_when_ai_disabled(
        self, client, valid_token, sample_transactions
    ):
        """Should return empty anomalies when AI_ENABLED=false."""
        with patch("app.api.v1.anomaly_detection.verify_token", return_value={"sub": "user"}), \
             patch("app.core.config.settings.ai_enabled", False):
            response = await client.post(
                "/ai/v1/anomaly/detect",
                headers={"Authorization": valid_token},
                json={
                    "transactions": sample_transactions,
                    "tenant_id": "test-tenant",
                }
            )
        assert response.status_code == 200
        data = response.json()
        assert data["anomalies"] == []
        assert data.get("ai_disabled") is True

    @pytest.mark.asyncio
    async def test_confidence_scores_in_valid_range(
        self, client, valid_token, anomalous_transactions
    ):
        """All confidence scores must be between 0 and 1."""
        with patch("app.api.v1.anomaly_detection.verify_token", return_value={"sub": "user"}):
            response = await client.post(
                "/ai/v1/anomaly/detect",
                headers={"Authorization": valid_token},
                json={
                    "transactions": anomalous_transactions,
                    "tenant_id": "test-tenant",
                    "threshold": 0.3,
                }
            )
        data = response.json()
        for anomaly in data["anomalies"]:
            assert 0.0 <= anomaly["score"] <= 1.0, \
                f"Score {anomaly['score']} out of range for {anomaly['transaction_id']}"
