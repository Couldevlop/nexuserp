package com.nexuserp.core.domain.value;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object représentant l'identifiant d'un utilisateur.
 * Immuable — wraps UUID pour éviter les confusions de paramètres.
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId cannot be null");
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UserId format: " + value, e);
        }
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
