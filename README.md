# FeatureFlagX: Production-Ready Feature Flag Service

FeatureFlagX is a lightweight, high-performance feature flag service built with Java and Spring Boot. It provides a centralized platform for managing application features, enabling safe rollouts, A/B testing, and dynamic configuration changes without requiring application redeployments.

## 🚀 Quick Start (5 Minutes)

### Prerequisites

- **Docker & Docker Compose**: [Install Docker](https://docs.docker.com/get-docker/)

### Launch the Service

1. **Clone or download this project**

2. **Navigate to the project directory**
   ```bash
   cd featureflagx-mvp
   ```

3. **Start all services**
   ```bash
   docker-compose up --build
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

   Expected response:
   ```json
   {"status":"UP"}
   ```

**That's it!** The service is now running and ready to use.

## 📋 API Usage Examples

### Evaluate a Flag (Public Endpoint)

Check if a feature is enabled. This endpoint is optimized for high performance and doesn't require authentication.

```bash
# Check the 'welcome-banner' flag (pre-seeded as enabled)
curl http://localhost:8080/api/v1/flags/evaluate/welcome-banner
# Returns: true

# Check the 'beta-features' flag (pre-seeded as disabled)
curl http://localhost:8080/api/v1/flags/evaluate/beta-features
# Returns: false

# Check a non-existent flag (returns false by default)
curl http://localhost:8080/api/v1/flags/evaluate/non-existent
# Returns: false
```

### Get All Flags

```bash
curl http://localhost:8080/api/v1/flags
```

### Create a New Flag

```bash
curl -X POST http://localhost:8080/api/v1/flags \
  -H "Content-Type: application/json" \
  -d '{
    "key": "new-checkout-flow",
    "enabled": true,
    "config": "{\"version\": \"v2\", \"timeout\": 5000}"
  }'
```

### Update a Flag

```bash
curl -X PUT http://localhost:8080/api/v1/flags/new-checkout-flow \
  -H "Content-Type: application/json" \
  -d '{
    "key": "new-checkout-flow",
    "enabled": false,
    "config": "{\"version\": \"v1\"}"
  }'
```

### Delete a Flag

```bash
curl -X DELETE http://localhost:8080/api/v1/flags/new-checkout-flow
```

### Search Flags

```bash
# Search for flags containing "welcome"
curl "http://localhost:8080/api/v1/flags?search=welcome"

# Get only enabled flags
curl "http://localhost:8080/api/v1/flags?enabled=true"

# Get only disabled flags
curl "http://localhost:8080/api/v1/flags?enabled=false"
```

## 🏗️ Architecture

The system consists of three main components:

| Component | Technology | Purpose |
|-----------|------------|---------|
| **API Service** | Java 11 + Spring Boot | RESTful API for flag management and evaluation |
| **Database** | PostgreSQL 15 | Persistent storage for flag configurations |
| **Cache** | Redis 7 | High-performance caching for flag evaluation |

### High-Level Flow

```
Client Request → API Service → Redis Cache (if available) → PostgreSQL (if cache miss) → Response
```

The service is designed to be **fault-tolerant**. If Redis is unavailable, the API will automatically fall back to PostgreSQL, ensuring your application continues to work.

## 🔧 Configuration

The service comes with sensible defaults but can be customized via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `featureflagx` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `password` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |

## 🧪 Testing

### Prerequisites for Local Development

- Java 11+
- Maven 3.6+

### Run Tests

```bash
cd api
mvn test
```

### Build from Source

```bash
cd api
mvn clean package
```

## 📊 Monitoring

The service exposes health and monitoring endpoints:

- **Health Check**: `GET /actuator/health`
- **Application Info**: `GET /actuator/info`

## 🔒 Security Features

- **Input Validation**: All API inputs are validated to prevent malformed data
- **Fail-Safe Defaults**: Non-existent flags return `false` by default
- **Container Security**: Runs as non-root user in Docker
- **Error Handling**: Comprehensive error handling with proper HTTP status codes

## 📁 Project Structure

```
featureflagx-mvp/
├── README.md                    # This file
├── docker-compose.yml           # Docker orchestration
├── init-db.sql                  # Database initialization
└── api/                         # Spring Boot application
    ├── Dockerfile
    ├── pom.xml                  # Maven dependencies
    └── src/
        ├── main/java/com/featureflagx/
        │   ├── Application.java         # Main application class
        │   ├── controller/              # REST controllers
        │   ├── dto/                     # Data transfer objects
        │   ├── model/                   # JPA entities
        │   ├── repository/              # Data repositories
        │   └── service/                 # Business logic
        └── test/                        # Unit tests
```

## 🚀 Production Deployment

For production deployment, consider:

1. **Environment Variables**: Override default passwords and configuration
2. **HTTPS**: Use a reverse proxy (nginx, ALB) to terminate SSL
3. **Monitoring**: Integrate with your monitoring stack via `/actuator` endpoints
4. **Scaling**: The service is stateless and can be horizontally scaled
5. **Database**: Use managed PostgreSQL and Redis services for high availability

## 📝 API Reference

### Flag Object

```json
{
  "key": "feature-name",           // Unique identifier (kebab-case)
  "enabled": true,                 // Whether the flag is enabled
  "config": "{\"version\": \"v1\"}", // Optional JSON configuration
  "createdAt": "2023-10-27T10:00:00",
  "updatedAt": "2023-10-27T10:05:00"
}
```

### Validation Rules

- **Flag Key**: Must be kebab-case (lowercase letters, numbers, hyphens only)
- **Config**: Optional, maximum 10,000 characters
- **Key Length**: 1-255 characters

### HTTP Status Codes

| Code | Meaning | When |
|------|---------|------|
| `200` | OK | Successful operation |
| `201` | Created | Flag created successfully |
| `204` | No Content | Flag deleted successfully |
| `400` | Bad Request | Invalid input or validation error |
| `404` | Not Found | Flag not found |
| `500` | Internal Server Error | Unexpected server error |

## 🤝 Contributing

This is a production-ready MVP. For enhancements:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Built with ❤️ for reliable feature flag management**
