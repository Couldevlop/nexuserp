-- Registre d'idempotence des paiements externes (Mobile Money via nexus-payment)
-- Garantit l'effet exactly-once à la consommation de nexus.payment.succeeded.

CREATE TABLE IF NOT EXISTS nexus_finance.processed_payments (
    id            VARCHAR(200) PRIMARY KEY,
    tenant_id     VARCHAR(100) NOT NULL,
    payment_id    VARCHAR(100) NOT NULL,
    invoice_id    VARCHAR(100),
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_processed_payment UNIQUE (tenant_id, payment_id)
);

CREATE INDEX IF NOT EXISTS idx_processed_payment_invoice
    ON nexus_finance.processed_payments (tenant_id, invoice_id);
