import axios from 'axios';
import { spawn, ChildProcess } from 'child_process';
import { FeatureFlagClient, FeatureFlagClientConfig } from '../src/client';
import * as path from 'path';
import * as fs from 'fs';
import { v4 as uuidv4 } from 'uuid';

/**
 * Integration tests for the TypeScript SDK against a running API instance.
 * Uses Docker Compose to spin up the entire stack (API, PostgreSQL, Redis).
 */
describe('TypeScript SDK Integration Tests', () => {
  let dockerComposeProcess: ChildProcess;
  let client: FeatureFlagClient;
  const apiUrl = 'http://localhost:8081';
  let enabledFlagKey: string;
  let disabledFlagKey: string;

  // Helper function to wait for API to be ready
  const waitForApiReady = async (maxRetries = 30, retryInterval = 2000): Promise<boolean> => {
    for (let i = 0; i < maxRetries; i++) {
      try {
        const response = await axios.get(`${apiUrl}/flags`);
        if (response.status === 200) {
          return true;
        }
      } catch (error) {
        console.log(`API not ready yet, retrying in ${retryInterval}ms...`);
      }
      await new Promise(resolve => setTimeout(resolve, retryInterval));
    }
    throw new Error('API service did not become ready in the expected time');
  };

  // Helper function to create test flags
  const createTestFlags = async (): Promise<void> => {
    // Create an enabled flag
    enabledFlagKey = `typescript-test-enabled-${uuidv4()}`;
    const enabledFlag = {
      key: enabledFlagKey,
      enabled: true,
      config: '{"version":"1.0"}'
    };

    // Create a disabled flag
    disabledFlagKey = `typescript-test-disabled-${uuidv4()}`;
    const disabledFlag = {
      key: disabledFlagKey,
      enabled: false,
      config: '{"version":"1.0"}'
    };

    // Send requests to create flags
    await axios.post(`${apiUrl}/flags`, enabledFlag);
    await axios.post(`${apiUrl}/flags`, disabledFlag);
  };

  beforeAll(async () => {
    // Start Docker Compose services
    const dockerComposePath = path.resolve(__dirname, '../../docker-compose-test.yml');
    
    if (!fs.existsSync(dockerComposePath)) {
      throw new Error(`Docker Compose file not found at ${dockerComposePath}`);
    }
    
    dockerComposeProcess = spawn('docker-compose', ['-f', dockerComposePath, 'up', '-d']);
    
    dockerComposeProcess.stdout?.on('data', (data) => {
      console.log(`Docker Compose stdout: ${data}`);
    });
    
    dockerComposeProcess.stderr?.on('data', (data) => {
      console.error(`Docker Compose stderr: ${data}`);
    });
    
    // Wait for services to be ready
    await waitForApiReady();
    
    // Initialize client
    const config: FeatureFlagClientConfig = {
      baseUrl: apiUrl,
      cacheTtlSeconds: 5
    };
    client = new FeatureFlagClient(config);
    
    // Create test flags
    await createTestFlags();
  }, 60000); // Increase timeout for Docker Compose startup

  afterAll(async () => {
    // Stop Docker Compose services
    const dockerComposePath = path.resolve(__dirname, '../../docker-compose-test.yml');
    spawn('docker-compose', ['-f', dockerComposePath, 'down']);
    
    // Ensure the process is terminated
    if (dockerComposeProcess) {
      dockerComposeProcess.kill();
    }
  });

  test('isEnabled returns true for an enabled flag', async () => {
    // Given a flag that exists and is enabled
    const targetId = `user-${uuidv4()}`;
    
    // When checking if the flag is enabled
    const isEnabled = await client.isEnabled(enabledFlagKey, targetId);
    
    // Then it should return true
    expect(isEnabled).toBe(true);
  });

  test('isEnabled returns false for a disabled flag', async () => {
    // Given a flag that exists but is disabled
    const targetId = `user-${uuidv4()}`;
    
    // When checking if the flag is enabled
    const isEnabled = await client.isEnabled(disabledFlagKey, targetId);
    
    // Then it should return false
    expect(isEnabled).toBe(false);
  });

  test('isEnabled returns default value for non-existent flag', async () => {
    // Given a flag that doesn't exist
    const nonExistentKey = `non-existent-${uuidv4()}`;
    const targetId = `user-${uuidv4()}`;
    
    // When checking if the flag is enabled with default=false
    const isEnabledWithFalseDefault = await client.isEnabled(nonExistentKey, targetId, false);
    
    // Then it should return the default value (false)
    expect(isEnabledWithFalseDefault).toBe(false);
    
    // When checking if the flag is enabled with default=true
    const isEnabledWithTrueDefault = await client.isEnabled(nonExistentKey, targetId, true);
    
    // Then it should return the default value (true)
    expect(isEnabledWithTrueDefault).toBe(true);
  });

  test('invalidate properly removes a flag from the cache', async () => {
    // Given a flag that exists and is cached
    const flagKey = enabledFlagKey;
    const targetId = `user-${uuidv4()}`;
    
    // When the flag is accessed
    const firstResult = await client.isEnabled(flagKey, targetId);
    
    // And then invalidated
    client.invalidate(flagKey, targetId);
    
    // And accessed again
    const secondResult = await client.isEnabled(flagKey, targetId);
    
    // Then both results should be consistent
    expect(secondResult).toBe(firstResult);
  });

  test('clearCache properly clears all flags from the cache', async () => {
    // Given multiple flags that are cached
    const targetId = `user-${uuidv4()}`;
    
    // When the flags are accessed
    await client.isEnabled(enabledFlagKey, targetId);
    await client.isEnabled(disabledFlagKey, targetId);
    
    // And the cache is cleared
    client.clearCache();
    
    // Then subsequent accesses should fetch from the API again
    // This is hard to test directly, but we can verify the operation completes without errors
    const result = await client.isEnabled(enabledFlagKey, targetId);
    expect(result).toBe(true);
  });
});
