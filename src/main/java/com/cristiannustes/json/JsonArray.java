package com.cristiannustes.json;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public record JsonArray(List<JsonValue> items) implements JsonValue {

    public JsonArray {
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }

    public static JsonArray empty() {
        return new JsonArray(List.of());
    }

    public static JsonArray of(JsonValue... values) {
        return new JsonArray(List.of(values));
    }

    public static Collector<JsonValue, ?, JsonArray> collector() {
        return Collectors.collectingAndThen(Collectors.toList(), JsonArray::new);
    }

    public int size() {
        return items.size();
    }
}
