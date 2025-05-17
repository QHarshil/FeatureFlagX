import axios, { AxiosError, AxiosResponse } from "axios";
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
            has: jest.fn(),
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
        defaultValueOnError: false, // Explicitly set for clarity in tests
    };

    beforeEach(() => {
        // Reset mocks before each test
        mockedAxios.create = jest.fn(() => mockedAxios);
        // Make default mock throw if called without specific setup in a test
        mockedAxios.get = jest.fn(); // Default mock for get, resolves to undefined
        mockCacheGet.mockReset();
        mockCacheSet.mockReset();
        mockCacheDel.mockReset();
        mockCacheFlushAll.mockReset();
        client = new FeatureFlagClient(baseConfig);
    });

    test("isEnabled fetches from API on cache miss", async () => {
        mockCacheGet.mockReturnValue(undefined);
        mockedAxios.get.mockResolvedValue({ data: true, status: 200, statusText: "OK", headers: {}, config: {} as any } as AxiosResponse<boolean>);

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
        // Ensure axios.get is NOT called. If it IS called, the default mock in beforeEach will throw.

        const result = await client.isEnabled(flagKey, targetId);

        expect(result).toBe(false);
        expect(mockCacheGet).toHaveBeenCalledWith(`${flagKey}:${targetId}`);
        expect(mockedAxios.get).not.toHaveBeenCalled();
        expect(mockCacheSet).not.toHaveBeenCalled();
    });

    test("isEnabled API generic error returns default value", async () => {
        mockCacheGet.mockReturnValue(undefined);
        const genericError = new Error("API Unreachable");
        // Explicitly cast to any to satisfy mockRejectedValue, though it's not a true AxiosError
        mockedAxios.get.mockRejectedValue(genericError as any);
        const flagKey = "error-feature";

        const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
        const consoleWarnSpy = jest.spyOn(console, "warn").mockImplementation(() => {});

        let result = await client.isEnabled(flagKey);
        expect(result).toBe(false);
        expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining(`Error fetching flag "${flagKey}" for target "undefined": ${genericError.message}`));
        expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining(`Could not fetch flag "${flagKey}"`));

        result = await client.isEnabled(flagKey, undefined, true);
        expect(result).toBe(true);
        expect(mockedAxios.get).toHaveBeenCalledTimes(2);
        
        consoleErrorSpy.mockRestore();
        consoleWarnSpy.mockRestore();
    });

    test("isEnabled returns default value when API response.data is not boolean", async () => {
        mockCacheGet.mockReturnValue(undefined);
        mockedAxios.get.mockResolvedValue({ data: { error: "some error object" } as any, status: 200, statusText: "OK", headers: {}, config: {} as any } as AxiosResponse<any>);

        const flagKey = "non-boolean-response-feature";
        const consoleWarnSpy = jest.spyOn(console, "warn").mockImplementation(() => {});

        let result = await client.isEnabled(flagKey);
        expect(result).toBe(false);
        expect(mockedAxios.get).toHaveBeenCalledWith(`/flags/evaluate/${flagKey}`, expect.anything());
        expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining(`Could not fetch flag "${flagKey}"`));

        consoleWarnSpy.mockRestore();
    });

    test("isEnabled returns default value on API HTTP error (e.g., 500)", async () => {
        mockCacheGet.mockReturnValue(undefined);
        const mockHttpError = {
            isAxiosError: true,
            message: "Request failed with status code 500",
            response: {
                status: 500,
                data: { error: "Server Error details" },
                headers: {},
                config: {} as any,
                statusText: "Internal Server Error"
            },
            config: {} as any,
            name: 'AxiosError',
            toJSON: () => ({})
        } as AxiosError;
        mockedAxios.get.mockRejectedValue(mockHttpError);

        const flagKey = "http-error-feature";
        const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
        const consoleWarnSpy = jest.spyOn(console, "warn").mockImplementation(() => {});

        let result = await client.isEnabled(flagKey);
        expect(result).toBe(false);

        expect(mockedAxios.get).toHaveBeenCalledWith(`/flags/evaluate/${flagKey}`, expect.anything());
        expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining(`Error fetching flag "${flagKey}" for target "undefined": ${mockHttpError.message}`));
        expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining(`API responded with status 500: ${JSON.stringify(mockHttpError.response?.data)}`));
        expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining(`Could not fetch flag "${flagKey}"`));
        
        consoleErrorSpy.mockRestore();
        consoleWarnSpy.mockRestore();
    });

    test("cache expiration leads to API refetch", async () => {
        const flagKey = "expiring-feature";

        mockCacheGet.mockReturnValueOnce(undefined); // First call: miss
        mockedAxios.get.mockResolvedValueOnce({ data: true, status:200, config:{} as any, headers:{}, statusText:"OK" } as AxiosResponse<boolean>);
        await client.isEnabled(flagKey);
        expect(mockedAxios.get).toHaveBeenCalledTimes(1);
        expect(mockCacheSet).toHaveBeenCalledWith(`${flagKey}:`, true);

        mockCacheGet.mockReturnValueOnce(true); // Simulate cache hit
        await client.isEnabled(flagKey);
        expect(mockedAxios.get).toHaveBeenCalledTimes(1); 

        mockCacheGet.mockReturnValueOnce(undefined); // Simulate cache expiry
        mockedAxios.get.mockResolvedValueOnce({ data: false, status:200, config:{} as any, headers:{}, statusText:"OK" } as AxiosResponse<boolean>); 
        const resultAfterExpiry = await client.isEnabled(flagKey);
        expect(resultAfterExpiry).toBe(false);
        expect(mockedAxios.get).toHaveBeenCalledTimes(2); 
        expect(mockCacheSet).toHaveBeenLastCalledWith(`${flagKey}:`, false);
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

    test("isEnabled with empty flagKey returns default value and warns", async () => {
        const consoleWarnSpy = jest.spyOn(console, "warn").mockImplementation(() => {});
        let result = await client.isEnabled("");
        expect(result).toBe(false);
        result = await client.isEnabled("", undefined, true);
        expect(result).toBe(true);
        expect(consoleWarnSpy).toHaveBeenCalledWith("flagKey cannot be empty.");
        expect(mockedAxios.get).not.toHaveBeenCalled(); // API should not be called for empty flagKey
        consoleWarnSpy.mockRestore();
    });

    test("isEnabled without targetId works correctly", async () => {
        mockCacheGet.mockReturnValue(undefined);
        mockedAxios.get.mockResolvedValue({ data: true, status:200, config:{} as any, headers:{}, statusText:"OK" } as AxiosResponse<boolean>);
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

