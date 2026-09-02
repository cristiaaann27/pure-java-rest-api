package com.cristiannustes.domain.validation;

import static com.cristiannustes.testing.Assertions.assertEquals;
import static com.cristiannustes.testing.Assertions.assertThrows;

import java.util.List;
import java.util.regex.Pattern;

import com.cristiannustes.http.error.FieldError;
import com.cristiannustes.http.error.ValidationException;
import com.cristiannustes.testing.Test;

public class ValidatorTest {

    @Test
    public void reportsEveryProblemAtOnce() {
        ValidationException failure = assertThrows(ValidationException.class, () -> Validator.create()
            .required("login", "")
            .required("password", null)
            .check());

        assertEquals(List.of(
            new FieldError("login", "must not be empty"),
            new FieldError("password", "must not be empty")), failure.errors());
    }

    @Test
    public void doesNotComplainTwiceAboutAMissingValue() {
        ValidationException failure = assertThrows(ValidationException.class, () -> Validator.create()
            .required("login", null)
            .length("login", null, 3, 64)
            .matches("login", null, Pattern.compile("\\d+"), "digits only")
            .check());

        assertEquals(1, failure.errors().size());
    }

    @Test
    public void staysSilentWhenEverythingIsValid() {
        Validator.create()
            .required("login", "marcin")
            .length("login", "marcin", 3, 64)
            .matches("login", "marcin", Pattern.compile("[a-z]+"), "letters only")
            .check();
    }
}
