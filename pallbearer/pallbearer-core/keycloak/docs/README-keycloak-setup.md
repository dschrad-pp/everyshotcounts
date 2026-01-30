## Internal Setup Document

https://logonlabs.com/articles/how-to-set-up-keycloak-openid-connect/

This only needs to be done once, by the Keycloak admin (KB)

Login:
http://localhost:8080/admin/master/console

Hover over 'Realm' - Add Ream: `thrones_realm`

Hover over `OpenID Endpoint Configuration`
Click Link
Copy entry for `token_endpoint`:  `http://localhost:8080/realms/thrones_realm/protocol/openid-connect/token`

Add roles: ADMIN, USER

Add Groups: ADMIN, USER. Associate appropriate Roles

Add users: kbrumer, sboles to ADMIN Group
 - User enabled, Email Verified, Add to ADMIN Group
 - set credentials for each

Add a Client: `thrones_client`
  Client Protocol: `openid-connect`
  Access Type: `confidential`
  Valid Redirect URLS : `http://localhost:8000/api/redirect`
  Go to Credentials Tab and copy the Secret: `iv2xJ1hwoeB9yClZoNeCTL77OgUsyMmE`
