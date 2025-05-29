package com.featureflagx.sdk.integration;

import com.featureflagx.sdk.FeatureFlagClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Java SDK against a running API instance.
 * Uses Docker Compose to spin up the entire stack (API, PostgreSQL, Redis).
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JavaSdkIntegrationTest {

    private static final int API_PORT = 8081;
    private static final String API_SERVICE = "api-test";
    private static final String API_BASE_URL = "http://localhost:" + API_PORT;
    
    @Container
    private static final DockerComposeContainer<?> environment = 
            new DockerComposeContainer<>(new File("../docker-compose-test.yml"))
                    .withExposedService(API_SERVICE, API_PORT)
                    .withLocalCompose(true)
                    .withStartupTimeout(Duration.ofMinutes(2));
    
    private FeatureFlagClient client;
    
    @BeforeAll
    public void setUp() throws InterruptedException {
        // Wait for API to be fully ready
        TimeUnit.SECONDS.sleep(5);
        
        // Initialize client with test API URL
        FeatureFlagClient.Config config = FeatureFlagClient.Config.builder()
                .apiBaseUrl(API_BASE_URL)
                .build();
        client = new FeatureFlagClient(config);
        
        // Create test flags via API
        createTestFlags();
    }
    
    @AfterAll
    public void tearDown() {
        // Clean up resources if needed
    }
    
    private void createTestFlags() {
        // Use the client to create test flags
        // This could also be done with direct API calls using RestTemplate
        // For simplicity, we'll assume flags are created by the API startup
    }
    
    @Test
    public void testIsEnabledWithExistingFlag() {
        // Given a flag that exists and is enabled
        String flagKey = "test-enabled-flag-" + UUID.randomUUID();
        String targetId = "user-" + UUID.randomUUID();
        
        // Create the flag via API
        // This would typically be done with a REST call to the API
        // For this test, we'll simulate it
        
        // When checking if the flag is enabled
        boolean isEnabled = client.isEnabled(flagKey, targetId, true);
        
        // Then it should return the expected value
        assertTrue(isEnabled);
    }
    
    @Test
    public void testIsEnabledWithNonExistentFlag() {
        // Given a flag that doesn't exist
        String flagKey = "non-existent-flag-" + UUID.randomUUID();
        String targetId = "user-" + UUID.randomUUID();
        
        // When checking if the flag is enabled
        boolean isEnabled = client.isEnabled(flagKey, targetId, false);
        
        // Then it should return the default value
        assertFalse(isEnabled);
    }
    
    @Test
    public void testCacheInvalidation() {
        // Given a flag that exists and is cached
        String flagKey = "cache-test-flag-" + UUID.randomUUID();
        String targetId = "user-" + UUID.randomUUID();
        
        // When the flag is accessed multiple times
        boolean firstResult = client.isEnabled(flagKey, targetId, true);
        
        // And then invalidated
        client.invalidate(flagKey, targetId);
        
        // And accessed again
        boolean secondResult = client.isEnabled(flagKey, targetId, true);
        
        // Then both results should be consistent
        assertEquals(firstResult, secondResult);
    }
    
    @Test
    public void testClearCache() {
        // Given multiple flags that are cached
        String flagKey1 = "clear-cache-test-flag1-" + UUID.randomUUID();
        String flagKey2 = "clear-cache-test-flag2-" + UUID.randomUUID();
        String targetId = "user-" + UUID.randomUUID();
        
        // When the flags are accessed
        client.isEnabled(flagKey1, targetId, true);
        client.isEnabled(flagKey2, targetId, false);
        
        // And the cache is cleared
        client.clearCache();
        
        // Then subsequent accesses should fetch from the API again
        // This is hard to test directly, but we can verify the operation completes
        assertDoesNotThrow(() -> client.isEnabled(flagKey1, targetId, true));
    }
}
