package com.nexuserp.config.adapter.out.persistence;

import com.nexuserp.config.domain.model.ConfigCategory;
import com.nexuserp.config.domain.model.ConfigParameter;
import com.nexuserp.config.domain.model.ConfigValueType;
import com.nexuserp.config.domain.port.out.ConfigRepository;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptateur JPA pour le port OUT ConfigRepository.
 * Traduction Domain Model ↔ JPA Entity.
 */
@Repository
public class ConfigRepositoryAdapter implements ConfigRepository {

    private final ConfigJpaRepository jpa;

    public ConfigRepositoryAdapter(ConfigJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ConfigParameter save(ConfigParameter parameter) {
        ConfigJpaEntity entity = jpa.findById(parameter.getId()).orElseGet(ConfigJpaEntity::new);
        toEntity(parameter, entity);
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<ConfigParameter> findByTenantIdAndKey(String tenantId, String key) {
        return jpa.findByTenantIdAndConfigKey(tenantId, key).map(this::toDomain);
    }

    @Override
    public List<ConfigParameter> findByTenantId(String tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ConfigParameter> findByTenantIdAndCategory(String tenantId, ConfigCategory category) {
        return jpa.findByTenantIdAndCategory(tenantId, category.name()).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean deleteByTenantIdAndKey(String tenantId, String key) {
        return jpa.deleteByTenantIdAndConfigKey(tenantId, key) > 0;
    }

    // ─── Mapping ────────────────────────────────────────────────────────────────

    private void toEntity(ConfigParameter p, ConfigJpaEntity e) {
        e.setId(p.getId());
        e.setTenantId(p.getTenantId().value());
        e.setConfigKey(p.getKey());
        e.setCategory(p.getCategory().name());
        e.setValueType(p.getValueType().name());
        e.setSecret(p.isSecret());
        e.setConfigValue(p.getStoredValue());
        e.setDescription(p.getDescription());
        e.setUpdatedBy(p.getUpdatedBy());
    }

    private ConfigParameter toDomain(ConfigJpaEntity e) {
        return ConfigParameter.builder()
            .id(e.getId())
            .tenantId(e.getTenantId())
            .key(e.getConfigKey())
            .category(ConfigCategory.valueOf(e.getCategory()))
            .valueType(ConfigValueType.valueOf(e.getValueType()))
            .secret(e.isSecret())
            .storedValue(e.getConfigValue())
            .description(e.getDescription())
            .updatedBy(e.getUpdatedBy())
            .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant(ZoneOffset.UTC) : null)
            .build();
    }
}
