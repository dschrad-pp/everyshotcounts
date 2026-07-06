package com.lektralabs.thrones.pallbearer.api.account;

import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.DeviceTokenService;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Daily purge of accounts whose 30-day deletion grace period has expired: delete the Keycloak
 * user, the device push tokens, and every local row (drills, media links, team membership, …)
 * via {@link UserService#deleteUserWithAllRelatedData}.
 *
 * <p><b>Disabled by default.</b> Rollout step "enable purge crons in prod" flips
 * {@code account.purge.enabled=true}; until then the schedule fires but exits immediately, so
 * deploying this code changes nothing in production. The CRM purges on its own clock — a lost
 * webhook never strands data on either side.
 *
 * <p>Each user is purged independently and failures are logged and skipped, so a crash mid-run
 * simply leaves the remaining users for the next day's re-select (crash-resumable).
 */
@ApplicationScoped
public class AccountPurgeJob {

    private static final Logger logger = Logger.getLogger(AccountPurgeJob.class);

    private static final UUID PLACEHOLDER_KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Inject
    UserService userService;
    @Inject
    KeycloakProvider keycloakProvider;
    @Inject
    DeviceTokenService deviceTokenService;
    @Inject
    Config config;

    // Cron is hardcoded on purpose: a "{property}" expression here requires the property to
    // exist in the box's application.properties and kills startup when it doesn't (the field
    // defaultValue does not apply to the annotation). Daily 04:30; the real gate is the
    // account.purge.enabled flag below.
    @Scheduled(cron = "0 30 4 * * ?", identity = "account-purge")
    void purgeExpiredAccounts() {
        // Read at runtime (same pattern as crm.sync.enabled) so prod can flip the flag
        // without a code change. Defaults to DISABLED.
        boolean enabled = config.getOptionalValue("account.purge.enabled", Boolean.class).orElse(false);
        if (!enabled) {
            logger.debug("Account purge is disabled (account.purge.enabled=false). Skipping.");
            return;
        }
        runPurge(System.currentTimeMillis());
    }

    /** Separated from the schedule trigger so it can be invoked directly (tests, staging override). */
    void runPurge(long now) {
        List<UserRow> due = userService.findPendingPurge(now);
        if (due.isEmpty()) {
            logger.debug("Account purge: nothing to purge.");
            return;
        }
        logger.infof("Account purge: %d account(s) past their grace period", due.size());

        for (UserRow user : due) {
            try {
                purgeUser(user);
            } catch (Exception e) {
                // Leave the row in place; tomorrow's run re-selects it.
                logger.errorf(e, "Account purge failed for user %s — will retry on next run", user.getId());
            }
        }
    }

    private void purgeUser(UserRow user) {
        logger.infof("Purging account %s (deletion requested at %s)", user.getId(), user.getDeletionRequestedAt());

        // Keycloak first: if this fails we abort before touching local rows, so the account
        // stays selectable for the next run. deleteUser treats 404 as success (idempotent).
        UUID keycloakId = user.getKeycloakId();
        if (keycloakId != null && !PLACEHOLDER_KEYCLOAK_ID.equals(keycloakId)) {
            keycloakProvider.deleteUser(keycloakId);
        }

        deviceTokenService.deleteAllByUserId(user.getId());
        userService.deleteUserWithAllRelatedData(user.getId());
        logger.infof("Purged account %s", user.getId());
    }
}
