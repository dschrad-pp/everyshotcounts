package com.lektralabs.thrones.pallbearer.api.account;

import com.lektralabs.thrones.pallbearer.jdbi.model.UserDeletionStateRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Grace-period middleware: while an account is pending deletion, every authenticated request
 * is refused with {@code 401 {code:"account_pending_deletion"}}. This is what cuts off a
 * second logged-in device whose token survived the Keycloak session revocation.
 *
 * <p>Exempt paths:
 * <ul>
 *   <li>{@code api/account/*} — a repeated delete must stay idempotent (200 with the existing
 *       purge date) and restore must remain reachable;</li>
 *   <li>{@code api/sso/*} — login endpoints are unauthenticated and implement their own
 *       pending-deletion response ({@code accountPendingDeletion}/{@code purgeAfter}, no token).</li>
 * </ul>
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class AccountPendingDeletionFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AccountPendingDeletionFilter.class);

    @Inject
    UserService userService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.startsWith("api/account/") || normalized.startsWith("api/sso/")) {
            return;
        }

        Principal principal = requestContext.getSecurityContext() != null
                ? requestContext.getSecurityContext().getUserPrincipal()
                : null;
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return;
        }

        UserDeletionStateRow user;
        try {
            // Deletion timestamps only — this runs on every authenticated request,
            // so it must not drag the user's avatar blob along with it.
            Optional<UserDeletionStateRow> maybeUser = userService
                    .findDeletionStateByUsername(principal.getName());
            if (maybeUser.isEmpty()) {
                return;
            }
            user = maybeUser.get();
        } catch (Exception e) {
            // Never let the grace check take the whole API down.
            logger.error("Pending-deletion check failed for principal '{}'", principal.getName(), e);
            return;
        }

        if (user.getDeletionRequestedAt() == null) {
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("code", "account_pending_deletion");
        if (user.getPurgeAfter() != null) {
            body.put("purgeAfter", Instant.ofEpochMilli(user.getPurgeAfter()).toString());
        }
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build());
    }
}
