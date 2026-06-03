-- NexusERP — nexus-notification — Schema initial

CREATE SCHEMA IF NOT EXISTS nexus_notification;

CREATE TABLE nexus_notification.notification_logs (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    type            VARCHAR(60)  NOT NULL,
    status          VARCHAR(10)  NOT NULL,
    error_message   TEXT,
    sent_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_tenant ON nexus_notification.notification_logs(tenant_id);
CREATE INDEX idx_notif_sent_at ON nexus_notification.notification_logs(sent_at DESC);
