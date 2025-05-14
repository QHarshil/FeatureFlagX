import requests
from cachetools import TTLCache
from typing import Optional, Union, Dict, Any
from dataclasses import dataclass, field
import time
import logging

logger = logging.getLogger(__name__)

@dataclass
class FeatureFlagClientConfig:
    base_url: str = "http://localhost:8080"  # Default API URL
    connect_timeout_seconds: float = 5.0
    read_timeout_seconds: float = 5.0
    cache_max_size: int = 1000
    cache_ttl_seconds: int = 300  # 5 minutes
    default_value_on_error: bool = False
    # Potentially add API key if the API requires authentication in the future
    # api_key: Optional[str] = None 

class FeatureFlagClient:
    def __init__(self, config: FeatureFlagClientConfig):
        self.config = config
        self.session = requests.Session()
        # if self.config.api_key:
        #     self.session.headers.update({"Authorization": f"Bearer {self.config.api_key}"})
        self.cache = TTLCache(maxsize=self.config.cache_max_size, ttl=self.config.cache_ttl_seconds)

    def _make_request(self, flag_key: str, target_id: Optional[str] = None) -> Optional[bool]:
        """Internal method to make the API request."""
        endpoint = f"{self.config.base_url}/flags/evaluate/{flag_key}"
        params: Dict[str, Any] = {}
        if target_id:
            params["targetId"] = target_id
        
        try:
            response = self.session.get(
                endpoint,
                params=params,
                timeout=(self.config.connect_timeout_seconds, self.config.read_timeout_seconds)
            )
            response.raise_for_status()  # Raises HTTPError for bad responses (4XX or 5XX)
            # Assuming the API returns a JSON boolean: true or false
            return response.json()
        except requests.exceptions.RequestException as e:
            logger.error(f"Error fetching flag 	{flag_key}	 for target 	{target_id}	: {e}")
            return None
        except ValueError as e: # Includes JSONDecodeError
            logger.error(f"Error decoding JSON response for flag 	{flag_key}	: {e}")
            return None

    def is_enabled(self, flag_key: str, target_id: Optional[str] = None, default_value: Optional[bool] = None) -> bool:
        """
        Checks if a feature flag is enabled.

        Args:
            flag_key: The unique key of the feature flag.
            target_id: (Optional) An identifier for the target (e.g., user ID, session ID)
                       to be used for more complex targeting rules if supported by the API.
            default_value: (Optional) The value to return if the flag cannot be evaluated or an error occurs.
                           If None, the client-level default_value_on_error will be used.

        Returns:
            True if the flag is enabled, False otherwise.
        """
        if not flag_key:
            logger.warning("flag_key cannot be empty.")
            return default_value if default_value is not None else self.config.default_value_on_error

        cache_key = f"{flag_key}:{target_id if target_id else ''}"
        
        # Check cache first
        cached_value = self.cache.get(cache_key)
        if cached_value is not None and isinstance(cached_value, bool):
            logger.debug(f"Flag 	{flag_key}	 for target 	{target_id}	 found in cache: {cached_value}")
            return cached_value

        # Fetch from API
        api_value = self._make_request(flag_key, target_id)

        if api_value is not None and isinstance(api_value, bool):
            logger.debug(f"Flag 	{flag_key}	 for target 	{target_id}	 fetched from API: {api_value}")
            self.cache[cache_key] = api_value
            return api_value
        else:
            logger.warning(f"Could not fetch flag 	{flag_key}	 for target 	{target_id}	 from API. Returning default.")
            # Optionally cache the default value on error to prevent hammering the API for missing/erroring flags
            # self.cache[cache_key] = default_value if default_value is not None else self.config.default_value_on_error
            return default_value if default_value is not None else self.config.default_value_on_error

    def invalidate(self, flag_key: str, target_id: Optional[str] = None) -> None:
        """Invalidates a specific flag from the local cache."""
        cache_key = f"{flag_key}:{target_id if target_id else ''}"
        if cache_key in self.cache:
            del self.cache[cache_key]
            logger.info(f"Invalidated flag 	{flag_key}	 for target 	{target_id}	 from cache.")

    def clear_cache(self) -> None:
        """Clears the entire local flag cache."""
        self.cache.clear()
        logger.info("Local flag cache cleared.")

# Example Usage (for testing purposes, would be in an example file or tests)
if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    
    # This assumes the FeatureFlagX API is running at http://localhost:8080
    # and has a flag with key "new-feature"
    client_config = FeatureFlagClientConfig(base_url="http://localhost:8080")
    client = FeatureFlagClient(client_config)

    # Test case 1: Flag exists and is enabled
    # Ensure a flag with key "test-enabled-flag" is created and enabled in your API
    # POST /flags  {"key": "test-enabled-flag", "enabled": true, "config": "{}"}
    print(f"Is 'test-enabled-flag' enabled? {client.is_enabled('test-enabled-flag')}")

    # Test case 2: Flag exists and is disabled
    # Ensure a flag with key "test-disabled-flag" is created and disabled in your API
    # POST /flags  {"key": "test-disabled-flag", "enabled": false, "config": "{}"}
    print(f"Is 'test-disabled-flag' enabled? {client.is_enabled('test-disabled-flag')}")

    # Test case 3: Flag does not exist, should use default
    print(f"Is 'non-existent-flag' enabled? {client.is_enabled('non-existent-flag')}")
    print(f"Is 'non-existent-flag' enabled (with specific default true)? {client.is_enabled('non-existent-flag', default_value=True)}")

    # Test cache
    print(f"Fetching 'test-enabled-flag' again (should be cached): {client.is_enabled('test-enabled-flag')}")
    time.sleep(2)
    print(f"Fetching 'test-enabled-flag' again after 2s: {client.is_enabled('test-enabled-flag')}")

    client.invalidate('test-enabled-flag')
    print(f"Fetching 'test-enabled-flag' after invalidation (should fetch from API): {client.is_enabled('test-enabled-flag')}")

    client.clear_cache()
    print(f"Fetching 'test-enabled-flag' after cache clear (should fetch from API): {client.is_enabled('test-enabled-flag')}")

