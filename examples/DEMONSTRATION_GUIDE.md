# FeatureFlagX Demonstration Guide

This document provides a guide to the FeatureFlagX demonstration files and how to run them.

## Overview

FeatureFlagX is a feature flag management system that allows developers to safely roll out new features, conduct A/B tests, and manage application functionality without requiring redeployments.

This demonstration showcases how to use the FeatureFlagX Python SDK to interact with the API and manage feature flags in your applications.

## Demonstration Files

The demonstration consists of two main components:

1. **Mock API Server** (`mock_api_server.py`): A simple HTTP server that simulates the FeatureFlagX API, providing endpoints for flag evaluation and management.

2. **SDK Demo Script** (`sdk_demo.py`): A Python script that demonstrates how to use the FeatureFlagX Python SDK to interact with the API.

## Running the Demonstration

To run the demonstration:

1. Ensure Python 3.6+ is installed on your system
2. Install the required dependencies:
   ```
   pip install cachetools requests
   ```
3. Make the scripts executable:
   ```
   chmod +x mock_api_server.py sdk_demo.py
   ```
4. Run the demonstration script:
   ```
   python sdk_demo.py
   ```

The demonstration script will:
- Start the mock API server
- Initialize the FeatureFlagX client
- Demonstrate various flag evaluation scenarios
- Show caching behavior
- Provide a real-world usage example
- Stop the mock API server

## Key Features Demonstrated

The demonstration showcases the following key features of FeatureFlagX:

1. **Flag Evaluation**: Checking if a feature flag is enabled or disabled
2. **User Targeting**: Enabling features for specific users based on targeting rules
3. **Default Values**: Handling non-existent flags with custom default values
4. **Caching**: Optimizing performance by caching flag values
5. **Cache Invalidation**: Clearing specific flags or the entire cache when needed

## Real-World Usage

In a real-world application, you would:

1. Initialize the FeatureFlagX client once at application startup
2. Use the client to check if features are enabled throughout your code
3. Implement conditional logic based on flag values

Example:

```python
# Initialize the client
config = FeatureFlagClientConfig(base_url="https://api.featureflagx.com")
client = FeatureFlagClient(config)

# In your application code
def process_checkout(user_id, cart):
    if client.is_enabled('new-checkout-flow', user_id):
        # Show the new checkout flow
        return new_checkout_process(cart)
    else:
        # Show the old checkout flow
        return old_checkout_process(cart)
```

## Project Structure

The full FeatureFlagX project includes:

- **API Server**: A Java Spring Boot application that provides the core API
- **SDKs**: Client libraries for Java, TypeScript, and Python
- **Database**: PostgreSQL for persistent storage of flag configurations
- **Cache**: Redis for high-performance flag evaluation

In a production environment, you would deploy the API server with PostgreSQL and Redis, and use one of the SDKs in your application to interact with the API.

## Additional Resources

For more information, refer to:

- `API_DOCUMENTATION.md`: Detailed API documentation
- `SECURITY.md`: Security guidelines and best practices
- `TEST_COVERAGE.md`: Overview of test coverage and quality metrics
- `README.md`: General project information and setup instructions
