package com.lektralabs.leagueapps;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Optional;

public class AccessTokenFactory implements LeagueAppsConstants {

    private static final Logger logger = Logger.getLogger(AccessTokenFactory.class);

    private final OkHttpClient client;

    public AccessTokenFactory(OkHttpClient client) {
        this.client = client;
    }

    public String requestAccessToken() throws Exception {
        long now = Instant.now().getEpochSecond();

        return getPrivateKey().map(privateKey -> {
            String token = JWT.create()
                    .withAudience("https://auth.leagueapps.io/v2/auth/token")
                    .withIssuer(CLIENT_ID)
                    .withSubject(CLIENT_ID)
                    .withIssuedAt(Instant.ofEpochSecond(now))
                    .withExpiresAt(Instant.ofEpochSecond(now + 300))
                    .sign(Algorithm.RSA256(null, privateKey));

            RequestBody formBody = new FormBody.Builder()
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                    .add("assertion", token)
                    .build();

            Request request = new Request.Builder()
                    .url(DEFAULT_AUTH + "/v2/auth/token")
                    .post(formBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body().string());
                    return jsonResponse.getString("access_token");
                } else {
                    logger.warn("Failed to get access_token: [%s] ".formatted(response.body()));
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).orElseGet(() -> "");
    }

    private Optional<RSAPrivateKey> getPrivateKey() {
        try {
            String rsaPrivateKey = IOUtils.toString(getClass().getResourceAsStream(PEM_FILE), StandardCharsets.UTF_8);
            rsaPrivateKey = rsaPrivateKey.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PRIVATE KEY-----", "");
            byte[] data = Base64.decodeBase64(rsaPrivateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(data);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
            return Optional.of(privateKey);
        } catch (Exception e) {
            logger.warn("Error getting private key from PEM file", e);
            return Optional.empty();
        }
    }

}
