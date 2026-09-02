package com.cristiannustes.json;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser {

    private static final int MAX_DEPTH = 64;

    private final String input;
    private int position;
    private int depth;

    private JsonParser(String input) {
        this.input = input;
    }

    public static JsonValue parse(String text) {
        if (text == null || text.isBlank()) {
            throw new JsonException("empty JSON document");
        }
        JsonParser parser = new JsonParser(text);
        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.position < text.length()) {
            throw parser.error("unexpected trailing content");
        }
        return value;
    }

    public static JsonValue parse(InputStream stream) throws IOException {
        return parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }

    public static JsonObject parseObject(String text) {
        JsonValue value = parse(text);
        if (value instanceof JsonObject object) {
            return object;
        }
        throw new JsonException("expected a JSON object at the root of the document");
    }

    private JsonValue parseValue() {
        skipWhitespace();
        char current = peek();
        return switch (current) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> new JsonString(parseString());
            case 't' -> literal("true", JsonBoolean.TRUE);
            case 'f' -> literal("false", JsonBoolean.FALSE);
            case 'n' -> literal("null", JsonNull.INSTANCE);
            default -> parseNumber();
        };
    }

    private JsonObject parseObject() {
        enter();
        expect('{');
        Map<String, JsonValue> fields = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            position++;
            leave();
            return new JsonObject(fields);
        }
        while (true) {
            skipWhitespace();
            String name = parseString();
            skipWhitespace();
            expect(':');
            JsonValue value = parseValue();
            if (fields.put(name, value) != null) {
                throw error("duplicate field '" + name + "'");
            }
            skipWhitespace();
            char separator = next();
            if (separator == '}') {
                leave();
                return new JsonObject(fields);
            }
            if (separator != ',') {
                throw error("expected ',' or '}' but found '" + separator + "'");
            }
        }
    }

    private JsonArray parseArray() {
        enter();
        expect('[');
        List<JsonValue> items = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            position++;
            leave();
            return new JsonArray(items);
        }
        while (true) {
            items.add(parseValue());
            skipWhitespace();
            char separator = next();
            if (separator == ']') {
                leave();
                return new JsonArray(items);
            }
            if (separator != ',') {
                throw error("expected ',' or ']' but found '" + separator + "'");
            }
        }
    }

    private String parseString() {
        expect('"');
        StringBuilder text = new StringBuilder();
        while (true) {
            char current = next();
            switch (current) {
                case '"' -> {
                    return text.toString();
                }
                case '\\' -> text.append(parseEscape());
                default -> {
                    if (current < 0x20) {
                        throw error("unescaped control character U+%04X in string".formatted((int) current));
                    }
                    text.append(current);
                }
            }
        }
    }

    private char parseEscape() {
        char code = next();
        return switch (code) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> parseUnicodeEscape();
            default -> throw error("invalid escape sequence '\\" + code + "'");
        };
    }

    private char parseUnicodeEscape() {
        if (position + 4 > input.length()) {
            throw error("truncated unicode escape");
        }
        String digits = input.substring(position, position + 4);
        position += 4;
        try {
            return (char) Integer.parseInt(digits, 16);
        } catch (NumberFormatException e) {
            throw error("invalid unicode escape '\\u" + digits + "'");
        }
    }

    private JsonNumber parseNumber() {
        int start = position;
        if (peek() == '-') {
            position++;
        }
        readDigits();
        if (position < input.length() && input.charAt(position) == '.') {
            position++;
            readDigits();
        }
        if (position < input.length() && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {
            position++;
            if (position < input.length() && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
                position++;
            }
            readDigits();
        }
        String literal = input.substring(start, position);
        try {
            return new JsonNumber(Double.parseDouble(literal));
        } catch (NumberFormatException e) {
            throw error("invalid number '" + literal + "'");
        }
    }

    private void readDigits() {
        int start = position;
        while (position < input.length() && Character.isDigit(input.charAt(position))) {
            position++;
        }
        if (position == start) {
            throw error("expected a digit");
        }
    }

    private JsonValue literal(String expected, JsonValue value) {
        if (!input.startsWith(expected, position)) {
            throw error("expected '" + expected + "'");
        }
        position += expected.length();
        return value;
    }

    private void enter() {
        if (++depth > MAX_DEPTH) {
            throw error("document nested deeper than " + MAX_DEPTH + " levels");
        }
    }

    private void leave() {
        depth--;
    }

    private void skipWhitespace() {
        while (position < input.length()) {
            char current = input.charAt(position);
            if (current != ' ' && current != '\t' && current != '\n' && current != '\r') {
                return;
            }
            position++;
        }
    }

    private char peek() {
        if (position >= input.length()) {
            throw error("unexpected end of document");
        }
        return input.charAt(position);
    }

    private char next() {
        char current = peek();
        position++;
        return current;
    }

    private void expect(char expected) {
        char current = next();
        if (current != expected) {
            throw error("expected '" + expected + "' but found '" + current + "'");
        }
    }

    private JsonException error(String message) {
        return new JsonException("%s (at offset %d)".formatted(message, position));
    }
}
