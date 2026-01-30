#!/usr/bin/env bash
# Script to verify LeagueApps credentials
# Usage: ./league_apps_verify_credentials.sh [username] [password]

set -e

# Configuration (can be overridden via environment variables)
KEYCLOAK_URL=${KEYCLOAK_URL:-https://keycloak.lektralabs.com}
KEYCLOAK_REALM=${KEYCLOAK_REALM:-thrones_realm}
CLIENT_ID=${CLIENT_ID:-thrones_client}
CLIENT_SECRET=${CLIENT_SECRET:-iv2xJ1hwoeB9yClZoNeCTL77OgUsyMmE}
API_URL=${API_URL:-http://localhost:8000}

# Get credentials from arguments or environment
USERNAME=${1:-${KEYCLOAK_USERNAME:-kbrumer}}
PASSWORD=${2:-${KEYCLOAK_PASSWORD:-password}}

echo "=========================================="
echo "LeagueApps Credentials Verification"
echo "=========================================="
echo "Getting access token for user: $USERNAME"
echo "Keycloak URL: $KEYCLOAK_URL"
echo "API URL: $API_URL"
echo ""

# Get access token
export access_token=$(
  curl --insecure -s -X POST "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" \
    --user "$CLIENT_ID:$CLIENT_SECRET" \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d "username=$USERNAME&password=$PASSWORD&grant_type=password" | \
    jq --raw-output '.access_token'
)

if [ -z "$access_token" ] || [ "$access_token" == "null" ]; then
  echo "❌ Error: Failed to get access token"
  exit 1
fi

echo "✅ Access token obtained successfully"
echo ""
echo "Verifying LeagueApps credentials..."
echo ""

# Call the verification endpoint
response=$(curl -s -w "\n%{http_code}" \
  -X GET \
  -H "Authorization: Bearer $access_token" \
  -H "Content-Type: application/json" \
  "$API_URL/api/league_apps_integration/verify-credentials")

# Extract HTTP status code (last line)
http_code=$(echo "$response" | tail -n1)
# Extract response body (everything except last line)
response_body=$(echo "$response" | sed '$d')

echo "HTTP Status Code: $http_code"
echo ""
echo "Verification Results:"
echo "$response_body" | jq '.'

# Check if credentials are valid
all_valid=$(echo "$response_body" | jq -r '.allCredentialsValid // false')

if [ "$all_valid" == "true" ]; then
  echo ""
  echo "✅ All LeagueApps credentials are valid!"
  exit 0
else
  echo ""
  echo "❌ Some credentials are invalid. Check the details above."
  exit 1
fi

