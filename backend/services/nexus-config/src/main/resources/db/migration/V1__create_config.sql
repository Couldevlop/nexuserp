-- NexusERP Config — Schema initial
-- Flyway V1 : magasin centralisé, multi-tenant et chiffré de paramètres/clés API.

-- Fonction trigger updated_at (idempotente, locale au schéma pour autonomie du service).
CREATE OR REPLACE FUNCTION nexus_config.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ─────────────────────────────────────────
-- Paramètres de configuration
--   config_value : valeur CHIFFRÉE (base64 AES-GCM) si secret=true, sinon valeur en clair.
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_config.config_parameters (
    id           UUID PRIMARY KEY,
    tenant_id    VARCHAR(100) NOT NULL,
    config_key   VARCHAR(200) NOT NULL,   -- ex. "payment.wave.apiKey"
    category     VARCHAR(30)  NOT NULL,   -- PAYMENT, NOTIFICATION, TAX, GENERAL, AI, SECURITY, INTEGRATION, REPORTING
    value_type   VARCHAR(20)  NOT NULL,   -- STRING, NUMBER, BOOLEAN, JSON, SECRET
    secret       BOOLEAN      NOT NULL DEFAULT FALSE,
    config_value TEXT,                    -- chiffré si secret, clair sinon
    description  VARCHAR(1000),
    updated_by   VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- A01 : un paramètre par clé et par tenant.
    CONSTRAINT uq_cfg_tenant_key UNIQUE (tenant_id, config_key)
);

-- A01 : indexation scoppée tenant pour les listings/filtres.
CREATE INDEX IF NOT EXISTS idx_cfg_tenant_category ON nexus_config.config_parameters (tenant_id, category);
CREATE INDEX IF NOT EXISTS idx_cfg_tenant_key      ON nexus_config.config_parameters (tenant_id, config_key);

-- Row-Level Security comme filet de sécurité multi-tenant (cohérent avec nexus-payment/finance).
ALTER TABLE nexus_config.config_parameters ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_config.config_parameters
    USING (tenant_id = current_setting('app.tenant_id', true));

-- Trigger updated_at
CREATE TRIGGER trg_cfg_updated_at BEFORE UPDATE ON nexus_config.config_parameters
    FOR EACH ROW EXECUTE FUNCTION nexus_config.update_updated_at_column();
