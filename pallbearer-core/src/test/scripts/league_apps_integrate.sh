#!/usr/bin/env bash
# eve access token
#export access_token=$(\
#curl --insecure -X POST https://keycloak.lektralabs.com/realms/thrones_realm/protocol/openid-connect/token \
#    --user thrones_client:iv2xJ1hwoeB9yClZoNeCTL77OgUsyMmE \
#    -H 'content-type: application/x-www-form-urlencoded' \
#    -d 'username=eve&password=Password4u&grant_type=password'| jq --raw-output '.access_token' \
#)

# ken access token
export access_token=$(\
curl --insecure -X POST https://keycloak.lektralabs.com/realms/thrones_realm/protocol/openid-connect/token \
     --user thrones_client:iv2xJ1hwoeB9yClZoNeCTL77OgUsyMmE \
     -H 'content-type: application/x-www-form-urlencoded' \
     -d 'username=kbrumer&password=password&grant_type=password'| jq --raw-output '.access_token' \
)

curl \
  -s -I \
  -X GET \
  -H "Authorization: Bearer "$access_token \
  http://localhost:8000/api/league_apps_integration/integrate
