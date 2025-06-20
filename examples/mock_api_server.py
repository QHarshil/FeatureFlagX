#!/usr/bin/env python3
"""
Mock API Server for FeatureFlagX demonstration
This simple server mocks the FeatureFlagX API for demonstration purposes
"""

from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import re
import urllib.parse
import logging

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger('mock_api')

# In-memory flag storage
FLAGS = {
    "new-checkout-flow": {
        "key": "new-checkout-flow",
        "enabled": True,
        "config": '{"version": "v1", "rolloutPercentage": 100}'
    },
    "dark-mode": {
        "key": "dark-mode",
        "enabled": False,
        "config": '{"version": "v1", "rolloutPercentage": 0}'
    },
    "beta-feature": {
        "key": "beta-feature",
        "enabled": True,
        "config": '{"version": "v1", "allowedUsers": ["user-123", "user-456"]}'
    }
}

class MockFeatureFlagHandler(BaseHTTPRequestHandler):
    def _set_headers(self, status_code=200, content_type='application/json'):
        self.send_response(status_code)
        self.send_header('Content-type', content_type)
        self.end_headers()
    
    def _send_json_response(self, data, status_code=200):
        self._set_headers(status_code)
        self.wfile.write(json.dumps(data).encode())
    
    def do_GET(self):
        logger.info(f"GET request received: {self.path}")
        
        # Parse URL and query parameters
        parsed_url = urllib.parse.urlparse(self.path)
        path = parsed_url.path
        query_params = dict(urllib.parse.parse_qsl(parsed_url.query))
        
        # Evaluate flag endpoint
        evaluate_match = re.match(r'/flags/evaluate/([a-zA-Z0-9-_]+)', path)
        if evaluate_match:
            flag_key = evaluate_match.group(1)
            target_id = query_params.get('targetId')
            
            if flag_key in FLAGS:
                flag = FLAGS[flag_key]
                
                # Simple targeting logic for demonstration
                if flag_key == "beta-feature" and target_id:
                    config = json.loads(flag["config"])
                    allowed_users = config.get("allowedUsers", [])
                    is_enabled = flag["enabled"] and target_id in allowed_users
                else:
                    is_enabled = flag["enabled"]
                
                self._set_headers(200, 'application/json')
                self.wfile.write(json.dumps(is_enabled).encode())
            else:
                self._set_headers(404)
                self.wfile.write(b'{"error": "Flag not found"}')
            return
        
        # Get all flags endpoint
        if path == '/flags':
            self._send_json_response(list(FLAGS.values()))
            return
        
        # Get specific flag endpoint
        flag_match = re.match(r'/flags/([a-zA-Z0-9-_]+)', path)
        if flag_match:
            flag_key = flag_match.group(1)
            if flag_key in FLAGS:
                self._send_json_response(FLAGS[flag_key])
            else:
                self._set_headers(404)
                self.wfile.write(b'{"error": "Flag not found"}')
            return
        
        # Health check endpoint
        if path == '/actuator/health':
            self._send_json_response({"status": "UP"})
            return
        
        # Default: Not found
        self._set_headers(404)
        self.wfile.write(b'{"error": "Endpoint not found"}')
    
    def do_POST(self):
        logger.info(f"POST request received: {self.path}")
        
        # Get content length
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        # Create flag endpoint
        if self.path == '/flags':
            try:
                flag_data = json.loads(post_data.decode('utf-8'))
                key = flag_data.get('key')
                
                if not key:
                    self._set_headers(400)
                    self.wfile.write(b'{"error": "Flag key is required"}')
                    return
                
                FLAGS[key] = flag_data
                self._send_json_response(flag_data, 201)
            except json.JSONDecodeError:
                self._set_headers(400)
                self.wfile.write(b'{"error": "Invalid JSON"}')
            return
        
        # Default: Not found
        self._set_headers(404)
        self.wfile.write(b'{"error": "Endpoint not found"}')
    
    def do_PUT(self):
        logger.info(f"PUT request received: {self.path}")
        
        # Get content length
        content_length = int(self.headers['Content-Length'])
        put_data = self.rfile.read(content_length)
        
        # Update flag endpoint
        flag_match = re.match(r'/flags/([a-zA-Z0-9-_]+)', self.path)
        if flag_match:
            flag_key = flag_match.group(1)
            
            if flag_key not in FLAGS:
                self._set_headers(404)
                self.wfile.write(b'{"error": "Flag not found"}')
                return
            
            try:
                flag_data = json.loads(put_data.decode('utf-8'))
                FLAGS[flag_key] = flag_data
                self._send_json_response(flag_data)
            except json.JSONDecodeError:
                self._set_headers(400)
                self.wfile.write(b'{"error": "Invalid JSON"}')
            return
        
        # Default: Not found
        self._set_headers(404)
        self.wfile.write(b'{"error": "Endpoint not found"}')
    
    def do_DELETE(self):
        logger.info(f"DELETE request received: {self.path}")
        
        # Delete flag endpoint
        flag_match = re.match(r'/flags/([a-zA-Z0-9-_]+)', self.path)
        if flag_match:
            flag_key = flag_match.group(1)
            
            if flag_key not in FLAGS:
                self._set_headers(404)
                self.wfile.write(b'{"error": "Flag not found"}')
                return
            
            del FLAGS[flag_key]
            self._set_headers(204)
            return
        
        # Default: Not found
        self._set_headers(404)
        self.wfile.write(b'{"error": "Endpoint not found"}')

def run_server(port=8080):
    server_address = ('', port)
    httpd = HTTPServer(server_address, MockFeatureFlagHandler)
    logger.info(f'Starting mock FeatureFlagX API server on port {port}...')
    httpd.serve_forever()

if __name__ == '__main__':
    run_server()
