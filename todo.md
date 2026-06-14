# 06/14/26

- We need to update apns.production=false to true when we deploy to app store/testflight. Steps: 

Option A — systemd env var (recommended)
This is the deploy-safe way. Quarkus lets any env var override a property, and apns.production maps to APNS_PRODUCTION. The override file is never touched by git pull or mvn, so it survives every deploy — unlike editing application.properties.


sudo systemctl edit quarkus-backend.service
Add under [Service]:


Environment=APNS_PRODUCTION=true
Then apply (config is only read at startup, so a restart is required):


sudo systemctl daemon-reload
sudo systemctl restart quarkus-backend.service
sudo journalctl -u quarkus-backend.service -f | grep -i apns
Confirm you see: APNs service initialized (production=true, bundleId=com.every.shot.counts). To go back, change it to false (or delete the line) and restart.

