#! /bin/sh

export TOKEN=$(\
  curl --insecure -k -X POST http://127.0.0.1:8080/realms/master/protocol/openid-connect/token \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -H "Authorization: Basic YWRtaW4tY2xpOmM3NWRhMjZlLWE0NDAtNDMyZC04YzE5LWNmZTgyM2UyZTJmMw==" \
    -d "username=admin&password=91GYV9rseDLnqTwz&grant_type=password&client_id=admin-cli" | jq --raw-output '.access_token' \
)

cat <<EOF > payload.log
  [
    {
      "name": "USER",
      "id": "411ce934-0b3f-4590-a38d-3b77581cb2ad"
    }
  ]
EOF

curl -k -X POST "http://127.0.0.1:8080/admin/realms/thrones_realm/users/b754e270-281d-4d4c-97c1-85c85a1255f3/role-mappings/realm" \
 -H "Content-Type: application/json" \
 -H "Authorization: Bearer ${TOKEN}" \
 -d @payload.log


