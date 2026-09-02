package com.cristiannustes.domain.user;

public record NewUser(String login, String password) {

    @Override
    public String toString() {
        return "NewUser[login=%s, password=***]".formatted(login);
    }
}
