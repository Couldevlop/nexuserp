package com.nexuserp.config.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository — requêtes paramétrées uniquement (A03 — pas de SQL concaténé).
 * Toutes les lectures/écritures sont scoppées par tenantId (A01).
 */
public interface ConfigJpaRepository extends JpaRepository<ConfigJpaEntity, UUID> {

    Optional<ConfigJpaEntity> findByTenantIdAndConfigKey(String tenantId, String configKey);

    List<ConfigJpaEntity> findByTenantId(String tenantId);

    List<ConfigJpaEntity> findByTenantIdAndCategory(String tenantId, String category);

    @Modifying
    @Query("delete from ConfigJpaEntity c where c.tenantId = :tenantId and c.configKey = :key")
    int deleteByTenantIdAndConfigKey(@Param("tenantId") String tenantId, @Param("key") String key);
}
