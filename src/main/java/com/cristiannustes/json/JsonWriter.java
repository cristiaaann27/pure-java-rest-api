package com.cristiannustes.json;

import java.util.List;
import java.util.Map;

public final class JsonWriter {

    private JsonWriter() {
    }

    public static String write(JsonValue value) {
        StringBuilder out = new StringBuilder(128);
        writeValue(value, out);
        return out.toString();
    }

    private static void writeValue(JsonValue value, StringBuilder out) {
        switch (value) {
            case JsonNull ignored -> out.append("null");
            case JsonBoolean(boolean flag) -> out.append(flag);
            case JsonNumber(double number) -> writeNumber(number, out);
            case JsonString(String text) -> writeString(text, out);
            case JsonArray(List<JsonValue> items) -> writeArray(items, out);
            case JsonObject(Map<String, JsonValue> fields) -> writeObject(fields, out);
        }
    }

    private static void writeArray(List<JsonValue> items, StringBuilder out) {
        out.append('[');
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            writeValue(items.get(i), out);
        }
        out.append(']');
    }

    private static void writeObject(Map<String, JsonValue> fields, StringBuilder out) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, JsonValue> field : fields.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(field.getKey(), out);
            out.append(':');
            writeValue(field.getValue(), out);
        }
        out.append('}');
    }

    private static void writeNumber(double number, StringBuilder out) {
        if (number == Math.rint(number) && Math.abs(number) < 1e15) {
            out.append((long) number);
        } else {
            out.append(number);
        }
    }

    private static void writeString(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            switch (current) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (current < 0x20) {
                        out.append("\\u%04x".formatted((int) current));
                    } else {
                        out.append(current);
                    }
                }
            }
        }
        out.append('"');
    }
}
