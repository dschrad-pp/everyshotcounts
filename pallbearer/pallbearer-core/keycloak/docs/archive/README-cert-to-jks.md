## Note: We did not do this

Certbot will refresh SSL certs for nginx periodcailly and automatically. Setting this up to refresh
as well would be a PITA.

Converting Standard certbot artifacts to a JKS
Once you have identified the right cert, you need to recreate the keystore with the new key and cert.

0.- Create a dir to store your keystore, I’m using /etc/tomcat8/keystore/ for this example, you should use the path that you want.

mkdir -p /etc/tomcat8/keystore/

1.- Create a pkcs12 store (change HERETHEPASSWORD with the password you want):

openssl pkcs12 -export -in /etc/letsencrypt/live/gpsowl.com/fullchain.pem -inkey /etc/letsencrypt/live/gpsowl.com/privkey.pem -out /etc/tomcat8/keystore/gpsowl.com.p12 -password pass:HERETHEPASSWORD

2.- Import pkcs12 store into a keystore (change HERETHEPASSWORD with the password used in previous command):

keytool -importkeystore -srckeystore /etc/tomcat8/keystore/gpsowl.com.p12 -srcstoretype pkcs12 -srcstorepass HERETHEPASSWORD -destkeystore /etc/tomcat8/keystore/gpsowl.com.keystore -deststoretype jks -deststorepass HERETHEPASSWORD

3.- Configure your tomcat to use the right keystore and pass:

4.- Restart or reload your tomcat.