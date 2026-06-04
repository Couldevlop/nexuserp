package com.nexuserp.config.domain.port.in;

/** Port IN — suppression d'un paramètre (publie un événement DELETED). */
public interface DeleteConfigUseCase {

    void delete(String tenantId, String key, String deletedBy);
}
