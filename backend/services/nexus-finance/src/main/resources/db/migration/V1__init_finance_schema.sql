-- NexusERP Finance — Schema initial
-- Flyway V1 : Création des tables domaine comptable

-- ─────────────────────────────────────────
-- Plan Comptable
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_finance.chart_of_accounts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       VARCHAR(100) NOT NULL,
    account_number  VARCHAR(20) NOT NULL,
    account_name    VARCHAR(255) NOT NULL,
    account_type    VARCHAR(50) NOT NULL, -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    parent_account  VARCHAR(20),
    currency        CHAR(3) NOT NULL DEFAULT 'EUR',
    country         CHAR(2) NOT NULL DEFAULT 'FR',
    is_detail       BOOLEAN NOT NULL DEFAULT true,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, account_number)
);

CREATE INDEX idx_coa_tenant ON nexus_finance.chart_of_accounts (tenant_id);
CREATE INDEX idx_coa_number ON nexus_finance.chart_of_accounts (account_number);

ALTER TABLE nexus_finance.chart_of_accounts ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_finance.chart_of_accounts
    USING (tenant_id = current_setting('app.tenant_id', true));

-- ─────────────────────────────────────────
-- Exercice Comptable
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_finance.fiscal_years (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       VARCHAR(100) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, CLOSING, CLOSED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);

ALTER TABLE nexus_finance.fiscal_years ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_finance.fiscal_years
    USING (tenant_id = current_setting('app.tenant_id', true));

-- ─────────────────────────────────────────
-- Journaux Comptables
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_finance.journals (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   VARCHAR(100) NOT NULL,
    code        VARCHAR(20) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(50) NOT NULL, -- GENERAL, SALE, PURCHASE, BANK, CASH, MISC
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, code)
);

ALTER TABLE nexus_finance.journals ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_finance.journals
    USING (tenant_id = current_setting('app.tenant_id', true));

-- ─────────────────────────────────────────
-- Centres Analytiques
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_finance.cost_centers (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   VARCHAR(100) NOT NULL,
    code        VARCHAR(50) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    parent_id   UUID REFERENCES nexus_finance.cost_centers(id),
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, code)
);

ALTER TABLE nexus_finance.cost_centers ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_finance.cost_centers
    USING (tenant_id = current_setting('app.tenant_id', true));

-- ─────────────────────────────────────────
-- Écritures Comptables (Journal Entries)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_finance.journal_entries (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       VARCHAR(100) NOT NULL,
    entry_number    VARCHAR(50) NOT NULL,
    journal_id      UUID NOT NULL REFERENCES nexus_finance.journals(id),
    fiscal_year_id  UUID NOT NULL REFERENCES nexus_finance.fiscal_years(id),
    entry_date      DATE NOT NULL,
    description     VARCHAR(500),
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, VALIDATED, LOCKED
    reference       VARCHAR(100),
    total_debit     NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_credit    NUMERIC(18,4) NOT NULL DEFAULT 0,
    created_by      VARCHAR(255) NOT NULL,
    validated_by    VARCHAR(255),
    validated_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, entry_number)
);

CREATE INDEX idx_je_tenant_date ON nexus_finance.journal_entries (tenant_id, entry_date DESC);
CREATE INDEX idx_je_status ON nexus_finance.journal_entries (tenant_id, status);
CREATE INDEX idx_je_fiscal_year ON nexus_finance.journal_entries (fiscal_year_id);

ALTER TABLE nexus_finance.journal_entries ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_finance.journal_entries
    USING (tenant_id = current_setting('app.tenant_id', true));

-- ─────────────────────────────────────────
-- Lignes d'Écriture (Journal Entry Lines)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_finance.journal_entry_lines (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       VARCHAR(100) NOT NULL,
    entry_id        UUID NOT NULL REFERENCES nexus_finance.journal_entries(id) ON DELETE CASCADE,
    line_number     SMALLINT NOT NULL,
    account_id      UUID NOT NULL REFERENCES nexus_finance.chart_of_accounts(id),
    cost_center_id  UUID REFERENCES nexus_finance.cost_centers(id),
    description     VARCHAR(500),
    debit           NUMERIC(18,4) NOT NULL DEFAULT 0,
    credit          NUMERIC(18,4) NOT NULL DEFAULT 0,
    currency        CHAR(3) NOT NULL DEFAULT 'EUR',
    exchange_rate   NUMERIC(12,6) NOT NULL DEFAULT 1,
    amount_currency NUMERIC(18,4),
    tax_code        VARCHAR(20),
    reference       VARCHAR(100),
    due_date        DATE,
    reconciled      BOOLEAN NOT NULL DEFAULT false,
    reconcile_id    UUID
);

CREATE INDEX idx_jel_entry ON nexus_finance.journal_entry_lines (entry_id);
CREATE INDEX idx_jel_account ON nexus_finance.journal_entry_lines (account_id, tenant_id);
CREATE INDEX idx_jel_cost_center ON nexus_finance.journal_entry_lines (cost_center_id);

ALTER TABLE nexus_finance.journal_entry_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_finance.journal_entry_lines
    USING (tenant_id = current_setting('app.tenant_id', true));

-- ─────────────────────────────────────────
-- Factures (Invoices)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_finance.invoices (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       VARCHAR(100) NOT NULL,
    invoice_number  VARCHAR(50) NOT NULL,
    type            VARCHAR(50) NOT NULL, -- CUSTOMER, SUPPLIER, CREDIT_NOTE, DEBIT_NOTE
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    partner_id      UUID,
    partner_name    VARCHAR(255),
    partner_vat     VARCHAR(50),
    invoice_date    DATE NOT NULL,
    due_date        DATE,
    currency        CHAR(3) NOT NULL DEFAULT 'EUR',
    subtotal        NUMERIC(18,4) NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(18,4) NOT NULL DEFAULT 0,
    total           NUMERIC(18,4) NOT NULL DEFAULT 0,
    amount_paid     NUMERIC(18,4) NOT NULL DEFAULT 0,
    amount_due      NUMERIC(18,4) GENERATED ALWAYS AS (total - amount_paid) STORED,
    journal_entry_id UUID REFERENCES nexus_finance.journal_entries(id),
    notes           TEXT,
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, invoice_number)
);

CREATE INDEX idx_inv_tenant_status ON nexus_finance.invoices (tenant_id, status);
CREATE INDEX idx_inv_due_date ON nexus_finance.invoices (due_date, tenant_id) WHERE status != 'PAID';
CREATE INDEX idx_inv_partner ON nexus_finance.invoices (tenant_id, partner_id);

ALTER TABLE nexus_finance.invoices ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_finance.invoices
    USING (tenant_id = current_setting('app.tenant_id', true));

-- ─────────────────────────────────────────
-- Lignes de Facture
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_finance.invoice_lines (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       VARCHAR(100) NOT NULL,
    invoice_id      UUID NOT NULL REFERENCES nexus_finance.invoices(id) ON DELETE CASCADE,
    line_number     SMALLINT NOT NULL,
    description     VARCHAR(500) NOT NULL,
    product_code    VARCHAR(50),
    quantity        NUMERIC(18,4) NOT NULL DEFAULT 1,
    unit_price      NUMERIC(18,4) NOT NULL DEFAULT 0,
    discount_pct    NUMERIC(5,2) NOT NULL DEFAULT 0,
    tax_rate        NUMERIC(5,2) NOT NULL DEFAULT 20,
    tax_amount      NUMERIC(18,4) NOT NULL DEFAULT 0,
    subtotal        NUMERIC(18,4) NOT NULL DEFAULT 0,
    total           NUMERIC(18,4) NOT NULL DEFAULT 0,
    account_id      UUID REFERENCES nexus_finance.chart_of_accounts(id),
    cost_center_id  UUID REFERENCES nexus_finance.cost_centers(id)
);

ALTER TABLE nexus_finance.invoice_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_finance.invoice_lines
    USING (tenant_id = current_setting('app.tenant_id', true));

-- ─────────────────────────────────────────
-- Budgets
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_finance.budgets (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       VARCHAR(100) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    fiscal_year_id  UUID NOT NULL REFERENCES nexus_finance.fiscal_years(id),
    account_id      UUID NOT NULL REFERENCES nexus_finance.chart_of_accounts(id),
    cost_center_id  UUID REFERENCES nexus_finance.cost_centers(id),
    period_month    SMALLINT,        -- NULL = annuel, 1-12 = mensuel
    budgeted_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    actual_amount   NUMERIC(18,4) NOT NULL DEFAULT 0,
    variance        NUMERIC(18,4) GENERATED ALWAYS AS (actual_amount - budgeted_amount) STORED,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE nexus_finance.budgets ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_finance.budgets
    USING (tenant_id = current_setting('app.tenant_id', true));

-- ─────────────────────────────────────────
-- Immobilisations
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexus_finance.fixed_assets (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           VARCHAR(100) NOT NULL,
    asset_code          VARCHAR(50) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    category            VARCHAR(100),
    acquisition_date    DATE NOT NULL,
    acquisition_cost    NUMERIC(18,4) NOT NULL,
    residual_value      NUMERIC(18,4) NOT NULL DEFAULT 0,
    useful_life_months  INTEGER NOT NULL,
    depreciation_method VARCHAR(50) NOT NULL DEFAULT 'LINEAR', -- LINEAR, DECLINING
    account_id          UUID REFERENCES nexus_finance.chart_of_accounts(id),
    depreciation_account UUID REFERENCES nexus_finance.chart_of_accounts(id),
    accumulated_depreciation NUMERIC(18,4) NOT NULL DEFAULT 0,
    net_book_value      NUMERIC(18,4) GENERATED ALWAYS AS (acquisition_cost - accumulated_depreciation) STORED,
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, asset_code)
);

ALTER TABLE nexus_finance.fixed_assets ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON nexus_finance.fixed_assets
    USING (tenant_id = current_setting('app.tenant_id', true));

-- Triggers updated_at
CREATE TRIGGER trg_coa_updated_at BEFORE UPDATE ON nexus_finance.chart_of_accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_je_updated_at BEFORE UPDATE ON nexus_finance.journal_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_inv_updated_at BEFORE UPDATE ON nexus_finance.invoices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_fa_updated_at BEFORE UPDATE ON nexus_finance.fixed_assets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
