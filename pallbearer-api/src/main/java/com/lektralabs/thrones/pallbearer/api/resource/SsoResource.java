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
import com.lektralabs.thrones.pallbearer.api.model.request.GoogleLoginRequest;
import com.lektralabs.thrones.crm.CrmGoogleResult;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.jdbi.exception.RegistrationException;
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
import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * Enforce {@code subscription_end_date} at login. Default false = shadow mode: the check
     * runs and logs what it WOULD have rejected, but lets the login through.
     *
     * <p>Off by default on purpose. Turning it on locks out every user whose end date has lapsed
     * but whose {@code payment_status} still reads paid/trial — people who log in fine today.
     * Measure the blast radius and read the shadow logs before flipping it. See todo.md, 07/28/26.
     */
    @ConfigProperty(name = "auth.subscription.enddate.enforced", defaultValue = "false")
    boolean subscriptionEndDateEnforced;

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
    private String clientIp(String forwardedFor, HttpServletRequest request) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (request != null && request.getRemoteAddr() != null) {
            return request.getRemoteAddr();
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

    /**
     * Single-key variant of {@link #throttleOrNull}, for callers that cannot check both the
     * account and IP buckets at the same moment. Google sign-in needs this: the request carries
     * only an opaque ID token, so it can throttle on IP up front and on the account only after
     * the CRM has resolved who the user is.
     *
     * @param key     the rate-limiter key to test.
     * @param context label for the log line, e.g. "Google login".
     * @return a 429 Response if that key is locked out, otherwise {@code null}.
     */
    private Response throttleKeyOrNull(String key, String context) {
        long retryAfter = loginRateLimiter.retryAfterSeconds(key);
        if (retryAfter <= 0) {
            return null;
        }
        logger.warn("{} throttled for key '{}', retry after {}s", context, key, retryAfter);
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

    /**
     * Generate the throwaway Keycloak password for a Google sign-in (spec option B).
     *
     * <p>Google users have no ESC password, but Keycloak provisioning and the password grant both
     * need one. This is safe specifically because {@code crm-login} already overwrites the
     * Keycloak password on <em>every</em> login — Keycloak's copy is already a disposable mirror
     * of the CRM's, not a source of truth. Setting it to a random value breaks nothing: the
     * user's next password login overwrites it again from the CRM.
     *
     * <p>Never stored, never logged, never returned to the client. The suffix guarantees the
     * upper/lower/digit/symbol mix in case the realm has a complexity policy.
     */
    private static String throwawayPassword() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "Aa1!";
    }

    /**
     * If the local account is pending deletion (30-day grace), build the login response the
     * app expects: {@code {accountPendingDeletion:true, purgeAfter}} with 200 and NO token.
     * Returns {@code null} when the account is not pending. Only call after the caller has
     * verified the user's credentials.
     */
    private Response pendingDeletionResponseOrNull(String usernameOrEmail) {
        UserRow row = userService.findByUsername(usernameOrEmail)
                .or(() -> userService.findByEmail(usernameOrEmail))
                .orElse(null);
        if (row == null || row.getDeletionRequestedAt() == null) {
            return null;
        }
        logger.info("Login while pending deletion for user {} — returning restore prompt, no token", row.getId());
        Map<String, Object> body = new HashMap<>();
        body.put("accountPendingDeletion", true);
        if (row.getPurgeAfter() != null) {
            body.put("purgeAfter", java.time.Instant.ofEpochMilli(row.getPurgeAfter()).toString());
        }
        return Response.ok(body).build();
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
            @Context HttpServletRequest httpRequest) {
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
            @Context HttpServletRequest httpRequest) {
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

            // Step 1a: Account pending deletion (30-day grace)? Credentials verified above, so
            // it's safe to disclose the state. Return the flag and NO token — the app offers
            // the restore flow. Must run BEFORE the payment check (the CRM registration is
            // REVOKED during grace and would misreport as PAYMENT_REQUIRED) and before any
            // Keycloak password sync / token issuance.
            Response pendingDeletion = pendingDeletionResponseOrNull(username);
            if (pendingDeletion != null) {
                clearLoginFailures(username, clientIp);
                return pendingDeletion;
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


    /**
     * Sign in with Google (iOS app).
     *
     * <p>Sign-in only. Account creation and Google/password account linking stay on the web CRM,
     * as signup and forgot-password already do — the app deep-links there on NO_ACCOUNT and
     * NOT_LINKED rather than showing a dead end.
     *
     * <p>This mirrors {@link #crmLogin} exactly, swapping only the identity proof: instead of
     * asking the CRM "is this password correct?", it asks "whose Google token is this?". Every
     * other guard — deletion grace, registration lookup, payment gate, find-or-create t_user,
     * Keycloak provisioning — is identical, so Google sign-in is no weaker than password sign-in.
     *
     * <p>The token is still issued here, by Keycloak, exactly as today. The CRM never issues a
     * token; it only answers "who is this?".
     *
     * <p>It is a separate endpoint rather than a branch inside {@code crm-login} because the
     * throttle ordering genuinely differs (see step 2), the request body has no username, and the
     * new error codes must not reach already-shipped iOS builds — only a new build calls this.
     */
    @Path("/google-login")
    @POST
    @PermitAll
    public Response googleLogin(GoogleLoginRequest googleLoginRequest,
            @HeaderParam("X-Forwarded-For") String forwardedFor,
            @Context HttpServletRequest httpRequest) {
        String credential = googleLoginRequest != null ? googleLoginRequest.getCredential() : null;
        String clientIp = clientIp(forwardedFor, httpRequest);

        // Step 1: input present. VALIDATION_ERROR's default message ("Email and password are
        // required") is wrong here, so override it.
        if (credential == null || credential.isBlank()) {
            return authError(AuthErrorCode.VALIDATION_ERROR, "A Google credential is required");
        }

        // Step 2: throttle on IP alone — we do not know the account yet. Without this the
        // endpoint is an unthrottled path into the CRM.
        String googleIpKey = LoginRateLimiter.googleIpKey(clientIp);
        Response throttled = throttleKeyOrNull(googleIpKey, "Google login");
        if (throttled != null) {
            return throttled;
        }

        String username = null;
        try {
            // Step 3: identity proof. Note the credential itself is never logged — it is a bearer
            // token and its claims carry the user's email and name in the clear.
            CrmGoogleResult result = crmApiClient.validateGoogleCredential(credential);

            if (!result.isValid()) {
                String code = result.getCode();
                switch (code) {
                    case "INVALID_GOOGLE_TOKEN":
                        // Step 3a: the only case that looks like an attack — a forged, tampered
                        // or replayed token. Count it against the IP bucket.
                        logger.warn("Google login: token failed CRM verification from ip {}", clientIp);
                        loginRateLimiter.recordFailure(googleIpKey, LoginRateLimiter.MAX_ATTEMPTS_PER_IP);
                        return authError(AuthErrorCode.INVALID_CREDENTIALS);

                    case "NO_ACCOUNT":
                        // Step 3b: Google verified them, they just have no ESC account. A real
                        // person mis-clicking, not an attack — do NOT record a failure.
                        logger.info("Google login: no ESC account for the supplied Google identity");
                        return authError(AuthErrorCode.NO_ACCOUNT);

                    case "NOT_LINKED":
                        // Step 3c: an ESC account has this email but Google is not connected.
                        // Surface it — never auto-link. Linking requires the account password,
                        // and that flow lives on the web.
                        logger.info("Google login: account exists but Google is not linked");
                        return authError(AuthErrorCode.NOT_LINKED);

                    case "ACCOUNT_INACTIVE":
                        logger.info("Google login: account is deactivated in the CRM");
                        return authError(AuthErrorCode.ACCOUNT_INACTIVE);

                    default:
                        logger.warn("Google login: unrecognised CRM code '{}'", code);
                        return authError(AuthErrorCode.INVALID_CREDENTIALS);
                }
            }

            // Step 4: identity settled. Everything below is identical to crm-login.
            username = result.getUsername();

            // The CRM contract guarantees a username on 200, but this drives every lookup below,
            // so a malformed body must not become an NPE (prod live-reloads, so a 500 here is a
            // real outage). Treat it as an upstream fault, not a user credential problem.
            if (username == null || username.isBlank()) {
                logger.error("Google login: CRM returned a valid identity with no username");
                return authError(AuthErrorCode.SERVICE_UNAVAILABLE);
            }

            // Now that the account is known, apply the per-account lockout too, so a stolen-token
            // replay against one account is rate-limited the same way a password attack is.
            Response userThrottled = throttleKeyOrNull(LoginRateLimiter.userKey(username), "Google login");
            if (userThrottled != null) {
                return userThrottled;
            }

            // Step 5: pending deletion (30-day grace). Identity is verified above, so it is safe
            // to disclose. Return the flag and NO token. Must run BEFORE the payment check — the
            // registration is REVOKED during grace and would misreport as PAYMENT_REQUIRED.
            //
            // Trust the CRM's flag FIRST, then fall back to the local row. Deletion can be
            // requested on the web CRM, and nothing guarantees t_user.deletion_requested_at was
            // mirrored locally — checking only the local row would issue a token to an account
            // that is mid-grace, which is exactly what this guard exists to stop.
            if (result.isAccountPendingDeletion()) {
                logger.info("Google login: CRM reports account pending deletion for {} — restore prompt, no token", username);
                loginRateLimiter.reset(googleIpKey, LoginRateLimiter.userKey(username));
                Map<String, Object> pendingBody = new HashMap<>();
                pendingBody.put("accountPendingDeletion", true);
                if (result.getPurgeAfter() != null) {
                    pendingBody.put("purgeAfter", java.time.Instant.ofEpochMilli(result.getPurgeAfter()).toString());
                }
                return Response.ok(pendingBody).build();
            }

            Response pendingDeletion = pendingDeletionResponseOrNull(username);
            if (pendingDeletion != null) {
                loginRateLimiter.reset(googleIpKey, LoginRateLimiter.userKey(username));
                return pendingDeletion;
            }

            // Step 6: local t_crm_registration must exist — it is written by the CRM webhook, so
            // a user who signed up with Google on the web is only app-eligible once that webhook
            // has landed. Distinct log line so support can tell this apart from PAYMENT_REQUIRED.
            String email = result.getEmail();
            CrmRegistrationRow registration = crmRegistrationService.findByUsername(username)
                    .or(() -> email != null ? crmRegistrationService.findByEmail(email) : java.util.Optional.empty())
                    .orElse(null);

            if (registration == null) {
                logger.warn("Google login: no t_crm_registration row for user {} — webhook may not have landed", username);
                return authError(AuthErrorCode.REGISTRATION_INCOMPLETE);
            }

            // Step 7: payment gate — unchanged from crm-login. No free access via Google.
            String paymentStatus = registration.getPaymentStatus() != null
                    ? registration.getPaymentStatus().orElse(null) : null;
            boolean hasPaidAccess = paymentStatus != null && (
                    paymentStatus.equalsIgnoreCase("PAID") ||
                    paymentStatus.equalsIgnoreCase("PARTIAL") ||
                    paymentStatus.equalsIgnoreCase("TRIAL") ||
                    paymentStatus.equalsIgnoreCase("TRAIL")); // TRAIL is the stored value for trial

            if (!hasPaidAccess) {
                logger.warn("Google login: payment required for user {} (status: {})", username, paymentStatus);
                return authError(AuthErrorCode.PAYMENT_REQUIRED);
            }

            // Step 8: subscription end date. Gated behind a flag and OFF by default — see the
            // field comment and todo.md (07/28/26). In shadow mode this only logs, so we can
            // measure who it would affect before it locks anyone out.
            Long endDate = registration.getSubscriptionEndDate() != null
                    ? registration.getSubscriptionEndDate().orElse(null) : null;
            if (endDate != null && endDate < System.currentTimeMillis()) {
                if (subscriptionEndDateEnforced) {
                    logger.warn("Google login: subscription expired for user {} (endDate: {})", username, endDate);
                    return authError(AuthErrorCode.SUBSCRIPTION_EXPIRED);
                }
                logger.warn("SUBSCRIPTION_EXPIRED_SHADOW user={} status={} endDate={} — allowed through (enforcement off)",
                        username, paymentStatus, endDate);
            }

            // Step 9: canonical identifiers from the registration row. Generated getters can hand
            // back a null Optional, so null-check before orElse.
            String regUsername = (registration.getUsername() != null
                    ? registration.getUsername().orElse(username) : username).toLowerCase();
            String regEmail = registration.getEmail() != null
                    ? registration.getEmail().orElse(email) : email;

            // The throwaway password covers both Keycloak branches below (new users go through
            // activateUser with .password(...), existing ones through changeUserPassword).
            String password = throwawayPassword();

            // Step 10: find or create local t_user.
            UserRow userRow = userService.findByUsername(regUsername)
                    .or(() -> regEmail != null ? userService.findByEmail(regEmail) : java.util.Optional.empty())
                    .orElse(null);

            if (userRow == null) {
                logger.info("Google login: creating local user for {} (first login)", regUsername);
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

            // Step 11: Keycloak provisioning.
            UUID placeholderUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
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
                logger.info("Google login: provisioning new Keycloak user for {}", regUsername);
                userService.activateUser(keycloakPartial);
            } else {
                logger.info("Google login: setting throwaway Keycloak password for existing user {}", regUsername);
                keycloakProvider.changeUserPassword(userRow.getKeycloakId(), password);

                // A pending required action (VERIFY_EMAIL, UPDATE_PASSWORD) makes Keycloak reject
                // the password grant below with invalid_grant / "Account is not fully set up",
                // even though the reset above succeeded. The new-user path already clears these
                // inside registerUserWithKeycloak; the existing-user path did not, so an account
                // carrying one could never mint a token. Identity is already proven by Google at
                // this point, so an interactive action cannot be satisfied here anyway.
                // Non-fatal, matching how registerUserWithKeycloak treats the same call.
                try {
                    keycloakProvider.clearRequiredActions(userRow.getKeycloakId());
                } catch (Exception clearEx) {
                    logger.warn("Google login: could not clear Keycloak required actions for {} ({})",
                            regUsername, clearEx.getMessage());
                }
            }

            // Step 12: mint the token, exactly as crm-login does.
            OpenIdResponse tokenResponse = keycloakProvider.getUserAccessToken(regUsername, password);

            loginRateLimiter.reset(googleIpKey, LoginRateLimiter.userKey(username));

            // Step 13: same body shape as crm-login, so the app reuses its existing decode path.
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("access_token", tokenResponse.getAccessToken());
            responseBody.put("refresh_token", tokenResponse.getRefreshToken());
            responseBody.put("expires_in", tokenResponse.getExpiresIn());
            responseBody.put("token_type", tokenResponse.getTokenType());
            responseBody.put("roles", tokenResponse.getRole());
            responseBody.put("is_new_user", isNewUser);
            return Response.ok(responseBody).build();

        } catch (IOException e) {
            // CRM unreachable, 5xx, or our own API key rejected. Not a credential problem — do
            // not record a failure, and do not let it read to the app as "bad sign-in".
            logger.error("Google login upstream unavailable (user: {})", username, e);
            return authError(AuthErrorCode.SERVICE_UNAVAILABLE);
        } catch (RegistrationException e) {
            // Keycloak provisioning could not place this identity — e.g. the email already exists
            // in Keycloak under an account we failed to recover. Same 500 the generic handler
            // returned before, but logged distinctly: this is a provisioning fault we can act on,
            // not an unknown crash, and it must not be lost among unrelated failures.
            logger.error("Google login: Keycloak provisioning failed for user {} [{}]",
                    username, e.getMessage(), e);
            return authError(AuthErrorCode.INTERNAL_ERROR);
        } catch (IllegalArgumentException e) {
            logger.warn("Google login Keycloak error for user {}: {}", username, e.getMessage());
            return authError(AuthErrorCode.INTERNAL_ERROR);
        } catch (Exception e) {
            logger.error("Google login failed for user: {} [{}] {}", username, e.getClass().getSimpleName(), e.getMessage(), e);
            return authError(AuthErrorCode.INTERNAL_ERROR);
        }
    }


    @Path("/coach-login")
@POST
@PermitAll
public Response coachLogin(LoginUser loginUser,
        @HeaderParam("X-Forwarded-For") String forwardedFor,
        @Context HttpServletRequest httpRequest) {
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

        // Account pending deletion (30-day grace)? Verify the credentials WITHOUT the usual
        // password sync below (which would overwrite the Keycloak password before verifying),
        // then return {accountPendingDeletion, purgeAfter} with NO token so the app can offer
        // the restore flow.
        UserRow coachUserRow = userService.findByUsername(coach.getUsername()).orElse(null);
        if (coachUserRow != null && coachUserRow.getDeletionRequestedAt() != null) {
            boolean credentialsValid;
            try {
                boolean crmBacked = crmRegistrationService.findByUsername(username)
                        .or(() -> crmRegistrationService.findByEmail(username))
                        .isPresent();
                if (crmBacked) {
                    credentialsValid = crmApiClient.validateUserCredentials(username, password);
                } else {
                    // Local-only coach: the Keycloak user stays enabled during grace precisely
                    // so this password check keeps working.
                    keycloakProvider.getUserAccessToken(coach.getUsername(), password);
                    credentialsValid = true;
                }
            } catch (IllegalArgumentException e) {
                credentialsValid = false;
            } catch (IOException e) {
                logger.error("Coach login: CRM unreachable during pending-deletion check for {}", username, e);
                return authError(AuthErrorCode.SERVICE_UNAVAILABLE);
            }
            if (!credentialsValid) {
                recordLoginFailure(username, clientIp);
                return authError(AuthErrorCode.INVALID_CREDENTIALS);
            }
            clearLoginFailures(username, clientIp);
            return pendingDeletionResponseOrNull(coach.getUsername());
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