-- NexusERP — nexus-auth — Schema initial
-- Gestion 2FA et sessions

CREATE SCHEMA IF NOT EXISTS nexus_auth;

CREATE TABLE nexus_auth.user_two_factor (
    user_id        UUID         NOT NULL PRIMARY KEY,
    tenant_id      VARCHAR(100) NOT NULL,
    method         VARCHAR(20)  NOT NULL CHECK (method IN ('TOTP', 'WEBAUTHN', 'SMS', 'EMAIL')),
    status         VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING_SETUP', 'ACTIVE', 'DISABLED')),
    totp_secret    VARCHAR(64),
    recovery_codes TEXT,
    last_used_at   TIMESTAMPTZ,
    enabled_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_two_factor_tenant ON nexus_auth.user_two_factor(tenant_id);
CREATE INDEX idx_user_two_factor_status ON nexus_auth.user_two_factor(status);

-- Table de log des événements d'authentification
CREATE TABLE nexus_auth.auth_event_log (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID         NOT NULL,
    tenant_id   VARCHAR(100) NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    success     BOOLEAN      NOT NULL DEFAULT TRUE,
    details     JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auth_event_log_user ON nexus_auth.auth_event_log(user_id, created_at DESC);
CREATE INDEX idx_auth_event_log_tenant ON nexus_auth.auth_event_log(tenant_id, created_at DESC);
