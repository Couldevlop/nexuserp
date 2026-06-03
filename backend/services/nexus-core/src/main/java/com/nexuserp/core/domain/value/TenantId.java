package com.nexuserp.core.domain.value;

import java.util.Objects;

/**
 * Value Object représentant l'identifiant d'un tenant.
 * Garantit la non-nullité et le format valide.
 */
public record TenantId(String value) {

    public TenantId {
        Objects.requireNonNull(value, "TenantId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be blank");
        }
        if (!value.matches("^[a-z0-9][a-z0-9\\-]{2,98}[a-z0-9]$")) {
            throw new IllegalArgumentException("TenantId must be lowercase alphanumeric with hyphens, 4-100 chars: " + value);
        }
    }

    public static TenantId of(String value) {
        return new TenantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
