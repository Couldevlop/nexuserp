# CLAUDE.md — NexusERP : Plateforme ERP/PGI Entreprise de Nouvelle Génération

> **Instruction principale pour Claude Code** : Ce fichier est la source de vérité absolue. Chaque décision d'implémentation doit s'y conformer. Ne jamais dévier sans justification explicite dans un commentaire de code.

---

## 0. VISION PRODUIT & POSITIONNEMENT STRATÉGIQUE

### 0.1 Nom du produit

**NexusERP** — _Intelligence at the Core of Every Decision_

### 0.2 Ambition

NexusERP est un ERP/PGI de nouvelle génération, multi-tenant, 100% paramétrable, déployable en SaaS ou On-Premise, conçu pour être **la meilleure solution ERP au monde** dans sa catégorie. Il intègre l'IA de bout en bout, respecte les législations françaises, européennes (RGPD, DSP2), ivoiriennes (OHADA, SYSCOHADA révisé 2018), UEMOA et OMS. Il est utilisable avec ou sans IA — l'IA est une couche additionnelle non bloquante.

### 0.3 Analyse concurrentielle — Top 5 mondial + stratégie de dépassement

| ERP                        | Forces récupérées                                             | Échecs capitalisés                                                                             | Innovation NexusERP                                             |
| -------------------------- | ------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| **SAP S/4HANA**            | Profondeur fonctionnelle, intégration financière, traçabilité | UX catastrophique, coût prohibitif, 18-36 mois de déploiement, monolithique, formation 6+ mois | Déploiement < 4h, UX premium zero-training, pricing transparent |
| **Oracle NetSuite**        | Multi-tenant natif, cloud-first, reporting avancé             | Rigidité de personnalisation, support défaillant, vendor lock-in total                         | Architecture ouverte, API-first, plugins marketplace            |
| **Microsoft Dynamics 365** | Intégration Office 365, Power Platform, adoption large        | Coût licences explosif, fragmentation modules, complexité d'intégration                        | Open standards, SMTP natif, intégration universelle             |
| **Odoo**                   | Open-source, modularité, communauté active                    | Qualité variable des modules tiers, performance dégradée > 500 users, dette technique          | Core performant, modules certifiés IA-ready, SLA garanti        |
| **Sage X3**                | Conformité légale France/OHADA, PME-friendly                  | Interface vieillissante, IA absente, mobilité limitée                                          | UI/UX premium + IA prédictive + mobile-first natif              |

**Innovation inattendue NexusERP** :

- **IA Contextuelle Universelle** : Chaque écran, chaque flux, chaque décision dispose d'un assistant IA conversationnel ancré dans le contexte métier réel-time
- **Compliance Automatique Multi-Juridictions** : Le moteur légal bascule automatiquement selon le pays (France/CI/UEMOA) sans reconfiguration
- **Zero-Downtime Tenant Onboarding** : Un nouveau tenant est opérationnel en < 4 heures avec migration de données
- **Adaptive UI** : L'interface se reconfigure selon le rôle, le pays, la culture et les habitudes d'usage (ML comportemental)
- **Audit Trail IA-Powered** : Détection d'anomalies comptables et d'accès suspects en temps réel

---

## 1. STACK TECHNOLOGIQUE

### 1.1 Backend — Java 21 + Spring Boot 3.x

```
backend/
├── services/
│   ├── nexus-gateway/          # Spring Cloud Gateway (API Gateway)
│   ├── nexus-auth/             # Keycloak adapter + 2FA orchestration
│   ├── nexus-core/             # Domaine métier principal (Hexagonal)
│   ├── nexus-finance/          # Comptabilité, TVA, OHADA, Plan Comptable
│   ├── nexus-procurement/      # Achats, fournisseurs, marchés
│   ├── nexus-inventory/        # Stocks, entrepôts, logistique
│   ├── nexus-sales/            # Ventes, CRM, facturation
│   ├── nexus-hr/               # RH, paie, gestion temps
│   ├── nexus-production/       # Production, MPS, ordonnancement
│   ├── nexus-ai/               # Python FastAPI — moteur IA/ML
│   ├── nexus-reporting/        # Jasper Reports + BI engine
│   ├── nexus-notification/     # SMTP Google, SMS, Push FCM
│   └── nexus-import/           # Import/Export XLSX, migration données
```

**Versions obligatoires** :

- Java 21 (Virtual Threads activés — Project Loom)
- Spring Boot 3.3.x
- Spring Security 6.x
- Spring Cloud 2023.x
- Hibernate 6.x + Jakarta EE 10
- Gradle 8.x (multi-module)

### 1.2 Frontend — Angular 18+

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/               # Guards, interceptors, services singleton
│   │   ├── shared/             # Composants réutilisables, pipes, directives
│   │   ├── layout/             # Shell, sidebar, header, footer
│   │   ├── features/
│   │   │   ├── auth/           # Login, 2FA, reset password
│   │   │   ├── dashboard/      # Dashboard adaptatif par rôle
│   │   │   ├── finance/        # Module financier
│   │   │   ├── procurement/    # Module achats
│   │   │   ├── inventory/      # Module stocks
│   │   │   ├── sales/          # Module ventes
│   │   │   ├── hr/             # Module RH
│   │   │   ├── production/     # Module production
│   │   │   ├── reporting/      # Rapports et BI
│   │   │   ├── ai-assistant/   # Assistant IA intégré
│   │   │   ├── admin/          # Administration tenant
│   │   │   └── settings/       # Paramétrage global
│   │   └── i18n/               # fr-FR, fr-CI, en-US, en-GB
│   ├── assets/
│   ├── environments/
│   └── styles/                 # SCSS design system
```

**RÈGLE ABSOLUE Angular** : Zéro HTML inline dans les composants TypeScript. Chaque composant a obligatoirement :

- `*.component.ts` — logique pure
- `*.component.html` — template
- `*.component.scss` — styles scoped
- `*.component.spec.ts` — tests unitaires

### 1.3 IA/ML — Python 3.12 + FastAPI

```
ai-service/
├── app/
│   ├── api/v1/
│   │   ├── anomaly_detection.py    # Détection anomalies comptables
│   │   ├── demand_forecasting.py   # Prévision stocks/ventes
│   │   ├── invoice_extraction.py   # OCR + extraction factures (LLM)
│   │   ├── chat_assistant.py       # Assistant IA conversationnel
│   │   ├── compliance_checker.py   # Vérification conformité légale
│   │   └── predictive_analytics.py # KPI prédictifs
│   ├── models/                     # Modèles ML entraînés
│   ├── schemas/                    # Pydantic schemas
│   └── core/                       # Config, security, db
```

### 1.4 Infrastructure

```yaml
Infrastructure:
  Orchestration: Kubernetes 1.29+ (K8s)
  Package Manager: Helm 3.x (charts versionés)
  Container Runtime: Docker + containerd
  Service Mesh: Istio (mTLS inter-services)
  Message Broker: Apache Kafka 3.x (Kraft mode, sans ZooKeeper)
  Cache: Redis 7.x (Cluster mode)
  Database: PostgreSQL 16 (primaire) + TimescaleDB (métriques)
  Search: Elasticsearch 8.x
  Object Storage: MinIO (compatible S3)
  Identity: Keycloak 24.x (OIDC/OAuth2)
  Monitoring: Prometheus + Grafana + Loki + Tempo (stack LGTM)
  Tracing: OpenTelemetry + Jaeger
  CI/CD: GitLab CI / GitHub Actions
  Secret Management: HashiCorp Vault
  Wiki: Wiki.js 3.x (containerisé)
  Ingress: Nginx Ingress Controller + cert-manager (Let's Encrypt)
  Backup: Velero (K8s backup)
```

---

## 2. ARCHITECTURE

### 2.1 Philosophie architecturale — Hybrid Clean/Hexagonal + CQRS + Event-Driven

NexusERP adopte une **architecture hybride** intentionnelle :

```
┌─────────────────────────────────────────────────────────────────┐
│                    COUCHE PRÉSENTATION                          │
│         Angular 18 (SPA) + PWA + Mobile Responsive             │
└─────────────────┬───────────────────────────────────────────────┘
                  │ HTTPS/WSS
┌─────────────────▼───────────────────────────────────────────────┐
│                    API GATEWAY (Spring Cloud Gateway)           │
│         Rate Limiting, JWT Validation, Load Balancing           │
└──────┬────────────────────────────────────────┬─────────────────┘
       │ REST/gRPC                               │ WebSocket
┌──────▼──────────────────────────────────────┐ │
│  MICROSERVICES — Architecture Hexagonale    │ │
│                                             │ │
│  ┌─────────────────────────────────────┐   │ │
│  │    DOMAIN (Pure Business Logic)     │   │ │
│  │  Entities, Aggregates, Value Objects│   │ │
│  │  Domain Services, Domain Events     │   │ │
│  └──────────┬──────────────────────────┘   │ │
│             │                              │ │
│  ┌──────────▼──────────┐  ┌─────────────┐ │ │
│  │   APPLICATION PORTS │  │   ADAPTERS  │ │ │
│  │   (Use Cases/CQRS)  │  │  (Driven)   │ │ │
│  │   Commands/Queries  │  │  REST, Kafka│ │ │
│  └─────────────────────┘  └─────────────┘ │ │
└─────────────────────────────────────────────┘ │
                                                 │
┌────────────────────────────────────────────────▼─┐
│            KAFKA EVENT BUS                        │
│  nexus.finance.*, nexus.sales.*, nexus.hr.*,...   │
└──────────────────────────────────────────────────┘
                  │
┌─────────────────▼────────────────────────────────┐
│         COUCHE PERSISTANCE                        │
│  PostgreSQL 16 + Redis 7 + Elasticsearch 8        │
│  + MinIO (fichiers) + TimescaleDB (métriques)     │
└──────────────────────────────────────────────────┘
```

### 2.2 Pattern CQRS obligatoire par service

```java
// Chaque service expose :
// Commands → modifient l'état (via Kafka ou direct)
// Queries  → lisent l'état (optimisées, cacheables)

// Exemple structure package (nexus-finance)
com.nexuserp.finance
├── domain/
│   ├── model/           # AccountEntry, Invoice, CostCenter...
│   ├── port/
│   │   ├── in/          # CreateInvoiceUseCase, ApprovePaymentUseCase
│   │   └── out/         # InvoiceRepository, NotificationPort
│   └── service/         # InvoiceService (implémente ports IN)
├── application/
│   ├── command/         # CreateInvoiceCommand, handlers
│   └── query/           # GetInvoiceQuery, handlers
├── adapter/
│   ├── in/
│   │   ├── rest/        # InvoiceController (@RestController)
│   │   ├── kafka/       # InvoiceEventConsumer
│   │   └── grpc/        # InvoiceGrpcService
│   └── out/
│       ├── persistence/ # InvoiceJpaRepository (JPA)
│       ├── kafka/       # InvoiceEventPublisher
│       └── external/    # ExternalTaxServiceAdapter
└── infrastructure/
    ├── config/          # Spring @Configuration
    ├── security/        # Security config
    └── migration/       # Flyway scripts
```

### 2.3 Multi-Tenancy

**Stratégie** : Schema-based isolation per tenant (PostgreSQL schemas) + Row-Level Security (RLS) comme filet de sécurité.

```java
// TenantContext — ThreadLocal + Kafka header propagation
@Component
public class TenantContext {
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();
    // set/get/clear avec @Around advice sur tous les controllers
}

// Flyway : chaque nouveau tenant → migration automatique du schema
// Keycloak : un realm par tenant OU realm partagé avec tenant claim
```

**Onboarding Tenant < 4h** :

1. Création realm Keycloak (automatisée via API)
2. Provisioning schema PostgreSQL + migrations Flyway
3. Helm values override pour le tenant
4. Seed data (plan comptable selon pays, taxes, etc.)
5. Import données via templates XLSX

---

## 3. SÉCURITÉ — ARCHITECTURE ZERO-TRUST

### 3.1 Keycloak ODC (On-Device Credential) + OAuth2 + OIDC

```yaml
# Keycloak 24.x configuration
Flows:
  - Authorization Code + PKCE (SPA Angular)
  - Client Credentials (service-to-service)
  - Device Authorization (mobile)

Realm Config:
  - Brute Force Protection: activé
  - Password Policy: min 12 chars, majuscule, chiffre, spécial
  - Session: 8h access token, 30j refresh token (sliding)
  - IP-based anomaly detection: activé

Roles (RBAC granulaire):
  - SUPER_ADMIN # Administration plateforme
  - TENANT_ADMIN # Administration tenant
  - FINANCE_MANAGER # Validation écritures comptables
  - FINANCE_USER # Saisie
  - PROCUREMENT_MANAGER
  - SALES_MANAGER
  - HR_MANAGER
  - PRODUCTION_MANAGER
  - AUDITOR # Read-only + export
  - AI_ANALYST # Accès dashboards IA
  - VIEWER # Consultation uniquement
```

### 3.2 Authentification 2FA

```java
// Méthodes 2FA supportées (par ordre de sécurité) :
// 1. TOTP (Google Authenticator, Authy) — recommandé
// 2. WebAuthn/FIDO2 (clé physique YubiKey)
// 3. SMS OTP (via Twilio/OVH SMS)
// 4. Email OTP (via SMTP Google)

// Keycloak gère nativement TOTP et WebAuthn
// SMS/Email OTP : service nexus-auth custom

@RestController
@RequestMapping("/api/v1/auth/2fa")
public class TwoFactorController {
    // POST /setup — Génère QR code TOTP
    // POST /verify — Vérifie code TOTP
    // POST /recovery — Codes de récupération (10 codes one-time)
    // POST /disable — Désactivation avec confirmation admin
}
```

### 3.3 Sécurité applicative

```java
// Headers sécurité obligatoires (Spring Security)
.headers(headers -> headers
    .contentSecurityPolicy("default-src 'self'; script-src 'self'")
    .referrerPolicy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
    .permissionsPolicy("camera=(), microphone=(), geolocation=()")
    .frameOptions(DENY)
    .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000))
)

// Audit Log — TOUTES les actions sensibles
@AuditLog(action = AuditAction.FINANCIAL_ENTRY_CREATED)
public Invoice createInvoice(CreateInvoiceCommand cmd) { ... }

// Rate Limiting (Spring Cloud Gateway)
// - 100 req/s par tenant
// - 1000 req/s global
// - 5 tentatives login → lockout 15 min

// Chiffrement données sensibles (AES-256-GCM)
@Convert(converter = EncryptedStringConverter.class)
private String ibanNumber;
```

### 3.4 Conformité RGPD

```java
// Annotations de données personnelles
@PersonalData(category = DataCategory.IDENTITY, retention = "5_YEARS")
private String fullName;

// Droit à l'oubli — endpoint API
// POST /api/v1/gdpr/erasure/{userId}
// → Anonymisation des données (pas suppression physique si contrainte légale)

// Consentement explicite tracé en base
// Export données personnelles (Art. 20 RGPD) en JSON/CSV
// DPO Dashboard intégré
```

---

## 4. CONFORMITÉ LÉGALE MULTI-JURIDICTIONNELLE

### 4.1 Moteur de compliance automatique

```java
@Component
public class ComplianceEngine {
    // Détecte automatiquement la juridiction du tenant
    // Applique les règles comptables correspondantes

    public AccountingRules getAccountingRules(TenantContext ctx) {
        return switch (ctx.getCountry()) {
            case "FR" -> new PCGFranceRules();          // Plan Comptable Général
            case "CI", "SN", "ML", "BF" -> new SYSCOHADARules(); // SYSCOHADA révisé 2018
            case "BE" -> new PCBBelgiqueRules();
            default -> new IFRSRules();
        };
    }
}
```

### 4.2 France — Obligations légales

```
✅ Plan Comptable Général (PCG) — Comptes 1xx à 8xx
✅ TVA sur débits / encaissements
✅ Déclaration TVA CA3 / CA12
✅ Liasse fiscale (2050, 2051, 2052, 2053)
✅ FEC (Fichier des Écritures Comptables) — export conforme DGFiP
✅ Facturation électronique obligatoire (Chorus Pro / PPF)
✅ DSN (Déclaration Sociale Nominative) — RH/Paie
✅ DADS / DADS-U
✅ Archivage légal 10 ans (écritures comptables)
✅ Signature électronique factures (valeur probante)
✅ RGPD (Règlement 2016/679)
✅ Loi Informatique et Libertés modifiée
✅ Anti-fraude TVA (logiciel certifié NF203 / 525)
✅ Code du Travail — congés, RTT, heures supplémentaires
✅ Convention collective (paramétrable)
```

### 4.3 Côte d'Ivoire & UEMOA — Obligations légales

```
✅ SYSCOHADA révisé 2018 (Acte uniforme OHADA)
✅ Plan Comptable SYSCOHADA — Comptes 1 à 9
✅ Déclaration DGI CI (Direction Générale des Impôts)
✅ TVA — taux standard 18%, taux réduit CI
✅ Taxe sur les salaires (CI)
✅ CNPS — Caisse Nationale de Prévoyance Sociale
✅ Code du Travail ivoirien
✅ Devise : XOF (Franc CFA BCEAO) + EUR + USD
✅ Règlement BCEAO N°15/2002 (instruments de paiement)
✅ Instruction BCEAO 2019 (paiements électroniques)
✅ UEMOA — Directive 06/2009 (harmonisation fiscale)
✅ OHADA — Acte uniforme comptable révisé
✅ Archivage légal 10 ans
```

### 4.4 OMS — Santé & Hygiène (si module santé activé)

```
✅ ICD-11 (Classification internationale des maladies)
✅ Traçabilité médicaments (GS1, numéros de lot)
✅ Cold chain management (stocks réfrigérés)
✅ Pharmacovigilance (déclaration effets indésirables)
✅ HL7 FHIR R4 (échanges données de santé)
✅ ANSM compliance (si module pharma)
```

---

## 5. MODULES FONCTIONNELS DÉTAILLÉS

### 5.1 Module Finance & Comptabilité (nexus-finance)

```java
// Entités domaine principales
public class JournalEntry { /* Écriture comptable */ }
public class Account { /* Compte du plan comptable */ }
public class CostCenter { /* Centre analytique */ }
public class Budget { /* Budget prévisionnel */ }
public class FiscalYear { /* Exercice comptable */ }
public class TaxDeclaration { /* Déclaration TVA */ }
public class FixedAsset { /* Immobilisation */ }

// Use Cases (ports IN)
interface CreateJournalEntryUseCase
interface CloseFiscalYearUseCase
interface GenerateBalanceSheetUseCase
interface GenerateFECExportUseCase     // Spécifique France
interface GenerateSYSCOHADAReportUseCase // Spécifique CI/UEMOA
interface DetectAccountingAnomalyUseCase // IA-powered
interface GenerateTaxDeclarationUseCase

// Valorisations stocks supportées
enum StockValuationMethod {
    STANDARD, FIFO, LIFO, PMP_PERIOD, PMP_REALTIME, SPECIFIC_IDENTIFICATION
}

// États financiers générés automatiquement
- Bilan (Balance Sheet)
- Compte de résultat (P&L)
- Tableau de flux de trésorerie
- Annexes légales
- Grand livre
- Balance générale
- Journal des opérations diverses
- FEC (France) / États de synthèse SYSCOHADA (CI)
```

### 5.2 Module Achats / Procurement (nexus-procurement)

```java
// Workflow d'achat complet
PurchaseRequest → RFQ → Quote Comparison → PurchaseOrder
    → GoodsReceipt → QualityControl → SupplierInvoice
    → 3-Way Matching → Payment → Accounting Entry

// IA dans les achats
- Recommandation fournisseur optimal (ML scoring)
- Détection anomalie prix (> 15% écart historique → alerte)
- Prévision besoins réapprovisionnement
- Classification ABC fournisseurs automatique
- Analyse risque fournisseur (score financier)

// e-Procurement
- Catalogue produits en ligne
- Portail fournisseur self-service
- EDI (EDIFACT, XML/EDI)
- Punch-out catalog (OCI/cXML)
```

### 5.3 Module Stocks / Inventory (nexus-inventory)

```java
// Gestion multi-entrepôts
- Emplacements hiérarchiques (entrepôt > zone > allée > étagère > case)
- FIFO/FEFO enforcement (pharmaceutique)
- Numéros de série et lots
- DLC/DLUO tracking (produits périssables)
- Inventaire tournant (cycle counting) avec IA
- VMI (Vendor Managed Inventory)
- Consignation client/fournisseur
- Cross-docking

// IA dans les stocks
- Prévision rupture de stock (J+7, J+30)
- Optimisation niveau de sécurité
- Détection obsolescence
- Suggestion emplacement optimal
```

### 5.4 Module Ventes / CRM (nexus-sales)

```java
// Cycle de vente complet
Prospect → Lead → Opportunity → Quote → SalesOrder
    → DeliveryNote → Invoice → Payment → Reconciliation

// Fonctionnalités avancées
- Configurateur produit (options/variantes)
- Prix multi-devises avec fixing automatique
- Remises multi-niveaux (article, famille, client, segment)
- Contrats cadres et appels
- Facturation électronique (Chorus Pro)
- Note de crédit/débit
- Relance automatique intelligente (IA)
- Scoring client (risque impayé)
- Prévision chiffre d'affaires (ML)
```

### 5.5 Module RH / Paie (nexus-hr)

```java
// Gestion RH complète
- Gestion des salariés (dossier complet)
- Contrats de travail (CDI, CDD, intérim, stage)
- Gestion des temps et activités (GTA)
- Congés, absences, RTT
- Formation (plan, DIF/CPF)
- Recrutement (ATS intégré)
- Entretiens annuels
- Organigramme dynamique

// Paie — France
- Calcul bulletins de paie (net/brut/charges)
- DSN mensuelle automatique
- URSSAF, AGIRC-ARRCO
- Prévoyance, mutuelle
- Convention collective (paramétrable)
- Solde tout compte

// Paie — Côte d'Ivoire
- CNPS (Caisse Nationale Prévoyance Sociale)
- Impôt sur traitement et salaires (ITS)
- Taxe d'apprentissage (TA)
- Fonds de Développement Formation Professionnelle (FDFP)

// IA RH
- Prédiction turnover (churn prédictif)
- Recommandations plan formation
- Analyse charge de travail et burn-out risk
- Matching candidats/postes
```

### 5.6 Module Production (nexus-production)

```java
// Fonctions MRP/MES
- Nomenclatures multi-niveaux avec variantes
- Gammes opératoires
- Plan Industriel et Commercial (PIC)
- Programme Directeur de Production (PDP/MPS)
- Calcul des Besoins Nets (CBN/MRP)
- Ordres de Fabrication (OF)
- Ordonnancement (Gantt interactif)
- Suivi temps réel production (MES lite)
- Contrôle qualité en cours de fabrication
- Prix de revient réel vs standard

// IA Production
- Prédiction pannes machines (PdM)
- Optimisation planning charge
- Détection dérives qualité
- Simulation "what-if" production
```

### 5.7 Module IA Transversal (nexus-ai)

```python
# FastAPI — Points d'entrée IA

# 1. Assistant Conversationnel (RAG + Anthropic Claude API)
POST /ai/v1/chat
# - Contextualisé par rôle, module, données du tenant
# - Répond en langue de l'utilisateur (FR/EN/...)
# - Peut exécuter des actions (créer facture, valider commande)
# - Cité les sources (quelles données il consulte)

# 2. Anomaly Detection (Isolation Forest + LSTM)
POST /ai/v1/anomaly/detect
# - Analyse écritures comptables
# - Détecte fraudes, doublons, erreurs de saisie
# - Score de confiance + explication (XAI)

# 3. Demand Forecasting (Prophet + XGBoost)
POST /ai/v1/forecast/demand
# - Prévision ventes/achats J+7, J+30, J+90
# - Données saisonnières, tendances, événements

# 4. Document Intelligence (Vision LLM)
POST /ai/v1/documents/extract
# - OCR + extraction structurée de factures PDF/image
# - Pré-remplissage automatique saisie fournisseur

# 5. Compliance Checker
POST /ai/v1/compliance/check
# - Vérifie conformité écriture avec règles PCG/SYSCOHADA
# - Suggestions de correction

# 6. Predictive Analytics
GET /ai/v1/analytics/kpi/{tenantId}
# - KPI prédictifs : trésorerie J+30, risque client, etc.

# 7. Natural Language Query (Text-to-SQL)
POST /ai/v1/query/natural
# - "Montre-moi les factures impayées > 60j pour le client Dupont"
# → SQL généré + résultat tabulaire

# MODE SANS IA
# Si la variable d'environnement AI_ENABLED=false
# Tous les endpoints AI retournent gracefully des données statiques
# Le frontend masque les fonctionnalités IA
# L'application reste 100% fonctionnelle
```

---

## 6. IMPORT DE DONNÉES — TEMPLATES XLSX

### 6.1 Architecture du module d'import

```java
// Pattern Template Method pour tous les imports
public abstract class AbstractExcelImporter<T> {
    public ImportResult<T> process(MultipartFile file) {
        validateFile(file);          // Extension, taille max 50MB
        List<Row> rows = parse(file); // Apache POI
        List<ValidationError> errors = validate(rows); // JSR-303
        if (!errors.isEmpty()) return ImportResult.withErrors(errors);
        List<T> entities = transform(rows);  // Mapping métier
        persist(entities);           // Batch insert (JPA batch size 500)
        publishEvents(entities);     // Kafka events
        return ImportResult.success(entities.size());
    }
}

// Templates disponibles (téléchargeables dans l'UI)
- plan_comptable_template.xlsx    → Import plan comptable
- fournisseurs_template.xlsx      → Import fournisseurs
- clients_template.xlsx           → Import clients
- articles_template.xlsx          → Import articles/produits
- salaries_template.xlsx          → Import salariés
- stocks_initiaux_template.xlsx   → Import stocks initiaux
- ecritures_template.xlsx         → Import écritures d'à-nouveaux
- immobilisations_template.xlsx   → Import immobilisations
- budgets_template.xlsx           → Import budgets prévisionnels
```

### 6.2 Validation et feedback

```java
// Retour détaillé ligne par ligne
public record ImportResult<T>(
    int totalRows,
    int successRows,
    int errorRows,
    List<ImportError> errors,  // { ligne, colonne, valeur, message }
    List<T> importedEntities,
    String downloadableErrorReport  // URL xlsx avec erreurs colorisées
) {}

// L'utilisateur peut :
// 1. Télécharger le template vierge avec exemples (ligne 2 = exemple)
// 2. Remplir localement
// 3. Uploader → validation en temps réel (WebSocket progress)
// 4. Télécharger le rapport d'erreur si KO
// 5. Corriger et ré-uploader
```

---

## 7. NOTIFICATIONS — SMTP GOOGLE

### 7.1 Configuration

```yaml
# application.yml (nexus-notification service)
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${SMTP_USERNAME} # Variable d'environnement (Vault)
    password: ${SMTP_APP_PASSWORD} # Google App Password (pas mdp compte)
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
        transport:
          protocol: smtp
    test-connection: true
```

### 7.2 Types de notifications

```java
public enum NotificationType {
    // Sécurité
    LOGIN_NEW_DEVICE, TWO_FA_CODE, PASSWORD_RESET, ACCOUNT_LOCKED,
    // Finance
    INVOICE_DUE_REMINDER, PAYMENT_RECEIVED, BUDGET_EXCEEDED,
    ACCOUNTING_ANOMALY_DETECTED,
    // Achats
    PURCHASE_ORDER_APPROVED, DELIVERY_EXPECTED, SUPPLIER_INVOICE_RECEIVED,
    // Stocks
    LOW_STOCK_ALERT, EXPIRY_DATE_ALERT,
    // RH
    LEAVE_APPROVED, PAYSLIP_AVAILABLE, CONTRACT_EXPIRY,
    // Système
    IMPORT_COMPLETE, BACKUP_SUCCESS, TENANT_ONBOARDING_COMPLETE,
    // IA
    AI_ANOMALY_DETECTED, AI_FORECAST_READY
}

// Templates email : Thymeleaf HTML responsive
// Langues : FR / EN (basé sur locale utilisateur)
// Design : template premium avec logo tenant
```

---

## 8. TESTS — COUVERTURE 100%

### 8.1 Stratégie de tests — Pyramide

```
          ┌────────────────────────────────────┐
          │    E2E Tests (Playwright/Cypress)  │ 5%
          │    Scénarios métier bout en bout   │
          └────────────────────────────────────┘
        ┌──────────────────────────────────────────┐
        │      Tests d'Intégration               │ 25%
        │  @SpringBootTest + TestContainers       │
        │  Kafka, PostgreSQL, Keycloak réels      │
        └──────────────────────────────────────────┘
    ┌──────────────────────────────────────────────────┐
    │           Tests Unitaires                      │ 70%
    │  JUnit 5 + Mockito (Java)                      │
    │  Jest + Testing Library (Angular)              │
    │  Pytest + coverage (Python)                    │
    └──────────────────────────────────────────────────┘
```

### 8.2 Tests unitaires Java — Règles obligatoires

```java
// RÈGLE : Chaque Use Case = minimum 1 test positif + 1 test négatif + edge cases

@ExtendWith(MockitoExtension.class)
class CreateInvoiceUseCaseTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private NotificationPort notificationPort;
    @Mock private ComplianceChecker complianceChecker;
    @InjectMocks private CreateInvoiceUseCase useCase;

    @Test
    @DisplayName("Should create invoice when all data is valid")
    void shouldCreateInvoice_whenValidData() { ... }

    @Test
    @DisplayName("Should throw DomainException when amount is negative")
    void shouldThrow_whenAmountIsNegative() { ... }

    @Test
    @DisplayName("Should apply French VAT rules when tenant country is FR")
    void shouldApplyFrenchVAT_whenTenantCountryIsFrance() { ... }

    @Test
    @DisplayName("Should apply OHADA rules when tenant country is CI")
    void shouldApplyOHADARules_whenTenantCountryIsCI() { ... }

    // Couverture ligne : 100%
    // Couverture branche : 100%
    // Couverture mutation (PIT) : > 85%
}

// JaCoCo config — build échoue si < 100% coverage
// Exclusions autorisées : @Generated, configuration Spring, main()
```

### 8.3 Tests d'intégration Java — TestContainers

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class InvoiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    // Tests couvrent :
    // - Création facture → écriture DB → event Kafka → notification email
    // - 3-way matching (commande/réception/facture)
    // - Rollback transactionnel sur erreur
    // - Multi-tenant isolation (tenant A ne voit pas données tenant B)
    // - Rate limiting
    // - Authentication JWT
}
```

### 8.4 Tests Angular

```typescript
// Chaque composant/service = fichier .spec.ts obligatoire
// Istanbul coverage : 100% statements, 100% branches, 100% functions

describe('InvoiceListComponent', () => {
    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [InvoiceListComponent],
            providers: [
                provideHttpClientTesting(),
                { provide: InvoiceService, useValue: mockInvoiceService }
            ]
        }).compileComponents();
    });

    it('should display loading state while fetching invoices', ...);
    it('should display empty state when no invoices', ...);
    it('should display error message on API failure', ...);
    it('should filter invoices by status', ...);
    it('should open creation modal on button click', ...);
    it('should be accessible (WCAG 2.1 AA)', ...);
});
```

### 8.5 Tests Python FastAPI

```python
# pytest + pytest-asyncio + httpx
# coverage >= 100%

class TestAnomalyDetection:
    async def test_detects_duplicate_invoice(self, client, sample_invoices):
        ...
    async def test_returns_empty_when_no_anomaly(self, client):
        ...
    async def test_requires_authentication(self, client):
        response = await client.post("/ai/v1/anomaly/detect", json={})
        assert response.status_code == 401
    async def test_handles_large_dataset(self, client, large_dataset):
        # Performance test : < 5s pour 100k transactions
        ...
```

---

## 9. API & DOCUMENTATION

### 9.1 OpenAPI / Swagger

```java
// Chaque service expose /swagger-ui.html et /v3/api-docs
// SpringDoc OpenAPI 3.x

@OpenAPIDefinition(
    info = @Info(
        title = "NexusERP Finance API",
        version = "1.0.0",
        description = "API de gestion financière multi-tenant"
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)

// Toutes les opérations documentées avec :
// - Description FR + EN
// - Exemples de requête et réponse
// - Codes d'erreur possibles
// - Schémas de données complets
// - Pagination documentée
// - Filtres disponibles

// Swagger UI accessible sans auth en développement
// En production : auth requise pour Swagger UI
```

### 9.2 Conventions API REST

```
# Versioning : /api/v1/...
# Pagination : ?page=0&size=20&sort=createdAt,desc
# Filtrage : ?status=PENDING&dateFrom=2024-01-01&dateTo=2024-12-31
# Recherche : ?q=dupont (full-text via Elasticsearch)

# Réponses standardisées
{
  "data": { ... },          // ou [] pour les listes
  "meta": {
    "page": 0,
    "size": 20,
    "total": 150,
    "totalPages": 8
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "traceId": "abc123"       // OpenTelemetry trace ID
}

# Erreurs standardisées (RFC 7807 Problem Details)
{
  "type": "https://nexuserp.io/errors/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Le montant doit être positif",
  "instance": "/api/v1/invoices",
  "traceId": "abc123",
  "errors": [
    { "field": "amount", "message": "must be greater than 0" }
  ]
}
```

---

## 10. MONITORING & OBSERVABILITÉ

### 10.1 Stack LGTM (Loki + Grafana + Tempo + Mimir/Prometheus)

```yaml
# Métriques (Prometheus + Micrometer)
- nexuserp_http_requests_total (par service, méthode, status)
- nexuserp_invoice_created_total (par tenant, pays)
- nexuserp_kafka_lag (par consumer group, topic)
- nexuserp_ai_inference_duration_seconds
- nexuserp_tenant_active_users
- nexuserp_import_rows_processed_total
- JVM metrics (heap, GC, threads)
- PostgreSQL metrics (connexions, query time, deadlocks)

# Logs structurés JSON (Logback + Loki)
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "INFO",
  "service": "nexus-finance",
  "tenantId": "tenant-abc",
  "userId": "user-123",
  "traceId": "abc123",
  "spanId": "def456",
  "message": "Invoice created",
  "invoiceId": "inv-789",
  "amount": 1500.00
}

# Traces distribuées (OpenTelemetry + Tempo)
- Auto-instrumentation Spring Boot
- Propagation Kafka headers (W3C TraceContext)
- Sampling : 100% erreurs, 10% succès (production)

# Dashboards Grafana (pré-configurés)
- Overview plateforme (SLA, erreurs, latence)
- Par tenant (usage, volume, performances)
- Finance (transactions/heure, anomalies)
- IA (inférences, accuracy, drift)
- Infrastructure (K8s nodes, pods, resources)
- Business KPIs (CA, DSO, stock tournant)
```

### 10.2 Alertes

```yaml
# AlertManager rules (Prometheus)
- ServiceDown: service_up == 0 → PagerDuty + Slack
- HighErrorRate: error_rate > 1% sur 5min → Slack
- KafkaConsumerLag > 10000 → Slack
- AnomalyDetected: score > 0.9 → Email + Slack
- TenantStorageLimit: usage > 80% → Email tenant admin
- CertificateExpiry < 30 jours → Email ops
```

---

## 11. INFRASTRUCTURE KUBERNETES / HELM

### 11.1 Structure Helm Charts

```
helm/
├── charts/
│   ├── nexus-gateway/
│   ├── nexus-finance/
│   ├── nexus-sales/
│   ├── nexus-hr/
│   ├── nexus-ai/
│   ├── nexus-auth/
│   ├── nexus-notification/
│   └── nexus-import/
├── infrastructure/
│   ├── postgresql/          # Bitnami chart + config
│   ├── kafka/               # Bitnami chart + config
│   ├── keycloak/            # Bitnami chart + realm import
│   ├── redis/               # Bitnami chart
│   ├── elasticsearch/       # ECK operator
│   ├── grafana-stack/       # Prometheus + Grafana + Loki + Tempo
│   ├── minio/               # Bitnami chart
│   ├── vault/               # HashiCorp Vault
│   └── wikijs/              # Wiki.js custom chart
└── environments/
    ├── values-dev.yaml
    ├── values-staging.yaml
    ├── values-prod-saas.yaml
    └── values-prod-onpremise.yaml
```

### 11.2 Déploiement < 4h

```bash
# Script de déploiement complet (nouveau tenant)
./scripts/deploy-tenant.sh \
  --tenant-id="acme-corp" \
  --country="FR" \
  --plan="enterprise" \
  --admin-email="admin@acme.com" \
  --domain="acme.nexuserp.io"

# Ce script :
# 1. Crée namespace K8s dédié (3 min)
# 2. Installe/configure Keycloak realm (5 min)
# 3. Provisionne schema PostgreSQL + migrations Flyway (2 min)
# 4. Déploie les microservices via Helm (15 min)
# 5. Configure Ingress + TLS cert (Let's Encrypt) (5 min)
# 6. Seed data (plan comptable, taxes, etc.) (3 min)
# 7. Envoie email bienvenue + credentials (1 min)
# Total : ~35 min automatisé
```

### 11.3 Ressources K8s par service

```yaml
# Exemple nexus-finance
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi
replicaCount: 2 # Minimum pour HA
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

---

## 12. WIKI.JS — DOCUMENTATION INTÉGRÉE

### 12.1 Déploiement Docker Compose (dev) / Helm (prod)

```yaml
# docker-compose.yml (extrait)
wikijs:
  image: ghcr.io/requarks/wiki:3
  environment:
    DB_TYPE: postgres
    DB_HOST: postgres
    DB_PORT: 5432
    DB_USER: wikijs
    DB_PASS: ${WIKIJS_DB_PASSWORD}
    DB_NAME: wikijs
  ports:
    - "3000:3000"
  depends_on:
    - postgres
  volumes:
    - wikijs_data:/wiki/data
```

### 12.2 Structure documentation Wiki.js

```
📚 NexusERP Documentation
├── 📖 Guide de démarrage rapide
│   ├── Installation (SaaS / On-Premise)
│   ├── Configuration initiale
│   └── Première connexion
├── 🏗️ Architecture
│   ├── Vue d'ensemble
│   ├── Microservices
│   ├── Sécurité
│   └── Infrastructure K8s
├── 📋 Guides utilisateur (FR + EN)
│   ├── Finance & Comptabilité
│   ├── Achats
│   ├── Stocks
│   ├── Ventes
│   ├── Ressources Humaines
│   ├── Production
│   └── Assistant IA
├── 🔧 Administration
│   ├── Gestion des tenants
│   ├── Keycloak & SSO
│   ├── Import de données
│   └── Paramétrage
├── ⚖️ Conformité légale
│   ├── France (PCG, TVA, FEC)
│   ├── Côte d'Ivoire (SYSCOHADA)
│   └── RGPD
├── 🤖 IA & Analytics
│   ├── Guide assistant IA
│   ├── Anomaly detection
│   └── Forecasting
└── 🔌 API Reference
    └── (Lien vers Swagger UI par service)
```

---

## 13. DESIGN SYSTEM & UX

### 13.1 Charte visuelle — NexusERP Premium Healthcare

```scss
// Design System — tokens CSS
:root {
  // Palette primaire — Bleu profond médical (confiance, rigueur)
  --color-primary-50: #eff6ff;
  --color-primary-100: #dbeafe;
  --color-primary-500: #1e40af; // Bleu marine profond
  --color-primary-600: #1e3a8a;
  --color-primary-900: #0f172a;

  // Accent — Vert émeraude (santé, croissance, validation)
  --color-accent-400: #34d399;
  --color-accent-500: #10b981;
  --color-accent-600: #059669;

  // Semantic
  --color-success: #10b981;
  --color-warning: #f59e0b;
  --color-error: #ef4444;
  --color-info: #3b82f6;

  // Neutrals
  --color-neutral-50: #f8fafc;
  --color-neutral-100: #f1f5f9;
  --color-neutral-900: #0f172a;

  // Typography — Élégance professionnelle
  --font-display: "Plus Jakarta Sans", sans-serif; // Titres
  --font-body: "DM Sans", sans-serif; // Corps
  --font-mono: "JetBrains Mono", monospace; // Code

  // Spacing scale (8px base)
  --space-1: 0.25rem; // 4px
  --space-2: 0.5rem; // 8px
  --space-4: 1rem; // 16px
  --space-8: 2rem; // 32px
  --space-16: 4rem; // 64px

  // Shadows — Depth system
  --shadow-sm: 0 1px 3px rgba(15, 23, 42, 0.08);
  --shadow-md: 0 4px 16px rgba(15, 23, 42, 0.12);
  --shadow-lg: 0 8px 32px rgba(15, 23, 42, 0.16);
  --shadow-glow: 0 0 0 3px rgba(30, 64, 175, 0.15);

  // Border radius
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --radius-xl: 24px;

  // Transitions
  --transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-base: 250ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: 400ms cubic-bezier(0.4, 0, 0.2, 1);
}
```

### 13.2 Composants Angular réutilisables (shared)

```typescript
// Composants à créer dans shared/components/
// Chaque composant = fichier .ts + .html + .scss + .spec.ts SÉPARÉS

NxButtonComponent; // Primaire, secondaire, danger, ghost, icon
NxInputComponent; // Text, number, date, select, textarea
NxTableComponent; // Tri, filtres, pagination, sélection multiple
NxCardComponent; // Avec header/body/footer slots
NxModalComponent; // Confirmation, formulaire, plein écran
NxToastComponent; // Success, error, warning, info
NxBadgeComponent; // Status, compteur, label
NxAvatarComponent; // Initiales, photo
NxSkeletonComponent; // Loading state (shimmer effect)
NxEmptyStateComponent; // No data illustration
NxSearchComponent; // Recherche globale avec suggestions
NxBreadcrumbComponent; // Navigation fil d'Ariane
NxSidebarComponent; // Navigation principale adaptive
NxAiAssistantComponent; // Panneau IA flottant (tous modules)
NxImportWizardComponent; // Wizard import XLSX step-by-step
NxNotificationBellComponent; // Notifications temps réel (WebSocket)
NxLanguageSwitcherComponent; // FR/EN/CI switcher
NxCurrencySwitcherComponent; // EUR/XOF/USD switcher
```

### 13.3 Responsive & Accessibilité

```
// Breakpoints
sm: 640px    // Smartphone portrait
md: 768px    // Smartphone paysage / tablette portrait
lg: 1024px   // Tablette paysage / petit laptop
xl: 1280px   // Laptop standard
2xl: 1536px  // Desktop large

// Accessibilité WCAG 2.1 niveau AA obligatoire
- Contraste minimum 4.5:1 (texte normal)
- Navigation clavier complète (Tab, Arrow keys, Enter, Escape)
- ARIA labels sur tous les éléments interactifs
- Focus visible (outline distinct)
- Annonces screen reader (@angular/cdk/a11y LiveAnnouncer)
- Textes alternatifs images
- Pas d'info transmise par couleur seule

// PWA (Progressive Web App)
- Service Worker (offline mode basique)
- Installable sur smartphone
- Push notifications
- App icon, splash screen
```

---

## 14. INTERNATIONALISATION

### 14.1 Structure i18n Angular

```typescript
// Fichiers de traduction : src/assets/i18n/
// fr-FR.json     — France
// fr-CI.json     — Côte d'Ivoire (variantes terminologiques)
// en-US.json     — Anglais international
// en-GB.json     — Anglais UK

// Clés structurées par module
{
  "common": {
    "save": "Enregistrer",
    "cancel": "Annuler",
    "confirm": "Confirmer"
  },
  "finance": {
    "invoice": {
      "title": "Facture",
      "create": "Créer une facture",
      "status": {
        "PENDING": "En attente",
        "APPROVED": "Approuvée",
        "PAID": "Payée",
        "OVERDUE": "En retard"
      }
    }
  }
}

// Dates : locale-aware (dd/MM/yyyy FR, MM/dd/yyyy US)
// Devises : locale-aware (1.000,00 € FR, $1,000.00 US, 1 000 F XOF CI)
// Nombres : locale-aware
```

---

## 15. ORDRE DE GÉNÉRATION DU CODE

Claude Code doit générer dans cet ordre strict :

```
Phase 1 — Infrastructure (Semaine 1)
├── docker-compose.yml (dev local complet)
├── Helm charts infrastructure (PostgreSQL, Kafka, Redis, Keycloak, MinIO)
├── Helm chart Wiki.js
├── Flyway migration scripts (schema base)
└── Keycloak realm export JSON

Phase 2 — Backend Core (Semaine 2-3)
├── nexus-gateway (Spring Cloud Gateway + JWT filter)
├── nexus-auth (Keycloak integration + 2FA)
├── nexus-core (domaine partagé : entités, events, utils)
├── nexus-notification (SMTP Google + Kafka consumer)
└── nexus-import (module XLSX)

Phase 3 — Modules Métier (Semaine 4-8)
├── nexus-finance (priorité 1)
├── nexus-procurement
├── nexus-inventory
├── nexus-sales
├── nexus-hr
└── nexus-production

Phase 4 — IA (Semaine 9-10)
├── nexus-ai (FastAPI)
├── Modèles ML (Prophet, Isolation Forest, LLM integration)
└── Intégration Anthropic Claude API

Phase 5 — Frontend (Semaine 11-15)
├── Design system (tokens, composants shared)
├── Layout shell + auth
├── Features modules (un par un, suivant priorité backend)
└── PWA + i18n

Phase 6 — Tests & Documentation (Semaine 16-18)
├── Tests unitaires 100%
├── Tests d'intégration 100%
├── Swagger documentation
├── Wiki.js contenu
└── Runbooks opérationnels

Phase 7 — DevOps & Finalisation (Semaine 19-20)
├── CI/CD pipelines
├── Monitoring dashboards Grafana
├── Alertes
├── Scripts déploiement tenant
└── Tests de charge (k6)
```

---

## 16. VARIABLES D'ENVIRONNEMENT

```bash
# Toutes les valeurs sensibles via HashiCorp Vault ou K8s Secrets
# JAMAIS en clair dans le code

# Base de données
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD

# Keycloak
KEYCLOAK_URL, KEYCLOAK_REALM, KEYCLOAK_CLIENT_ID, KEYCLOAK_CLIENT_SECRET

# Kafka
KAFKA_BOOTSTRAP_SERVERS

# Redis
REDIS_HOST, REDIS_PORT, REDIS_PASSWORD

# SMTP Google
SMTP_USERNAME, SMTP_APP_PASSWORD

# IA
ANTHROPIC_API_KEY  # Pour Claude API dans nexus-ai
AI_ENABLED=true    # Bascule IA (true/false)

# Stockage
MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY

# Vault
VAULT_ADDR, VAULT_TOKEN

# Application
APP_ENV=production  # dev / staging / production
TENANT_ISOLATION=schema  # schema / row-level
LOG_LEVEL=INFO
```

---

## 17. RÈGLES DE DÉVELOPPEMENT OBLIGATOIRES

### Code Java

- ✅ Architecture hexagonale stricte (domain/ port/ adapter/ infrastructure/)
- ✅ Pas de logique métier dans les controllers (uniquement orchestration)
- ✅ Pas de logique métier dans les entités JPA (anémique = interdit)
- ✅ Value Objects pour : Money, TenantId, UserId, InvoiceNumber
- ✅ Domain Events pour toute mutation d'état significative
- ✅ @Transactional uniquement dans la couche application (use cases)
- ✅ Records Java pour les DTOs (immuabilité)
- ✅ Sealed classes pour les types algébriques
- ✅ Pattern Builder pour les entités complexes
- ✅ Pas de null → Optional<T> ou lancer exception domaine
- ✅ Logging : JAMAIS de données personnelles en clair dans les logs
- ✅ Toutes les exceptions domaine héritent de NexusException
- ✅ Virtual Threads (Project Loom) pour les I/O-bound operations

### Code Angular

- ✅ Standalone Components (pas de NgModule)
- ✅ Signals (Angular 17+) pour la gestion d'état local
- ✅ OnPush ChangeDetectionStrategy par défaut
- ✅ Lazy loading de toutes les features
- ✅ Pas de `any` TypeScript (strict: true dans tsconfig)
- ✅ Séparation stricte .ts / .html / .scss
- ✅ Pas de logique dans les templates (déléguée au composant)
- ✅ RxJS : unsubscribe proprement (takeUntilDestroyed)
- ✅ HttpClient interceptors pour JWT et error handling global

### Code Python

- ✅ Type hints partout (mypy strict)
- ✅ Pydantic v2 pour toutes les entrées/sorties
- ✅ Async/await pour toutes les opérations I/O
- ✅ Dependency Injection FastAPI pour testabilité
- ✅ Pas d'import \*

---

## 18. CHECKLIST PRÉ-DÉPLOIEMENT

```
□ Tests unitaires : 100% coverage (JaCoCo / Istanbul / pytest-cov)
□ Tests intégration : tous green
□ Swagger : toutes les API documentées
□ Variables d'environnement : toutes dans Vault
□ RGPD : DPO dashboard configuré
□ Monitoring : alertes configurées
□ Backup : politique définie et testée
□ Keycloak : 2FA obligatoire pour rôles sensibles
□ Rate limiting : configuré par tenant
□ Audit log : activé pour toutes actions financières
□ SSL/TLS : certificats valides, HSTS activé
□ Wiki.js : documentation déployée et à jour
□ Smoke tests : exécutés en staging
□ Plan de rollback : documenté
```

Services ajoutés à docker-compose

procurement (8083), inventory (8084), sales
(8087), hr (8088), production (8089), import
(8091), reporting (8092)

---

Procédure de lancement

# 1. Lancer uniquement l'infrastructure

(première fois)
cd D:/OPENLAB/nexuserp
docker-compose up -d postgres kafka redis
keycloak minio

# Attendre ~2 min que Keycloak démarre, puis :

# 2. Lancer tous les services (les builds

Maven se font dans Docker)
docker-compose up -d --build

# OU utiliser le script :

bash scripts/start-dev.sh all

URLs :

┌───────────────────┬───────────────────────┬─
───────────────────┐
│ Service │ URL │
Credentials │
├───────────────────┼───────────────────────┼─
───────────────────┤
│ Application │ http://localhost:4200 │
admin / Admin1234! │
│ Angular │ │
│
├───────────────────┼───────────────────────┼─
───────────────────┤
│ API Gateway │ http://localhost:8080 │
JWT Bearer │
├───────────────────┼───────────────────────┼─
───────────────────┤
│ Keycloak │ http://localhost:8180 │
admin / admin │
├───────────────────┼───────────────────────┼─
───────────────────┤
│ MinIO Console │ http://localhost:9001 │
nexus_minio_access │
├───────────────────┼───────────────────────┼─
───────────────────┤
│ Kafka UI │ http://localhost:8090 │

-                  │
  └───────────────────┴───────────────────────┴─
  ───────────────────┘

Comptes prêts à l'emploi :

- admin / Admin1234! — TENANT_ADMIN + tous les
  rôles manager
- finance / Finance1234! — FINANCE_MANAGER +
  AUDITOR
- ops / Ops12345! — PRODUCTION + INVENTORY +
  PROCUREMENT

---

_Document généré pour NexusERP — EXPERTISE-IA — Grasse, France_
_Version 1.0.0 — Avril 2026_
_Ce fichier est la loi. Claude Code doit le respecter intégralement._
