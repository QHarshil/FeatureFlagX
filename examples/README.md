# FeatureFlagX Examples

This directory contains example scripts and guides to help you understand and use FeatureFlagX.

## Contents

- `mock_api_server.py`: A simple server that simulates the FeatureFlagX API for local testing and development
- `sdk_demo.py`: A demonstration script showing how to use the Python SDK with various scenarios
- `DEMONSTRATION_GUIDE.md`: A comprehensive guide explaining the demonstration and how to use FeatureFlagX

## Running the Examples

### Mock API Server

The mock API server provides a simplified version of the FeatureFlagX API for testing and development purposes.

```bash
# Make the script executable
chmod +x mock_api_server.py

# Run the server
python mock_api_server.py
```

The server will start on port 8080 by default and provide the following endpoints:
- `GET /flags`: Get all flags
- `GET /flags/{key}`: Get a specific flag
- `POST /flags`: Create a new flag
- `PUT /flags/{key}`: Update a flag
- `DELETE /flags/{key}`: Delete a flag
- `GET /flags/evaluate/{key}`: Evaluate a flag

### SDK Demo

The SDK demo script demonstrates how to use the FeatureFlagX Python SDK to interact with the API.

```bash
# Make the script executable
chmod +x sdk_demo.py

# Run the demo
python sdk_demo.py
```

The demo will:
1. Start the mock API server
2. Initialize the FeatureFlagX client
3. Demonstrate various flag evaluation scenarios
4. Show caching behavior
5. Provide a real-world usage example
6. Stop the mock API server

## Integration with Your Application

To integrate FeatureFlagX with your application, follow these steps:

1. Choose the appropriate SDK for your language (Java, TypeScript, or Python)
2. Install the SDK in your project
3. Initialize the FeatureFlagX client with your API configuration
4. Use the client to check if features are enabled throughout your code

For more detailed information, refer to the SDK-specific README files in the respective SDK directories.
