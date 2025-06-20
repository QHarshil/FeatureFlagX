package com.featureflagx.integration;

import com.featureflagx.dto.FlagRequest;
import com.featureflagx.dto.FlagResponse;
import com.featureflagx.model.Flag;
import com.featureflagx.repository.FlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Flag Controller.
 * Tests the full API functionality with a real database and Redis cache.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "false")
@DisabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "^$", disabledReason = "Integration tests are not enabled")
public class FlagControllerIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FlagRepository flagRepository;

    private String baseUrl;

    @BeforeEach
    public void setUp() {
        this.baseUrl = "http://localhost:" + port + "/flags";
    }

    @Test
    public void testCreateFlag() {
        // Given
        String flagKey = "test-flag-" + UUID.randomUUID();
        FlagRequest request = new FlagRequest();
        request.setKey(flagKey);
        request.setEnabled(true);
        request.setConfig("{\"version\":\"1.0\"}");

        // When
        ResponseEntity<FlagResponse> response = restTemplate.postForEntity(
                baseUrl, request, FlagResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getKey()).isEqualTo(flagKey);
        assertThat(response.getBody().isEnabled()).isTrue();
        assertThat(response.getBody().getConfig()).isEqualTo("{\"version\":\"1.0\"}");

        // Verify flag is in the database
        Optional<Flag> savedFlag = flagRepository.findByKey(flagKey);
        assertThat(savedFlag).isPresent();
        assertThat(savedFlag.get().isEnabled()).isTrue();
    }

    @Test
    public void testGetFlag() {
        // Given
        String flagKey = "get-test-flag-" + UUID.randomUUID();
        FlagRequest request = new FlagRequest();
        request.setKey(flagKey);
        request.setEnabled(true);
        request.setConfig("{\"version\":\"1.0\"}");
        restTemplate.postForEntity(baseUrl, request, FlagResponse.class);

        // When
        ResponseEntity<FlagResponse> response = restTemplate.getForEntity(
                baseUrl + "/" + flagKey, FlagResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getKey()).isEqualTo(flagKey);
        assertThat(response.getBody().isEnabled()).isTrue();
    }

    @Test
    public void testGetAllFlags() {
        // Given
        String flagKey1 = "list-test-flag-1-" + UUID.randomUUID();
        String flagKey2 = "list-test-flag-2-" + UUID.randomUUID();
        
        FlagRequest request1 = new FlagRequest();
        request1.setKey(flagKey1);
        request1.setEnabled(true);
        request1.setConfig("{\"version\":\"1.0\"}");
        
        FlagRequest request2 = new FlagRequest();
        request2.setKey(flagKey2);
        request2.setEnabled(false);
        request2.setConfig("{\"version\":\"2.0\"}");
        
        restTemplate.postForEntity(baseUrl, request1, FlagResponse.class);
        restTemplate.postForEntity(baseUrl, request2, FlagResponse.class);

        // When
        ResponseEntity<FlagResponse[]> response = restTemplate.getForEntity(
                baseUrl, FlagResponse[].class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThanOrEqualTo(2);
        
        // Verify our flags are in the response
        boolean foundFlag1 = false;
        boolean foundFlag2 = false;
        
        for (FlagResponse flag : response.getBody()) {
            if (flag.getKey().equals(flagKey1)) {
                foundFlag1 = true;
                assertThat(flag.isEnabled()).isTrue();
            } else if (flag.getKey().equals(flagKey2)) {
                foundFlag2 = true;
                assertThat(flag.isEnabled()).isFalse();
            }
        }
        
        assertThat(foundFlag1).isTrue();
        assertThat(foundFlag2).isTrue();
    }

    @Test
    public void testUpdateFlag() {
        // Given
        String flagKey = "update-test-flag-" + UUID.randomUUID();
        FlagRequest createRequest = new FlagRequest();
        createRequest.setKey(flagKey);
        createRequest.setEnabled(true);
        createRequest.setConfig("{\"version\":\"1.0\"}");
        
        restTemplate.postForEntity(baseUrl, createRequest, FlagResponse.class);
        
        // When
        FlagRequest updateRequest = new FlagRequest();
        updateRequest.setKey(flagKey);
        updateRequest.setEnabled(false);
        updateRequest.setConfig("{\"version\":\"2.0\"}");
        
        ResponseEntity<FlagResponse> response = restTemplate.exchange(
                baseUrl + "/" + flagKey,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                FlagResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getKey()).isEqualTo(flagKey);
        assertThat(response.getBody().isEnabled()).isFalse();
        assertThat(response.getBody().getConfig()).isEqualTo("{\"version\":\"2.0\"}");
        
        // Verify flag is updated in the database
        Optional<Flag> updatedFlag = flagRepository.findByKey(flagKey);
        assertThat(updatedFlag).isPresent();
        assertThat(updatedFlag.get().isEnabled()).isFalse();
        assertThat(updatedFlag.get().getConfig()).isEqualTo("{\"version\":\"2.0\"}");
    }

    @Test
    public void testDeleteFlag() {
        // Given
        String flagKey = "delete-test-flag-" + UUID.randomUUID();
        FlagRequest request = new FlagRequest();
        request.setKey(flagKey);
        request.setEnabled(true);
        request.setConfig("{\"version\":\"1.0\"}");
        
        restTemplate.postForEntity(baseUrl, request, FlagResponse.class);
        
        // Verify flag exists before deletion
        Optional<Flag> flagBeforeDeletion = flagRepository.findByKey(flagKey);
        assertThat(flagBeforeDeletion).isPresent();
        
        // When
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/" + flagKey,
                HttpMethod.DELETE,
                null,
                Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // Verify flag is deleted from the database
        Optional<Flag> flagAfterDeletion = flagRepository.findByKey(flagKey);
        assertThat(flagAfterDeletion).isEmpty();
    }

    @Test
    public void testEvaluateFlag() {
        // Given
        String flagKey = "evaluate-test-flag-" + UUID.randomUUID();
        FlagRequest request = new FlagRequest();
        request.setKey(flagKey);
        request.setEnabled(true);
        request.setConfig("{\"version\":\"1.0\"}");
        
        restTemplate.postForEntity(baseUrl, request, FlagResponse.class);
        
        // When
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
                baseUrl + "/evaluate/" + flagKey + "?targetId=user123",
                Boolean.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
    }

    @Test
    public void testEvaluateDisabledFlag() {
        // Given
        String flagKey = "evaluate-disabled-flag-" + UUID.randomUUID();
        FlagRequest request = new FlagRequest();
        request.setKey(flagKey);
        request.setEnabled(false);
        request.setConfig("{\"version\":\"1.0\"}");
        
        restTemplate.postForEntity(baseUrl, request, FlagResponse.class);
        
        // When
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
                baseUrl + "/evaluate/" + flagKey + "?targetId=user123",
                Boolean.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    public void testEvaluateNonExistentFlag() {
        // Given
        String nonExistentFlagKey = "non-existent-flag-" + UUID.randomUUID();
        
        // When
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
                baseUrl + "/evaluate/" + nonExistentFlagKey + "?targetId=user123",
                Boolean.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse(); // Default to false for non-existent flags
    }
}