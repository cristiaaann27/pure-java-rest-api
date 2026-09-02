package com.cristiannustes.http.error;

import java.util.List;

public final class ValidationException extends BadRequestException {

    private static final long serialVersionUID = 1L;

    private final transient List<FieldError> errors;

    public ValidationException(List<FieldError> errors) {
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("a validation failure needs at least one field error");
        }
        super("the request is not valid");
        this.errors = List.copyOf(errors);
    }

    public List<FieldError> errors() {
        return errors;
    }
}
