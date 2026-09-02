package com.cristiannustes.http.error;

import com.cristiannustes.http.HttpStatus;

public sealed class BadRequestException extends HttpException permits ValidationException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
