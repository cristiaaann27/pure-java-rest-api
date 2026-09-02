package com.cristiannustes.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.BasicAuthenticator;

public final class CredentialsAuthenticator extends BasicAuthenticator {

    private final Map<String, Boolean> verified = new ConcurrentHashMap<>();

    private final PasswordHasher passwordHasher;
    private final String expectedUser;
    private final String expectedPasswordHash;

    public CredentialsAuthenticator(String realm, String expectedUser, String expectedPasswordHash,
                                    PasswordHasher passwordHasher) {
        super(realm);
        this.expectedUser = expectedUser;
        this.expectedPasswordHash = expectedPasswordHash;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public boolean checkCredentials(String user, String password) {
        if (user == null || password == null) {
            return false;
        }
        if (!constantTimeEquals(expectedUser, user)) {
            return false;
        }
        String cacheKey = fingerprint(user, password);
        Boolean cached = verified.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        boolean valid = passwordHasher.matches(password, expectedPasswordHash);
        if (valid && verified.size() < 64) {
            verified.put(cacheKey, Boolean.TRUE);
        }
        return valid;
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String fingerprint(String user, String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest((user + '\0' + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }
}
