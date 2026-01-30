package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.crm.CrmIntegration;
import com.lektralabs.thrones.crm.model.CrmRegistration;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;

import java.util.Optional;

@Path("/api/crm_integration")
public class CrmIntegrationResource {

    private static final Logger logger = Logger.getLogger(CrmIntegrationResource.class);

    @Inject
    CrmIntegration crmIntegration;

    @POST
    @Path("/sync")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncRegistrations(
            @QueryParam("lastUpdated") Optional<Long> lastUpdatedTimestamp,
            @Context UriInfo uriInfo) {
        return crmIntegration.syncRegistrations(lastUpdatedTimestamp);
    }

    /**
     * Webhook endpoint for CRM system to register new/updated registrations
     * This endpoint is publicly accessible (no authentication required)
     * The CRM developer should configure their system to POST registration data to this endpoint
     */
    @POST
    @Path("/webhook/register")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response webhookRegister(CrmRegistration registration, @Context UriInfo uriInfo) {
        // Log incoming webhook data
        logger.info("=== WEBHOOK RECEIVED ===");
        logger.info("Webhook URL: " + uriInfo.getRequestUri());
        logger.info("Registration ID: " + registration.getRegistrationId());
        logger.info("Email: " + registration.getEmail());
        logger.info("User ID: " + registration.getUserId());
        logger.info("Username: " + registration.getUsername());
        logger.info("First Name: " + registration.getFirstName());
        logger.info("Last Name: " + registration.getLastName());
        logger.info("Phone Number: " + registration.getPhoneNumber());
        logger.info("Role: " + registration.getRole());
        logger.info("Team ID: " + registration.getTeamId());
        logger.info("Payment Status: " + registration.getPaymentStatus());
        logger.info("Subscription Start Date: " + registration.getSubscriptionStartDate());
        logger.info("Subscription End Date: " + registration.getSubscriptionEndDate());
        logger.info("Last Updated: " + registration.getLastUpdated());
        logger.info("=== END WEBHOOK DATA ===");
        
        return crmIntegration.handleWebhookRegistration(registration);
    }
}
