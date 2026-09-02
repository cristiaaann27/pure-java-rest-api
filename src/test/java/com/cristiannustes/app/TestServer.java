package com.cristiannustes.app;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cristiannustes.json.JsonObject;
import com.cristiannustes.json.JsonParser;
import com.cristiannustes.security.PasswordHasher;

public final class TestServer implements AutoCloseable {

    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASSWORD = "test-only-password";

    private static final int CHEAP_ITERATIONS = 1_000;

    static {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.WARNING);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(Level.WARNING);
        }
    }

    private final ApiServer server;
    private final HttpClient client;
    private final String baseUrl;

    private TestServer(ApiServer server) {
        this.server = server;
        this.client = HttpClient.newHttpClient();
        this.baseUrl = "http://localhost:" + server.port();
    }

    public static TestServer start() throws IOException {
        AppConfig config = new AppConfig(
            0,
            0,
            ADMIN_USER,
            Optional.of(new PasswordHasher(CHEAP_ITERATIONS).hash(ADMIN_PASSWORD)),
            64 * 1024,
            0,
            CHEAP_ITERATIONS);

        ApiServer server = new ApiServer(new ServiceRegistry(config, Clock.systemUTC()));
        server.start();
        return new TestServer(server);
    }

    public HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send(request(path).GET());
    }

    public HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return send(request(path).POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    public HttpResponse<String> put(String path, String body) throws IOException, InterruptedException {
        return send(request(path).PUT(HttpRequest.BodyPublishers.ofString(body)));
    }

    public HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return send(request(path).DELETE());
    }

    public HttpResponse<String> method(String verb, String path) throws IOException, InterruptedException {
        return send(request(path).method(verb, HttpRequest.BodyPublishers.noBody()));
    }

    public HttpResponse<String> authenticated(String path, String user, String password)
        throws IOException, InterruptedException {
        String credentials = Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        return send(request(path).header("Authorization", "Basic " + credentials).GET());
    }

    public static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseObject(response.body());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json");
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws IOException, InterruptedException {
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Override
    public void close() {
        client.close();
        server.close();
    }
}
