<!-- Merci de remplir ce modèle. Les PR incomplètes peuvent être refusées. -->

## Description

<!-- Que fait cette PR ? Pourquoi ? Contexte métier / technique. -->

## Type de changement

- [ ] 🐛 Correction de bug (`fix/**`)
- [ ] ✨ Nouvelle fonctionnalité (`feature/**`)
- [ ] ♻️ Refactoring (sans changement fonctionnel)
- [ ] 🚀 Release (`release/**`) / Hotfix (`hotfix/**`)
- [ ] 📝 Documentation / outillage / CI

## Branche cible

- [ ] Cette PR cible **`develop`** (feature/fix) — _ou_ **`main`** uniquement pour une release/hotfix.

## Lié à

<!-- Issues / tickets : Closes #123 -->

## Comment tester

<!-- Étapes de validation, commandes, captures d'écran si UI. -->

## Checklist qualité

- [ ] Le code respecte la **Clean / Hexagonal Architecture** (domain / port / adapter / infrastructure)
- [ ] Angular : composants `standalone`, signals, `OnPush`, fichiers `.ts/.html/.scss/.spec.ts` séparés (pas de HTML inline)
- [ ] Pas de logique métier dans les controllers ni les entités JPA
- [ ] Tests ajoutés / mis à jour et **CI verte**
- [ ] Documentation / commentaires mis à jour si nécessaire
- [ ] Aucune donnée personnelle (RGPD) loggée en clair

## Checklist sécurité (OWASP)

- [ ] **A01** — Contrôle d'accès : endpoints protégés (`@PreAuthorize`), données filtrées par tenant
- [ ] **A02** — Aucun secret en clair (variables d'env / Vault) ; rien de sensible dans les logs
- [ ] **A03** — Entrées validées (Jakarta Validation côté back, validation côté front) ; pas d'injection
- [ ] **A08** — Intégrité : webhooks/événements vérifiés (signature) ; idempotence des opérations sensibles
- [ ] Pas de nouvelle dépendance vulnérable (CodeQL / Trivy / Dependency-Check OK)

## Impact

- [ ] Migration de base de données (Flyway) incluse si schéma modifié
- [ ] Variables d'environnement / secrets à ajouter documentés
- [ ] Pas de breaking change non documenté
