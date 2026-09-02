package com.cristiannustes.json;

public sealed interface JsonValue
    permits JsonArray, JsonBoolean, JsonNull, JsonNumber, JsonObject, JsonString {

    static JsonValue of(String value) {
        return value == null ? JsonNull.INSTANCE : new JsonString(value);
    }

    static JsonValue of(long value) {
        return new JsonNumber(value);
    }

    static JsonValue of(double value) {
        return new JsonNumber(value);
    }

    static JsonValue of(boolean value) {
        return JsonBoolean.of(value);
    }

    default String toJson() {
        return JsonWriter.write(this);
    }
}
