package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.keycloak.OpenIdResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.LoginUser;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;
import com.lektralabs.thrones.crm.CrmApiClient;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.jdbi.service.CrmRegistrationService;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.CrmRegistrationRow;
import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;
import com.lektralabs.thrones.pallbearer.api.model.request.AdminSignupRequest;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.security.Authenticated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;

import jakarta.ws.rs.HeaderParam;


import com.lektralabs.thrones.pallbearer.jdbi.service.CoachDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.service.TeamService;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.CoachPartial;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;


@Path("/api/sso")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SsoResource {

    private static Logger logger = LoggerFactory.getLogger(SsoResource.class);

    @Inject
    KeycloakProvider keycloakProvider;
    @Inject
    CrmApiClient crmApiClient;
    @Inject
    UserService userService;
    @Inject
    CrmRegistrationService crmRegistrationService;
    @Inject
    CoachDrillService coachDrillService;
    @Inject
    TeamService teamService;

    @ConfigProperty(name = "admin.bootstrap.api.key")
    String adminBootstrapApiKey;

    @Path("/login")
    @POST
    public Response login(LoginUser loginUser) {
        try {
            logger.info("Recieved user request : {}", loginUser.getUsername());
            OpenIdResponse openIdResponse = keycloakProvider.getUserAccessToken(loginUser.getUsername(),
                    loginUser.getPassword());
            return Response.ok(openIdResponse).build();
        } catch (Exception e) {
            logger.warn("Error in current", e);
            return Response.status(Response.Status.FORBIDDEN.getStatusCode()).
                    entity("You shall not pass").build();
        }
    }

    @Path("/admin-signup")
    @POST
    @PermitAll
    public Response adminSignup(@HeaderParam("X-BOOTSTRAP-KEY") String bootstrapKey,
            AdminSignupRequest request) {
        try {
            if (bootstrapKey == null || bootstrapKey.isBlank() || !bootstrapKey.equals(adminBootstrapApiKey)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new GenericApiResponse<>(403, "Invalid bootstrap key", null))
                        .build();
            }

            if (request == null
                    || request.getUsername() == null || request.getUsername().isBlank()
                    || request.getEmail() == null || request.getEmail().isBlank()
                    || request.getPassword() == null || request.getPassword().isBlank()
                    || request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new GenericApiResponse<>(400, "username, email, password, confirmPassword are required", null))
                        .build();
            }

            String username = request.getUsername().trim().toLowerCase();
            String email = request.getEmail().trim().toLowerCase();

            if (request.getPassword().length() < 8) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new GenericApiResponse<>(400, "Password must be at least 8 characters", null))
                        .build();
            }

            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new GenericApiResponse<>(400, "Password and confirmPassword do not match", null))
                        .build();
            }

            if (userService.findByUsername(username).isPresent()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new GenericApiResponse<>(409, "Username already exists", null))
                        .build();
            }

            if (userService.findByEmail(email).isPresent()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new GenericApiResponse<>(409, "Email already exists", null))
                        .build();
            }

            RegisterUserPartial registerUserPartial = RegisterUserPartial.builder()
                    .username(username)
                    .email(email)
                    .password(request.getPassword())
                    .firstName("Admin")
                    .lastName("User")
                    .phoneNumber("")
                    .birthDate(0L)
                    .role("ADMIN")
                    .teamId(null)
                    .build();

            userService.registerUser(registerUserPartial, true);
            OpenIdResponse openIdResponse = keycloakProvider.getUserAccessToken(username, request.getPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("email", email);
            response.put("role", "ADMIN");
            response.put("access_token", openIdResponse.getAccessToken());
            response.put("refresh_token", openIdResponse.getRefreshToken());
            response.put("expires_in", openIdResponse.getExpiresIn());
            response.put("token_type", openIdResponse.getTokenType());
            return Response.ok(new GenericApiResponse<>(200, "Admin created successfully", response)).build();
        } catch (Exception e) {
            logger.error("Admin signup failed", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new GenericApiResponse<>(400, "Admin signup failed: " + e.getMessage(), null))
                    .build();
        }
    }

    @Path("/admin-login")
    @POST
    @PermitAll
    public Response adminLogin(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUsername() == null || loginUser.getUsername().isBlank()
                || loginUser.getPassword() == null || loginUser.getPassword().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new GenericApiResponse<>(400, "Username and password are required", null))
                    .build();
        }
        try {
            OpenIdResponse tokenResponse = keycloakProvider.getUserAccessToken(loginUser.getUsername(),
                    loginUser.getPassword());

            if (tokenResponse.getRole() == null || !"ADMIN".equalsIgnoreCase(tokenResponse.getRole())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new GenericApiResponse<>(403, "Only ADMIN can login here", null))
                        .build();
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("access_token", tokenResponse.getAccessToken());
            responseBody.put("refresh_token", tokenResponse.getRefreshToken());
            responseBody.put("expires_in", tokenResponse.getExpiresIn());
            responseBody.put("token_type", tokenResponse.getTokenType());
            responseBody.put("role", tokenResponse.getRole());
            return Response.ok(new GenericApiResponse<>(200, "Admin login successful", responseBody)).build();
        } catch (Exception e) {
            logger.warn("Admin login failed", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new GenericApiResponse<>(401, "Invalid credentials", null))
                    .build();
        }
    }

    @Path("/logout")
    @POST
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@HeaderParam("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                logger.warn("Invalid or missing Authorization header");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new GenericApiResponse<>(
                                Response.Status.BAD_REQUEST.getStatusCode(),
                                "Invalid or missing Authorization header",
                                null))
                        .build();
            }

            String token = authorizationHeader.substring("Bearer ".length()).trim();
            logger.info("Initiating logout for user token");
            keycloakProvider.logoutUser(token);

            return Response.ok()
                    .entity(new GenericApiResponse<>(
                            Response.Status.OK.getStatusCode(),
                            "Logged out successfully",
                            null))
                    .build();

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid logout request: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new GenericApiResponse<>(
                            Response.Status.BAD_REQUEST.getStatusCode(),
                            e.getMessage(),
                            null))
                    .build();
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(
                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            "Logout failed",
                            null))
                    .build();
        }
    }

    @Path("/crm-login")
    @POST
    @PermitAll
    public Response crmLogin(LoginUser loginUser) {
        String username = loginUser.getUsername();
        String password = loginUser.getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new GenericApiResponse<>(400, "Username and password are required", null))
                    .build();
        }

        try {
            // Step 1: Validate credentials against CRM
            boolean crmValid = crmApiClient.validateUserCredentials(username, password);
            if (!crmValid) {
                logger.warn("CRM credential validation failed for user: {}", username);
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new GenericApiResponse<>(401, "Invalid credentials", null))
                        .build();
            }

            // Step 2: Check local t_crm_registration — confirms they were synced/webhoooked (i.e. registered in CRM)
            CrmRegistrationRow registration = crmRegistrationService.findByUsername(username)
                    .or(() -> crmRegistrationService.findByEmail(username))
                    .orElse(null);

            if (registration == null) {
                logger.warn("CRM login: no registration record found for user: {}", username);
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new GenericApiResponse<>(401, "No registration found. Please complete your registration.", null))
                        .build();
            }

            // Step 3: Enforce payment — only PAID, PARTIAL, TRIAL allowed
            String paymentStatus = registration.getPaymentStatus().orElse(null);
            boolean hasPaidAccess = paymentStatus != null && (
                    paymentStatus.equalsIgnoreCase("PAID") ||
                    paymentStatus.equalsIgnoreCase("PARTIAL") ||
                    paymentStatus.equalsIgnoreCase("TRIAL") ||
                    paymentStatus.equalsIgnoreCase("TRAIL")); // TRAIL is the stored value for trial

            if (!hasPaidAccess) {
                logger.warn("CRM login: payment required for user {} (status: {})", username, paymentStatus);
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new GenericApiResponse<>(403, "Payment required to access this app", null))
                        .build();
            }

            // Step 4: From here use the canonical username/email from the registration row
            // String regUsername = registration.getUsername().orElse(username).toLowerCase();
            // String regEmail    = registration.getEmail().orElse(null);
            String regUsername = (registration.getUsername() != null ? registration.getUsername().orElse(username) : username).toLowerCase();
            String regEmail    = registration.getEmail() != null ? registration.getEmail().orElse(null) : null;

            // Step 5: Find or create local t_user
            UserRow userRow = userService.findByUsername(regUsername)
                    .or(() -> regEmail != null ? userService.findByEmail(regEmail) : java.util.Optional.empty())
                    .orElse(null);

            if (userRow == null) {
                logger.info("CRM login: creating local user for {} (first login)", regUsername);
                RegisterUserPartial createPartial = RegisterUserPartial.builder()
                        .username(regUsername)
                        .password(password)
                        .email(regEmail)
                        .firstName(registration.getFirstName() != null ? registration.getFirstName().orElse("") : "")
                        .lastName(registration.getLastName() != null ? registration.getLastName().orElse("") : "")
                        .phoneNumber(registration.getPhoneNumber() != null ? registration.getPhoneNumber().orElse("") : "")
                        .role(registration.getRole() != null ? registration.getRole().orElse("ATHLETE") : "ATHLETE")
                        .birthDate(0L)
                        .teamId(registration.getTeamId() != null ? registration.getTeamId().orElse(null) : null)
                        .build();
                userRow = userService.registerUser(createPartial, false);
            }

            // Step 6: Keycloak provisioning
            java.util.UUID placeholderUuid = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
            boolean isNewUser = (userRow.getKeycloakId() == null || placeholderUuid.equals(userRow.getKeycloakId()));

            RegisterUserPartial keycloakPartial = RegisterUserPartial.builder()
                    .username(regUsername)
                    .password(password)
                    .email(userRow.getEmail())
                    .firstName(registration.getFirstName() != null ? registration.getFirstName().orElse("") : "")
                    .lastName(registration.getLastName() != null ? registration.getLastName().orElse("") : "")
                    .phoneNumber(registration.getPhoneNumber() != null ? registration.getPhoneNumber().orElse("") : "")
                    .role(registration.getRole() != null ? registration.getRole().orElse("ATHLETE") : "ATHLETE")
                    .birthDate(0L)
                    .teamId(registration.getTeamId() != null ? registration.getTeamId().orElse(null) : null)
                    .build();

            if (isNewUser) {
                logger.info("CRM login: provisioning new Keycloak user for {}", regUsername);
                try {
                    userService.activateUser(keycloakPartial);
                } catch (Exception e) {
                    logger.warn("CRM login: activation issue for {} (will attempt token anyway): {}", regUsername, e.getMessage());
                }
            } else {
                logger.info("CRM login: syncing Keycloak password for existing user {}", regUsername);
                keycloakProvider.changeUserPassword(userRow.getKeycloakId(), password);
                // Refresh team assignment from CRM data on every login
                java.util.UUID crmTeamId = registration.getTeamId() != null ? registration.getTeamId().orElse(null) : null;
                if (crmTeamId != null) {
                    try {
                        teamService.mapUserToTeam(userRow.getId(), crmTeamId);
                    } catch (Exception e) {
                        logger.warn("CRM login: team mapping refresh failed for {} (non-fatal): {}", regUsername, e.getMessage());
                    }
                }
            }

            // Step 7: Get token using canonical regUsername
            OpenIdResponse tokenResponse = keycloakProvider.getUserAccessToken(regUsername, password);

            java.util.Map<String, Object> responseBody = new java.util.HashMap<>();
            responseBody.put("access_token", tokenResponse.getAccessToken());
            responseBody.put("refresh_token", tokenResponse.getRefreshToken());
            responseBody.put("expires_in", tokenResponse.getExpiresIn());
            responseBody.put("token_type", tokenResponse.getTokenType());
            responseBody.put("roles", tokenResponse.getRole());
            responseBody.put("is_new_user", isNewUser);
            return Response.ok(responseBody).build();

        } catch (IllegalArgumentException e) {
            logger.warn("CRM login Keycloak error for user {}: {}", username, e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new GenericApiResponse<>(401, "Authentication failed: " + e.getMessage(), null))
                    .build();
        } catch (Exception e) {
            logger.error("CRM login failed for user: {} [{}] {}", username, e.getClass().getSimpleName(), e.getMessage(), e);
            String debugMsg = "Login failed [" + e.getClass().getSimpleName() + ": " + e.getMessage() + "]";
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(500, debugMsg, null))
                    .build();
        }
    }


    @Path("/coach-login")
@POST
@PermitAll
public Response coachLogin(LoginUser loginUser) {
    String username = loginUser.getUsername();
    String password = loginUser.getPassword();

    if (username == null || username.isBlank() || password == null || password.isBlank()) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new GenericApiResponse<>(400, "Username and password are required", null))
                .build();
    }

    try {
        // Step 1: Find user and verify COACH role in one query
        CoachPartial coach = coachDrillService.findCoachByUsername(username).orElse(null);

        if (coach == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new GenericApiResponse<>(401, "Invalid credentials", null))
                    .build();
        }

        // Look up CRM registration to get the coach's team UUID
        CrmRegistrationRow coachCrmReg = crmRegistrationService.findByUsername(username)
                .or(() -> crmRegistrationService.findByEmail(username))
                .orElse(null);
        java.util.UUID crmTeamId = (coachCrmReg != null && coachCrmReg.getTeamId() != null)
                ? coachCrmReg.getTeamId().orElse(null) : null;

        // Step 2: Keycloak provisioning
        UUID placeholderUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        boolean isNewUser = (coach.getKeycloakId() == null || placeholderUuid.equals(coach.getKeycloakId()));

        RegisterUserPartial keycloakPartial = RegisterUserPartial.builder()
                .username(coach.getUsername())
                .password(password)
                .email(coach.getEmail())
                .role("COACH")
                .birthDate(0L)
                .teamId(crmTeamId)
                .build();

        if (isNewUser) {
            try {
                userService.activateUser(keycloakPartial);
            } catch (Exception e) {
                logger.warn("coach-login: activation issue for {} (will attempt token anyway): {}", username, e.getMessage());
            }
        } else {
            keycloakProvider.changeUserPassword(coach.getKeycloakId(), password);
        }

        // Step 3: Return token
        OpenIdResponse tokenResponse = keycloakProvider.getUserAccessToken(coach.getUsername(), password);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", tokenResponse.getAccessToken());
        responseBody.put("refresh_token", tokenResponse.getRefreshToken());
        responseBody.put("expires_in", tokenResponse.getExpiresIn());
        responseBody.put("token_type", tokenResponse.getTokenType());
        responseBody.put("roles", tokenResponse.getRole());
        responseBody.put("coach_id", coach.getId());
        responseBody.put("first_name", coach.getFirstName());
        responseBody.put("last_name", coach.getLastName());
        return Response.ok(responseBody).build();

    } catch (Exception e) {
        logger.error("Coach login failed for user: {} [{}] {}", username, e.getClass().getSimpleName(), e.getMessage(), e);
        String debugMsg = "Login failed [" + e.getClass().getSimpleName() + ": " + e.getMessage() + "]";
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new GenericApiResponse<>(500, debugMsg, null))
                .build();
    }
}
}