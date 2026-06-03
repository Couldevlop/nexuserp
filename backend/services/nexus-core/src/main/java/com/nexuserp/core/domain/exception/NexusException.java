package com.nexuserp.core.domain.exception;

/**
 * Exception racine de NexusERP.
 * Toutes les exceptions domaine héritent de cette classe.
 */
public abstract class NexusException extends RuntimeException {

    private final String errorCode;
    private final String traceId;

    protected NexusException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.traceId = null;
    }

    protected NexusException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.traceId = null;
    }

    protected NexusException(String errorCode, String message, String traceId) {
        super(message);
        this.errorCode = errorCode;
        this.traceId = traceId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getTraceId() {
        return traceId;
    }
}
