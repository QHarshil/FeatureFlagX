# FeatureFlagX API Documentation

## Overview

FeatureFlagX is a comprehensive feature flagging solution designed to empower development teams to safely roll out new features, conduct A/B tests, and manage application functionality without requiring redeployments.

This document provides detailed information on how to use the FeatureFlagX API effectively and securely.

## Getting Started

### Prerequisites
- Docker and Docker Compose (for local development)
- Java 17+ and Maven (for building from source)

### Quick Start
1. Clone the repository
2. Run `docker-compose up` to start the API, PostgreSQL, and Redis
3. The API will be available at `http://localhost:8080`

## API Endpoints

### Flag Management

#### Create a Flag
```
POST /flags
```

**Request Body:**
```json
{
  "key": "my-new-feature",
  "enabled": true,
  "config": "{\"version\": \"v1\"}"
}
```

**Response:** (201 Created)
```json
{
  "key": "my-new-feature",
  "enabled": true,
  "config": "{\"version\": \"v1\"}",
  "createdAt": "2025-05-19T06:30:00Z",
  "updatedAt": "2025-05-19T06:30:00Z"
}
```

#### Get a Flag
```
GET /flags/{key}
```

**Response:** (200 OK)
```json
{
  "key": "my-new-feature",
  "enabled": true,
  "config": "{\"version\": \"v1\"}",
  "createdAt": "2025-05-19T06:30:00Z",
  "updatedAt": "2025-05-19T06:30:00Z"
}
```

#### Get All Flags
```
GET /flags
```

**Response:** (200 OK)
```json
[
  {
    "key": "feature-one",
    "enabled": true,
    "config": "{\"version\": \"v1\"}",
    "createdAt": "2025-05-19T06:30:00Z",
    "updatedAt": "2025-05-19T06:30:00Z"
  },
  {
    "key": "feature-two",
    "enabled": false,
    "config": "{\"version\": \"v2\"}",
    "createdAt": "2025-05-19T06:30:00Z",
    "updatedAt": "2025-05-19T06:30:00Z"
  }
]
```

#### Update a Flag
```
PUT /flags/{key}
```

**Request Body:**
```json
{
  "key": "my-new-feature",
  "enabled": false,
  "config": "{\"version\": \"v2\"}"
}
```

**Response:** (200 OK)
```json
{
  "key": "my-new-feature",
  "enabled": false,
  "config": "{\"version\": \"v2\"}",
  "createdAt": "2025-05-19T06:30:00Z",
  "updatedAt": "2025-05-19T06:35:00Z"
}
```

#### Delete a Flag
```
DELETE /flags/{key}
```

**Response:** (204 No Content)

### Flag Evaluation

#### Evaluate a Flag
```
GET /flags/evaluate/{key}?targetId={targetId}
```

**Response:** (200 OK)
```
true
```
or
```
false
```

The `targetId` parameter is optional and can be used for more complex targeting rules in future enhancements.

## Using the SDKs

FeatureFlagX provides SDKs for Java, TypeScript, and Python to simplify integration with your applications.

### Java SDK Example

```java
// Initialize the client
FeatureFlagClient.Config config = FeatureFlagClient.Config.builder()
    .apiBaseUrl("http://localhost:8080")
    .build();
FeatureFlagClient client = new FeatureFlagClient(config);

// Check if a feature is enabled
String flagKey = "new-checkout-flow";
String userId = "user-12345";
boolean isEnabled = client.isEnabled(flagKey, userId, false);

if (isEnabled) {
    // Show new checkout flow
} else {
    // Show old checkout flow
}
```

### TypeScript SDK Example

```typescript
// Initialize the client
const config: FeatureFlagClientConfig = {
    baseUrl: "http://localhost:8080",
    cacheTtlSeconds: 300
};
const client = new FeatureFlagClient(config);

// Check if a feature is enabled
const flagKey = "new-checkout-flow";
const userId = "user-12345";
const isEnabled = await client.isEnabled(flagKey, userId, false);

if (isEnabled) {
    // Show new checkout flow
} else {
    // Show old checkout flow
}
```

### Python SDK Example

```python
# Initialize the client
config = FeatureFlagClientConfig(
    base_url="http://localhost:8080",
    cache_ttl_seconds=300
)
client = FeatureFlagClient(config)

# Check if a feature is enabled
flag_key = "new-checkout-flow"
user_id = "user-12345"
is_enabled = client.is_enabled(flag_key, user_id, default=False)

if is_enabled:
    # Show new checkout flow
else:
    # Show old checkout flow
```

## Best Practices

### Flag Naming Conventions
- Use kebab-case for flag keys (e.g., `new-checkout-flow`, `beta-feature`)
- Use descriptive names that clearly indicate the feature's purpose
- Consider prefixing flags with their domain (e.g., `checkout-new-flow`, `admin-beta-feature`)

### Flag Configuration
- Store structured data in the `config` field as a JSON string
- Keep configuration minimal and focused on the feature's needs
- Consider versioning your configuration (e.g., `{"version": "v1", "data": {...}}`)

### Security Considerations
- Protect your API with proper authentication and authorization
- Consider using API keys for SDK authentication
- Limit access to flag management endpoints to authorized personnel
- Use HTTPS in production environments

### Performance Optimization
- SDKs implement local caching to reduce API calls
- Consider the cache TTL based on your update frequency needs
- For high-traffic applications, consider implementing a distributed cache

## Monitoring and Observability

The API exposes metrics and health endpoints for monitoring:

- Health check: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus endpoint: `GET /actuator/prometheus`

## Error Handling

The API returns standard HTTP status codes:

- 200 OK: Successful operation
- 201 Created: Resource created successfully
- 204 No Content: Resource deleted successfully
- 400 Bad Request: Invalid request parameters
- 404 Not Found: Resource not found
- 500 Internal Server Error: Server-side error

Error responses include a JSON body with details:

```json
{
  "timestamp": "2025-05-19T06:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid flag key format",
  "path": "/flags"
}
```

## Rate Limiting

The API implements rate limiting to protect against abuse:

- 100 requests per minute for flag evaluation endpoints
- 30 requests per minute for flag management endpoints

Rate limit headers are included in responses:
- `X-RateLimit-Limit`: The maximum number of requests allowed per minute
- `X-RateLimit-Remaining`: The number of requests remaining in the current window
- `X-RateLimit-Reset`: The time at which the current rate limit window resets

## Support

For issues or questions, please open an issue on the GitHub repository or contact the maintainers.
