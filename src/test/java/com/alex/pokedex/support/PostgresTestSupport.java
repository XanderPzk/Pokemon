package com.alex.pokedex.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides a PostgreSQL datasource for integration tests. Uses the CI service container when {@code
 * USE_EXTERNAL_POSTGRES=true}; otherwise starts an ephemeral Testcontainers instance when Docker is
 * available. Each test class gets its own PostgreSQL schema so Flyway migrations and seed scripts
 * do not leak between classes.
 */
public final class PostgresTestSupport {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("postgres:16-alpine");

    private static final PostgreSQLContainer<?> CONTAINER = startContainerIfNeeded();

    private PostgresTestSupport() {}

    public static void register(DynamicPropertyRegistry registry, String schema) {
        registry.add("spring.datasource.url", () -> jdbcUrl(schema));
        registry.add("spring.datasource.username", PostgresTestSupport::username);
        registry.add("spring.datasource.password", PostgresTestSupport::password);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.schemas", () -> schema);
        registry.add("spring.flyway.default-schema", () -> schema);
        registry.add("spring.flyway.create-schemas", () -> "true");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> schema);
    }

    /** True when tests can run against PostgreSQL (external service or Testcontainers). */
    public static boolean isAvailable() {
        return useExternalPostgres() || (CONTAINER != null && CONTAINER.isRunning());
    }

    private static PostgreSQLContainer<?> startContainerIfNeeded() {
        if (useExternalPostgres() || !DockerClientFactory.instance().isDockerAvailable()) {
            return null;
        }
        PostgreSQLContainer<?> postgres =
                new PostgreSQLContainer<>(POSTGRES_IMAGE)
                        .withDatabaseName("pokedex")
                        .withUsername("pokedex")
                        .withPassword("pokedex");
        postgres.start();
        return postgres;
    }

    private static boolean useExternalPostgres() {
        return "true".equalsIgnoreCase(System.getenv("USE_EXTERNAL_POSTGRES"));
    }

    private static String jdbcUrl(String schema) {
        String base = baseJdbcUrl();
        return base + (base.contains("?") ? "&" : "?") + "currentSchema=" + schema;
    }

    private static String baseJdbcUrl() {
        if (useExternalPostgres()) {
            String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
            String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
            String database = System.getenv().getOrDefault("POSTGRES_DB", "pokedex");
            return "jdbc:postgresql://" + host + ":" + port + "/" + database;
        }
        return CONTAINER.getJdbcUrl();
    }

    private static String username() {
        if (useExternalPostgres()) {
            return System.getenv().getOrDefault("POSTGRES_USER", "pokedex");
        }
        return CONTAINER.getUsername();
    }

    private static String password() {
        if (useExternalPostgres()) {
            return System.getenv().getOrDefault("POSTGRES_PASSWORD", "pokedex");
        }
        return CONTAINER.getPassword();
    }
}
