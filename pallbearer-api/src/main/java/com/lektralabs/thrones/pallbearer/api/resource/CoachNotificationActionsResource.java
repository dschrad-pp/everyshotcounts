package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.jdbi.service.CoachNotificationService;
import com.lektralabs.thrones.pallbearer.security.CurrentUserUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.UUID;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CoachNotificationActionsResource {

    private static final Logger logger = Logger.getLogger(CoachNotificationActionsResource.class);

    @Inject
    CoachNotificationService coachNotificationService;

    @Inject
    CurrentUserUtils currentUserUtils;

    @PATCH
    @Path("/{notificationId}/read")
    @RolesAllowed({"ADMIN", "COACH"})
    public Response markAsRead(@PathParam("notificationId") UUID notificationId) {
        try {
            UUID callerId = currentUserUtils.getCurrentUserId();
            int updated = coachNotificationService.markAsRead(notificationId, callerId);
            if (updated == 0) {
                return Response.status(404)
                        .entity(new GenericApiResponse<>(404, "Notification not found", null))
                        .build();
            }
            return Response.ok(new GenericApiResponse<>(200, "Notification marked as read", null)).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to mark notification as read: %s", notificationId);
            return Response.status(500)
                    .entity(new GenericApiResponse<>(500, "Failed to update notification", null))
                    .build();
        }
    }

    @DELETE
    @Path("/{notificationId}")
    @RolesAllowed({"ADMIN", "COACH"})
    public Response dismiss(@PathParam("notificationId") UUID notificationId) {
        try {
            UUID callerId = currentUserUtils.getCurrentUserId();
            int updated = coachNotificationService.dismiss(notificationId, callerId);
            if (updated == 0) {
                return Response.status(404)
                        .entity(new GenericApiResponse<>(404, "Notification not found", null))
                        .build();
            }
            return Response.ok(new GenericApiResponse<>(200, "Notification dismissed", null)).build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to dismiss notification: %s", notificationId);
            return Response.status(500)
                    .entity(new GenericApiResponse<>(500, "Failed to dismiss notification", null))
                    .build();
        }
    }
}
