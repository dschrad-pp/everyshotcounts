package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.keycloak.OpenIdResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.LoginUser;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;
import com.lektralabs.thrones.crm.CrmApiClient;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.jdbi.service.CrmRegistrationService;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.CrmRegistrationRow;
import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;
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
            String regUsername = registration.getUsername().orElse(username).toLowerCase();
            String regEmail    = registration.getEmail().orElse(null);

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
                        .firstName(registration.getFirstName().orElse(""))
                        .lastName(registration.getLastName().orElse(""))
                        .phoneNumber(registration.getPhoneNumber().orElse(""))
                        .birthDate(0L)
                        .role(registration.getRole().orElse("ATHLETE"))
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
                    .firstName(registration.getFirstName().orElse(""))
                    .lastName(registration.getLastName().orElse(""))
                    .phoneNumber(registration.getPhoneNumber().orElse(""))
                    .birthDate(0L)
                    .role(registration.getRole().orElse("ATHLETE"))
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
            logger.error("CRM login failed for user: {}", username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(500, "Login failed", null))
                    .build();
        }
    }
}