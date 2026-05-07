package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillPartial;
import com.lektralabs.thrones.pallbearer.api.model.partial.BulkCompleteResponse;
import com.lektralabs.thrones.pallbearer.api.model.request.GroupLevelCompleteRequest;
import com.lektralabs.thrones.pallbearer.api.model.request.UpdateDrillMediaRequest;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.manager.AthleteDrillItemProgressManager;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillService;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.manager.AthleteMetricManager;
import com.lektralabs.thrones.pallbearer.manager.utils.AthleteManagerUtils;
import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;
import com.lektralabs.thrones.pallbearer.security.CurrentUserUtils;
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

    @Inject
    CurrentUserUtils currentUserUtils;

    @Inject
    AthleteDrillService athleteDrillService;

    @Inject
    AthleteManagerUtils athleteManagerUtils;

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
    @Path("/group-level/complete")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response completeDrillsByGroupAndLevel(GroupLevelCompleteRequest request) {
        try {
            // Validate input
            if (request == null) {
                logger.error("❌ Invalid request: request is null");
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "Request is null", null))
                        .build();
            }

            String groupName = request.getGroupName();
            if (groupName == null || groupName.trim().isEmpty()) {
                logger.error("❌ Invalid group name: {}", groupName);
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "Group name is required", null))
                        .build();
            }

            // Validate that either levelIndex or orderIndex is provided, but not both
            if (request.getLevelIndex() == null && request.getOrderIndex() == null) {
                logger.error("❌ Either levelIndex or orderIndex must be provided");
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "Either levelIndex or orderIndex must be provided", null))
                        .build();
            }

            if (request.getLevelIndex() != null && request.getOrderIndex() != null) {
                logger.error("❌ Both levelIndex and orderIndex cannot be provided at the same time");
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "Both levelIndex and orderIndex cannot be provided at the same time", null))
                        .build();
            }

            // Extract userId from token
            UUID userId;
            try {
                userId = currentUserUtils.getCurrentUserId();
                logger.info("🎯 Extracted userId from token: {}", userId);
            } catch (Exception e) {
                logger.error("❌ Failed to extract userId from token: {}", e.getMessage());
                return Response.status(401)
                        .entity(new GenericApiResponse<>(401, "Failed to extract user ID from token: " + e.getMessage(), null))
                        .build();
            }

            // Map group name to UUID
            UUID drillGroupId;
            switch (groupName.toUpperCase()) {
                case "BEGINNER":
                    drillGroupId = DrillGroupConstants.BEGINNER_GROUP_ID;
                    break;
                case "INTERMEDIATE":
                    drillGroupId = DrillGroupConstants.INTERMEDIATE_GROUP_ID;
                    break;
                case "ADVANCE":
                case "ADVANCED":
                    drillGroupId = DrillGroupConstants.ADVANCE_GROUP_ID;
                    break;
                case "ELITE":
                    drillGroupId = DrillGroupConstants.ELITE_GROUP_ID;
                    break;
                default:
                    logger.error("❌ Invalid group name: {}", groupName);
                    return Response.status(400)
                            .entity(new GenericApiResponse<>(400, 
                                    "Invalid group name. Valid values are: BEGINNER, INTERMEDIATE, ADVANCE, ELITE", null))
                            .build();
            }

            logger.info("🎯 Completing drills for userId={}, groupName={}, drillGroupId={}, levelIndex={}, orderIndex={}",
                    userId, groupName, drillGroupId, request.getLevelIndex(), request.getOrderIndex());

            // Find drills matching the criteria
            List<AthleteDrillDetail> athleteDrillDetails;
            if (request.getLevelIndex() != null) {
                // Find by group and level index
                athleteDrillDetails = athleteDrillService.findWithAthleteGroupAndLevel(
                        userId, drillGroupId, request.getLevelIndex(), 
                        FindOptions.builder().limit(9999).offset(0).build());
            } else {
                // Find by group and filter by order index
                List<AthleteDrillDetail> allDrills = athleteDrillService.findWithAthleteAndGroup(
                        userId, drillGroupId, 
                        FindOptions.builder().limit(9999).offset(0).build());
                athleteDrillDetails = athleteManagerUtils.withOrderIndex(
                        drillGroupId, request.getOrderIndex(), allDrills);
            }

            if (athleteDrillDetails == null || athleteDrillDetails.isEmpty()) {
                logger.warn("⚠️ No drills found for userId={}, groupName={}, levelIndex={}, orderIndex={}",
                        userId, groupName, request.getLevelIndex(), request.getOrderIndex());
                return Response.ok(
                        new GenericApiResponse<>(200, 
                                "No drills found matching the criteria", 
                                new ArrayList<>()))
                        .build();
            }

            logger.info("📋 Found {} drills to complete", athleteDrillDetails.size());

            // Build DrillPartial list for bulk completion
            List<DrillPartial> drillPartials = new ArrayList<>();
            for (AthleteDrillDetail athleteDrillDetail : athleteDrillDetails) {
                // Get passingScore and shotsMax from drill item
                Integer passingScore = athleteDrillDetail.getPassingScore();
                Integer shotsMax = athleteDrillDetail.getShotsMax();

                // Set defaults if not available
                if (passingScore == null) {
                    passingScore = 3; // Default passing score
                    logger.warn("⚠️ Passing score not found for drillItemId={}, using default: {}", 
                            athleteDrillDetail.getDrillItemId(), passingScore);
                }
                if (shotsMax == null) {
                    shotsMax = 20; // Default shots max
                    logger.warn("⚠️ Shots max not found for drillItemId={}, using default: {}", 
                            athleteDrillDetail.getDrillItemId(), shotsMax);
                }

                // Create DrillPartial with attempts/makes based on drill requirements
                DrillPartial drillPartial = DrillPartial.builder()
                        .drillItemId(athleteDrillDetail.getDrillItemId())
                        .userId(userId)
                        .drillStatus("COMPLETE")
                        .mediaId(request.getMediaId() != null ? Optional.of(request.getMediaId()) : Optional.empty())
                        .attemptsDetected(shotsMax) // Set to shotsMax
                        .attemptsReported(shotsMax) // Set to shotsMax
                        .makesDetected(passingScore) // Set to passingScore
                        .makesReported(passingScore) // Set to passingScore
                        .version(Optional.empty()) // Will be set by completeDrill
                        .build();

                drillPartials.add(drillPartial);
            }

            logger.info("📦 Prepared {} drill partials for completion", drillPartials.size());

            // Use existing bulk complete logic
            List<Object> results = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            // Process each drill completion
            for (int i = 0; i < drillPartials.size(); i++) {
                DrillPartial drillPartial = drillPartials.get(i);

                try {
                    UUID drillItemId = drillPartial.getDrillItemId();
                    UUID placeholderDrillId = UUID.randomUUID(); // Will be ignored by completeDrill

                    // Call the existing complete drill logic
                    int result = athleteDrillItemProgressManager.completeDrill(placeholderDrillId, userId, drillPartial);

                    if (result == 0) {
                        String error = String.format("Drill at index %d: Failed to complete drill for drillItemId=%s", 
                                i, drillItemId);
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
                            logger.info("✅ Drill completed successfully at index %d. DrillId=%s, DrillItemId=%s", 
                                    i, actualDrillId, drillItemId);
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
                athleteMetricManager.updateDrillCompletionMetrics(userId);
            }

            // Prepare response
            if (failureCount == 0) {
                // All successful
                logger.info("✅ All {} drills completed successfully for group={}, levelIndex={}, orderIndex={}", 
                        successCount, groupName, request.getLevelIndex(), request.getOrderIndex());
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
            logger.error("💥 Exception occurred while completing drills by group and level: {}", e.getMessage(), e);
            return Response.status(500)
                    .entity(new GenericApiResponse<>(500, "Failed to complete drills: " + e.getMessage(), null))
                    .build();
        }
    }

    @PATCH
    @Path("/{drillItemId}/media")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateDrillMedia(
            @PathParam("drillItemId") UUID drillItemId,
            UpdateDrillMediaRequest request) {
        try {
            if (request == null || request.getMediaId() == null) {
                logger.error("❌ Invalid request: mediaId is null");
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "mediaId is required", null))
                        .build();
            }

            UUID userId;
            try {
                userId = currentUserUtils.getCurrentUserId();
            } catch (Exception e) {
                logger.error("❌ Failed to extract userId from token: {}", e.getMessage());
                return Response.status(401)
                        .entity(new GenericApiResponse<>(401, "Failed to extract user ID from token: " + e.getMessage(), null))
                        .build();
            }

            java.util.Optional<DrillRow> drillRowOpt = drillService.findByDrillItemIdAndUserId(drillItemId, userId);
            if (drillRowOpt == null || drillRowOpt.isEmpty()) {
                logger.error("❌ Drill not found for drillItemId={}, userId={}", drillItemId, userId);
                return Response.status(404)
                        .entity(new GenericApiResponse<>(404, "Drill not found", null))
                        .build();
            }

            UUID drillId = drillRowOpt.get().getId();
            int result = drillService.updateMediaId(drillId, request.getMediaId());

            if (result == 0) {
                logger.error("❌ Failed to update mediaId for drillId={}", drillId);
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "Failed to update media ID", null))
                        .build();
            }

            if (request.getVersion() != null) {
                int updated = drillService.updateAttemptMediaId(drillId, request.getMediaId(), request.getVersion());
                if (updated == 0) {
                    logger.warn("⚠️ Version {} matched 0 rows for drillId={}, falling back to latest attempt", request.getVersion(), drillId);
                    drillService.updateLatestAttemptMediaId(drillId, request.getMediaId());
                } else {
                    logger.info("✅ MediaId updated for drillId={}, mediaId={}, version={}", drillId, request.getMediaId(), request.getVersion());
                }
            } else {
                drillService.updateLatestAttemptMediaId(drillId, request.getMediaId());
                logger.info("✅ MediaId updated for drillId={}, mediaId={} (no version — back-filled latest attempt)", drillId, request.getMediaId());
            }

            return drillService.findByIdWithHistory(drillId)
                    .map(drillWithHistory -> Response.ok(
                            new GenericApiResponse<>(200, "Media ID updated successfully", drillWithHistory)).build())
                    .orElseGet(() -> Response.status(404)
                            .entity(new GenericApiResponse<>(404, "Drill not found after update", null)).build());

        } catch (Exception e) {
            logger.error("💥 Exception updating mediaId for drillItemId={}: {}", drillItemId, e.getMessage(), e);
            return Response.status(500)
                    .entity(new GenericApiResponse<>(500, "Failed to update media ID: " + e.getMessage(), null))
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
