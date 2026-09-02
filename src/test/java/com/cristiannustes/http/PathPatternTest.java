package com.cristiannustes.http;

import static com.cristiannustes.testing.Assertions.assertEquals;
import static com.cristiannustes.testing.Assertions.assertThrows;
import static com.cristiannustes.testing.Assertions.assertTrue;

import java.util.Map;

import com.cristiannustes.testing.Test;

public class PathPatternTest {

    @Test
    public void matchesALiteralPath() {
        PathPattern pattern = PathPattern.of("/api/users");

        assertEquals(Map.of(), pattern.match("/api/users").orElseThrow());
        assertTrue(pattern.match("/api/users/42").isEmpty(), "a longer path must not match");
        assertTrue(pattern.match("/api").isEmpty(), "a shorter path must not match");
    }

    @Test
    public void extractsPathVariables() {
        PathPattern pattern = PathPattern.of("/api/users/{id}/roles/{role}");

        Map<String, String> variables = pattern.match("/api/users/abc-123/roles/admin").orElseThrow();

        assertEquals("abc-123", variables.get("id"));
        assertEquals("admin", variables.get("role"));
    }

    @Test
    public void doesNotLetAVariableSwallowASlash() {
        PathPattern pattern = PathPattern.of("/api/users/{id}");

        assertTrue(pattern.match("/api/users/a/b").isEmpty(), "{id} must match a single segment");
    }

    @Test
    public void toleratesATrailingSlash() {
        assertTrue(PathPattern.of("/api/users").match("/api/users/").isPresent(), "trailing slash is not a 404");
    }

    @Test
    public void treatsRegexCharactersInTheTemplateAsLiterals() {
        PathPattern pattern = PathPattern.of("/api/v1.0/items");

        assertTrue(pattern.match("/api/v1.0/items").isPresent(), "the dot must match itself");
        assertTrue(pattern.match("/api/v1x0/items").isEmpty(), "the dot must not act as a wildcard");
    }

    @Test
    public void rejectsATemplateWithoutALeadingSlash() {
        assertThrows(IllegalArgumentException.class, () -> PathPattern.of("api/users"));
    }
}
