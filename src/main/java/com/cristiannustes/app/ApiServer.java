package com.cristiannustes.app;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.cristiannustes.api.HealthApi;
import com.cristiannustes.api.HelloApi;
import com.cristiannustes.api.user.UserApi;
import com.cristiannustes.http.Router;
import com.cristiannustes.http.filter.BodyLimitFilter;
import com.cristiannustes.http.filter.RequestLogFilter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

public final class ApiServer implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(ApiServer.class.getName());

    private final ServiceRegistry services;
    private final HttpServer server;
    private final ExecutorService executor;

    public ApiServer(ServiceRegistry services) throws IOException {
        this.services = services;
        AppConfig config = services.config();

        this.server = HttpServer.create(new InetSocketAddress(config.port()), config.backlog());
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        server.setExecutor(executor);

        filter(server.createContext("/api", publicRoutes()));

        filter(server.createContext("/api/hello", secureRoutes()))
            .setAuthenticator(services.authenticator());
    }

    private Router publicRoutes() {
        Router router = new Router(services.config().maxBodyBytes());
        new UserApi(services.userService()).registerOn(router);
        new HealthApi(services.startedAt(), services.clock(), services.userRepository()::count).registerOn(router);
        return router;
    }

    private Router secureRoutes() {
        Router router = new Router(services.config().maxBodyBytes());
        new HelloApi().registerOn(router);
        return router;
    }

    private HttpContext filter(HttpContext context) {
        context.getFilters().add(new RequestLogFilter());
        context.getFilters().add(new BodyLimitFilter(services.config().maxBodyBytes()));
        return context;
    }

    public void start() {
        server.start();
        LOGGER.log(System.Logger.Level.INFO, "Listening on http://localhost:%d/api".formatted(port()));
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(services.config().shutdownGraceSeconds());
        executor.close();
        LOGGER.log(System.Logger.Level.INFO, "Server stopped");
    }
}
