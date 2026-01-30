Install
======

* Initial Setup
* Testing
* Session Timeout Configuration


# Initial Setup

Setup Env Variables

```bash
export KEYCLOAK_HOME=/usr/local/keycloak
```

Install Keycloak

```bash
cd ~/Downloads
wget https://github.com/keycloak/keycloak/releases/download/17.0.1/keycloak-17.0.1.zip
cd /usr/local
sudo rm keycloak
sudo rm -rf keycloak-17.0.1
sudo unzip ~/Downloads/keycloak-17.0.1.zip
sudo ln -s keycloak-17.0.1 keycloak
sudo chown -R `whoami`:admin keycloak-17.0.1
```

Setup Database
Note: This will clobber and overwrite your keycloak DB. Keycloak should not be running. 

```bash
pushd $THRONES_HOME/pallbearer
./src/main/scripts/keycloak-bootstrap.sh
popd
```

When `keycloak-bootstrap.sh` is finished, take a look at
`$KEYCLOAK_HOME/conf/keycloak.conf`. Verify the configuration settings,
particularly the postgres password.

Start Keycloak
```bash
cd $KEYCLOAK_HOME
./bin/kc.sh start-dev
open http://localhost:8080
```

# Test

Run through testing setup in [README-testing.md](./docs/README-testing.md)

# Session Timeout Configuration

In development, you may find that the default timeouts are very short,
particularly for mobile development where sessions tend to not timeout

Note: I am not a keycloak expert. These setting seem to do the trick

Login to Keycloak Admin and select the Thrones Realm (likely selected by
default)

In the lefthand nav bar, under Configure, select Realm Settings and then
the Tokens tab

* SSO Session Idle: 30 Days
* SSO Session Max: 30 Days
* Access Token Lifespan: 30 Days

----

