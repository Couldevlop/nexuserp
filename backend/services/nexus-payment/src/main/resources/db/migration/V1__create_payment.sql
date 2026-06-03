-- NexusERP Payment — Schema initial
-- Flyway V1 : table des paiements Mobile Money (collecte & réconciliation)

-- Fonction trigger updated_at (idempotente, locale au schéma pour autonomie du service).
CREATE OR REPLACE FUNCTION nexus_payment.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ─────────────────────────────────────────
-- Paiements Mobile Money
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_payment.payments (
    id              UUID PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    reference       VARCHAR(80) NOT NULL,
    provider        VARCHAR(30) NOT NULL,  -- ORANGE_MONEY, WAVE, MTN_MOMO, MOOV_MONEY
    direction       VARCHAR(20) NOT NULL DEFAULT 'COLLECTION', -- COLLECTION, DISBURSEMENT
    msisdn          VARCHAR(20) NOT NULL,
    amount          NUMERIC(18,4) NOT NULL,
    currency        CHAR(3) NOT NULL DEFAULT 'XOF',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, INITIATED, SUCCEEDED, FAILED, CANCELLED, REFUNDED
    external_tx_id  VARCHAR(120),
    invoice_id      UUID,                  -- lien optionnel vers nexus-finance.invoices
    idempotency_key VARCHAR(120) NOT NULL,
    created_by      VARCHAR(255) NOT NULL,
    failure_reason  VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- A04 (Insecure Design) : unicité par tenant pour éviter le double-débit.
    CONSTRAINT uq_pay_tenant_reference UNIQUE (tenant_id, reference),
    CONSTRAINT uq_pay_tenant_idem      UNIQUE (tenant_id, idempotency_key)
);

-- A01 : indexation scoppée tenant pour les listings.
CREATE INDEX IF NOT EXISTS idx_pay_tenant_status  ON nexus_payment.payments (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_pay_tenant_created ON nexus_payment.payments (tenant_id, created_at DESC);
-- Recherche par référence (chemin webhook).
CREATE INDEX IF NOT EXISTS idx_pay_reference      ON nexus_payment.payments (reference);
CREATE INDEX IF NOT EXISTS idx_pay_invoice        ON nexus_payment.payments (tenant_id, invoice_id);

-- Row-Level Security comme filet de sécurité multi-tenant (cohérent avec nexus-finance).
ALTER TABLE nexus_payment.payments ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_payment.payments
    USING (tenant_id = current_setting('app.tenant_id', true));

-- Trigger updated_at
CREATE TRIGGER trg_pay_updated_at BEFORE UPDATE ON nexus_payment.payments
    FOR EACH ROW EXECUTE FUNCTION nexus_payment.update_updated_at_column();
