package com.cristiannustes.domain.user;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Gatherers;

import com.cristiannustes.domain.validation.Validator;
import com.cristiannustes.http.error.NotFoundException;
import com.cristiannustes.security.PasswordHasher;

public final class UserService {

    private static final Pattern LOGIN_FORMAT = Pattern.compile("[a-zA-Z0-9._-]+");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public UserService(UserRepository repository, PasswordHasher passwordHasher, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public UserView register(NewUser request) {
        validate(request.login(), request.password());
        Instant now = clock.instant();
        User user = new User(
            UUID.randomUUID().toString(),
            request.login(),
            passwordHasher.hash(request.password()),
            now,
            now);
        return repository.create(user).toView();
    }

    public UserView update(String id, UserUpdate request) {
        validate(request.login(), request.password());
        User existing = require(id);
        User updated = existing.withCredentials(
            request.login(),
            passwordHasher.hash(request.password()),
            clock.instant());
        return repository.update(updated).toView();
    }

    public UserView findById(String id) {
        return require(id).toView();
    }

    public void delete(String id) {
        if (!repository.deleteById(id)) {
            throw new NotFoundException("no user with id " + id);
        }
    }

    public Page<UserView> list(int page, int size) {
        List<UserView> items = repository.findAll().stream()
            .map(User::toView)
            .gather(Gatherers.windowFixed(size))
            .skip(page)
            .findFirst()
            .orElseGet(List::of);
        return new Page<>(items, page, size, repository.count());
    }

    private User require(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("no user with id " + id));
    }

    private static void validate(String login, String password) {
        Validator.create()
            .required("login", login)
            .length("login", login, 3, 64)
            .matches("login", login, LOGIN_FORMAT, "may only contain letters, digits, dots, dashes and underscores")
            .required("password", password)
            .length("password", password, MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH)
            .check();
    }
}
