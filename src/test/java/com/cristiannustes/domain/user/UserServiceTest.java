package com.cristiannustes.domain.user;

import static com.cristiannustes.testing.Assertions.assertEquals;
import static com.cristiannustes.testing.Assertions.assertFalse;
import static com.cristiannustes.testing.Assertions.assertThrows;
import static com.cristiannustes.testing.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.cristiannustes.data.user.InMemoryUserRepository;
import com.cristiannustes.http.error.ConflictException;
import com.cristiannustes.http.error.NotFoundException;
import com.cristiannustes.http.error.ValidationException;
import com.cristiannustes.security.PasswordHasher;
import com.cristiannustes.testing.Test;

public class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final String VALID_PASSWORD = "a-good-password";

    private final UserRepository repository = new InMemoryUserRepository();
    private final PasswordHasher hasher = new PasswordHasher(1_000);
    private final UserService service =
        new UserService(repository, hasher, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    public void registersAUserAndStoresOnlyTheHash() {
        UserView created = service.register(new NewUser("marcin", VALID_PASSWORD));

        assertEquals("marcin", created.login());
        assertEquals(NOW, created.createdAt());

        User stored = repository.findById(created.id()).orElseThrow();
        assertFalse(VALID_PASSWORD.equals(stored.passwordHash()), "the password must never be stored as typed");
        assertTrue(hasher.matches(VALID_PASSWORD, stored.passwordHash()), "the stored hash must verify");
    }

    @Test
    public void keepsTheHashOutOfToString() {
        service.register(new NewUser("marcin", VALID_PASSWORD));
        User stored = repository.findByLogin("marcin").orElseThrow();

        assertFalse(stored.toString().contains(stored.passwordHash()), "toString must not leak the hash");
    }

    @Test
    public void refusesADuplicateLogin() {
        service.register(new NewUser("marcin", VALID_PASSWORD));

        assertThrows(ConflictException.class, () -> service.register(new NewUser("marcin", "another-password")));
    }

    @Test
    public void refusesAnInvalidPayload() {
        ValidationException failure =
            assertThrows(ValidationException.class, () -> service.register(new NewUser("x", "short")));

        assertEquals(2, failure.errors().size());
    }

    @Test
    public void refusesALoginWithUnexpectedCharacters() {
        assertThrows(ValidationException.class, () -> service.register(new NewUser("marcin nowak", VALID_PASSWORD)));
    }

    @Test
    public void readsAUserBackById() {
        UserView created = service.register(new NewUser("marcin", VALID_PASSWORD));

        assertEquals(created, service.findById(created.id()));
    }

    @Test
    public void failsWithNotFoundForAnUnknownId() {
        assertThrows(NotFoundException.class, () -> service.findById("does-not-exist"));
        assertThrows(NotFoundException.class, () -> service.delete("does-not-exist"));
    }

    @Test
    public void replacesTheCredentialsOfAnExistingUser() {
        UserView created = service.register(new NewUser("marcin", VALID_PASSWORD));

        UserView updated = service.update(created.id(), new UserUpdate("marcin.nowak", "a-new-password"));

        assertEquals(created.id(), updated.id());
        assertEquals("marcin.nowak", updated.login());
        assertTrue(repository.findByLogin("marcin").isEmpty(), "the old login must be released");
        assertTrue(hasher.matches("a-new-password", repository.findById(created.id()).orElseThrow().passwordHash()),
            "the new password must be the one stored");
    }

    @Test
    public void refusesToStealAnotherUsersLogin() {
        service.register(new NewUser("marcin", VALID_PASSWORD));
        UserView second = service.register(new NewUser("tomasz", VALID_PASSWORD));

        assertThrows(ConflictException.class, () -> service.update(second.id(), new UserUpdate("marcin", "whatever1")));
    }

    @Test
    public void deletesAUser() {
        UserView created = service.register(new NewUser("marcin", VALID_PASSWORD));

        service.delete(created.id());

        assertEquals(0L, repository.count());
        assertThrows(NotFoundException.class, () -> service.findById(created.id()));
    }

    @Test
    public void slicesTheCollectionIntoPages() {
        for (int i = 0; i < 5; i++) {
            service.register(new NewUser("user-" + i, VALID_PASSWORD));
        }

        Page<UserView> first = service.list(0, 2);
        Page<UserView> last = service.list(2, 2);
        Page<UserView> beyond = service.list(9, 2);

        assertEquals(List.of("user-0", "user-1"), logins(first));
        assertEquals(List.of("user-4"), logins(last));
        assertEquals(List.of(), logins(beyond));
        assertEquals(5L, first.totalItems());
        assertEquals(3L, first.totalPages());
    }

    private static List<String> logins(Page<UserView> page) {
        return page.items().stream().map(UserView::login).toList();
    }
}
