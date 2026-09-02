package com.cristiannustes.http;

import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public record RequestContext(String requestId, Instant startedAt, String principal) {

    private static final ScopedValue<RequestContext> CURRENT = ScopedValue.newInstance();
    private static final RequestContext DETACHED = new RequestContext("-", Instant.EPOCH, null);

    public static RequestContext start() {
        return new RequestContext(newRequestId(), Instant.now(), null);
    }

    public static RequestContext current() {
        return CURRENT.orElse(DETACHED);
    }

    public static <X extends Throwable> void runWhere(RequestContext context, ScopedBlock<X> body) throws X {
        ScopedValue.where(CURRENT, context).call(() -> {
            body.run();
            return null;
        });
    }

    public RequestContext withPrincipal(String user) {
        return new RequestContext(requestId, startedAt, user);
    }

    public Optional<String> authenticatedUser() {
        return Optional.ofNullable(principal);
    }

    private static String newRequestId() {
        byte[] bytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @FunctionalInterface
    public interface ScopedBlock<X extends Throwable> {
        void run() throws X;
    }
}
