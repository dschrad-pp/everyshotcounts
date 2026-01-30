#! /bin/bash -xv

set -e
pushd $THRONES_HOME/pallbearer

BACKUP_FILE=$THRONES_HOME/pallbearer/keycloak/data/keycloak_thrones_db_20221114.gz

## drop and create database
cat << EOF | psql -Upostgres
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'keycloak_thrones_db';

DROP DATABASE IF EXISTS keycloak_thrones_db;

CREATE DATABASE keycloak_thrones_db
WITH OWNER = postgres
TEMPLATE template0
ENCODING = 'UTF8'
LC_COLLATE = 'en_US.UTF-8'
LC_CTYPE = 'en_US.UTF-8'
CONNECTION LIMIT = -1;
EOF

gunzip -c $BACKUP_FILE | psql -h $PGHOST -Upostgres -d keycloak_thrones_db

## update conf
cp $THRONES_HOME/pallbearer/keycloak/conf/keycloak.conf $KEYCLOAK_HOME/conf/keycloak.conf

popd
set +e
