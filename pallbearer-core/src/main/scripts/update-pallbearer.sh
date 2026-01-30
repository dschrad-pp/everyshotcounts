#! /bin/sh

sudo systemctl stop pallbearer.service

pushd $THRONES_HOME/pallbearer
git pull origin development
mvn clean install -DskipTests=true
popd

sudo systemctl start pallbearer.service

echo "=== systemctl status pallbearer.service ==="
sudo systemctl status pallbearer.service

echo "=== journalctl -u pallbearer.service ==="
sudo journalctl -u pallbearer.service | tail -n 500

