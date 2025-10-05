package com.featureflagx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for FeatureFlagX API.
 * 
 * A simple, production-ready feature flag service that provides:
 * - RESTful API for managing feature flags
 * - High-performance flag evaluation with Redis caching
 * - PostgreSQL persistence
 * - Health monitoring
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
