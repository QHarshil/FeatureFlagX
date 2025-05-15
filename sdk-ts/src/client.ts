import axios, { AxiosInstance, AxiosError, AxiosResponse } from "axios";
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
        } catch (error: any) {
            console.error(
                `Error fetching flag "${flagKey}" for target "${targetId}": ${error?.message || "Unknown error"}`
            );
            // Check if it looks like an AxiosError with a response object
            if (error && typeof error === 'object' && error.isAxiosError && error.response) {
                const axiosResponse = error.response as AxiosResponse;
                const status = axiosResponse.status || 'N/A';
                let responseDataStr = "No response data or data is not stringifiable.";
                if (axiosResponse.hasOwnProperty('data')) {
                    try {
                        responseDataStr = JSON.stringify(axiosResponse.data);
                    } catch (stringifyError: any) {
                        responseDataStr = `Error stringifying response data: ${stringifyError?.message || "Unknown stringify error"}`;
                    }
                }
                console.error(`API responded with status ${status}: ${responseDataStr}`);
            } else if (error && typeof error === 'object' && error.hasOwnProperty('message')) {
                console.error("Error details:", error.message);
            } else {
                console.error("Unknown error structure caught:", error);
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

export { FeatureFlagClient as Client };

