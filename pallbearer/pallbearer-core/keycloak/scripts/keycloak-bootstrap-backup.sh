#! /bin/bash -xv

# BACKUP_FILE=$THRONES_HOME/pallbearer/keycloak/data/keycloak_thrones_db.gz
BACKUP_FILE=$THRONES_HOME/pallbearer/keycloak/data/keycloak_thrones_db_20221114.gz
DB_HOST=localhost

pg_dump -U postgres -h $DB_HOST keycloak_thrones_db | gzip > "$BACKUP_FILE"
