package com.lektralabs.thrones.pallbearer.api.account;

import com.lektralabs.thrones.crm.CrmAccountPurgedException;
import com.lektralabs.thrones.crm.CrmApiClient;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.CrmRegistrationService;
import com.lektralabs.thrones.pallbearer.jdbi.service.DeviceTokenService;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Orchestrates the 30-day account-deletion grace period (Apple 5.1.1(v)) on the main backend.
 *
 * <p>Ordering invariant: the CRM leg (Stripe cancel + CRM soft delete) always runs first, and
 * nothing is committed locally if it fails — so any error surfaced to the app means "nothing
 * changed anywhere, retry is safe". Both backends keep their own {@code purge_after} and purge
 * independently; webhooks between them are advisory only.
 *
 * <p>The Keycloak user is never disabled during grace (the restore flow re-verifies the
 * password); only sessions are revoked, and {@link AccountPendingDeletionFilter} bounces any
 * surviving access token with 401 {@code account_pending_deletion}.
 */
@ApplicationScoped
public class AccountDeletionService {

    private static final Logger logger = LoggerFactory.getLogger(AccountDeletionService.class);

    static final long GRACE_PERIOD_MILLIS = 30L * 24 * 60 * 60 * 1000;

    private static final UUID PLACEHOLDER_KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Inject
    CrmApiClient crmApiClient;
    @Inject
    UserService userService;
    @Inject
    CrmRegistrationService crmRegistrationService;
    @Inject
    KeycloakProvider keycloakProvider;
    @Inject
    DeviceTokenService deviceTokenService;

    /** Overridable in tests; production uses the system clock. */
    LongSupplier clock = System::currentTimeMillis;

    /** Outcome of a deletion request; {@code purgeAfter} is epoch millis. */
    public record DeletionResult(long purgeAfter) {
    }

    /**
     * Outcome of a restore; {@code subscriptionRestored} is false when the CRM could not
     * resume the Stripe subscription (e.g. a canceled trial) and the app should prompt
     * the user to re-subscribe.
     */
    public record RestoreResult(boolean subscriptionRestored) {
    }

    /**
     * Starts the grace period for the user. Idempotent: repeating the call while already
     * pending returns the existing {@code purge_after} without touching the CRM again.
     *
     * @throws IOException the CRM leg failed — nothing was committed locally.
     */
    public DeletionResult requestDeletion(UserRow user) throws IOException {
        if (user.getDeletionRequestedAt() != null && user.getPurgeAfter() != null) {
            logger.info("Deletion already pending for user {} (purge after {})", user.getId(), user.getPurgeAfter());
            return new DeletionResult(user.getPurgeAfter());
        }

        // CRM leg first: Stripe cancel + CRM soft delete. Local-only accounts ("coachprime"
        // coaches, no CRM registration) skip it; a CRM 404 for the email is likewise treated
        // as "no CRM leg needed" rather than a failure.
        if (hasCrmAccount(user)) {
            crmApiClient.requestAccountDeletion(user.getEmail());
        }

        long now = clock.getAsLong();
        long purgeAfter = now + GRACE_PERIOD_MILLIS;
        userService.updateDeletionState(user.getId(), now, purgeAfter);
        logger.info("Deletion requested for user {} — purge after {}", user.getId(), purgeAfter);

        // Post-commit hardening, best-effort: the pending-deletion filter is the backstop for
        // any session that survives, so a Keycloak hiccup here must not fail the request
        // (the app has torn down locally once we answer 200).
        revokeSessionsQuietly(user);
        deleteDeviceTokensQuietly(user);

        return new DeletionResult(purgeAfter);
    }

    /**
     * Restores an account inside its grace window. Idempotent: restoring a non-pending
     * account succeeds too. Credential verification is the caller's responsibility.
     *
     * @throws CrmAccountPurgedException the CRM already purged the account (410) — permanent.
     * @throws IOException the CRM leg failed — local flags were left untouched.
     */
    public RestoreResult restore(UserRow user) throws IOException, CrmAccountPurgedException {
        // CRM first, mirroring deletion: if it fails, our flags stay set and the user can retry.
        Boolean subscriptionRestored = null;
        if (hasCrmAccount(user)) {
            subscriptionRestored = crmApiClient.restoreAccount(user.getEmail());
        }

        if (user.getDeletionRequestedAt() != null) {
            userService.updateDeletionState(user.getId(), null, null);
            logger.info("Restored user {} from pending deletion", user.getId());
        }

        // No CRM subscription involved (local-only account or CRM 404) → nothing to re-subscribe.
        return new RestoreResult(subscriptionRestored == null || subscriptionRestored);
    }

    /** CRM-backed accounts have a t_crm_registration row; "coachprime" local coaches don't. */
    public boolean hasCrmAccount(UserRow user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }
        return crmRegistrationService.findByUsername(user.getUsername())
                .or(() -> crmRegistrationService.findByEmail(user.getEmail()))
                .isPresent();
    }

    private void revokeSessionsQuietly(UserRow user) {
        UUID keycloakId = user.getKeycloakId();
        if (keycloakId == null || PLACEHOLDER_KEYCLOAK_ID.equals(keycloakId)) {
            return;
        }
        try {
            keycloakProvider.logoutAllSessions(keycloakId);
        } catch (Exception e) {
            logger.error("Failed to revoke Keycloak sessions for user {} (filter will bounce surviving tokens)",
                    user.getId(), e);
        }
    }

    private void deleteDeviceTokensQuietly(UserRow user) {
        try {
            deviceTokenService.deleteAllByUserId(user.getId());
        } catch (Exception e) {
            logger.error("Failed to delete device tokens for user {}", user.getId(), e);
        }
    }
}
