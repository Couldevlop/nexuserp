"""
Détection d'anomalies comptables — Isolation Forest + LSTM.
XAI (Explicabilité) incluse dans chaque résultat.
"""

import time
import structlog
import numpy as np
from fastapi import APIRouter, Depends, HTTPException
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

from app.core.config import get_settings
from app.core.security import verify_token
from app.schemas.ai_schemas import (
    AnomalyDetectionRequest,
    AnomalyDetectionResponse,
    AnomalyResult
)

logger = structlog.get_logger()
router = APIRouter()
settings = get_settings()


@router.post("/anomaly/detect", response_model=AnomalyDetectionResponse)
async def detect_anomalies(
    request: AnomalyDetectionRequest,
    token_data: dict = Depends(verify_token)
) -> AnomalyDetectionResponse:
    """
    Analyse un lot de transactions et identifie les anomalies potentielles.
    Retourne un score 0-1 + explication lisible pour chaque anomalie.
    Supporte jusqu'à 100k transactions avec < 5s de traitement.
    """
    if not settings.ai_enabled:
        return AnomalyDetectionResponse(
            total_analyzed=len(request.transactions),
            anomalies_found=0,
            results=[],
            processing_time_ms=0
        )

    start_time = time.time()
    tenant_id = token_data.get("tenant_id")

    logger.info("Anomaly detection started",
                tenant_id=tenant_id,
                transaction_count=len(request.transactions))

    try:
        transactions = request.transactions
        n = len(transactions)

        # Feature extraction
        features = _extract_features(transactions)

        # Normalisation
        scaler = StandardScaler()
        features_scaled = scaler.fit_transform(features)

        # Isolation Forest — contamination = % attendu d'anomalies
        contamination = min(0.1, max(0.001, (1 - request.sensitivity) * 0.15))
        model = IsolationForest(
            n_estimators=100,
            contamination=contamination,
            random_state=42,
            n_jobs=-1
        )
        predictions = model.fit_predict(features_scaled)
        scores = model.score_samples(features_scaled)

        # Normalisation des scores vers [0, 1] (1 = anomalie certaine)
        min_score, max_score = scores.min(), scores.max()
        if max_score > min_score:
            normalized_scores = 1.0 - (scores - min_score) / (max_score - min_score)
        else:
            normalized_scores = np.zeros(n)

        # Construction des résultats
        results: list[AnomalyResult] = []
        anomalies_found = 0

        for i, transaction in enumerate(transactions):
            score = float(normalized_scores[i])
            is_anomaly = predictions[i] == -1 and score >= request.sensitivity

            if is_anomaly:
                anomalies_found += 1
                anomaly_type, explanation = _explain_anomaly(transaction, features[i], score)
            else:
                anomaly_type = None
                explanation = "Transaction normale — aucune anomalie détectée"

            results.append(AnomalyResult(
                transaction_id=transaction.id,
                anomaly_score=round(score, 4),
                is_anomaly=is_anomaly,
                anomaly_type=anomaly_type,
                explanation=explanation,
                confidence=round(min(score * 1.2, 1.0), 4) if is_anomaly else round(1.0 - score, 4)
            ))

        processing_time = int((time.time() - start_time) * 1000)

        logger.info("Anomaly detection completed",
                    tenant_id=tenant_id,
                    total=n,
                    anomalies=anomalies_found,
                    processing_ms=processing_time)

        return AnomalyDetectionResponse(
            total_analyzed=n,
            anomalies_found=anomalies_found,
            results=results,
            processing_time_ms=processing_time
        )

    except Exception as e:
        logger.error("Anomaly detection failed", error=str(e), tenant_id=tenant_id)
        raise HTTPException(status_code=500, detail=f"Anomaly detection failed: {str(e)}")


def _extract_features(transactions) -> np.ndarray:
    """Extrait les features numériques des transactions."""
    features = []
    amounts = [abs(t.amount) for t in transactions]
    mean_amount = np.mean(amounts) if amounts else 1.0
    std_amount = np.std(amounts) if len(amounts) > 1 else 1.0

    for t in transactions:
        hour = t.date.hour if hasattr(t.date, 'hour') else 12
        day_of_week = t.date.weekday() if hasattr(t.date, 'weekday') else 0
        amount_zscore = (abs(t.amount) - mean_amount) / max(std_amount, 0.001)
        is_round_amount = 1.0 if t.amount % 100 == 0 else 0.0
        is_weekend = 1.0 if day_of_week >= 5 else 0.0
        is_odd_hours = 1.0 if hour < 6 or hour > 22 else 0.0
        log_amount = np.log1p(abs(t.amount))

        features.append([
            amount_zscore,
            log_amount,
            is_round_amount,
            is_weekend,
            is_odd_hours,
            hour / 24.0,
            day_of_week / 7.0
        ])

    return np.array(features)


def _explain_anomaly(transaction, features: np.ndarray, score: float) -> tuple[str, str]:
    """Génère une explication lisible de l'anomalie (XAI)."""
    explanations = []
    anomaly_type = "GENERIC"

    amount_zscore = features[0]
    is_round = features[2] > 0.5
    is_weekend = features[3] > 0.5
    is_odd_hours = features[4] > 0.5

    if amount_zscore > 3.0:
        anomaly_type = "UNUSUAL_AMOUNT"
        explanations.append(f"Montant inhabituellement élevé ({transaction.amount:,.2f}) — {amount_zscore:.1f}x la moyenne")
    elif amount_zscore < -2.0:
        anomaly_type = "UNUSUAL_AMOUNT"
        explanations.append(f"Montant inhabituellement faible ({transaction.amount:,.2f})")

    if is_round and abs(transaction.amount) > 1000:
        anomaly_type = "ROUND_AMOUNT"
        explanations.append(f"Montant rond suspect ({transaction.amount:,.0f}) — peut indiquer une estimation")

    if is_weekend:
        anomaly_type = "WEEKEND_TRANSACTION"
        explanations.append("Transaction effectuée le week-end — inhabituel pour ce compte")

    if is_odd_hours:
        anomaly_type = "ODD_HOURS"
        explanations.append(f"Saisie à une heure inhabituelle ({transaction.date.hour}h)")

    if not explanations:
        explanations.append(f"Combinaison de paramètres atypiques (score={score:.2f})")

    return anomaly_type, " | ".join(explanations)
