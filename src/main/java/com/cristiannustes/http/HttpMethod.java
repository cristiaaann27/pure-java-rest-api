package com.cristiannustes.http;

import java.util.Optional;

public enum HttpMethod {

    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS;

    public static Optional<HttpMethod> parse(String raw) {
        for (HttpMethod method : values()) {
            if (method.name().equals(raw)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }
}
