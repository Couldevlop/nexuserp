# Changelog

Toutes les modifications notables de NexusERP sont documentées dans ce fichier.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/)
et le projet adhère au [versionnement sémantique](https://semver.org/lang/fr/).

## [1.0.0] - 2026-06-03

Première version de **NexusERP** — plateforme ERP/PGI multi-tenant (SaaS / On-Premise),
conforme PCG (France) et SYSCOHADA (OHADA/UEMOA), avec couche IA optionnelle.

### Ajouté — Backend (Java 21 / Spring Boot 3.3, architecture hexagonale + CQRS)
- Microservices : `nexus-gateway`, `nexus-auth`, `nexus-core`, `nexus-finance`,
  `nexus-procurement`, `nexus-inventory`, `nexus-sales`, `nexus-hr`, `nexus-production`,
  `nexus-notification`, `nexus-import`, `nexus-reporting`.
- **`nexus-payment` (nouveau)** : Mobile Money natif (Orange Money, Wave, MTN MoMo, Moov)
  derrière un port `PaymentProviderGateway` (providers simulés, vrais providers enfichables),
  webhooks vérifiés par HMAC-SHA256 à temps constant, clés d'idempotence, multi-devise XOF.
- Finance : facturation, comptabilité, export FEC (DGFiP) et états SYSCOHADA, moteur de
  conformité auto (FR/CI/SN/ML/BF) ; **consommation de `nexus.payment.succeeded`** pour
  régler les factures (idempotent via table `processed_payments`).
- Multi-tenant (isolation par schéma PostgreSQL + RLS), événementiel Kafka, sécurité
  OAuth2/Keycloak + RBAC + 2FA TOTP.

### Ajouté — Notifications
- Canaux **SMS et WhatsApp** (en plus de l'email SMTP) : ports dédiés, bascule
  simulé↔réel selon présence des credentials, templates courts FR/EN.

### Ajouté — Frontend (Angular 18, standalone + signals + OnPush)
- Design system (tokens SCSS, composants partagés), shell, auth Keycloak, dashboard.
- Modules complets : **Finance, Ventes, Stocks, Achats, RH, Production, Reporting**
  (listes/création/détail, tableaux de bord KPI via `nx-stat-card`).
- **Offline-first** : file d'attente IndexedDB (outbox), intercepteur, synchronisation,
  résolution de conflits (409), indicateur de connectivité.
- **Command palette** (Ctrl/Cmd+K), animations de route, motion utilitaire,
  i18n (fr-FR, fr-CI, en-US), PWA.

### Ajouté — Service IA (Python 3.12 / FastAPI)
- Endpoints : assistant conversationnel (Claude + prompt caching), détection d'anomalies,
  prévision de demande, extraction de documents, vérification de conformité.
- Dégradation gracieuse quand `AI_ENABLED=false`.

### Ajouté — CI/CD & Infrastructure
- **CI** (`ci.yml`) : build Maven, build Angular, tests (Karma headless), pytest.
- **Images** (`deploy.yml`) : build + push des 14 images sur GHCR via `GITHUB_TOKEN`
  éphémère — aucun secret de déploiement stocké dans GitHub.
- **Déploiement** : chart Helm générique `helm/charts/nexuserp` (Deployment/Service/Ingress)
  + `scripts/deploy.sh` (déploiement manuel par SSH depuis la machine, staging/prod sur k3s).
- Outillage dépôt : `CODEOWNERS`, modèle de PR, `docs/deployment.md`.

### Sécurité (OWASP)
- Pipeline `security.yml` : SAST **CodeQL** (Java/TS/Python), **Trivy** (vulnérabilités + IaC
  + secrets), **Gitleaks**, **OWASP Dependency-Check** (hebdomadaire).
- `GITHUB_TOKEN` en moindre privilège ; mots de passe uniquement en Secrets Kubernetes
  côté serveur ; webhooks paiement signés (HMAC) ; validation des entrées (Jakarta/Angular) ;
  pas de donnée personnelle (RGPD) en logs.

### Notes & limitations connues
- Providers Mobile Money et SMS/WhatsApp en **mode simulé** par défaut (vrais providers
  à câbler avec credentials).
- Identifiants Keycloak du realm fourni = **DEV uniquement** (`*-dev`, comptes démo) —
  à régénérer avant tout usage en production.
- Suite de tests présente mais non encore bloquante en CI (en cours de stabilisation).

[1.0.0]: https://github.com/Couldevlop/nexuserp/releases/tag/v1.0.0
