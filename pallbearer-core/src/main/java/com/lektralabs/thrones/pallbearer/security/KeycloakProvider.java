package com.lektralabs.thrones.pallbearer.security;

import com.lektralabs.thrones.pallbearer.api.model.keycloak.FindUserResponse;
import com.lektralabs.thrones.pallbearer.api.model.keycloak.OpenIdResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import static com.lektralabs.thrones.pallbearer.common.CoreConstants.GROUP_TO_ID;
import static com.lektralabs.thrones.pallbearer.common.CoreConstants.ROLE_TO_ID;

@ApplicationScoped
public class KeycloakProvider {

    private static Logger logger = LoggerFactory.getLogger(KeycloakProvider.class);

    @ConfigProperty(name = "keycloak.url")
    String keycloakUrl;

    @ConfigProperty(name = "keycloak.realm")
    String keycloakRealm;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    String clientSecret;

    public Keycloak keycloakFactory() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(keycloakRealm)
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .username(KeycloakConstants.ADMIN_USERNAME)
                .password(KeycloakConstants.ADMIN_PASSWORD)
                .build();
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        ResteasyClient client = null;
        try {
            Form form = new Form()
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret)
                    .param("token", token);

            client = new ResteasyClientBuilderImpl().build();
            String introspectUrl = authServerUrl + "/protocol/openid-connect/token/introspect";

            Response response = client.target(introspectUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.form(form));

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                String responseBody = response.readEntity(String.class);
                return responseBody.contains("\"active\":true");
            }
            return false;
        } catch (Exception e) {
            logger.error("Error validating token", e);
            return false;
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    public OpenIdResponse getUserAccessToken(String username, String password) {
        ResteasyClient client = null;
        Response response = null;
        try {
            logger.info("Attempting to get user access token for username: {}", username);
            Form form = new Form();
            form.param(KeycloakConstants.FORM_USERNAME, username)
                    .param(KeycloakConstants.FORM_PASSWORD, password)
                    .param(KeycloakConstants.FORM_GRANT_TYPE, KeycloakConstants.GRANT_TYPE)
                    .param(KeycloakConstants.FORM_CLIENT_ID, clientId)
                    .param("client_secret", clientSecret);

            Entity<Form> entity = Entity.form(form);
            client = new ResteasyClientBuilderImpl().build();
            String tokenUrl = authServerUrl + KeycloakConstants.TOKEN_URL_FRAGMENT;
            logger.debug("Token URL: {}", tokenUrl);
            ResteasyWebTarget target = client.target(tokenUrl);
            response = target.request(MediaType.APPLICATION_JSON).post(entity);

            int statusCode = response.getStatus();
            logger.info("User token request response status: {} (expected: 200)", statusCode);

            if (statusCode != 200) {
                String errorBody = response.hasEntity() ? response.readEntity(String.class) : "No error body";
                logger.error("Failed to get user access token. HTTP Status: {}, Error: {}", statusCode, errorBody);
                
                // Parse error for better diagnostics
                if (errorBody.contains("Account is not fully set up") || 
                    errorBody.contains("invalid_grant")) {
                    logger.error("TROUBLESHOOTING: User account requires setup");
                    logger.error("STEP 1: This error typically means Keycloak has 'Required Actions' set on the user");
                    logger.error("STEP 2: Go to Keycloak Admin Console -> Realm '{}' -> Users -> Find user '{}'", keycloakRealm, username);
                    logger.error("STEP 3: Check 'Required Actions' tab - common actions: VERIFY_EMAIL, UPDATE_PASSWORD");
                    logger.error("STEP 4: Either clear required actions OR configure realm to not require them");
                    logger.error("STEP 5: Realm Settings -> Login -> Disable 'Email Verified' requirement if not needed");
                    logger.error("STEP 6: Realm Settings -> Login -> Disable 'Required Actions' that block login");
                    throw new IllegalArgumentException("User account is not fully set up. Keycloak requires additional actions. Error: " + errorBody);
                } else if (errorBody.contains("invalid_grant")) {
                    logger.error("TROUBLESHOOTING: Invalid grant - check user credentials and account status");
                    logger.error("STEP 1: Verify username '{}' exists in Keycloak realm '{}'", username, keycloakRealm);
                    logger.error("STEP 2: Verify password is correct");
                    logger.error("STEP 3: Check if user account is enabled");
                    logger.error("STEP 4: Check if user account is locked or disabled");
                    throw new IllegalArgumentException("Invalid credentials or account status. Error: " + errorBody);
                } else if (statusCode == 401) {
                    logger.error("TROUBLESHOOTING: 401 Unauthorized - check client credentials");
                    throw new IllegalArgumentException("Authentication failed. Check client credentials. Error: " + errorBody);
                } else {
                    throw new IllegalArgumentException("Failed to get user access token. HTTP " + statusCode + " - " + errorBody);
                }
            }

            OpenIdResponse openIdResponse = response.readEntity(OpenIdResponse.class);
            if (openIdResponse == null || openIdResponse.getAccessToken() == null) {
                logger.error("Token response is null or missing access token");
                throw new IllegalArgumentException("User not found or invalid credentials - token response is empty");
            }

            logger.info("Successfully obtained user access token");
            
            DecodedJWT jwt = JWT.decode(openIdResponse.getAccessToken());
            Claim realmAccessClaim = jwt.getClaim("realm_access");

            if (!realmAccessClaim.isNull()) {
                Map<String, Object> realmAccessMap = realmAccessClaim.asMap();
                Object rolesObj = realmAccessMap.get("roles");

                if (rolesObj instanceof List<?>) {
                    List<?> roleList = (List<?>) rolesObj;

                    // Filter to pick application role
                    for (Object roleObj : roleList) {
                        String roleStr = roleObj.toString();
                        if (!roleStr.equals("offline_access")
                                && !roleStr.equals("uma_authorization")
                                && !roleStr.startsWith("default-roles")) {
                            openIdResponse.setRole(roleStr);
                            logger.debug("Extracted role from token: {}", roleStr);
                            break;
                        }
                    }
                }
            }
            
            logger.info("User access token obtained successfully for username: {}", username);
            return openIdResponse;
        } catch (IllegalArgumentException e) {
            // Re-throw IllegalArgumentException as-is (already has detailed message)
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while getting user access token for username '{}': {}", username, e.getMessage(), e);
            throw new IllegalArgumentException("Error getting user access token: " + e.getMessage(), e);
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }

    public OpenIdResponse getAdminAccessToken() {
        ResteasyClient client = null;
        try {
            logger.info("Attempting to get admin access token for client_id={}, realm={}", clientId, keycloakRealm);
            Form form = new Form()
                    .param(KeycloakConstants.FORM_GRANT_TYPE, "client_credentials")
                    .param(KeycloakConstants.FORM_CLIENT_ID, clientId);
            // If your client is confidential, add client_secret
            if (clientSecret != null && !clientSecret.isEmpty()) {
                form.param("client_secret", clientSecret);
            }
            Entity<Form> entity = Entity.form(form);
            client = new ResteasyClientBuilderImpl().build();
            String tokenUrl = authServerUrl + KeycloakConstants.TOKEN_URL_FRAGMENT;
            logger.info("Token URL: {}", tokenUrl);
            ResteasyWebTarget target = client.target(tokenUrl);
            Response response = target.request(MediaType.APPLICATION_JSON).post(entity);
            if (response.getStatus() != 200) {
                String error = response.readEntity(String.class);
                logger.error("Failed to get admin access token. Status: {}, Error: {}", response.getStatus(), error);
                logger.error("TROUBLESHOOTING: This error usually means:");
                logger.error("1. Service Account is NOT enabled for client '{}' in Keycloak", clientId);
                logger.error("2. Go to Keycloak Admin Console -> Clients -> '{}' -> Settings -> Enable 'Service accounts enabled'", clientId);
                logger.error("3. Then go to 'Service account roles' tab and assign 'realm-management' roles (manage-users, view-users)");
                throw new IllegalArgumentException("Failed to get token. Status: " + response.getStatus() + ", Error: " + error);
            }
            OpenIdResponse tokenResponse = response.readEntity(OpenIdResponse.class);
            logger.info("Successfully obtained admin access token");
            return tokenResponse;
        } catch (Exception e) {
            logger.error("Exception while getting admin access token", e);
            throw new IllegalArgumentException(e);
        } finally {
            if (client != null) {
                client.close();
                logger.info("Closed ResteasyClient");
            }
        }
    }

    // Add this method to your KeycloakProvider class
    public void logoutUser(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }

        try (ResteasyClient client = new ResteasyClientBuilderImpl().build()) {
            // 1. First revoke the token
            Form revokeForm = new Form()
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret)
                    .param("token", accessToken)
                    .param("token_type_hint", "access_token");

            Response revokeResponse = client.target(authServerUrl + "/protocol/openid-connect/revoke")
                    .request()
                    .post(Entity.form(revokeForm));

            if (revokeResponse.getStatus() != 200) {
                String error = revokeResponse.readEntity(String.class);
                logger.warn("Token revocation failed: {}", error);
                // Continue anyway as logout might still work
            }

            // 2. Then perform logout
            Form logoutForm = new Form()
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret);

            Response logoutResponse = client.target(authServerUrl + "/protocol/openid-connect/logout")
                    .request()
                    .header("Authorization", "Bearer " + accessToken)
                    .post(Entity.form(logoutForm));

            if (logoutResponse.getStatus() != 204 && logoutResponse.getStatus() != 200) {
                String error = logoutResponse.readEntity(String.class);
                throw new IllegalStateException("Logout failed with status "
                        + logoutResponse.getStatus() + ": " + error);
            }
        } catch (Exception e) {
            logger.error("Logout process failed", e);
            throw new IllegalStateException("Logout process failed", e);
        }
    }

    public void registerUser(RegisterUserPartial registerUserPartial) {
        ResteasyClient client = null;
        Response response = null;
        try {
            OpenIdResponse securityResponse = getAdminAccessToken();
            client = new ResteasyClientBuilderImpl().build();
            ResteasyWebTarget target = client.target(KeycloakConstants.USER_URL);
            String token = securityResponse.getAccessToken();
            logger.info("Registering user in Keycloak: email={}, username={}, URL={}", 
                registerUserPartial.getEmail(), registerUserPartial.getUsername(), KeycloakConstants.USER_URL);
            target.register((ClientRequestFilter) ctx -> ctx.getHeaders().add(KeycloakConstants.AUTH_HEADER, KeycloakConstants.AUTH_BEARER + token));
            CredentialRepresentation credential = getCredentialRepresentation(registerUserPartial);
            UserRepresentation userRepresentation = getUserRepresentation(registerUserPartial, credential);
            response = target.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRepresentation, MediaType.APPLICATION_JSON));
            
            int statusCode = response.getStatus();
            if (statusCode == 409) {
                String errorBody = getResponseBody(response);
                String errorMsg = String.format("Couldn't create user. Email '%s' or username '%s' is already in use. Keycloak response: %s", 
                    registerUserPartial.getEmail(), registerUserPartial.getUsername(), errorBody);
                logger.warn(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
            if (statusCode == 403) {
                String errorBody = getResponseBody(response);
                String errorMsg = String.format("Couldn't create user. Keycloak returned status 403 (Forbidden). Response: %s", errorBody);
                logger.error(errorMsg);
                logger.error("TROUBLESHOOTING: 403 Forbidden - Service account lacks 'manage-users' permission.");
                logger.error("REALM: You are using '{}' realm. The realm-management roles exist here too.", keycloakRealm);
                logger.error("STEP 1: Go to Keycloak Admin Console -> SELECT '{}' from realm dropdown (top left)", keycloakRealm);
                logger.error("STEP 2: Navigate to Clients -> '{}'", clientId);
                logger.error("STEP 3: Click on 'Service account roles' tab");
                logger.error("STEP 4: In 'Filter by clients' dropdown, type and select 'realm-management'");
                logger.error("STEP 5: Look in 'Available roles' - you should see 'manage-users', 'view-users', etc.");
                logger.error("STEP 6: Select 'manage-users' and click 'Add selected' (>) button");
                logger.error("STEP 7: Also add 'view-users' while you're there");
                logger.error("STEP 8: Verify both roles appear in 'Assigned roles' section");
                logger.error("NOTE: If 'realm-management' doesn't appear in dropdown, check 'Clients' page and ensure it exists in your realm");
                logger.error("ALTERNATIVE: You can also assign realm-level roles via 'Assign role' button (but client roles are preferred)");
                throw new IllegalArgumentException(errorMsg);
            }
            if (statusCode != 201) {
                String errorBody = getResponseBody(response);
                String errorMsg = String.format("Couldn't create user. Keycloak returned status %d. Response: %s", statusCode, errorBody);
                logger.error(errorMsg);
                logger.error("User details: email={}, username={}", registerUserPartial.getEmail(), registerUserPartial.getUsername());
                throw new IllegalArgumentException(errorMsg);
            }
            
            logger.info("User created successfully in Keycloak");
        } catch (IllegalArgumentException e) {
            // Re-throw IllegalArgumentException as-is (already has detailed message)
            throw e;
        } catch (Exception e) {
            String errorDetails = String.format("Error registering user with email '%s' and username '%s': %s", 
                registerUserPartial.getEmail(), registerUserPartial.getUsername(), e.getMessage());
            logger.error(errorDetails, e);
            throw new IllegalArgumentException(errorDetails, e);
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }
    
    /**
     * Clears required actions for a user to prevent "Account is not fully set up" errors
     */
    public void clearRequiredActions(UUID userId) {
        ResteasyClient client = null;
        Response getUserResponse = null;
        Response updateResponse = null;
        try {
            logger.info("Clearing required actions for user: {}", userId);
            OpenIdResponse securityResponse = getAdminAccessToken();
            String adminToken = securityResponse.getAccessToken();
            client = new ResteasyClientBuilderImpl().build();
            
            // Update user to clear required actions - we'll use PUT to update user representation
            String userUpdateUrl = String.format("%s/%s", KeycloakConstants.USER_URL, userId.toString());
            ResteasyWebTarget updateTarget = client.target(userUpdateUrl);
            updateTarget.register((ClientRequestFilter) ctx -> ctx.getHeaders()
                .add(KeycloakConstants.AUTH_HEADER, KeycloakConstants.AUTH_BEARER + adminToken));
            
            // Get current user representation
            getUserResponse = updateTarget.request(MediaType.APPLICATION_JSON).get();
            if (getUserResponse.getStatus() == 200) {
                UserRepresentation userRep = getUserResponse.readEntity(UserRepresentation.class);
                // Clear required actions and ensure email is verified
                userRep.setRequiredActions(List.of());
                userRep.setEmailVerified(true);
                
                // Update user
                updateResponse = updateTarget.request(MediaType.APPLICATION_JSON)
                    .put(Entity.entity(userRep, MediaType.APPLICATION_JSON));
                
                if (updateResponse.getStatus() == 204 || updateResponse.getStatus() == 200) {
                    logger.info("Successfully cleared required actions and verified email for user {}", userId);
                } else {
                    String errorBody = updateResponse.hasEntity() ? updateResponse.readEntity(String.class) : "No error body";
                    logger.warn("Failed to clear required actions for user {}. Status: {}, Error: {}", 
                        userId, updateResponse.getStatus(), errorBody);
                }
            } else {
                String errorBody = getUserResponse.hasEntity() ? getUserResponse.readEntity(String.class) : "No error body";
                logger.warn("Failed to get user representation for user {}. Status: {}, Error: {}", 
                    userId, getUserResponse.getStatus(), errorBody);
            }
        } catch (Exception e) {
            logger.warn("Error clearing required actions for user {}: {}", userId, e.getMessage(), e);
            // Don't throw - this is not critical for user registration
        } finally {
            if (updateResponse != null) {
                updateResponse.close();
            }
            if (getUserResponse != null) {
                getUserResponse.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }
    
    private String getResponseBody(Response response) {
        try {
            if (response != null && response.hasEntity()) {
                return response.readEntity(String.class);
            }
        } catch (Exception e) {
            logger.warn("Could not read response body", e);
        }
        return "No response body available";
    }

    public void changeUserPassword(UUID keycloakId, String password) {

        logger.info("KeycloakProvider: Initiating password change for Keycloak ID={}", keycloakId);

        try (ResteasyClient client = new ResteasyClientBuilderImpl().build()) {
            OpenIdResponse securityResponse = getAdminAccessToken();
            String token = securityResponse.getAccessToken();
            String formattedUrl = KeycloakConstants.PASSWORD_URL.formatted(keycloakId.toString());
            logger.info("Attempting password reset at URL: {}", formattedUrl);
            ResteasyWebTarget target = client.target(formattedUrl);
            target.register((ClientRequestFilter) ctx -> ctx.getHeaders().add(KeycloakConstants.AUTH_HEADER, KeycloakConstants.AUTH_BEARER + token));
            CredentialRepresentation credentialRepresentation = getCredentialRepresentation(password);
            Response response = target.request(MediaType.APPLICATION_JSON)
                    .put(Entity.entity(credentialRepresentation, MediaType.APPLICATION_JSON));
            if (response.getStatus() < 200 || response.getStatus() > 300) {
                String errorBody = response.hasEntity() ? response.readEntity(String.class) : "No error body";
                String errorMsg = "Couldn't reset password for keycloak id [%s]. Response status: %d, Error: %s"
                        .formatted(keycloakId, response.getStatus(), errorBody);
                logger.error(errorMsg);
                if (response.getStatus() == 404) {
                    logger.error("TROUBLESHOOTING: User with Keycloak ID {} not found. Verify the user exists in Keycloak.", keycloakId);
                } else if (response.getStatus() == 400) {
                    logger.error("TROUBLESHOOTING: Bad request - likely password policy violation. Check Keycloak password policy requirements.");
                } else if (response.getStatus() == 403) {
                    logger.error("TROUBLESHOOTING: 403 Forbidden - Service account lacks 'manage-users' permission.");
                    logger.error("REALM: You are using 'thrones_realm' (custom realm). The realm-management roles exist here too.");
                    logger.error("STEP 1: Go to Keycloak Admin Console -> SELECT 'thrones_realm' from realm dropdown (top left)");
                    logger.error("STEP 2: Navigate to Clients -> 'thrones_client'");
                    logger.error("STEP 3: Click on 'Service account roles' tab");
                    logger.error("STEP 4: In 'Filter by clients' dropdown, type and select 'realm-management'");
                    logger.error("STEP 5: Look in 'Available roles' - you should see 'manage-users', 'view-users', etc.");
                    logger.error("STEP 6: Select 'manage-users' and click 'Add selected' (>) button");
                    logger.error("STEP 7: Also add 'view-users' while you're there");
                    logger.error("STEP 8: Verify both roles appear in 'Assigned roles' section");
                    logger.error("NOTE: If 'realm-management' doesn't appear in dropdown, check 'Clients' page and ensure it exists in your realm");
                    logger.error("ALTERNATIVE: You can also assign realm-level roles via 'Assign role' button (but client roles are preferred)");
                }
                throw new IllegalArgumentException(errorMsg);
            }
            logger.info("Successfully reset password for Keycloak ID={}", keycloakId);
        } catch (Exception e) {
            String msg = "KeycloakProvider: Error resetting user password for Keycloak ID=" + keycloakId;
            logger.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

    public void changeUsername(UUID keycloakId, String username) {
        ResteasyClient client = null;
        try {
            OpenIdResponse securityResponse = getAdminAccessToken();
            client = new ResteasyClientBuilderImpl().build();
            ResteasyWebTarget target = client.target(KeycloakConstants.USERNAME_URL.formatted(keycloakId.toString()));
            String token = securityResponse.getAccessToken();
            target.register((ClientRequestFilter) ctx -> ctx.getHeaders().add(KeycloakConstants.AUTH_HEADER, KeycloakConstants.AUTH_BEARER + token));
            UserRepresentation userRepresentation = getUsernameRepresentation(username);
            Response response = target.request(MediaType.APPLICATION_JSON).put(Entity.entity(userRepresentation, MediaType.APPLICATION_JSON));
            if (response.getStatus() < 200 || response.getStatus() > 300) {
                throw new IllegalArgumentException("Couldn't change username for keycloak id [%s].".formatted(keycloakId));
            }

        } catch (Exception e) {
            String msg = "Error changing username";
            logger.warn(msg, e);
            throw new IllegalArgumentException(msg);

        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * Use associateGroupWithUser instead
     *
     * @param userId
     * @param registerUserPartial
     */
    @Deprecated
    public void associateRoleWithUser(UUID userId, RegisterUserPartial registerUserPartial) {
        ResteasyClient client = null;
        try {
            OpenIdResponse securityResponse = getAdminAccessToken();
            client = new ResteasyClientBuilderImpl().build();
            ResteasyWebTarget target = client.target(String.format(KeycloakConstants.ROLE_URL, userId.toString()));
            String token = securityResponse.getAccessToken();
            target.register((ClientRequestFilter) ctx -> ctx.getHeaders().add(KeycloakConstants.AUTH_HEADER, KeycloakConstants.AUTH_BEARER + token));
            List<RoleRepresentation> roleRepresentationList = List.of(getRoleRepresentation(registerUserPartial));
            Response response = target.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(roleRepresentationList, MediaType.APPLICATION_JSON));
            if (response.getStatus() < 200 || response.getStatus() > 300) {
                throw new IllegalArgumentException("Couldn't associate role with user.");
            }
        } catch (Exception e) {
            String msg = "Error associating role with user";
            logger.warn(msg, e);
            throw new IllegalArgumentException(msg);

        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    public RoleRepresentation fetchRoleRepresentationByName(String roleName) {
        ResteasyClient client = null;
        Response response = null;
        try {
            logger.info("Fetching role representation for role name: {}", roleName);
            client = new ResteasyClientBuilderImpl().build();
            OpenIdResponse securityResponse = getAdminAccessToken();
            String token = securityResponse.getAccessToken();

            String encodedRoleName = URLEncoder.encode(roleName, StandardCharsets.UTF_8);
            String url = String.format("%s/%s", KeycloakConstants.REALM_ROLES_BASE_URL, encodedRoleName);
            logger.info("Role fetch URL: {}", url);
            logger.debug("Encoded role name: {} -> {}", roleName, encodedRoleName);

            ResteasyWebTarget target = client.target(url);
            target.register((ClientRequestFilter) ctx -> ctx.getHeaders()
                    .add(KeycloakConstants.AUTH_HEADER, KeycloakConstants.AUTH_BEARER + token));

            response = target.request(MediaType.APPLICATION_JSON).get();
            int statusCode = response.getStatus();
            logger.info("Role fetch response status: {} for role: {}", statusCode, roleName);
            
            if (statusCode != 200) {
                String error = response.hasEntity() ? response.readEntity(String.class) : "No error body available";
                logger.error("Failed to fetch role [{}] from Keycloak. HTTP Status: {}, Error: {}", roleName, statusCode, error);
                logger.error("TROUBLESHOOTING: Role fetch failed for role '{}'", roleName);
                logger.error("STEP 1: Verify the role exists in Keycloak realm '{}'", keycloakRealm);
                logger.error("STEP 2: Go to Keycloak Admin Console -> Realm '{}' -> Roles -> Check if '{}' role exists", keycloakRealm, roleName);
                logger.error("STEP 3: Ensure it's a REALM role (not a client role)");
                logger.error("STEP 4: If role doesn't exist, create it: Realm '{}' -> Roles -> Add role -> Name: '{}'", keycloakRealm, roleName);
                if (statusCode == 404) {
                    logger.error("STEP 5: 404 Not Found - The role '{}' does not exist in realm '{}'", roleName, keycloakRealm);
                } else if (statusCode == 403) {
                    logger.error("STEP 5: 403 Forbidden - Service account lacks 'view-realm' or 'view-users' permission");
                    logger.error("STEP 6: Go to Clients -> '{}' -> Service account roles -> Filter by 'realm-management'", clientId);
                    logger.error("STEP 7: Add 'view-realm' role to service account");
                } else if (statusCode == 401) {
                    logger.error("STEP 5: 401 Unauthorized - Admin token may have expired or is invalid");
                }
                throw new RuntimeException("Failed to fetch role [" + roleName + "]: HTTP " + statusCode + " - " + error);
            }

            RoleRepresentation roleRep = response.readEntity(RoleRepresentation.class);
            if (roleRep == null) {
                logger.error("Role representation is null for role: {}", roleName);
                throw new RuntimeException("Role representation is null for role: " + roleName);
            }
            logger.info("Successfully fetched role '{}' with ID: {}", roleName, roleRep.getId());
            return roleRep;
        } catch (RuntimeException e) {
            // Re-throw RuntimeException as-is (already has detailed message)
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while fetching role '{}': {}", roleName, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch role [" + roleName + "]: " + e.getMessage(), e);
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }

    public void associateGroupWithUser(UUID userId, RegisterUserPartial registerUserPartial) {
        try (ResteasyClient client = new ResteasyClientBuilderImpl().build()) {
            OpenIdResponse securityResponse = getAdminAccessToken();
            UUID groupId = getGroupId(registerUserPartial);
            ResteasyWebTarget target = client.target(String.format(KeycloakConstants.GROUP_URL, userId.toString(), groupId.toString()));
            String token = securityResponse.getAccessToken();
            target.register((ClientRequestFilter) ctx -> ctx.getHeaders().add(KeycloakConstants.AUTH_HEADER, KeycloakConstants.AUTH_BEARER + token));
            Response response = target.request(MediaType.APPLICATION_JSON).put(Entity.entity("", MediaType.APPLICATION_JSON));

            if (response.getStatus() < 200 || response.getStatus() > 300) {
                String errorResponse = response.readEntity(String.class);
                logger.error("Failed to associate group with user. Status: {}, Response: {}", response.getStatus(), errorResponse);
                throw new IllegalArgumentException("Couldn't associate group with user. Status: " + response.getStatus());
            }
        } catch (Exception e) {
            String msg = "Error associating group with user";
            logger.warn(msg, e);
            throw new IllegalArgumentException(msg);
        }
    }

    public FindUserResponse findUser(RegisterUserPartial registerUserPartial) {
        ResteasyClient client = null;
        try {
            OpenIdResponse securityResponse = getAdminAccessToken();
            client = new ResteasyClientBuilderImpl().build();
            String url = String.format("%s?username=%s", KeycloakConstants.USER_URL,
                    URLEncoder.encode(registerUserPartial.getUsername(), "UTF-8"));
            ResteasyWebTarget target = client.target(url);
            String token = securityResponse.getAccessToken();
            target.register((ClientRequestFilter) ctx -> ctx.getHeaders().add(KeycloakConstants.AUTH_HEADER, KeycloakConstants.AUTH_BEARER + token));
            Response response = target.request(MediaType.APPLICATION_JSON).get();
            List<FindUserResponse> users = response.readEntity(new GenericType<List<FindUserResponse>>() {
            });
            if (response.getStatus() < 200 || response.getStatus() > 300 || users.size() < 1) {
                throw new IllegalArgumentException("Could not find user " + registerUserPartial.getUsername());
            } else {
                return users.get(0);
            }
        } catch (Exception e) {
            String msg = "Error finding user";
            logger.warn(msg, e);
            throw new IllegalArgumentException(msg);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    public FindUserResponse findUserByEmail(String email) {
        ResteasyClient client = null;
        try {
            OpenIdResponse securityResponse = getAdminAccessToken();
            client = new ResteasyClientBuilderImpl().build();
            String url = String.format("%s?email=%s", KeycloakConstants.USER_URL, URLEncoder.encode(email, "UTF-8"));
            ResteasyWebTarget target = client.target(url);
            String token = securityResponse.getAccessToken();
            target.register((ClientRequestFilter) ctx -> ctx.getHeaders().add(KeycloakConstants.AUTH_HEADER, KeycloakConstants.AUTH_BEARER + token));
            Response response = target.request(MediaType.APPLICATION_JSON).get();
            List<FindUserResponse> users = response.readEntity(new GenericType<List<FindUserResponse>>() {
            });
            if (response.getStatus() < 200 || response.getStatus() > 300 || users.size() < 1) {
                throw new IllegalArgumentException("Could not find user with email " + email);
            } else {
                return users.get(0);
            }
        } catch (Exception e) {
            String msg = "Error finding user by email";
            logger.warn(msg, e);
            throw new IllegalArgumentException(msg);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    public void assignRoleToUser(UUID userId, String roleName) {
        ResteasyClient client = null;
        Response response = null;
        try {
            logger.info("Starting role assignment: user={}, role={}", userId, roleName);
            client = new ResteasyClientBuilderImpl().build();
            OpenIdResponse securityResponse = getAdminAccessToken();
            String token = securityResponse.getAccessToken();

            // 1. Fetch the complete RoleRepresentation using its name to get the ID
            logger.info("Step 1: Fetching role representation for role: {}", roleName);
            RoleRepresentation fullRoleRep = fetchRoleRepresentationByName(roleName);
            if (fullRoleRep == null || fullRoleRep.getId() == null) {
                logger.error("Role representation is null or missing ID for role: {}", roleName);
                throw new RuntimeException("Could not find role '" + roleName + "' by name or its ID is missing.");
            }
            logger.info("Step 1 completed: Found role '{}' with ID: {}", roleName, fullRoleRep.getId());

            String roleUrl = String.format("%s/%s/role-mappings/realm",
                    KeycloakConstants.USER_URL, userId.toString());
            logger.info("Step 2: Assigning role to user. Role assignment URL: {}", roleUrl);
            logger.debug("Role details - Name: {}, ID: {}", fullRoleRep.getName(), fullRoleRep.getId());

            ResteasyWebTarget target = client.target(roleUrl);
            target.register((ClientRequestFilter) ctx
                    -> ctx.getHeaders().add(KeycloakConstants.AUTH_HEADER, KeycloakConstants.AUTH_BEARER + token));

            // 2. Use the fetched fullRoleRep (which now has the ID) for the POST request
            response = target.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(List.of(fullRoleRep), MediaType.APPLICATION_JSON));

            int statusCode = response.getStatus();
            logger.info("Step 2 completed: Role assignment response status: {} (expected: 204)", statusCode);

            if (statusCode != Response.Status.NO_CONTENT.getStatusCode()) {
                String errorBody = response.hasEntity() ? response.readEntity(String.class) : "No error body";
                logger.error("Role assignment failed for user {} with role '{}': HTTP Status: {}, Error: {}", 
                    userId, roleName, statusCode, errorBody);
                logger.error("TROUBLESHOOTING: Role assignment failed");
                logger.error("STEP 1: Verify user exists: User ID: {}", userId);
                logger.error("STEP 2: Verify role exists: Role Name: '{}', Role ID: {}", roleName, fullRoleRep.getId());
                logger.error("STEP 3: Check service account permissions - needs 'manage-users' role");
                if (statusCode == 404) {
                    logger.error("STEP 4: 404 Not Found - User {} may not exist in Keycloak", userId);
                } else if (statusCode == 403) {
                    logger.error("STEP 4: 403 Forbidden - Service account lacks 'manage-users' permission");
                    logger.error("STEP 5: Go to Clients -> '{}' -> Service account roles -> Add 'manage-users' from 'realm-management'", clientId);
                } else if (statusCode == 400) {
                    logger.error("STEP 4: 400 Bad Request - Role may already be assigned or invalid role representation");
                }
                throw new RuntimeException("Role assignment failed: HTTP " + statusCode + " - " + errorBody);
            }
            
            logger.info("Successfully assigned role '{}' to user {}", roleName, userId);
        } catch (RuntimeException e) {
            // Re-throw RuntimeException as-is (already has detailed message)
            logger.error("RuntimeException during role assignment for user {} with role '{}': {}", 
                userId, roleName, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during role assignment for user {} with role '{}': {}", 
                userId, roleName, e.getMessage(), e);
            throw new RuntimeException("Role assignment failed for user " + userId + " with role " + roleName, e);
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
                logger.debug("Closed ResteasyClient for role assignment");
            }
        }
    }

    private CredentialRepresentation getCredentialRepresentation(RegisterUserPartial registerUserPartial) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(registerUserPartial.getPassword());
        return credential;
    }

    private UserRepresentation getUserRepresentation(RegisterUserPartial registerUserPartial, CredentialRepresentation credential) {
        UserRepresentation userRepresentation = new UserRepresentation();
        // Username is stored in lowercase for consistency (both in database and Keycloak)
        String username = registerUserPartial.getUsername() != null ? registerUserPartial.getUsername().toLowerCase() : null;
        userRepresentation.setUsername(username);
        userRepresentation.setFirstName(registerUserPartial.getFirstName());
        userRepresentation.setLastName(registerUserPartial.getLastName());
        userRepresentation.setEmail(registerUserPartial.getEmail());
        userRepresentation.setCredentials(Arrays.asList(credential));
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true); // Mark email as verified to avoid "Account is not fully set up" error
        userRepresentation.setRealmRoles(Arrays.asList(registerUserPartial.getRole()));
        return userRepresentation;
    }

    private UserRepresentation getUsernameRepresentation(String username) {
        UserRepresentation userRepresentation = new UserRepresentation();
        // Username is stored in lowercase for consistency
        String lowercasedUsername = username != null ? username.toLowerCase() : null;
        userRepresentation.setUsername(lowercasedUsername);
        return userRepresentation;
    }

    private RoleRepresentation getRoleRepresentation(RegisterUserPartial registerUserPartial) {
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setId(ROLE_TO_ID.get(registerUserPartial.getRole()).toString());
        roleRepresentation.setName(registerUserPartial.getRole());
        return roleRepresentation;
    }

    private UUID getGroupId(RegisterUserPartial registerUserPartial) {
        return GROUP_TO_ID.get(registerUserPartial.getRole());
    }

    private CredentialRepresentation getCredentialRepresentation(String password) {
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType("password");
        credentialRepresentation.setTemporary(false);
        credentialRepresentation.setValue(password);
        return credentialRepresentation;
    }

}
