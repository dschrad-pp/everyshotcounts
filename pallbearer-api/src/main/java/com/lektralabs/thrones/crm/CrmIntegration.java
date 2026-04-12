package com.lektralabs.thrones.crm;

import com.lektralabs.thrones.crm.model.CrmRegistration;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SystemPropertiesPartial;
import com.lektralabs.thrones.pallbearer.common.UserPropertyConstants;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.TeamBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.CrmRegistrationRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.SystemPropertiesRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TeamRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.CrmRegistrationService;
import com.lektralabs.thrones.pallbearer.jdbi.service.SystemPropertiesService;
import com.lektralabs.thrones.pallbearer.jdbi.service.TeamService;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserPropertyService;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@ApplicationScoped
public class CrmIntegration implements UserPropertyConstants {
    private static final Logger logger = Logger.getLogger(CrmIntegration.class);

    private final Random random = new Random();

    @Inject
    CrmApiClient crmApiClient;

    @Inject
    CrmRegistrationService crmRegistrationService;

    @Inject
    SystemPropertiesService systemPropertiesService;

    @Inject
    UserService userService;

    @Inject
    UserPropertyService userPropertyService;

    @Inject
    TeamService teamService;

    @Inject
    JdbiProvider jdbiProvider;

    // Use a UUID for the system property ID - generate once and store
    private static final java.util.UUID CRM_RECORD_LAST_UPDATE_ID =
        java.util.UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String CRM_RECORD_LAST_UPDATE_KEY = "crm.records.last_updated";

    // The default team that all users fall back to — used as sport/org template when auto-creating CRM teams
    private static final UUID DEFAULT_TEAM_ID = UUID.fromString("2a3b4697-26ee-4294-812e-6e1b00bd8e90");

    /**
     * Sync registrations from CRM API
     */
    public jakarta.ws.rs.core.Response syncRegistrations(Optional<Long> lastUpdatedTimestamp) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Get last sync timestamp
            long lastSyncTimestamp;
            boolean usingProvidedTimestamp = false;
            boolean processAllRegistrations = false;

            if (lastUpdatedTimestamp.isPresent() && lastUpdatedTimestamp.get() > 0) {
                lastSyncTimestamp = lastUpdatedTimestamp.get();
                usingProvidedTimestamp = true;
                logger.info("Using provided lastUpdated timestamp: " + lastSyncTimestamp);
            } else {
                // When lastUpdated is not provided, process ALL registrations
                lastSyncTimestamp = 0;
                processAllRegistrations = true;
                logger.info("No lastUpdated parameter provided - processing ALL registrations");
            }

            // Fetch registrations from CRM API
            List<CrmRegistration> registrations = crmApiClient.fetchRegistrations();
            logger.info("Fetched " + registrations.size() + " registrations from CRM API");

            // Store ALL registrations (not filtered by timestamp)
            // The timestamp filter is only used for tracking what's new, but we store everything
            int processed = 0;
            int created = 0;
            int updated = 0;
            long maxLastUpdated = lastSyncTimestamp;

            for (CrmRegistration registration : registrations) {
                CrmRegistrationRow row = convertToRow(registration);
                CrmRegistrationRow existing = crmRegistrationService.findByRegistrationId(registration.getRegistrationId()).orElse(null);
                
                crmRegistrationService.createOrUpdate(row);
                processed++;

                if (existing == null) {
                    created++;
                } else {
                    updated++;
                }

                // Track max lastUpdated for next sync (only for new/updated records)
                if (registration.getLastUpdated() != null && registration.getLastUpdated() > maxLastUpdated) {
                    maxLastUpdated = registration.getLastUpdated();
                }
            }

            // Update last sync timestamp (only if not using provided timestamp)
            if (!usingProvidedTimestamp) {
                updateLastUpdated(maxLastUpdated);
            }

            // Process registrations to create/update users
            int usersProcessed = 0;
            int usersCreated = 0;
            int usersUpdated = 0;

            for (CrmRegistration registration : registrations) {
                // If processAllRegistrations is true, process all registrations
                // Otherwise, only process registrations newer than lastSyncTimestamp
                boolean shouldProcess = processAllRegistrations || 
                    (registration.getLastUpdated() != null && registration.getLastUpdated() > lastSyncTimestamp);
                
                if (shouldProcess) {
                    try {
                        int[] processResult = processRegistration(registration);
                        usersProcessed += processResult[0];
                        usersCreated += processResult[1];
                        usersUpdated += processResult[2];
                    } catch (Exception e) {
                        logger.warn("Error processing registration: " + registration.getRegistrationId(), e);
                    }
                }
            }

            // Build result
            long duration = System.currentTimeMillis() - startTime;
            result.put("status", "SUCCESS");
            result.put("registrationsFetched", registrations.size());
            result.put("registrationsProcessed", processed);
            result.put("registrationsCreated", created);
            result.put("registrationsUpdated", updated);
            result.put("usersProcessed", usersProcessed);
            result.put("usersCreated", usersCreated);
            result.put("usersUpdated", usersUpdated);
            result.put("lastSyncTimestamp", lastSyncTimestamp);
            result.put("newSyncTimestamp", maxLastUpdated);
            result.put("durationMs", duration);

            logger.info("CRM sync completed successfully in " + duration + "ms");
            return jakarta.ws.rs.core.Response.ok(result).build();

        } catch (Exception e) {
            logger.error("Error in CRM sync", e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(result).build();
        }
    }

    /**
     * Convert CRM Registration to CrmRegistrationRow
     */
    private CrmRegistrationRow convertToRow(CrmRegistration registration) {
        return CrmRegistrationRow.builder()
                .registrationId(registration.getRegistrationId())
                .userId(Optional.ofNullable(registration.getUserId()))
                .username(Optional.ofNullable(registration.getUsername()))
                .email(Optional.ofNullable(registration.getEmail()))
                .firstName(Optional.ofNullable(registration.getFirstName()))
                .lastName(Optional.ofNullable(registration.getLastName()))
                .phoneNumber(Optional.ofNullable(registration.getPhoneNumber()))
                .role(Optional.ofNullable(registration.getRole()))
                .teamId(Optional.ofNullable(registration.getTeamId()))
                .paymentStatus(Optional.ofNullable(registration.getPaymentStatus()))
                .subscriptionStartDate(Optional.ofNullable(registration.getSubscriptionStartDate()))
                .subscriptionEndDate(Optional.ofNullable(registration.getSubscriptionEndDate()))
                .lastUpdated(registration.getLastUpdated() != null ? registration.getLastUpdated() : System.currentTimeMillis())
                .build();
    }

    /**
     * Handle webhook registration from CRM system
     * This method processes a single registration received via webhook
     * It stores the registration and processes it to create/update user
     */
    public jakarta.ws.rs.core.Response handleWebhookRegistration(CrmRegistration registration) {
        Map<String, Object> result = new HashMap<>();
        
        // Log the received registration data in a structured format
        logger.info("Processing webhook registration:");
        logger.info("  Registration ID: " + registration.getRegistrationId());
        logger.info("  Email: " + registration.getEmail());
        logger.info("  User ID: " + registration.getUserId());
        logger.info("  Username: " + registration.getUsername());
        logger.info("  First Name: " + registration.getFirstName());
        logger.info("  Last Name: " + registration.getLastName());
        logger.info("  Phone Number: " + registration.getPhoneNumber());
        logger.info("  Role: " + registration.getRole());
        logger.info("  Team ID: " + registration.getTeamId());
        logger.info("  Payment Status: " + registration.getPaymentStatus());
        logger.info("  Subscription Start Date: " + registration.getSubscriptionStartDate());
        logger.info("  Subscription End Date: " + registration.getSubscriptionEndDate());
        logger.info("  Last Updated: " + registration.getLastUpdated());
        
        try {
            // Validate required fields
            if (registration.getRegistrationId() == null) {
                result.put("status", "FAILED");
                result.put("error", "registrationId is required");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                        .entity(result).build();
            }
            
            if (registration.getEmail() == null || registration.getEmail().isEmpty()) {
                result.put("status", "FAILED");
                result.put("error", "email is required");
                return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.BAD_REQUEST)
                        .entity(result).build();
            }
            
            // Set lastUpdated if not provided
            if (registration.getLastUpdated() == null) {
                registration.setLastUpdated(System.currentTimeMillis());
            }
            
            // Store registration in database
            CrmRegistrationRow row = convertToRow(registration);
            CrmRegistrationRow existing = crmRegistrationService.findByRegistrationId(registration.getRegistrationId()).orElse(null);
            crmRegistrationService.createOrUpdate(row);
            
            boolean wasCreated = existing == null;
            
            // Process registration to create/update user
            int[] processResult = processRegistration(registration);
            int usersProcessed = processResult[0];
            int usersCreated = processResult[1];
            int usersUpdated = processResult[2];
            
            // Build result
            result.put("status", "SUCCESS");
            result.put("registrationId", registration.getRegistrationId());
            result.put("registrationCreated", wasCreated);
            result.put("registrationUpdated", !wasCreated);
            result.put("usersProcessed", usersProcessed);
            result.put("usersCreated", usersCreated);
            result.put("usersUpdated", usersUpdated);
            result.put("message", wasCreated ? "Registration created and processed" : "Registration updated and processed");
            
            logger.info("Webhook registration processed successfully: " + registration.getRegistrationId());
            return jakarta.ws.rs.core.Response.ok(result).build();
            
        } catch (Exception e) {
            logger.error("Error processing webhook registration: " + registration.getRegistrationId(), e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(result).build();
        }
    }

    /**
     * Process a CRM registration to create or update user
     * Returns array: [processed, created, updated]
     * Follows the exact same flow as LeagueApps: creates user in DB only (no Keycloak), generates 6-digit code, sends email
     */
    private int[] processRegistration(CrmRegistration registration) {
        // Check payment status - only process if PAID or has payment
        boolean hasPayment = registration.getPaymentStatus() != null && 
                            (registration.getPaymentStatus().equalsIgnoreCase("PAID") || 
                             registration.getPaymentStatus().equalsIgnoreCase("PARTIAL"));

        // Only process users with payment (same as LeagueApps)
        if (!hasPayment) {
            return new int[]{0, 0, 0}; // No action taken for unpaid registrations
        }

        // Prepare user property map
        Map<String, String> userPropertyMap = new HashMap<>();

        // Generate six digit code for registration
        String sixDigitCode = createRegistrationKey();
        userPropertyMap.put(USER_REGISTRATION_SIX_DIGIT_CODE_KEY, sixDigitCode);

        // Normalize payment status and add to properties if present
        if (registration.getPaymentStatus() != null) {
            userPropertyMap.put(USER_REGISTRATION_PAYMENT_STATE_KEY, normalizePaymentStatus(registration.getPaymentStatus()));
        }

        // Check if a user with the given email already exists
        Optional<UserRow> maybeExistingUser = userService.findByEmail(registration.getEmail());

        if (maybeExistingUser.isPresent()) {
            UserRow existingUser = maybeExistingUser.get();
            // Always update their payment status property
            userPropertyService.addProperties(existingUser.getId(), userPropertyMap);

            // If user has a Keycloak ID, consider them fully registered and skip new registration
            if (existingUser.getKeycloakId() != null) {
                logger.info("User already registered with Keycloak: " + registration.getEmail());
                return new int[]{1, 0, 1}; // processed=1, created=0, updated=1
            }

            // User exists but not activated in Keycloak - just update properties, don't send email again
            // (User should use existing 6-digit code or request a new one via activation endpoint)
            logger.info("User exists in DB but not activated in Keycloak: " + registration.getEmail());
            return new int[]{1, 0, 1}; // processed=1, created=0, updated=1
        } else {
            // Build the new user registration object
            // Username is stored in lowercase for consistency
            String username = registration.getUsername() != null ? registration.getUsername().toLowerCase() : null;
            RegisterUserPartial registerUserPartial = RegisterUserPartial.builder()
                    .username(username)
                    .password(UUID.randomUUID().toString()) // Random password - user will set during activation
                    .email(registration.getEmail())
                    .firstName(registration.getFirstName() != null ? registration.getFirstName() : "")
                    .lastName(registration.getLastName() != null ? registration.getLastName() : "")
                    .phoneNumber(registration.getPhoneNumber() != null ? registration.getPhoneNumber() : "")
                    .birthDate(0L) // Default to 0L if not provided (same as LeagueApps)
                    .role(registration.getRole() != null ? registration.getRole() : "ATHLETE")
                    .build();

            // Register the new user (without Keycloak - will be activated later)
            UserRow userRow = userService.registerUser(registerUserPartial, false);

            // Link user to their CRM team
            UUID crmTeamId = registration.getTeamId();
            if (crmTeamId != null) {
                ensureTeamExists(crmTeamId, registration.getTeamName());
                try {
                    teamService.mapUserToTeam(userRow.getId(), crmTeamId);
                    logger.infof("Linked user %s to CRM team %s", userRow.getId(), crmTeamId);
                } catch (Exception e) {
                    logger.warnf("Failed to link user %s to team %s: %s", userRow.getId(), crmTeamId, e.getMessage());
                }
            }

            // Save user properties for the new user
            userPropertyService.addProperties(userRow.getId(), userPropertyMap);

            // Send new registration email to the user
            try {
                userService.newRegistrationEmail(
                        registration.getEmail(),
                        sixDigitCode
                );
                logger.info("Sent registration email with 6-digit code to: " + registration.getEmail());
            } catch (Exception e) {
                logger.error("Could not send email to user [%s]".formatted(registration.getEmail()), e);
            }

            logger.info("Created new user from CRM registration: " + registration.getEmail());
            return new int[]{1, 1, 0}; // processed=1, created=1, updated=0
        }
    }

    /**
     * Ensure a team exists in t_team for the given CRM teamId.
     * If absent, auto-creates it using the sport/org from the default fallback team.
     */
    private void ensureTeamExists(UUID crmTeamId, String teamName) {
        if (teamService.findById(crmTeamId).isPresent()) return;
        TeamRow fallback = teamService.findById(DEFAULT_TEAM_ID)
                .orElseThrow(() -> new IllegalStateException("Default team not found: " + DEFAULT_TEAM_ID));
        TeamRow newTeam = TeamRow.builder()
                .id(crmTeamId)
                .sportId(fallback.getSportId())
                .organizationId(fallback.getOrganizationId())
                .name(Optional.of(teamName != null && !teamName.isBlank() ? teamName : "CRM Team " + crmTeamId))
                .description(Optional.of("Auto-created from CRM registration"))
                .build();
        jdbiProvider.getJdbi().onDemand(TeamBaseDao.class).insert(newTeam);
        logger.infof("Auto-created team %s (%s) from CRM data", crmTeamId, newTeam.getName());
    }

    /**
     * Normalize payment status from CRM to internal format
     */
    private String normalizePaymentStatus(String paymentStatus) {
        if (paymentStatus == null) {
            return "";
        }
        switch (paymentStatus.toUpperCase()) {
            case "PAID":
                return USER_REGISTRATION_PAYMENT_STATE_PAID;
            case "TRIAL":
            case "TRAIL":
                return USER_REGISTRATION_PAYMENT_STATE_TRIAL;
            default:
                return paymentStatus.toUpperCase();
        }
    }

    /**
     * Generate a 6-digit registration code
     */
    private String createRegistrationKey() {
        int number = random.nextInt(999999);
        return String.format("%06d", number);
    }

    private long getLastUpdated() {
        SystemPropertiesRow row = systemPropertiesService.findById(CRM_RECORD_LAST_UPDATE_ID)
                .orElseGet(() -> {
                    // Create default if not exists
                    SystemPropertiesPartial partial = SystemPropertiesPartial.builder()
                            .systemPropertiesId(Optional.of(CRM_RECORD_LAST_UPDATE_ID))
                            .propertyKey(CRM_RECORD_LAST_UPDATE_KEY)
                            .propertyValue("0")
                            .build();
                    systemPropertiesService.create(partial);
                    return systemPropertiesService.findById(CRM_RECORD_LAST_UPDATE_ID).orElseThrow();
                });

        try {
            return Long.parseLong(row.getPropertyValue());
        } catch (NumberFormatException e) {
            logger.error("Invalid timestamp value in system properties: using 0", e);
            return 0;
        }
    }

    private void updateLastUpdated(long timestamp) {
        SystemPropertiesRow row = systemPropertiesService.findById(CRM_RECORD_LAST_UPDATE_ID)
                .orElseThrow(() -> new IllegalStateException("No lastUpdated record found"));

        SystemPropertiesPartial partial = SystemPropertiesPartial.builder()
                .systemPropertiesId(Optional.of(CRM_RECORD_LAST_UPDATE_ID))
                .propertyKey(row.getPropertyKey())
                .propertyValue(String.valueOf(timestamp))
                .build();
        systemPropertiesService.update(partial);
    }
}
