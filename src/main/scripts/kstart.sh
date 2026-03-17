#!/bin/bash
set -e

USAGE="$(basename "$0") [-h] [-r -d] -- restart Quarkus

where:
    -h  show this help text
    -r  recompile
    -d  debug mode"

# Default flags
RECOMPILE=0
LOGDEBUG=0

while getopts ":h?rd" opt; do
  case "$opt" in
    h|\?) echo "$USAGE" >&2; exit 0 ;;
    r) RECOMPILE=1 ;;
    d) LOGDEBUG=1 ;;
  esac
done

# Absolute paths
PALLBEARER_HOME="//"
PALLBEARER_HOME="/var/www/html/pallbearer"
PALLBEARER_API="$PALLBEARER_HOME/pallbearer-api"

# Add SDKMAN Quarkus and Maven to PATH
export PATH="$HOME/.sdkman/candidates/quarkus/current/bin:$HOME/.sdkman/candidates/maven/current/bin:$PATH"

# Optional: Java Home
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"

# Database and Keycloak (Keycloak on 8180, not 8080)
export PGHOST="${PGHOST:-localhost}"
export PGPASSWORD="${PGPASSWORD:-db#2700}"
export KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"

# Recompile if requested
if [[ $RECOMPILE -eq 1 ]]; then
  echo "Recompile called"
  if [[ -d "$PALLBEARER_HOME" ]]; then
    cd "$PALLBEARER_HOME"
    mvn clean install
  else
    echo "Error: $PALLBEARER_HOME does not exist"
    exit 1
  fi
fi

# Run Quarkus in dev mode
if [[ $LOGDEBUG -eq 1 ]]; then
  echo "Debug enabled"
  if [[ -d "$PALLBEARER_API" ]]; then
    cd "$PALLBEARER_API"
    quarkus dev -Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8000 -Dquarkus.log.level=DEBUG
  else
    echo "Error: $PALLBEARER_API does not exist"
    exit 1
  fi
else
  echo "Debug not enabled"
  if [[ -d "$PALLBEARER_API" ]]; then
    cd "$PALLBEARER_API"
    quarkus dev -Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8000
  else
    echo "Error: $PALLBEARER_API does not exist"
    exit 1
  fi
fi

set +e
