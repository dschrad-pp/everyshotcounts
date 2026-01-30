## End-to-End Test

First run:
```bash
curl --insecure -X POST https://keycloak.lektralabs.com/realms/thrones_realm/protocol/openid-connect/token \
    --user thrones_client:iv2xJ1hwoeB9yClZoNeCTL77OgUsyMmE \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=kbrumer&password=password&grant_type=password'
```

```bash
export access_token=$(\
curl --insecure -X POST https://keycloak.lektralabs.com/realms/thrones_realm/protocol/openid-connect/token \
    --user thrones_client:iv2xJ1hwoeB9yClZoNeCTL77OgUsyMmE \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=kbrumer&password=password&grant_type=password'| jq --raw-output '.access_token' \
)
```

```bash
curl -v -X GET \
  http://localhost:8000/api/user/current \
  -H "Authorization: Bearer "$access_token
```

```bash
curl -v -X POST \
  http://localhost:8000/api/upload/tus/ab9542ce-f1f8-4650-8641-fd7379bd9882 \
  -H "Authorization: Bearer "$access_token
```


Edit `upload.sh` URL:
```bash
export TUSD="http://localhost:8000/api/upload/tus/5d560fc0-e21e-4590-83be-3dafed406365"
```

Where `5d560fc0-e21e-4590-83be-3dafed406365` is a drill item id.
./src/test/scripts/upload.sh ~/Desktop/one_punch.mkv

after ffmpeg processing, call one more time to attach the media
./src/test/scripts/upload.sh ~/Desktop/one_punch.mkv