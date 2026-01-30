#! /bin/sh


sudo mkdir -p /etc/systemd/system/thrones-cv.service.d
sudo cp ~/thrones-cv.service /etc/systemd/system

sudo systemctl daemon-reload
sudo systemctl enable thrones-cv.service
sudo systemctl start thrones-cv.service
sudo systemctl status thrones-cv.service

sudo journalctl -u thrones-cv.service