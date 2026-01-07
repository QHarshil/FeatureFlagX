# FeatureFlagX

Lightweight feature flag service built with Java and Spring Boot. Centralized flag management with Redis caching and PostgreSQL persistence.

## Quick Start

```bash
git clone https://github.com/yourname/featureflagx.git
cd featureflagx
docker-compose up --build
```

Verify:
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## API

### Evaluate Flag
```bash
curl http://localhost:8080/api/v1/flags/evaluate/welcome-banner
# true
```

### CRUD Operations
```bash
# List all
curl http://localhost:8080/api/v1/flags

# Create
curl -X POST http://localhost:8080/api/v1/flags \
  -H "Content-Type: application/json" \
  -d '{"key": "new-checkout-flow", "enabled": true, "config": "{\"version\": \"v2\"}"}'

# Update
curl -X PUT http://localhost:8080/api/v1/flags/new-checkout-flow \
  -H "Content-Type: application/json" \
  -d '{"key": "new-checkout-flow", "enabled": false}'

# Delete
curl -X DELETE http://localhost:8080/api/v1/flags/new-checkout-flow

# Search
curl "http://localhost:8080/api/v1/flags?search=welcome&enabled=true"
```

## Architecture

```
Client → API Service → Redis Cache → PostgreSQL (on cache miss)
```

| Component | Technology | Purpose |
|-----------|------------|---------|
| API | Java 11, Spring Boot | RESTful flag management |
| Cache | Redis 7 | Low-latency flag evaluation |
| Database | PostgreSQL 15 | Persistent storage |

**Fault tolerance:** If Redis is unavailable, the service falls back to PostgreSQL automatically.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `featureflagx` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `password` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |

## Flag Schema

```json
{
  "key": "feature-name",
  "enabled": true,
  "config": "{\"version\": \"v1\"}",
  "createdAt": "2023-10-27T10:00:00",
  "updatedAt": "2023-10-27T10:05:00"
}
```

**Validation:**
- `key`: kebab-case, 1-255 characters
- `config`: optional JSON, max 10,000 characters

## Project Structure

```
featureflagx/
├── docker-compose.yml
├── init-db.sql
└── api/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/featureflagx/
        ├── Application.java
        ├── controller/
        ├── dto/
        ├── model/
        ├── repository/
        └── service/
```

## Development

```bash
cd api
mvn test          # Run tests
mvn clean package # Build JAR
```

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/flags` | GET | List flags (supports `?search=` and `?enabled=`) |
| `/api/v1/flags` | POST | Create flag |
| `/api/v1/flags/{key}` | PUT | Update flag |
| `/api/v1/flags/{key}` | DELETE | Delete flag |
| `/api/v1/flags/evaluate/{key}` | GET | Evaluate flag (returns `true`/`false`) |
| `/actuator/health` | GET | Health check |

## License

MIT
