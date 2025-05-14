# FeatureFlagX Java SDK

This is the official Java SDK for the FeatureFlagX service. It allows your Java applications to easily evaluate feature flags managed by the FeatureFlagX API.

## 1. Installation

To use the FeatureFlagX Java SDK in your project, you need to add it as a dependency. The SDK uses OkHttp for HTTP communication and Caffeine for in-memory caching.

### Maven

Add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>com.featureflagx</groupId> <!-- Replace with actual groupId if different -->
    <artifactId>sdk-java</artifactId>    <!-- Replace with actual artifactId if different -->
    <version>0.1.0</version>         <!-- Replace with the latest version -->
</dependency>

<!-- You will also need to ensure OkHttp and Caffeine are available -->
<!-- If not transitively included, add them explicitly: -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version> <!-- Or the version used by the SDK -->
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version> <!-- Or the version used by the SDK -->
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.0</version> <!-- Or a compatible version -->
</dependency>
```

*(Please check the `sdk-java/pom.xml` for the exact dependencies and versions used by the SDK.)*

### Gradle

Add the following to your `build.gradle` file:

```gradle
implementation 'com.featureflagx:sdk-java:0.1.0' // Replace with actual coordinates and version

// If not transitively included, add them explicitly:
implementation 'com.squareup.okhttp3:okhttp:4.12.0' // Or the version used by the SDK
implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8' // Or the version used by the SDK
implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.0' // Or a compatible version
```

## 2. Configuration

The SDK client is configured using the `FeatureFlagClient.Config` class, which provides a builder pattern.

Available configuration options:

-   `apiBaseUrl` (String): The base URL of your FeatureFlagX API. Defaults to `"http://localhost:8080"`.
-   `connectTimeout` (Duration): Timeout for establishing a connection to the API. Defaults to `Duration.ofSeconds(5)`.
-   `readTimeout` (Duration): Timeout for receiving data from the API. Defaults to `Duration.ofSeconds(5)`.
-   `cacheMaxSize` (long): The maximum number of flag evaluations to store in the local Caffeine cache. Defaults to `1000`.
-   `cacheExpireAfterWrite` (Duration): The time-to-live for cached flag evaluations. Defaults to `Duration.ofMinutes(5)`.

**Example Configuration:**

```java
import com.featureflagx.sdk.FeatureFlagClient;
import java.time.Duration;

// Configure the client
FeatureFlagClient.Config sdkConfig = FeatureFlagClient.Config.builder()
    .apiBaseUrl("https://your-featureflagx-api.example.com")
    .connectTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(10))
    .cacheMaxSize(2000)
    .cacheExpireAfterWrite(Duration.ofMinutes(10))
    .build();

// Initialize the client
FeatureFlagClient client = new FeatureFlagClient(sdkConfig);
```

## 3. Usage

The primary method for checking a flag is `isEnabled()`.

```java
import com.featureflagx.sdk.FeatureFlagClient;
import java.time.Duration;

public class MyApp {
    public static void main(String[] args) {
        // 1. Configure and initialize the client
        FeatureFlagClient.Config sdkConfig = FeatureFlagClient.Config.builder()
            .apiBaseUrl("http://localhost:8080") // Ensure your FeatureFlagX API is running here
            .build();
        FeatureFlagClient client = new FeatureFlagClient(sdkConfig);

        // 2. Define your flag key and an optional target identifier
        String flagKeyForNewUI = "new-user-interface";
        String userId = "user-xyz-789"; // Can be null or empty if not used for targeting

        // 3. Evaluate the flag
        // The last argument is a default value if the flag can't be evaluated or is not found.
        boolean isNewUIEnabled = client.isEnabled(flagKeyForNewUI, userId, false);

        if (isNewUIEnabled) {
            System.out.println("User " + userId + " sees the new User Interface!");
            // ... implement new UI logic ...
        } else {
            System.out.println("User " + userId + " sees the old User Interface.");
            // ... implement old UI logic ...
        }

        // Example: Evaluating a flag that might not exist, relying on the default value
        String nonExistentFlagKey = "experimental-feature-alpha";
        boolean isExperimentalFeatureActive = client.isEnabled(nonExistentFlagKey, null, false);
        System.out.println(
            "Is '" + nonExistentFlagKey + "' active? " + isExperimentalFeatureActive
        ); // Will be false due to the provided default
    }
}
```

### Overloaded `isEnabled` methods:

-   `isEnabled(String flagKey, String targetId, boolean defaultValue)`: Evaluates the flag. If an error occurs or the flag is not found, `defaultValue` is returned.
-   `isEnabled(String flagKey, String targetId)`: A convenience method that calls the above with `defaultValue` set to `false`.

### 3.1. Caching

The SDK uses an in-memory Caffeine cache to store flag evaluations. This reduces the number of HTTP requests to the FeatureFlagX API and improves performance.

-   **Cache Key:** The `flagKey` is used as the cache key. Note: The current implementation of the Java SDK caches based on `flagKey` only, not a combination of `flagKey` and `targetId`. This means if targeting rules on the server-side depend on `targetId` to return different boolean values for the *same flag key*, the cache might return a stale value for a different `targetId` until the cache entry for that `flagKey` expires or is invalidated.
-   **Cache Invalidation:**
    -   You can invalidate a specific flag from the cache:
        ```java
        client.invalidateFlag("my-flag-key-to-refresh");
        ```
    -   You can clear the entire local cache:
        ```java
        client.clearCache();
        ```
    Flags are automatically evicted from the cache when their `cacheExpireAfterWrite` duration is met.

### 3.2. Error Handling

-   The `isEnabled` method is designed to be resilient. If the API is unreachable, returns an error, or if the flag key is not found, it will log an error (currently to `System.err` in the provided snippet, this might be improved with a proper logging facade) and return the `defaultValue`.
-   The SDK caches the `defaultValue` on error to prevent repeatedly hitting a failing API for the same flag.

## 4. Building the SDK (from source)

If you need to build the SDK from source:

1.  **Clone the main `featureflagx` repository.**
2.  **Navigate to the `sdk-java` directory.**
3.  **Build with Maven:**
    ```bash
    mvn clean package
    ```
    This will compile the SDK and run any tests, producing a JAR file in the `target/` directory.

## 5. Considerations

-   **Logging:** The current example uses `System.err.println` for some error logging. For production use, we need to consider integrating a proper logging facade like SLF4J.
-   **Thread Safety:** The `FeatureFlagClient` should be thread-safe for use as a singleton or shared instance due to the thread-safe nature of OkHttpClient and Caffeine.
-   **Targeting and Cache:** As noted, the current Java SDK's cache key is based solely on `flagKey`. If your flag evaluations heavily depend on `targetId` producing different results for the same `flagKey`, you might need to adjust the caching strategy or use shorter TTLs / more frequent invalidations for those specific flags.


