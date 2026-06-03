#!/usr/bin/env bash
#
# Déploiement NexusERP sur le k3s du serveur — exécuté DEPUIS TA MACHINE.
# Aucun secret n'est stocké dans GitHub : on réutilise ta connexion SSH existante.
# Les mots de passe vivent uniquement sur le serveur en Secrets Kubernetes.
#
# Usage :
#   export NEXUS_SSH=user@62.238.11.20      # ta cible SSH
#   ./scripts/deploy.sh staging [tag]
#   ./scripts/deploy.sh prod    sha-1a2b3c4
#
# Prérequis serveur : helm + kubectl + kubeconfig accessible, ingress traefik.
# Prérequis (une seule fois, côté serveur) : secret ghcr-pull + ConfigMap/Secret
# applicatifs — voir docs/deployment.md.

set -euo pipefail

ENVIRONMENT="${1:?usage: deploy.sh <staging|prod> [image_tag]}"
SSH_TARGET="${NEXUS_SSH:?définis NEXUS_SSH=user@62.238.11.20}"
OWNER="${GHCR_OWNER:-couldevlop}"

case "$ENVIRONMENT" in
  staging) NS="nexuserp-staging"; VALUES="values-staging.yaml"; DEFAULT_TAG="develop" ;;
  prod)    NS="nexuserp-prod";    VALUES="values-prod.yaml";    DEFAULT_TAG="main" ;;
  *) echo "Environnement invalide '$ENVIRONMENT' (staging|prod)"; exit 1 ;;
esac

# Par défaut : l'image taguée du nom de branche (develop/main) publiée par la CI.
# Sinon, passe un tag précis : ./scripts/deploy.sh prod sha-1a2b3c4
TAG="${2:-$DEFAULT_TAG}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHART_DIR="$SCRIPT_DIR/../helm/charts/nexuserp"

echo ">> Validation locale du chart…"
helm lint "$CHART_DIR" -f "$CHART_DIR/$VALUES"

echo ">> Copie du chart vers $SSH_TARGET…"
ssh "$SSH_TARGET" 'mkdir -p ~/nexuserp-deploy'
scp -r "$CHART_DIR" "$SSH_TARGET:~/nexuserp-deploy/"

echo ">> Déploiement Helm ($ENVIRONMENT / namespace $NS / tag $TAG)…"
ssh "$SSH_TARGET" NS="$NS" VALUES="$VALUES" OWNER="$OWNER" TAG="$TAG" 'bash -s' <<'REMOTE'
  set -euo pipefail
  export KUBECONFIG="${KUBECONFIG:-$HOME/.kube/config}"
  kubectl create namespace "$NS" --dry-run=client -o yaml | kubectl apply -f -
  helm upgrade --install nexuserp "$HOME/nexuserp-deploy/nexuserp" \
    --namespace "$NS" \
    -f "$HOME/nexuserp-deploy/nexuserp/$VALUES" \
    --set global.image.registry="ghcr.io/$OWNER" \
    --set global.image.tag="$TAG" \
    --wait --timeout 10m --atomic
  kubectl -n "$NS" rollout status deploy/nexus-gateway --timeout=120s || true
REMOTE

echo ">> Déploiement $ENVIRONMENT terminé."
