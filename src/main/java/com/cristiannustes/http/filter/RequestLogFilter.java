package com.cristiannustes.http.filter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.cristiannustes.http.MediaType;
import com.cristiannustes.http.RequestContext;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

public final class RequestLogFilter extends Filter {

    private static final System.Logger LOGGER = System.getLogger("com.cristiannustes.http.access");

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        RequestContext context = RequestContext.start();
        exchange.getResponseHeaders().set(MediaType.REQUEST_ID, context.requestId());

        try {
            RequestContext.runWhere(context, () -> chain.doFilter(exchange));
        } finally {
            log(exchange, context);
        }
    }

    private static void log(HttpExchange exchange, RequestContext context) {
        long millis = Duration.between(context.startedAt(), Instant.now()).toMillis();
        int status = exchange.getResponseCode();
        System.Logger.Level level = status >= 500 ? System.Logger.Level.WARNING : System.Logger.Level.INFO;
        LOGGER.log(level, "%s %s %s %s -> %d (%d ms)".formatted(
            context.requestId(),
            caller(exchange),
            exchange.getRequestMethod(),
            exchange.getRequestURI(),
            status,
            millis));
    }

    private static String caller(HttpExchange exchange) {
        return Optional.ofNullable(exchange.getPrincipal())
            .map(HttpPrincipal::getUsername)
            .orElse("-");
    }

    @Override
    public String description() {
        return "Assigns a request id, binds the request context and logs the outcome";
    }
}
