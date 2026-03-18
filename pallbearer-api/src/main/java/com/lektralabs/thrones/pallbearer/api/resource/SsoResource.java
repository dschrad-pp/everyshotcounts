package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.keycloak.OpenIdResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.LoginUser;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;
import com.lektralabs.thrones.crm.CrmApiClient;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
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
import com.lektralabs.thrones.pallbearer.jdbi.exception.RegistrationException;

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
            // Validate Authorization header
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

            // Perform logout
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
            boolean crmValid = crmApiClient.validateUserCredentials(username, password);
            if (!crmValid) {
                logger.warn("CRM credential validation failed for user: {}", username);
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new GenericApiResponse<>(401, "Invalid credentials", null))
                        .build();
            }
    
            UserRow userRow = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found in system after CRM validation: " + username));
    
            // FIX: treat placeholder UUID the same as null — user not yet in Keycloak
            java.util.UUID placeholderUuid = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
            boolean isNewUser = (userRow.getKeycloakId() == null || placeholderUuid.equals(userRow.getKeycloakId()));
    
            if (isNewUser) {
                logger.info("CRM login: provisioning new Keycloak user for {}", username);
                RegisterUserPartial partial = RegisterUserPartial.builder()
                        .username(username.toLowerCase())
                        .password(password)
                        .email(userRow.getEmail())
                        .firstName("")
                        .lastName("")
                        .phoneNumber("")
                        .birthDate(0L)
                        .role("ATHLETE")
                        .build();
                // try {
                //     userService.activateUser(partial);
                // } catch (RegistrationException e) {
                //     // User was already activated in a previous attempt — safe to continue
                //     logger.warn("CRM login: activation skipped for {} (already activated): {}", username, e.getMessage());
                // }
                try {
                userService.activateUser(partial);
                    } catch (Exception e) {
                        logger.warn("CRM login: activation issue for {} (will attempt token anyway): {}", username, e.getMessage());
                    }
            } else {
                logger.info("CRM login: syncing Keycloak password for existing user {}", username);
                keycloakProvider.changeUserPassword(userRow.getKeycloakId(), password);
            }
    
            OpenIdResponse tokenResponse = keycloakProvider.getUserAccessToken(username, password);
    
            java.util.Map<String, Object> responseBody = new java.util.HashMap<>();
            responseBody.put("access_token", tokenResponse.getAccessToken());
            responseBody.put("refresh_token", tokenResponse.getRefreshToken());
            responseBody.put("expires_in", tokenResponse.getExpiresIn());
            responseBody.put("token_type", tokenResponse.getTokenType());
            responseBody.put("roles", tokenResponse.getRole());   // FIX: was "role", iOS expects "roles"
            responseBody.put("is_new_user", isNewUser);
            return Response.ok(responseBody).build();
    
        } catch (IllegalArgumentException e) {
            // Keycloak rejected the token request (required actions, bad password sync, etc.)
            logger.warn("CRM login Keycloak error for user {}: {}", username, e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new GenericApiResponse<>(401, "Authentication failed: " + e.getMessage(), null))
                    .build();
        } catch (IllegalStateException e) {
            logger.warn("CRM login user not found: {}", e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new GenericApiResponse<>(401, "User not registered in system", null))
                    .build();
        } catch (Exception e) {
            logger.error("CRM login failed for user: {}", username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(500, "Login failed", null))
                    .build();
        }
    }
}
