"""
NexusERP AI — Invoice Extraction
Extraction structurée de factures PDF/image via LLM (Vision) + OCR.
"""

import base64
from typing import Optional

import structlog
from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from pydantic import BaseModel, Field

from app.core.config import Settings, get_settings
from app.core.security import verify_jwt

logger = structlog.get_logger()
router = APIRouter()


class ExtractedInvoice(BaseModel):
    """Facture extraite structurée."""
    supplier_name: Optional[str] = None
    supplier_address: Optional[str] = None
    supplier_tax_id: Optional[str] = None
    invoice_number: Optional[str] = None
    invoice_date: Optional[str] = None
    due_date: Optional[str] = None
    currency: Optional[str] = "EUR"
    subtotal_ht: Optional[float] = None
    vat_amount: Optional[float] = None
    total_ttc: Optional[float] = None
    payment_terms: Optional[str] = None
    line_items: list[dict] = Field(default_factory=list)
    confidence_score: float = Field(default=0.0, ge=0.0, le=1.0)
    raw_text: Optional[str] = None


class ExtractionResponse(BaseModel):
    invoice: ExtractedInvoice
    processing_time_ms: int
    model_used: str
    ai_enabled: bool


def _static_extraction_response() -> ExtractionResponse:
    """Retourne une réponse statique si AI_ENABLED=false."""
    return ExtractionResponse(
        invoice=ExtractedInvoice(
            supplier_name="Exemple Fournisseur SARL",
            invoice_number="FA-2026-00001",
            invoice_date="2026-01-15",
            due_date="2026-02-15",
            currency="EUR",
            subtotal_ht=1000.0,
            vat_amount=200.0,
            total_ttc=1200.0,
            confidence_score=0.0,
            line_items=[{"description": "Prestation de service", "quantity": 1, "unit_price": 1000.0, "total": 1000.0}]
        ),
        processing_time_ms=0,
        model_used="static",
        ai_enabled=False
    )


@router.post(
    "/documents/extract",
    response_model=ExtractionResponse,
    summary="Extraire les données d'une facture PDF/image",
    description="Utilise un LLM Vision pour extraire structurellement les données d'une facture scannée ou PDF."
)
async def extract_invoice(
    file: UploadFile = File(..., description="Fichier PDF ou image de la facture"),
    tenant_id: str = Form(...),
    settings: Settings = Depends(get_settings),
    _token: dict = Depends(verify_jwt)
) -> ExtractionResponse:
    import time
    start = time.time()

    if not settings.ai_enabled:
        return _static_extraction_response()

    # Validation du fichier
    allowed_types = {"application/pdf", "image/jpeg", "image/png", "image/webp"}
    if file.content_type not in allowed_types:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Type de fichier non supporté: {file.content_type}. Acceptés: {allowed_types}"
        )

    file_bytes = await file.read()
    if len(file_bytes) > 10 * 1024 * 1024:  # 10 MB max
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail="Fichier trop volumineux (max 10 MB)"
        )

    try:
        import anthropic
        client = anthropic.AsyncAnthropic(api_key=settings.anthropic_api_key)

        # Encode en base64 pour l'API Vision
        file_b64 = base64.standard_b64encode(file_bytes).decode("utf-8")
        media_type = file.content_type if file.content_type != "application/pdf" else "image/jpeg"

        prompt = """Analysez cette facture et extrayez les informations suivantes au format JSON strict:
{
  "supplier_name": "nom du fournisseur",
  "supplier_address": "adresse complète",
  "supplier_tax_id": "SIRET ou numéro fiscal",
  "invoice_number": "numéro de facture",
  "invoice_date": "date format YYYY-MM-DD",
  "due_date": "date d'échéance format YYYY-MM-DD",
  "currency": "EUR/XOF/USD",
  "subtotal_ht": 0.00,
  "vat_amount": 0.00,
  "total_ttc": 0.00,
  "payment_terms": "conditions de paiement",
  "line_items": [{"description": "...", "quantity": 1, "unit_price": 0.00, "vat_rate": 0.20, "total": 0.00}],
  "confidence_score": 0.95
}
Répondez UNIQUEMENT avec le JSON, sans texte supplémentaire."""

        response = await client.messages.create(
            model=settings.anthropic_model,
            max_tokens=2000,
            messages=[{
                "role": "user",
                "content": [
                    {"type": "image", "source": {"type": "base64", "media_type": media_type, "data": file_b64}},
                    {"type": "text", "text": prompt}
                ]
            }]
        )

        import json
        extracted_data = json.loads(response.content[0].text)
        invoice = ExtractedInvoice(**extracted_data)

        elapsed_ms = int((time.time() - start) * 1000)
        logger.info("invoice_extracted", tenant_id=tenant_id, confidence=invoice.confidence_score, ms=elapsed_ms)

        return ExtractionResponse(
            invoice=invoice,
            processing_time_ms=elapsed_ms,
            model_used=settings.anthropic_model,
            ai_enabled=True
        )

    except Exception as e:
        logger.error("invoice_extraction_failed", tenant_id=tenant_id, error=str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Erreur lors de l'extraction: {str(e)}"
        )
