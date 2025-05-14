import pytest
import requests
import time
from unittest.mock import patch, MagicMock

from featureflagx.sdk.client import FeatureFlagClient, FeatureFlagClientConfig

@pytest.fixture
def config():
    return FeatureFlagClientConfig(
        base_url="http://mockapi.example.com",
        cache_ttl_seconds=2 # Short TTL for testing cache expiry
    )

@pytest.fixture
def client(config):
    return FeatureFlagClient(config)

@patch("requests.Session.get")
def test_is_enabled_fetches_from_api_on_cache_miss(mock_get, client):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = True # API returns True
    mock_get.return_value = mock_response

    flag_key = "my-feature"
    target_id = "user123"

    # First call: should hit API
    assert client.is_enabled(flag_key, target_id) == True
    mock_get.assert_called_once_with(
        f"{client.config.base_url}/flags/evaluate/{flag_key}",
        params={"targetId": target_id},
        timeout=(client.config.connect_timeout_seconds, client.config.read_timeout_seconds)
    )
    assert client.cache.get(f"{flag_key}:{target_id}") == True

@patch("requests.Session.get")
def test_is_enabled_uses_cache_on_hit(mock_get, client):
    flag_key = "cached-feature"
    target_id = "user456"
    cache_key = f"{flag_key}:{target_id}"
    client.cache[cache_key] = False # Pre-populate cache

    # Call: should use cached value, not hit API
    assert client.is_enabled(flag_key, target_id) == False
    mock_get.assert_not_called()

@patch("requests.Session.get")
def test_is_enabled_api_error_returns_default(mock_get, client):
    mock_get.side_effect = requests.exceptions.RequestException("API Unreachable")
    flag_key = "error-feature"

    # Default from client config (False)
    assert client.is_enabled(flag_key) == client.config.default_value_on_error
    # Specific default provided
    assert client.is_enabled(flag_key, default_value=True) == True
    mock_get.assert_called() # API was attempted

@patch("requests.Session.get")
def test_is_enabled_api_returns_non_200_status_returns_default(mock_get, client):
    mock_response = MagicMock()
    mock_response.status_code = 500
    mock_response.raise_for_status.side_effect = requests.exceptions.HTTPError("Server Error")
    mock_get.return_value = mock_response
    flag_key = "http-error-feature"

    assert client.is_enabled(flag_key) == client.config.default_value_on_error
    assert client.is_enabled(flag_key, default_value=True) == True
    mock_get.assert_called()

@patch("requests.Session.get")
def test_is_enabled_api_returns_invalid_json_returns_default(mock_get, client):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.side_effect = ValueError("Invalid JSON") # requests.exceptions.JSONDecodeError inherits from ValueError
    mock_get.return_value = mock_response
    flag_key = "json-error-feature"

    assert client.is_enabled(flag_key) == client.config.default_value_on_error
    assert client.is_enabled(flag_key, default_value=True) == True
    mock_get.assert_called()


@patch("requests.Session.get")
def test_cache_expiration(mock_get, client, config):
    mock_response_true = MagicMock()
    mock_response_true.status_code = 200
    mock_response_true.json.return_value = True
    
    mock_response_false = MagicMock()
    mock_response_false.status_code = 200
    mock_response_false.json.return_value = False

    mock_get.return_value = mock_response_true
    flag_key = "expiring-feature"

    # First call, API returns True, gets cached
    assert client.is_enabled(flag_key) == True
    assert mock_get.call_count == 1
    assert client.cache.get(f"{flag_key}:") == True

    # Second call, should use cache
    assert client.is_enabled(flag_key) == True
    assert mock_get.call_count == 1 # Still 1, used cache

    # Wait for cache to expire (config.cache_ttl_seconds = 2s)
    time.sleep(config.cache_ttl_seconds + 0.5)

    # Third call, cache expired, should hit API again
    # Let API return a different value now to confirm it re-fetched
    mock_get.return_value = mock_response_false
    assert client.is_enabled(flag_key) == False
    assert mock_get.call_count == 2 # Now 2, API was hit again
    assert client.cache.get(f"{flag_key}:") == False

def test_invalidate_flag(client):
    flag_key = "invalidate-me"
    target_id = "user789"
    cache_key = f"{flag_key}:{target_id}"
    client.cache[cache_key] = True # Manually add to cache

    assert client.cache.get(cache_key) == True
    client.invalidate(flag_key, target_id)
    assert client.cache.get(cache_key) is None

def test_clear_cache(client):
    client.cache["key1:target1"] = True
    client.cache["key2:target2"] = False
    assert len(client.cache) == 2

    client.clear_cache()
    assert len(client.cache) == 0

def test_is_enabled_empty_flag_key_returns_default(client):
    assert client.is_enabled("") == client.config.default_value_on_error
    assert client.is_enabled("   ") == client.config.default_value_on_error
    assert client.is_enabled("", default_value=True) == True

@patch("requests.Session.get")
def test_is_enabled_no_target_id(mock_get, client):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = True
    mock_get.return_value = mock_response

    flag_key = "no-target-feature"
    assert client.is_enabled(flag_key) == True
    mock_get.assert_called_once_with(
        f"{client.config.base_url}/flags/evaluate/{flag_key}",
        params={},
        timeout=(client.config.connect_timeout_seconds, client.config.read_timeout_seconds)
    )
    assert client.cache.get(f"{flag_key}:") == True

