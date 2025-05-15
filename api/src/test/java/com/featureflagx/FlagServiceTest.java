package com.featureflagx;

import com.featureflagx.model.Flag;
import com.featureflagx.repository.FlagRepository;
import com.featureflagx.service.FlagService;
import com.featureflagx.dto.FlagRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class FlagServiceTest {

    @Mock
    private FlagRepository flagRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private FlagService flagService;

    private Flag flag1;
    private FlagRequest flagRequest1;
    private final String FLAG_KEY_1 = "test-flag-1";
    private final String REDIS_PREFIXED_KEY_1 = "flag:" + FLAG_KEY_1;

    @BeforeEach
    void setUp() {
        flag1 = new Flag();
        flag1.setKey(FLAG_KEY_1);
        flag1.setEnabled(true);
        flag1.setConfig("{ \"variant\": \"A\" }");
        flag1.setUpdatedAt(Instant.now());

        flagRequest1 = new FlagRequest();
        flagRequest1.setKey(FLAG_KEY_1);
        flagRequest1.setEnabled(true);
        flagRequest1.setConfig("{ \"variant\": \"A\" }");

        // Mock Redis operations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createFlag_shouldSaveToRepositoryAndClearCache() {
        when(flagRepository.save(any(Flag.class))).thenReturn(flag1);

        Flag result = flagService.createFlag(flagRequest1);

        assertNotNull(result);
        assertEquals(FLAG_KEY_1, result.getKey());
        verify(flagRepository, times(1)).save(any(Flag.class));
        verify(redisTemplate, times(1)).delete(REDIS_PREFIXED_KEY_1);
    }

    @Test
    void updateFlag_whenFlagExists_shouldUpdateAndClearCache() {
        FlagRequest updatedRequest = new FlagRequest();
        updatedRequest.setKey(FLAG_KEY_1);
        updatedRequest.setEnabled(false);
        updatedRequest.setConfig("{ \"variant\": \"B\" }");

        Flag updatedFlag = new Flag();
        updatedFlag.setKey(FLAG_KEY_1);
        updatedFlag.setEnabled(false);
        updatedFlag.setConfig("{ \"variant\": \"B\" }");
        updatedFlag.setUpdatedAt(Instant.now());

        when(flagRepository.findById(FLAG_KEY_1)).thenReturn(Optional.of(flag1));
        when(flagRepository.save(any(Flag.class))).thenReturn(updatedFlag);

        Optional<Flag> result = flagService.updateFlag(FLAG_KEY_1, updatedRequest);

        assertTrue(result.isPresent());
        assertEquals(false, result.get().isEnabled());
        verify(flagRepository, times(1)).findById(FLAG_KEY_1);
        verify(flagRepository, times(1)).save(any(Flag.class));
        verify(redisTemplate, times(1)).delete(REDIS_PREFIXED_KEY_1);
    }

    @Test
    void updateFlag_whenFlagNotExists_shouldReturnEmpty() {
        when(flagRepository.findById("non-existent-key")).thenReturn(Optional.empty());

        Optional<Flag> result = flagService.updateFlag("non-existent-key", flagRequest1);

        assertFalse(result.isPresent());
        verify(flagRepository, times(1)).findById("non-existent-key");
        verify(flagRepository, never()).save(any(Flag.class));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void deleteFlag_whenFlagExists_shouldDeleteAndClearCache() {
        when(flagRepository.existsById(FLAG_KEY_1)).thenReturn(true);
        doNothing().when(flagRepository).deleteById(FLAG_KEY_1);

        boolean result = flagService.deleteFlag(FLAG_KEY_1);

        assertTrue(result);
        verify(flagRepository, times(1)).existsById(FLAG_KEY_1);
        verify(flagRepository, times(1)).deleteById(FLAG_KEY_1);
        verify(redisTemplate, times(1)).delete(REDIS_PREFIXED_KEY_1);
    }

    @Test
    void deleteFlag_whenFlagNotExists_shouldReturnFalse() {
        when(flagRepository.existsById("non-existent-key")).thenReturn(false);

        boolean result = flagService.deleteFlag("non-existent-key");

        assertFalse(result);
        verify(flagRepository, times(1)).existsById("non-existent-key");
        verify(flagRepository, never()).deleteById(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void getFlag_shouldReturnFlagFromRepository() {
        when(flagRepository.findById(FLAG_KEY_1)).thenReturn(Optional.of(flag1));
        Optional<Flag> result = flagService.getFlag(FLAG_KEY_1);
        assertTrue(result.isPresent());
        assertEquals(FLAG_KEY_1, result.get().getKey());
        verify(flagRepository, times(1)).findById(FLAG_KEY_1);
    }

    @Test
    void getAllFlags_shouldReturnAllFlagsFromRepository() {
        when(flagRepository.findAll()).thenReturn(Arrays.asList(flag1));
        List<Flag> results = flagService.getAllFlags();
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        verify(flagRepository, times(1)).findAll();
    }

    @Test
    void isEnabled_whenCached_shouldReturnCachedValue() {
        when(valueOperations.get(REDIS_PREFIXED_KEY_1)).thenReturn(true);

        boolean result = flagService.isEnabled(FLAG_KEY_1, "user123");

        assertTrue(result);
        verify(valueOperations, times(1)).get(REDIS_PREFIXED_KEY_1);
        verify(flagRepository, never()).findById(anyString());
    }

    @Test
    void isEnabled_whenNotCachedAndFlagExists_shouldFetchFromDbAndCache() {
        when(valueOperations.get(REDIS_PREFIXED_KEY_1)).thenReturn(null);
        when(flagRepository.findById(FLAG_KEY_1)).thenReturn(Optional.of(flag1));

        boolean result = flagService.isEnabled(FLAG_KEY_1, "user123");

        assertTrue(result);
        verify(valueOperations, times(1)).get(REDIS_PREFIXED_KEY_1);
        verify(flagRepository, times(1)).findById(FLAG_KEY_1);
        verify(valueOperations, times(1)).set(REDIS_PREFIXED_KEY_1, true, Duration.ofMinutes(5));
    }

    @Test
    void isEnabled_whenNotCachedAndFlagNotExists_shouldReturnFalseAndCacheMiss() {
        String nonExistentKey = "non-existent-flag";
        String redisNonExistentKey = "flag:" + nonExistentKey;
        when(valueOperations.get(redisNonExistentKey)).thenReturn(null);
        when(flagRepository.findById(nonExistentKey)).thenReturn(Optional.empty());

        boolean result = flagService.isEnabled(nonExistentKey, "user123");

        assertFalse(result);
        verify(valueOperations, times(1)).get(redisNonExistentKey);
        verify(flagRepository, times(1)).findById(nonExistentKey);
        verify(valueOperations, times(1)).set(redisNonExistentKey, false, Duration.ofMinutes(5));
    }
}

