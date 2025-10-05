# FeatureFlagX API Documentation

This document provides detailed information about the FeatureFlagX REST API endpoints.

## Base URL

```
http://localhost:8080/api/v1
```

## Endpoints

### 1. Flag Evaluation

#### `GET /flags/evaluate/{key}`

Evaluates a feature flag and returns whether it's enabled. This endpoint is optimized for high performance and is publicly accessible.

**Parameters:**
- `key` (path): The flag key to evaluate
- `targetId` (query, optional): User/system identifier for future targeting features

**Response:**
- `200 OK`: Returns `true` or `false`
- Content-Type: `application/json`

**Examples:**
```bash
# Basic evaluation
curl http://localhost:8080/api/v1/flags/evaluate/welcome-banner
# Response: true

# With target ID (for future use)
curl "http://localhost:8080/api/v1/flags/evaluate/beta-features?targetId=user123"
# Response: false
```

### 2. Flag Management

#### `GET /flags`

Retrieves all feature flags with optional filtering.

**Query Parameters:**
- `search` (optional): Filter flags by key containing this text
- `enabled` (optional): Filter by enabled status (`true` or `false`)

**Response:**
```json
[
  {
    "key": "welcome-banner",
    "enabled": true,
    "config": "{\"message\": \"Welcome!\"}",
    "createdAt": "2023-10-27T10:00:00",
    "updatedAt": "2023-10-27T10:00:00"
  }
]
```

**Examples:**
```bash
# Get all flags
curl http://localhost:8080/api/v1/flags

# Search flags
curl "http://localhost:8080/api/v1/flags?search=welcome"

# Get only enabled flags
curl "http://localhost:8080/api/v1/flags?enabled=true"
```

#### `GET /flags/{key}`

Retrieves a specific feature flag.

**Parameters:**
- `key` (path): The flag key

**Response:**
- `200 OK`: Flag object
- `404 Not Found`: Flag doesn't exist

**Example:**
```bash
curl http://localhost:8080/api/v1/flags/welcome-banner
```

#### `POST /flags`

Creates a new feature flag.

**Request Body:**
```json
{
  "key": "new-feature",
  "enabled": true,
  "config": "{\"version\": \"v1\"}"
}
```

**Response:**
- `201 Created`: Flag created successfully
- `400 Bad Request`: Validation error or flag already exists

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/flags \
  -H "Content-Type: application/json" \
  -d '{
    "key": "new-checkout-flow",
    "enabled": true,
    "config": "{\"timeout\": 5000}"
  }'
```

#### `PUT /flags/{key}`

Updates an existing feature flag.

**Parameters:**
- `key` (path): The flag key to update

**Request Body:**
```json
{
  "key": "new-feature",
  "enabled": false,
  "config": "{\"version\": \"v2\"}"
}
```

**Response:**
- `200 OK`: Flag updated successfully
- `404 Not Found`: Flag doesn't exist
- `400 Bad Request`: Validation error

**Example:**
```bash
curl -X PUT http://localhost:8080/api/v1/flags/new-checkout-flow \
  -H "Content-Type: application/json" \
  -d '{
    "key": "new-checkout-flow",
    "enabled": false,
    "config": "{\"version\": \"v1\"}"
  }'
```

#### `DELETE /flags/{key}`

Deletes a feature flag.

**Parameters:**
- `key` (path): The flag key to delete

**Response:**
- `204 No Content`: Flag deleted successfully
- `404 Not Found`: Flag doesn't exist

**Example:**
```bash
curl -X DELETE http://localhost:8080/api/v1/flags/new-checkout-flow
```

## Data Models

### Flag Object

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| `key` | String | Unique flag identifier | Required, 1-255 chars, kebab-case |
| `enabled` | Boolean | Whether flag is active | Required |
| `config` | String | Optional JSON configuration | Optional, max 10,000 chars |
| `createdAt` | DateTime | Creation timestamp | Auto-generated |
| `updatedAt` | DateTime | Last update timestamp | Auto-generated |

### Validation Rules

#### Flag Key Format
- Must follow kebab-case: lowercase letters, numbers, and hyphens only
- Examples: `welcome-banner`, `beta-features-v2`, `checkout-flow`
- Invalid: `WelcomeBanner`, `beta_features`, `checkout.flow`

#### Config Field
- Optional JSON string
- Maximum 10,000 characters
- Must be valid JSON if provided
- Examples: `"{\"version\": \"v1\"}"`, `"{\"timeout\": 5000, \"retries\": 3}"`

## Error Responses

All error responses follow this format:

```json
{
  "timestamp": "2023-10-27T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Flag key must follow kebab-case format",
  "path": "/api/v1/flags"
}
```

### Common Error Scenarios

| Scenario | Status Code | Message |
|----------|-------------|---------|
| Flag not found | 404 | "Flag not found: {key}" |
| Flag already exists | 400 | "Flag already exists: {key}" |
| Invalid key format | 400 | "Flag key must follow kebab-case format" |
| Config too long | 400 | "Config must not exceed 10000 characters" |
| Missing required field | 400 | "Flag key cannot be blank" |

## Performance Characteristics

### Caching Strategy

The service implements a two-tier caching strategy:

1. **Redis Cache**: Primary cache with 5-minute TTL
2. **Database Fallback**: PostgreSQL as the source of truth

### Response Times

| Operation | Typical Response Time |
|-----------|----------------------|
| Flag Evaluation (cache hit) | < 5ms |
| Flag Evaluation (cache miss) | < 50ms |
| Flag Management | < 100ms |

### Rate Limits

Currently, no rate limits are enforced, but the service is designed to handle:
- **Evaluation**: 10,000+ requests/second
- **Management**: 1,000+ requests/second

## Health Monitoring

### Health Check Endpoint

```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### Application Info

```bash
curl http://localhost:8080/actuator/info
```

## Integration Examples

### Java Client

```java
// Simple HTTP client example
public class FeatureFlagClient {
    private final String baseUrl = "http://localhost:8080/api/v1";
    
    public boolean isEnabled(String flagKey) {
        try {
            String response = httpGet(baseUrl + "/flags/evaluate/" + flagKey);
            return Boolean.parseBoolean(response);
        } catch (Exception e) {
            return false; // Fail safe
        }
    }
}
```

### JavaScript/Node.js Client

```javascript
class FeatureFlagClient {
    constructor(baseUrl = 'http://localhost:8080/api/v1') {
        this.baseUrl = baseUrl;
    }
    
    async isEnabled(flagKey) {
        try {
            const response = await fetch(`${this.baseUrl}/flags/evaluate/${flagKey}`);
            return await response.json();
        } catch (error) {
            return false; // Fail safe
        }
    }
}
```

### Python Client

```python
import requests

class FeatureFlagClient:
    def __init__(self, base_url="http://localhost:8080/api/v1"):
        self.base_url = base_url
    
    def is_enabled(self, flag_key):
        try:
            response = requests.get(f"{self.base_url}/flags/evaluate/{flag_key}")
            return response.json()
        except:
            return False  # Fail safe
```

## Best Practices

### Flag Naming

- Use descriptive, kebab-case names: `new-checkout-flow`, `enhanced-search`
- Include version when appropriate: `payment-gateway-v2`
- Avoid abbreviations: `user-dashboard` not `usr-dash`

### Configuration Management

- Keep config JSON simple and flat when possible
- Use meaningful keys: `{"timeout": 5000}` not `{"t": 5000}`
- Validate JSON before sending to API

### Error Handling

- Always implement fallback behavior for flag evaluation
- Log errors but don't fail application flow
- Use circuit breaker pattern for high-traffic applications

### Performance Optimization

- Cache flag values in your application when appropriate
- Use the evaluation endpoint for real-time checks
- Batch management operations when possible
