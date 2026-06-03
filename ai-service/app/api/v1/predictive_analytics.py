"""
NexusERP AI — Predictive Analytics
KPI prédictifs : trésorerie J+30, risque client, churn RH, scoring fournisseur.
"""

from datetime import date, timedelta
from typing import Optional
import random

import structlog
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from app.core.config import Settings, get_settings
from app.core.security import verify_jwt

logger = structlog.get_logger()
router = APIRouter()


class CashFlowForecast(BaseModel):
    date: str
    predicted_balance: float
    lower_bound: float
    upper_bound: float
    confidence: float


class CustomerRiskScore(BaseModel):
    customer_id: str
    risk_score: float = Field(ge=0.0, le=1.0)
    risk_level: str  # LOW, MEDIUM, HIGH, CRITICAL
    overdue_amount: Optional[float] = None
    days_overdue: Optional[int] = None
    recommendation: str


class HrChurnRisk(BaseModel):
    employee_id: str
    churn_probability: float = Field(ge=0.0, le=1.0)
    risk_factors: list[str]
    recommendation: str


class KpiDashboard(BaseModel):
    tenant_id: str
    computed_at: str
    cash_flow_30d: list[CashFlowForecast]
    customer_risks: list[CustomerRiskScore]
    hr_churn_risks: list[HrChurnRisk]
    top_kpis: dict[str, float]
    ai_enabled: bool


def _generate_static_kpis(tenant_id: str) -> KpiDashboard:
    """Données statiques si AI_ENABLED=false."""
    today = date.today()
    cash_flow = [
        CashFlowForecast(
            date=(today + timedelta(days=i)).isoformat(),
            predicted_balance=50000.0 + i * 1000,
            lower_bound=45000.0 + i * 800,
            upper_bound=55000.0 + i * 1200,
            confidence=0.85
        )
        for i in range(1, 31)
    ]
    return KpiDashboard(
        tenant_id=tenant_id,
        computed_at=today.isoformat(),
        cash_flow_30d=cash_flow,
        customer_risks=[
            CustomerRiskScore(
                customer_id="DEMO-001",
                risk_score=0.2,
                risk_level="LOW",
                overdue_amount=0.0,
                days_overdue=0,
                recommendation="Client fiable, aucune action requise"
            )
        ],
        hr_churn_risks=[],
        top_kpis={
            "dso_days": 35.0,
            "gross_margin_pct": 42.5,
            "stock_turnover": 6.2,
            "current_ratio": 1.8,
            "overdue_receivables_pct": 8.3
        },
        ai_enabled=False
    )


@router.get(
    "/analytics/kpi/{tenant_id}",
    response_model=KpiDashboard,
    summary="Tableau de bord KPI prédictifs",
    description="Calcule et retourne les KPI prédictifs pour un tenant: trésorerie J+30, risques clients, churn RH."
)
async def get_predictive_kpis(
    tenant_id: str,
    settings: Settings = Depends(get_settings),
    _token: dict = Depends(verify_jwt)
) -> KpiDashboard:
    if not settings.ai_enabled:
        return _generate_static_kpis(tenant_id)

    # En production: charge les données depuis PostgreSQL et applique les modèles ML
    # Pour cette implémentation, on retourne des prédictions simulées réalistes
    today = date.today()

    # Cash flow forecast simulé (Prophet-like)
    seed = sum(ord(c) for c in tenant_id)
    rng = random.Random(seed)
    base_balance = rng.uniform(20000, 200000)

    cash_flow = []
    balance = base_balance
    for i in range(1, 31):
        delta = rng.gauss(500, 2000)
        balance += delta
        cash_flow.append(CashFlowForecast(
            date=(today + timedelta(days=i)).isoformat(),
            predicted_balance=round(balance, 2),
            lower_bound=round(balance * 0.85, 2),
            upper_bound=round(balance * 1.15, 2),
            confidence=round(0.95 - i * 0.01, 2)
        ))

    # Risques clients simulés
    customer_risks = [
        CustomerRiskScore(
            customer_id=f"CUST-{i:03d}",
            risk_score=round(rng.uniform(0, 1), 2),
            risk_level=["LOW", "MEDIUM", "HIGH", "CRITICAL"][int(rng.uniform(0, 4))],
            overdue_amount=round(rng.uniform(0, 5000), 2),
            days_overdue=int(rng.uniform(0, 120)),
            recommendation="Surveillance recommandée" if rng.random() > 0.5 else "Relance prioritaire"
        )
        for i in range(1, 6)
    ]

    logger.info("kpi_computed", tenant_id=tenant_id)

    return KpiDashboard(
        tenant_id=tenant_id,
        computed_at=today.isoformat(),
        cash_flow_30d=cash_flow,
        customer_risks=customer_risks,
        hr_churn_risks=[],
        top_kpis={
            "dso_days": round(rng.uniform(25, 65), 1),
            "gross_margin_pct": round(rng.uniform(30, 55), 1),
            "stock_turnover": round(rng.uniform(4, 12), 1),
            "current_ratio": round(rng.uniform(1.2, 3.0), 2),
            "overdue_receivables_pct": round(rng.uniform(5, 25), 1)
        },
        ai_enabled=True
    )


@router.get(
    "/analytics/cash-flow/{tenant_id}",
    summary="Prévision trésorerie J+90",
    response_model=list[CashFlowForecast]
)
async def get_cash_flow_forecast(
    tenant_id: str,
    days: int = 30,
    settings: Settings = Depends(get_settings),
    _token: dict = Depends(verify_jwt)
) -> list[CashFlowForecast]:
    if days > 90:
        raise HTTPException(status_code=400, detail="Horizon maximum: 90 jours")

    today = date.today()
    rng = random.Random(sum(ord(c) for c in tenant_id))
    balance = rng.uniform(30000, 150000)

    return [
        CashFlowForecast(
            date=(today + timedelta(days=i)).isoformat(),
            predicted_balance=round(balance := balance + rng.gauss(300, 1500), 2),
            lower_bound=round(balance * 0.80, 2),
            upper_bound=round(balance * 1.20, 2),
            confidence=round(max(0.5, 0.95 - i * 0.005), 2)
        )
        for i in range(1, days + 1)
    ]
