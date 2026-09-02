package com.cristiannustes.http.error;

import com.cristiannustes.http.HttpStatus;

public final class ConflictException extends HttpException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
