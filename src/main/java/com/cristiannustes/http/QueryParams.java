package com.cristiannustes.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.cristiannustes.http.error.BadRequestException;

public record QueryParams(Map<String, List<String>> values) {

    private static final QueryParams EMPTY = new QueryParams(Map.of());

    public QueryParams {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static QueryParams parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return EMPTY;
        }
        Map<String, List<String>> parsed = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int separator = pair.indexOf('=');
            String name = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            parsed.computeIfAbsent(decode(name), key -> new ArrayList<>()).add(decode(value));
        }
        return new QueryParams(parsed);
    }

    public Optional<String> first(String name) {
        List<String> found = values.get(name);
        return found == null || found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    public String firstOrDefault(String name, String fallback) {
        return first(name).filter(value -> !value.isEmpty()).orElse(fallback);
    }

    public int intOrDefault(String name, int fallback, int min, int max) {
        Optional<String> raw = first(name).filter(value -> !value.isEmpty());
        if (raw.isEmpty()) {
            return fallback;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(raw.get());
        } catch (NumberFormatException e) {
            throw new BadRequestException("query parameter '%s' must be a number".formatted(name));
        }
        if (parsed < min || parsed > max) {
            throw new BadRequestException("query parameter '%s' must be between %d and %d".formatted(name, min, max));
        }
        return parsed;
    }

    private static String decode(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }
}
