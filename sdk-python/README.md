# FeatureFlagX Python SDK

This is the official Python SDK for the FeatureFlagX service. It allows your Python applications to easily evaluate feature flags managed by the FeatureFlagX API.

## 1. Installation

You can install the SDK using pip:

```bash
pip install featureflagx-sdk
```

(Note: This assumes the package will be published to PyPI with the name `featureflagx-sdk`. For local development, you can install it from the `sdk-python` directory using `pip install .`)

## 2. Requirements

- Python 3.7+
- `requests` library (for HTTP communication)
- `cachetools` library (for in-memory caching)

These dependencies will be installed automatically when you install the SDK via pip.

## 3. Configuration

The SDK client is configured using the `FeatureFlagClientConfig` dataclass. You can customize the following parameters:

-   `base_url` (str): The base URL of your FeatureFlagX API. Defaults to `"http://localhost:8080"`.
-   `connect_timeout_seconds` (float): Timeout for establishing a connection to the API. Defaults to `5.0` seconds.
-   `read_timeout_seconds` (float): Timeout for receiving data from the API. Defaults to `5.0` seconds.
-   `cache_max_size` (int): The maximum number of flag evaluations to store in the local cache. Defaults to `1000`.
-   `cache_ttl_seconds` (int): The time-to-live for cached flag evaluations in seconds. Defaults to `300` (5 minutes).
-   `default_value_on_error` (bool): The default boolean value to return if an error occurs during flag evaluation or if the flag is not found and no specific default is provided to `is_enabled`. Defaults to `False`.

**Example Configuration:**

```python
from featureflagx.sdk.client import FeatureFlagClient, FeatureFlagClientConfig

# Configure the client
config = FeatureFlagClientConfig(
    base_url="https://your-featureflagx-api.example.com",
    cache_ttl_seconds=600,  # Cache flags for 10 minutes
    default_value_on_error=False
)

# Initialize the client
client = FeatureFlagClient(config)
```

## 4. Usage

The primary method for checking a flag is `is_enabled()`.

```python
import logging
from featureflagx.sdk.client import FeatureFlagClient, FeatureFlagClientConfig

# Optional: Configure logging to see SDK debug messages
logging.basicConfig(level=logging.INFO)
# For more detailed logs from the SDK itself:
# logging.getLogger("featureflagx.sdk.client").setLevel(logging.DEBUG)

# 1. Configure and initialize the client
client_config = FeatureFlagClientConfig(
    base_url="http://localhost:8080" # Ensure your FeatureFlagX API is running here
)
client = FeatureFlagClient(client_config)

# 2. Define your flag key and an optional target identifier
flag_key_new_dashboard = "new-dashboard-feature"
user_id = "user123-beta-tester"

# 3. Evaluate the flag
# The last argument is a default value if the flag can't be evaluated or is not found.
# If this specific default_value is not provided, the client.config.default_value_on_error is used.
is_new_dashboard_enabled = client.is_enabled(flag_key_new_dashboard, target_id=user_id, default_value=False)

if is_new_dashboard_enabled:
    print(f"User {user_id} sees the new dashboard!")
    # ... show new dashboard logic ...
else:
    print(f"User {user_id} sees the old dashboard.")
    # ... show old dashboard logic ...

# Example: Evaluating a flag that might not exist, relying on client-level default
non_existent_flag_key = "super-secret-feature"
is_secret_feature_active = client.is_enabled(non_existent_flag_key)
print(f"Is 	{non_existent_flag_key}	 active? {is_secret_feature_active}") # Will likely be False due to default_value_on_error

# Example: Evaluating a flag with a specific default if it doesn't exist
is_another_feature_active = client.is_enabled("another-feature", default_value=True)
print(f"Is 'another-feature' active (with specific default true)? {is_another_feature_active}")
```

### 4.1. Caching

The SDK uses an in-memory TTLCache (`cachetools.TTLCache`) to store flag evaluations. This reduces the number of HTTP requests to the FeatureFlagX API and improves performance.

-   **Cache Key:** The cache key is a combination of the `flag_key` and `target_id` (if provided).
-   **Cache Invalidation:**
    -   You can invalidate a specific flag from the cache:
        ```python
        client.invalidate("my-flag-key", target_id="some-user")
        ```
    -   You can clear the entire local cache:
        ```python
        client.clear_cache()
        ```
    Flags are automatically evicted from the cache when their TTL (Time-To-Live), defined by `cache_ttl_seconds`, expires.

### 4.2. Error Handling

-   The `is_enabled` method is designed to be resilient. If the API is unreachable, returns an error, or if the flag key is not found, it will return the `default_value` provided to the method, or fall back to the `default_value_on_error` specified in the `FeatureFlagClientConfig`.
-   Errors during API communication (e.g., network issues, HTTP errors) are logged by the SDK (uses Python's standard `logging` module). You can configure your application's logger to see these messages.

## 5. Development and Testing (for contributors)

1.  **Clone the main `featureflagx` repository.**
2.  **Navigate to the `sdk-python` directory.**
3.  **Set up a virtual environment (recommended):**
    ```bash
    python -m venv venv
    source venv/bin/activate  # On Windows: venv\Scripts\activate
    ```
4.  **Install dependencies (including development dependencies if specified in `setup.py` or a `requirements-dev.txt`):
    ```bash
    pip install -e .[dev] # If you add a [dev] extra in setup.py for test dependencies
    # or
    pip install -e .
    pip install pytest requests-mock # or other test dependencies
    ```
5.  **Running Tests:**
    Tests will be located in a `tests` subdirectory (e.g., `sdk-python/tests/`).
    ```bash
    pytest
    ```


