-- FeatureFlagX Database Initialization
CREATE TABLE IF NOT EXISTS flags (
    flag_key VARCHAR(255) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT false,
    config TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_flags_enabled ON flags(enabled);
CREATE INDEX IF NOT EXISTS idx_flags_updated_at ON flags(updated_at);

-- Insert sample data
INSERT INTO flags (flag_key, enabled, config) VALUES 
    ('welcome-banner', true, '{"message": "Welcome to our application!", "color": "blue"}'),
    ('beta-features', false, '{"features": ["advanced-search", "real-time-notifications"]}'),
    ('maintenance-mode', false, '{"message": "System maintenance in progress"}')
ON CONFLICT (flag_key) DO NOTHING;
