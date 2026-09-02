package com.cristiannustes.domain.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.cristiannustes.http.error.FieldError;
import com.cristiannustes.http.error.ValidationException;

public final class Validator {

    private final List<FieldError> errors = new ArrayList<>();

    private Validator() {
    }

    public static Validator create() {
        return new Validator();
    }

    public Validator required(String field, String value) {
        if (value == null || value.isBlank()) {
            errors.add(new FieldError(field, "must not be empty"));
        }
        return this;
    }

    public Validator length(String field, String value, int min, int max) {
        if (value != null && (value.length() < min || value.length() > max)) {
            errors.add(new FieldError(field, "must be between %d and %d characters".formatted(min, max)));
        }
        return this;
    }

    public Validator matches(String field, String value, Pattern pattern, String requirement) {
        if (value != null && !value.isBlank() && !pattern.matcher(value).matches()) {
            errors.add(new FieldError(field, requirement));
        }
        return this;
    }

    public void check() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
