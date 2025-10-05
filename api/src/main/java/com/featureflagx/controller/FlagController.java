package com.featureflagx.controller;

import com.featureflagx.dto.FlagRequest;
import com.featureflagx.dto.FlagResponse;
import com.featureflagx.model.Flag;
import com.featureflagx.service.FlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing feature flags.
 */
@RestController
@RequestMapping("/api/v1/flags")
@CrossOrigin(origins = "*")
public class FlagController {

    private static final Logger logger = LoggerFactory.getLogger(FlagController.class);

    private final FlagService flagService;

    @Autowired
    public FlagController(FlagService flagService) {
        this.flagService = flagService;
    }

    /**
     * Create a new feature flag.
     */
    @PostMapping
    public ResponseEntity<?> createFlag(@Valid @RequestBody FlagRequest flagRequest) {
        try {
            logger.info("Creating flag: {}", flagRequest.getKey());
            Flag createdFlag = flagService.createFlag(flagRequest);
            FlagResponse response = FlagResponse.fromFlag(createdFlag);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating flag: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Update an existing feature flag.
     */
    @PutMapping("/{key}")
    public ResponseEntity<?> updateFlag(@PathVariable @NotBlank String key, 
                                       @Valid @RequestBody FlagRequest flagRequest) {
        try {
            logger.info("Updating flag: {}", key);
            Flag updatedFlag = flagService.updateFlag(key, flagRequest);
            FlagResponse response = FlagResponse.fromFlag(updatedFlag);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating flag {}: {}", key, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Delete a feature flag.
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<?> deleteFlag(@PathVariable @NotBlank String key) {
        try {
            logger.info("Deleting flag: {}", key);
            flagService.deleteFlag(key);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting flag {}: {}", key, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get a specific feature flag.
     */
    @GetMapping("/{key}")
    public ResponseEntity<?> getFlag(@PathVariable @NotBlank String key) {
        try {
            logger.debug("Getting flag: {}", key);
            Flag flag = flagService.getFlag(key);
            FlagResponse response = FlagResponse.fromFlag(flag);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting flag {}: {}", key, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get all feature flags with optional filtering.
     */
    @GetMapping
    public ResponseEntity<?> getAllFlags(@RequestParam(required = false) String search,
                                        @RequestParam(required = false) Boolean enabled) {
        try {
            logger.debug("Getting all flags (search: {}, enabled: {})", search, enabled);
            
            List<Flag> flags;
            
            if (search != null && !search.trim().isEmpty()) {
                flags = flagService.searchFlags(search);
            } else if (enabled != null) {
                flags = enabled ? flagService.getEnabledFlags() : flagService.getDisabledFlags();
            } else {
                flags = flagService.getAllFlags();
            }
            
            List<FlagResponse> responses = flags.stream()
                .map(FlagResponse::fromFlag)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error getting flags: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Evaluate a feature flag (public endpoint for high performance).
     */
    @GetMapping("/evaluate/{key}")
    public ResponseEntity<Boolean> evaluateFlag(@PathVariable @NotBlank String key,
                                               @RequestParam(required = false) String targetId) {
        try {
            logger.debug("Evaluating flag: {} for target: {}", key, targetId);
            boolean isEnabled = flagService.isEnabled(key, targetId);
            return ResponseEntity.ok(isEnabled);
        } catch (Exception e) {
            logger.error("Error evaluating flag {}: {}", key, e.getMessage());
            // Return false for any error to fail safely
            return ResponseEntity.ok(false);
        }
    }
}
