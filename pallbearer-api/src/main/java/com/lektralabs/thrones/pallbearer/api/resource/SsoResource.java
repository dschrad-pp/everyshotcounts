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
import com.lektralabs.thrones.pallbearer.api.auth.AuthErrorCode;
import com.lektralabs.thrones.pallbearer.api.auth.LoginRateLimiter;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.Context;
import io.vertx.core.http.HttpServerRequest;
import java.io.IOException;


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
    @Inject
    LoginRateLimiter loginRateLimiter;

    @ConfigProperty(name = "admin.bootstrap.api.key")
    String adminBootstrapApiKey;

    // ---------------------------------------------------------------------------------------------
    // Auth helpers: machine-readable error codes + brute-force protection
    // ---------------------------------------------------------------------------------------------

    /** Build a structured error carrying the stable {@code error_code} and the code's default message. */
    private Response authError(AuthErrorCode code) {
        return authError(code, code.defaultMessage());
    }

    /** Build a structured error carrying the stable {@code error_code} and a custom message. */
    private Response authError(AuthErrorCode code, String message) {
        return Response.status(code.httpStatus())
                .entity(new GenericApiResponse<>(code.httpStatus(), code.name(), message, null))
                .build();
    }

    /** Resolve the client IP, honouring a reverse proxy's X-Forwarded-For when present. */
    private String clientIp(String forwardedFor, HttpServerRequest request) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (request != null && request.remoteAddress() != null) {
            return request.remoteAddress().host();
        }
        return "unknown";
    }

    /**
     * Reject the request with 429 + Retry-After if the account or IP is currently locked out.
     *
     * @return a 429 Response if throttled, otherwise {@code null} (caller may proceed).
     */
    private Response throttleOrNull(String username, String clientIp) {
        long retryAfter = loginRateLimiter.retryAfterSeconds(
                LoginRateLimiter.userKey(username), LoginRateLimiter.ipKey(clientIp));
        if (retryAfter <= 0) {
            return null;
        }
        logger.warn("Login throttled for user '{}' / ip '{}', retry after {}s", username, clientIp, retryAfter);
        Map<String, Object> data = new HashMap<>();
        data.put("retry_after_seconds", retryAfter);
        return Response.status(AuthErrorCode.TOO_MANY_ATTEMPTS.httpStatus())
                .header("Retry-After", retryAfter)
                .entity(new GenericApiResponse<>(
                        AuthErrorCode.TOO_MANY_ATTEMPTS.httpStatus(),
                        AuthErrorCode.TOO_MANY_ATTEMPTS.name(),
                        AuthErrorCode.TOO_MANY_ATTEMPTS.defaultMessage(),
                        data))
                .build();
    }

    /** Record a failed attempt against both the account and IP buckets (different thresholds). */
    private void recordLoginFailure(String username, String clientIp) {
        loginRateLimiter.recordFailure(LoginRateLimiter.userKey(username), LoginRateLimiter.MAX_ATTEMPTS_PER_USER);
        loginRateLimiter.recordFailure(LoginRateLimiter.ipKey(clientIp), LoginRateLimiter.MAX_ATTEMPTS_PER_IP);
    }

    /** Clear lockout counters for a successful authentication. */
    private void clearLoginFailures(String username, String clientIp) {
        loginRateLimiter.reset(LoginRateLimiter.userKey(username), LoginRateLimiter.ipKey(clientIp));
    }

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
    public Response adminLogin(LoginUser loginUser,
            @HeaderParam("X-Forwarded-For") String forwardedFor,
            @Context HttpServerRequest httpRequest) {
        if (loginUser == null || loginUser.getUsername() == null || loginUser.getUsername().isBlank()
                || loginUser.getPassword() == null || loginUser.getPassword().isBlank()) {
            return authError(AuthErrorCode.VALIDATION_ERROR);
        }
        String username = loginUser.getUsername();
        String clientIp = clientIp(forwardedFor, httpRequest);

        Response throttled = throttleOrNull(username, clientIp);
        if (throttled != null) {
            return throttled;
        }

        try {
            OpenIdResponse tokenResponse = keycloakProvider.getUserAccessToken(username,
                    loginUser.getPassword());

            if (tokenResponse.getRole() == null || !"ADMIN".equalsIgnoreCase(tokenResponse.getRole())) {
                // Authenticated successfully, so clear failure counters; the rejection is authorization, not credentials.
                clearLoginFailures(username, clientIp);
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new GenericApiResponse<>(403, "FORBIDDEN_ROLE", "Only ADMIN can login here", null))
                        .build();
            }

            clearLoginFailures(username, clientIp);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("access_token", tokenResponse.getAccessToken());
            responseBody.put("refresh_token", tokenResponse.getRefreshToken());
            responseBody.put("expires_in", tokenResponse.getExpiresIn());
            responseBody.put("token_type", tokenResponse.getTokenType());
            responseBody.put("role", tokenResponse.getRole());
            return Response.ok(new GenericApiResponse<>(200, "Admin login successful", responseBody)).build();
        } catch (Exception e) {
            logger.warn("Admin login failed", e);
            recordLoginFailure(username, clientIp);
            return authError(AuthErrorCode.INVALID_CREDENTIALS);
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
    public Response crmLogin(LoginUser loginUser,
            @HeaderParam("X-Forwarded-For") String forwardedFor,
            @Context HttpServerRequest httpRequest) {
        String username = loginUser != null ? loginUser.getUsername() : null;
        String password = loginUser != null ? loginUser.getPassword() : null;
        String clientIp = clientIp(forwardedFor, httpRequest);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return authError(AuthErrorCode.VALIDATION_ERROR);
        }

        // Brute-force protection: bail out early if this account/IP is locked out.
        Response throttled = throttleOrNull(username, clientIp);
        if (throttled != null) {
            return throttled;
        }

        try {
            // Step 1: Validate credentials against CRM.
            // A wrong password AND an unknown user both surface here as INVALID_CREDENTIALS —
            // identical response on purpose, to avoid user enumeration.
            boolean crmValid = crmApiClient.validateUserCredentials(username, password);
            if (!crmValid) {
                logger.warn("CRM credential validation failed for user: {}", username);
                recordLoginFailure(username, clientIp);
                return authError(AuthErrorCode.INVALID_CREDENTIALS);
            }

            // Step 2: Check local t_crm_registration — confirms they were synced/webhoooked (i.e. registered in CRM)
            CrmRegistrationRow registration = crmRegistrationService.findByUsername(username)
                    .or(() -> crmRegistrationService.findByEmail(username))
                    .orElse(null);

            if (registration == null) {
                logger.warn("CRM login: no registration record found for user: {}", username);
                // Credentials are valid (proven above), so this is not a credential-probe vector;
                // a distinct code lets the app guide the user to finish registration.
                return authError(AuthErrorCode.REGISTRATION_INCOMPLETE);
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
                return authError(AuthErrorCode.PAYMENT_REQUIRED);
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
                userService.activateUser(keycloakPartial);
            } else {
                logger.info("CRM login: syncing Keycloak password for existing user {}", regUsername);
                keycloakProvider.changeUserPassword(userRow.getKeycloakId(), password);
            }

            // Step 7: Get token using canonical regUsername
            OpenIdResponse tokenResponse = keycloakProvider.getUserAccessToken(regUsername, password);

            // Successful login — clear any accumulated failure counters.
            clearLoginFailures(username, clientIp);

            java.util.Map<String, Object> responseBody = new java.util.HashMap<>();
            responseBody.put("access_token", tokenResponse.getAccessToken());
            responseBody.put("refresh_token", tokenResponse.getRefreshToken());
            responseBody.put("expires_in", tokenResponse.getExpiresIn());
            responseBody.put("token_type", tokenResponse.getTokenType());
            responseBody.put("roles", tokenResponse.getRole());
            responseBody.put("is_new_user", isNewUser);
            return Response.ok(responseBody).build();

        } catch (IOException e) {
            // CRM / upstream identity provider unreachable — this is not a credential problem,
            // so don't penalize the user with a failed-attempt count, and don't return 401/500.
            logger.error("CRM login upstream unavailable for user: {}", username, e);
            return authError(AuthErrorCode.SERVICE_UNAVAILABLE);
        } catch (IllegalArgumentException e) {
            // Keycloak rejected the credentials/provisioning — treat as invalid credentials.
            logger.warn("CRM login Keycloak error for user {}: {}", username, e.getMessage());
            recordLoginFailure(username, clientIp);
            return authError(AuthErrorCode.INVALID_CREDENTIALS);
        } catch (Exception e) {
            logger.error("CRM login failed for user: {} [{}] {}", username, e.getClass().getSimpleName(), e.getMessage(), e);
            return authError(AuthErrorCode.INTERNAL_ERROR);
        }
    }


    @Path("/coach-login")
@POST
@PermitAll
public Response coachLogin(LoginUser loginUser,
        @HeaderParam("X-Forwarded-For") String forwardedFor,
        @Context HttpServerRequest httpRequest) {
    String username = loginUser != null ? loginUser.getUsername() : null;
    String password = loginUser != null ? loginUser.getPassword() : null;
    String clientIp = clientIp(forwardedFor, httpRequest);

    if (username == null || username.isBlank() || password == null || password.isBlank()) {
        return authError(AuthErrorCode.VALIDATION_ERROR);
    }

    Response throttled = throttleOrNull(username, clientIp);
    if (throttled != null) {
        return throttled;
    }

    try {
        // Step 1: Find user and verify COACH role in one query
        CoachPartial coach = coachDrillService.findCoachByUsername(username).orElse(null);

        if (coach == null) {
            recordLoginFailure(username, clientIp);
            return authError(AuthErrorCode.INVALID_CREDENTIALS);
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
            userService.activateUser(keycloakPartial);
        } else {
            keycloakProvider.changeUserPassword(coach.getKeycloakId(), password);
        }

        // Step 3: Return token
        OpenIdResponse tokenResponse = keycloakProvider.getUserAccessToken(coach.getUsername(), password);

        clearLoginFailures(username, clientIp);

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

    } catch (IllegalArgumentException e) {
        // Keycloak rejected the password for an existing coach — invalid credentials, not a 500.
        logger.warn("Coach login Keycloak error for user {}: {}", username, e.getMessage());
        recordLoginFailure(username, clientIp);
        return authError(AuthErrorCode.INVALID_CREDENTIALS);
    } catch (Exception e) {
        logger.error("Coach login failed for user: {} [{}] {}", username, e.getClass().getSimpleName(), e.getMessage(), e);
        return authError(AuthErrorCode.INTERNAL_ERROR);
    }
}
}