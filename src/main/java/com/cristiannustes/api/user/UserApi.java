package com.cristiannustes.api.user;

import java.util.Objects;

import com.cristiannustes.domain.user.NewUser;
import com.cristiannustes.domain.user.Page;
import com.cristiannustes.domain.user.UserService;
import com.cristiannustes.domain.user.UserUpdate;
import com.cristiannustes.domain.user.UserView;
import com.cristiannustes.http.Request;
import com.cristiannustes.http.ResponseEntity;
import com.cristiannustes.http.Router;
import com.cristiannustes.json.JsonObject;
import com.cristiannustes.json.JsonValue;

public final class UserApi {

    private static final String COLLECTION = "/api/users";
    private static final String ITEM = "/api/users/{id}";

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;

    public UserApi(UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService");
    }

    public void registerOn(Router router) {
        router
            .post(COLLECTION, this::create)
            .post("/api/users/register", this::create)
            .get(COLLECTION, this::list)
            .get(ITEM, this::findById)
            .put(ITEM, this::update)
            .delete(ITEM, this::delete);
    }

    private ResponseEntity<JsonObject> create(Request request) {
        NewUser newUser = UserJson.toNewUser(request.jsonBody());
        UserView created = userService.register(newUser);
        return ResponseEntity.created(COLLECTION + "/" + created.id(), UserJson.toJson(created));
    }

    private ResponseEntity<JsonObject> list(Request request) {
        int page = request.query().intOrDefault("page", 0, 0, Integer.MAX_VALUE);
        int size = request.query().intOrDefault("size", DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE);
        Page<UserView> users = userService.list(page, size);
        return ResponseEntity.ok(UserJson.toJson(users));
    }

    private ResponseEntity<JsonObject> findById(Request request) {
        UserView user = userService.findById(request.pathVariable("id"));
        return ResponseEntity.ok(UserJson.toJson(user));
    }

    private ResponseEntity<JsonObject> update(Request request) {
        UserUpdate update = UserJson.toUserUpdate(request.jsonBody());
        UserView updated = userService.update(request.pathVariable("id"), update);
        return ResponseEntity.ok(UserJson.toJson(updated));
    }

    private ResponseEntity<JsonValue> delete(Request request) {
        userService.delete(request.pathVariable("id"));
        return ResponseEntity.noContent();
    }
}
