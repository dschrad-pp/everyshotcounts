## README - pallbearer

### Environment Readme

Follow setup instructions in [README.md](../README.md)


### Keycloak Setup

Follow Keycloak setup instructions in [README-install.md](./keycloak/README-install.md)


### Pallbearer Setup

Add to your profile: 
```bash
## Keycloak
export KEYCLOAK_URL=http://localhost:8080

## Thrones
export THRONES_HOME=<root of thrones project i.e. /Volumes/Work/github.com/lektralabs/thrones>
```

Seeding and starting Pallbearer
```bash
./src/main/scripts/bootstrap.sh ## only need to call once, or for each destroy
./src/main/scripts/kstart.sh
```

Navigate to locahost:8000/api to see pallbearer API endpoint documentation

----

