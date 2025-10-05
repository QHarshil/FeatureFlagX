#!/usr/bin/env python3
"""
FeatureFlagX SDK Demo Script
This script demonstrates how to use the FeatureFlagX Python SDK with a mock API server
"""

import sys
import time
import logging
from threading import Thread
import subprocess
import os
import signal

# Add the SDK directory to the Python path
sys.path.append('/home/ubuntu/demo/sdk-python')

# Import the FeatureFlagX SDK
from featureflagx.sdk.client import FeatureFlagClient, FeatureFlagClientConfig

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger('featureflagx_demo')

def start_mock_server():
    """Start the mock API server in a separate process"""
    logger.info("Starting mock FeatureFlagX API server...")
    server_process = subprocess.Popen(
        ["python3", "/home/ubuntu/demo/mock_api_server.py"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )
    # Give the server a moment to start
    time.sleep(2)
    return server_process

def stop_mock_server(server_process):
    """Stop the mock API server"""
    logger.info("Stopping mock FeatureFlagX API server...")
    os.kill(server_process.pid, signal.SIGTERM)
    server_process.wait()

def run_demo():
    """Run the FeatureFlagX SDK demonstration"""
    print("\n" + "="*80)
    print("FeatureFlagX SDK Demonstration".center(80))
    print("="*80 + "\n")
    
    # Start the mock server
    server_process = start_mock_server()
    
    try:
        # Initialize the FeatureFlagX client
        print("1. Initializing the FeatureFlagX client")
        print("-" * 50)
        config = FeatureFlagClientConfig(
            base_url="http://localhost:8080",
            cache_ttl_seconds=60,
            cache_max_size=100,
            default_value_on_error=False
        )
        client = FeatureFlagClient(config)
        print("   Client initialized with configuration:")
        print(f"   - API Base URL: {config.base_url}")
        print(f"   - Cache TTL: {config.cache_ttl_seconds} seconds")
        print(f"   - Default value on error: {config.default_value_on_error}")
        print()
        
        # Scenario 1: Check a flag that is enabled
        print("2. Checking a flag that is enabled")
        print("-" * 50)
        flag_key = "new-checkout-flow"
        is_enabled = client.is_enabled(flag_key)
        print(f"   Flag '{flag_key}' is enabled: {is_enabled}")
        print("   This means the new checkout flow should be shown to all users.")
        print()
        
        # Scenario 2: Check a flag that is disabled
        print("3. Checking a flag that is disabled")
        print("-" * 50)
        flag_key = "dark-mode"
        is_enabled = client.is_enabled(flag_key)
        print(f"   Flag '{flag_key}' is enabled: {is_enabled}")
        print("   This means the dark mode feature should not be shown to users.")
        print()
        
        # Scenario 3: Check a flag with targeting rules
        print("4. Checking a flag with targeting rules")
        print("-" * 50)
        flag_key = "beta-feature"
        
        # User who should have access
        target_id = "user-123"
        is_enabled = client.is_enabled(flag_key, target_id)
        print(f"   Flag '{flag_key}' is enabled for user '{target_id}': {is_enabled}")
        
        # User who should not have access
        target_id = "user-999"
        is_enabled = client.is_enabled(flag_key, target_id)
        print(f"   Flag '{flag_key}' is enabled for user '{target_id}': {is_enabled}")
        print("   This demonstrates how targeting rules can enable features for specific users.")
        print()
        
        # Scenario 4: Check a non-existent flag
        print("5. Checking a non-existent flag")
        print("-" * 50)
        flag_key = "non-existent-flag"
        is_enabled = client.is_enabled(flag_key)
        print(f"   Flag '{flag_key}' is enabled: {is_enabled}")
        print("   When a flag doesn't exist, the default value (False) is returned.")
        print()
        
        # Scenario 5: Check a non-existent flag with custom default
        print("6. Checking a non-existent flag with custom default")
        print("-" * 50)
        flag_key = "another-non-existent-flag"
        is_enabled = client.is_enabled(flag_key, default_value=True)
        print(f"   Flag '{flag_key}' is enabled (with custom default=True): {is_enabled}")
        print("   You can specify a custom default value for non-existent flags.")
        print()
        
        # Scenario 6: Demonstrate caching
        print("7. Demonstrating caching behavior")
        print("-" * 50)
        flag_key = "new-checkout-flow"
        print(f"   First check of '{flag_key}' (should hit API): {client.is_enabled(flag_key)}")
        print(f"   Second check of '{flag_key}' (should use cache): {client.is_enabled(flag_key)}")
        
        # Invalidate the cache for this flag
        print("\n   Invalidating cache for this flag...")
        client.invalidate(flag_key)
        print(f"   Check after invalidation (should hit API again): {client.is_enabled(flag_key)}")
        print()
        
        # Scenario 7: Clear entire cache
        print("8. Clearing the entire cache")
        print("-" * 50)
        client.clear_cache()
        print("   All flags have been removed from the cache.")
        print("   The next request for any flag will hit the API.")
        print()
        
        # Real-world example
        print("9. Real-world usage example")
        print("-" * 50)
        print("   // In your application code:")
        print("   if (client.is_enabled('new-checkout-flow', user_id)):")
        print("       # Show the new checkout flow")
        print("       show_new_checkout_flow()")
        print("   else:")
        print("       # Show the old checkout flow")
        print("       show_old_checkout_flow()")
        print()
        
        print("="*80)
        print("FeatureFlagX SDK Demonstration Complete".center(80))
        print("="*80 + "\n")
        
    finally:
        # Stop the mock server
        stop_mock_server(server_process)

if __name__ == "__main__":
    run_demo()
