package com.lektralabs.thrones.leagueapps;

import com.lektralabs.leagueapps.ApiFetcher;
import com.lektralabs.thrones.leagueapps.handlers.MemberHandler;
import com.lektralabs.thrones.leagueapps.handlers.RegistrationHandler;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SystemPropertiesPartial;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.SystemPropertiesRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.RegistrationItem;
import com.lektralabs.thrones.pallbearer.jdbi.service.SystemPropertiesService;
import com.lektralabs.thrones.leagueapps.model.json.Registration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import okhttp3.OkHttpClient;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import com.lektralabs.thrones.pallbearer.jdbi.model.item.EscActionItem;
import com.lektralabs.thrones.leagueapps.model.json.Member;
import com.lektralabs.leagueapps.AccessTokenFactory;
import com.lektralabs.leagueapps.LeagueAppsConstants;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.EscLeagueAppsMemberActionPartial;
import com.lektralabs.thrones.pallbearer.common.UserPropertyConstants;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import com.lektralabs.thrones.leagueapps.dto.LeagueAppsBasicDto;
import com.lektralabs.thrones.pallbearer.jdbi.service.LeagueAppsMemberService;
import com.lektralabs.thrones.pallbearer.jdbi.service.LeagueAppsRegistrationService;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsMemberRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsRegistrationRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.BasicUserRow;

import static com.lektralabs.thrones.pallbearer.common.SystemConstants.LEAGUE_APPS_RECORD_LAST_UPDATE_ID;

@ApplicationScoped
public class LeagueAppsIntegration extends LeagueAppsActions {

    private static final Logger logger = Logger.getLogger(LeagueAppsIntegration.class);

    @Inject
    MemberHandler memberHandler;

    @Inject
    RegistrationHandler registrationHandler;

    @Inject
    SystemPropertiesService systemPropertiesService;
    
    @Inject
    LeagueAppsMemberService leagueAppsMemberService;
    
    @Inject
    LeagueAppsRegistrationService leagueAppsRegistrationService;

    public int update() {
        // Retrieve the last sync timestamp from the database
        // This represents the last time we synced, so we fetch records updated AFTER this time
        long lastSyncTimestamp = getLastUpdated();
        logger.info("Retrieved last sync timestamp: " + lastSyncTimestamp);
        logger.info("Fetching LeagueApps records updated after: " + lastSyncTimestamp);

        try {
            // Fetch records using the previous timestamp (gets all records since last sync)
            fetchUserRecords(lastSyncTimestamp);
        } catch (Exception e) {
            logger.warn("Error integrating members", e);
            throw new RuntimeException(e);
        }
        try {
            // Fetch records using the previous timestamp (gets all records since last sync)
            fetchRegistrationRecords(lastSyncTimestamp);
        } catch (Exception e) {
            logger.warn("Error integrating registration", e);
            throw new RuntimeException(e);
        }
        
        // Update actions using the previous timestamp
        int actionsUpdated = escLeagueAppsMemberActionService.updateActions(lastSyncTimestamp);
        
        // Update the lastUpdated timestamp AFTER fetching
        // This ensures we don't miss records created between syncs
        updateLastUpdated();
        long newLastUpdated = getLastUpdated();
        logger.info("Updated lastUpdated timestamp to: " + newLastUpdated + " for next sync");
        
        return actionsUpdated;
    }

    /**
     * Complete sync endpoint that properly paginates through ALL LeagueApps records
     * and stores them in the database, then processes action items to create users.
     * 
     * This method:
     * 1. Fetches ALL members from LeagueApps API (with proper pagination)
     * 2. Fetches ALL registrations from LeagueApps API (with proper pagination)
     * 3. Stores members in t_league_apps_member
     * 4. Stores registrations in t_league_apps_registration
     * 5. Creates action items in t_esc_league_apps_member_action
     * 6. Processes action items to create users in t_user (for paid users)
     * 7. Updates payment status for existing users
     * 
     * @param lastUpdatedTimestamp Optional timestamp to use for sync. If not provided, uses stored timestamp from database.
     * @return Map with sync statistics
     */
    public jakarta.ws.rs.core.Response syncComplete(Optional<Long> lastUpdatedTimestamp) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        System.out.println("=========================================================================== \n");
        System.out.println("Starting complete sync");
        System.out.println("=========================================================================== \n");
        try {
            // Step 1: Get last sync timestamp (use provided parameter or get from database)
            long lastSyncTimestamp;
            boolean usingProvidedTimestamp = false;
            
            if (lastUpdatedTimestamp.isPresent() && lastUpdatedTimestamp.get() > 0) {
                lastSyncTimestamp = lastUpdatedTimestamp.get();
                usingProvidedTimestamp = true;
                System.out.println("=========================================================================== \n");
                System.out.println("Using provided lastUpdated timestamp: " + lastSyncTimestamp);
                System.out.println("=========================================================================== \n");
            } else {
                lastSyncTimestamp = getLastUpdated();
                System.out.println("=========================================================================== \n");
                System.out.println("Using stored lastUpdated timestamp: " + lastSyncTimestamp);
                System.out.println("=========================================================================== \n");
            }
            
            logger.info("Starting complete sync with lastUpdated timestamp: " + lastSyncTimestamp);
            
            // Step 2: Fetch and store ALL members (with proper pagination)
            int membersProcessed = 0;
            try {
                membersProcessed = fetchAndStoreAllMembers(lastSyncTimestamp);
                logger.info("Processed " + membersProcessed + " members");
            } catch (Exception e) {
                logger.error("Error fetching members", e);
                result.put("membersError", e.getMessage());
            }
            
            // Step 3: Fetch and store ALL registrations (with proper pagination)
            int registrationsProcessed = 0;
            try {
                registrationsProcessed = fetchAndStoreAllRegistrations(lastSyncTimestamp);
                logger.info("Processed " + registrationsProcessed + " registrations");
            } catch (Exception e) {
                logger.error("Error fetching registrations", e);
                result.put("registrationsError", e.getMessage());
            }
            
            // Step 4: Create action items from members and registrations
            int actionsCreated = 0;
            try {
                actionsCreated = escLeagueAppsMemberActionService.updateActions(lastSyncTimestamp);
                logger.info("Created " + actionsCreated + " action items");
            } catch (Exception e) {
                logger.error("Error creating action items", e);
                result.put("actionsError", e.getMessage());
            }
            
            // Step 5: Process action items to create/update users
            int usersProcessed = 0;
            int usersCreated = 0;
            int usersUpdated = 0;
            try {
                List<EscActionItem> actionItems = escLeagueAppsMemberActionService.getActionItems();
                if (actionItems != null && !actionItems.isEmpty()) {
                    logger.info("Processing " + actionItems.size() + " action items");
                    for (EscActionItem actionItem : actionItems) {
                        try {
                            EscActionEnum action = getAction(actionItem);
                            
                            if (action == EscActionEnum.NEW_REGISTRATION) {
                                // New user with payment - create user account
                                autoRegisterUser(actionItem);
                                usersCreated++;
                                usersProcessed++;
                            } else if (action == EscActionEnum.REGISTERED) {
                                // Existing user with payment - update payment status
                                updatePaymentStatus(actionItem);
                                usersUpdated++;
                                usersProcessed++;
                            } else if (action == EscActionEnum.STOP_PAYMENT) {
                                // Existing user stopped payment - unregister
                                unregisterUser(actionItem);
                                usersUpdated++;
                                usersProcessed++;
                            } else {
                                // NO_REGISTRATION_NO_PAYMENT - no action
                                logger.debug("No action for: " + action);
                            }
                            
                            // Mark action as completed
                            setActionCodeToCompleted(actionItem);
                        } catch (Exception e) {
                            logger.warn("Error processing action item: " + actionItem.getElama().getEmail(), e);
                            Object warnings = result.getOrDefault("processingWarnings", 0);
                            int warningCount = warnings instanceof Integer ? (Integer) warnings : 0;
                            result.put("processingWarnings", warningCount + 1);
                        }
                    }
                } else {
                    logger.info("No action items to process");
                }
            } catch (Exception e) {
                logger.error("Error processing action items", e);
                result.put("processingError", e.getMessage());
            }
            
            // Step 6: Update last sync timestamp (only if not using provided timestamp)
            long newLastUpdated;
            if (!usingProvidedTimestamp) {
                updateLastUpdated();
                newLastUpdated = getLastUpdated();
            } else {
                // If using provided timestamp, don't update the stored timestamp
                newLastUpdated = lastSyncTimestamp;
                logger.info("Skipping timestamp update - using provided timestamp for testing");
            }
            
            // Build result
            long duration = System.currentTimeMillis() - startTime;
            result.put("status", "SUCCESS");
            result.put("membersProcessed", membersProcessed);
            result.put("registrationsProcessed", registrationsProcessed);
            result.put("actionsCreated", actionsCreated);
            result.put("usersProcessed", usersProcessed);
            result.put("usersCreated", usersCreated);
            result.put("usersUpdated", usersUpdated);
            result.put("lastSyncTimestamp", lastSyncTimestamp);
            result.put("newSyncTimestamp", newLastUpdated);
            result.put("usingProvidedTimestamp", usingProvidedTimestamp);
            result.put("timestampUpdated", !usingProvidedTimestamp);
            result.put("durationMs", duration);
            
            logger.info("Complete sync finished successfully in " + duration + "ms");
            return jakarta.ws.rs.core.Response.ok(result).build();
            
        } catch (Exception e) {
            logger.error("Error in complete sync", e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
        }
    }

    /**
     * Fetch and store ALL members from LeagueApps API with proper pagination
     */
    private int fetchAndStoreAllMembers(long lastUpdated) throws Exception {
        OkHttpClient client = new OkHttpClient();
        AccessTokenFactory tokenFactory = new AccessTokenFactory(client);
        String accessToken = tokenFactory.requestAccessToken();
        
        if (accessToken == null || accessToken.isEmpty()) {
            throw new Exception("Failed to get access token from LeagueApps");
        }
        
        long currentLastUpdated = lastUpdated;
        long currentLastId = 0;
        int totalProcessed = 0;
        boolean hasMore = true;
        int batchCount = 0;
        
        while (hasMore) {
            String url = LeagueAppsConstants.ADMIN_HOST + "/v2/sites/" + LeagueAppsConstants.SITE_ID + 
                        "/export/" + LeagueAppsConstants.MEMBERS2_RECORD_TYPE + 
                        "?last-updated=" + currentLastUpdated + "&last-id=" + currentLastId;
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 401) {
                    accessToken = tokenFactory.requestAccessToken();
                    if (accessToken == null || accessToken.isEmpty()) {
                        throw new Exception("Failed to refresh access token");
                    }
                    continue;
                }
                
                if (response.code() != 200) {
                    throw new Exception("API returned status code: " + response.code());
                }
                
                String responseBody = response.body().string();
                JSONArray records = new JSONArray(responseBody);

                System.out.println("=========================================================================== \n");
                System.out.println("records: " + records);
                System.out.println("=========================================================================== \n");
                if (records.length() == 0) {
                    hasMore = false;
                    break;
                }
                
                batchCount++;
                logger.infof("Processing members batch %d with %d records", batchCount, records.length());
                
                // Process batch through MemberHandler
                memberHandler.handlePayload(batchCount, responseBody);
                totalProcessed += records.length();
                
                // Update lastUpdated and lastId for next batch
                for (int i = 0; i < records.length(); i++) {
                    JSONObject record = records.getJSONObject(i);
                    long recordLastUpdated = record.getLong("lastUpdated");
                    long recordId = record.getLong("id");
                    currentLastUpdated = Math.max(currentLastUpdated, recordLastUpdated);
                    currentLastId = Math.max(currentLastId, recordId);
                }
            }
        }
        
        logger.info("Completed fetching members. Total processed: " + totalProcessed);
        return totalProcessed;
    }

    /**
     * Fetch and store ALL registrations from LeagueApps API with proper pagination
     */
    private int fetchAndStoreAllRegistrations(long lastUpdated) throws Exception {
        OkHttpClient client = new OkHttpClient();
        AccessTokenFactory tokenFactory = new AccessTokenFactory(client);
        String accessToken = tokenFactory.requestAccessToken();
        
        if (accessToken == null || accessToken.isEmpty()) {
            throw new Exception("Failed to get access token from LeagueApps");
        }
        
        long currentLastUpdated = lastUpdated;
        long currentLastId = 0;
        int totalProcessed = 0;
        boolean hasMore = true;
        int batchCount = 0;
        
        while (hasMore) {
            String url = LeagueAppsConstants.ADMIN_HOST + "/v2/sites/" + LeagueAppsConstants.SITE_ID + 
                        "/export/" + LeagueAppsConstants.REGISTRATIONS2_RECORD_TYPE + 
                        "?last-updated=" + currentLastUpdated + "&last-id=" + currentLastId;
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 401) {
                    accessToken = tokenFactory.requestAccessToken();
                    if (accessToken == null || accessToken.isEmpty()) {
                        throw new Exception("Failed to refresh access token");
                    }
                    continue;
                }
                
                if (response.code() != 200) {
                    throw new Exception("API returned status code: " + response.code());
                }
                
                String responseBody = response.body().string();
                JSONArray records = new JSONArray(responseBody);
                
                if (records.length() == 0) {
                    hasMore = false;
                    break;
                }
                
                batchCount++;
                logger.infof("Processing registrations batch %d with %d records", batchCount, records.length());
                
                // Process batch through RegistrationHandler
                registrationHandler.handlePayload(batchCount, responseBody);
                totalProcessed += records.length();
                
                // Update lastUpdated and lastId for next batch
                for (int i = 0; i < records.length(); i++) {
                    JSONObject record = records.getJSONObject(i);
                    long recordLastUpdated = record.getLong("lastUpdated");
                    long recordId = record.getLong("id");
                    currentLastUpdated = Math.max(currentLastUpdated, recordLastUpdated);
                    currentLastId = Math.max(currentLastId, recordId);
                }
            }
        }
        
        logger.info("Completed fetching registrations. Total processed: " + totalProcessed);
        return totalProcessed;
    }

    /**
     * Update payment status for existing registered user
     */
    private void updatePaymentStatus(EscActionItem actionItem) {
        EscLeagueAppsMemberActionPartial partial = actionItem.getElama();
        logger.info("EscLeagueAppsMemberActionPartial: " + partial);
        
        if (actionItem.getUser() == null || actionItem.getUser().getUserId() == null || actionItem.getUser().getUserId().isEmpty()) {
            logger.warn("No user found for payment status update: " + partial.getEmail());
            return;
        }
        
        UUID userId = actionItem.getUser().getUserId().get();
        Map<String, String> userPropertyMap = new HashMap<>();
        
        // Update payment status
        partial.getPaymentStatus().ifPresent(paymentStatus -> {
            userPropertyMap.put(UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_KEY, normalizePaymentStatus(paymentStatus));
        });
        
        if (!userPropertyMap.isEmpty()) {
            userPropertyService.addProperties(userId, userPropertyMap);
            logger.info("Updated payment status for user: " + userId);
        }
    }

    /**
     * Lightweight list of LeagueApps action items joined to users.
     */
    public jakarta.ws.rs.core.Response listBasicLeagueAppsUsers() {
        List<EscActionItem> actionItems = escLeagueAppsMemberActionService.getAllActionItems();
        
        List<LeagueAppsBasicDto> payload = actionItems.stream().map(item -> {
            EscLeagueAppsMemberActionPartial elama = item.getElama();
            String name = buildName(elama.getFirstName(), elama.getLastName());
            String email = elama.getEmail().orElse(null);
            String phone = elama.getMobilePhone().orElse(null);
            String username = elama.getUsernameLa();
            String keycloakId = item.getUser() != null && item.getUser().getKeycloakId() != null
                    ? item.getUser().getKeycloakId().toString()
                    : null;
            String statusCode = null;
            String actionCode = elama.getActionCode();
            String paymentStatus = elama.getPaymentStatus().orElse(null);
            return LeagueAppsBasicDto.builder()
                    .name(name)
                    .email(email)
                    .phone(phone)
                    .username(username)
                    .keycloakId(keycloakId)
                    .statusCode(statusCode)
                    .actionCode(actionCode)
                    .paymentStatus(paymentStatus)
                    .build();
        }).toList();
        
        return jakarta.ws.rs.core.Response.ok(payload).build();
    }

    /**
     * Lightweight list of LeagueApps members from t_league_apps_member.
     */
    public jakarta.ws.rs.core.Response listBasicMembers() {
        List<LeagueAppsMemberRow> members = leagueAppsMemberService.findAll(new FindOptions());
        List<LeagueAppsBasicDto> payload = members.stream().map(member -> {
            String name = buildName(member.getFirstName(), member.getLastName());
            String email = member.getEmail().orElse(null);
            String phone = member.getMobilePhone().orElse(null);
            String username = member.getUsername().orElse(null);
            return LeagueAppsBasicDto.builder()
                    .name(name)
                    .email(email)
                    .phone(phone)
                    .username(username)
                    .keycloakId(null)
                    .statusCode(null)
                    .actionCode(null)
                    .paymentStatus(null)
                    .build();
        }).toList();
        return jakarta.ws.rs.core.Response.ok(payload).build();
    }

    /**
     * Lightweight list of LeagueApps registrations from t_league_apps_registration.
     */
    public jakarta.ws.rs.core.Response listBasicRegistrations() {
        List<LeagueAppsRegistrationRow> registrations = leagueAppsRegistrationService.findAll(new FindOptions());
        List<LeagueAppsBasicDto> payload = registrations.stream().map(reg -> {
            String name = buildName(reg.getFirstName(), reg.getLastName());
            String email = reg.getEmail().orElse(null);
            String phone = reg.getPhone().orElse(reg.getParentPhone().orElse(null));
            String username = reg.getUserName().orElse(null);
            String paymentStatus = reg.getPaymentStatus().orElse(null);
            return LeagueAppsBasicDto.builder()
                    .name(name)
                    .email(email)
                    .phone(phone)
                    .username(username)
                    .keycloakId(null)
                    .statusCode(null)
                    .actionCode(null)
                    .paymentStatus(paymentStatus)
                    .build();
        }).toList();
        return jakarta.ws.rs.core.Response.ok(payload).build();
    }

    /**
     * Lightweight list of users directly from t_user.
     */
    public jakarta.ws.rs.core.Response listBasicUsersFromDb() {
        List<BasicUserRow> users = userService.findAllBasicUsers();
        List<LeagueAppsBasicDto> payload = users.stream().map(u -> {
            String name = buildName(Optional.ofNullable(u.getFirstName()), Optional.ofNullable(u.getLastName()));
            return LeagueAppsBasicDto.builder()
                    .name(name)
                    .email(u.getEmail())
                    .phone(u.getTelephone())
                    .username(u.getUsername())
                    .keycloakId(u.getKeycloakId() != null ? u.getKeycloakId().toString() : null)
                    .statusCode(u.getStatusCode())
                    .actionCode(null)
                    .paymentStatus(null)
                    .build();
        }).toList();
        return jakarta.ws.rs.core.Response.ok(payload).build();
    }

    private String buildName(Optional<String> first, Optional<String> last) {
        String f = first.orElse("").trim();
        String l = last.orElse("").trim();
        String combined = (f + " " + l).trim();
        return combined.isEmpty() ? null : combined;
    }

    public void process() {
        try {
            List<EscActionItem> actionItems = escLeagueAppsMemberActionService.getActionItems();
            if (actionItems != null && !actionItems.isEmpty()) {
                actionItems.forEach(this::processActionItem);
            } else {
                logger.info("No pending action items to process");
            }
        } catch (Exception e) {
            logger.warn("Error processing action items", e);
            // Don't throw - allow integration to continue even if processing fails
        }
    }

    public void register(RegistrationItem registrationItem) {
        selfRegisterUser(registrationItem);
    }

    private long getLastUpdated() {
        SystemPropertiesRow row = systemPropertiesService.findById(LEAGUE_APPS_RECORD_LAST_UPDATE_ID).orElseThrow(() -> new IllegalStateException("No lastUpdated record found"));
        long timestamp;
        try {
            timestamp = Long.parseLong(row.getPropertyValue());
        } catch (NumberFormatException e) {
            logger.error("Invalid timestamp value in system properties : using 0", e);
            timestamp = 0;
        }
        logger.info("fetching with timestamp : " + timestamp);
        return timestamp;
    }

    private void updateLastUpdated() {
        SystemPropertiesRow row = systemPropertiesService.findById(LEAGUE_APPS_RECORD_LAST_UPDATE_ID).orElseThrow();

        SystemPropertiesPartial partial = SystemPropertiesPartial.builder()
                .systemPropertiesId(Optional.of(LEAGUE_APPS_RECORD_LAST_UPDATE_ID))
                .propertyKey(row.getPropertyKey())
                .propertyValue(String.valueOf(DateTimeUtils.now().getMillis()))
                .build();
        systemPropertiesService.update(partial);
    }

    private void fetchUserRecords(long lastUpdated) throws Exception {
        ApiFetcher userFetcher = new ApiFetcher(new OkHttpClient(), memberHandler, new Random(), lastUpdated, 0);
        userFetcher.fetch();
    }

    private void fetchRegistrationRecords(long lastUpdated) throws Exception {
        ApiFetcher registrationFetcher = new ApiFetcher(new OkHttpClient(), registrationHandler, new Random(), lastUpdated, 0);
        registrationFetcher.fetch();
    }

    public jakarta.ws.rs.core.Response verifyCredentials() {
        Map<String, Object> result = new HashMap<>();
        OkHttpClient client = new OkHttpClient();
        
        // Check 1: PEM file exists
        boolean pemFileExists = false;
        try {
            java.io.InputStream pemStream = getClass().getResourceAsStream(LeagueAppsConstants.PEM_FILE);
            pemFileExists = pemStream != null;
            if (pemStream != null) {
                pemStream.close();
            }
        } catch (Exception e) {
            logger.warn("Error checking PEM file", e);
        }
        result.put("pemFileExists", pemFileExists);
        result.put("pemFilePath", LeagueAppsConstants.PEM_FILE);
        
        // Check 2: Try to get access token
        boolean accessTokenValid = false;
        String accessTokenError = null;
        try {
            AccessTokenFactory tokenFactory = new AccessTokenFactory(client);
            String accessToken = tokenFactory.requestAccessToken();
            if (accessToken != null && !accessToken.isEmpty()) {
                accessTokenValid = true;
                result.put("accessToken",  accessToken);
            } else {
                accessTokenError = "Failed to get access token - check PEM file and CLIENT_ID";
            }
        } catch (Exception e) {
            accessTokenError = e.getMessage();
            logger.warn("Error getting access token", e);
        }
        result.put("accessTokenValid", accessTokenValid);
        if (accessTokenError != null) {
            result.put("accessTokenError", accessTokenError);
        }
        
        // Check 3: Try to make a test API call
        boolean apiCallSuccessful = false;
        String apiCallError = null;
        if (accessTokenValid) {
            try {
                AccessTokenFactory tokenFactory = new AccessTokenFactory(client);
                String accessToken = tokenFactory.requestAccessToken();
                
                // Try to fetch members with a very old timestamp (should return empty or error, but validates credentials)
                String testUrl = LeagueAppsConstants.ADMIN_HOST + "/v2/sites/" + LeagueAppsConstants.SITE_ID + 
                               "/export/" + LeagueAppsConstants.MEMBERS2_RECORD_TYPE + 
                               "?last-updated=0&last-id=0";
                
                Request request = new Request.Builder()
                    .url(testUrl)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.code() == 200 || response.code() == 404) {
                        apiCallSuccessful = true;
                        result.put("apiResponseCode", response.code());
                    } else {
                        apiCallError = "API returned status code: " + response.code();
                        result.put("apiResponseCode", response.code());
                        if (response.body() != null) {
                            result.put("apiResponseBody", response.body().string());
                        }
                    }
                }
            } catch (Exception e) {
                apiCallError = e.getMessage();
                logger.warn("Error making test API call", e);
            }
        } else {
            apiCallError = "Skipped - access token not valid";
        }
        result.put("apiCallSuccessful", apiCallSuccessful);
        if (apiCallError != null) {
            result.put("apiCallError", apiCallError);
        }
        
        // Configuration values
        Map<String, Object> config = new HashMap<>();
        config.put("siteId", LeagueAppsConstants.SITE_ID);
        config.put("clientId", LeagueAppsConstants.CLIENT_ID);
        config.put("authUrl", LeagueAppsConstants.DEFAULT_AUTH);
        config.put("adminHost", LeagueAppsConstants.ADMIN_HOST);
        config.put("membersEndpoint", LeagueAppsConstants.MEMBERS2_RECORD_TYPE);
        config.put("registrationsEndpoint", LeagueAppsConstants.REGISTRATIONS2_RECORD_TYPE);
        result.put("configuration", config);
        
        // Overall status
        boolean allValid = pemFileExists && accessTokenValid && apiCallSuccessful;
        result.put("allCredentialsValid", allValid);
        result.put("status", allValid ? "SUCCESS" : "FAILED");
        
        jakarta.ws.rs.core.Response.ResponseBuilder responseBuilder = allValid ? 
            jakarta.ws.rs.core.Response.ok(result) : 
            jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST).entity(result);
        
        return responseBuilder.build();
    }

    public jakarta.ws.rs.core.Response getAllUsersFromLeagueApps() {
        Map<String, Object> result = new HashMap<>();
        OkHttpClient client = new OkHttpClient();
        List<Member> allMembers = new ArrayList<>();
        
        try {
            AccessTokenFactory tokenFactory = new AccessTokenFactory(client);
            String accessToken = tokenFactory.requestAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                result.put("error", "Failed to get access token from LeagueApps");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.UNAUTHORIZED).entity(result).build();
            }
            
            // Fetch all members by using last-updated=0 and last-id=0
            // This will get all members from the beginning
            long lastUpdated = 0;
            long lastId = 0;
            int batchCount = 0;
            boolean hasMore = true;
            
            while (hasMore) {
                String url = LeagueAppsConstants.ADMIN_HOST + "/v2/sites/" + LeagueAppsConstants.SITE_ID + 
                            "/export/" + LeagueAppsConstants.MEMBERS2_RECORD_TYPE + 
                            "?last-updated=" + lastUpdated + "&last-id=" + lastId;
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.code() == 401) {
                        // Token expired, get a new one
                        accessToken = tokenFactory.requestAccessToken();
                        if (accessToken == null || accessToken.isEmpty()) {
                            result.put("error", "Failed to refresh access token");
                            break;
                        }
                        continue; // Retry with new token
                    }
                    
                    if (response.code() != 200) {
                        result.put("error", "API returned status code: " + response.code());
                        if (response.body() != null) {
                            result.put("responseBody", response.body().string());
                        }
                        break;
                    }
                    
                    String responseBody = response.body().string();
                    JSONArray records = new JSONArray(responseBody);
                    
                    if (records.length() == 0) {
                        hasMore = false;
                        break;
                    }
                    
                    batchCount++;
                    logger.infof("Fetched batch %d with %d members", batchCount, records.length());
                    
                    // Parse members
                    List<Member> batchMembers = memberHandler.getObjectMapper().readValue(
                        responseBody,
                        memberHandler.getTypeFactory().constructCollectionType(List.class, Member.class)
                    );
                    
                    allMembers.addAll(batchMembers);
                    
                    // Update lastUpdated and lastId for next batch
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.getJSONObject(i);
                        long recordLastUpdated = record.getLong("lastUpdated");
                        long recordId = record.getLong("id");
                        lastUpdated = Math.max(lastUpdated, recordLastUpdated);
                        lastId = Math.max(lastId, recordId);
                    }
                    
                    // If we got fewer records than expected, we might be done
                    // But continue to be safe
                }
            }
            
            result.put("totalUsers", allMembers.size());
            result.put("batchesFetched", batchCount);
            result.put("users", allMembers);
            result.put("status", "SUCCESS");
            
            return jakarta.ws.rs.core.Response.ok(result).build();
            
        } catch (Exception e) {
            logger.error("Error fetching all users from LeagueApps", e);
            result.put("error", e.getMessage());
            result.put("status", "FAILED");
            result.put("totalUsers", allMembers.size());
            result.put("users", allMembers); // Return what we got so far
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
        }
    }

    public jakarta.ws.rs.core.Response getUserFromLeagueApps(long userId) {
        Map<String, Object> result = new HashMap<>();
        OkHttpClient client = new OkHttpClient();
        
        try {
            AccessTokenFactory tokenFactory = new AccessTokenFactory(client);
            String accessToken = tokenFactory.requestAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                result.put("error", "Failed to get access token from LeagueApps");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.UNAUTHORIZED).entity(result).build();
            }
            
            // Fetch all members and filter by userId
            // Note: LeagueApps API doesn't have a direct "get user by ID" endpoint,
            // so we fetch and filter
            long lastUpdated = 0;
            long lastId = 0;
            boolean found = false;
            Member foundMember = null;
            
            while (!found) {
                String url = LeagueAppsConstants.ADMIN_HOST + "/v2/sites/" + LeagueAppsConstants.SITE_ID + 
                            "/export/" + LeagueAppsConstants.MEMBERS2_RECORD_TYPE + 
                            "?last-updated=" + lastUpdated + "&last-id=" + lastId;
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.code() == 401) {
                        accessToken = tokenFactory.requestAccessToken();
                        if (accessToken == null || accessToken.isEmpty()) {
                            result.put("error", "Failed to refresh access token");
                            break;
                        }
                        continue;
                    }
                    
                    if (response.code() != 200) {
                        result.put("error", "API returned status code: " + response.code());
                        if (response.body() != null) {
                            result.put("responseBody", response.body().string());
                        }
                        break;
                    }
                    
                    String responseBody = response.body().string();
                    JSONArray records = new JSONArray(responseBody);
                    
                    if (records.length() == 0) {
                        break; // No more records
                    }
                    
                    // Parse members and search for the user
                    List<Member> batchMembers = memberHandler.getObjectMapper().readValue(
                        responseBody,
                        memberHandler.getTypeFactory().constructCollectionType(List.class, Member.class)
                    );
                    
                    for (Member member : batchMembers) {
                        if (member.getId() == userId) {
                            foundMember = member;
                            found = true;
                            break;
                        }
                    }
                    
                    if (found) {
                        break;
                    }
                    
                    // Update lastUpdated and lastId for next batch
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.getJSONObject(i);
                        long recordLastUpdated = record.getLong("lastUpdated");
                        long recordId = record.getLong("id");
                        lastUpdated = Math.max(lastUpdated, recordLastUpdated);
                        lastId = Math.max(lastId, recordId);
                    }
                }
            }
            
            if (foundMember != null) {
                result.put("user", foundMember);
                result.put("status", "SUCCESS");
                return jakarta.ws.rs.core.Response.ok(result).build();
            } else {
                result.put("error", "User with ID " + userId + " not found in LeagueApps");
                result.put("status", "NOT_FOUND");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.NOT_FOUND).entity(result).build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching user from LeagueApps", e);
            result.put("error", e.getMessage());
            result.put("status", "FAILED");
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
        }
    }

    public jakarta.ws.rs.core.Response getUserByUsernameFromLeagueApps(String username) {
        Map<String, Object> result = new HashMap<>();
        OkHttpClient client = new OkHttpClient();
        
        try {
            AccessTokenFactory tokenFactory = new AccessTokenFactory(client);
            String accessToken = tokenFactory.requestAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                result.put("error", "Failed to get access token from LeagueApps");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.UNAUTHORIZED).entity(result).build();
            }
            
            // Fetch all members and filter by username
            long lastUpdated = 0;
            long lastId = 0;
            boolean found = false;
            Member foundMember = null;
            
            while (!found) {
                String url = LeagueAppsConstants.ADMIN_HOST + "/v2/sites/" + LeagueAppsConstants.SITE_ID + 
                            "/export/" + LeagueAppsConstants.MEMBERS2_RECORD_TYPE + 
                            "?last-updated=" + lastUpdated + "&last-id=" + lastId;
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.code() == 401) {
                        accessToken = tokenFactory.requestAccessToken();
                        if (accessToken == null || accessToken.isEmpty()) {
                            result.put("error", "Failed to refresh access token");
                            break;
                        }
                        continue;
                    }
                    
                    if (response.code() != 200) {
                        result.put("error", "API returned status code: " + response.code());
                        if (response.body() != null) {
                            result.put("responseBody", response.body().string());
                        }
                        break;
                    }
                    
                    String responseBody = response.body().string();
                    JSONArray records = new JSONArray(responseBody);
                    
                    if (records.length() == 0) {
                        break; // No more records
                    }
                    
                    // Parse members and search for the user
                    List<Member> batchMembers = memberHandler.getObjectMapper().readValue(
                        responseBody,
                        memberHandler.getTypeFactory().constructCollectionType(List.class, Member.class)
                    );
                    
                    for (Member member : batchMembers) {
                        if (username.equals(member.getUsername())) {
                            foundMember = member;
                            found = true;
                            break;
                        }
                    }
                    
                    if (found) {
                        break;
                    }
                    
                    // Update lastUpdated and lastId for next batch
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.getJSONObject(i);
                        long recordLastUpdated = record.getLong("lastUpdated");
                        long recordId = record.getLong("id");
                        lastUpdated = Math.max(lastUpdated, recordLastUpdated);
                        lastId = Math.max(lastId, recordId);
                    }
                }
            }
            
            if (foundMember != null) {
                result.put("user", foundMember);
                result.put("status", "SUCCESS");
                return jakarta.ws.rs.core.Response.ok(result).build();
            } else {
                result.put("error", "User with username '" + username + "' not found in LeagueApps");
                result.put("status", "NOT_FOUND");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.NOT_FOUND).entity(result).build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching user by username from LeagueApps", e);
            result.put("error", e.getMessage());
            result.put("status", "FAILED");
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
        }
    }

    public jakarta.ws.rs.core.Response getPaymentStatusByUserId(long userId) {
        Map<String, Object> result = new HashMap<>();
        OkHttpClient client = new OkHttpClient();
        
        try {
            AccessTokenFactory tokenFactory = new AccessTokenFactory(client);
            String accessToken = tokenFactory.requestAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                result.put("error", "Failed to get access token from LeagueApps");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.UNAUTHORIZED).entity(result).build();
            }
            
            // Fetch all registrations and filter by userId
            long lastUpdated = 0;
            long lastId = 0;
            Registration foundRegistration = null;
            long latestLastUpdated = 0;
            boolean hasMore = true;
            
            while (hasMore) {
                String url = LeagueAppsConstants.ADMIN_HOST + "/v2/sites/" + LeagueAppsConstants.SITE_ID + 
                            "/export/" + LeagueAppsConstants.REGISTRATIONS2_RECORD_TYPE + 
                            "?last-updated=" + lastUpdated + "&last-id=" + lastId;
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.code() == 401) {
                        accessToken = tokenFactory.requestAccessToken();
                        if (accessToken == null || accessToken.isEmpty()) {
                            result.put("error", "Failed to refresh access token");
                            break;
                        }
                        continue;
                    }
                    
                    if (response.code() != 200) {
                        result.put("error", "API returned status code: " + response.code());
                        if (response.body() != null) {
                            result.put("responseBody", response.body().string());
                        }
                        break;
                    }
                    
                    String responseBody = response.body().string();
                    JSONArray records = new JSONArray(responseBody);
                    
                    if (records.length() == 0) {
                        break; // No more records
                    }
                    
                    // Parse registrations and search for the user
                    List<Registration> batchRegistrations = registrationHandler.getObjectMapper().readValue(
                        responseBody,
                        registrationHandler.getTypeFactory().constructCollectionType(List.class, Registration.class)
                    );
                    
                    // Find registration with matching userId and most recent lastUpdated
                    for (Registration registration : batchRegistrations) {
                        if (registration.getUserId() == userId) {
                            if (registration.getLastUpdated() > latestLastUpdated) {
                                foundRegistration = registration;
                                latestLastUpdated = registration.getLastUpdated();
                            }
                        }
                    }
                    
                    // Update lastUpdated and lastId for next batch
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.getJSONObject(i);
                        long recordLastUpdated = record.getLong("lastUpdated");
                        long recordId = record.getLong("id");
                        lastUpdated = Math.max(lastUpdated, recordLastUpdated);
                        lastId = Math.max(lastId, recordId);
                    }
                    
                    // Check if we have more records to fetch
                    if (records.length() == 0) {
                        hasMore = false;
                    }
                }
            }
            
            if (foundRegistration != null) {
                result.put("userId", userId);
                result.put("paymentStatus", foundRegistration.getPaymentStatus() != null ? foundRegistration.getPaymentStatus() : "UNKNOWN");
                result.put("paymentPlan", foundRegistration.getPaymentPlan());
                result.put("amountPaid", foundRegistration.getAmountPaid());
                result.put("totalAmountDue", foundRegistration.getTotalAmountDue());
                result.put("outstandingBalance", foundRegistration.getOutstandingBalance());
                result.put("lastPaymentDate", foundRegistration.getLastPaymentDate());
                result.put("registrationId", foundRegistration.getRegistrationId());
                result.put("programName", foundRegistration.getProgramName());
                result.put("registrationStatus", foundRegistration.getRegistrationStatus());
                result.put("status", "SUCCESS");
                
                return jakarta.ws.rs.core.Response.ok(result).build();
            } else {
                result.put("error", "No registrations found for user ID: " + userId);
                result.put("status", "NOT_FOUND");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.NOT_FOUND).entity(result).build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching payment status by user ID from LeagueApps", e);
            result.put("error", e.getMessage());
            result.put("status", "FAILED");
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
        }
    }

    public jakarta.ws.rs.core.Response getPaymentStatusByUsername(String username) {
        Map<String, Object> result = new HashMap<>();
        OkHttpClient client = new OkHttpClient();
        
        try {
            AccessTokenFactory tokenFactory = new AccessTokenFactory(client);
            String accessToken = tokenFactory.requestAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                result.put("error", "Failed to get access token from LeagueApps");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.UNAUTHORIZED).entity(result).build();
            }
            
            // Fetch all registrations and filter by username
            long lastUpdated = 0;
            long lastId = 0;
            Registration foundRegistration = null;
            long latestLastUpdated = 0;
            boolean hasMore = true;
            
            while (hasMore) {
                String url = LeagueAppsConstants.ADMIN_HOST + "/v2/sites/" + LeagueAppsConstants.SITE_ID + 
                            "/export/" + LeagueAppsConstants.REGISTRATIONS2_RECORD_TYPE + 
                            "?last-updated=" + lastUpdated + "&last-id=" + lastId;
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.code() == 401) {
                        accessToken = tokenFactory.requestAccessToken();
                        if (accessToken == null || accessToken.isEmpty()) {
                            result.put("error", "Failed to refresh access token");
                            break;
                        }
                        continue;
                    }
                    
                    if (response.code() != 200) {
                        result.put("error", "API returned status code: " + response.code());
                        if (response.body() != null) {
                            result.put("responseBody", response.body().string());
                        }
                        break;
                    }
                    
                    String responseBody = response.body().string();
                    JSONArray records = new JSONArray(responseBody);
                    
                    if (records.length() == 0) {
                        break; // No more records
                    }
                    
                    // Parse registrations and search for the user
                    List<Registration> batchRegistrations = registrationHandler.getObjectMapper().readValue(
                        responseBody,
                        registrationHandler.getTypeFactory().constructCollectionType(List.class, Registration.class)
                    );
                    
                    // Find registration with matching username and most recent lastUpdated
                    for (Registration registration : batchRegistrations) {
                        if (username.equals(registration.getUserName())) {
                            if (registration.getLastUpdated() > latestLastUpdated) {
                                foundRegistration = registration;
                                latestLastUpdated = registration.getLastUpdated();
                            }
                        }
                    }
                    
                    // Update lastUpdated and lastId for next batch
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject record = records.getJSONObject(i);
                        long recordLastUpdated = record.getLong("lastUpdated");
                        long recordId = record.getLong("id");
                        lastUpdated = Math.max(lastUpdated, recordLastUpdated);
                        lastId = Math.max(lastId, recordId);
                    }
                    
                    // Check if we have more records to fetch
                    if (records.length() == 0) {
                        hasMore = false;
                    }
                }
            }
            
            if (foundRegistration != null) {
                result.put("username", username);
                result.put("userId", foundRegistration.getUserId());
                result.put("paymentStatus", foundRegistration.getPaymentStatus() != null ? foundRegistration.getPaymentStatus() : "UNKNOWN");
                result.put("paymentPlan", foundRegistration.getPaymentPlan());
                result.put("amountPaid", foundRegistration.getAmountPaid());
                result.put("totalAmountDue", foundRegistration.getTotalAmountDue());
                result.put("outstandingBalance", foundRegistration.getOutstandingBalance());
                result.put("lastPaymentDate", foundRegistration.getLastPaymentDate());
                result.put("registrationId", foundRegistration.getRegistrationId());
                result.put("programName", foundRegistration.getProgramName());
                result.put("registrationStatus", foundRegistration.getRegistrationStatus());
                result.put("status", "SUCCESS");
                
                return jakarta.ws.rs.core.Response.ok(result).build();
            } else {
                result.put("error", "No registrations found for username: " + username);
                result.put("status", "NOT_FOUND");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.NOT_FOUND).entity(result).build();
            }
            
        } catch (Exception e) {
            logger.error("Error fetching payment status by username from LeagueApps", e);
            result.put("error", e.getMessage());
            result.put("status", "FAILED");
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
        }
    }

    /**
     * Get member data by user ID using direct League Apps API endpoint
     * Uses: /v2/sites/[SITE_ID]/members/[USER_ID]
     */
    public jakarta.ws.rs.core.Response getMemberByUserId(long userId) {
        Map<String, Object> result = new HashMap<>();
        OkHttpClient client = new OkHttpClient();
        
        try {
            AccessTokenFactory tokenFactory = new AccessTokenFactory(client);
            String accessToken = tokenFactory.requestAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                result.put("error", "Failed to get access token from LeagueApps");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.UNAUTHORIZED).entity(result).build();
            }
            
            // Use direct API endpoint: /v2/sites/[SITE_ID]/members/[USER_ID]
            String url = LeagueAppsConstants.ADMIN_HOST + "/v2/sites/" + LeagueAppsConstants.SITE_ID + 
                        "/members/" + userId;
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 401) {
                    // Token expired, get a new one and retry
                    accessToken = tokenFactory.requestAccessToken();
                    if (accessToken == null || accessToken.isEmpty()) {
                        result.put("error", "Failed to refresh access token");
                        return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.UNAUTHORIZED).entity(result).build();
                    }
                    
                    // Retry with new token
                    request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .build();
                    
                    try (Response retryResponse = client.newCall(request).execute()) {
                        return processMemberResponse(retryResponse, result);
                    }
                }
                
                return processMemberResponse(response, result);
            }
            
        } catch (Exception e) {
            logger.error("Error fetching member by user ID from LeagueApps", e);
            result.put("error", e.getMessage());
            result.put("status", "FAILED");
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
        }
    }
    
    private jakarta.ws.rs.core.Response processMemberResponse(Response response, Map<String, Object> result) {
        try {
            if (response.code() == 404) {
                result.put("error", "Member not found in LeagueApps");
                result.put("status", "NOT_FOUND");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.NOT_FOUND).entity(result).build();
            }
            
            if (response.code() != 200) {
                result.put("error", "API returned status code: " + response.code());
                if (response.body() != null) {
                    result.put("responseBody", response.body().string());
                }
                result.put("status", "FAILED");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
            }
            
            String responseBody = response.body().string();
            
            // Parse the JSON response to include all fields
            JSONObject memberJson = new JSONObject(responseBody);
            
            // Convert JSONObject to Map to preserve all fields including custom ones
            Map<String, Object> memberData = jsonObjectToMap(memberJson);
            
            result.put("status", "SUCCESS");
            result.putAll(memberData);
            
            return jakarta.ws.rs.core.Response.ok(result).build();
            
        } catch (Exception e) {
            logger.error("Error processing member response", e);
            result.put("error", e.getMessage());
            result.put("status", "FAILED");
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
        }
    }
    
    private Map<String, Object> jsonObjectToMap(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.put(key, jsonObjectToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.put(key, jsonArrayToList((JSONArray) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }
    
    private List<Object> jsonArrayToList(JSONArray jsonArray) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                list.add(jsonObjectToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                list.add(jsonArrayToList((JSONArray) value));
            } else {
                list.add(value);
            }
        }
        return list;
    }

}
