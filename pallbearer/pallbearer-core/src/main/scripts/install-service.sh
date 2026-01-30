#! /bin/sh

sudo chown -R root:root /media
sudo chown -R root:root /etc/letsencrypt
sudo chown -R root:root /tmp/vertx-cache

sudo mkdir -p /etc/systemd/system/pallbearer.service.d
sudo cp $THRONES_HOME/pallbearer/src/main/resources/pallbearer.service /etc/systemd/system
sudo cp $THRONES_HOME/pallbearer/src/main/resources/pallbearer.conf /etc/systemd/system/pallbearer.service.d

sudo systemctl daemon-reload
sudo systemctl enable pallbearer.service
sudo systemctl start pallbearer.service
sudo systemctl status pallbearer.service

sudo journalctl -u pallbearer.service