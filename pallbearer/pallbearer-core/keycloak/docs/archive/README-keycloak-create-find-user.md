## Allow Create User
https://www.appsdeveloperblog.com/keycloak-rest-api-create-a-new-user/

# get an access token
```bash
export access_token=$(\
  curl -X POST \
    https://sso.kazzah.com/auth/realms/master/protocol/openid-connect/token \
    -H 'Accept: application/json' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -H 'cache-control: no-cache' \
    --user admin-cli:cf9ca96d-5291-4392-a064-7d94dea00175 \
    -d 'grant_type=password&username=admin&password=D1rK5FFKcZXvbtFP&client_id=admin-cli' \
    | jq --raw-output '.access_token' \
)
```

## create user
```bash
curl -X POST \
  https://sso.kazzah.com/auth/admin/realms/kazzah/users \
  -H 'Authorization: Bearer '$access_token \
  -H "Content-Type: application/json"  \
  --data '{"username":"test1","enabled":true,"firstName":"test1","lastName":"test1","realmRoles": ["CLIENT"], "credentials":[{"type":"password","value":"test1"}]}' 
```

# get all users of gateway realm, use the token from above and use Bearer as prefix
```bash
curl -X GET \
  https://sso.kazzah.com/auth/admin/realms/kazzah/users?username=carol \
  -H 'Authorization: Bearer '$access_token
```

# delete user
# DELETE /{realm}/users/{id}
```bash
curl -X DELETE \
  https://sso.kazzah.com/auth/admin/realms/kazzah/users/$id \
  -H 'Authorization: Bearer '$access_token
```
