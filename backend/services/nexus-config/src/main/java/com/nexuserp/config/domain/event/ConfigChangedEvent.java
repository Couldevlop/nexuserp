package com.nexuserp.config.domain.event;

import com.nexuserp.core.domain.event.DomainEvent;

/**
 * Événement publié à chaque mutation (upsert/delete) d'un paramètre.
 * Topic : nexus.config.changed.
 *
 * Les consommateurs (autres services) l'utilisent pour invalider leur cache local
 * de configuration : "ajouter la clé dans l'UI admin -> elle s'active" sans redéploiement.
 *
 * Ne transporte JAMAIS la valeur (A02/A09) : uniquement tenantId, clé, catégorie et type d'action.
 */
public class ConfigChangedEvent extends DomainEvent {

    public enum Action { UPSERTED, DELETED }

    private final String key;
    private final String category;
    private final String valueType;
    private final boolean secret;
    private final Action action;

    public ConfigChangedEvent(String tenantId, String userId, String key, String category,
                              String valueType, boolean secret, Action action) {
        super("nexus.config.changed", tenantId, userId, null);
        this.key = key;
        this.category = category;
        this.valueType = valueType;
        this.secret = secret;
        this.action = action;
    }

    public String getKey() { return key; }
    public String getCategory() { return category; }
    public String getValueType() { return valueType; }
    public boolean isSecret() { return secret; }
    public Action getAction() { return action; }
}
