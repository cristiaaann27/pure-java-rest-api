package com.cristiannustes.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.cristiannustes.http.error.ErrorMapper;
import com.cristiannustes.http.error.MethodNotAllowedException;
import com.cristiannustes.http.error.NotFoundException;
import com.cristiannustes.json.JsonValue;
import com.cristiannustes.json.JsonWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;

public final class Router implements HttpHandler {

    private final List<Route> routes = new ArrayList<>();
    private final ErrorMapper errorMapper = new ErrorMapper();
    private final long maxBodyBytes;

    public Router(long maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    public Router get(String template, RouteHandler handler) {
        return route(HttpMethod.GET, template, handler);
    }

    public Router post(String template, RouteHandler handler) {
        return route(HttpMethod.POST, template, handler);
    }

    public Router put(String template, RouteHandler handler) {
        return route(HttpMethod.PUT, template, handler);
    }

    public Router delete(String template, RouteHandler handler) {
        return route(HttpMethod.DELETE, template, handler);
    }

    public Router route(HttpMethod method, String template, RouteHandler handler) {
        routes.add(new Route(method, PathPattern.of(template), handler));
        return this;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            HttpPrincipal principal = exchange.getPrincipal();
            if (principal == null) {
                serve(exchange);
            } else {
                RequestContext authenticated = RequestContext.current().withPrincipal(principal.getUsername());
                RequestContext.runWhere(authenticated, () -> serve(exchange));
            }
        }
    }

    private void serve(HttpExchange exchange) throws IOException {
        ResponseEntity<? extends JsonValue> response;
        try {
            response = dispatch(exchange);
        } catch (RuntimeException failure) {
            response = errorMapper.toResponse(failure);
        }
        write(exchange, response);
    }

    private ResponseEntity<? extends JsonValue> dispatch(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String rawMethod = exchange.getRequestMethod();

        List<Match> matches = matchesFor(path);
        if (matches.isEmpty()) {
            throw new NotFoundException("no resource is mapped to " + path);
        }

        Set<HttpMethod> allowed = EnumSet.noneOf(HttpMethod.class);
        matches.forEach(match -> allowed.add(match.route().method()));

        HttpMethod wanted = HttpMethod.parse(rawMethod)
            .map(method -> method == HttpMethod.HEAD ? HttpMethod.GET : method)
            .orElseThrow(() -> new MethodNotAllowedException(rawMethod, path, allowed));

        return matches.stream()
            .filter(match -> match.route().method() == wanted)
            .findFirst()
            .map(match -> invoke(exchange, match, wanted))
            .orElseThrow(() -> new MethodNotAllowedException(rawMethod, path, allowed));
    }

    private ResponseEntity<? extends JsonValue> invoke(HttpExchange exchange, Match match, HttpMethod method) {
        Request request = new Request(exchange, method, match.variables(), maxBodyBytes);
        return match.route().handler().handle(request);
    }

    private List<Match> matchesFor(String path) {
        List<Match> matches = new ArrayList<>();
        for (Route route : routes) {
            Optional<Map<String, String>> variables = route.pattern().match(path);
            variables.ifPresent(values -> matches.add(new Match(route, values)));
        }
        return matches;
    }

    private void write(HttpExchange exchange, ResponseEntity<? extends JsonValue> response) throws IOException {
        response.headers().forEach(exchange.getResponseHeaders()::set);

        JsonValue body = response.body();
        int code = response.status().code();
        if (body == null || response.status() == HttpStatus.NO_CONTENT) {
            exchange.sendResponseHeaders(code, -1);
            return;
        }

        byte[] payload = JsonWriter.write(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(MediaType.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (HttpMethod.HEAD.name().equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(code, -1);
            return;
        }
        exchange.sendResponseHeaders(code, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    private record Match(Route route, Map<String, String> variables) {
    }
}
