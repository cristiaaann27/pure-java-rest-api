package com.cristiannustes.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.LongSupplier;

import com.cristiannustes.http.Request;
import com.cristiannustes.http.ResponseEntity;
import com.cristiannustes.http.Router;
import com.cristiannustes.json.JsonObject;

public final class HealthApi {

    private final Instant startedAt;
    private final Clock clock;
    private final LongSupplier userCount;

    public HealthApi(Instant startedAt, Clock clock, LongSupplier userCount) {
        this.startedAt = startedAt;
        this.clock = clock;
        this.userCount = userCount;
    }

    public void registerOn(Router router) {
        router.get("/api/health", this::health);
    }

    private ResponseEntity<JsonObject> health(Request request) {
        JsonObject body = JsonObject.builder()
            .put("status", "UP")
            .put("uptimeSeconds", Duration.between(startedAt, clock.instant()).toSeconds())
            .put("users", userCount.getAsLong())
            .put("java", Runtime.version().toString())
            .build();
        return ResponseEntity.ok(body);
    }
}
