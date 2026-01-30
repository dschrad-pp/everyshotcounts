
## Keycloak DB

DROP DATABASE IF EXISTS keycloak_db;

CREATE DATABASE keycloak_db
WITH OWNER = postgres
TEMPLATE template0
ENCODING = 'UTF8'
LC_COLLATE = 'en_US.UTF-8'
LC_CTYPE = 'en_US.UTF-8'
CONNECTION LIMIT = -1;


----
## Keycloak on AWS
## see https://medium.com/@hasnat.saeed/setup-keycloak-server-on-ubuntu-18-04-ed8c7c79a2d9

sudo yum update
sudo amazon-linux-extras install postgresql11
sudo yum install -y java-11-amazon-corretto-headless
sudo wget https://github.com/keycloak/keycloak/releases/download/12.0.4/keycloak-12.0.4.zip
sudo unzip keycloak-12.0.4.zip
sudo ln -s keycloak-12.0.4 keycloak
sudo rm keycloak-12.0.4.zip
sudo groupadd keycloak
sudo useradd -r -g keycloak -d /opt/keycloak -s /sbin/nologin keycloak
sudo chown -R keycloak:keycloak /opt/keycloak /opt/keycloak-12.0.4
sudo mkdir -p /etc/keycloak
sudo cp /opt/keycloak/docs/contrib/scripts/systemd/wildfly.conf /etc/keycloak/keycloak.conf
sudo cp /opt/keycloak/docs/contrib/scripts/systemd/launch.sh /opt/keycloak/bin/
sudo chown keycloak:keycloak /opt/keycloak/bin/launch.sh
sudo emacs -nw /opt/keycloak/bin/launch.sh
## change /opt/wildfly to /opt/keycloak
## copy mitchell/config/keycloak-server to ec2 instance
sudo cp /opt/keycloak/docs/contrib/scripts/systemd/wildfly.service /etc/systemd/system/keycloak.service
sudo systemctl daemon-reload
sudo systemctl enable keycloak


ssh -L 8180:localhost:8080 kazzah-sso
ssh -L 8443:localhost:8443 kazzah-sso

## nginx with Let's Encrypt
sudo amazon-linux-extras install nginx1
sudo systemctl enable nginx
sudo systemctl start nginx


31-23-16 nginx]$ sudo touch /etc/letsencrypt/options-ssl-nginx.conf
touch: cannot touch ‘/etc/letsencrypt/options-ssl-nginx.conf’: No such file or directory
[ec2-user@ip-172-31-23-16 nginx]$ sudo mkdir -p /etc/letsencrypt
[ec2-user@ip-172-31-23-16 nginx]$ sudo touch /etc/letsencrypt/options-ssl-nginx.conf

192  sudo yum install python3 python3-venv
193  sudo python3 -m venv /opt/certbot/
194  sudo /opt/certbot/bin/pip install --upgrade pip
195  sudo /opt/certbot/bin/pip install certbot certbot-nginx
196  sudo ln -s /opt/certbot/bin/certbot /usr/bin/certbot
197  sudo certbot certonly --nginx
198  cat /etc/letsencrypt/options-ssl-nginx.conf
199  cat /opt/certbot/lib64/python3.7/site-packages/certbot_nginx/_internal/tls_configs/options-ssl-nginx-tls13-session-tix-on.conf
200  sudo cp /opt/certbot/lib64/python3.7/site-packages/certbot_nginx/_internal/tls_configs/options-ssl-nginx-tls13-session-tix-on.conf /etc/letsencrypt/options-ssl-nginx.conf
201  echo "0 0,12 * * * root /opt/certbot/bin/python -c 'import random; import time; time.sleep(random.random() * 3600)' && certbot renew -q" | sudo tee -a /etc/crontab > /dev/null
202  history



sudo hostnamectl set-hostname sso.kazzah.com
ls -al


address to bind to
WILDFLY_BIND=0.0.0.0
[ec2-user@ip-172-31-23-16 configuration]$ sudo mkdir -p /var/run/keycloak
[ec2-user@ip-172-31-23-16 configuration]$ sudo chown -R keycloak:keycloak /var/run/keycloak/
[ec2-user@ip-172-31-23-16 configuration]$






KEYCLOAK_FRONTEND_URL ???

https://access.redhat.com/articles/3266201