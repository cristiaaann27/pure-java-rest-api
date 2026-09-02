package com.cristiannustes.security;

import static com.cristiannustes.testing.Assertions.assertEquals;
import static com.cristiannustes.testing.Assertions.assertFalse;
import static com.cristiannustes.testing.Assertions.assertTrue;

import com.cristiannustes.testing.Test;

public class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(1_000);

    @Test
    public void neverStoresThePasswordItself() {
        String encoded = hasher.hash("correct horse battery staple");

        assertFalse(encoded.contains("correct horse"), "the hash must not contain the password");
        assertTrue(encoded.startsWith("pbkdf2-sha512$1000$"), encoded);
    }

    @Test
    public void producesADifferentHashForTheSamePassword() {
        String first = hasher.hash("same-password");
        String second = hasher.hash("same-password");

        assertFalse(first.equals(second), "a random salt must make the two hashes differ");
        assertTrue(hasher.matches("same-password", first), "the first hash must still verify");
        assertTrue(hasher.matches("same-password", second), "the second hash must still verify");
    }

    @Test
    public void rejectsAWrongPassword() {
        String encoded = hasher.hash("right");

        assertFalse(hasher.matches("wrong", encoded), "a different password must not verify");
        assertFalse(hasher.matches(null, encoded), "a null password must not verify");
    }

    @Test
    public void treatsACorruptHashAsAFailedLoginRatherThanAnError() {
        assertFalse(hasher.matches("anything", "not-a-hash"), "garbage must not authenticate");
        assertFalse(hasher.matches("anything", "pbkdf2-sha512$x$y$z"), "garbage must not authenticate");
        assertFalse(hasher.matches("anything", ""), "an empty hash must not authenticate");
    }

    @Test
    public void keepsTheCostFactorInsideTheHashSoItCanBeRaisedLater() {
        String cheap = new PasswordHasher(1_000).hash("secret");

        assertEquals("1000", cheap.split("\\$")[1]);
        assertTrue(new PasswordHasher(50_000).matches("secret", cheap),
            "an old hash must verify even after the cost is raised");
    }
}
