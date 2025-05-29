package com.featureflagx.integration;

import com.featureflagx.dto.FlagRequest;
import com.featureflagx.dto.FlagResponse;
import com.featureflagx.model.Flag;
import com.featureflagx.repository.FlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Flag Controller endpoints.
 * Uses real PostgreSQL and Redis instances via testcontainers.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
        // Clear database before each test
        flagRepository.deleteAll();
    }

    @Test
    public void testCreateFlag() {
        // Given
        String flagKey = "test-feature-" + UUID.randomUUID();
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

        // Verify flag is in database
        Flag savedFlag = flagRepository.findByKey(flagKey).orElse(null);
        assertThat(savedFlag).isNotNull();
        assertThat(savedFlag.isEnabled()).isTrue();
    }

    @Test
    public void testGetFlag() {
        // Given
        String flagKey = "get-test-feature-" + UUID.randomUUID();
        Flag flag = new Flag();
        flag.setKey(flagKey);
        flag.setEnabled(true);
        flag.setConfig("{\"version\":\"1.0\"}");
        flagRepository.save(flag);

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
        String flagKey1 = "list-test-feature-1-" + UUID.randomUUID();
        String flagKey2 = "list-test-feature-2-" + UUID.randomUUID();
        
        Flag flag1 = new Flag();
        flag1.setKey(flagKey1);
        flag1.setEnabled(true);
        flag1.setConfig("{\"version\":\"1.0\"}");
        
        Flag flag2 = new Flag();
        flag2.setKey(flagKey2);
        flag2.setEnabled(false);
        flag2.setConfig("{\"version\":\"2.0\"}");
        
        flagRepository.saveAll(List.of(flag1, flag2));

        // When
        ResponseEntity<FlagResponse[]> response = restTemplate.getForEntity(
                baseUrl, FlagResponse[].class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThanOrEqualTo(2);
        
        // Verify both flags are in the response
        boolean flag1Found = false;
        boolean flag2Found = false;
        
        for (FlagResponse flagResponse : response.getBody()) {
            if (flagResponse.getKey().equals(flagKey1)) {
                flag1Found = true;
                assertThat(flagResponse.isEnabled()).isTrue();
            } else if (flagResponse.getKey().equals(flagKey2)) {
                flag2Found = true;
                assertThat(flagResponse.isEnabled()).isFalse();
            }
        }
        
        assertThat(flag1Found).isTrue();
        assertThat(flag2Found).isTrue();
    }

    @Test
    public void testUpdateFlag() {
        // Given
        String flagKey = "update-test-feature-" + UUID.randomUUID();
        Flag flag = new Flag();
        flag.setKey(flagKey);
        flag.setEnabled(true);
        flag.setConfig("{\"version\":\"1.0\"}");
        flagRepository.save(flag);

        FlagRequest updateRequest = new FlagRequest();
        updateRequest.setKey(flagKey);
        updateRequest.setEnabled(false);
        updateRequest.setConfig("{\"version\":\"2.0\"}");

        // When
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

        // Verify flag is updated in database
        Flag updatedFlag = flagRepository.findByKey(flagKey).orElse(null);
        assertThat(updatedFlag).isNotNull();
        assertThat(updatedFlag.isEnabled()).isFalse();
        assertThat(updatedFlag.getConfig()).isEqualTo("{\"version\":\"2.0\"}");
    }

    @Test
    public void testDeleteFlag() {
        // Given
        String flagKey = "delete-test-feature-" + UUID.randomUUID();
        Flag flag = new Flag();
        flag.setKey(flagKey);
        flag.setEnabled(true);
        flag.setConfig("{\"version\":\"1.0\"}");
        flagRepository.save(flag);

        // When
        restTemplate.delete(baseUrl + "/" + flagKey);

        // Then
        assertThat(flagRepository.findByKey(flagKey).isPresent()).isFalse();
    }

    @Test
    public void testEvaluateFlag() {
        // Given
        String flagKey = "evaluate-test-feature-" + UUID.randomUUID();
        Flag flag = new Flag();
        flag.setKey(flagKey);
        flag.setEnabled(true);
        flag.setConfig("{\"version\":\"1.0\"}");
        flagRepository.save(flag);

        // When - without targetId
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
                baseUrl + "/evaluate/" + flagKey, Boolean.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();

        // When - with targetId
        ResponseEntity<Boolean> responseWithTarget = restTemplate.getForEntity(
                baseUrl + "/evaluate/" + flagKey + "?targetId=user123", Boolean.class);

        // Then
        assertThat(responseWithTarget.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseWithTarget.getBody()).isTrue();
        
        // When - flag is disabled
        flag.setEnabled(false);
        flagRepository.save(flag);
        
        ResponseEntity<Boolean> disabledResponse = restTemplate.getForEntity(
                baseUrl + "/evaluate/" + flagKey, Boolean.class);
        
        // Then
        assertThat(disabledResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(disabledResponse.getBody()).isFalse();
    }

    @Test
    public void testFlagNotFound() {
        // When
        ResponseEntity<FlagResponse> response = restTemplate.getForEntity(
                baseUrl + "/non-existent-flag", FlagResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
