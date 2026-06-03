-- NexusERP — nexus-reporting — Schema initial

CREATE SCHEMA IF NOT EXISTS nexus_reporting;

CREATE TABLE nexus_reporting.report_requests (
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    tenant_id      VARCHAR(100) NOT NULL,
    requested_by   VARCHAR(100),
    type           VARCHAR(50)  NOT NULL,
    period_from    DATE,
    period_to      DATE,
    output_format  VARCHAR(10)  NOT NULL DEFAULT 'XLSX',
    status         VARCHAR(15)  NOT NULL DEFAULT 'PENDING',
    download_url   TEXT,
    error_message  TEXT,
    requested_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMPTZ
);

CREATE INDEX idx_reports_tenant ON nexus_reporting.report_requests(tenant_id);
CREATE INDEX idx_reports_status ON nexus_reporting.report_requests(tenant_id, status);
