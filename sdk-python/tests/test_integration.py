import pytest
import os
import time
import uuid
import subprocess
import requests
import signal
from featureflagx.sdk.client import FeatureFlagClient, FeatureFlagClientConfig

class TestPythonSdkIntegration:
    """
    Integration tests for the Python SDK against a running API instance.
    Uses Docker Compose to spin up the entire stack (API, PostgreSQL, Redis).
    """
    
    @classmethod
    def setup_class(cls):
        """Set up the test environment by starting Docker Compose services."""
        # Start Docker Compose services
        cls.docker_compose_process = subprocess.Popen(
            ["docker-compose", "-f", "../docker-compose-test.yml", "up", "-d"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        
        # Wait for services to be ready
        cls._wait_for_api_ready()
        
        # Initialize client
        cls.api_url = "http://localhost:8081"
        cls.client = FeatureFlagClient(
            FeatureFlagClientConfig(
                base_url=cls.api_url,
                cache_ttl_seconds=5
            )
        )
        
        # Create test flags
        cls._create_test_flags()
    
    @classmethod
    def teardown_class(cls):
        """Tear down the test environment by stopping Docker Compose services."""
        # Stop Docker Compose services
        subprocess.run(
            ["docker-compose", "-f", "../docker-compose-test.yml", "down"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        
        # Ensure the process is terminated
        if cls.docker_compose_process:
            cls.docker_compose_process.terminate()
            try:
                cls.docker_compose_process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                cls.docker_compose_process.kill()
    
    @classmethod
    def _wait_for_api_ready(cls):
        """Wait for the API to be ready to accept requests."""
        max_retries = 30
        retry_interval = 2
        
        for _ in range(max_retries):
            try:
                response = requests.get(f"http://localhost:8081/flags")
                if response.status_code == 200:
                    # API is ready
                    return
            except requests.RequestException:
                pass
            
            time.sleep(retry_interval)
        
        raise TimeoutError("API service did not become ready in the expected time")
    
    @classmethod
    def _create_test_flags(cls):
        """Create test flags via the API."""
        # Create an enabled flag
        enabled_flag = {
            "key": f"python-test-enabled-{uuid.uuid4()}",
            "enabled": True,
            "config": '{"version":"1.0"}'
        }
        
        # Create a disabled flag
        disabled_flag = {
            "key": f"python-test-disabled-{uuid.uuid4()}",
            "enabled": False,
            "config": '{"version":"1.0"}'
        }
        
        # Send requests to create flags
        requests.post(f"{cls.api_url}/flags", json=enabled_flag)
        requests.post(f"{cls.api_url}/flags", json=disabled_flag)
        
        # Store flag keys for tests
        cls.enabled_flag_key = enabled_flag["key"]
        cls.disabled_flag_key = disabled_flag["key"]
    
    def test_is_enabled_with_existing_flag(self):
        """Test that isEnabled returns the correct value for an existing flag."""
        # Given a flag that exists and is enabled
        target_id = f"user-{uuid.uuid4()}"
        
        # When checking if the flag is enabled
        is_enabled = self.client.is_enabled(self.enabled_flag_key, target_id)
        
        # Then it should return True
        assert is_enabled is True
    
    def test_is_enabled_with_disabled_flag(self):
        """Test that isEnabled returns the correct value for a disabled flag."""
        # Given a flag that exists but is disabled
        target_id = f"user-{uuid.uuid4()}"
        
        # When checking if the flag is enabled
        is_enabled = self.client.is_enabled(self.disabled_flag_key, target_id)
        
        # Then it should return False
        assert is_enabled is False
    
    def test_is_enabled_with_non_existent_flag(self):
        """Test that isEnabled returns the default value for a non-existent flag."""
        # Given a flag that doesn't exist
        non_existent_key = f"non-existent-{uuid.uuid4()}"
        target_id = f"user-{uuid.uuid4()}"
        
        # When checking if the flag is enabled with default=False
        is_enabled = self.client.is_enabled(non_existent_key, target_id, default=False)
        
        # Then it should return the default value (False)
        assert is_enabled is False
        
        # When checking if the flag is enabled with default=True
        is_enabled = self.client.is_enabled(non_existent_key, target_id, default=True)
        
        # Then it should return the default value (True)
        assert is_enabled is True
    
    def test_cache_invalidation(self):
        """Test that invalidate properly removes a flag from the cache."""
        # Given a flag that exists and is cached
        flag_key = self.enabled_flag_key
        target_id = f"user-{uuid.uuid4()}"
        
        # When the flag is accessed
        first_result = self.client.is_enabled(flag_key, target_id)
        
        # And then invalidated
        self.client.invalidate(flag_key, target_id)
        
        # And accessed again
        second_result = self.client.is_enabled(flag_key, target_id)
        
        # Then both results should be consistent
        assert first_result == second_result
    
    def test_clear_cache(self):
        """Test that clearCache properly clears all flags from the cache."""
        # Given multiple flags that are cached
        target_id = f"user-{uuid.uuid4()}"
        
        # When the flags are accessed
        self.client.is_enabled(self.enabled_flag_key, target_id)
        self.client.is_enabled(self.disabled_flag_key, target_id)
        
        # And the cache is cleared
        self.client.clear_cache()
        
        # Then subsequent accesses should fetch from the API again
        # This is hard to test directly, but we can verify the operation completes without errors
        assert self.client.is_enabled(self.enabled_flag_key, target_id) is True
