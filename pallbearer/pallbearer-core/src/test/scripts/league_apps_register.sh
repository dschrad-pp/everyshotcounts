#!/usr/bin/env bash

cat <<EOF > payload.log
  {
      "email": "ken+test17@lektralabs.com",
      "username": "my_test17_username",
      "skillLevel": "ad8903ec-e765-4f0e-a6a2-359337f37b82",
      "sixDigitCode": "420159",
      "password": "password1234",
      "confirmPassword": "password1234",
      "ageAcknowledgement": "true"
  }
EOF

curl -v -k -X POST "http://localhost:8000/api/league_apps_integration/register" \
 -H "Content-Type: application/json" \
 -d @payload.log
