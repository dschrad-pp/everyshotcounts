package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillPartial;
import com.lektralabs.thrones.pallbearer.api.model.partial.BulkCompleteResponse;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.manager.AthleteDrillItemProgressManager;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillService;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillRow;
import com.lektralabs.thrones.pallbearer.manager.AthleteMetricManager;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Path("/api/drill")
public class DrillResource {

    private static Logger logger = LoggerFactory.getLogger(DrillResource.class);

    @Inject
    DrillService drillService;

    @Inject
    AthleteDrillItemProgressManager athleteDrillItemProgressManager;

    @Inject
    AthleteMetricManager athleteMetricManager;

    @GET
    @Path("/{drillId}")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDrill(@PathParam("drillId") UUID drillId) {
        return drillService.findByIdWithHistory(drillId)
                .map(drillWithHistory -> Response.ok(
                        new GenericApiResponse<>(200, "Drill fetched successfully", drillWithHistory)).build())
                .orElseGet(() -> Response.status(404)
                        .entity(new GenericApiResponse<>(404, "Drill not found", null)).build());
    }

    @POST
    @Path("/")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDrill(DrillPartial drillPartial) {
        try {
            return Response
                    .ok(new GenericApiResponse<>(200, "Drill created successfully", drillService.create(drillPartial)))
                    .build();
        } catch (Exception e) {
            return Response.status(400).entity(new GenericApiResponse<>(400, "Failed to create drill", null)).build();
        }
    }

    @PUT
    @Path("/")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDrill(DrillPartial drillPartial) {
        try {
            drillService.update(drillPartial, true);
            // Return the updated drill with history
            return drillService.findByIdWithHistory(drillPartial.getDrillId().get())
                    .map(drillWithHistory -> Response.ok(
                            new GenericApiResponse<>(200, "Drill updated successfully", drillWithHistory)).build())
                    .orElseGet(() -> Response.status(404)
                            .entity(new GenericApiResponse<>(404, "Drill not found after update", null)).build());
        } catch (Exception e) {
            return Response.status(400)
                    .entity(new GenericApiResponse<>(400, "Failed to update drill", null)).build();
        }
    }

    @PUT
    @Path("/{drillId}/athlete/{athleteUserId}/complete")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response completeDrill(DrillPartial drillPartial,
            @PathParam("drillId") UUID drillId,
            @PathParam("athleteUserId") UUID athleteUserId) {
        try {
            // Add validation logging
            logger.info("🎯 Attempting to complete drill. DrillId={}, AthleteUserId={}, DrillPartial={}",
                    drillId, athleteUserId, drillPartial);

            // Validate required fields
            if (drillPartial == null || drillPartial.getDrillItemId() == null) {
                logger.error("❌ Invalid drill partial: {}", drillPartial);
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "DrillPartial or DrillItemId is null", null))
                        .build();
            }

            int result = athleteDrillItemProgressManager.completeDrill(drillId, athleteUserId, drillPartial);

            // Check if the update actually succeeded
            if (result == 0) {
                logger.error("❌ Drill completion failed - no rows updated. DrillId={}, AthleteUserId={}",
                        drillId, athleteUserId);
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "No drill rows were updated", null))
                        .build();
            }

            athleteMetricManager.updateDrillCompletionMetrics(athleteUserId);

            return drillService.findByIdWithHistory(drillId)
                    .map(drillWithHistory -> {
                        logger.info("✅ Drill completed successfully. DrillId={}, AthleteUserId={}",
                                drillId, athleteUserId);
                        return Response.ok(
                                new GenericApiResponse<>(200,
                                        "Drill updated successfully to {" + drillPartial.getDrillStatus() + "}",
                                        drillWithHistory))
                                .build();
                    })
                    .orElseGet(() -> {
                        logger.error("❌ Drill not found after completion. DrillId={}", drillId);
                        return Response.status(404)
                                .entity(new GenericApiResponse<>(404, "Drill not found after completion", null))
                                .build();
                    });

        } catch (Exception e) {
            // Log the actual exception details
            logger.error("💥 Exception occurred while completing drill. DrillId={}, AthleteUserId={}, Error: {}",
                    drillId, athleteUserId, e.getMessage(), e);

            return Response.status(400)
                    .entity(new GenericApiResponse<>(400, "Failed to complete drill: " + e.getMessage(), null))
                    .build();
        }
    }

    @PUT
    @Path("/bulk/complete")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response completeMultipleDrills(List<DrillPartial> drillPartials) {
        try {
            // Validate input
            if (drillPartials == null || drillPartials.isEmpty()) {
                logger.error("❌ Invalid drill partials list: {}", drillPartials);
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "DrillPartials list is null or empty", null))
                        .build();
            }

            logger.info("🎯 Attempting to complete {} drills in bulk", drillPartials.size());

            List<Object> results = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            // Process each drill completion
            for (int i = 0; i < drillPartials.size(); i++) {
                DrillPartial drillPartial = drillPartials.get(i);

                try {
                    // Validate individual drill partial - drillId is now optional
                    if (drillPartial == null || drillPartial.getDrillItemId() == null) {
                        String error = String
                                .format("Drill at index %d: Invalid drill partial (drillItemId is null)", i);
                        logger.error("❌ {}", error);
                        errors.add(error);
                        failureCount++;
                        continue;
                    }

                    UUID userId = drillPartial.getUserId();
                    UUID drillItemId = drillPartial.getDrillItemId();
                    UUID drillId = drillPartial.getDrillId().orElse(null);

                    if (userId == null) {
                        String error = String.format("Drill at index %d: Missing userId", i);
                        logger.error("❌ {}", error);
                        errors.add(error);
                        failureCount++;
                        continue;
                    }

                    // If drillId is provided, use it; otherwise it will be created by completeDrill
                    if (drillId == null) {
                        logger.info("📝 DrillId not provided for index %d. Will create drill if it doesn't exist. DrillItemId=%s, UserId=%s", 
                                i, drillItemId, userId);
                        // Use a placeholder UUID - completeDrill will find or create the drill
                        drillId = UUID.randomUUID();
                    }

                    // Call the existing complete drill logic - it will create drill if it doesn't exist
                    int result = athleteDrillItemProgressManager.completeDrill(drillId, userId, drillPartial);

                    if (result == 0) {
                        String error = String.format("Drill at index %d: Failed to complete drill for drillItemId=%s, userId=%s", i,
                                drillItemId, userId);
                        logger.error("❌ {}", error);
                        errors.add(error);
                        failureCount++;
                    } else {
                        // Find the actual drill ID (may have been created)
                        Optional<DrillRow> drillRowOpt = drillService.findByDrillItemIdAndUserId(drillItemId, userId);
                        if (drillRowOpt.isPresent()) {
                            UUID actualDrillId = drillRowOpt.get().getId();
                            // Get the updated drill with history
                            drillService.findByIdWithHistory(actualDrillId).ifPresent(drillWithHistory -> {
                                results.add(drillWithHistory);
                            });
                            successCount++;
                            logger.info("✅ Drill completed successfully at index %d. DrillId=%s, DrillItemId=%s, UserId=%s", 
                                    i, actualDrillId, drillItemId, userId);
                        } else {
                            String error = String.format("Drill at index %d: Drill was completed but not found after completion", i);
                            logger.error("❌ {}", error);
                            errors.add(error);
                            failureCount++;
                        }
                    }

                } catch (Exception e) {
                    String error = String.format("Drill at index %d: Exception - %s", i, e.getMessage());
                    logger.error("💥 {}", error, e);
                    errors.add(error);
                    failureCount++;
                }
            }

            // Update metrics for all successful completions
            if (successCount > 0) {
                // Get unique user IDs from successful completions
                drillPartials.stream()
                        .filter(dp -> dp != null && dp.getUserId() != null)
                        .map(DrillPartial::getUserId)
                        .distinct()
                        .forEach(athleteMetricManager::updateDrillCompletionMetrics);
            }

            // Prepare response
            if (failureCount == 0) {
                // All successful
                logger.info("✅ All {} drills completed successfully", successCount);
                return Response.ok(
                        new GenericApiResponse<>(200,
                                String.format("All %d drills completed successfully", successCount),
                                results))
                        .build();
            } else if (successCount == 0) {
                // All failed
                logger.error("❌ All {} drills failed to complete", failureCount);
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400,
                                String.format("All %d drills failed to complete", failureCount),
                                errors))
                        .build();
            } else {
                // Partial success
                logger.warn("⚠️ Partial success: {} drills completed, {} drills failed", successCount, failureCount);
                return Response.status(207) // Multi-Status
                        .entity(new GenericApiResponse<>(207,
                                String.format("Partial success: %d drills completed, %d drills failed", successCount,
                                        failureCount),
                                new BulkCompleteResponse(results, errors, successCount, failureCount)))
                        .build();
            }

        } catch (Exception e) {
            logger.error("💥 Exception occurred while completing multiple drills: {}", e.getMessage(), e);
            return Response.status(500)
                    .entity(new GenericApiResponse<>(500, "Failed to complete multiple drills: " + e.getMessage(),
                            null))
                    .build();
        }
    }

    @PUT
    @Path("/{drillId}/retry")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response retryDrill(@PathParam("drillId") UUID drillId) {
        try {
            drillService.retryDrill(drillId);
            // Return the drill with history after retry
            return drillService.findByIdWithHistory(drillId)
                    .map(drillWithHistory -> Response.ok(
                            new GenericApiResponse<>(200, "Drill retry initiated successfully", drillWithHistory))
                            .build())
                    .orElseGet(() -> Response.status(404)
                            .entity(new GenericApiResponse<>(404, "Drill not found after retry", null)).build());
        } catch (Exception e) {
            return Response.status(400)
                    .entity(new GenericApiResponse<>(400, "Failed to retry drill", null)).build();
        }
    }

    @GET
    @Path("/")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllDrills(@Context UriInfo uriInfo) {
        try {
            return Response.ok(new GenericApiResponse<>(200, "Drills fetched successfully",
                    drillService.findAll(new FindOptions(uriInfo)))).build();
        } catch (Exception e) {
            return Response.status(400).entity(new GenericApiResponse<>(400, "Failed to fetch drills", null)).build();
        }
    }
}
