#!/usr/bin/env bash
# Simple script to trigger LeagueApps integration
# Usage: ./league_apps_integrate_simple.sh [username] [password]

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

echo "Getting access token for user: $USERNAME"
echo "Keycloak URL: $KEYCLOAK_URL"
echo "API URL: $API_URL"
echo ""

# Get access token
export access_token=$(
  curl --insecure -X POST "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" \
    --user "$CLIENT_ID:$CLIENT_SECRET" \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d "username=$USERNAME&password=$PASSWORD&grant_type=password" | \
    jq --raw-output '.access_token'
)

if [ -z "$access_token" ] || [ "$access_token" == "null" ]; then
  echo "Error: Failed to get access token"
  exit 1
fi

echo "Access token obtained successfully"
echo "Triggering LeagueApps integration..."
echo ""

# Call the integration endpoint
response=$(curl -s -w "\n%{http_code}" \
  -X GET \
  -H "Authorization: Bearer $access_token" \
  "$API_URL/api/league_apps_integration/integrate")

# Extract HTTP status code (last line)
http_code=$(echo "$response" | tail -n1)
# Extract response body (everything except last line)
response_body=$(echo "$response" | sed '$d')

echo "HTTP Status Code: $http_code"
echo "Response: $response_body"

if [ "$http_code" -eq 200 ]; then
  echo ""
  echo "✅ LeagueApps integration triggered successfully!"
else
  echo ""
  echo "❌ Error: HTTP $http_code"
  exit 1
fi

