package com.cristiannustes.api;

import static com.cristiannustes.testing.Assertions.assertEquals;
import static com.cristiannustes.testing.Assertions.assertFalse;
import static com.cristiannustes.testing.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import com.cristiannustes.app.TestServer;
import com.cristiannustes.json.JsonObject;
import com.cristiannustes.testing.Test;

public class ApiIntegrationTest {

    private static final String VALID_USER = """
        {"login":"marcin","password":"a-good-password"}""";

    @Test
    public void reportsItsOwnHealth() throws Exception {
        try (TestServer server = TestServer.start()) {
            HttpResponse<String> response = server.get("/api/health");

            assertEquals(200, response.statusCode());
            assertEquals("UP", TestServer.json(response).requireString("status"));
        }
    }

    @Test
    public void stampsEveryResponseWithACorrelationId() throws Exception {
        try (TestServer server = TestServer.start()) {
            HttpResponse<String> response = server.get("/api/health");

            assertTrue(response.headers().firstValue("X-Request-Id").isPresent(),
                "responses must carry the request id used in the logs");
        }
    }

    @Test
    public void createsAUserAndAnswersWhereItLives() throws Exception {
        try (TestServer server = TestServer.start()) {
            HttpResponse<String> response = server.post("/api/users", VALID_USER);

            assertEquals(201, response.statusCode());
            JsonObject created = TestServer.json(response);
            assertEquals("marcin", created.requireString("login"));
            assertFalse(created.has("password"), "a response must never echo the password");
            assertEquals("/api/users/" + created.requireString("id"),
                response.headers().firstValue("Location").orElseThrow());
        }
    }

    @Test
    public void keepsTheRegisterAliasFromTheOriginalTutorial() throws Exception {
        try (TestServer server = TestServer.start()) {
            assertEquals(201, server.post("/api/users/register", VALID_USER).statusCode());
        }
    }

    @Test
    public void readsAUserBackAndThenDeletesIt() throws Exception {
        try (TestServer server = TestServer.start()) {
            String id = TestServer.json(server.post("/api/users", VALID_USER)).requireString("id");

            assertEquals(200, server.get("/api/users/" + id).statusCode());
            assertEquals(204, server.delete("/api/users/" + id).statusCode());
            assertEquals(404, server.get("/api/users/" + id).statusCode());
        }
    }

    @Test
    public void replacesCredentialsWithPut() throws Exception {
        try (TestServer server = TestServer.start()) {
            String id = TestServer.json(server.post("/api/users", VALID_USER)).requireString("id");

            HttpResponse<String> response =
                server.put("/api/users/" + id, """
                    {"login":"marcin.nowak","password":"another-good-one"}""");

            assertEquals(200, response.statusCode());
            assertEquals("marcin.nowak", TestServer.json(response).requireString("login"));
        }
    }

    @Test
    public void paginatesTheCollection() throws Exception {
        try (TestServer server = TestServer.start()) {
            for (int i = 0; i < 3; i++) {
                server.post("/api/users", """
                    {"login":"user-%d","password":"a-good-password"}""".formatted(i));
            }

            JsonObject page = TestServer.json(server.get("/api/users?page=1&size=2"));

            assertEquals(1, page.requireArray("items").size());
            assertEquals(3L, page.optionalLong("totalItems").orElseThrow());
            assertEquals(2L, page.optionalLong("totalPages").orElseThrow());
        }
    }

    @Test
    public void rejectsADuplicateLoginWithAConflict() throws Exception {
        try (TestServer server = TestServer.start()) {
            server.post("/api/users", VALID_USER);

            assertEquals(409, server.post("/api/users", VALID_USER).statusCode());
        }
    }

    @Test
    public void listsEveryBrokenFieldInOneAnswer() throws Exception {
        try (TestServer server = TestServer.start()) {
            HttpResponse<String> response = server.post("/api/users", """
                {"login":"x","password":"short"}""");

            assertEquals(400, response.statusCode());
            assertEquals(2, TestServer.json(response).requireArray("errors").size());
        }
    }

    @Test
    public void rejectsAnUnknownFieldInsteadOfIgnoringIt() throws Exception {
        try (TestServer server = TestServer.start()) {
            HttpResponse<String> response = server.post("/api/users", """
                {"wrong":"request"}""");

            assertEquals(400, response.statusCode());
            assertTrue(TestServer.json(response).requireString("message").contains("unknown field"),
                response.body());
        }
    }

    @Test
    public void answers404ForAnUnmappedPath() throws Exception {
        try (TestServer server = TestServer.start()) {
            assertEquals(404, server.get("/api/does-not-exist").statusCode());
        }
    }

    @Test
    public void answers405WithTheAllowHeader() throws Exception {
        try (TestServer server = TestServer.start()) {
            HttpResponse<String> response = server.method("PATCH", "/api/users");

            assertEquals(405, response.statusCode());
            String allow = response.headers().firstValue("Allow").orElseThrow();
            assertEquals(Set.of("GET", "POST"), Set.copyOf(List.of(allow.split(", "))));
        }
    }

    @Test
    public void protectsTheGreetingWithBasicAuthentication() throws Exception {
        try (TestServer server = TestServer.start()) {
            assertEquals(401, server.get("/api/hello").statusCode());
            assertEquals(401, server.authenticated("/api/hello", TestServer.ADMIN_USER, "wrong").statusCode());

            HttpResponse<String> response =
                server.authenticated("/api/hello", TestServer.ADMIN_USER, TestServer.ADMIN_PASSWORD);

            assertEquals(200, response.statusCode());
            assertEquals("Hello admin!", TestServer.json(response).requireString("message"));
        }
    }

    @Test
    public void refusesABodyThatIsNotJson() throws Exception {
        try (TestServer server = TestServer.start()) {
            HttpResponse<String> response = server.post("/api/users", "login=marcin");

            assertEquals(400, response.statusCode());
        }
    }

    @Test
    public void servesManyRequestsAtOnce() throws Exception {
        int requests = 60;
        try (TestServer server = TestServer.start();
             ExecutorService clients = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Callable<Integer>> calls = IntStream.range(0, requests)
                .<Callable<Integer>>mapToObj(i -> () -> server.post("/api/users", """
                    {"login":"user-%d","password":"a-good-password"}""".formatted(i)).statusCode())
                .toList();

            List<Future<Integer>> results = clients.invokeAll(calls);
            for (Future<Integer> result : results) {
                assertEquals(201, result.get());
            }

            assertEquals((long) requests,
                TestServer.json(server.get("/api/users?size=1")).optionalLong("totalItems").orElseThrow());
        }
    }
}
