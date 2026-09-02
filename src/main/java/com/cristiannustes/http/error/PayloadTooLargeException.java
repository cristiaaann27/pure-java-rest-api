package com.cristiannustes.http.error;

import com.cristiannustes.http.HttpStatus;

public final class PayloadTooLargeException extends HttpException {

    private static final long serialVersionUID = 1L;

    public PayloadTooLargeException(long limitBytes) {
        super(HttpStatus.PAYLOAD_TOO_LARGE, "request body exceeds the limit of %d bytes".formatted(limitBytes));
    }
}
