#!/bin/sh

# echo "localhost:7432:*:postgres:EbGRMiTuBi3HWX4k" >> ~/.pgpass
# chmod 0600 ~/.pgpass

set -e

echo "=== backing up prod database ==="
rm -f /tmp/thrones_db.sql
BACKUPNAME=/tmp/thrones_db_$(date "+%Y%m%d%H%M").sql.gz
unset PGPASSWORD
ssh -MS ctrl-socket -fNT -L 7432:rs-database.crw3bxnxyerb.us-east-1.rds.amazonaws.com:5432 rs-keycloak-server
pg_dump -p 7432 thrones_db > /tmp/thrones_db.sql
gzip -cvf /tmp/thrones_db.sql > "$BACKUPNAME"
ssh -S ctrl-socket -O exit rs-keycloak-server

## syncing files
echo "=== syncing up backup ==="
rsync -avz -e ssh --rsync-path="sudo rsync" "$BACKUPNAME" rs-quarkus-server:/home/ec2-user/database_backups

set +e