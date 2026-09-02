package com.cristiannustes.testing;

import java.util.Objects;

public final class Assertions {

    private Assertions() {
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionFailure(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, "values differ");
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionFailure("%s%n  expected: %s%n  actual:   %s"
                .formatted(message, describe(expected), describe(actual)));
        }
    }

    public static void assertNotNull(Object value, String message) {
        assertTrue(value != null, message);
    }

    public static void assertNull(Object value, String message) {
        assertTrue(value == null, message);
    }

    public static <T extends Throwable> T assertThrows(Class<T> expected, ThrowingBlock block) {
        try {
            block.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) {
                return expected.cast(thrown);
            }
            throw new AssertionFailure("expected %s but got %s: %s"
                .formatted(expected.getSimpleName(), thrown.getClass().getSimpleName(), thrown.getMessage()));
        }
        throw new AssertionFailure("expected %s but nothing was thrown".formatted(expected.getSimpleName()));
    }

    private static String describe(Object value) {
        return value instanceof String text ? '"' + text + '"' : String.valueOf(value);
    }

    @FunctionalInterface
    public interface ThrowingBlock {
        void run() throws Exception;
    }
}
