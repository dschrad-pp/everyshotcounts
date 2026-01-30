#!/usr/bin/env bash
# One-liner curl command to trigger LeagueApps integration
# This version gets the token and calls the API in a single command

KEYCLOAK_URL=${KEYCLOAK_URL:-https://keycloak.lektralabs.com}
KEYCLOAK_REALM=${KEYCLOAK_REALM:-thrones_realm}
CLIENT_ID=${CLIENT_ID:-thrones_client}
CLIENT_SECRET=${CLIENT_SECRET:-iv2xJ1hwoeB9yClZoNeCTL77OgUsyMmE}
API_URL=${API_URL:-http://localhost:8000}
USERNAME=${1:-${KEYCLOAK_USERNAME:-kbrumer}}
PASSWORD=${2:-${KEYCLOAK_PASSWORD:-password}}

# Single command that gets token and calls integration
curl -X GET "$API_URL/api/league_apps_integration/integrate" \
  -H "Authorization: Bearer $(curl --insecure -s -X POST "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" \
    --user "$CLIENT_ID:$CLIENT_SECRET" \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d "username=$USERNAME&password=$PASSWORD&grant_type=password" | \
    jq --raw-output '.access_token')" \
  -v

