-- NexusERP — nexus-procurement — Schema initial

CREATE SCHEMA IF NOT EXISTS nexus_procurement;

CREATE TABLE nexus_procurement.suppliers (
    id                  UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(100) NOT NULL,
    code                VARCHAR(30),
    name                VARCHAR(255) NOT NULL,
    contact_name        VARCHAR(100),
    email               VARCHAR(255),
    phone               VARCHAR(30),
    address             TEXT,
    country             VARCHAR(10),
    vat_number          VARCHAR(30),
    iban                VARCHAR(34),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    payment_terms_days  INTEGER      NOT NULL DEFAULT 30,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_suppliers_tenant ON nexus_procurement.suppliers(tenant_id);

CREATE TABLE nexus_procurement.purchase_orders (
    id                      UUID          NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(100)  NOT NULL,
    po_number               VARCHAR(30)   NOT NULL,
    supplier_id             UUID,
    supplier_name           VARCHAR(255),
    status                  VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    expected_delivery_date  DATE,
    actual_delivery_date    DATE,
    currency                VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    subtotal_amount         DECIMAL(19,4),
    tax_amount              DECIMAL(19,4),
    total_amount            DECIMAL(19,4),
    notes                   TEXT,
    approved_by             VARCHAR(100),
    created_by              VARCHAR(100),
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_purchase_orders_tenant ON nexus_procurement.purchase_orders(tenant_id);
CREATE INDEX idx_purchase_orders_status ON nexus_procurement.purchase_orders(tenant_id, status);

CREATE TABLE nexus_procurement.purchase_order_lines (
    id              UUID          NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id           UUID          NOT NULL REFERENCES nexus_procurement.purchase_orders(id),
    line_number     INTEGER       NOT NULL,
    product_code    VARCHAR(50),
    description     VARCHAR(500)  NOT NULL,
    quantity        DECIMAL(19,4) NOT NULL,
    unit_price      DECIMAL(19,4) NOT NULL,
    tax_rate        DECIMAL(5,2)  NOT NULL DEFAULT 20.00,
    line_total      DECIMAL(19,4) NOT NULL
);
