#!/usr/bin/env bash
# NexusERP — Script de démarrage développement
# Usage: bash scripts/start-dev.sh [infra|all|stop]

set -e

MODE=${1:-all}
COMPOSE="docker-compose"

infra_only() {
  echo ">>> Démarrage infrastructure (Postgres, Kafka, Redis, Keycloak, MinIO)..."
  $COMPOSE up -d postgres kafka redis keycloak minio elasticsearch
  echo ""
  echo ">>> Attente Keycloak..."
  until $COMPOSE exec -T keycloak curl -sf http://localhost:8080/health/ready > /dev/null 2>&1; do
    printf '.'
    sleep 3
  done
  echo ""
  echo "Infrastructure prête :"
  echo "  Keycloak : http://localhost:8180 (admin/admin)"
  echo "  MinIO    : http://localhost:9001 (nexus_minio_access/nexus_minio_secret)"
  echo "  Kafka UI : http://localhost:8090"
}

case "$MODE" in
  infra)
    infra_only
    ;;
  all)
    infra_only
    echo ""
    echo ">>> Construction et démarrage des microservices..."
    $COMPOSE up -d --build nexus-gateway nexus-auth nexus-finance \
      nexus-procurement nexus-inventory nexus-sales \
      nexus-hr nexus-production nexus-notification \
      nexus-import nexus-reporting nexus-ai frontend
    echo ""
    echo ">>> NexusERP démarré !"
    echo ""
    echo "  Application  : http://localhost:4200"
    echo "  API Gateway  : http://localhost:8080"
    echo "  Keycloak     : http://localhost:8180"
    echo "  Grafana      : http://localhost:3001 (admin/nexus_grafana_secret)"
    echo "  Wiki.js      : http://localhost:3000"
    echo ""
    echo "Comptes de test :"
    echo "  admin   / Admin1234!   (TENANT_ADMIN)"
    echo "  finance / Finance1234! (FINANCE_MANAGER)"
    echo "  ops     / Ops12345!    (PRODUCTION/INVENTORY)"
    ;;
  stop)
    echo ">>> Arrêt de NexusERP..."
    $COMPOSE down
    ;;
  *)
    echo "Usage: $0 [infra|all|stop]"
    exit 1
    ;;
esac
