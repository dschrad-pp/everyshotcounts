#!/usr/bin/env bash
# from https://github.com/tus/tus.io/issues/96
# ref:
# - https://tus.io/blog/2015/11/16/tus.1.0
# - https://github.com/tus/tus.io/issues/96

export TUSD="http://localhost:8000/api/upload/tus/5d560fc0-e21e-4590-83be-3dafed406365"

if [ ! -f "${1}" ]; then
  echo -e "\n\033[1;31m✘\033[0m First argument needs to be a file.\n"
  exit 1
fi

file=${1}
filename=$(basename "${file}" | base64)
filesize="$(wc -c <"${file}")"


export access_token=$(\
curl --insecure -X POST https://keycloak.lektralabs.com/realms/thrones_realm/protocol/openid-connect/token \
    --user thrones_client:iv2xJ1hwoeB9yClZoNeCTL77OgUsyMmE \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=eve&password=Password4u&grant_type=password'| jq --raw-output '.access_token' \
)

# Apparently 'Location: ..' is terminated by CRLF. grep and awk faithfully
# preserve the line ending, and the shell's $() substitution strips off the
# final LF leaving you with a string that just ends with a CR.
#
# When the CR is printed, the cursor moves to the beginning of the line and
# whatever gets printed next overwrites what was there.
# ... | tr -d '\015'
location=$(curl \
  -s -I \
  -X POST \
  -H "Tus-Resumable: 1.0.0" \
  -H "Content-Length: 0" \
  -H "Upload-Length: ${filesize}" \
  -H "Upload-Metadata: name ${filename}" \
  -H "Authorization: Bearer "$access_token \
  ${TUSD} | grep 'Location:' | awk '{print $2}' | tr -d '\015')

host="http://localhost:8000/"

if [ -n "${location}" ]; then
  curl -vv \
    -X PATCH \
    -H "Tus-Resumable: 1.0.0" \
    -H "Upload-Offset: 0" \
    -H "Content-Length: ${filesize}" \
    -H "Content-Type: application/offset+octet-stream" \
    -H "Authorization: Bearer "$access_token \
    --data-binary "@${file}" \
    "${host}${location}" -v
else
  echo -e "\n\033[1;31m✘\033[0m File creation failed..\n"
  exit 1
fi