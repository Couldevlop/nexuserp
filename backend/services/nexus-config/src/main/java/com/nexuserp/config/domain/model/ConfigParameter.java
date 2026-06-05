package com.nexuserp.config.domain.model;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.domain.value.TenantId;
import com.nexuserp.config.domain.event.ConfigChangedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat ConfigParameter — un paramètre de configuration multi-tenant.
 *
 * Rich domain model : la validation et la machine d'événements vivent ici, jamais dans
 * l'entité JPA (anémique interdit, CLAUDE.md §17).
 *
 * Représentation de la valeur :
 *  - {@code storedValue} contient la valeur TELLE QU'ELLE EST PERSISTÉE :
 *      - pour un SECRET : le texte chiffré (base64 AES-GCM) ;
 *      - pour un non-secret : la valeur en clair.
 *  - Le domaine ne chiffre/déchiffre pas lui-même : c'est le {@code ConfigService} qui
 *    applique le {@code SecretCipher} avant de construire/lire l'agrégat. Le domaine
 *    garantit seulement l'invariant "secret => marqué secret + type SECRET".
 *
 * OWASP :
 *  - A02 : la valeur en clair d'un secret ne transite jamais par cet agrégat persistant.
 *  - A09 : {@link #toString()} ne révèle jamais la valeur.
 */
public class ConfigParameter {

    /** Clés de la forme "payment.wave.apiKey" : segments alphanum + . _ - */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9._-]{0,198}[a-zA-Z0-9]$");
    private static final int MAX_VALUE_LENGTH = 16_384;

    private final UUID id;
    private final TenantId tenantId;
    private final String key;
    private ConfigCategory category;
    private ConfigValueType valueType;
    private boolean secret;
    private String storedValue;       // chiffré si secret, clair sinon
    private String description;
    private String updatedBy;
    private Instant updatedAt;

    private final List<Object> domainEvents;

    private ConfigParameter(Builder b) {
        this.id = b.id != null ? b.id : UUID.randomUUID();
        this.tenantId = b.tenantId;
        this.key = b.key;
        this.category = b.category;
        this.valueType = b.valueType;
        this.secret = b.secret;
        this.storedValue = b.storedValue;
        this.description = b.description;
        this.updatedBy = b.updatedBy;
        this.updatedAt = b.updatedAt != null ? b.updatedAt : Instant.now();
        this.domainEvents = new ArrayList<>();
    }

    // ─── Mutations ──────────────────────────────────────────────────────────────

    /**
     * Met à jour la valeur stockée et les métadonnées d'un paramètre existant.
     * Émet un {@link ConfigChangedEvent} UPSERTED.
     *
     * @param storedValue valeur prête à persister (déjà chiffrée par le service si secret)
     */
    public void update(ConfigCategory category, ConfigValueType valueType, boolean secret,
                       String storedValue, String description, String updatedBy) {
        applyState(category, valueType, secret, storedValue, description, updatedBy);
        this.updatedAt = Instant.now();
        registerUpserted();
    }

    /**
     * Émet l'événement DELETED. Appelé par le service avant suppression effective.
     */
    public void markDeleted(String deletedBy) {
        domainEvents.add(new ConfigChangedEvent(
            tenantId.value(), deletedBy, key, category.name(),
            valueType.name(), secret, ConfigChangedEvent.Action.DELETED));
    }

    private void registerUpserted() {
        domainEvents.add(new ConfigChangedEvent(
            tenantId.value(), updatedBy, key, category.name(),
            valueType.name(), secret, ConfigChangedEvent.Action.UPSERTED));
    }

    private void applyState(ConfigCategory category, ConfigValueType valueType, boolean secret,
                            String storedValue, String description, String updatedBy) {
        if (category == null) throw DomainException.of("CONFIG_INVALID", "category is required");
        if (valueType == null) throw DomainException.of("CONFIG_INVALID", "valueType is required");
        if (storedValue != null && storedValue.length() > MAX_VALUE_LENGTH) {
            throw DomainException.of("CONFIG_INVALID", "value exceeds maximum length");
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            throw DomainException.of("CONFIG_INVALID", "updatedBy is required");
        }
        // Invariant : un SECRET est toujours marqué secret, et vice-versa.
        boolean effectiveSecret = secret || valueType == ConfigValueType.SECRET;
        this.category = category;
        this.valueType = effectiveSecret ? ConfigValueType.SECRET : valueType;
        this.secret = effectiveSecret;
        this.storedValue = storedValue;
        this.description = description;
        this.updatedBy = updatedBy;
    }

    // ─── Events ─────────────────────────────────────────────────────────────────

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    /** Indique si une valeur est définie (non nulle/non vide) — sans la révéler. */
    public boolean hasValue() {
        return storedValue != null && !storedValue.isEmpty();
    }

    // ─── Getters ────────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getKey() { return key; }
    public ConfigCategory getCategory() { return category; }
    public ConfigValueType getValueType() { return valueType; }
    public boolean isSecret() { return secret; }
    public String getStoredValue() { return storedValue; }
    public String getDescription() { return description; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        // A09 — ne jamais journaliser la valeur.
        return "ConfigParameter{key=" + key + ", category=" + category
            + ", valueType=" + valueType + ", secret=" + secret + ", set=" + hasValue() + "}";
    }

    // ─── Builder ────────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private TenantId tenantId;
        private String key;
        private ConfigCategory category;
        private ConfigValueType valueType;
        private boolean secret;
        private String storedValue;
        private String description;
        private String updatedBy;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(TenantId tenantId) { this.tenantId = tenantId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = TenantId.of(tenantId); return this; }
        public Builder key(String key) { this.key = key; return this; }
        public Builder category(ConfigCategory category) { this.category = category; return this; }
        public Builder valueType(ConfigValueType valueType) { this.valueType = valueType; return this; }
        public Builder secret(boolean secret) { this.secret = secret; return this; }
        public Builder storedValue(String storedValue) { this.storedValue = storedValue; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder updatedBy(String updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public ConfigParameter build() {
            validate();
            return new ConfigParameter(this);
        }

        private void validate() {
            if (tenantId == null) throw DomainException.of("CONFIG_INVALID", "tenantId is required");
            if (key == null || key.isBlank()) throw DomainException.of("CONFIG_INVALID", "key is required");
            if (!KEY_PATTERN.matcher(key).matches()) {
                throw DomainException.of("CONFIG_INVALID",
                    "key must match ^[a-zA-Z0-9][a-zA-Z0-9._-]*[a-zA-Z0-9]$ (e.g. payment.wave.apiKey)");
            }
            if (category == null) throw DomainException.of("CONFIG_INVALID", "category is required");
            if (valueType == null) throw DomainException.of("CONFIG_INVALID", "valueType is required");
            if (storedValue != null && storedValue.length() > MAX_VALUE_LENGTH) {
                throw DomainException.of("CONFIG_INVALID", "value exceeds maximum length");
            }
            if (updatedBy == null || updatedBy.isBlank()) throw DomainException.of("CONFIG_INVALID", "updatedBy is required");
            // Normaliser l'invariant secret au build aussi.
            if (valueType == ConfigValueType.SECRET) {
                this.secret = true;
            }
            if (this.secret) {
                this.valueType = ConfigValueType.SECRET;
            }
        }
    }
}
