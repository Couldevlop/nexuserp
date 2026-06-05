package com.nexuserp.config.application.command;

import com.nexuserp.config.domain.model.ConfigCategory;
import com.nexuserp.config.domain.model.ConfigValueType;

/**
 * Commande d'upsert (création/mise à jour) d'un paramètre.
 * {@code value} est la valeur EN CLAIR fournie par l'admin ; le service la chiffre
 * si le paramètre est un secret avant persistance.
 */
public record UpsertConfigCommand(
    String tenantId,
    String key,
    String value,
    ConfigValueType valueType,
    ConfigCategory category,
    boolean secret,
    String description,
    String updatedBy
) {}
