package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.api.model.request.PassingScoreUpdateRequest;
import com.lektralabs.thrones.pallbearer.api.model.request.TimeLimitByLevelUpdateRequest;
import com.lektralabs.thrones.pallbearer.api.model.request.TimeLimitByDrillItemsUpdateRequest;
import com.lektralabs.thrones.pallbearer.api.model.response.DrillListResponse;
import com.lektralabs.thrones.pallbearer.api.model.response.PassingScoreUpdateResponse;
import com.lektralabs.thrones.pallbearer.api.model.response.TimeLimitByLevelUpdateResponse;
import com.lektralabs.thrones.pallbearer.api.model.response.TimeLimitByDrillItemsUpdateResponse;
import com.lektralabs.thrones.pallbearer.api.service.DrillItemPassingScoreService;
import com.lektralabs.thrones.pallbearer.api.service.DrillItemTimeLimitService;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.service.CoachDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillItemDetailService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/api/drill/detail")
public class DrillDetailResource {

        private static Logger logger = LoggerFactory.getLogger(DrillDetailResource.class);

        @Inject
        AthleteDrillService athleteDrillService;

        @Inject
        CoachDrillService coachDrillService;

        @Inject
        DrillItemDetailService drillItemDetailService;

        @Inject
        DrillItemPassingScoreService drillItemPassingScoreService;

        @Inject
        DrillItemTimeLimitService drillItemTimeLimitService;

        // Map level names to drill group IDs
        private static final Map<String, UUID> LEVEL_TO_GROUP_ID = Map.of(
                        "Beginner", DrillGroupConstants.BEGINNER_GROUP_ID,
                        "Intermediate", DrillGroupConstants.INTERMEDIATE_GROUP_ID,
                        "Advanced", DrillGroupConstants.ADVANCE_GROUP_ID,
                        // "Advance", DrillGroupConstants.ADVANCE_GROUP_ID,
                        "Elite", DrillGroupConstants.ELITE_GROUP_ID);

        /**
         * Get the athlete drill detail for the specified drill item
         *
         * @param athleteUserId Athlete user ID
         * @param drillItemId   Drill item ID
         * @return Matching drill detail
         */
        @GET
        @Path("/athlete/{athleteUserId}/drill_item/{drillItemId}")
        @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAthleteDrillDetail(@PathParam("athleteUserId") UUID athleteUserId,
                        @PathParam("drillItemId") UUID drillItemId) {
                return Response.ok(athleteDrillService.findWithAthleteAndDrillItem(
                                athleteUserId, drillItemId)).build();
        }

        /**
         * Get athlete drill details for the specified drill group
         *
         * @param uriInfo       URI information and request parameters
         * @param athleteUserId Athlete user ID
         * @param drillGroupId  Drill group ID
         * @return Matching drill details
         */
        @GET
        @Path("/athlete/{athleteUserId}/group/{drillGroupId}")
        @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAthleteGroupDrillDetails(@Context UriInfo uriInfo,
                        @PathParam("athleteUserId") UUID athleteUserId,
                        @PathParam("drillGroupId") UUID drillGroupId) {
                try {
                        logger.info("Fetching drill details for athlete: {}, group: {}", athleteUserId, drillGroupId);

                        List<AthleteDrillDetail> drillDetails = athleteDrillService.findWithAthleteAndGroup(
                                        athleteUserId, drillGroupId, new FindOptions(uriInfo));

                        logger.info("Found {} drill details for athlete: {}, group: {}",
                                        drillDetails != null ? drillDetails.size() : 0, athleteUserId, drillGroupId);

                        if (drillDetails == null) {
                                logger.warn("Service returned null for athlete: {}, group: {}", athleteUserId,
                                                drillGroupId);
                                drillDetails = new java.util.ArrayList<>();
                        }

                        // Replace "Expert" with "Advanced" in drillGroup name
                        drillDetails.forEach(detail -> {
                                if (detail.getDrillGroup() != null && detail.getDrillGroup().getName().isPresent()) {
                                        String name = detail.getDrillGroup().getName().get();
                                        if ("Expert".equals(name)) {
                                                detail.getDrillGroup().setName(Optional.of("Advanced"));
                                        }
                                }
                        });

                        return Response.ok(drillDetails).build();
                } catch (Exception e) {
                        logger.error("Error fetching drill details for athlete: {}, group: {}",
                                        athleteUserId, drillGroupId, e);
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                        .entity("{\"error\": \"Failed to fetch drill details: " + e.getMessage()
                                                        + "\"}")
                                        .build();
                }
        }

        /**
         * Get athlete drill details for the specified drill group and level index
         *
         * @param uriInfo       URI information and request parameters
         * @param athleteUserId Athlete user ID
         * @param drillGroupId  Drill group ID
         * @param levelIndex    Level index to filter by
         * @return Matching drill details
         */
        @GET
        @Path("/athlete/{athleteUserId}/group/{drillGroupId}/level/{levelIndex}")
        @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAthleteGroupLevelDrillDetails(
                        @Context UriInfo uriInfo,
                        @PathParam("athleteUserId") UUID athleteUserId,
                        @PathParam("drillGroupId") UUID drillGroupId,
                        @PathParam("levelIndex") Integer levelIndex) {
                return Response.ok(athleteDrillService.findWithAthleteGroupAndLevel(
                                athleteUserId, drillGroupId, levelIndex, new FindOptions(uriInfo))).build();
        }

        /**
         * Get media URLs for athlete drill details for the specified drill group and
         * level index
         *
         * @param uriInfo       URI information and request parameters
         * @param athleteUserId Athlete user ID
         * @param drillGroupId  Drill group ID
         * @param levelIndex    Level index to filter by
         * @return Media URLs and metadata for the drills
         */
        @GET
        @Path("/athlete/{athleteUserId}/group/{drillGroupId}/level/{levelIndex}/media")
        @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAthleteGroupLevelDrillMedia(
                        @Context UriInfo uriInfo,
                        @PathParam("athleteUserId") UUID athleteUserId,
                        @PathParam("drillGroupId") UUID drillGroupId,
                        @PathParam("levelIndex") Integer levelIndex) {
                return Response.ok(athleteDrillService.findMediaWithAthleteGroupAndLevel(
                                athleteUserId, drillGroupId, levelIndex, new FindOptions(uriInfo))).build();
        }

        // @GET
        // @Path("/athlete/{athleteUserId}/group/{drillGroupId}/level/{levelIndex}")
        // @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
        // @Produces(MediaType.APPLICATION_JSON)
        // public Response getAthleteGroupLevelDrillDetails(
        // @Context UriInfo uriInfo,
        // @PathParam("athleteUserId") UUID athleteUserId,
        // @PathParam("drillGroupId") UUID drillGroupId,
        // @PathParam("levelIndex") Integer levelIndex) {
        // // return
        // Response.ok(athleteDrillService.findWithAthleteGroupAndLevel(athleteUserId,
        // drillGroupId, levelIndex, new FindOptions(uriInfo))).build();
        // return Response.ok().build();
        // }

        @GET
        @Path("/athlete/{athleteUserId}/timeline")
        @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAthleteTimelineDrillDetails(@Context UriInfo uriInfo,
                        @PathParam("athleteUserId") UUID athleteUserId) {
                return Response.ok(athleteDrillService.findWithAthleteTimeline(
                                athleteUserId, new FindOptions(uriInfo))).build();
        }

        @GET
        @Path("/team/{teamId}/timeline")
        @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
        @Produces(MediaType.APPLICATION_JSON)
        public Response getTeamTimelineDrillDetails(@Context UriInfo uriInfo,
                        @PathParam("teamId") UUID teamId) {
                return Response.ok(coachDrillService.findWithTeamTimeline(
                                teamId, new FindOptions(uriInfo))).build();
        }

        /**
         * Get all drills for a specific level (Beginner, Intermediate,
         * Advanced/Advance, Elite)
         * 
         * @param level Level name: Beginner, Intermediate, Advanced, Advance, or Elite
         * @return List of drills with total count
         */
        @GET
        @Path("/level/{level}")
        @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
        @Produces(MediaType.APPLICATION_JSON)
        public Response getDrillsByLevel(@PathParam("level") String level) {
                try {
                        if (level == null || level.trim().isEmpty()) {
                                return Response.status(HttpStatus.SC_BAD_REQUEST)
                                                .entity(new GenericApiResponse<>(
                                                                HttpStatus.SC_BAD_REQUEST,
                                                                "Level parameter is required",
                                                                null))
                                                .build();
                        }

                        // Normalize level name (case-insensitive)
                        String normalizedLevel = level.trim();
                        if (normalizedLevel.length() > 0) {
                                normalizedLevel = normalizedLevel.substring(0, 1).toUpperCase() +
                                                (normalizedLevel.length() > 1
                                                                ? normalizedLevel.substring(1).toLowerCase()
                                                                : "");
                        }

                        // Handle "Expert" and "Advance" as the same as "Advanced"
                        if ("Expert".equalsIgnoreCase(normalizedLevel) || "Advance".equalsIgnoreCase(normalizedLevel)) {
                                normalizedLevel = "Advanced";
                        }

                        // Get drill group ID for the level
                        UUID drillGroupId = LEVEL_TO_GROUP_ID.get(normalizedLevel);

                        if (drillGroupId == null) {
                                logger.warn("Invalid level name provided: {}", level);
                                return Response.status(HttpStatus.SC_BAD_REQUEST)
                                                .entity(new GenericApiResponse<>(
                                                                HttpStatus.SC_BAD_REQUEST,
                                                                "Invalid level. Valid levels are: Beginner, Intermediate, Advanced/Advance, Elite",
                                                                null))
                                                .build();
                        }

                        // Fetch drills for the drill group
                        List<DrillItemDetail> drills = drillItemDetailService.findDrillItemByGroupId(drillGroupId);
                        int totalCount = drills.size();

                        // Create response
                        DrillListResponse response = DrillListResponse.builder()
                                        .drills(drills)
                                        .totalCount(totalCount)
                                        .level(normalizedLevel)
                                        .build();

                        logger.info("Retrieved {} drills for level: {}", totalCount, normalizedLevel);

                        return Response.ok(new GenericApiResponse<>(
                                        HttpStatus.SC_OK,
                                        "Drills fetched successfully",
                                        response)).build();

                } catch (Exception e) {
                        logger.error("Error fetching drills for level: {}", level, e);
                        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                                        .entity(new GenericApiResponse<>(
                                                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                                        "Failed to fetch drills: " + e.getMessage(),
                                                        null))
                                        .build();
                }
        }

        /**
         * Bulk update passing scores for drills based on level, name, and description
         * 
         * @param request Update request containing list of drills to update and
         *                matching options
         * @return Response with update results
         */
        @PUT
        @Path("/passing-score/bulk-update")
        @RolesAllowed({ "ADMIN", "COACH" })
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response bulkUpdatePassingScores(PassingScoreUpdateRequest request) {
                try {
                        if (request == null || request.getUpdates() == null || request.getUpdates().isEmpty()) {
                                return Response.status(HttpStatus.SC_BAD_REQUEST)
                                                .entity(new GenericApiResponse<>(
                                                                HttpStatus.SC_BAD_REQUEST,
                                                                "Request body is required with at least one update",
                                                                null))
                                                .build();
                        }

                        PassingScoreUpdateResponse response = drillItemPassingScoreService.updatePassingScores(request);

                        int statusCode = response.getTotalUpdated() > 0
                                        ? HttpStatus.SC_OK
                                        : HttpStatus.SC_NOT_FOUND;

                        String message = response.getTotalUpdated() > 0
                                        ? String.format("Successfully updated %d out of %d drills",
                                                        response.getTotalUpdated(), response.getTotalRequested())
                                        : "No drills were updated";

                        return Response.status(statusCode)
                                        .entity(new GenericApiResponse<>(
                                                        statusCode,
                                                        message,
                                                        response))
                                        .build();

                } catch (Exception e) {
                        logger.error("Error in bulk update passing scores", e);
                        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                                        .entity(new GenericApiResponse<>(
                                                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                                        "Failed to update passing scores: " + e.getMessage(),
                                                        null))
                                        .build();
                }
        }

        @PUT
        @Path("/time-limit/by-level")
        @RolesAllowed({ "ADMIN" })
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response bulkUpdateTimeLimitByLevel(TimeLimitByLevelUpdateRequest request) {
                try {
                        if (request == null) {
                                return Response.status(HttpStatus.SC_BAD_REQUEST)
                                                .entity(new GenericApiResponse<>(
                                                                HttpStatus.SC_BAD_REQUEST,
                                                                "Request body is required",
                                                                null))
                                                .build();
                        }

                        TimeLimitByLevelUpdateResponse response = drillItemTimeLimitService
                                        .updateTimeLimitByLevel(request);

                        String message = Boolean.TRUE.equals(request.getDryRun())
                                        ? String.format("Dry run successful. Matched %d drills", response.getMatchedCount())
                                        : String.format("Successfully updated %d drills", response.getUpdatedCount());

                        return Response.ok(new GenericApiResponse<>(
                                        HttpStatus.SC_OK,
                                        message,
                                        response)).build();
                } catch (IllegalArgumentException e) {
                        return Response.status(HttpStatus.SC_BAD_REQUEST)
                                        .entity(new GenericApiResponse<>(
                                                        HttpStatus.SC_BAD_REQUEST,
                                                        e.getMessage(),
                                                        null))
                                        .build();
                } catch (Exception e) {
                        logger.error("Error in bulk time limit update by level", e);
                        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                                        .entity(new GenericApiResponse<>(
                                                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                                        "Failed to update time limit: " + e.getMessage(),
                                                        null))
                                        .build();
                }
        }

        @PUT
        @Path("/time-limit/by-drill-items")
        @RolesAllowed({ "ADMIN" })
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response bulkUpdateTimeLimitByDrillItems(TimeLimitByDrillItemsUpdateRequest request) {
                try {
                        if (request == null || request.getUpdates() == null || request.getUpdates().isEmpty()) {
                                return Response.status(HttpStatus.SC_BAD_REQUEST)
                                                .entity(new GenericApiResponse<>(
                                                                HttpStatus.SC_BAD_REQUEST,
                                                                "Request body must include at least one update item",
                                                                null))
                                                .build();
                        }

                        TimeLimitByDrillItemsUpdateResponse response = drillItemTimeLimitService
                                        .updateTimeLimitByDrillItems(request);
                        String message = Boolean.TRUE.equals(request.getDryRun())
                                        ? String.format("Dry run successful. %d updates validated", response.getTotalUpdated())
                                        : String.format("Successfully updated %d drill items", response.getTotalUpdated());

                        return Response.ok(new GenericApiResponse<>(
                                        HttpStatus.SC_OK,
                                        message,
                                        response)).build();
                } catch (IllegalArgumentException e) {
                        return Response.status(HttpStatus.SC_BAD_REQUEST)
                                        .entity(new GenericApiResponse<>(
                                                        HttpStatus.SC_BAD_REQUEST,
                                                        e.getMessage(),
                                                        null))
                                        .build();
                } catch (Exception e) {
                        logger.error("Error in bulk time limit update by drill items", e);
                        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                                        .entity(new GenericApiResponse<>(
                                                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                                        "Failed to update time limit: " + e.getMessage(),
                                                        null))
                                        .build();
                }
        }
}
