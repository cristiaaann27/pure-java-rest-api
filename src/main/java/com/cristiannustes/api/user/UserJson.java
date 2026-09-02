package com.cristiannustes.api.user;

import java.time.Instant;
import java.util.Set;
import java.util.function.Supplier;

import com.cristiannustes.domain.user.NewUser;
import com.cristiannustes.domain.user.Page;
import com.cristiannustes.domain.user.UserUpdate;
import com.cristiannustes.domain.user.UserView;
import com.cristiannustes.http.error.BadRequestException;
import com.cristiannustes.json.JsonArray;
import com.cristiannustes.json.JsonException;
import com.cristiannustes.json.JsonObject;
import com.cristiannustes.json.JsonValue;

final class UserJson {

    private static final Set<String> CREDENTIAL_FIELDS = Set.of("login", "password");

    private UserJson() {
    }

    static NewUser toNewUser(JsonObject body) {
        return read(() -> {
            body.rejectUnknownFields(CREDENTIAL_FIELDS);
            return new NewUser(text(body, "login"), text(body, "password"));
        });
    }

    static UserUpdate toUserUpdate(JsonObject body) {
        return read(() -> {
            body.rejectUnknownFields(CREDENTIAL_FIELDS);
            return new UserUpdate(text(body, "login"), text(body, "password"));
        });
    }

    static JsonObject toJson(UserView user) {
        return JsonObject.builder()
            .put("id", user.id())
            .put("login", user.login())
            .put("createdAt", format(user.createdAt()))
            .put("updatedAt", format(user.updatedAt()))
            .build();
    }

    static JsonObject toJson(Page<UserView> page) {
        JsonArray items = page.items().stream()
            .<JsonValue>map(UserJson::toJson)
            .collect(JsonArray.collector());
        return JsonObject.builder()
            .put("items", items)
            .put("page", page.page())
            .put("size", page.size())
            .put("totalItems", page.totalItems())
            .put("totalPages", page.totalPages())
            .build();
    }

    private static String text(JsonObject body, String field) {
        return body.optionalString(field).orElse(null);
    }

    private static <T> T read(Supplier<T> reader) {
        try {
            return reader.get();
        } catch (JsonException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private static String format(Instant instant) {
        return instant.toString();
    }
}
