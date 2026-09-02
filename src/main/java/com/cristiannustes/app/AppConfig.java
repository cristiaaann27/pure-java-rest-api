package com.cristiannustes.app;

import java.util.Optional;

import com.cristiannustes.security.PasswordHasher;

public record AppConfig(
    int port,
    int backlog,
    String adminUser,
    Optional<String> adminPasswordHash,
    int maxBodyBytes,
    int shutdownGraceSeconds,
    int passwordIterations) {

    private static final int DEFAULT_PORT = 8000;
    private static final int DEFAULT_MAX_BODY_BYTES = 64 * 1024;
    private static final int DEFAULT_SHUTDOWN_GRACE_SECONDS = 5;

    public static AppConfig fromEnvironment() {
        return new AppConfig(
            intVariable("API_PORT", DEFAULT_PORT),
            intVariable("API_BACKLOG", 0),
            variable("API_ADMIN_USER").orElse("admin"),
            variable("API_ADMIN_PASSWORD_HASH"),
            intVariable("API_MAX_BODY_BYTES", DEFAULT_MAX_BODY_BYTES),
            intVariable("API_SHUTDOWN_GRACE_SECONDS", DEFAULT_SHUTDOWN_GRACE_SECONDS),
            intVariable("API_PASSWORD_ITERATIONS", PasswordHasher.DEFAULT_ITERATIONS));
    }

    private static Optional<String> variable(String name) {
        return Optional.ofNullable(System.getenv(name)).filter(value -> !value.isBlank());
    }

    private static int intVariable(String name, int fallback) {
        return variable(name)
            .map(value -> parse(name, value))
            .orElse(fallback);
    }

    private static int parse(String name, String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("%s must be a number but was '%s'".formatted(name, value));
        }
    }
}
