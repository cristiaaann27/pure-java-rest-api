package com.cristiannustes.http.error;

import com.cristiannustes.http.HttpStatus;

public final class NotFoundException extends HttpException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
