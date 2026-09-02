package com.cristiannustes.http.error;

import java.io.UncheckedIOException;
import java.util.List;

import com.cristiannustes.http.HttpStatus;
import com.cristiannustes.http.MediaType;
import com.cristiannustes.http.RequestContext;
import com.cristiannustes.http.ResponseEntity;
import com.cristiannustes.json.JsonArray;
import com.cristiannustes.json.JsonObject;
import com.cristiannustes.json.JsonValue;

public final class ErrorMapper {

    private static final System.Logger LOGGER = System.getLogger(ErrorMapper.class.getName());

    public ResponseEntity<JsonObject> toResponse(Throwable failure) {
        return switch (failure) {
            case ValidationException e -> response(e.status(), e.getMessage(), e.errors());
            case MethodNotAllowedException e ->
                response(e.status(), e.getMessage(), List.of()).withHeader(MediaType.ALLOW, e.allowHeader());
            case HttpException e -> response(e.status(), e.getMessage(), List.of());
            case UncheckedIOException e -> {
                LOGGER.log(System.Logger.Level.DEBUG, "I/O failure while serving the request", e);
                yield internalError(e);
            }
            default -> internalError(failure);
        };
    }

    private ResponseEntity<JsonObject> internalError(Throwable failure) {
        LOGGER.log(System.Logger.Level.ERROR,
            "unhandled failure while serving request " + RequestContext.current().requestId(), failure);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "the server could not handle the request", List.of());
    }

    private ResponseEntity<JsonObject> response(HttpStatus status, String message, List<FieldError> errors) {
        JsonObject.Builder body = JsonObject.builder()
            .put("code", status.code())
            .put("status", status.reason())
            .put("message", message)
            .put("requestId", RequestContext.current().requestId());
        if (!errors.isEmpty()) {
            body.put("errors", errors.stream()
                .<JsonValue>map(error -> JsonObject.builder()
                    .put("field", error.field())
                    .put("message", error.message())
                    .build())
                .collect(JsonArray.collector()));
        }
        return ResponseEntity.status(status, body.build());
    }
}
