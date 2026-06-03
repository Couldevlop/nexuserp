#!/usr/bin/env bash
#
# Crée et pousse un tag de release SemVer depuis `main`.
# Le push du tag déclenche le build d'images taguées vX.Y.Z sur GHCR (deploy.yml).
#
# Prérequis : la PR develop -> main est mergée (main contient la release).
# Usage : ./scripts/release.sh v1.0.0
#
set -euo pipefail

VERSION="${1:?usage: release.sh vX.Y.Z}"
[[ "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "Version invalide '$VERSION' (attendu vX.Y.Z)"; exit 1; }

echo ">> Mise à jour de main…"
git fetch origin --quiet
git checkout main
git pull --ff-only origin main

# Le CHANGELOG doit mentionner la version (sans le préfixe 'v')
if ! grep -q "\[${VERSION#v}\]" CHANGELOG.md; then
  echo "⚠️  $VERSION absent de CHANGELOG.md — ajoute l'entrée avant de taguer."; exit 1
fi

# Refuse un tag déjà existant
if git rev-parse "$VERSION" >/dev/null 2>&1; then
  echo "Le tag $VERSION existe déjà."; exit 1
fi

echo ">> Création du tag annoté $VERSION…"
git tag -a "$VERSION" -m "NexusERP $VERSION"
git push origin "$VERSION"

echo "✅ Tag $VERSION poussé."
echo "   → GitHub Actions construit les images taguées $VERSION sur GHCR."
echo "   → Crée ensuite la Release GitHub depuis ce tag (texte = section CHANGELOG)."
echo "   → Déploiement prod figé : ./scripts/deploy.sh prod $VERSION"
