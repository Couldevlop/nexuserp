package com.nexuserp.config.domain.port.out;

import com.nexuserp.config.domain.model.ConfigCategory;
import com.nexuserp.config.domain.model.ConfigParameter;

import java.util.List;
import java.util.Optional;

/** Port OUT — persistance des paramètres. Toutes les requêtes scoppées par tenant (A01). */
public interface ConfigRepository {

    ConfigParameter save(ConfigParameter parameter);

    Optional<ConfigParameter> findByTenantIdAndKey(String tenantId, String key);

    List<ConfigParameter> findByTenantId(String tenantId);

    List<ConfigParameter> findByTenantIdAndCategory(String tenantId, ConfigCategory category);

    /** @return true si une ligne a été supprimée. */
    boolean deleteByTenantIdAndKey(String tenantId, String key);
}
