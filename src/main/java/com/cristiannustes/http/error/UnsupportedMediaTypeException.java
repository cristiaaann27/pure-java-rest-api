package com.cristiannustes.http.error;

import com.cristiannustes.http.HttpStatus;

public final class UnsupportedMediaTypeException extends HttpException {

    private static final long serialVersionUID = 1L;

    public UnsupportedMediaTypeException(String contentType) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "content type '%s' is not supported, use application/json".formatted(contentType));
    }
}
