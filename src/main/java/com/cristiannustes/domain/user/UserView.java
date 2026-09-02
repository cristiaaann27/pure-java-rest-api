package com.cristiannustes.domain.user;

import java.time.Instant;

public record UserView(String id, String login, Instant createdAt, Instant updatedAt) {
}
