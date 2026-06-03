"""
Vérification de conformité légale — PCG France & SYSCOHADA CI.
Valide les écritures comptables et factures.
"""

import structlog
from fastapi import APIRouter, Depends

from app.core.config import get_settings
from app.core.security import verify_token
from app.schemas.ai_schemas import (
    ComplianceCheckRequest, ComplianceCheckResponse, ComplianceIssue
)

logger = structlog.get_logger()
router = APIRouter()
settings = get_settings()

# Règles PCG France
PCG_RULES = {
    "balance_required": "Toute écriture doit être équilibrée (débit = crédit)",
    "valid_accounts_fr": "Les comptes doivent appartenir au PCG (classes 1-9)",
    "tva_rates_fr": {0, 2.1, 5.5, 10.0, 20.0},
    "min_date": "La date d'écriture ne peut pas être antérieure à la date d'ouverture de l'exercice",
}

# Règles SYSCOHADA
SYSCOHADA_RULES = {
    "valid_accounts_ci": "Les comptes doivent appartenir au SYSCOHADA (classes 1-9)",
    "tva_rates_ci": {0, 18.0},
    "currency": "XOF ou EUR uniquement",
}


@router.post("/compliance/check", response_model=ComplianceCheckResponse)
async def check_compliance(
    request: ComplianceCheckRequest,
    token_data: dict = Depends(verify_token)
) -> ComplianceCheckResponse:
    """
    Vérifie la conformité d'une écriture ou d'une facture
    avec le référentiel comptable du pays du tenant.
    """
    issues: list[ComplianceIssue] = []

    if request.country in ("FR", "BE"):
        issues.extend(_check_pcg_france(request.data, request.entry_type))
    elif request.country in ("CI", "SN", "ML", "BF"):
        issues.extend(_check_syscohada(request.data, request.entry_type))
    else:
        issues.extend(_check_ifrs(request.data, request.entry_type))

    is_compliant = not any(i.severity == "ERROR" for i in issues)
    framework = "PCG" if request.country == "FR" else \
                "SYSCOHADA" if request.country in ("CI", "SN", "ML", "BF") else "IFRS"

    logger.info("Compliance check",
                country=request.country,
                entry_type=request.entry_type,
                is_compliant=is_compliant,
                issues=len(issues))

    return ComplianceCheckResponse(
        is_compliant=is_compliant,
        issues=issues,
        country=request.country,
        framework=framework
    )


def _check_pcg_france(data: dict, entry_type: str) -> list[ComplianceIssue]:
    issues = []

    if entry_type == "JOURNAL_ENTRY":
        lines = data.get("lines", [])

        # Équilibre débit/crédit
        total_debit = sum(l.get("debit", 0) for l in lines)
        total_credit = sum(l.get("credit", 0) for l in lines)
        if abs(total_debit - total_credit) > 0.01:
            issues.append(ComplianceIssue(
                severity="ERROR",
                rule_code="PCG_BALANCE",
                message=f"Écriture déséquilibrée : débit={total_debit:.2f}, crédit={total_credit:.2f}",
                field="lines",
                suggestion="Vérifiez que la somme des débits est égale à la somme des crédits"
            ))

        # Vérification comptes PCG
        for line in lines:
            account = str(line.get("account_number", ""))
            if account and not account[0].isdigit():
                issues.append(ComplianceIssue(
                    severity="ERROR",
                    rule_code="PCG_ACCOUNT",
                    message=f"Compte invalide pour le PCG France : {account}",
                    field="account_number",
                    suggestion="Utilisez un compte du Plan Comptable Général (classes 1-9)"
                ))

    elif entry_type == "INVOICE":
        # TVA
        tax_rate = data.get("tax_rate")
        if tax_rate is not None and float(tax_rate) not in PCG_RULES["tva_rates_fr"]:
            issues.append(ComplianceIssue(
                severity="ERROR",
                rule_code="PCG_TVA_RATE",
                message=f"Taux TVA {tax_rate}% non valide en France",
                field="tax_rate",
                suggestion=f"Taux valides : {sorted(PCG_RULES['tva_rates_fr'])}"
            ))

        # Numéro de facture
        invoice_number = data.get("invoice_number")
        if not invoice_number:
            issues.append(ComplianceIssue(
                severity="ERROR",
                rule_code="PCG_INVOICE_NUMBER",
                message="Le numéro de facture est obligatoire (Art. 289 CGI)",
                field="invoice_number",
                suggestion="Générez un numéro séquentiel unique"
            ))

        # Date facture
        if not data.get("invoice_date"):
            issues.append(ComplianceIssue(
                severity="ERROR",
                rule_code="PCG_INVOICE_DATE",
                message="La date de facture est obligatoire",
                field="invoice_date"
            ))

        # Identité du vendeur
        if not data.get("seller_name") and not data.get("partner_name"):
            issues.append(ComplianceIssue(
                severity="WARNING",
                rule_code="PCG_SELLER_INFO",
                message="Les informations du vendeur sont recommandées",
                field="seller_name"
            ))

    return issues


def _check_syscohada(data: dict, entry_type: str) -> list[ComplianceIssue]:
    issues = []

    if entry_type == "JOURNAL_ENTRY":
        lines = data.get("lines", [])
        total_debit = sum(l.get("debit", 0) for l in lines)
        total_credit = sum(l.get("credit", 0) for l in lines)

        if abs(total_debit - total_credit) > 1:  # XOF = entiers
            issues.append(ComplianceIssue(
                severity="ERROR",
                rule_code="SYSCOHADA_BALANCE",
                message=f"Écriture déséquilibrée : débit={total_debit:.0f}, crédit={total_credit:.0f} F CFA",
                field="lines",
                suggestion="Vérifiez l'équilibre de l'écriture (SYSCOHADA)"
            ))

    elif entry_type == "INVOICE":
        tax_rate = data.get("tax_rate")
        if tax_rate is not None and float(tax_rate) not in SYSCOHADA_RULES["tva_rates_ci"]:
            issues.append(ComplianceIssue(
                severity="ERROR",
                rule_code="SYSCOHADA_TVA",
                message=f"Taux TVA {tax_rate}% non valide en UEMOA",
                field="tax_rate",
                suggestion="Taux TVA valides : 0% et 18%"
            ))

        currency = data.get("currency")
        if currency and currency not in ("XOF", "EUR", "USD"):
            issues.append(ComplianceIssue(
                severity="WARNING",
                rule_code="SYSCOHADA_CURRENCY",
                message=f"Devise {currency} inhabituelle pour la zone UEMOA",
                field="currency",
                suggestion="Utilisez XOF (FCFA), EUR ou USD"
            ))

    return issues


def _check_ifrs(data: dict, entry_type: str) -> list[ComplianceIssue]:
    issues = []
    if entry_type == "JOURNAL_ENTRY":
        lines = data.get("lines", [])
        total_debit = sum(l.get("debit", 0) for l in lines)
        total_credit = sum(l.get("credit", 0) for l in lines)
        if abs(total_debit - total_credit) > 0.01:
            issues.append(ComplianceIssue(
                severity="ERROR",
                rule_code="IFRS_BALANCE",
                message="Journal entry must be balanced (debit = credit)",
                field="lines"
            ))
    return issues
