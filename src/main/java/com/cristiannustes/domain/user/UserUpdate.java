package com.cristiannustes.domain.user;

public record UserUpdate(String login, String password) {

    @Override
    public String toString() {
        return "UserUpdate[login=%s, password=***]".formatted(login);
    }
}
