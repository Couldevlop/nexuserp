package com.nexuserp.config.domain.port.in;

import com.nexuserp.config.application.command.UpsertConfigCommand;
import com.nexuserp.config.domain.model.ConfigView;

/** Port IN — créer ou mettre à jour un paramètre (secrets chiffrés au repos). */
public interface UpsertConfigUseCase {

    /** @return vue MASQUÉE du paramètre persisté (jamais le secret en clair). */
    ConfigView upsert(UpsertConfigCommand command);
}
