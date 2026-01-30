package com.lektralabs.leagueapps;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

public class ApiFetcher implements LeagueAppsConstants {

    private static final Logger logger = Logger.getLogger(ApiFetcher.class);

    private final OkHttpClient client;
    private final ApiHandler apiHandler;
    private final Random random;

    private final AccessTokenFactory accessTokenFactory;

    private long lastUpdated = 0;
    private long lastId = 0;
    private String accessToken = null;
    private int batchCount = 0;
    private int maxAttempts = 5;

    public ApiFetcher(OkHttpClient client, ApiHandler apiHandler, Random random, long lastUpdated, long lastId) {
        this.client = client;
        this.apiHandler = apiHandler;
        this.random = random;
        this.accessTokenFactory = new AccessTokenFactory(client);
        this.lastUpdated = lastUpdated;  // Use the passed value
        this.lastId = lastId;           // Use the passed value
        logger.infof("Initialized fetcher for %s with lastUpdated=%d, lastId=%d",
                apiHandler.getEndpoint(), lastUpdated, lastId);
    }

    public ApiFetcher(OkHttpClient client, ApiHandler apiHandler, Random random) {
        this(client, apiHandler, random, 0, 0);
    }

    public void fetch() throws Exception {
        logger.info("fetching user records");
        boolean hasMore = true;
        int retryAttempts = 0;
        
        while (hasMore && retryAttempts < maxAttempts) {
            if (accessToken == null) {
                accessToken = accessTokenFactory.requestAccessToken();
                if (accessToken == null) {
                    logger.error("Failed to get access token after retries");
                    break;
                }
            }

            Request request = buildRequest();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 401) {
                    logger.error("Error [%d]: %s".formatted(response.code(), response.body()));
                    accessToken = null;
                    retryAttempts++;
                    continue;
                }

                if (response.code() == 429 || response.code() >= 500) {
                    int waitSeconds = exponentialBackoff(retryAttempts, 1.42, 5);
                    logger.info("Retry in [%d] on error status [%d]: %s".formatted(waitSeconds, response.code(), response.message()));
                    Thread.sleep(waitSeconds * 1000L);
                    retryAttempts++;
                    continue;
                }

                if (response.code() != 200) {
                    logger.error("Unexpected error [%d]: %s".formatted(response.code(), response.message()));
                    break;
                }

                String responseBody = response.body().string();
                JSONArray records = new JSONArray(responseBody);

                if (records.length() == 0) {
                    logger.info("Fetch completed - no more records found");
                    hasMore = false;
                    break;
                } else {
                    logger.infof("Processing batch %d with %d records", batchCount + 1, records.length());
                }

                // Process this batch and update lastUpdated/lastId for next batch
                parseResponse(responseBody, records);
                
                // Reset retry attempts on successful fetch
                retryAttempts = 0;
                
                // Continue to fetch next batch with updated lastUpdated/lastId
            }
        }
    }

    private int parseResponse(String responseBody, JSONArray records) throws IOException {
        batchCount++;

        logger.infof("Processing batch %d with %d records (lastUpdated: %d, lastId: %d)",
                batchCount, records.length(), lastUpdated, lastId);

        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.getJSONObject(i);
            lastUpdated = Math.max(lastUpdated, record.getLong("lastUpdated"));
            lastId = Math.max(lastId, record.getLong("id"));
        }

        apiHandler.handlePayload(batchCount, responseBody);

        return 0;
    }

    @NotNull
    private Request buildRequest() {
        HttpUrl.Builder urlBuilder = HttpUrl
                .parse(ADMIN_HOST + "/v2/sites/" + SITE_ID + "/export/" + apiHandler.getEndpoint())
                .newBuilder();

        urlBuilder.addQueryParameter("last-updated", String.valueOf(lastUpdated));
        urlBuilder.addQueryParameter("last-id", String.valueOf(lastId));

        return new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    private int exponentialBackoff(int attempts, double slotTime, int maxSlots) {
        if (maxSlots > 0) {
            attempts = Math.min(attempts, maxSlots);
        }
        return random.nextInt((int) Math.pow(2, attempts)) * (int) slotTime;
    }

}
