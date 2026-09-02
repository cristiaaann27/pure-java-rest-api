package com.cristiannustes.http.error;

import com.cristiannustes.http.HttpStatus;

public final class UnauthorizedException extends HttpException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
