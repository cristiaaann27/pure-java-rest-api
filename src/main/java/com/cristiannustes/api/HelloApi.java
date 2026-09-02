package com.cristiannustes.api;

import com.cristiannustes.http.Request;
import com.cristiannustes.http.RequestContext;
import com.cristiannustes.http.ResponseEntity;
import com.cristiannustes.http.Router;
import com.cristiannustes.json.JsonObject;

public final class HelloApi {

    private static final String ANONYMOUS = "Anonymous";

    public void registerOn(Router router) {
        router.get("/api/hello", this::hello);
    }

    private ResponseEntity<JsonObject> hello(Request request) {
        String name = request.query()
            .first("name")
            .filter(value -> !value.isBlank())
            .or(() -> RequestContext.current().authenticatedUser())
            .orElse(ANONYMOUS);
        JsonObject body = JsonObject.builder()
            .put("message", "Hello %s!".formatted(name))
            .build();
        return ResponseEntity.ok(body);
    }
}
