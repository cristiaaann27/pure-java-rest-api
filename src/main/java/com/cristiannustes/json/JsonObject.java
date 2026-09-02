package com.cristiannustes.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record JsonObject(Map<String, JsonValue> fields) implements JsonValue {

    public JsonObject {
        Objects.requireNonNull(fields, "fields");
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static JsonObject empty() {
        return new JsonObject(Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean has(String name) {
        return fields.containsKey(name) && !(fields.get(name) instanceof JsonNull);
    }

    public Optional<JsonValue> find(String name) {
        JsonValue value = fields.get(name);
        return value == null || value instanceof JsonNull ? Optional.empty() : Optional.of(value);
    }

    public String requireString(String name) {
        return optionalString(name).orElseThrow(() -> missing(name));
    }

    public Optional<String> optionalString(String name) {
        return find(name).map(value -> switch (value) {
            case JsonString(String text) -> text;
            default -> throw wrongType(name, "a string", value);
        });
    }

    public Optional<Long> optionalLong(String name) {
        return find(name).map(value -> switch (value) {
            case JsonNumber number -> number.asLong();
            default -> throw wrongType(name, "a number", value);
        });
    }

    public JsonObject requireObject(String name) {
        JsonValue value = find(name).orElseThrow(() -> missing(name));
        return value instanceof JsonObject object ? object : rejected(name, "an object", value);
    }

    public JsonArray requireArray(String name) {
        JsonValue value = find(name).orElseThrow(() -> missing(name));
        return value instanceof JsonArray array ? array : rejected(name, "an array", value);
    }

    public void rejectUnknownFields(Set<String> known) {
        for (String name : fields.keySet()) {
            if (!known.contains(name)) {
                throw new JsonException("unknown field '%s', expected one of %s".formatted(name, known));
            }
        }
    }

    private static JsonException missing(String name) {
        return new JsonException("missing required field '%s'".formatted(name));
    }

    private static JsonException wrongType(String name, String expected, JsonValue actual) {
        return new JsonException("field '%s' must be %s but was %s"
            .formatted(name, expected, actual.getClass().getSimpleName()));
    }

    private static <T> T rejected(String name, String expected, JsonValue actual) {
        throw wrongType(name, expected, actual);
    }

    public static final class Builder {

        private final Map<String, JsonValue> fields = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder put(String name, JsonValue value) {
            fields.put(Objects.requireNonNull(name, "name"), value == null ? JsonNull.INSTANCE : value);
            return this;
        }

        public Builder put(String name, String value) {
            return put(name, JsonValue.of(value));
        }

        public Builder put(String name, long value) {
            return put(name, JsonValue.of(value));
        }

        public Builder put(String name, boolean value) {
            return put(name, JsonValue.of(value));
        }

        public JsonObject build() {
            return new JsonObject(fields);
        }
    }
}
