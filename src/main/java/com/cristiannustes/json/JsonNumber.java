package com.cristiannustes.json;

public record JsonNumber(double value) implements JsonValue {

    public JsonNumber {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new JsonException("JSON cannot represent " + value);
        }
    }

    public int asInt() {
        return Math.toIntExact(asLong());
    }

    public long asLong() {
        if (value != Math.rint(value)) {
            throw new JsonException("expected an integer but got " + value);
        }
        return (long) value;
    }
}
