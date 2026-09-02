package com.cristiannustes.http.error;

import java.util.Objects;

import com.cristiannustes.http.HttpStatus;

public sealed abstract class HttpException extends RuntimeException
    permits BadRequestException, ConflictException, MethodNotAllowedException, NotFoundException,
            PayloadTooLargeException, UnauthorizedException, UnsupportedMediaTypeException {

    private static final long serialVersionUID = 1L;

    private final transient HttpStatus status;

    protected HttpException(HttpStatus status, String message) {
        Objects.requireNonNull(status, "status");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("an HTTP error must carry a message");
        }
        super(message);
        this.status = status;
    }

    public final HttpStatus status() {
        return status;
    }
}
