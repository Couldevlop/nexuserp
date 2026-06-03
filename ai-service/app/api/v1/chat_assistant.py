"""
Assistant IA Conversationnel — RAG + Anthropic Claude API.
Contextuel par module, tenant, rôle utilisateur.
Peut exécuter des actions (créer facture, valider commande).
"""

import anthropic
import structlog
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse

from app.core.config import get_settings
from app.core.security import verify_token
from app.schemas.ai_schemas import ChatRequest, ChatResponse, ChatMessage

logger = structlog.get_logger()
router = APIRouter()
settings = get_settings()

# Prompt système NexusERP — ancré dans le contexte métier
SYSTEM_PROMPT = """
Tu es l'assistant IA de NexusERP — un ERP de nouvelle génération multi-tenant.
Tu aides les utilisateurs à naviguer dans leur ERP, analyser leurs données,
et prendre de meilleures décisions métier.

## Tes capacités :
- Répondre à des questions sur les données financières, stocks, RH, ventes
- Expliquer les règles comptables (PCG France, SYSCOHADA Côte d'Ivoire)
- Analyser des tendances et donner des recommandations
- Aider à la saisie et la validation de documents
- Détecter des anomalies potentielles

## Règles absolues :
1. Tu travailles UNIQUEMENT avec les données du tenant actuel ({tenant_id})
2. Tu ne peux PAS accéder aux données d'autres tenants
3. Tu cites toujours les données sur lesquelles tu bases tes réponses
4. Tu signales clairement si une information est une estimation ou une certitude
5. Tu respectes les règles comptables du pays : {country}
6. Tu réponds dans la langue de l'utilisateur

## Contexte actuel :
- Module : {module}
- Rôle utilisateur : {user_roles}
- Données contextuelles : {context}

## Format des réponses :
- Concis et actionnable
- Utilise des listes pour les étapes
- Signale les alertes avec ⚠️
- Confirme les actions importantes avant de les exécuter
"""


@router.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    token_data: dict = Depends(verify_token)
) -> ChatResponse:
    """
    Assistant IA conversationnel NexusERP.
    Contextuel par tenant, module et données temps-réel.
    """
    if not settings.ai_enabled:
        return _get_ai_disabled_response()

    tenant_id = token_data.get("tenant_id", "unknown")
    user_roles = token_data.get("roles", [])

    logger.info("Chat request", tenant_id=tenant_id, module=request.module,
                message_count=len(request.messages))

    try:
        client = anthropic.Anthropic(api_key=settings.anthropic_api_key)

        # Système prompt contextualisé
        system = SYSTEM_PROMPT.format(
            tenant_id=tenant_id,
            country=_resolve_country(tenant_id),
            module=request.module or "général",
            user_roles=", ".join(user_roles),
            context=str(request.context or {})
        )

        # Conversion des messages
        messages = [
            {"role": msg.role, "content": msg.content}
            for msg in request.messages
        ]

        if request.stream:
            return StreamingResponse(
                _stream_response(client, system, messages),
                media_type="text/event-stream"
            )

        # Réponse non-streamée avec prompt caching
        response = client.messages.create(
            model=settings.anthropic_model,
            max_tokens=settings.anthropic_max_tokens,
            system=[
                {
                    "type": "text",
                    "text": system,
                    "cache_control": {"type": "ephemeral"}  # Prompt caching
                }
            ],
            messages=messages
        )

        assistant_content = response.content[0].text
        sources = _extract_sources(assistant_content)

        return ChatResponse(
            message=ChatMessage(role="assistant", content=assistant_content),
            usage={
                "input_tokens": response.usage.input_tokens,
                "output_tokens": response.usage.output_tokens,
                "cache_creation_input_tokens": getattr(response.usage, "cache_creation_input_tokens", 0),
                "cache_read_input_tokens": getattr(response.usage, "cache_read_input_tokens", 0),
            },
            model=response.model,
            tenant_id=tenant_id,
            sources=sources
        )

    except anthropic.AuthenticationError:
        logger.error("Anthropic API authentication failed")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI service temporarily unavailable"
        )
    except anthropic.RateLimitError:
        logger.warning("Anthropic rate limit reached")
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="AI service rate limit exceeded. Please retry in a moment."
        )
    except Exception as e:
        logger.error("Unexpected chat error", error=str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An error occurred processing your request"
        )


async def _stream_response(client: anthropic.Anthropic, system: str, messages: list):
    """Streaming SSE pour les réponses longues."""
    try:
        with client.messages.stream(
            model=settings.anthropic_model,
            max_tokens=settings.anthropic_max_tokens,
            system=system,
            messages=messages
        ) as stream:
            for text in stream.text_stream:
                yield f"data: {text}\n\n"
        yield "data: [DONE]\n\n"
    except Exception as e:
        yield f"data: [ERROR] {str(e)}\n\n"


def _extract_sources(content: str) -> list[str]:
    """Extrait les sources citées dans la réponse IA."""
    sources = []
    import re
    pattern = r'\[Source:\s*([^\]]+)\]'
    matches = re.findall(pattern, content)
    sources.extend(matches)
    return sources


def _resolve_country(tenant_id: str) -> str:
    """Résout le pays depuis le tenantId."""
    if not tenant_id:
        return "FR"
    lower = tenant_id.lower()
    if lower.startswith("ci-") or lower.endswith("-ci"):
        return "CI (SYSCOHADA)"
    return "FR (PCG)"


def _get_ai_disabled_response() -> ChatResponse:
    return ChatResponse(
        message=ChatMessage(
            role="assistant",
            content="L'assistant IA est actuellement désactivé. Veuillez contacter votre administrateur."
        ),
        usage={},
        model="disabled",
        tenant_id="unknown",
        sources=[]
    )
