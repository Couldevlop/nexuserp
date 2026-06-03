-- NexusERP — nexus-hr — Schema initial

CREATE SCHEMA IF NOT EXISTS nexus_hr;

CREATE TABLE nexus_hr.employees (
    id                      UUID          NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               VARCHAR(100)  NOT NULL,
    employee_number         VARCHAR(30)   NOT NULL,
    first_name              VARCHAR(100)  NOT NULL,
    last_name               VARCHAR(100)  NOT NULL,
    email                   VARCHAR(255),
    phone                   VARCHAR(30),
    department              VARCHAR(100),
    job_title               VARCHAR(200),
    contract_type           VARCHAR(20)   NOT NULL DEFAULT 'CDI',
    status                  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    hire_date               DATE          NOT NULL,
    termination_date        DATE,
    gross_salary_amount     DECIMAL(19,4) NOT NULL,
    gross_salary_currency   VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    country                 VARCHAR(10)   NOT NULL DEFAULT 'FR',
    social_security_number  VARCHAR(50),
    bank_iban               VARCHAR(34),
    bank_bic                VARCHAR(11),
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_employee_number_tenant UNIQUE (tenant_id, employee_number)
);

CREATE INDEX idx_employees_tenant ON nexus_hr.employees(tenant_id);
CREATE INDEX idx_employees_department ON nexus_hr.employees(tenant_id, department);
CREATE INDEX idx_employees_status ON nexus_hr.employees(tenant_id, status);

CREATE TABLE nexus_hr.leaves (
    id               UUID          NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        VARCHAR(100)  NOT NULL,
    employee_id      UUID          NOT NULL REFERENCES nexus_hr.employees(id),
    leave_type       VARCHAR(20)   NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    start_date       DATE          NOT NULL,
    end_date         DATE          NOT NULL,
    duration_days    INTEGER       NOT NULL,
    reason           TEXT,
    approved_by      VARCHAR(100),
    rejected_by      VARCHAR(100),
    rejection_reason TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leaves_employee ON nexus_hr.leaves(employee_id, created_at DESC);
CREATE INDEX idx_leaves_tenant_status ON nexus_hr.leaves(tenant_id, status);
