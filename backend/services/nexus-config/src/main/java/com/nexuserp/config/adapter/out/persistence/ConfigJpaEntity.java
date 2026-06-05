package com.nexuserp.config.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité JPA — table nexus_config.config_parameters.
 * Anémique par conception (mapping pur) : aucune logique métier ici.
 *
 * Unicité (tenant_id, config_key) -> un paramètre par clé et par tenant (A01).
 * {@code config_value} contient la valeur chiffrée (si secret) ou en clair (sinon).
 */
@Entity
@Table(name = "config_parameters", schema = "nexus_config",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_cfg_tenant_key", columnNames = {"tenant_id", "config_key"})
    },
    indexes = {
        @Index(name = "idx_cfg_tenant_category", columnList = "tenant_id,category"),
        @Index(name = "idx_cfg_tenant_key", columnList = "tenant_id,config_key")
    })
public class ConfigJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "config_key", nullable = false, length = 200)
    private String configKey;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType;

    @Column(name = "secret", nullable = false)
    private boolean secret;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public boolean isSecret() { return secret; }
    public void setSecret(boolean secret) { this.secret = secret; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
