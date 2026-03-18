package com.lektralabs.thrones.crm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lektralabs.thrones.crm.model.CrmRegistration;
import okhttp3.*;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@jakarta.enterprise.context.ApplicationScoped
public class CrmApiClient {
    private static final Logger logger = Logger.getLogger(CrmApiClient.class);

    private static final String CRM_BASE_URL = "https://esc-crm-backend.onrender.com/api/v1";
    private static final String AUTH_ENDPOINT = "/auth/applogin/";
    private static final String REGISTRATIONS_ENDPOINT = "/registrations/";
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "crm.ios.api.key")
    String crmIosApiKey;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private long tokenExpiryTime;

    public CrmApiClient() {
        // Configure OkHttpClient with longer timeouts for Render.com API
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .callTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Authenticate and get access token
     */
    public String getAccessToken() throws IOException {
        // Check if token is still valid (with 5 minute buffer)
        if (accessToken != null && System.currentTimeMillis() < tokenExpiryTime - 300000) {
            return accessToken;
        }

        String url = CRM_BASE_URL + AUTH_ENDPOINT;
        
        // TODO: Move these to configuration
        String username = System.getenv("CRM_USERNAME");
        String password = System.getenv("CRM_PASSWORD");
        
        if (username == null) {
            username = "athlete";
        }
        if (password == null) {
            password = "athlete1234";
        }

        RequestBody body = RequestBody.create(
            String.format("{\"grant_type\":\"password\",\"username\":\"%s\",\"password\":\"%s\"}", 
                username, password),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build();

        logger.info("Attempting to authenticate with CRM API at: " + url);
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("Failed to authenticate with CRM API. Status: " + response.code() + ", Error: " + errorBody);
            }

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            
            // Extract token from response
            // Assuming response format: {"access_token": "...", "expires_in": 3600}
            if (responseMap.containsKey("access_token")) {
                accessToken = (String) responseMap.get("access_token");
                // Default to 1 hour expiry if not provided
                long expiresIn = responseMap.containsKey("expires_in") 
                    ? ((Number) responseMap.get("expires_in")).longValue() * 1000 
                    : 3600000;
                tokenExpiryTime = System.currentTimeMillis() + expiresIn;
                logger.info("Successfully authenticated with CRM API");
                return accessToken;
            } else {
                throw new IOException("Access token not found in response: " + responseBody);
            }
        } catch (java.net.SocketTimeoutException e) {
            logger.error("Timeout connecting to CRM API. URL: " + url, e);
            throw new IOException("Timeout connecting to CRM API. The server may be slow or unreachable. URL: " + url, e);
        } catch (IOException e) {
            logger.error("Error authenticating with CRM API. URL: " + url, e);
            throw new IOException("Failed to authenticate with CRM API. URL: " + url + ", Error: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch all registrations from CRM API
     */
    public List<CrmRegistration> fetchRegistrations() throws IOException {
        String token = getAccessToken();
        String url = CRM_BASE_URL + REGISTRATIONS_ENDPOINT;

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + token)
            .addHeader("Accept", "application/json")
            .build();

        logger.info("Fetching registrations from CRM API at: " + url);
        Response response = client.newCall(request).execute();
        try {
            if (response.code() == 401) {
                // Token expired, refresh and retry
                response.close();
                accessToken = null;
                token = getAccessToken();
                request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Accept", "application/json")
                    .build();
                response = client.newCall(request).execute();
            }

            return processRegistrationsResponse(response);
        } catch (java.net.SocketTimeoutException e) {
            logger.error("Timeout fetching registrations from CRM API. URL: " + url, e);
            throw new IOException("Timeout fetching registrations from CRM API. The server may be slow or unreachable. URL: " + url, e);
        } catch (IOException e) {
            logger.error("Error fetching registrations from CRM API. URL: " + url, e);
            throw new IOException("Failed to fetch registrations from CRM API. URL: " + url + ", Error: " + e.getMessage(), e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private List<CrmRegistration> processRegistrationsResponse(Response response) throws IOException {
        if (response.code() != 200) {
            String errorBody = response.body() != null ? response.body().string() : "No error body";
            throw new IOException("Failed to fetch registrations from CRM API. Status: " + response.code() + ", Error: " + errorBody);
        }

        String responseBody = response.body().string();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        
        // Extract data array from response
        if (responseMap.containsKey("data") && responseMap.get("data") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseMap.get("data");
            
            return dataList.stream()
                .map(data -> objectMapper.convertValue(data, CrmRegistration.class))
                .toList();
        } else {
            throw new IOException("Unexpected response format: " + responseBody);
        }
    }

    ///**
     //* Validate user credentials against CRM API
     //*/
    /**
 * Validate user credentials against CRM /api/auth/validate-credentials endpoint
 */

public boolean validateUserCredentials(String username, String password) throws IOException {
    // Updated endpoint path
    String url = "https://crm.everyshotcounts.ai/api/auth/validate-credentials";
    
    // Get CRM API key from environment variable
    // String crmApiKey = System.getenv("CRM_IOS_API_KEY");
    // if (crmApiKey == null || crmApiKey.isBlank()) {
    //     logger.error("CRM_IOS_API_KEY environment variable is not set");
    //     throw new IOException("CRM API key not configured");
    // }
    // With:
if (crmIosApiKey == null || crmIosApiKey.isBlank()) {
    logger.error("CRM iOS API key is not configured");
    throw new IOException("CRM API key not configured");
}

    // Build request body
    RequestBody body = RequestBody.create(
        String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password),
        MediaType.parse("application/json")
    );

    Request request = new Request.Builder()
        .url(url)
        .post(body)
        // .addHeader("Authorization", "Bearer " + crmApiKey)
        .addHeader("Authorization", "Bearer " + crmIosApiKey)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json")
        .build();

    logger.info("Validating CRM credentials for user: " + username);
    try (Response response = client.newCall(request).execute()) {
        if (response.code() == 200) {
            // Optionally parse and store the user data from response
            String responseBody = response.body().string();
            logger.info("CRM validation successful for user: " + username);
            return true;
        } else if (response.code() == 401 || response.code() == 400) {
            logger.info("CRM validation failed for user: " + username);
            return false;
        } else {
            String errorBody = response.body() != null ? response.body().string() : "No error body";
            logger.error("Unexpected CRM response. Status: " + response.code() + ", Error: " + errorBody);
            throw new IOException("Unexpected CRM response. Status: " + response.code());
        }
    } catch (java.net.SocketTimeoutException e) {
        logger.error("Timeout validating CRM credentials", e);
        throw new IOException("Timeout connecting to CRM API", e);
    }
}
}
