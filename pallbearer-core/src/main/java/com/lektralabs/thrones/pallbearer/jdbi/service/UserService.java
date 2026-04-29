package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.keycloak.FindUserResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;
import com.lektralabs.thrones.pallbearer.common.CoreConstants;
import com.lektralabs.thrones.pallbearer.common.UserPropertyConstants;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.CurrentUserDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.RoleDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.UserDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.UserPropertyDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.generated.ContactBaseDao;
import com.lektralabs.thrones.pallbearer.jdbi.exception.RegistrationException;
import com.lektralabs.thrones.pallbearer.jdbi.factory.ContactFactory;
import com.lektralabs.thrones.pallbearer.jdbi.factory.UserFactory;
import com.lektralabs.thrones.pallbearer.jdbi.model.RoleRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserPropertyRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ContactRow;
import com.lektralabs.thrones.pallbearer.mailer.MailClient;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import com.lektralabs.thrones.pallbearer.util.PropertyKeyUtils;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import java.util.*;
import java.util.stream.Collectors;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;
import com.lektralabs.thrones.pallbearer.jdbi.dao.UserGroupPropertyDao;
import com.lektralabs.thrones.pallbearer.jdbi.dao.UserOTPDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserGroupPropertyRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.UserOTPRow;


@ApplicationScoped
public class UserService implements CoreConstants, UserPropertyConstants {

    private static final Logger logger = Logger.getLogger(UserService.class);

    @Inject
    KeycloakProvider keycloakProvider;

    @Inject
    SecurityIdentity keycloakSecurityContext;

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    MailClient mailClient;

    @Inject
    DrillItemService drillItemService;

    @Inject
    DrillService drillService;

    @Inject
    DrillGroupService drillGroupService;

    @Inject
    TeamService teamService;

    private CurrentUserDao currentUserDao;
    private ContactBaseDao contactBaseDao;
    private RoleDao roleDao;
    private UserDao userDao;
    private UserPropertyDao userPropertyDao;
    private UserGroupPropertyDao userGroupPropertyDao;
    private UserOTPDao userOTPDao;

    @PostConstruct
    public void init() {
        this.currentUserDao = jdbiProvider.getJdbi().onDemand(CurrentUserDao.class);
        this.contactBaseDao = jdbiProvider.getJdbi().onDemand(ContactBaseDao.class);
        this.roleDao = jdbiProvider.getJdbi().onDemand(RoleDao.class);
        this.userDao = jdbiProvider.getJdbi().onDemand(UserDao.class);
        this.userPropertyDao = jdbiProvider.getJdbi().onDemand(UserPropertyDao.class);
        this.userOTPDao = jdbiProvider.getJdbi().onDemand(UserOTPDao.class);
        this.userGroupPropertyDao = jdbiProvider.getJdbi().onDemand(UserGroupPropertyDao.class);
    }

    public CurrentUser getCurrentUser() {
        try {
            // Check if there's an authenticated user context
            if (keycloakSecurityContext == null || keycloakSecurityContext.getPrincipal() == null) {
                // No authenticated user (e.g., scheduled task context) - return system user
                return getSystemUser();
            }
            String username = keycloakSecurityContext.getPrincipal().getName();
            if (username == null || username.trim().isEmpty()) {
                // Empty username - return system user
                return getSystemUser();
            }
            return getCurrentUser(username);
        } catch (Exception e) {
            // If there's any error getting the user context (e.g., no authentication), return system user
            logger.debug("No authenticated user context available, using system user: " + e.getMessage());
            return getSystemUser();
        }
    }

    /**
     * Returns a system user for audit purposes when there's no authenticated user context
     * (e.g., scheduled tasks, background jobs)
     */
    private CurrentUser getSystemUser() {
        return CurrentUser.builder()
                .id(CoreConstants.SYSTEM_USER_ID)
                .username("system")
                .email("system@lektralabs.com")
                .firstName("System")
                .lastName("User")
                .metadata(new HashMap<>())
                .build();
    }

    // @TODO - kbrumer - enable cache after registration is working
    // @CacheResult(cacheName = "current-user-by-username-cache")
    public CurrentUser getCurrentUser(String username) {
        if (username == null) {
            throw new RuntimeException("Username cannot be null");
        }
        // SQL query uses case-insensitive comparison (lower()), so we can pass username as-is
        // But we lowercase it for consistency with our storage policy
        String lowercasedUsername = username.toLowerCase();
        CurrentUser currentUser = currentUserDao.findByName(lowercasedUsername);
        
        if (currentUser == null) {
            throw new RuntimeException("User not found for username: " + username);
        }
        
        UUID userId = currentUser.getId();

        // Load user properties
        var userProps = userPropertyDao.findByUserId(userId);
        Map<String, String> userProperties = userProps.stream()
                .collect(Collectors.toMap(UserPropertyRow::getPropertyKey, UserPropertyRow::getPropertyValue));

        // Load group properties
        var groupProps = userGroupPropertyDao.findByUserId(userId);
        Map<String, Map<String, String>> groupProperties = new HashMap<>();
        for (UserGroupPropertyRow row : groupProps) {
            UUID groupId = row.getDrillGroupId(); // Do NOT convert to string
            String groupName = DrillGroupConstants.drillGroupIdNameMap.getOrDefault(groupId, groupId.toString());
            groupProperties
                    .computeIfAbsent(groupName, k -> new HashMap<>())
                    .put(row.getPropertyKey(), row.getPropertyValue());
        }

        // Convert dot-separated keys to camelCase
        Map<String, String> camelCaseUserProperties = PropertyKeyUtils.convertKeysToCamelCase(userProperties);

        // Quick fix: ensure userDrillGroupName reflects current canonical name mapping
        String drillGroupIdStr = camelCaseUserProperties.get("userDrillGroup");
        if (drillGroupIdStr != null) {
            try {
                UUID drillGroupId = UUID.fromString(drillGroupIdStr);
                String canonicalGroupName = DrillGroupConstants.drillGroupIdNameMap.get(drillGroupId);
                if (canonicalGroupName != null) {
                    camelCaseUserProperties.put("userDrillGroupName", canonicalGroupName);
                }
            } catch (IllegalArgumentException ignored) {
                // leave existing value if not a UUID
            }
        }
        Map<String, Map<String, String>> camelCaseGroupProperties = PropertyKeyUtils
                .convertNestedKeysToCamelCase(groupProperties);

        currentUser.setUserProperties(camelCaseUserProperties);
        currentUser.setGroupProperties(camelCaseGroupProperties);

        // Set flattened group properties for easier access
        currentUser.setBeginnerGroupProperties(camelCaseGroupProperties.getOrDefault("Beginner", new HashMap<>()));
        currentUser
                .setIntermediateGroupProperties(camelCaseGroupProperties.getOrDefault("Intermediate", new HashMap<>()));
        currentUser.setAdvanceGroupProperties(camelCaseGroupProperties.getOrDefault("Advanced", new HashMap<>()));
        currentUser.setEliteGroupProperties(camelCaseGroupProperties.getOrDefault("Elite", new HashMap<>()));

        return currentUser;
    }

    public UserRow registerUser(RegisterUserPartial registerUserPartial, boolean includeKeycloak) {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                UserRow userRow = createUser(registerUserPartial);
                RoleRow roleRow = getRole(registerUserPartial);
                associateUserToRole(userRow, roleRow);
                if (includeKeycloak) {
                    registerUserWithKeycloak(registerUserPartial, userRow);
                }
                setRegistrationStep(userRow);
                return userRow;
            } catch (Exception e) {
                logger.warn("Error registering user", e);
                throw new TransactionException(e);
            }
        });
    }

    public UserRow activateUser(RegisterUserPartial registerUserPartial) {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                Optional<UserRow> userRow = findByEmail(registerUserPartial.getEmail());
                if (userRow.isEmpty()) {
                    throw new RegistrationException(RegistrationException.EMAIL_MATCH_FAILURE);
                }
                RoleRow roleRow = getRole(registerUserPartial);
                associateUserToRole(userRow.get(), roleRow);
                // Activate the user
                activateUser(registerUserPartial, userRow.get());
                // Return the activated user
                return userRow.get(); // This was missing in your original code
            } catch (Exception e) {
                logger.warn("Error activating user", e);
                throw e;
            }
        });
    }

    public void activateUserByCode(RegisterUserPartial registerUserPartial, String registrationCode) {
        try {
            Optional<UserRow> userRow = findByRegistrationCode(registrationCode);
            if (userRow.isEmpty()) {
                throw new RegistrationException(
                        RegistrationException.INCORRECT_REGISTRATION_CODE);
            } else {
                activateUser(registerUserPartial, userRow.get());
            }
        } catch (Exception e) {
            logger.warn("Error activating user", e);
            throw e;
        }
    }

    public void forgotPasswordEmail(String email) throws Exception {
        findByEmail(email).map(user -> {
            String temporaryPassword = RandomStringUtils.randomAlphabetic(8);
            keycloakProvider.changeUserPassword(user.getKeycloakId(), temporaryPassword);
            mailClient.sendForgotPassword(user.getEmail(), temporaryPassword);
            return "OK";
        }).orElseThrow(() -> new RuntimeException("No user found by that email [%s]".formatted(email)));
    }

    public void forgotUsernameEmail(String email) throws Exception {
        findByEmail(email).map(user -> {
            mailClient.sendForgotUsername(user.getEmail(), user.getUsername());
            return "OK";
        }).orElseThrow(() -> new RuntimeException("No user found by that email [%s]".formatted(email)));
    }

    public void newRegistrationEmail(String email, String sixDigitCode) throws Exception {
        findByEmail(email).map(user -> {
            mailClient.sendNewRegistration(user.getEmail(), sixDigitCode);
            return "OK";
        }).orElseThrow(() -> new RuntimeException("No user found by that email [%s]".formatted(email)));
    }

    public void changeUsername(UUID userId, String newUsername) throws Exception {
        if (newUsername == null || newUsername.length() < 3) {
            throw new RuntimeException("Username must not be null and"
                    + " at least 3 characters long");
        }
        // Username is stored in lowercase for consistency
        String lowercasedUsername = newUsername.toLowerCase();
        CurrentUser cu = getCurrentUser();
        if (cu.getId().equals(userId)) {
            Optional<UserRow> maybeUserRow = userDao.findByUsername(lowercasedUsername);
            if (maybeUserRow.isEmpty()) {
                changeUsername(cu, lowercasedUsername);
            } else {
                throw new RuntimeException("Request to change username to"
                        + " a username already taken by another user");
            }
        } else {
            throw new RuntimeException("Someone requested to change"
                    + " the username for user ID=" + userId + ", but"
                    + " user ID=" + userId + " is not current user");
        }
    }

    private void changeUsername(CurrentUser cu, String newUsername) {
        Optional<UserRow> maybeUserRow = userDao.findById(cu.getId());
        if (maybeUserRow.isPresent()) {
            UserRow userRow = maybeUserRow.get();
            userRow.setUsername(newUsername);
            userDao.update(userRow);
            keycloakProvider.changeUsername(cu.getKeycloakId(), newUsername);
        }
    }

    public void changePassword(UUID userId, String newPassword) throws Exception {
        if (newPassword == null || newPassword.length() < 7) {
            throw new RuntimeException("Password must not be null and"
                    + " at least 7 characters long");
        }
        CurrentUser cu = getCurrentUser();
        if (cu.getId().equals(userId)) {
            changePassword(cu, newPassword);
        } else {
            throw new RuntimeException("Someone requested to change"
                    + " the password for user ID=" + userId + ", but"
                    + " user ID=" + userId + " is not current user");
        }
    }

    private void changePassword(CurrentUser cu, String newPassword) {
        keycloakProvider.changeUserPassword(cu.getKeycloakId(), newPassword);
    }

    private void activateUser(RegisterUserPartial registerUserPartial, UserRow userRow) {
        logger.infof("Checking activation eligibility for user ID: %s, Email: %s, Username: %s", 
            userRow.getId(), registerUserPartial.getEmail(), registerUserPartial.getUsername());
        
        Optional<UserPropertyRow> registrationState = userPropertyDao.findByKey(
                userRow.getId(), UserPropertyConstants.USER_REGISTRATION_STATE_KEY);
        
        // Check for email collision - email must belong to the current user or not exist for any other user
        Optional<UserRow> maybeExistingUserByEmail = findByEmail(registerUserPartial.getEmail());
        if (maybeExistingUserByEmail.isPresent()) {
            UserRow existingUserByEmail = maybeExistingUserByEmail.get();
            // Only treat as collision if the email belongs to a different user
            if (!existingUserByEmail.getId().equals(userRow.getId())) {
                logger.warnf("Email collision detected. Email '%s' already exists for a different user ID: %s (Username: %s)", 
                    registerUserPartial.getEmail(), existingUserByEmail.getId(), existingUserByEmail.getUsername());
                logger.warnf("Current user attempting activation: ID: %s, Username: %s", 
                    userRow.getId(), registerUserPartial.getUsername());
                logger.warnf("TROUBLESHOOTING: Email '%s' is already taken by another user", registerUserPartial.getEmail());
                logger.warnf("STEP 1: Use a different email address");
                logger.warnf("STEP 2: Or contact admin to resolve the email conflict");
                throw new RegistrationException(RegistrationException.EMAIL_COLLISION);
            } else {
                // Same user - email already set, this is fine
                logger.infof("Email '%s' already belongs to this user (ID: %s). No collision.", 
                    registerUserPartial.getEmail(), userRow.getId());
            }
        }
        
        // Check for username collision - ONLY throw validation error if username is associated with a DIFFERENT user
        if (registerUserPartial.getUsername() != null && !registerUserPartial.getUsername().trim().isEmpty()) {
            Optional<UserRow> maybeExistingUser = findByUsername(registerUserPartial.getUsername());
            if (maybeExistingUser.isPresent()) {
                UserRow existingUser = maybeExistingUser.get();
                // CRITICAL: Only throw validation error if the username belongs to a DIFFERENT user
                // If it belongs to the same user, allow it (user might be updating their own username)
                if (!existingUser.getId().equals(userRow.getId())) {
                    logger.warnf("Username collision detected. Username '%s' is already associated with a different user ID: %s (Email: %s)", 
                        registerUserPartial.getUsername(), existingUser.getId(), existingUser.getEmail());
                    logger.warnf("Current user attempting activation: ID: %s, Email: %s", 
                        userRow.getId(), registerUserPartial.getEmail());
                    logger.warnf("TROUBLESHOOTING: Username '%s' is already in use by another user", registerUserPartial.getUsername());
                    logger.warnf("STEP 1: Choose a different username");
                    logger.warnf("STEP 2: Or contact admin to resolve the username conflict");
                    throw new RegistrationException(RegistrationException.USERNAME_COLLISION);
                } else {
                    // Same user - username already belongs to this user, this is fine (no validation error)
                    logger.infof("Username '%s' already belongs to this user (ID: %s). No collision - allowing activation.", 
                        registerUserPartial.getUsername(), userRow.getId());
                }
            } else {
                // Username doesn't exist for any user - this is fine, proceed with activation
                logger.infof("Username '%s' is available. No collision detected.", registerUserPartial.getUsername());
            }
        } else {
            logger.warnf("Username is null or empty for user ID: %s. Skipping username validation.", userRow.getId());
        }

        if (registrationState.isPresent()) {
            String currentState = registrationState.get().getPropertyValue();
            logger.infof("Found registration state for user %s: %s", userRow.getId(), currentState);
            
            if (currentState.equals(USER_REGISTRATION_STATE_REGISTERED)) {
                logger.warnf("User %s (Email: %s) is already registered. Current state: %s. Activation rejected.", 
                    userRow.getId(), registerUserPartial.getEmail(), currentState);
                logger.warnf("TROUBLESHOOTING: This user has already completed activation. If you need to reactivate:");
                logger.warnf("STEP 1: Check if user exists in Keycloak with email: %s", registerUserPartial.getEmail());
                logger.warnf("STEP 2: If user should be reactivated, update registration state in database");
                logger.warnf("STEP 3: Or use a different email/username for testing");
                throw new RegistrationException(RegistrationException.USER_ALREADY_REGISTERED);
            } else {
                logger.infof("User registration state is '%s' (not REGISTERED), proceeding with activation", currentState);
            }
        } else {
            logger.infof("No registration state found for user %s, proceeding with activation", userRow.getId());
        }
        
        logger.infof("Activation checks passed for user %s. Proceeding with Keycloak registration.", userRow.getId());

        // Update username first
        userRow.setUsername(registerUserPartial.getUsername());
        // userDao.update(userRow);

        // Debug: Log before Keycloak registration
        logger.infof("Attempting Keycloak registration for user: %s", userRow.getId());

        // Handle Keycloak registration
        try {
            registerUserWithKeycloak(registerUserPartial, userRow);
            userDao.update(userRow); // Update user with Keycloak ID
        } catch (Exception e) {
            logger.errorf("Keycloak activation failed for user %s: %s",
                    userRow.getId(), e.getMessage());
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                String lowerError = errorMessage.toLowerCase();
                if (lowerError.contains("already in use") || lowerError.contains("user exists")) {
                    if (lowerError.contains("email") || lowerError.contains("user exists with same email")) {
                        // User already exists in Keycloak from a previous partial activation.
                        // Look them up and recover the real UUID instead of failing.
                        logger.infof("User %s already exists in Keycloak — recovering existing UUID", registerUserPartial.getEmail());
                        try {
                            FindUserResponse existingKeycloakUser = keycloakProvider.findUser(registerUserPartial);
                            userRow.setKeycloakId(existingKeycloakUser.getId());
                            userDao.update(userRow);
                            logger.infof("Recovered Keycloak ID %s for user %s", existingKeycloakUser.getId(), userRow.getId());
                        } catch (Exception lookupEx) {
                            logger.errorf("Could not recover Keycloak user for %s: %s", registerUserPartial.getEmail(), lookupEx.getMessage());
                            throw new RegistrationException(RegistrationException.EMAIL_COLLISION);
                        }
                    } else if (lowerError.contains("username")) {
                        throw new RegistrationException(RegistrationException.USERNAME_COLLISION);
                    }
                } else {
                    throw new RegistrationException("Keycloak activation failed", e);
                }
            } else {
                throw new RegistrationException("Keycloak activation failed", e);
            }
        }

        // Finalize activation
        activateRegistration(userRow);
        // Create drills for the new user
        // createDrillsForNewUser(userRow.getId());
        createDrillsForNewUser(userRow.getId());

        // create user and team map
        mapUserToTeam(registerUserPartial, userRow.getId());
    }

    private void mapUserToTeam(RegisterUserPartial partial, UUID userId) {
        UUID teamId = partial.getTeamId();
        if (teamId == null) {
            logger.warnf("No teamId for user %s — falling back to default team", userId);
            teamId = UUID.fromString("2a3b4697-26ee-4294-812e-6e1b00bd8e90");
        }
        try {
            teamService.mapUserToTeam(userId, teamId);
        } catch (Exception e) {
            logger.warnf("Failed to map user %s to team %s: %s", userId, teamId, e.getMessage());
        }
    }

    private void assignKeycloakRole(UUID keycloakUserId, String roleName) {
        try {
            logger.infof("Attempting to assign Keycloak role '%s' to user with Keycloak ID: %s", roleName, keycloakUserId);
            keycloakProvider.assignRoleToUser(keycloakUserId, roleName);
            logger.infof("Successfully assigned Keycloak role '%s' to user %s", roleName, keycloakUserId);
        } catch (Exception e) {
            logger.errorf("Failed to assign role '%s' to user with Keycloak ID: %s", roleName, keycloakUserId, e);
            logger.errorf("Error details: %s", e.getMessage());
            if (e.getCause() != null) {
                logger.errorf("Root cause: %s", e.getCause().getMessage());
            }
            throw new RegistrationException("Role assignment failed for role '" + roleName + "' and user " + keycloakUserId + ": " + e.getMessage(), e);
        }
    }

    public int update(UserRow userRow) {
        return userDao.update(userRow);
    }

    public int updateMetadata(UUID userId, java.util.Map<String, Object> metadata) {
        try {
            String value = metadata != null
                    ? new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata)
                    : "{}";
            return userDao.updateMetadata(userId, value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }
    }

    public Optional<UserRow> findById(UUID id) {
        return userDao.findById(id);
    }

    public Optional<UserRow> findByEmail(String email) {
        return userDao.findByEmail(email);
    }

    public Optional<UserRow> findByRegistrationCode(String registrationCode) {
        return userDao.findByRegistrationCode(registrationCode);
    }

    public Optional<UserRow> findByUsername(String username) {
        // Username is stored in lowercase, so lowercase the lookup
        String lowercasedUsername = username != null ? username.toLowerCase() : null;
        return userDao.findByUsername(lowercasedUsername);
    }

    public java.util.List<com.lektralabs.thrones.pallbearer.jdbi.model.BasicUserRow> findAllBasicUsers() {
        return userDao.findAllBasic();
    }

    private UserRow createUser(RegisterUserPartial registerUserPartial) {
        try {
            UUID contactId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID keycloakId = INVALID_PLACEHOLDER_UUID;

            ContactRow contactRow = ContactFactory.factoryCreateContact(contactId, registerUserPartial);
            UUID contactRv = contactBaseDao.insert(contactRow);
            logger.warn(String.format("insertContact returned [%s]", contactRv));

            UserRow userRow = UserFactory.factoryCreateUser(userId, contactId, keycloakId, registerUserPartial);
            UUID userRv = userDao.insert(userRow);
            logger.warn(String.format("insertUser returned [%s]", userRv));
            return userRow;
        } catch (Exception e) {
            throw new RegistrationException(String.format("Could not create a user"), e);
        }
    }

    private RoleRow getRole(RegisterUserPartial registerUserPartial) {
        try {
            List<RoleRow> roleRows = roleDao.findByName(registerUserPartial.getRole());
            logger.warn(String.format("Found roles %s", StringUtils.join(roleRows, "||")));
            RoleRow roleRow = roleRows.get(0);
            return roleRow;
        } catch (Exception e) {
            throw new RegistrationException(String.format("Could not find role [%s] for user",
                    registerUserPartial.getRole()), e);
        }
    }

    private void associateUserToRole(UserRow userRow, RoleRow roleRow) {
        try {
            userDao.associateRole(userRow.getId(), roleRow.getId());
        } catch (Exception e) {
            throw new RegistrationException(String.format("Could not associate user to role"), e);
        }
    }

    private void registerUserWithKeycloak(RegisterUserPartial registerUserPartial, UserRow userRow)
            throws RegistrationException {
        try {
            logger.infof("Starting Keycloak registration for user: %s", userRow.getId());
            logger.infof("User details - Email: %s, Username: %s, Role: %s", 
                registerUserPartial.getEmail(), registerUserPartial.getUsername(), registerUserPartial.getRole());
            
            logger.infof("Step 1: Registering user in Keycloak");
            keycloakProvider.registerUser(registerUserPartial);
            logger.infof("Step 1 completed: User registered successfully");
            
            logger.infof("Step 2: Finding user in Keycloak");
            FindUserResponse findUserResponse = keycloakProvider.findUser(registerUserPartial);
            logger.infof("Step 2 completed: Found user with Keycloak ID: %s", findUserResponse.getId());
            
            logger.infof("Step 2a: Clearing required actions to prevent 'Account is not fully set up' error");
            try {
                keycloakProvider.clearRequiredActions(findUserResponse.getId());
                logger.infof("Step 2a completed: Required actions cleared successfully");
            } catch (Exception e) {
                logger.warnf("Failed to clear required actions (non-critical): %s", e.getMessage());
                // Don't fail registration if clearing required actions fails
            }
            
            logger.infof("Step 3: Associating group with user");
            keycloakProvider.associateGroupWithUser(findUserResponse.getId(), registerUserPartial);
            logger.infof("Step 3 completed: Group associated successfully");
            
            logger.infof("Step 4: Assigning role to user");
            // Note: Using hardcoded "ATHLETE" role - consider using registerUserPartial.getRole() if roles match
            // String roleToAssign = "ATHLETE";
            String roleToAssign = registerUserPartial.getRole() != null ? registerUserPartial.getRole() : "ATHLETE";
            logger.infof("Assigning role '%s' to user with Keycloak ID: %s", roleToAssign, findUserResponse.getId());
            assignKeycloakRole(findUserResponse.getId(), roleToAssign);
            logger.infof("Step 4 completed: Role assigned successfully");
            
            logger.infof("Step 5: Updating user record with Keycloak ID");
            userRow.setKeycloakId(findUserResponse.getId());
            userDao.update(userRow);
            logger.infof("Step 5 completed: User record updated with Keycloak ID: %s", findUserResponse.getId());
            
            logger.infof("Keycloak registration completed successfully for user: %s", userRow.getId());
        } catch (RegistrationException e) {
            logger.errorf("RegistrationException during Keycloak registration for user %s: %s", 
                userRow.getId(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.errorf("Unexpected error during Keycloak registration for user %s (Email: %s, Username: %s): %s", 
                userRow.getId(), registerUserPartial.getEmail(), registerUserPartial.getUsername(), e.getMessage(), e);
            throw new RegistrationException("Could not register user with Keycloak: " + e.getMessage(), e);
        }
    }

    private void setRegistrationStep(UserRow userRow) throws RegistrationException {
        try {
            // remove any existing properties for this user by that name
            userPropertyDao.deleteByKey(userRow.getId(), USER_REGISTRATION_STATE_KEY);

            UserPropertyRow record = UserPropertyRow.builder()
                    .id(UUID.randomUUID())
                    .userId(userRow.getId())
                    .propertyKey(USER_REGISTRATION_STEP_KEY)
                    .propertyValue(USER_REGISTRATION_STEP_1_6).build();
            userPropertyDao.insert(record);
        } catch (Exception e) {
            throw new RegistrationException("Could not set registration step", e);
        }
    }

    private void activateRegistration(UserRow userRow) throws RegistrationException {
        try {
            // Clear existing registration state
            userPropertyDao.deleteByKey(userRow.getId(), UserPropertyConstants.USER_REGISTRATION_STATE_KEY);

            // Set registration state
            UserPropertyRow stateRecord = UserPropertyRow.builder()
                    .id(UUID.randomUUID())
                    .userId(userRow.getId())
                    .propertyKey(UserPropertyConstants.USER_REGISTRATION_STATE_KEY)
                    .propertyValue(UserPropertyConstants.USER_REGISTRATION_STATE_REGISTERED)
                    .build();
            userPropertyDao.insert(stateRecord);

            // Set registration date
            userPropertyDao.deleteByKey(userRow.getId(), UserPropertyConstants.USER_REGISTRATION_DATE_KEY);
            UserPropertyRow dateRecord = UserPropertyRow.builder()
                    .id(UUID.randomUUID())
                    .userId(userRow.getId())
                    .propertyKey(UserPropertyConstants.USER_REGISTRATION_DATE_KEY)
                    .propertyValue(String.valueOf(DateTimeUtils.now().getMillis()))
                    .build();
            userPropertyDao.insert(dateRecord);

            // Set default drill group properties
            UUID beginnerLevelId = UUID.fromString("aac04c9c-b71a-47af-8cc2-861ce4cd5acd"); // Beginner level ID

            List<DrillGroupRow> allGroups = drillGroupService.findAll(new FindOptions());

            userPropertyDao.deleteByKey(userRow.getId(), "user.drill.group.name");
            userPropertyDao.deleteByKey(userRow.getId(), "user.registration.payment.state");
            userPropertyDao.deleteByKey(userRow.getId(), "user.drill.group");
            userPropertyDao.deleteByKey(userRow.getId(), UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_KEY);
            insertProperty(userRow.getId(), "user.drill.group.name", "Beginner");
            insertProperty(userRow.getId(), "user.drill.group", beginnerLevelId.toString());
            insertProperty(userRow.getId(), UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_KEY,
                    UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_PAID);

            for (DrillGroupRow group : allGroups) {
                UUID groupId = group.getId();

                // Clean up any existing properties for this group
                userGroupPropertyDao.deleteByUserAndGroup(userRow.getId(), groupId);

                // Insert common default group metrics
                insertGroupProperty(userRow.getId(), groupId,
                        UserPropertyConstants.USER_METRIC_DRILL_GROUP_COMPLETION_PERCENT, "0");
                insertGroupProperty(userRow.getId(), groupId,
                        UserPropertyConstants.USER_METRIC_DRILL_LEVEL_COMPLETION_PERCENT, "0");
                insertGroupProperty(userRow.getId(), groupId, UserPropertyConstants.USER_DRILL_GROUP_ORDER_INDEX_KEY,
                        "1.0");
                insertGroupProperty(userRow.getId(), groupId, UserPropertyConstants.USER_DRILL_GROUP_LEVEL_KEY, "1.0");
            }

        } catch (Exception e) {
            throw new RegistrationException("Could not set registration state", e);
        }
    }

    private void createDrillsForNewUser(UUID userId) {
        logger.info("Creating drills for the user with id : " + userId);
        try {
            FindOptions findOptions = new FindOptions();
            findOptions.setLimit(Integer.MAX_VALUE); // Or -1, depending on your system
            List<DrillItemRow> allDrillItems = drillItemService.findAll(findOptions);
            logger.info("all drill items : " + allDrillItems.size());
            for (DrillItemRow drillItem : allDrillItems) {
                DrillPartial drillPartial = DrillPartial.builder()
                        .drillId(Optional.empty()) // Fix: ensure it's not null
                        .drillItemId(drillItem.getId())
                        .userId(userId)
                        .mediaId(Optional.empty())
                        .drillStatus("NOT-ATTEMPTED")
                        .attemptsDetected(0)
                        .attemptsReported(0)
                        .makesDetected(0)
                        .makesReported(0)
                        .version(Optional.of(0))
                        .build();

                drillService.create(drillPartial, userId);
            }
        } catch (Exception e) {
            logger.info("Failed to create drill for user : " + userId + " due to : " + e.getMessage());
        }
    }

    private void insertProperty(UUID userId, String key, String value) {
        userPropertyDao.insert(UserPropertyRow.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .propertyKey(key)
                .propertyValue(value)
                .build());
    }

    private void insertGroupProperty(UUID userId, UUID drillGroupId, String key, String value) {
        userGroupPropertyDao.insert(UserGroupPropertyRow.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .drillGroupId(drillGroupId)
                .propertyKey(key)
                .propertyValue(value)
                .build());
    }

    public void sendEmailVerificationOTP(String email) throws Exception {
        try {
            if (findByEmail(email).isEmpty()) {
                throw new RuntimeException("User not found");
            }
            userOTPDao.deleteByEmail(email); // Clear any existing OTPs for the email

            Integer otp = generateOTP();
            String emailContent = getOtpEmailTemplate(otp);
            UserOTPRow otpRow = new UserOTPRow();
            otpRow.setOtp(otp.longValue());
            otpRow.setEmail(email);
            otpRow.setCreatedAt(new java.sql.Timestamp(DateTimeUtils.now().toDateTime().getMillis()));
            userOTPDao.insert(otpRow);
            mailClient.sendHtmlEmail(email, "Email Verification OTP", emailContent);
        } catch (Exception e) {
            logger.warn("Error sending email verification OTP", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public boolean verifyEmailOTP(String email, String otp) {
        try {
            Optional<UserOTPRow> maybeOtpRow = userOTPDao.getByEmail(email);
            Integer userOtp = Integer.parseInt(otp);
            if (maybeOtpRow.isPresent()) {
                UserOTPRow otpRow = maybeOtpRow.get();
                if (otpRow.getOtp().equals(userOtp.longValue())) {
                    userOTPDao.deleteByEmail(email);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.warn("Error verifying email OTP", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private static int generateOTP() {
        Random random = new Random();
        return 100000 + random.nextInt(900000); // Generates a 6-digit OTP
    }

    private String getOtpEmailTemplate(Integer otp) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "  <meta charset='UTF-8'>"
                + "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "  <title>Email Verification</title>"
                + "</head>"
                + "<body style='font-family: Arial, sans-serif; background-color: #f2f2f2; margin: 0; padding: 0;'>"
                + "  <table align='center' width='100%' height='100%' cellpadding='0' cellspacing='0' border='0' style='background-color: #f2f2f2;'>"
                + "    <tr>"
                + "      <td align='center'>"
                + "        <table width='400' cellpadding='0' cellspacing='0' border='0' style='background-color: #ffffff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1);'>"
                + "          <tr>"
                + "            <td style='text-align: center;'>"
                + "              <h2 style='color: #333333;'>Verify Your Email</h2>"
                + "              <p style='font-size: 16px; color: #666666;'>Your One-Time Password (OTP) is:</p>"
                + "              <p style='font-size: 32px; font-weight: bold; color: #4CAF50; margin: 20px 0;'>" + otp
                + "</p>"
                + "              <p style='font-size: 14px; color: #999999;'>This OTP is valid for the next 10 minutes. Please do not share it with anyone.</p>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    /**
     * Deletes a user and all related data from all database tables.
     * This method carefully deletes data in the correct order to respect foreign key constraints.
     * 
     * @param userId The UUID of the user to delete
     * @return Map containing deletion statistics
     * @throws RuntimeException if user not found or deletion fails
     */
    public Map<String, Object> deleteUserWithAllRelatedData(UUID userId) {
        return jdbiProvider.getJdbi().inTransaction(TransactionIsolationLevel.SERIALIZABLE, handle -> {
            try {
                // First, verify user exists
                Optional<UserRow> userOpt = userDao.findById(userId);
                if (userOpt.isEmpty()) {
                    throw new RuntimeException("User not found with id: " + userId);
                }
                
                UserRow user = userOpt.get();
                UUID contactId = user.getContactId();
                String email = user.getEmail();
                
                Map<String, Object> deletionStats = new HashMap<>();
                
                // Delete in order to respect foreign key constraints
                // 1. Delete drill attempt history (references t_drill via drill_id, and user_id)
                int drillAttemptHistoryDeleted = handle.createUpdate(
                    "DELETE FROM t_drill_attempt_history WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("drillAttemptHistoryDeleted", drillAttemptHistoryDeleted);
                logger.info("Deleted " + drillAttemptHistoryDeleted + " drill attempt history records for user " + userId);
                
                // 2. Delete drills (references t_drill_item and t_user)
                int drillsDeleted = handle.createUpdate(
                    "DELETE FROM t_drill WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("drillsDeleted", drillsDeleted);
                logger.info("Deleted " + drillsDeleted + " drill records for user " + userId);
                
                // 3. Delete user drill group properties
                int userGroupPropertiesDeleted = handle.createUpdate(
                    "DELETE FROM t_user_drill_group_property WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("userGroupPropertiesDeleted", userGroupPropertiesDeleted);
                logger.info("Deleted " + userGroupPropertiesDeleted + " user group property records for user " + userId);
                
                // 4. Delete user OTP records (by email)
                int userOtpDeleted = handle.createUpdate(
                    "DELETE FROM t_user_otp WHERE email = :email")
                    .bind("email", email)
                    .execute();
                deletionStats.put("userOtpDeleted", userOtpDeleted);
                logger.info("Deleted " + userOtpDeleted + " user OTP records for email " + email);
                
                // 5. Delete user properties
                int userPropertiesDeleted = handle.createUpdate(
                    "DELETE FROM t_user_property WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("userPropertiesDeleted", userPropertiesDeleted);
                logger.info("Deleted " + userPropertiesDeleted + " user property records for user " + userId);
                
                // 6. Delete user role xref
                int userRoleXrefDeleted = handle.createUpdate(
                    "DELETE FROM t_user_role_xref WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("userRoleXrefDeleted", userRoleXrefDeleted);
                logger.info("Deleted " + userRoleXrefDeleted + " user role xref records for user " + userId);
                
                // 7. Delete team user xref
                int teamUserXrefDeleted = handle.createUpdate(
                    "DELETE FROM t_team_user_xref WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("teamUserXrefDeleted", teamUserXrefDeleted);
                logger.info("Deleted " + teamUserXrefDeleted + " team user xref records for user " + userId);
                
                // 8. Delete organization user xref
                int orgUserXrefDeleted = handle.createUpdate(
                    "DELETE FROM t_organization_user_xref WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("orgUserXrefDeleted", orgUserXrefDeleted);
                logger.info("Deleted " + orgUserXrefDeleted + " organization user xref records for user " + userId);
                
                // 9. Delete user media xref
                int userMediaXrefDeleted = handle.createUpdate(
                    "DELETE FROM t_user_media_xref WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("userMediaXrefDeleted", userMediaXrefDeleted);
                logger.info("Deleted " + userMediaXrefDeleted + " user media xref records for user " + userId);
                
                // 10. Delete challenge tokens
                int challengeTokensDeleted = handle.createUpdate(
                    "DELETE FROM t_challenge_token WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("challengeTokensDeleted", challengeTokensDeleted);
                logger.info("Deleted " + challengeTokensDeleted + " challenge token records for user " + userId);
                
                // 11. Delete challenge invitations (where user is invited)
                int challengeInvitationsDeleted = handle.createUpdate(
                    "DELETE FROM t_challenge_invitation WHERE invite_user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("challengeInvitationsDeleted", challengeInvitationsDeleted);
                logger.info("Deleted " + challengeInvitationsDeleted + " challenge invitation records for user " + userId);
                
                // 12. Delete challenge participants
                int challengeParticipantsDeleted = handle.createUpdate(
                    "DELETE FROM t_challenge_participant WHERE participant_user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("challengeParticipantsDeleted", challengeParticipantsDeleted);
                logger.info("Deleted " + challengeParticipantsDeleted + " challenge participant records for user " + userId);
                
                // 13. Delete notifications (both sent and received)
                int notificationsDeleted = handle.createUpdate(
                    "DELETE FROM t_notification WHERE sent_user_id = :userId OR recd_user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("notificationsDeleted", notificationsDeleted);
                logger.info("Deleted " + notificationsDeleted + " notification records for user " + userId);
                
                // 14. Delete challenge entry like xref
                int challengeEntryLikeXrefDeleted = handle.createUpdate(
                    "DELETE FROM t_challenge_entry_like_xref WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("challengeEntryLikeXrefDeleted", challengeEntryLikeXrefDeleted);
                logger.info("Deleted " + challengeEntryLikeXrefDeleted + " challenge entry like xref records for user " + userId);
                
                // 15. Delete challenge entry comment xref
                int challengeEntryCommentXrefDeleted = handle.createUpdate(
                    "DELETE FROM t_challenge_entry_comment_xref WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("challengeEntryCommentXrefDeleted", challengeEntryCommentXrefDeleted);
                logger.info("Deleted " + challengeEntryCommentXrefDeleted + " challenge entry comment xref records for user " + userId);
                
                // 16. Delete comments
                int commentsDeleted = handle.createUpdate(
                    "DELETE FROM t_comment WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("commentsDeleted", commentsDeleted);
                logger.info("Deleted " + commentsDeleted + " comment records for user " + userId);
                
                // 17. Delete likes
                int likesDeleted = handle.createUpdate(
                    "DELETE FROM t_like WHERE user_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("likesDeleted", likesDeleted);
                logger.info("Deleted " + likesDeleted + " like records for user " + userId);
                
                // 18. Delete challenge entries created by user (if any)
                int challengeEntriesDeleted = handle.createUpdate(
                    "DELETE FROM t_challenge_entry WHERE created_by_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("challengeEntriesDeleted", challengeEntriesDeleted);
                logger.info("Deleted " + challengeEntriesDeleted + " challenge entry records created by user " + userId);
                
                // 19. Delete challenge entry votes created by user
                int challengeEntryVotesDeleted = handle.createUpdate(
                    "DELETE FROM t_challenge_entry_vote WHERE created_by_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("challengeEntryVotesDeleted", challengeEntryVotesDeleted);
                logger.info("Deleted " + challengeEntryVotesDeleted + " challenge entry vote records created by user " + userId);
                
                // 20. Delete challenges created by user
                int challengesDeleted = handle.createUpdate(
                    "DELETE FROM t_challenge WHERE created_by_id = :userId")
                    .bind("userId", userId)
                    .execute();
                deletionStats.put("challengesDeleted", challengesDeleted);
                logger.info("Deleted " + challengesDeleted + " challenge records created by user " + userId);
                
                // 21. Delete the user record itself
                int userDeleted = userDao.delete(userId);
                deletionStats.put("userDeleted", userDeleted);
                logger.info("Deleted user record " + userId);
                
                // 22. Delete the associated contact (only if not used by other users)
                // Check if contact is used by other users
                int contactUsageCount = handle.createQuery(
                    "SELECT COUNT(*) FROM t_user WHERE contact_id = :contactId")
                    .bind("contactId", contactId)
                    .mapTo(Integer.class)
                    .one();
                
                if (contactUsageCount == 0) {
                    int contactDeleted = handle.createUpdate(
                        "DELETE FROM t_contact WHERE id = :contactId")
                        .bind("contactId", contactId)
                        .execute();
                    deletionStats.put("contactDeleted", contactDeleted);
                    logger.info("Deleted contact record " + contactId);
                } else {
                    deletionStats.put("contactDeleted", 0);
                    logger.info("Contact " + contactId + " not deleted as it is still referenced by other users");
                }
                
                deletionStats.put("status", "SUCCESS");
                deletionStats.put("userId", userId.toString());
                deletionStats.put("email", email);
                
                logger.info("Successfully deleted user " + userId + " and all related data");
                return deletionStats;
                
            } catch (Exception e) {
                logger.error("Error deleting user {} and related data", userId, e);
                throw new TransactionException("Failed to delete user and related data: " + e.getMessage(), e);
            }
        });
    }
}
