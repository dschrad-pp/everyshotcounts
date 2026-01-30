# README - auth

## Keycloak

```bash
docker system prune -f
docker run --name keycloak -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -e KEYCLOAK_IMPORT=/tmp/quarkus-realm.json -v $KAZZAH_HOME/mitchell/config/quarkus-realm.json:/tmp/quarkus-realm.json -p 8180:8080 -p 8543:8443 jboss/keycloak


-v $KAZZAH_HOME/config/certs:/etc/x509/https

put tls.crt and tls.key in there 
```

You should be able to access your Keycloak Server at [localhost:8180/auth](http://localhost:8180/auth).

Log in as the `admin` user to access the Keycloak Administration Console.
Username should be `admin` and password `admin`.


## Quarkus

```bash
cd $KAZZAH_HOME
mvn clean install
cd $KAZZAH_HOME/api-service
mvn clean compile quarkus:dev
```

## Testing

```bash
export access_token=$(\
    curl --insecure -X POST https://sso.kazzah.com/auth/realms/kazzah/protocol/openid-connect/token \
    --user mitchell-service:510a3b55-666c-486e-bc20-2e12e93be501 \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=alice&password=alice&grant_type=password' | jq --raw-output '.access_token' \
 )
```

The example above obtains an access token for user `alice`.

Any user is allowed to access the
`http://localhost:8080/api/users/me` endpoint
which basically returns a JSON payload with details about the user.

```bash
curl -v -X GET \
  http://localhost:8080/api/user/current \
  -H "Authorization: Bearer "$access_token
```

The `http://localhost:8080/api/admin` endpoint can only be accessed by users with the `admin` role.
If you try to access this endpoint with the previously issued access token, you should get a `403` response from the server.

```bash
 curl -v -X GET \
   http://localhost:8080/api/admin \
   -H "Authorization: Bearer "$access_token
```

In order to access the admin endpoint you should obtain a token for the `admin` user:

```bash
export access_token=$(\
    curl --insecure -X POST https://localhost:8543/auth/realms/quarkus/protocol/openid-connect/token \
    --user backend-service:secret \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=admin&password=admin&grant_type=password' | jq --raw-output '.access_token' \
 )
```

