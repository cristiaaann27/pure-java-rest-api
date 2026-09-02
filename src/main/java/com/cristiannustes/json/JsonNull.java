package com.cristiannustes.json;

public record JsonNull() implements JsonValue {

    public static final JsonNull INSTANCE = new JsonNull();
}
