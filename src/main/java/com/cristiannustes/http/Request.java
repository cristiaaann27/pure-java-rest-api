package com.cristiannustes.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import com.cristiannustes.http.error.BadRequestException;
import com.cristiannustes.http.error.PayloadTooLargeException;
import com.cristiannustes.http.error.UnsupportedMediaTypeException;
import com.cristiannustes.json.JsonException;
import com.cristiannustes.json.JsonObject;
import com.cristiannustes.json.JsonParser;
import com.sun.net.httpserver.HttpExchange;

public final class Request {

    private final HttpExchange exchange;
    private final HttpMethod method;
    private final Map<String, String> pathVariables;
    private final QueryParams queryParams;
    private final long maxBodyBytes;

    Request(HttpExchange exchange, HttpMethod method, Map<String, String> pathVariables, long maxBodyBytes) {
        this.exchange = exchange;
        this.method = method;
        this.pathVariables = Map.copyOf(pathVariables);
        this.queryParams = QueryParams.parse(exchange.getRequestURI().getRawQuery());
        this.maxBodyBytes = maxBodyBytes;
    }

    public HttpMethod method() {
        return method;
    }

    public URI uri() {
        return exchange.getRequestURI();
    }

    public String path() {
        return exchange.getRequestURI().getPath();
    }

    public QueryParams query() {
        return queryParams;
    }

    public String pathVariable(String name) {
        String value = pathVariables.get(name);
        if (value == null) {
            throw new IllegalArgumentException("route does not declare a '%s' variable".formatted(name));
        }
        return value;
    }

    public Optional<String> header(String name) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst(name));
    }

    public byte[] body() {
        try (InputStream stream = exchange.getRequestBody()) {
            byte[] bytes = stream.readNBytes(Math.toIntExact(maxBodyBytes) + 1);
            if (bytes.length > maxBodyBytes) {
                throw new PayloadTooLargeException(maxBodyBytes);
            }
            return bytes;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String bodyAsText() {
        return new String(body(), StandardCharsets.UTF_8);
    }

    public JsonObject jsonBody() {
        String contentType = header(MediaType.CONTENT_TYPE).orElse(null);
        if (contentType != null && !MediaType.isJson(contentType)) {
            throw new UnsupportedMediaTypeException(contentType);
        }
        try {
            return JsonParser.parseObject(bodyAsText());
        } catch (JsonException e) {
            throw new BadRequestException("malformed JSON body: " + e.getMessage());
        }
    }
}
