#! /bin/bash -xv

set -e
pushd $THRONES_HOME/pallbearer

cat << EOF | psql -Upostgres -h $PGHOST
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

popd
set +e