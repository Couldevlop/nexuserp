-- NexusERP — nexus-sales — Schema initial

CREATE SCHEMA IF NOT EXISTS nexus_sales;

CREATE TABLE nexus_sales.customers (
    id            UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     VARCHAR(100) NOT NULL,
    code          VARCHAR(30),
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255),
    phone         VARCHAR(30),
    address       TEXT,
    country       VARCHAR(10),
    vat_number    VARCHAR(30),
    credit_limit  DECIMAL(19,4),
    payment_terms INTEGER      NOT NULL DEFAULT 30,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_tenant ON nexus_sales.customers(tenant_id);

CREATE TABLE nexus_sales.sales_orders (
    id                       UUID          NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                VARCHAR(100)  NOT NULL,
    order_number             VARCHAR(30)   NOT NULL,
    customer_id              UUID,
    customer_name            VARCHAR(255),
    customer_ref             VARCHAR(50),
    status                   VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    order_date               DATE          NOT NULL DEFAULT CURRENT_DATE,
    requested_delivery_date  DATE,
    actual_delivery_date     DATE,
    currency                 VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    subtotal_amount          DECIMAL(19,4),
    tax_amount               DECIMAL(19,4),
    total_amount             DECIMAL(19,4),
    shipping_address         TEXT,
    notes                    TEXT,
    created_by               VARCHAR(100),
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sales_orders_tenant ON nexus_sales.sales_orders(tenant_id);
CREATE INDEX idx_sales_orders_status ON nexus_sales.sales_orders(tenant_id, status);
CREATE INDEX idx_sales_orders_customer ON nexus_sales.sales_orders(customer_id);

CREATE TABLE nexus_sales.sales_order_lines (
    id           UUID          NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID          NOT NULL REFERENCES nexus_sales.sales_orders(id),
    line_number  INTEGER       NOT NULL,
    product_code VARCHAR(50),
    product_name VARCHAR(255)  NOT NULL,
    quantity     DECIMAL(19,4) NOT NULL,
    unit_price   DECIMAL(19,4) NOT NULL,
    tax_rate     DECIMAL(5,2)  NOT NULL DEFAULT 20.00,
    line_total   DECIMAL(19,4) NOT NULL
);
