# FeatureFlagX TypeScript SDK

This is the official TypeScript SDK for the FeatureFlagX service. It allows your TypeScript (and JavaScript) applications to easily evaluate feature flags managed by the FeatureFlagX API. This SDK is primarily designed for Node.js environments but can be adapted for browser usage with appropriate polyfills or a different HTTP client if needed.

## 1. Installation

You can install the SDK using npm or yarn:

```bash
npm install featureflagx-sdk-ts
# or
yarn add featureflagx-sdk-ts
```

(Note: This assumes the package will be published to npm with the name `featureflagx-sdk-ts`. For local development, you can link the package or install it from the `sdk-ts` directory.)

## 2. Requirements

- Node.js (LTS versions recommended, e.g., 16.x, 18.x, 20.x)
- `axios` (for HTTP communication)
- `node-cache` (for in-memory caching)

These dependencies are listed in `package.json` and will be installed automatically.

## 3. Configuration

The SDK client is configured using the `FeatureFlagClientConfig` interface. You can customize the following parameters:

-   `baseUrl` (string, optional): The base URL of your FeatureFlagX API. Defaults to `"http://localhost:8080"`.
-   `connectTimeoutSeconds` (number, optional): Timeout for establishing a connection to the API in seconds. Defaults to `5`.
-   `readTimeoutSeconds` (number, optional): Timeout for receiving data from the API in seconds. Defaults to `5`.
-   `cacheMaxSize` (number, optional): The maximum number of flag evaluations to store in the local cache. Defaults to `1000`.
-   `cacheTtlSeconds` (number, optional): The time-to-live for cached flag evaluations in seconds. Defaults to `300` (5 minutes).
-   `defaultValueOnError` (boolean, optional): The default boolean value to return if an error occurs during flag evaluation or if the flag is not found and no specific default is provided to `isEnabled`. Defaults to `False`.

**Example Configuration:**

```typescript
import { FeatureFlagClient, FeatureFlagClientConfig } from "featureflagx-sdk-ts"; // Or your package name

// Configure the client
const config: FeatureFlagClientConfig = {
    baseUrl: "https://your-featureflagx-api.example.com",
    cacheTtlSeconds: 600, // Cache flags for 10 minutes
    defaultValueOnError: false,
};

// Initialize the client
const client = new FeatureFlagClient(config);
```

## 4. Usage

The primary method for checking a flag is `isEnabled()`, which returns a Promise.

```typescript
import { FeatureFlagClient, FeatureFlagClientConfig } from "featureflagx-sdk-ts";

async function checkFeatures() {
    // 1. Configure and initialize the client
    const clientConfig: FeatureFlagClientConfig = {
        baseUrl: "http://localhost:8080", // Ensure your FeatureFlagX API is running here
    };
    const client = new FeatureFlagClient(clientConfig);

    // 2. Define your flag key and an optional target identifier
    const flagKeyNewCheckout = "new-checkout-flow";
    const userId = "user-abc-premium";

    // 3. Evaluate the flag
    // The last argument is a default value if the flag can't be evaluated or is not found.
    // If this specific defaultValue is not provided, the client.config.defaultValueOnError is used.
    const isNewCheckoutEnabled = await client.isEnabled(
        flagKeyNewCheckout,
        userId,
        false // Default value for this specific call
    );

    if (isNewCheckoutEnabled) {
        console.log(`User ${userId} gets the new checkout flow!`);
        // ... implement new checkout logic ...
    } else {
        console.log(`User ${userId} gets the old checkout flow.`);
        // ... implement old checkout logic ...
    }

    // Example: Evaluating a flag that might not exist, relying on client-level default
    const nonExistentFlagKey = "experimental-feature-x";
    const isExperimentalFeatureActive = await client.isEnabled(nonExistentFlagKey);
    console.log(
        `Is "${nonExistentFlagKey}" active? ${isExperimentalFeatureActive}`
    ); // Will likely be false due to defaultValueOnError

    // Example: Evaluating a flag with a specific default if it doesn't exist
    const isAnotherFeatureActive = await client.isEnabled(
        "another-shiny-feature",
        undefined, // No targetId for this one
        true
    );
    console.log(
        `Is "another-shiny-feature" active (with specific default true)? ${isAnotherFeatureActive}`
    );
}

checkFeatures().catch(console.error);
```

### 4.1. Caching

The SDK uses an in-memory TTL cache (`node-cache`) to store flag evaluations. This reduces the number of HTTP requests to the FeatureFlagX API and improves performance.

-   **Cache Key:** The cache key is a combination of the `flag_key` and `target_id` (if provided).
-   **Cache Invalidation:**
    -   You can invalidate a specific flag from the cache:
        ```typescript
        client.invalidate("my-flag-key", "some-user");
        ```
    -   You can clear the entire local cache:
        ```typescript
        client.clearCache();
        ```
    Flags are automatically evicted from the cache when their TTL (Time-To-Live), defined by `cacheTtlSeconds`, expires.

### 4.2. Error Handling

-   The `isEnabled` method is designed to be resilient. If the API is unreachable, returns an error, or if the flag key is not found, it will return the `defaultValue` provided to the method, or fall back to the `defaultValueOnError` specified in the `FeatureFlagClientConfig`.
-   Errors during API communication (e.g., network issues, HTTP errors) are logged to the console by the SDK. You can adapt this logging if needed by modifying the SDK or using a more sophisticated logging library in your application.

## 5. Building the SDK

To compile the TypeScript code to JavaScript, run:

```bash
npm run build
# or
yarn build
```

This will use the `tsc` command (TypeScript compiler) as defined in your `package.json` and `tsconfig.json`, typically outputting files to a `dist` directory.

## 6. Development and Testing (for contributors)

1.  **Clone the main `featureflagx` repository.**
2.  **Navigate to the `sdk-ts` directory.**
3.  **Install dependencies:**
    ```bash
    npm install
    # or
    yarn install
    ```
4.  **Running Tests:**
    A testing framework like Jest or Mocha should be set up. Tests would typically be located in a `tests` or `__tests__` subdirectory.
    ```bash
    npm test
    # or
    yarn test
    ```
    (Note: The `test` script in `package.json` needs to be configured for your chosen test runner.)

