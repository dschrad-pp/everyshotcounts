
```bash
export PALLBEARER_HOME=`cwd`
git pull origin development
mvn clean install -DskipTests=true
sudo -E java -jar target/quarkus-app/quarkus-run.jar -Dquarkus.profile=prod
```