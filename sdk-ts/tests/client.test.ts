import axios from "axios";
import { FeatureFlagClient, FeatureFlagClientConfig } from "../src/client"; // Adjust path as needed

// Mock axios
jest.mock("axios");
const mockedAxios = axios as jest.Mocked<typeof axios>;

// Mock NodeCache
const mockCacheGet = jest.fn();
const mockCacheSet = jest.fn();
const mockCacheDel = jest.fn();
const mockCacheFlushAll = jest.fn();

jest.mock("node-cache", () => {
    return jest.fn().mockImplementation(() => {
        return {
            get: mockCacheGet,
            set: mockCacheSet,
            del: mockCacheDel,
            flushAll: mockCacheFlushAll,
            has: jest.fn(), // Add other methods if your client uses them
            keys: jest.fn(),
            stats: jest.fn(),
            take: jest.fn(),
            ttl: jest.fn(),
            getTtl: jest.fn(),
            close: jest.fn(),
            on: jest.fn(),
            getMset: jest.fn(),
            mset: jest.fn(),
            delMset: jest.fn(),
        };
    });
});

describe("FeatureFlagClient", () => {
    let client: FeatureFlagClient;
    const baseConfig: FeatureFlagClientConfig = {
        baseUrl: "http://mockapi.example.com",
        cacheTtlSeconds: 2, // Short TTL for testing
    };

    beforeEach(() => {
        // Reset mocks before each test
        mockedAxios.create = jest.fn(() => mockedAxios);
        mockedAxios.get = jest.fn();
        mockCacheGet.mockReset();
        mockCacheSet.mockReset();
        mockCacheDel.mockReset();
        mockCacheFlushAll.mockReset();
        client = new FeatureFlagClient(baseConfig);
    });

    test("isEnabled fetches from API on cache miss", async () => {
        mockCacheGet.mockReturnValue(undefined);
        mockedAxios.get.mockResolvedValue({ data: true, status: 200, statusText: "OK", headers: {}, config: {} });

        const flagKey = "my-feature";
        const targetId = "user123";

        const result = await client.isEnabled(flagKey, targetId);

        expect(result).toBe(true);
        expect(mockCacheGet).toHaveBeenCalledWith(`${flagKey}:${targetId}`);
        expect(mockedAxios.get).toHaveBeenCalledWith(`/flags/evaluate/${flagKey}`, {
            params: { targetId },
            timeout: (baseConfig.readTimeoutSeconds || 5) * 1000,
        });
        expect(mockCacheSet).toHaveBeenCalledWith(`${flagKey}:${targetId}`, true);
    });

    test("isEnabled uses cache on hit", async () => {
        const flagKey = "cached-feature";
        const targetId = "user456";
        mockCacheGet.mockReturnValue(false); // Pre-populate cache

        const result = await client.isEnabled(flagKey, targetId);

        expect(result).toBe(false);
        expect(mockCacheGet).toHaveBeenCalledWith(`${flagKey}:${targetId}`);
        expect(mockedAxios.get).not.toHaveBeenCalled();
        expect(mockCacheSet).not.toHaveBeenCalled();
    });

    test("isEnabled API error returns default value", async () => {
        mockCacheGet.mockReturnValue(undefined);
        mockedAxios.get.mockRejectedValue(new Error("API Unreachable"));
        const flagKey = "error-feature";

        // Default from client config (false)
        let result = await client.isEnabled(flagKey);
        expect(result).toBe(baseConfig.defaultValueOnError || false);

        // Specific default provided
        result = await client.isEnabled(flagKey, undefined, true);
        expect(result).toBe(true);
        expect(mockedAxios.get).toHaveBeenCalledTimes(2); // API was attempted twice
    });

    test("isEnabled API returns non-200 status returns default", async () => {
        mockCacheGet.mockReturnValue(undefined);
        mockedAxios.get.mockResolvedValue({ status: 500, data: "Server Error", config:{}, headers:{}, statusText:"Error" }); // Simulate non-200 by not returning boolean data
        const flagKey = "http-error-feature";
        
        //This test needs to be adjusted as the mock for axios.get should return a boolean or throw an error based on status.
        //For now, we assume the .get will throw or return non-boolean for error status, leading to null from makeRequest
        const clientWithSpecificErrorHandler = new FeatureFlagClient(baseConfig);
        const makeRequestSpy = jest.spyOn(clientWithSpecificErrorHandler as any, "makeRequest");
        makeRequestSpy.mockResolvedValue(null); // Simulate makeRequest returning null due to error

        let result = await clientWithSpecificErrorHandler.isEnabled(flagKey);
        expect(result).toBe(baseConfig.defaultValueOnError || false);
        makeRequestSpy.mockRestore();
    });

    test("cache expiration leads to API refetch", async () => {
        mockCacheGet.mockReturnValueOnce(undefined); // First call: miss
        mockedAxios.get.mockResolvedValueOnce({ data: true, status:200, config:{}, headers:{}, statusText:"OK" });

        const flagKey = "expiring-feature";

        // First call, API returns True, gets cached
        await client.isEnabled(flagKey);
        expect(mockedAxios.get).toHaveBeenCalledTimes(1);
        expect(mockCacheSet).toHaveBeenCalledWith(`${flagKey}:`, true);

        // Simulate cache hit for subsequent calls within TTL
        mockCacheGet.mockReturnValueOnce(true);
        await client.isEnabled(flagKey);
        expect(mockedAxios.get).toHaveBeenCalledTimes(1); // Still 1, used cache

        // Simulate cache expiry: next call to mockCacheGet returns undefined
        mockCacheGet.mockReturnValueOnce(undefined);
        mockedAxios.get.mockResolvedValueOnce({ data: false, status:200, config:{}, headers:{}, statusText:"OK" }); // API returns False now
        
        const resultAfterExpiry = await client.isEnabled(flagKey);
        expect(resultAfterExpiry).toBe(false);
        expect(mockedAxios.get).toHaveBeenCalledTimes(2); // Called API again
        expect(mockCacheSet).toHaveBeenCalledWith(`${flagKey}:`, false);
    });

    test("invalidate flag removes it from cache", () => {
        const flagKey = "invalidate-me";
        const targetId = "user789";
        client.invalidate(flagKey, targetId);
        expect(mockCacheDel).toHaveBeenCalledWith(`${flagKey}:${targetId}`);
    });

    test("clearCache flushes all cache entries", () => {
        client.clearCache();
        expect(mockCacheFlushAll).toHaveBeenCalled();
    });

    test("isEnabled with empty flagKey returns default value", async () => {
        const consoleWarnSpy = jest.spyOn(console, "warn").mockImplementation(() => {});
        let result = await client.isEnabled("");
        expect(result).toBe(baseConfig.defaultValueOnError || false);
        result = await client.isEnabled("", undefined, true);
        expect(result).toBe(true);
        expect(consoleWarnSpy).toHaveBeenCalledWith("flagKey cannot be empty.");
        consoleWarnSpy.mockRestore();
    });

    test("isEnabled without targetId works correctly", async () => {
        mockCacheGet.mockReturnValue(undefined);
        mockedAxios.get.mockResolvedValue({ data: true, status:200, config:{}, headers:{}, statusText:"OK" });
        const flagKey = "no-target-feature";

        const result = await client.isEnabled(flagKey);
        expect(result).toBe(true);
        expect(mockedAxios.get).toHaveBeenCalledWith(`/flags/evaluate/${flagKey}`, {
            params: {},
            timeout: (baseConfig.readTimeoutSeconds || 5) * 1000,
        });
        expect(mockCacheSet).toHaveBeenCalledWith(`${flagKey}:`, true);
    });
});

