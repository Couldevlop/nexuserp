-- NexusERP — nexus-inventory — Schema initial

CREATE SCHEMA IF NOT EXISTS nexus_inventory;

CREATE TABLE nexus_inventory.products (
    id                      UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(100) NOT NULL,
    product_code            VARCHAR(50)  NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    category                VARCHAR(100),
    unit                    VARCHAR(20)  NOT NULL DEFAULT 'UNIT',
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    quantity_on_hand        DECIMAL(19,4) NOT NULL DEFAULT 0,
    quantity_reserved       DECIMAL(19,4) NOT NULL DEFAULT 0,
    reorder_point           DECIMAL(19,4) NOT NULL DEFAULT 0,
    reorder_quantity        DECIMAL(19,4) NOT NULL DEFAULT 0,
    safety_stock            DECIMAL(19,4) NOT NULL DEFAULT 0,
    valuation_method        VARCHAR(20)  NOT NULL DEFAULT 'PMP_REALTIME',
    standard_cost_amount    DECIMAL(19,4),
    standard_cost_currency  VARCHAR(3),
    average_cost_amount     DECIMAL(19,4),
    average_cost_currency   VARCHAR(3),
    warehouse_id            VARCHAR(100),
    warehouse_location      VARCHAR(100),
    serial_tracked          BOOLEAN      NOT NULL DEFAULT FALSE,
    lot_tracked             BOOLEAN      NOT NULL DEFAULT FALSE,
    expiry_tracked          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_code_tenant UNIQUE (tenant_id, product_code)
);

CREATE INDEX idx_products_tenant ON nexus_inventory.products(tenant_id);
CREATE INDEX idx_products_category ON nexus_inventory.products(tenant_id, category);
CREATE INDEX idx_products_status ON nexus_inventory.products(tenant_id, status);
CREATE INDEX idx_products_reorder ON nexus_inventory.products(tenant_id, quantity_on_hand, reorder_point);

-- Mouvements de stock
CREATE TABLE nexus_inventory.stock_movements (
    id              UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(100) NOT NULL,
    product_id      UUID         NOT NULL REFERENCES nexus_inventory.products(id),
    movement_type   VARCHAR(20)  NOT NULL CHECK (movement_type IN ('RECEIPT', 'ISSUE', 'ADJUSTMENT', 'TRANSFER', 'RETURN')),
    quantity        DECIMAL(19,4) NOT NULL,
    unit_cost_amount DECIMAL(19,4),
    unit_cost_currency VARCHAR(3),
    reference       VARCHAR(100),
    notes           TEXT,
    created_by      VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_product ON nexus_inventory.stock_movements(product_id, created_at DESC);
CREATE INDEX idx_stock_movements_tenant ON nexus_inventory.stock_movements(tenant_id, created_at DESC);
