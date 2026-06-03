-- NexusERP — nexus-production — Schema initial

CREATE SCHEMA IF NOT EXISTS nexus_production;

CREATE TABLE nexus_production.work_orders (
    id                  VARCHAR(36)   NOT NULL PRIMARY KEY,
    tenant_id           VARCHAR(100)  NOT NULL,
    order_number        VARCHAR(30)   NOT NULL,
    product_id          VARCHAR(36),
    product_name        VARCHAR(255),
    bom_id              VARCHAR(36),
    routing_id          VARCHAR(36),
    status              VARCHAR(25)   NOT NULL DEFAULT 'PLANNED',
    priority            VARCHAR(10)   NOT NULL DEFAULT 'NORMAL',
    quantity_planned    DECIMAL(19,4) NOT NULL,
    quantity_produced   DECIMAL(19,4) NOT NULL DEFAULT 0,
    quantity_rejected   DECIMAL(19,4) NOT NULL DEFAULT 0,
    planned_start_date  DATE          NOT NULL,
    planned_end_date    DATE          NOT NULL,
    actual_start_date   TIMESTAMPTZ,
    actual_end_date     TIMESTAMPTZ,
    workcenter          VARCHAR(100),
    operator            VARCHAR(100),
    notes               TEXT,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_work_orders_tenant ON nexus_production.work_orders(tenant_id);
CREATE INDEX idx_work_orders_status ON nexus_production.work_orders(tenant_id, status);
CREATE INDEX idx_work_orders_product ON nexus_production.work_orders(product_id);

-- Bill of Materials
CREATE TABLE nexus_production.bom_headers (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    tenant_id    VARCHAR(100) NOT NULL,
    product_id   VARCHAR(36)  NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    version      VARCHAR(20)  NOT NULL DEFAULT '1.0',
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    notes        TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE nexus_production.bom_lines (
    id               VARCHAR(36)   NOT NULL PRIMARY KEY,
    bom_id           VARCHAR(36)   NOT NULL REFERENCES nexus_production.bom_headers(id),
    component_id     VARCHAR(36)   NOT NULL,
    component_name   VARCHAR(255)  NOT NULL,
    quantity         DECIMAL(19,4) NOT NULL,
    unit             VARCHAR(20)   NOT NULL DEFAULT 'UNIT',
    scrap_percentage DECIMAL(5,2)  NOT NULL DEFAULT 0
);
