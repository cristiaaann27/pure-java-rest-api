package com.cristiannustes.domain.user;

import java.time.Instant;
import java.util.Objects;

public record User(String id, String login, String passwordHash, Instant createdAt, Instant updatedAt) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(login, "login");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public User withCredentials(String newLogin, String newPasswordHash, Instant modifiedAt) {
        return new User(id, newLogin, newPasswordHash, createdAt, modifiedAt);
    }

    public UserView toView() {
        return new UserView(id, login, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "User[id=%s, login=%s]".formatted(id, login);
    }
}
