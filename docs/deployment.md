# Déploiement NexusERP — CI/CD

## Principe (conforme OWASP — aucun secret exposé)

- **GitHub Actions ne fait que construire/publier les images** sur GHCR, avec le
  `GITHUB_TOKEN` éphémère du run. **Aucun secret de déploiement n'est stocké dans GitHub**
  (pas de clé SSH, pas de mot de passe) → OWASP CICD-SEC-6 (insufficient credential hygiene).
- **Le déploiement se fait depuis TA machine** via `scripts/deploy.sh`, en réutilisant ta
  connexion SSH au serveur. Rien n'est stocké côté GitHub.
- **Les mots de passe vivent uniquement sur le serveur**, en *Secrets Kubernetes*
  (jamais dans le repo, jamais dans les logs).

| Branche   | Environnement | Namespace k3s        | Images (auto)            | Déploiement        |
|-----------|---------------|----------------------|--------------------------|--------------------|
| `develop` | staging       | `nexuserp-staging`   | push GHCR via CI         | `deploy.sh staging` |
| `main`    | production    | `nexuserp-prod`      | push GHCR via CI         | `deploy.sh prod`    |

Workflows : `.github/workflows/deploy.yml` (build/push images) — `ci.yml` (build+tests) — `security.yml` (OWASP).

## 1) Images (automatique)

À chaque push sur `develop`/`main`, la CI construit les 14 images et les pousse sur
`ghcr.io/<owner>/nexus-*` (tags `sha-<court>` et nom de branche). Aucune action requise.

> Rendre les packages GHCR **privés** (par défaut) ; le cluster les tire via un secret
> `ghcr-pull` créé **sur le serveur** (étape 2).

## 2) Préparation du serveur (une seule fois, par namespace)

Sur le serveur (via ta session SSH). Les mots de passe restent ici, jamais ailleurs.

```bash
NS=nexuserp-staging   # puis recommencer avec nexuserp-prod
kubectl create namespace "$NS" --dry-run=client -o yaml | kubectl apply -f -

# Secret de pull GHCR (PAT GitHub à scope read:packages, saisi ici uniquement)
kubectl -n "$NS" create secret docker-registry ghcr-pull \
  --docker-server=ghcr.io \
  --docker-username=<ton_user_github> \
  --docker-password=<PAT_read_packages>

# Config non sensible
kubectl -n "$NS" create configmap nexuserp-config \
  --from-literal=DB_HOST=postgres --from-literal=DB_PORT=5432 \
  --from-literal=DB_NAME=nexuserp \
  --from-literal=KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  --from-literal=REDIS_HOST=redis \
  --from-literal=KEYCLOAK_URL=http://keycloak:8080 \
  --from-literal=MINIO_ENDPOINT=http://minio:9000 \
  --from-literal=AI_ENABLED=true

# Secrets (mots de passe — restent sur le serveur)
kubectl -n "$NS" create secret generic nexuserp-secrets \
  --from-literal=DB_PASSWORD='***' \
  --from-literal=REDIS_PASSWORD='***' \
  --from-literal=ANTHROPIC_API_KEY='***' \
  --from-literal=SMTP_APP_PASSWORD='***' \
  --from-literal=ORANGE_WEBHOOK_SECRET='***' \
  --from-literal=WAVE_WEBHOOK_SECRET='***' \
  --from-literal=MTN_WEBHOOK_SECRET='***'
```

Prérequis : `helm` + `kubectl` + kubeconfig accessible par ton user
(k3s : `sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config && sudo chown $USER ~/.kube/config`),
ingress **Traefik** (défaut k3s), et pour le TLS prod **cert-manager** + ClusterIssuer `letsencrypt-prod`.

> L'infra (PostgreSQL, Kafka, Redis, Keycloak, MinIO) n'est **pas** déployée par ce chart
> applicatif : réutilise tes services existants, ou ajoute-les via les charts Bitnami.

## 3) Déployer (depuis ta machine)

```bash
export NEXUS_SSH=<user>@62.238.11.20    # ta cible SSH
export GHCR_OWNER=<owner_minuscule>     # défaut : couldevlop

# staging (dernier build de develop)
./scripts/deploy.sh staging sha-1a2b3c4

# production
./scripts/deploy.sh prod sha-1a2b3c4
```

Le script : valide le chart (`helm lint`), copie le chart sur le serveur (scp), puis lance
`helm upgrade --install … --atomic` dans le bon namespace. Le tag d'image correspond au
`sha-<court>` produit par la CI (visible dans l'onglet Actions / Packages).

## 4) Domaines

Édite `helm/charts/nexuserp/values-staging.yaml` et `values-prod.yaml` (`global.domain`).
Gateway exposé sur `api.<domain>`, frontend sur `<domain>`.

## Hygiène secrets (OWASP)

- ✅ Aucun secret de déploiement dans GitHub (clé SSH = la tienne, locale).
- ✅ Mots de passe uniquement en Secrets k8s sur le serveur.
- ⚠️ Le realm Keycloak `infrastructure/keycloak/realms/nexuserp-realm.json` contient des
  identifiants **de DEV** (`*-dev`, comptes démo) — à **ne pas** réutiliser en prod ;
  régénère realm + secrets clients pour la prod.
- 🔎 Le workflow `security.yml` scanne secrets/vulns (Gitleaks, Trivy, CodeQL).
