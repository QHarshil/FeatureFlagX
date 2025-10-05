package com.featureflagx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Request DTO for creating or updating feature flags.
 */
public class FlagRequest {

    @JsonProperty("key")
    @NotBlank(message = "Flag key cannot be blank")
    @Size(min = 1, max = 255, message = "Flag key must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", 
             message = "Flag key must follow kebab-case format")
    private String key;

    @JsonProperty("enabled")
    private boolean enabled = false;

    @JsonProperty("config")
    @Size(max = 10000, message = "Config must not exceed 10000 characters")
    private String config;

    // Constructors
    public FlagRequest() {}

    public FlagRequest(String key, boolean enabled) {
        this.key = key;
        this.enabled = enabled;
    }

    public FlagRequest(String key, boolean enabled, String config) {
        this.key = key;
        this.enabled = enabled;
        this.config = config;
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
}
