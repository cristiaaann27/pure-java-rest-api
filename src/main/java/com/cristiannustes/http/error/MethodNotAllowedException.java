package com.cristiannustes.http.error;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cristiannustes.http.HttpMethod;
import com.cristiannustes.http.HttpStatus;

public final class MethodNotAllowedException extends HttpException {

    private static final long serialVersionUID = 1L;

    private final transient List<HttpMethod> allowed;

    public MethodNotAllowedException(String method, String path, Set<HttpMethod> allowed) {
        super(HttpStatus.METHOD_NOT_ALLOWED, "method %s is not allowed for %s".formatted(method, path));
        this.allowed = allowed.stream().sorted().toList();
    }

    public String allowHeader() {
        return allowed.stream().map(Enum::name).collect(Collectors.joining(", "));
    }
}
