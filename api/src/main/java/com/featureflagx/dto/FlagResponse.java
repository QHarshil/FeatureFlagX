package com.featureflagx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.featureflagx.model.Flag;
import java.time.LocalDateTime;

/**
 * Response DTO for feature flag operations.
 */
public class FlagResponse {

    @JsonProperty("key")
    private String key;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("config")
    private String config;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    // Constructors
    public FlagResponse() {}

    public FlagResponse(String key, boolean enabled, String config, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.key = key;
        this.enabled = enabled;
        this.config = config;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a FlagResponse from a Flag entity.
     */
    public static FlagResponse fromFlag(Flag flag) {
        return new FlagResponse(
            flag.getKey(),
            flag.isEnabled(),
            flag.getConfig(),
            flag.getCreatedAt(),
            flag.getUpdatedAt()
        );
    }

    // Getters and Setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
