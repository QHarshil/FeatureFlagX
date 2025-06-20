package com.featureflagx.integration;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Base class for integration tests that require PostgreSQL and Redis.
 * Uses Testcontainers to spin up containerized instances for testing.
 * Tests will be skipped if Docker is not available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Testcontainers
@ContextConfiguration(initializers = AbstractIntegrationTest.Initializer.class)
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
public abstract class AbstractIntegrationTest {

    static {
        // Set system property to skip tests if Docker is not available
        try {
            // Simple check if Docker is available
            boolean dockerAvailable = org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
            System.setProperty("docker.available", String.valueOf(dockerAvailable));
        } catch (Exception e) {
            System.setProperty("docker.available", "false");
        }
    }

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("featureflagx_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @Container
    private static final RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    /**
     * Initializer to set up dynamic properties for the test containers.
     */
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                    "spring.datasource.password=" + postgreSQLContainer.getPassword(),
                    "spring.redis.host=" + redisContainer.getHost(),
                    "spring.redis.port=" + redisContainer.getFirstMappedPort()
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}
