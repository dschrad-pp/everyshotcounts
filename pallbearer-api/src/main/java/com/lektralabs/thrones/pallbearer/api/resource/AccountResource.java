package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.crm.CrmAccountPurgedException;
import com.lektralabs.thrones.crm.CrmApiClient;
import com.lektralabs.thrones.pallbearer.api.account.AccountDeletionService;
import com.lektralabs.thrones.pallbearer.api.auth.AuthErrorCode;
import com.lektralabs.thrones.pallbearer.api.auth.LoginRateLimiter;
import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.LoginUser;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * In-app account deletion with a 30-day grace period (Apple 5.1.1(v)).
 *
 * <p>The iOS app is built against these exact shapes — do not change field names or statuses:
 * <ul>
 *   <li>{@code POST api/account/delete} → 200 {@code {status:"deletion_pending", purgeAfter}},
 *       502 {@code {error:"crm_unavailable"}} when the CRM leg fails (nothing committed);</li>
 *   <li>{@code POST api/account/restore} → 200 {@code {status:"restored", subscriptionRestored}},
 *       401 invalid credentials, 410 {@code {code:"account_purged"}} after the grace window.</li>
 * </ul>
 */
@Path("/api/account")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    private static final Logger logger = LoggerFactory.getLogger(AccountResource.class);

    @Inject
    AccountDeletionService accountDeletionService;
    @Inject
    UserService userService;
    @Inject
    CrmApiClient crmApiClient;
    @Inject
    KeycloakProvider keycloakProvider;
    @Inject
    LoginRateLimiter loginRateLimiter;

    @Path("/delete")
    @POST
    @Authenticated
    public Response delete(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal() != null
                ? securityContext.getUserPrincipal().getName()
                : null;
        if (username == null || username.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new GenericApiResponse<>(401, "No authenticated user", null))
                    .build();
        }

        UserRow user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            logger.warn("Account delete: no local user for authenticated principal '{}'", username);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new GenericApiResponse<>(404, "User not found", null))
                    .build();
        }

        try {
            AccountDeletionService.DeletionResult result = accountDeletionService.requestDeletion(user);
            Map<String, Object> body = new HashMap<>();
            body.put("status", "deletion_pending");
            body.put("purgeAfter", Instant.ofEpochMilli(result.purgeAfter()).toString());
            return Response.ok(body).build();
        } catch (IOException e) {
            // CRM leg failed → nothing committed anywhere; the app may retry safely.
            logger.error("Account delete: CRM unavailable for user {}", user.getId(), e);
            return Response.status(502)
                    .entity(Map.of("error", "crm_unavailable"))
                    .build();
        } catch (Exception e) {
            logger.error("Account delete failed for user {}", user.getId(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(500, "Account deletion failed", null))
                    .build();
        }
    }

    @Path("/restore")
    @POST
    @PermitAll
    public Response restore(LoginUser loginUser,
            @HeaderParam("X-Forwarded-For") String forwardedFor,
            @Context HttpServletRequest httpRequest) {
        String username = loginUser != null ? loginUser.getUsername() : null;
        String password = loginUser != null ? loginUser.getPassword() : null;
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return authError(AuthErrorCode.VALIDATION_ERROR);
        }

        // Credential-bearing endpoint → same brute-force protection as the login endpoints.
        String clientIp = clientIp(forwardedFor, httpRequest);
        long retryAfter = loginRateLimiter.retryAfterSeconds(
                LoginRateLimiter.userKey(username), LoginRateLimiter.ipKey(clientIp));
        if (retryAfter > 0) {
            return Response.status(AuthErrorCode.TOO_MANY_ATTEMPTS.httpStatus())
                    .header("Retry-After", retryAfter)
                    .entity(new GenericApiResponse<>(
                            AuthErrorCode.TOO_MANY_ATTEMPTS.httpStatus(),
                            AuthErrorCode.TOO_MANY_ATTEMPTS.name(),
                            AuthErrorCode.TOO_MANY_ATTEMPTS.defaultMessage(),
                            null))
                    .build();
        }

        UserRow user = userService.findByUsername(username)
                .or(() -> userService.findByEmail(username))
                .orElse(null);
        if (user == null) {
            // Unknown here also covers "already purged locally" — indistinguishable from bad
            // credentials on purpose (anti-enumeration); the app shows a credentials error.
            recordLoginFailure(username, clientIp);
            return authError(AuthErrorCode.INVALID_CREDENTIALS);
        }

        try {
            // Verify the just-entered credentials. CRM-backed users verify against the CRM
            // (whose password check keeps working during grace); local-only coaches verify
            // against Keycloak (never disabled during grace for exactly this reason).
            if (!verifyCredentials(user, username, password)) {
                recordLoginFailure(username, clientIp);
                return authError(AuthErrorCode.INVALID_CREDENTIALS);
            }

            AccountDeletionService.RestoreResult result = accountDeletionService.restore(user);
            loginRateLimiter.reset(LoginRateLimiter.userKey(username), LoginRateLimiter.ipKey(clientIp));

            Map<String, Object> body = new HashMap<>();
            body.put("status", "restored");
            body.put("subscriptionRestored", result.subscriptionRestored());
            return Response.ok(body).build();
        } catch (CrmAccountPurgedException e) {
            return Response.status(410)
                    .entity(Map.of("code", "account_purged"))
                    .build();
        } catch (IOException e) {
            logger.error("Account restore: CRM unavailable for user {}", user.getId(), e);
            return Response.status(502)
                    .entity(Map.of("error", "crm_unavailable"))
                    .build();
        } catch (Exception e) {
            logger.error("Account restore failed for user {}", user.getId(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(500, "Account restore failed", null))
                    .build();
        }
    }

    private boolean verifyCredentials(UserRow user, String username, String password) throws IOException {
        if (accountDeletionService.hasCrmAccount(user)) {
            return crmApiClient.validateUserCredentials(username, password);
        }
        try {
            keycloakProvider.getUserAccessToken(user.getUsername(), password);
            return true;
        } catch (IllegalArgumentException e) {
            logger.info("Restore: Keycloak rejected credentials for user {}", user.getId());
            return false;
        }
    }

    private Response authError(AuthErrorCode code) {
        return Response.status(code.httpStatus())
                .entity(new GenericApiResponse<>(code.httpStatus(), code.name(), code.defaultMessage(), null))
                .build();
    }

    private String clientIp(String forwardedFor, HttpServletRequest request) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (request != null && request.getRemoteAddr() != null) {
            return request.getRemoteAddr();
        }
        return "unknown";
    }

    private void recordLoginFailure(String username, String clientIp) {
        loginRateLimiter.recordFailure(LoginRateLimiter.userKey(username), LoginRateLimiter.MAX_ATTEMPTS_PER_USER);
        loginRateLimiter.recordFailure(LoginRateLimiter.ipKey(clientIp), LoginRateLimiter.MAX_ATTEMPTS_PER_IP);
    }
}
