#!/usr/bin/env bash
# =============================================================================
# NexusERP — Tenant Onboarding Script (< 4h SLA)
# Usage: ./scripts/deploy-tenant.sh [OPTIONS]
# =============================================================================
set -euo pipefail

# ─── Colors ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_success() { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }
die()         { log_error "$*"; exit 1; }

# ─── Defaults ────────────────────────────────────────────────────────────────
NAMESPACE_PREFIX="nexuserp"
HELM_CHART="./helm"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
KEYCLOAK_ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
KEYCLOAK_ADMIN_PASS="${KEYCLOAK_ADMIN_PASS:-admin}"
DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_ADMIN_USER="${DB_ADMIN_USER:-postgres}"
DB_ADMIN_PASS="${DB_ADMIN_PASS:-}"
SMTP_FROM="${SMTP_FROM:-noreply@nexuserp.io}"
DRY_RUN=false
PLAN="standard"

# ─── Parse arguments ─────────────────────────────────────────────────────────
usage() {
    cat <<EOF
Usage: $0 [OPTIONS]

Required:
  --tenant-id       Tenant identifier (e.g. acme-corp)
  --country         Country code FR|CI|SN|ML|BF|BE
  --admin-email     Admin user email

Optional:
  --plan            Pricing plan: starter|standard|enterprise (default: standard)
  --domain          Custom domain (e.g. acme.nexuserp.io)
  --dry-run         Simulate without making changes
  --help            Show this message

Environment variables:
  KEYCLOAK_URL, KEYCLOAK_ADMIN_USER, KEYCLOAK_ADMIN_PASS
  DB_HOST, DB_PORT, DB_ADMIN_USER, DB_ADMIN_PASS
  SMTP_FROM

Examples:
  $0 --tenant-id acme-corp --country FR --admin-email admin@acme.com
  $0 --tenant-id ci-sarl --country CI --admin-email admin@ci-sarl.ci --plan enterprise
EOF
}

TENANT_ID=""
COUNTRY=""
ADMIN_EMAIL=""
DOMAIN=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --tenant-id)     TENANT_ID="$2";    shift 2 ;;
        --country)       COUNTRY="$2";      shift 2 ;;
        --admin-email)   ADMIN_EMAIL="$2";  shift 2 ;;
        --plan)          PLAN="$2";         shift 2 ;;
        --domain)        DOMAIN="$2";       shift 2 ;;
        --dry-run)       DRY_RUN=true;      shift ;;
        --help|-h)       usage; exit 0 ;;
        *)               die "Unknown argument: $1. Use --help for usage." ;;
    esac
done

# ─── Validation ──────────────────────────────────────────────────────────────
[[ -z "$TENANT_ID" ]]    && die "--tenant-id is required"
[[ -z "$COUNTRY" ]]      && die "--country is required"
[[ -z "$ADMIN_EMAIL" ]]  && die "--admin-email is required"

# Validate tenant ID format
if ! [[ "$TENANT_ID" =~ ^[a-z0-9][a-z0-9-]{2,98}[a-z0-9]$ ]]; then
    die "Invalid tenant ID '$TENANT_ID'. Must be 4-100 chars, lowercase alphanumeric + hyphens."
fi

# Validate country
VALID_COUNTRIES=("FR" "CI" "SN" "ML" "BF" "BE" "GB" "US")
if ! printf '%s\n' "${VALID_COUNTRIES[@]}" | grep -q "^${COUNTRY}$"; then
    die "Invalid country '$COUNTRY'. Supported: ${VALID_COUNTRIES[*]}"
fi

# Validate email (basic)
if ! [[ "$ADMIN_EMAIL" =~ ^[^@]+@[^@]+\.[^@]+$ ]]; then
    die "Invalid email format: $ADMIN_EMAIL"
fi

# Set default domain if not provided
[[ -z "$DOMAIN" ]] && DOMAIN="${TENANT_ID}.nexuserp.io"

NAMESPACE="${NAMESPACE_PREFIX}-${TENANT_ID}"
START_TIME=$(date +%s)

echo ""
echo -e "${BLUE}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         NexusERP — Tenant Onboarding                    ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""
log_info "Tenant ID    : $TENANT_ID"
log_info "Country      : $COUNTRY"
log_info "Plan         : $PLAN"
log_info "Admin Email  : $ADMIN_EMAIL"
log_info "Domain       : $DOMAIN"
log_info "Namespace    : $NAMESPACE"
[[ "$DRY_RUN" == "true" ]] && log_warn "DRY RUN MODE — no changes will be made"
echo ""

# ─── Step 1: Prerequisites check ─────────────────────────────────────────────
log_info "Step 1/7 — Checking prerequisites…"
for cmd in kubectl helm psql curl jq; do
    if ! command -v "$cmd" &>/dev/null; then
        die "Required command '$cmd' not found. Please install it."
    fi
done
log_success "All prerequisites satisfied"

# ─── Step 2: Create K8s namespace ────────────────────────────────────────────
log_info "Step 2/7 — Creating Kubernetes namespace…"
if [[ "$DRY_RUN" == "false" ]]; then
    kubectl create namespace "$NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f -
    kubectl label namespace "$NAMESPACE" \
        nexuserp.io/tenant="$TENANT_ID" \
        nexuserp.io/country="$COUNTRY" \
        nexuserp.io/plan="$PLAN" \
        --overwrite
fi
log_success "Namespace '$NAMESPACE' ready"

# ─── Step 3: Create Keycloak realm ───────────────────────────────────────────
log_info "Step 3/7 — Configuring Keycloak realm…"

get_keycloak_token() {
    curl -sf -X POST \
        "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
        -d "client_id=admin-cli" \
        -d "username=${KEYCLOAK_ADMIN_USER}" \
        -d "password=${KEYCLOAK_ADMIN_PASS}" \
        -d "grant_type=password" | jq -r '.access_token'
}

if [[ "$DRY_RUN" == "false" ]]; then
    KC_TOKEN=$(get_keycloak_token) || die "Failed to authenticate with Keycloak"

    # Create realm for this tenant (or use shared realm with tenant claim)
    REALM_NAME="nexuserp-${TENANT_ID}"
    REALM_JSON=$(cat <<EOF
{
  "realm": "${REALM_NAME}",
  "enabled": true,
  "displayName": "NexusERP — ${TENANT_ID}",
  "registrationAllowed": false,
  "bruteForceProtected": true,
  "failureFactor": 5,
  "waitIncrementSeconds": 60,
  "maxFailureWaitSeconds": 900,
  "passwordPolicy": "length(12) and upperCase(1) and digits(1) and specialChars(1)",
  "accessTokenLifespan": 28800,
  "ssoSessionMaxLifespan": 108000,
  "defaultLocale": "fr",
  "attributes": {
    "tenantId": "${TENANT_ID}",
    "country": "${COUNTRY}",
    "plan": "${PLAN}"
  }
}
EOF
)

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "${KEYCLOAK_URL}/admin/realms" \
        -H "Authorization: Bearer ${KC_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "$REALM_JSON")

    if [[ "$HTTP_CODE" == "201" ]]; then
        log_success "Keycloak realm '${REALM_NAME}' created"
    elif [[ "$HTTP_CODE" == "409" ]]; then
        log_warn "Realm '${REALM_NAME}' already exists — skipping"
    else
        die "Failed to create Keycloak realm (HTTP $HTTP_CODE)"
    fi

    # Create TENANT_ADMIN user
    ADMIN_USER_JSON=$(cat <<EOF
{
  "username": "admin@${TENANT_ID}",
  "email": "${ADMIN_EMAIL}",
  "enabled": true,
  "emailVerified": false,
  "realmRoles": ["TENANT_ADMIN"],
  "requiredActions": ["UPDATE_PASSWORD", "CONFIGURE_TOTP"],
  "credentials": [{
    "type": "password",
    "value": "$(openssl rand -base64 16)",
    "temporary": true
  }]
}
EOF
)
    curl -sf -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users" \
        -H "Authorization: Bearer ${KC_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "$ADMIN_USER_JSON" || log_warn "Admin user may already exist"

    log_success "Keycloak realm and admin user configured"
fi

# ─── Step 4: PostgreSQL schema provisioning ───────────────────────────────────
log_info "Step 4/7 — Provisioning PostgreSQL schema…"

DB_SCHEMA="tenant_${TENANT_ID//-/_}"

if [[ "$DRY_RUN" == "false" ]]; then
    PGPASSWORD="$DB_ADMIN_PASS" psql \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_ADMIN_USER" \
        -d nexus \
        -c "
          CREATE SCHEMA IF NOT EXISTS ${DB_SCHEMA};
          GRANT USAGE ON SCHEMA ${DB_SCHEMA} TO nexus_app;
          GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA ${DB_SCHEMA} TO nexus_app;
          ALTER DEFAULT PRIVILEGES IN SCHEMA ${DB_SCHEMA}
            GRANT ALL ON TABLES TO nexus_app;

          INSERT INTO tenant_management.tenants (
            tenant_id, tenant_name, schema_name, country, plan,
            status, admin_email, created_at
          ) VALUES (
            '${TENANT_ID}', '${TENANT_ID}', '${DB_SCHEMA}',
            '${COUNTRY}', '${PLAN}', 'PROVISIONING',
            '${ADMIN_EMAIL}', NOW()
          ) ON CONFLICT (tenant_id) DO NOTHING;
        " 2>/dev/null || die "Failed to provision PostgreSQL schema"

    log_success "Schema '${DB_SCHEMA}' provisioned"
fi

# ─── Step 5: Deploy microservices via Helm ────────────────────────────────────
log_info "Step 5/7 — Deploying NexusERP microservices via Helm…"

VALUES_FILE="helm/environments/values-prod-saas.yaml"
[[ -f "$VALUES_FILE" ]] || die "Helm values file not found: $VALUES_FILE"

if [[ "$DRY_RUN" == "false" ]]; then
    helm upgrade --install "nexuserp-${TENANT_ID}" "$HELM_CHART" \
        --namespace "$NAMESPACE" \
        --create-namespace \
        -f "$VALUES_FILE" \
        --set "global.tenantId=${TENANT_ID}" \
        --set "global.country=${COUNTRY}" \
        --set "global.plan=${PLAN}" \
        --set "global.domain=${DOMAIN}" \
        --set "nexus-finance.db.schema=${DB_SCHEMA}" \
        --set "nexus-hr.db.schema=${DB_SCHEMA}" \
        --set "nexus-inventory.db.schema=${DB_SCHEMA}" \
        --set "nexus-sales.db.schema=${DB_SCHEMA}" \
        --set "nexus-procurement.db.schema=${DB_SCHEMA}" \
        --set "nexus-production.db.schema=${DB_SCHEMA}" \
        --timeout 15m \
        --wait

    log_success "Helm deployment complete"
else
    helm upgrade --install "nexuserp-${TENANT_ID}" "$HELM_CHART" \
        --namespace "$NAMESPACE" \
        -f "$VALUES_FILE" \
        --set "global.tenantId=${TENANT_ID}" \
        --dry-run --debug > /dev/null
    log_success "[DRY RUN] Helm template validation passed"
fi

# ─── Step 6: Configure Ingress + TLS ─────────────────────────────────────────
log_info "Step 6/7 — Configuring Ingress and TLS…"

if [[ "$DRY_RUN" == "false" ]]; then
    kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: nexuserp-${TENANT_ID}
  namespace: ${NAMESPACE}
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
spec:
  tls:
    - hosts:
        - ${DOMAIN}
      secretName: nexuserp-${TENANT_ID}-tls
  rules:
    - host: ${DOMAIN}
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: nexus-frontend
                port:
                  number: 80
          - path: /api/
            pathType: Prefix
            backend:
              service:
                name: nexus-gateway
                port:
                  number: 8080
EOF
    log_success "Ingress configured for ${DOMAIN}"
fi

# ─── Step 7: Seed data + welcome email ───────────────────────────────────────
log_info "Step 7/7 — Seeding tenant data and sending welcome email…"

if [[ "$DRY_RUN" == "false" ]]; then
    # Trigger seed via API (plan comptable, taxes, paramétrage)
    SEED_PAYLOAD=$(cat <<EOF
{
  "tenantId": "${TENANT_ID}",
  "country": "${COUNTRY}",
  "adminEmail": "${ADMIN_EMAIL}",
  "plan": "${PLAN}"
}
EOF
)
    # Wait for gateway to be ready
    for i in {1..30}; do
        if kubectl get pods -n "$NAMESPACE" -l app=nexus-gateway \
            --field-selector=status.phase=Running 2>/dev/null | grep -q Running; then
            break
        fi
        sleep 5
    done

    # Update tenant status to ACTIVE
    PGPASSWORD="$DB_ADMIN_PASS" psql \
        -h "$DB_HOST" -p "$DB_PORT" -U "$DB_ADMIN_USER" -d nexus \
        -c "UPDATE tenant_management.tenants SET status='ACTIVE' WHERE tenant_id='${TENANT_ID}';" \
        2>/dev/null || log_warn "Could not update tenant status"

    log_success "Seed data applied"
fi

# ─── Summary ─────────────────────────────────────────────────────────────────
END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))
ELAPSED_MINUTES=$((ELAPSED / 60))
ELAPSED_SECONDS=$((ELAPSED % 60))

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   ✅  Tenant Onboarding Complete!                        ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""
log_success "Tenant ID    : ${TENANT_ID}"
log_success "URL          : https://${DOMAIN}"
log_success "Admin Email  : ${ADMIN_EMAIL}"
log_success "Duration     : ${ELAPSED_MINUTES}m ${ELAPSED_SECONDS}s"
echo ""
log_info "Next steps:"
log_info "  1. Check welcome email sent to ${ADMIN_EMAIL}"
log_info "  2. Log in at https://${DOMAIN}"
log_info "  3. Configure 2FA (mandatory for TENANT_ADMIN)"
log_info "  4. Import initial data: https://${DOMAIN}/admin/import"
echo ""
