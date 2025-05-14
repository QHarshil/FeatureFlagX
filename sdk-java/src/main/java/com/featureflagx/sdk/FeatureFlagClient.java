package com.featureflagx.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class FeatureFlagClient {

    private final String apiBaseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Cache<String, Boolean> flagCache;

    @Getter
    private final Config config;

    public static class Config {
        private final String apiBaseUrl;
        private final Duration connectTimeout;
        private final Duration readTimeout;
        private final long cacheMaxSize;
        private final Duration cacheExpireAfterWrite;

        @Builder
        public Config(String apiBaseUrl, Duration connectTimeout, Duration readTimeout, long cacheMaxSize, Duration cacheExpireAfterWrite) {
            this.apiBaseUrl = apiBaseUrl != null ? apiBaseUrl : "http://localhost:8080"; // Default API URL
            this.connectTimeout = connectTimeout != null ? connectTimeout : Duration.ofSeconds(5);
            this.readTimeout = readTimeout != null ? readTimeout : Duration.ofSeconds(5);
            this.cacheMaxSize = cacheMaxSize > 0 ? cacheMaxSize : 1000; // Default cache size
            this.cacheExpireAfterWrite = cacheExpireAfterWrite != null ? cacheExpireAfterWrite : Duration.ofMinutes(5); // Default cache TTL
        }
    }

    public FeatureFlagClient(Config config) {
        this.config = config;
        this.apiBaseUrl = config.apiBaseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(config.readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.flagCache = Caffeine.newBuilder()
                .maximumSize(config.cacheMaxSize)
                .expireAfterWrite(config.cacheExpireAfterWrite.toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    public boolean isEnabled(String flagKey, String targetId) {
        return isEnabled(flagKey, targetId, false); // Default to false if flag not found or error
    }

    public boolean isEnabled(String flagKey, String targetId, boolean defaultValue) {
        if (flagKey == null || flagKey.trim().isEmpty()) {
            return defaultValue;
        }

        // Try to get from cache first
        Boolean cachedValue = flagCache.getIfPresent(flagKey);
        if (cachedValue != null) {
            return cachedValue;
        }

        // If not in cache, fetch from API
        try {
            String url = String.format("%s/flags/evaluate/%s", apiBaseUrl, flagKey);
            if (targetId != null && !targetId.trim().isEmpty()) {
                url += "?targetId=" + targetId;
            }

            Request request = new Request.Builder().url(url).get().build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    // Assuming the API returns a simple boolean (true/false) as a string or JSON boolean
                    boolean enabled = objectMapper.readValue(responseBody, Boolean.class);
                    flagCache.put(flagKey, enabled); // Cache the result
                    return enabled;
                } else {
                    // Log error or handle non-successful response
                    // System.err.println("Failed to evaluate flag " + flagKey + ": " + response.code());
                    flagCache.put(flagKey, defaultValue); // Cache default value on error to avoid hammering
                    return defaultValue;
                }
            }
        } catch (IOException e) {
            // Log error or handle exception
            // System.err.println("Error evaluating flag " + flagKey + ": " + e.getMessage());
            flagCache.put(flagKey, defaultValue); // Cache default value on error
            return defaultValue;
        }
    }

    public void clearCache() {
        flagCache.invalidateAll();
    }

    public void invalidateFlag(String flagKey) {
        if (flagKey != null) {
            flagCache.invalidate(flagKey);
        }
    }
}

