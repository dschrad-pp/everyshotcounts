package com.lektralabs.thrones.crm;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Scheduled service to automatically sync CRM registrations.
 * The schedule can be configured via application.properties using:
 * - crm.sync.enabled (default: true)
 * - crm.sync.cron (default: every 20 seconds)
 */
@ApplicationScoped
public class CrmSyncScheduler {
    private static final Logger logger = Logger.getLogger(CrmSyncScheduler.class);

    @Inject
    CrmIntegration crmIntegration;

    @Inject
    Config config;

    @ConfigProperty(name = "crm.sync.cron", defaultValue = "*/20 * * * * ?")
    String syncCron;

    /**
     * Automatically sync CRM registrations on a scheduled basis.
     * Default schedule: Every 20 seconds.
     * Cron format: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "{crm.sync.cron}", identity = "crm-sync-scheduler")
    void syncRegistrations() {
        // Read config dynamically at runtime to support configuration changes without restart
        boolean syncEnabled = config.getOptionalValue("crm.sync.enabled", Boolean.class)
                .orElse(true);
        
        if (!syncEnabled) {
            logger.debug("CRM sync is disabled. Skipping scheduled sync.");
            return;
        }

        try {
            logger.info("Starting scheduled CRM sync...");
            jakarta.ws.rs.core.Response response = crmIntegration.syncRegistrations(Optional.empty());
            
            if (response.getStatus() == jakarta.ws.rs.core.Response.Status.OK.getStatusCode()) {
                logger.info("Scheduled CRM sync completed successfully");
            } else {
                logger.warn("Scheduled CRM sync completed with status: " + response.getStatus());
            }
        } catch (Exception e) {
            logger.error("Error during scheduled CRM sync", e);
        }
    }
}
