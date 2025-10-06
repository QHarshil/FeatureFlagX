package com.featureflagx.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.featureflagx.dto.FlagRequest;
import com.featureflagx.model.Flag;
import com.featureflagx.repository.FlagRepository;

/**
 * Service class for managing feature flags.
 */
@Service
public class FlagService {

    private static final Logger logger = LoggerFactory.getLogger(FlagService.class);
    private static final String REDIS_KEY_PREFIX = "flag:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final FlagRepository flagRepository;
    private final RedisTemplate<String, Object> redisTemplate; // may be null if Redis is disabled

    @Autowired
    public FlagService(FlagRepository flagRepository,
                       @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) { // ✅ Optional injection
        this.flagRepository = flagRepository;
        this.redisTemplate = redisTemplate;
    }

    /** Create a new feature flag. */
    @Transactional
    public Flag createFlag(FlagRequest flagRequest) {
        logger.info("Creating flag: {}", flagRequest.getKey());

        if (flagRepository.existsByKeyIgnoreCase(flagRequest.getKey())) {
            throw new RuntimeException("Flag already exists: " + flagRequest.getKey());
        }

        Flag flag = new Flag();
        flag.setKey(flagRequest.getKey().toLowerCase());
        flag.setEnabled(flagRequest.isEnabled());
        flag.setConfig(flagRequest.getConfig());

        Flag savedFlag = flagRepository.save(flag);
        clearFlagCache(savedFlag.getKey());

        logger.info("Flag created successfully: {}", savedFlag.getKey());
        return savedFlag;
    }

    /** Update an existing feature flag. */
    @Transactional
    public Flag updateFlag(String key, FlagRequest flagRequest) {
        logger.info("Updating flag: {}", key);

        Flag existingFlag = flagRepository.findByKeyIgnoreCase(key)
            .orElseThrow(() -> new RuntimeException("Flag not found: " + key));

        existingFlag.setEnabled(flagRequest.isEnabled());
        existingFlag.setConfig(flagRequest.getConfig());

        Flag updatedFlag = flagRepository.save(existingFlag);
        clearFlagCache(updatedFlag.getKey());

        logger.info("Flag updated successfully: {}", updatedFlag.getKey());
        return updatedFlag;
    }

    /** Delete a feature flag. */
    @Transactional
    public void deleteFlag(String key) {
        logger.info("Deleting flag: {}", key);

        Flag flag = flagRepository.findByKeyIgnoreCase(key)
            .orElseThrow(() -> new RuntimeException("Flag not found: " + key));

        flagRepository.delete(flag);
        clearFlagCache(key);

        logger.info("Flag deleted successfully: {}", key);
    }

    /** Get a feature flag by key. */
    public Flag getFlag(String key) {
        logger.debug("Getting flag: {}", key);
        return flagRepository.findByKeyIgnoreCase(key)
            .orElseThrow(() -> new RuntimeException("Flag not found: " + key));
    }

    /** Get all feature flags. */
    public List<Flag> getAllFlags() {
        logger.debug("Getting all flags");
        return flagRepository.findAll();
    }

    /** Get all enabled flags. */
    public List<Flag> getEnabledFlags() {
        logger.debug("Getting enabled flags");
        return flagRepository.findByEnabledTrue();
    }

    /** Get all disabled flags. */
    public List<Flag> getDisabledFlags() {
        logger.debug("Getting disabled flags");
        return flagRepository.findByEnabledFalse();
    }

    /** Search flags by key pattern. */
    public List<Flag> searchFlags(String pattern) {
        logger.debug("Searching flags with pattern: {}", pattern);
        return flagRepository.findByKeyContainingIgnoreCase(pattern);
    }

    /**
     * Evaluate a feature flag for a given target.
     * Implements caching for high performance when Redis is available.
     */
    public boolean isEnabled(String key, String targetId) {
        logger.debug("Evaluating flag: {} for target: {}", key, targetId);
        String cacheKey = REDIS_KEY_PREFIX + key.toLowerCase();

        // If Redis is not configured (test profile, CI), skip caching entirely
        if (redisTemplate == null) {
            logger.debug("Redis disabled — reading flag {} directly from DB", key);
            return flagRepository.findByKeyIgnoreCase(key)
                .map(Flag::isEnabled)
                .orElse(false);
        }

        try {
            Boolean cachedValue = (Boolean) redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                logger.debug("Cache hit: {} = {}", key, cachedValue);
                return cachedValue;
            }

            // Cache miss → fetch from DB
            Optional<Flag> flagOpt = flagRepository.findByKeyIgnoreCase(key);
            boolean enabled = flagOpt.map(Flag::isEnabled).orElse(false);

            // Store in cache
            redisTemplate.opsForValue().set(cacheKey, enabled, CACHE_TTL);
            logger.debug("Cache miss: {} = {} (cached)", key, enabled);
            return enabled;

        } catch (Exception e) {
            logger.warn("Redis error for flag {}, fallback to DB: {}", key, e.getMessage());
            return flagRepository.findByKeyIgnoreCase(key)
                .map(Flag::isEnabled)
                .orElse(false);
        }
    }

    /** Check if a flag exists. */
    public boolean flagExists(String key) {
        return flagRepository.existsByKeyIgnoreCase(key);
    }

    /** Clear cache for a specific flag. */
    private void clearFlagCache(String key) {
        if (redisTemplate == null) {
            logger.debug("Redis disabled — skipping cache clear for {}", key);
            return;
        }

        try {
            String cacheKey = REDIS_KEY_PREFIX + key.toLowerCase();
            redisTemplate.delete(cacheKey);
            logger.debug("Cleared cache for flag: {}", key);
        } catch (Exception e) {
            logger.warn("Failed to clear cache for {}: {}", key, e.getMessage());
        }
    }
}
