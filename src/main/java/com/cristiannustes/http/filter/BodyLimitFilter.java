package com.cristiannustes.http.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.cristiannustes.http.HttpStatus;
import com.cristiannustes.http.MediaType;
import com.cristiannustes.http.RequestContext;
import com.cristiannustes.json.JsonObject;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public final class BodyLimitFilter extends Filter {

    private final long maxBodyBytes;

    public BodyLimitFilter(long maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        long declared = declaredLength(exchange);
        if (declared > maxBodyBytes) {
            reject(exchange);
            return;
        }
        chain.doFilter(exchange);
    }

    private static long declaredLength(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst(MediaType.CONTENT_LENGTH);
        if (header == null) {
            return -1;
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void reject(HttpExchange exchange) throws IOException {
        JsonObject body = JsonObject.builder()
            .put("code", HttpStatus.PAYLOAD_TOO_LARGE.code())
            .put("status", HttpStatus.PAYLOAD_TOO_LARGE.reason())
            .put("message", "request body exceeds the limit of %d bytes".formatted(maxBodyBytes))
            .put("requestId", RequestContext.current().requestId())
            .build();
        byte[] payload = body.toJson().getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(MediaType.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        exchange.sendResponseHeaders(HttpStatus.PAYLOAD_TOO_LARGE.code(), payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
        exchange.close();
    }

    @Override
    public String description() {
        return "Rejects requests whose declared body size exceeds the configured limit";
    }
}
