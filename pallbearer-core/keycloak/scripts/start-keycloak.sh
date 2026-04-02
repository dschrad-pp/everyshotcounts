#!/bin/bash
# Start Keycloak on port 8180 (8080 is used by Apache)
# Usage: ./start-keycloak.sh

export KEYCLOAK_HOME="${KEYCLOAK_HOME:-/usr/local/keycloak}"
cd "$KEYCLOAK_HOME"
exec ./bin/kc.sh start-dev --http-port=8180 "$@"
