package com.cristiannustes.http;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.cristiannustes.json.JsonValue;

public record ResponseEntity<T extends JsonValue>(HttpStatus status, Map<String, String> headers, T body) {

    public ResponseEntity {
        Objects.requireNonNull(status, "status");
        headers = Map.copyOf(headers);
    }

    public static <T extends JsonValue> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>(HttpStatus.OK, Map.of(), body);
    }

    public static <T extends JsonValue> ResponseEntity<T> created(String location, T body) {
        return new ResponseEntity<>(HttpStatus.CREATED, Map.of(MediaType.LOCATION, location), body);
    }

    public static ResponseEntity<JsonValue> noContent() {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT, Map.of(), null);
    }

    public static <T extends JsonValue> ResponseEntity<T> status(HttpStatus status, T body) {
        return new ResponseEntity<>(status, Map.of(), body);
    }

    public ResponseEntity<T> withHeader(String name, String value) {
        Map<String, String> merged = new LinkedHashMap<>(headers);
        merged.put(name, value);
        return new ResponseEntity<>(status, merged, body);
    }
}
