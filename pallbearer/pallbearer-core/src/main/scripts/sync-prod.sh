#!/bin/sh

# echo "localhost:6432:*:postgres:EbGRMiTuBi3HWX4k" >> ~/.pgpass
# chmod 0600 ~/.pgpass

set -e

echo "=== syncing thrones_db database ==="
unset PGPASSWORD
ssh -MS ctrl-socket -fNT -L 6432:rs-database.crw3bxnxyerb.us-east-1.rds.amazonaws.com:5432 rs-keycloak-server
pg_dump -p 6432 thrones_db > /tmp/thrones_db.sql
pg_dump -p 6432 keycloak_thrones_db > /tmp/keycloak_thrones_db.sql
ssh -S ctrl-socket -O exit rs-keycloak-server

cat << EOF | psql -Upostgres -p 5432
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'thrones_db';

DROP DATABASE IF EXISTS thrones_db;

CREATE DATABASE thrones_db
WITH OWNER = postgres
TEMPLATE template0
ENCODING = 'UTF8'
LC_COLLATE = 'en_US.UTF-8'
LC_CTYPE = 'en_US.UTF-8'
CONNECTION LIMIT = -1;
EOF

psql -Upostgres -p 5432 -d thrones_db -f /tmp/thrones_db.sql
rm -rf /tmp/thrones_db.sql

## echo "=== syncing keycloak_thrones_db database ==="
##
## cat << EOF | psql -Upostgres -p 5432
## SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'keycloak_thrones_db';
##
## DROP DATABASE IF EXISTS keycloak_thrones_db;
##
## CREATE DATABASE keycloak_thrones_db
## WITH OWNER = postgres
## TEMPLATE template0
## ENCODING = 'UTF8'
## LC_COLLATE = 'en_US.UTF-8'
## LC_CTYPE = 'en_US.UTF-8'
## CONNECTION LIMIT = -1;
## EOF
##
## psql -Upostgres -p 5432 -d keycloak_thrones_db -f /tmp/keycloak_thrones_db.sql
## rm -rf /tmp/keycloak_thrones_db.sql


## syncing files
## there is no need to do this - @TOD - kbrumer - we should sync the default reference files
## echo "=== syncing files ==="
## rsync -avz -e ssh --rsync-path="sudo rsync" rs-quarkus-server:/media/thrones/pallbearer/media /Volumes/mnt/thrones/pallbearer

echo "=== done ==="

set +e