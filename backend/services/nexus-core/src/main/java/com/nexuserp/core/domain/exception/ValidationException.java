package com.nexuserp.core.domain.exception;

import java.util.List;
import java.util.Map;

public class ValidationException extends NexusException {

    private final List<FieldError> fieldErrors;

    public ValidationException(String message, List<FieldError> fieldErrors) {
        super("VALIDATION_ERROR", message);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public static ValidationException of(String field, String message) {
        return new ValidationException("Validation failed",
            List.of(new FieldError(field, null, message)));
    }

    public record FieldError(String field, Object rejectedValue, String message) {}
}
