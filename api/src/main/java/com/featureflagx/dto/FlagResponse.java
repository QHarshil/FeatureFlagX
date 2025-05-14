package com.featureflagx.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class FlagResponse {
    private String key;
    private boolean enabled;
    private String config; // JSON string
    private Instant updatedAt;

    // Static factory method or constructor for conversion from Flag entity can be added here
    public static FlagResponse fromFlag(com.featureflagx.model.Flag flag) {
        FlagResponse response = new FlagResponse();
        response.setKey(flag.getKey());
        response.setEnabled(flag.isEnabled());
        response.setConfig(flag.getConfig());
        response.setUpdatedAt(flag.getUpdatedAt());
        return response;
    }
}

