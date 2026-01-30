#!/bin/bash

# Script to get Keycloak group IDs
KEYCLOAK_URL="${KEYCLOAK_URL:-http://10.10.0.215:8080}"
REALM="thrones_realm"
ADMIN_USER="admin"
ADMIN_PASS="91GYV9rseDLnqTwz"

echo "Getting admin token..."
TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASS" \
  -d "grant_type=password" \
  -d "client_id=admin-cli")

TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('access_token', ''))" 2>/dev/null)

if [ -z "$TOKEN" ] || [ "$TOKEN" == "None" ]; then
  echo "ERROR: Failed to get admin token"
  echo "Trying alternative method..."
  # Try to extract token manually
  TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
  if [ -z "$TOKEN" ]; then
    echo "Please check your admin credentials"
    exit 1
  fi
fi

echo "Fetching groups..."
GROUPS=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/groups" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo ""
echo "=========================================="
echo "Group IDs for CoreConstants.java"
echo "=========================================="
echo ""

echo "$GROUPS" | python3 -c "
import sys, json
try:
    groups = json.load(sys.stdin)
    required_groups = ['ADMIN', 'USER', 'ATHLETE', 'COACH', 'FAN']
    
    print('Copy these into CoreConstants.java:\n')
    print('// Group IDs from Keycloak')
    
    for group_name in required_groups:
        found = False
        for group in groups:
            if group.get('name') == group_name:
                group_id = group.get('id', 'NOT_FOUND')
                var_name = group_name + '_GROUP_ID'
                print(f'    UUID {var_name} = UUID.fromString(\"{group_id}\");')
                found = True
                break
        if not found:
            print(f'    // {group_name} group NOT FOUND - create it in Keycloak')
    
    print('\n')
    print('All groups found:')
    for group in groups:
        if group.get('name') in required_groups:
            print(f'  ✓ {group.get(\"name\")} (ID: {group.get(\"id\")})')
except Exception as e:
    print(f'Error parsing groups: {e}')
    print('Raw response:')
    print(sys.stdin.read())
"
