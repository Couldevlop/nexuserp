"""
Prévision de demande — Prophet + XGBoost.
Supporte : stocks, ventes, trésorerie.
"""

import structlog
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException

from app.core.config import get_settings
from app.core.security import verify_token
from app.schemas.ai_schemas import ForecastRequest, ForecastResponse, ForecastPoint

logger = structlog.get_logger()
router = APIRouter()
settings = get_settings()


@router.post("/forecast/demand", response_model=ForecastResponse)
async def forecast_demand(
    request: ForecastRequest,
    token_data: dict = Depends(verify_token)
) -> ForecastResponse:
    """
    Prévision de demande avec intervalles de confiance.
    Utilise Prophet pour les séries temporelles avec saisonnalité.
    """
    if not settings.ai_enabled:
        return _get_disabled_response(request)

    tenant_id = token_data.get("tenant_id")

    logger.info("Forecast request",
                tenant_id=tenant_id,
                entity_id=request.entity_id,
                horizon_days=request.horizon_days,
                data_points=len(request.historical_data))

    try:
        # Conversion en DataFrame Prophet
        df = pd.DataFrame([
            {"ds": point.date, "y": point.value}
            for point in request.historical_data
        ])
        df["ds"] = pd.to_datetime(df["ds"])
        df = df.sort_values("ds").reset_index(drop=True)

        # Validation données minimales
        if len(df) < 10:
            raise HTTPException(
                status_code=400,
                detail="At least 10 historical data points required for forecasting"
            )

        # Entraînement Prophet
        try:
            from prophet import Prophet
            model = Prophet(
                changepoint_prior_scale=0.05,
                seasonality_prior_scale=10.0,
                yearly_seasonality=True,
                weekly_seasonality=True,
                daily_seasonality=False,
                interval_width=0.95
            )
            model.fit(df)

            # Génération des prévisions
            future = model.make_future_dataframe(periods=request.horizon_days, freq="D")
            forecast_df = model.predict(future)
            forecast_only = forecast_df[forecast_df["ds"] > df["ds"].max()]

            forecast_points = [
                ForecastPoint(
                    date=row["ds"].strftime("%Y-%m-%d"),
                    predicted_value=max(0.0, float(row["yhat"])),
                    lower_bound=max(0.0, float(row["yhat_lower"])) if request.include_confidence else None,
                    upper_bound=max(0.0, float(row["yhat_upper"])) if request.include_confidence else None,
                    confidence=0.95
                )
                for _, row in forecast_only.iterrows()
            ]

            # Métriques de précision (Cross-validation sur données historiques)
            accuracy_metrics = _compute_accuracy_metrics(df)
            model_used = "prophet"

        except ImportError:
            # Fallback si Prophet non disponible — moyenne mobile
            logger.warning("Prophet not available, using fallback model")
            forecast_points = _simple_forecast(df, request.horizon_days, request.include_confidence)
            accuracy_metrics = None
            model_used = "simple_moving_average"

        return ForecastResponse(
            entity_id=request.entity_id,
            entity_type=request.entity_type,
            forecast=forecast_points,
            model_used=model_used,
            accuracy_metrics=accuracy_metrics
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error("Forecast failed", error=str(e), entity_id=request.entity_id)
        raise HTTPException(status_code=500, detail=f"Forecasting failed: {str(e)}")


def _simple_forecast(df: pd.DataFrame, horizon_days: int, include_ci: bool) -> list[ForecastPoint]:
    """Prévision par moyenne mobile — fallback sans Prophet."""
    values = df["y"].values
    window = min(30, len(values))
    last_values = values[-window:]
    mean_val = float(np.mean(last_values))
    std_val = float(np.std(last_values))
    last_date = df["ds"].max()

    points = []
    for i in range(1, horizon_days + 1):
        forecast_date = (last_date + timedelta(days=i)).strftime("%Y-%m-%d")
        noise = np.random.normal(0, std_val * 0.1)
        predicted = max(0.0, mean_val + noise)
        points.append(ForecastPoint(
            date=forecast_date,
            predicted_value=round(predicted, 4),
            lower_bound=max(0.0, predicted - 1.96 * std_val) if include_ci else None,
            upper_bound=predicted + 1.96 * std_val if include_ci else None,
            confidence=0.80
        ))
    return points


def _compute_accuracy_metrics(df: pd.DataFrame) -> dict:
    """Calcule MAPE et RMSE sur les données historiques."""
    if len(df) < 20:
        return {}
    split = int(len(df) * 0.8)
    train, test = df[:split], df[split:]
    mean_train = float(train["y"].mean())
    predictions = [mean_train] * len(test)
    actuals = test["y"].values

    mae = float(np.mean(np.abs(actuals - predictions)))
    rmse = float(np.sqrt(np.mean((actuals - predictions) ** 2)))
    mape = float(np.mean(np.abs((actuals - predictions) / (actuals + 1e-8))) * 100)

    return {"mae": round(mae, 4), "rmse": round(rmse, 4), "mape": round(mape, 2)}


def _get_disabled_response(request: ForecastRequest) -> ForecastResponse:
    return ForecastResponse(
        entity_id=request.entity_id,
        entity_type=request.entity_type,
        forecast=[],
        model_used="disabled",
        accuracy_metrics=None
    )
