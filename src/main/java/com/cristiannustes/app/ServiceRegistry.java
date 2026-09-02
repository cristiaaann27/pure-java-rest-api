package com.cristiannustes.app;

import java.time.Clock;
import java.time.Instant;

import com.cristiannustes.data.user.InMemoryUserRepository;
import com.cristiannustes.domain.user.UserRepository;
import com.cristiannustes.domain.user.UserService;
import com.cristiannustes.security.CredentialsAuthenticator;
import com.cristiannustes.security.PasswordHasher;
import com.sun.net.httpserver.Authenticator;

public final class ServiceRegistry {

    private static final System.Logger LOGGER = System.getLogger(ServiceRegistry.class.getName());
    private static final String REALM = "pure-java-rest-api";

    private final AppConfig config;
    private final Clock clock;
    private final Instant startedAt;
    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;
    private final UserService userService;
    private final String adminPasswordHash;

    public ServiceRegistry(AppConfig config) {
        this(config, Clock.systemUTC());
    }

    public ServiceRegistry(AppConfig config, Clock clock) {
        this.config = config;
        this.clock = clock;
        this.startedAt = clock.instant();
        this.passwordHasher = new PasswordHasher(config.passwordIterations());
        this.userRepository = new InMemoryUserRepository();
        this.userService = new UserService(userRepository, passwordHasher, clock);
        this.adminPasswordHash = config.adminPasswordHash().orElseGet(this::generateAdminPassword);
    }

    public AppConfig config() {
        return config;
    }

    public Clock clock() {
        return clock;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public UserService userService() {
        return userService;
    }

    public UserRepository userRepository() {
        return userRepository;
    }

    public Authenticator authenticator() {
        return new CredentialsAuthenticator(REALM, config.adminUser(), adminPasswordHash, passwordHasher);
    }

    private String generateAdminPassword() {
        String password = PasswordHasher.randomPassword();
        LOGGER.log(System.Logger.Level.WARNING,
            """
            No API_ADMIN_PASSWORD_HASH configured, generated a temporary password for this run:
                user:     %s
                password: %s
            Set API_ADMIN_PASSWORD_HASH to a value produced by './api hash <password>' to keep it stable.
            """.formatted(config.adminUser(), password));
        return passwordHasher.hash(password);
    }
}
