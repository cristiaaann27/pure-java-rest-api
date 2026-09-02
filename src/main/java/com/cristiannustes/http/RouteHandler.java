package com.cristiannustes.http;

import com.cristiannustes.json.JsonValue;

@FunctionalInterface
public interface RouteHandler {

    ResponseEntity<? extends JsonValue> handle(Request request);
}
