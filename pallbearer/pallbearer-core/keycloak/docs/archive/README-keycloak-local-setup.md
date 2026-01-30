## Keycloak Setup

This does not need to be followed. It was the instrcutions used to get Keycloak and an SSL 
configured successfully. 

https://wjw465150.gitbooks.io/keycloak-documentation/content/server_installation/topics/network/https.html

```bash
cd $KAZZAH_HOME/mitchell/config/certs

> keytool -genkey -alias kazzah.com -keyalg RSA -keystore keycloak.jks -validity 10950
##  Keystore password: suw70OTgAJd6ulFi

keytool -certreq -alias kazzah.com -keystore keycloak.jks > keycloak.careq

xzTVCctHKhNA3oME

GwG6f0FIRf3MuhHr

```
## add to .bash_profile
export jboss.socket.binding.port-offset=100
export KEYCLOAK_HOME=/usr/local/keycloak

cd /usr/local
wget https://github.com/keycloak/keycloak/releases/download/12.0.4/keycloak-12.0.4.zip
sudo unzip keycloak-12.0.4.zip
sudo ln -s keycloak-12.0.4 keycloak
sudo chown -R `whoami`:admin keycloak-12.0.4 keycloak
cd modules/system/layers/keycloak
mkdir -p org/postgresql/main
cd org/postgres/main
cp $HOME/.m2/repository/org/postgresql/postgresql/42.2.18/postgresql-42.2.18.jar .
cat <<EOF > module.xml
<?xml version="1.0" ?>
<module xmlns="urn:jboss:module:1.3" name="org.postgresql">

    <resources>
        <resource-root path="postgresql-42.2.18.jar"/>
    </resources>

    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
    </dependencies>
</module>
EOF

## see https://www.keycloak.org/docs/latest/server_installation/index.html#_database
emacs -nw $KEYCLOAK_HOME/standalone/configuration/standalone.xml

-- add
<driver name="postgresql" module="org.postgresql">
  <xa-datasource-class>org.postgresql.xa.PGXADataSource</xa-datasource-class>
</driver>

## 
cp $KAZZAH_HOME/mitchell/config/keycloak/standalone.xml $KEYCLOAK_HOME/standalone/configuration/

./bin/standalone.sh -Djboss.http.port=8180 -Djboss.https.port=8543
```



docker run --name keycloak -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -e KEYCLOAK_IMPORT=/tmp/quarkus-realm.json -v $KAZZAH_HOME/mitchell/config/quarkus-realm.json:/tmp/quarkus-realm.json -p 8280:8080 -p 8643:8443 jboss/keycloak


## add users
https://www.appsdeveloperblog.com/keycloak-rest-api-create-a-new-user/