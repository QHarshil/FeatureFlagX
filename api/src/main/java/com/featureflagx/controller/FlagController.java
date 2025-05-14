package com.featureflagx.controller;

import com.featureflagx.dto.FlagRequest;
import com.featureflagx.dto.FlagResponse;
import com.featureflagx.model.Flag;
import com.featureflagx.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/flags")
public class FlagController {

    private final FlagService flagService;

    @Autowired
    public FlagController(FlagService flagService) {
        this.flagService = flagService;
    }

    @PostMapping
    public ResponseEntity<FlagResponse> createFlag(@RequestBody FlagRequest flagRequest) {
        if (flagRequest.getKey() == null || flagRequest.getKey().trim().isEmpty()) {
            return ResponseEntity.badRequest().build(); // Or a custom error response
        }
        Flag createdFlag = flagService.createFlag(flagRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(FlagResponse.fromFlag(createdFlag));
    }

    @PutMapping("/{key}")
    public ResponseEntity<FlagResponse> updateFlag(@PathVariable String key, @RequestBody FlagRequest flagRequest) {
        Optional<Flag> updatedFlagOpt = flagService.updateFlag(key, flagRequest);
        return updatedFlagOpt
                .map(flag -> ResponseEntity.ok(FlagResponse.fromFlag(flag)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteFlag(@PathVariable String key) {
        boolean deleted = flagService.deleteFlag(key);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<FlagResponse> getFlag(@PathVariable String key) {
        Optional<Flag> flagOpt = flagService.getFlag(key);
        return flagOpt
                .map(flag -> ResponseEntity.ok(FlagResponse.fromFlag(flag)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FlagResponse>> getAllFlags() {
        List<Flag> flags = flagService.getAllFlags();
        List<FlagResponse> flagResponses = flags.stream()
                .map(FlagResponse::fromFlag)
                .collect(Collectors.toList());
        return ResponseEntity.ok(flagResponses);
    }

    @GetMapping("/evaluate/{key}")
    public ResponseEntity<Boolean> evaluateFlag(@PathVariable String key, @RequestParam(required = false) String targetId) {
        // targetId is passed to service, though current service logic doesn't use it for evaluation
        boolean isEnabled = flagService.isEnabled(key, targetId);
        // Even if flag doesn't exist, isEnabled returns false, so we don't need specific notFound handling here for evaluation
        return ResponseEntity.ok(isEnabled);
    }
}

