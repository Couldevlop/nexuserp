package com.nexuserp.auth.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA stockant l'état 2FA d'un utilisateur Keycloak.
 * Keycloak gère l'authentification principale ; cette table gère les métadonnées 2FA custom.
 */
@Entity
@Table(name = "user_two_factor", schema = "nexus_auth")
public class UserTwoFactor {

    public enum TwoFactorMethod { TOTP, WEBAUTHN, SMS, EMAIL }
    public enum TwoFactorStatus { PENDING_SETUP, ACTIVE, DISABLED }

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private TwoFactorMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TwoFactorStatus status;

    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Column(name = "recovery_codes", columnDefinition = "TEXT")
    private String recoveryCodes; // JSON array of hashed codes

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "enabled_at")
    private Instant enabledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        if (this.status == null) this.status = TwoFactorStatus.PENDING_SETUP;
    }

    public static UserTwoFactor createTotpSetup(UUID userId, String tenantId, String totpSecret) {
        UserTwoFactor utf = new UserTwoFactor();
        utf.userId = userId;
        utf.tenantId = tenantId;
        utf.method = TwoFactorMethod.TOTP;
        utf.status = TwoFactorStatus.PENDING_SETUP;
        utf.totpSecret = totpSecret;
        return utf;
    }

    public void activate(String recoveryCodes) {
        this.status = TwoFactorStatus.ACTIVE;
        this.enabledAt = Instant.now();
        this.recoveryCodes = recoveryCodes;
    }

    public void disable() {
        this.status = TwoFactorStatus.DISABLED;
        this.totpSecret = null;
        this.recoveryCodes = null;
    }

    public void recordUsage() {
        this.lastUsedAt = Instant.now();
    }

    // Getters
    public UUID getUserId() { return userId; }
    public String getTenantId() { return tenantId; }
    public TwoFactorMethod getMethod() { return method; }
    public TwoFactorStatus getStatus() { return status; }
    public String getTotpSecret() { return totpSecret; }
    public String getRecoveryCodes() { return recoveryCodes; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getEnabledAt() { return enabledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isActive() { return TwoFactorStatus.ACTIVE == status; }
}
