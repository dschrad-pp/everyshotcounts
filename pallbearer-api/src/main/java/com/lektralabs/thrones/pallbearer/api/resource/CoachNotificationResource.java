package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.jdbi.model.CoachNotificationRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.service.CoachNotificationService;
import com.lektralabs.thrones.pallbearer.security.CurrentUserUtils;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/coach")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CoachNotificationResource {

    private static final Logger logger = Logger.getLogger(CoachNotificationResource.class);

    @Inject
    CoachNotificationService coachNotificationService;

    @Inject
    AthleteDrillService athleteDrillService;

    @Inject
    CurrentUserUtils currentUserUtils;

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Path("/{coachId}/notifications")
    @RolesAllowed({"ADMIN", "COACH"})
    public Response getNotifications(
            @PathParam("coachId") UUID coachId,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        try {
            if (!isCallerOrAdmin(coachId)) {
                return Response.status(403)
                        .entity(new GenericApiResponse<>(403, "Forbidden", null))
                        .build();
            }

            if (page < 1) page = 1;
            if (limit < 1 || limit > 100) limit = 20;

            List<CoachNotificationRow> rows = coachNotificationService.findByCoachId(coachId, page, limit);
            int total = coachNotificationService.countByCoachId(coachId);
            int unreadCount = coachNotificationService.countUnreadByCoachId(coachId);
            int totalPages = (int) Math.ceil((double) total / limit);

            List<Map<String, Object>> notifications = rows.stream()
                    .map(this::toResponseMap)
                    .collect(Collectors.toList());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("notifications", notifications);
            payload.put("totalPages", totalPages);
            payload.put("unreadCount", unreadCount);

            return Response.ok(new GenericApiResponse<>(200, "OK", payload)).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to fetch notifications for coachId=%s", coachId);
            return Response.status(500)
                    .entity(new GenericApiResponse<>(500, "Failed to fetch notifications", null))
                    .build();
        }
    }

    /**
     * Resolve a tapped coach notification to the single passing round it was created
     * for — one round, one video, that round's own (non-summed) stats. Keyed on the
     * server-unique notification id the client already holds, so the iOS side does
     * one exact lookup instead of fetching the whole drill list and filtering.
     */
    @GET
    @Path("/{coachId}/notification/{notificationId}/round")
    @RolesAllowed({"ADMIN", "COACH"})
    public Response getNotificationRound(
            @PathParam("coachId") UUID coachId,
            @PathParam("notificationId") UUID notificationId) {
        try {
            if (!isCallerOrAdmin(coachId)) {
                return Response.status(403)
                        .entity(new GenericApiResponse<>(403, "Forbidden", null))
                        .build();
            }

            Optional<CoachNotificationRow> notificationOpt = coachNotificationService.findById(notificationId);
            if (notificationOpt.isEmpty() || !coachId.equals(notificationOpt.get().getCoachId())) {
                return Response.status(404)
                        .entity(new GenericApiResponse<>(404, "Notification not found", null))
                        .build();
            }

            CoachNotificationRow notification = notificationOpt.get();
            Optional<AthleteDrillDetail> round = athleteDrillService.findPassingRound(
                    notification.getAthleteId(),
                    notification.getDrillId(),
                    notification.getAttemptLocalId());

            return round
                    .map(r -> Response.ok(new GenericApiResponse<>(200, "OK", r)).build())
                    .orElseGet(() -> Response.status(404)
                            .entity(new GenericApiResponse<>(404, "Round not found for notification", null))
                            .build());
        } catch (Exception e) {
            logger.errorf(e, "Failed to fetch round for notificationId=%s", notificationId);
            return Response.status(500)
                    .entity(new GenericApiResponse<>(500, "Failed to fetch notification round", null))
                    .build();
        }
    }

    @DELETE
    @Path("/{coachId}/notifications")
    @RolesAllowed({"ADMIN", "COACH"})
    public Response dismissAll(@PathParam("coachId") UUID coachId) {
        try {
            if (!isCallerOrAdmin(coachId)) {
                return Response.status(403)
                        .entity(new GenericApiResponse<>(403, "Forbidden", null))
                        .build();
            }
            coachNotificationService.dismissAll(coachId);
            return Response.ok(new GenericApiResponse<>(200, "All notifications dismissed", null)).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to dismiss all notifications for coachId=%s", coachId);
            return Response.status(500)
                    .entity(new GenericApiResponse<>(500, "Failed to dismiss notifications", null))
                    .build();
        }
    }

    private boolean isCallerOrAdmin(UUID coachId) {
        if (securityIdentity.hasRole("ADMIN")) return true;
        try {
            return coachId.equals(currentUserUtils.getCurrentUserId());
        } catch (Exception e) {
            logger.warnf(e, "Failed to resolve current user for authorization check");
            return false;
        }
    }

    private Map<String, Object> toResponseMap(CoachNotificationRow row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", row.getId().toString());
        m.put("athleteId", row.getAthleteId().toString());
        m.put("athleteFirstName", row.getAthleteFirstName());
        m.put("athleteLastName", row.getAthleteLastName());
        m.put("drillId", row.getDrillId().toString());
        m.put("drillItemId", row.getDrillItemId().toString());
        m.put("attemptLocalId", row.getAttemptLocalId());
        m.put("drillName", row.getDrillName());
        m.put("completedAt", Instant.ofEpochMilli(row.getCompletedAt()).toString());
        // Coach notifications only fire on a passing completion, so a stored
        // notification is always a pass. Surfaced so the list can render pass state
        // without a per-row drill-item fetch.
        m.put("passed", true);
        m.put("isRead", row.getIsRead());
        m.put("scoreAdjusted", row.getScoreAdjusted());
        m.put("makesDetected", row.getMakesDetected());
        m.put("attemptsDetected", row.getAttemptsDetected());
        m.put("makesReported", row.getMakesReported());
        m.put("attemptsReported", row.getAttemptsReported());
        return m;
    }
}
