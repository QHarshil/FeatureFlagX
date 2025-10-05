package com.featureflagx.model;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Flag entity representing a feature flag in the system.
 */
@Entity
@Table(name = "flags")
public class Flag {

    @Id
    @Column(name = "flag_key", nullable = false)
    @NotBlank(message = "Flag key cannot be blank")
    @Size(min = 1, max = 255, message = "Flag key must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", 
             message = "Flag key must follow kebab-case format")
    private String key;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(columnDefinition = "TEXT")
    @Size(max = 10000, message = "Config must not exceed 10000 characters")
    private String config;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Flag() {}

    public Flag(String key, boolean enabled) {
        this.key = key;
        this.enabled = enabled;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Flag(String key, boolean enabled, String config) {
        this.key = key;
        this.enabled = enabled;
        this.config = config;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    @Override
    public String toString() {
        return "Flag{" +
                "key='" + key + '\'' +
                ", enabled=" + enabled +
                ", config='" + config + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
