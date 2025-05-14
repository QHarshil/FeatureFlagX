package com.featureflagx.service;

import com.featureflagx.model.Flag;
import com.featureflagx.repository.FlagRepository;
import com.featureflagx.dto.FlagRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class FlagService {

    private final FlagRepository flagRepository;
    private final RedisTemplate<String, Object> redisTemplate; // Using Object value for flexibility, can be Boolean

    private static final String REDIS_KEY_PREFIX = "flag:";
    private static final Duration REDIS_CACHE_TTL = Duration.ofMinutes(5);

    @Autowired
    public FlagService(FlagRepository flagRepository, RedisTemplate<String, Object> redisTemplate) {
        this.flagRepository = flagRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Flag createFlag(FlagRequest flagRequest) {
        Flag flag = new Flag();
        flag.setKey(flagRequest.getKey());
        flag.setEnabled(flagRequest.isEnabled());
        flag.setConfig(flagRequest.getConfig()); // Assuming config is a JSON string
        flag.setUpdatedAt(Instant.now());
        Flag savedFlag = flagRepository.save(flag);
        clearCache(savedFlag.getKey());
        return savedFlag;
    }

    @Transactional
    public Optional<Flag> updateFlag(String key, FlagRequest flagRequest) {
        Optional<Flag> existingFlagOpt = flagRepository.findById(key);
        if (existingFlagOpt.isPresent()) {
            Flag existingFlag = existingFlagOpt.get();
            existingFlag.setEnabled(flagRequest.isEnabled());
            existingFlag.setConfig(flagRequest.getConfig());
            existingFlag.setUpdatedAt(Instant.now());
            Flag updatedFlag = flagRepository.save(existingFlag);
            clearCache(updatedFlag.getKey());
            return Optional.of(updatedFlag);
        }
        return Optional.empty();
    }

    @Transactional
    public boolean deleteFlag(String key) {
        if (flagRepository.existsById(key)) {
            flagRepository.deleteById(key);
            clearCache(key);
            return true;
        }
        return false;
    }

    public Optional<Flag> getFlag(String key) {
        return flagRepository.findById(key);
    }

    public List<Flag> getAllFlags() {
        return flagRepository.findAll();
    }

    public boolean isEnabled(String key, String targetId) {
        // targetId is not used in this basic version but can be used for more complex evaluation logic
        String redisKey = REDIS_KEY_PREFIX + key;
        Boolean cachedEnabled = (Boolean) redisTemplate.opsForValue().get(redisKey);

        if (cachedEnabled != null) {
            return cachedEnabled;
        }

        Optional<Flag> flagOpt = flagRepository.findById(key);
        if (flagOpt.isPresent()) {
            boolean enabled = flagOpt.get().isEnabled();
            redisTemplate.opsForValue().set(redisKey, enabled, REDIS_CACHE_TTL);
            return enabled;
        }
        // Default behavior for non-existent flag: false
        // Cache the miss as well to prevent DB hammering for non-existent flags
        redisTemplate.opsForValue().set(redisKey, false, REDIS_CACHE_TTL); 
        return false;
    }

    private void clearCache(String key) {
        redisTemplate.delete(REDIS_KEY_PREFIX + key);
    }
}

