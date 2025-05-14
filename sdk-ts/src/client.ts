import axios, { AxiosInstance, AxiosError } from "axios";
import NodeCache from "node-cache";

export interface FeatureFlagClientConfig {
    baseUrl?: string;
    connectTimeoutSeconds?: number;
    readTimeoutSeconds?: number;
    cacheMaxSize?: number;
    cacheTtlSeconds?: number;
    defaultValueOnError?: boolean;
    // apiKey?: string; // Future consideration
}

const DEFAULT_CONFIG: Required<Omit<FeatureFlagClientConfig, "apiKey">> = {
    baseUrl: "http://localhost:8080",
    connectTimeoutSeconds: 5,
    readTimeoutSeconds: 5,
    cacheMaxSize: 1000,
    cacheTtlSeconds: 300, // 5 minutes
    defaultValueOnError: false,
};

export class FeatureFlagClient {
    private config: Required<Omit<FeatureFlagClientConfig, "apiKey">>;
    private httpClient: AxiosInstance;
    private cache: NodeCache;

    constructor(config?: FeatureFlagClientConfig) {
        this.config = { ...DEFAULT_CONFIG, ...config }; 
        this.httpClient = axios.create({
            baseURL: this.config.baseUrl,
            timeout: this.config.connectTimeoutSeconds * 1000, // Axios timeout is in ms
        });
        // if (this.config.apiKey) {
        //     this.httpClient.defaults.headers.common["Authorization"] = `Bearer ${this.config.apiKey}`;
        // }
        this.cache = new NodeCache({
            stdTTL: this.config.cacheTtlSeconds,
            maxKeys: this.config.cacheMaxSize,
            useClones: false, // For performance, as we are storing booleans
        });
    }

    private async makeRequest(flagKey: string, targetId?: string): Promise<boolean | null> {
        const endpoint = `/flags/evaluate/${flagKey}`;
        const params: { targetId?: string } = {};
        if (targetId) {
            params.targetId = targetId;
        }

        try {
            const response = await this.httpClient.get<boolean>(endpoint, {
                params,
                timeout: this.config.readTimeoutSeconds * 1000, // Override for read timeout
            });
            return response.data;
        } catch (error) {
            const axiosError = error as AxiosError;
            console.error(
                `Error fetching flag "${flagKey}" for target "${targetId}": ${axiosError.message}`
            );
            if (axiosError.response) {
                console.error(`API responded with status ${axiosError.response.status}: ${JSON.stringify(axiosError.response.data)}`);
            }
            return null;
        }
    }

    public async isEnabled(
        flagKey: string,
        targetId?: string,
        defaultValue?: boolean
    ): Promise<boolean> {
        if (!flagKey) {
            console.warn("flagKey cannot be empty.");
            return defaultValue !== undefined ? defaultValue : this.config.defaultValueOnError;
        }

        const cacheKey = `${flagKey}:${targetId || ""}`;
        const cachedValue = this.cache.get<boolean>(cacheKey);

        if (cachedValue !== undefined) {
            console.debug(
                `Flag "${flagKey}" for target "${targetId}" found in cache: ${cachedValue}`
            );
            return cachedValue;
        }

        const apiValue = await this.makeRequest(flagKey, targetId);

        if (apiValue !== null && typeof apiValue === "boolean") {
            console.debug(
                `Flag "${flagKey}" for target "${targetId}" fetched from API: ${apiValue}`
            );
            this.cache.set(cacheKey, apiValue);
            return apiValue;
        } else {
            console.warn(
                `Could not fetch flag "${flagKey}" for target "${targetId}" from API. Returning default.`
            );
            return defaultValue !== undefined ? defaultValue : this.config.defaultValueOnError;
        }
    }

    public invalidate(flagKey: string, targetId?: string): void {
        const cacheKey = `${flagKey}:${targetId || ""}`;
        this.cache.del(cacheKey);
        console.info(`Invalidated flag "${flagKey}" for target "${targetId}" from cache.`);
    }

    public clearCache(): void {
        this.cache.flushAll();
        console.info("Local flag cache cleared.");
    }
}

// Export an alias for the main class for easier top-level import
export { FeatureFlagClient as Client };

// Example Usage (for local testing, move to an example file or tests later)
// async function main() {
//     console.log("TypeScript SDK Example");
//     const clientConfig: FeatureFlagClientConfig = { baseUrl: "http://localhost:8080" };
//     const client = new FeatureFlagClient(clientConfig);

//     // Ensure a flag with key "ts-test-enabled" is created and enabled in your API
//     // POST /flags  {"key": "ts-test-enabled", "enabled": true, "config": "{}"}
//     console.log(`Is "ts-test-enabled" enabled? ${await client.isEnabled("ts-test-enabled")}`);

//     // Ensure a flag with key "ts-test-disabled" is created and disabled in your API
//     // POST /flags  {"key": "ts-test-disabled", "enabled": false, "config": "{}"}
//     console.log(`Is "ts-test-disabled" enabled? ${await client.isEnabled("ts-test-disabled")}`);

//     console.log(`Is "non-existent-flag" enabled? ${await client.isEnabled("non-existent-flag")}`);
//     console.log(
//         `Is "non-existent-flag" enabled (with specific default true)? ${await client.isEnabled(
//             "non-existent-flag",
//             undefined,
//             true
//         )}`
//     );

//     // Test cache
//     console.log(
//         `Fetching "ts-test-enabled" again (should be cached): ${await client.isEnabled(
//             "ts-test-enabled"
//         )}`
//     );
//     await new Promise(resolve => setTimeout(resolve, 2000));
//     console.log(
//         `Fetching "ts-test-enabled" again after 2s: ${await client.isEnabled("ts-test-enabled")}`
//     );

//     client.invalidate("ts-test-enabled");
//     console.log(
//         `Fetching "ts-test-enabled" after invalidation (should fetch from API): ${await client.isEnabled(
//             "ts-test-enabled"
//         )}`
//     );

//     client.clearCache();
//     console.log(
//         `Fetching "ts-test-enabled" after cache clear (should fetch from API): ${await client.isEnabled(
//             "ts-test-enabled"
//         )}`
//     );
// }

// if (require.main === module) {
//     main().catch(console.error);
// }

