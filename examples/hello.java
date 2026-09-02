// The whole tutorial, in one file, with no class and no static main.
//
// This is a compact source file (JEP 512, final in Java 25): the compiler wraps
// the code in an implicit class, java.base is imported as a module, and main can
// be an instance method with no arguments.
//
// Run it straight from source, no compilation step:
//
//     java examples/hello.java
//
// Then:  curl localhost:8000/api/hello?name=Cristian

import module jdk.httpserver;

void main() throws IOException {
    var server = HttpServer.create(new InetSocketAddress(8000), 0);

    server.createContext("/api/hello", exchange -> {
        try (exchange) {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            var query = exchange.getRequestURI().getRawQuery();
            var name = query != null && query.startsWith("name=")
                ? URLDecoder.decode(query.substring("name=".length()), StandardCharsets.UTF_8)
                : "Anonymous";
            var body = "{\"message\":\"Hello %s!\"}".formatted(name).getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    });

    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.start();

    IO.println("Listening on http://localhost:8000/api/hello");
}
