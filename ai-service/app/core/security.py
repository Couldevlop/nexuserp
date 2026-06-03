"""Sécurité JWT — valide les tokens Keycloak."""

from fastapi import HTTPException, Security, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt
from typing import Optional
import httpx
import structlog

from app.core.config import get_settings

logger = structlog.get_logger()
security = HTTPBearer()
settings = get_settings()

# Cache JWKS
_jwks_cache: Optional[dict] = None


async def get_jwks() -> dict:
    global _jwks_cache
    if _jwks_cache is None:
        jwks_url = f"{settings.keycloak_url}/realms/{settings.keycloak_realm}/protocol/openid-connect/certs"
        async with httpx.AsyncClient() as client:
            response = await client.get(jwks_url)
            response.raise_for_status()
            _jwks_cache = response.json()
    return _jwks_cache


async def verify_token(credentials: HTTPAuthorizationCredentials = Security(security)) -> dict:
    """Vérifie le JWT Keycloak et retourne le payload."""
    token = credentials.credentials
    try:
        jwks = await get_jwks()
        # Extraction de la clé publique depuis JWKS
        headers = jwt.get_unverified_headers(token)
        kid = headers.get("kid")
        key = next(
            (k for k in jwks.get("keys", []) if k.get("kid") == kid),
            None
        )
        if key is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid token signature key"
            )

        payload = jwt.decode(
            token,
            key,
            algorithms=["RS256"],
            options={"verify_aud": False}
        )

        tenant_id = payload.get("tenantId") or payload.get("tenant_id")
        user_id = payload.get("sub")

        return {
            "user_id": user_id,
            "tenant_id": tenant_id,
            "roles": payload.get("realm_access", {}).get("roles", []),
            "email": payload.get("email"),
        }
    except JWTError as e:
        logger.error("JWT validation failed", error=str(e))
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token"
        )
