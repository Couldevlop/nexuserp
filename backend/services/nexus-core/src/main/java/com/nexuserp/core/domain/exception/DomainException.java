package com.nexuserp.core.domain.exception;

public class DomainException extends NexusException {

    public DomainException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static DomainException of(String errorCode, String message) {
        return new DomainException(errorCode, message);
    }

    // Factories pour les cas courants
    public static DomainException notFound(String entity, Object id) {
        return new DomainException("ENTITY_NOT_FOUND", entity + " not found with id: " + id);
    }

    public static DomainException alreadyExists(String entity, String identifier) {
        return new DomainException("ENTITY_ALREADY_EXISTS", entity + " already exists: " + identifier);
    }

    public static DomainException invalidState(String entity, String currentState, String requiredState) {
        return new DomainException("INVALID_STATE",
            entity + " is in state " + currentState + " but required state is " + requiredState);
    }

    public static DomainException unauthorized(String action) {
        return new DomainException("UNAUTHORIZED", "Unauthorized action: " + action);
    }

    public static DomainException tenantMismatch() {
        return new DomainException("TENANT_MISMATCH", "Resource does not belong to current tenant");
    }
}
