package com.cristiannustes.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    public static final int DEFAULT_ITERATIONS = 210_000;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final String PREFIX = "pbkdf2-sha512";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String SEPARATOR = "$";

    private final SecureRandom random = new SecureRandom();
    private final int iterations;

    public PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    public PasswordHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        this.iterations = iterations;
    }

    public String hash(String plainText) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] digest = derive(plainText.toCharArray(), salt, iterations);
        return String.join(SEPARATOR,
            PREFIX,
            Integer.toString(iterations),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(digest));
    }

    public boolean matches(String plainText, String encoded) {
        if (plainText == null || encoded == null) {
            return false;
        }
        String[] parts = encoded.split("\\" + SEPARATOR);
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int storedIterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(plainText.toCharArray(), salt, storedIterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("%s is required but unavailable".formatted(ALGORITHM), e);
        } finally {
            spec.clearPassword();
        }
    }

    public static String randomPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
