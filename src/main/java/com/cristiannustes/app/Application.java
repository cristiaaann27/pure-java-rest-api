package com.cristiannustes.app;

import java.io.IOException;

public final class Application {

    private Application() {
    }

    public static void main(String[] args) throws IOException {
        AppConfig config = AppConfig.fromEnvironment();
        ApiServer server = new ApiServer(new ServiceRegistry(config));

        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(server::close));

        server.start();
    }
}
