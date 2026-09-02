package com.cristiannustes.testing;

public final class AssertionFailure extends RuntimeException {

    private static final long serialVersionUID = 1L;

    AssertionFailure(String message) {
        super(message);
    }
}
