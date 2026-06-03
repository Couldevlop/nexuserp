-- NexusERP — Initialisation PostgreSQL 16
-- Création des schémas par service (isolation multi-tenant)

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";    -- Recherche full-text
CREATE EXTENSION IF NOT EXISTS "btree_gist"; -- Index avancés

-- Schémas infrastructure
CREATE SCHEMA IF NOT EXISTS keycloak;
CREATE SCHEMA IF NOT EXISTS wikijs;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS tenant_management;

-- Schémas services NexusERP (tenant par défaut: public)
CREATE SCHEMA IF NOT EXISTS nexus_auth;
CREATE SCHEMA IF NOT EXISTS nexus_finance;
CREATE SCHEMA IF NOT EXISTS nexus_procurement;
CREATE SCHEMA IF NOT EXISTS nexus_inventory;
CREATE SCHEMA IF NOT EXISTS nexus_sales;
CREATE SCHEMA IF NOT EXISTS nexus_hr;
CREATE SCHEMA IF NOT EXISTS nexus_production;
CREATE SCHEMA IF NOT EXISTS nexus_notification;

-- Base wikijs
CREATE DATABASE wikijs;
GRANT ALL PRIVILEGES ON DATABASE wikijs TO nexus;

-- Rôles PostgreSQL
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'nexus_app') THEN
        CREATE ROLE nexus_app LOGIN PASSWORD 'nexus_app_secret';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'nexus_readonly') THEN
        CREATE ROLE nexus_readonly LOGIN PASSWORD 'nexus_readonly_secret';
    END IF;
END
$$;

GRANT USAGE ON SCHEMA nexus_auth, nexus_finance, nexus_procurement, nexus_inventory,
              nexus_sales, nexus_hr, nexus_production, nexus_notification TO nexus_app;
GRANT SELECT ON ALL TABLES IN SCHEMA nexus_finance TO nexus_readonly;

-- Table tenant_management (master record des tenants)
CREATE TABLE IF NOT EXISTS tenant_management.tenants (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       VARCHAR(100) UNIQUE NOT NULL,
    name            VARCHAR(255) NOT NULL,
    country         CHAR(2) NOT NULL,                         -- FR, CI, SN, etc.
    plan            VARCHAR(50) NOT NULL DEFAULT 'starter',   -- starter, pro, enterprise
    status          VARCHAR(50) NOT NULL DEFAULT 'active',
    schema_name     VARCHAR(100) NOT NULL,
    admin_email     VARCHAR(255) NOT NULL,
    domain          VARCHAR(255),
    keycloak_realm  VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    onboarded_at    TIMESTAMPTZ,
    metadata        JSONB NOT NULL DEFAULT '{}'
);

-- Table audit log globale
CREATE TABLE IF NOT EXISTS audit.audit_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(100),
    user_id         VARCHAR(255),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       VARCHAR(255),
    old_value       JSONB,
    new_value       JSONB,
    ip_address      INET,
    user_agent      TEXT,
    trace_id        VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- Partitions audit (par année)
CREATE TABLE IF NOT EXISTS audit.audit_log_2024 PARTITION OF audit.audit_log
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE IF NOT EXISTS audit.audit_log_2025 PARTITION OF audit.audit_log
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS audit.audit_log_2026 PARTITION OF audit.audit_log
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS audit.audit_log_2027 PARTITION OF audit.audit_log
    FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');

-- Index audit
CREATE INDEX IF NOT EXISTS idx_audit_tenant_date ON audit.audit_log (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit.audit_log (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit.audit_log (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_trace ON audit.audit_log (trace_id);

-- Fonction mise à jour updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Row Level Security helper
CREATE OR REPLACE FUNCTION current_tenant_id()
RETURNS TEXT AS $$
BEGIN
    RETURN current_setting('app.tenant_id', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
