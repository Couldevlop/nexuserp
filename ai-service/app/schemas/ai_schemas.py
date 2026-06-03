"""Schemas Pydantic v2 — Entrées/Sorties API IA."""

from pydantic import BaseModel, Field
from typing import Optional, List, Any
from datetime import datetime
from enum import Enum


# ─── Chat Assistant ──────────────────────────────────────────────────────────

class ChatMessage(BaseModel):
    role: str = Field(..., pattern="^(user|assistant)$")
    content: str = Field(..., min_length=1, max_length=10000)


class ChatRequest(BaseModel):
    messages: List[ChatMessage] = Field(..., min_length=1)
    module: Optional[str] = None       # finance, inventory, hr, etc.
    context: Optional[dict] = None     # Données contextuelles du module
    stream: bool = False


class ChatResponse(BaseModel):
    message: ChatMessage
    usage: dict
    model: str
    tenant_id: str
    sources: List[str] = []


# ─── Anomaly Detection ───────────────────────────────────────────────────────

class TransactionRecord(BaseModel):
    id: str
    tenant_id: str
    amount: float
    account_code: str
    description: str
    date: datetime
    created_by: str
    metadata: Optional[dict] = None


class AnomalyDetectionRequest(BaseModel):
    transactions: List[TransactionRecord] = Field(..., min_length=1, max_length=10000)
    sensitivity: float = Field(default=0.7, ge=0.0, le=1.0)


class AnomalyResult(BaseModel):
    transaction_id: str
    anomaly_score: float           # 0.0 (normal) à 1.0 (anomalie certaine)
    is_anomaly: bool
    anomaly_type: Optional[str]    # DUPLICATE, ROUND_AMOUNT, ODD_HOURS, etc.
    explanation: str               # Explication lisible (XAI)
    confidence: float


class AnomalyDetectionResponse(BaseModel):
    total_analyzed: int
    anomalies_found: int
    results: List[AnomalyResult]
    processing_time_ms: int


# ─── Demand Forecasting ──────────────────────────────────────────────────────

class HistoricalDataPoint(BaseModel):
    date: str                       # YYYY-MM-DD
    value: float
    metadata: Optional[dict] = None


class ForecastRequest(BaseModel):
    entity_id: str                  # ID produit, client, compte
    entity_type: str                # PRODUCT, CUSTOMER, ACCOUNT
    historical_data: List[HistoricalDataPoint] = Field(..., min_length=10)
    horizon_days: int = Field(default=30, ge=7, le=365)
    include_confidence: bool = True


class ForecastPoint(BaseModel):
    date: str
    predicted_value: float
    lower_bound: Optional[float] = None
    upper_bound: Optional[float] = None
    confidence: float = 0.95


class ForecastResponse(BaseModel):
    entity_id: str
    entity_type: str
    forecast: List[ForecastPoint]
    model_used: str
    accuracy_metrics: Optional[dict] = None


# ─── Document Intelligence ───────────────────────────────────────────────────

class DocumentExtractionResponse(BaseModel):
    document_type: str              # INVOICE, RECEIPT, CONTRACT
    extracted_fields: dict          # Champs extraits structurés
    confidence: float
    raw_text: str
    suggested_account_code: Optional[str] = None
    suggested_journal: Optional[str] = None


# ─── Compliance Checker ──────────────────────────────────────────────────────

class ComplianceCheckRequest(BaseModel):
    entry_type: str                 # JOURNAL_ENTRY, INVOICE, etc.
    country: str                    # FR, CI, BE, etc.
    data: dict                      # Données à vérifier


class ComplianceIssue(BaseModel):
    severity: str                   # ERROR, WARNING, INFO
    rule_code: str
    message: str
    field: Optional[str] = None
    suggestion: Optional[str] = None


class ComplianceCheckResponse(BaseModel):
    is_compliant: bool
    issues: List[ComplianceIssue]
    country: str
    framework: str                  # PCG, SYSCOHADA, IFRS


# ─── Natural Language Query ──────────────────────────────────────────────────

class NLQueryRequest(BaseModel):
    question: str = Field(..., min_length=5, max_length=1000)
    module: Optional[str] = None


class NLQueryResponse(BaseModel):
    question: str
    sql_query: str
    results: List[dict]
    explanation: str
    row_count: int


# ─── Predictive Analytics ────────────────────────────────────────────────────

class KPIPrediction(BaseModel):
    kpi_name: str
    current_value: float
    predicted_value: float
    horizon: str                    # J+7, J+30, J+90
    trend: str                      # UP, DOWN, STABLE
    confidence: float
    alert_level: Optional[str]      # GREEN, YELLOW, RED
    insight: str


class PredictiveAnalyticsResponse(BaseModel):
    tenant_id: str
    generated_at: datetime
    predictions: List[KPIPrediction]
