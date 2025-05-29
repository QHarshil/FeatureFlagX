package com.featureflagx.performance;

import com.featureflagx.dto.FlagRequest;
import com.featureflagx.dto.FlagResponse;
import com.featureflagx.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for the Flag API.
 * Tests high-traffic scenarios and response times.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FlagApiPerformanceTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    public void setUp() {
        this.baseUrl = "http://localhost:" + port + "/flags";
    }

    /**
     * Test concurrent flag creation performance.
     * Creates multiple flags concurrently and measures the time taken.
     */
    @Test
    public void testConcurrentFlagCreation() throws Exception {
        // Given
        int numFlags = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<CompletableFuture<FlagResponse>> futures = new ArrayList<>();

        // When - create flags concurrently
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numFlags; i++) {
            final int index = i;
            CompletableFuture<FlagResponse> future = CompletableFuture.supplyAsync(() -> {
                String flagKey = "perf-test-flag-" + index + "-" + UUID.randomUUID();
                FlagRequest request = new FlagRequest();
                request.setKey(flagKey);
                request.setEnabled(true);
                request.setConfig("{\"version\":\"1.0\"}");
                
                ResponseEntity<FlagResponse> response = restTemplate.postForEntity(
                        baseUrl, request, FlagResponse.class);
                
                return response.getBody();
            }, executorService);
            
            futures.add(future);
        }
        
        // Wait for all futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        allFutures.get(30, TimeUnit.SECONDS); // Timeout after 30 seconds
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then - verify all flags were created successfully
        List<FlagResponse> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        
        assertThat(results).hasSize(numFlags);
        assertThat(results).allMatch(flag -> flag != null && flag.getKey() != null);
        
        // Log performance metrics
        System.out.println("Created " + numFlags + " flags concurrently in " + duration + "ms");
        System.out.println("Average time per flag: " + (double) duration / numFlags + "ms");
        
        executorService.shutdown();
    }

    /**
     * Test concurrent flag evaluation performance.
     * Evaluates multiple flags concurrently and measures the time taken.
     */
    @Test
    public void testConcurrentFlagEvaluation() throws Exception {
        // Given - create a set of flags first
        int numFlags = 10;
        List<String> flagKeys = new ArrayList<>();
        
        for (int i = 0; i < numFlags; i++) {
            String flagKey = "perf-eval-flag-" + i + "-" + UUID.randomUUID();
            FlagRequest request = new FlagRequest();
            request.setKey(flagKey);
            request.setEnabled(i % 2 == 0); // Alternate enabled/disabled
            request.setConfig("{\"version\":\"1.0\"}");
            
            restTemplate.postForEntity(baseUrl, request, FlagResponse.class);
            flagKeys.add(flagKey);
        }
        
        // When - evaluate flags concurrently with high load
        int numRequests = 1000; // 1000 evaluation requests
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numRequests; i++) {
            final int index = i;
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                // Cycle through the available flags
                String flagKey = flagKeys.get(index % flagKeys.size());
                String targetId = "user-" + (index % 100); // Simulate 100 different users
                
                ResponseEntity<Boolean> response = restTemplate.getForEntity(
                        baseUrl + "/evaluate/" + flagKey + "?targetId=" + targetId, Boolean.class);
                
                return response.getBody();
            }, executorService);
            
            futures.add(future);
        }
        
        // Wait for all futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        allFutures.get(60, TimeUnit.SECONDS); // Timeout after 60 seconds
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then - verify all evaluations completed
        List<Boolean> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        
        assertThat(results).hasSize(numRequests);
        
        // Log performance metrics
        System.out.println("Processed " + numRequests + " flag evaluations concurrently in " + duration + "ms");
        System.out.println("Average time per evaluation: " + (double) duration / numRequests + "ms");
        System.out.println("Throughput: " + (numRequests * 1000.0 / duration) + " requests/second");
        
        executorService.shutdown();
    }

    /**
     * Test Redis cache performance improvement.
     * Compares response times with and without cache hits.
     */
    @Test
    public void testCachePerformance() throws Exception {
        // Given - create a test flag
        String flagKey = "cache-perf-test-flag-" + UUID.randomUUID();
        FlagRequest request = new FlagRequest();
        request.setKey(flagKey);
        request.setEnabled(true);
        request.setConfig("{\"version\":\"1.0\"}");
        
        restTemplate.postForEntity(baseUrl, request, FlagResponse.class);
        
        // When - first request (cache miss)
        long startTimeMiss = System.currentTimeMillis();
        ResponseEntity<Boolean> responseMiss = restTemplate.getForEntity(
                baseUrl + "/evaluate/" + flagKey + "?targetId=user1", Boolean.class);
        long endTimeMiss = System.currentTimeMillis();
        long durationMiss = endTimeMiss - startTimeMiss;
        
        // When - second request (cache hit)
        long startTimeHit = System.currentTimeMillis();
        ResponseEntity<Boolean> responseHit = restTemplate.getForEntity(
                baseUrl + "/evaluate/" + flagKey + "?targetId=user1", Boolean.class);
        long endTimeHit = System.currentTimeMillis();
        long durationHit = endTimeHit - startTimeHit;
        
        // Then - verify both responses are correct
        assertThat(responseMiss.getBody()).isTrue();
        assertThat(responseHit.getBody()).isTrue();
        
        // Log performance metrics
        System.out.println("Cache miss request time: " + durationMiss + "ms");
        System.out.println("Cache hit request time: " + durationHit + "ms");
        System.out.println("Cache performance improvement: " + 
                (durationMiss > 0 ? ((double) durationMiss / Math.max(1, durationHit)) : "N/A") + "x");
    }

    /**
     * Test bulk flag retrieval performance.
     * Retrieves all flags and measures the time taken.
     */
    @Test
    public void testBulkFlagRetrieval() throws Exception {
        // Given - create a large number of flags
        int numFlags = 100;
        
        for (int i = 0; i < numFlags; i++) {
            String flagKey = "bulk-test-flag-" + i + "-" + UUID.randomUUID();
            FlagRequest request = new FlagRequest();
            request.setKey(flagKey);
            request.setEnabled(i % 2 == 0);
            request.setConfig("{\"version\":\"1.0\",\"index\":" + i + "}");
            
            restTemplate.postForEntity(baseUrl, request, FlagResponse.class);
        }
        
        // When - retrieve all flags
        long startTime = System.currentTimeMillis();
        ResponseEntity<FlagResponse[]> response = restTemplate.getForEntity(
                baseUrl, FlagResponse[].class);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then - verify all flags were retrieved
        FlagResponse[] flags = response.getBody();
        assertThat(flags).isNotNull();
        assertThat(flags.length).isGreaterThanOrEqualTo(numFlags);
        
        // Log performance metrics
        System.out.println("Retrieved " + flags.length + " flags in " + duration + "ms");
    }
}
