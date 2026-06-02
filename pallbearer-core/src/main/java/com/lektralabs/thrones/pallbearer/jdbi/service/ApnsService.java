package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@ApplicationScoped
public class ApnsService {

    private static final Logger logger = Logger.getLogger(ApnsService.class);
    private static final String APNS_HOST_PROD = "https://api.push.apple.com";
    private static final String APNS_HOST_SANDBOX = "https://api.sandbox.push.apple.com";

    @ConfigProperty(name = "apns.key-path", defaultValue = "")
    String keyPath;

    @ConfigProperty(name = "apns.key-id", defaultValue = "")
    String keyId;

    @ConfigProperty(name = "apns.team-id", defaultValue = "")
    String teamId;

    @ConfigProperty(name = "apns.bundle-id", defaultValue = "")
    String bundleId;

    @ConfigProperty(name = "apns.production", defaultValue = "false")
    boolean production;

    private ECPrivateKey privateKey;
    private HttpClient httpClient;
    private volatile String cachedJwt;
    private volatile long jwtCreatedAt;

    @PostConstruct
    public void init() {
        if (keyPath == null || keyPath.isBlank()) {
            logger.warn("APNs key-path not configured — push notifications disabled");
            return;
        }
        try {
            privateKey = loadPrivateKey(keyPath);
            httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            logger.infof("APNs service initialized (production=%s, bundleId=%s)", production, bundleId);
        } catch (Exception e) {
            logger.errorf(e, "Failed to initialize APNs service — push notifications disabled");
        }
    }

    public void sendDrillCompletionNotification(
            String deviceToken,
            String athleteFirstName,
            String athleteLastName,
            String drillName,
            UUID athleteId,
            UUID drillId,
            UUID drillItemId,
            Runnable onInvalidToken) {
        if (privateKey == null) {
            return;
        }
        try {
            String jwt = getOrRefreshJwt();
            String host = production ? APNS_HOST_PROD : APNS_HOST_SANDBOX;
            String url = host + "/3/device/" + deviceToken;

            String body = String.format(
                "{\"aps\":{\"alert\":{\"title\":\"Drill Completed\",\"body\":\"%s %s finished %s\"}," +
                "\"badge\":1,\"sound\":\"default\"}," +
                "\"type\":\"drill_completed\"," +
                "\"athleteId\":\"%s\"," +
                "\"drillId\":\"%s\"," +
                "\"drillItemId\":\"%s\"}",
                escapeJson(athleteFirstName), escapeJson(athleteLastName), escapeJson(drillName),
                athleteId, drillId, drillItemId
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("authorization", "bearer " + jwt)
                    .header("apns-topic", bundleId)
                    .header("apns-push-type", "alert")
                    .header("apns-priority", "10")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 200) {
                logger.debugf("APNs push sent to coach for drillId=%s", drillId);
            } else if (status == 410) {
                // Token is no longer valid — remove it
                logger.infof("APNs returned 410 Gone for token, removing from DB");
                onInvalidToken.run();
            } else {
                logger.warnf("APNs returned HTTP %d: %s", status, response.body());
            }
        } catch (Exception e) {
            logger.warnf(e, "Failed to send APNs push for drillId=%s", drillId);
        }
    }

    private String getOrRefreshJwt() {
        long now = System.currentTimeMillis() / 1000;
        // Refresh if older than 55 minutes (APNs max is 60 min)
        if (cachedJwt == null || (now - jwtCreatedAt) > 3300) {
            cachedJwt = JWT.create()
                    .withIssuer(teamId)
                    .withKeyId(keyId)
                    .withIssuedAt(new Date())
                    .sign(Algorithm.ECDSA256(null, privateKey));
            jwtCreatedAt = now;
        }
        return cachedJwt;
    }

    private ECPrivateKey loadPrivateKey(String path) throws Exception {
        String pem = new String(Files.readAllBytes(Paths.get(path)));
        pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                  .replace("-----END PRIVATE KEY-----", "")
                  .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(spec);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
