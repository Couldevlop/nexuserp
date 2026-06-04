package com.nexuserp.config.domain.port.in;

import com.nexuserp.config.application.query.ConfigQuery;
import com.nexuserp.config.domain.model.ConfigView;

import java.util.List;

/** Port IN — lecture MASQUÉE de la configuration (jamais de secret en clair). */
public interface GetConfigUseCase {

    /** Liste les paramètres du tenant, filtrés par catégorie si fournie. Secrets masqués. */
    List<ConfigView> list(ConfigQuery query);

    /** Récupère un paramètre par clé. Secret masqué. */
    ConfigView getByKey(String tenantId, String key);
}
