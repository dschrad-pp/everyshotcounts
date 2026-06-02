package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.jdbi.service.DeviceTokenService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;

@Path("/api/device")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeviceResource {

    private static final Logger logger = Logger.getLogger(DeviceResource.class);

    @Inject
    DeviceTokenService deviceTokenService;

    @POST
    @Path("/register-token")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    public Response registerToken(Map<String, String> body) {
        try {
            String userIdStr = body.get("userId");
            String token = body.get("token");
            String platform = body.get("platform");

            if (userIdStr == null || token == null || platform == null) {
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "userId, token, and platform are required", null))
                        .build();
            }

            if (!platform.equals("ios") && !platform.equals("android")) {
                return Response.status(400)
                        .entity(new GenericApiResponse<>(400, "platform must be 'ios' or 'android'", null))
                        .build();
            }

            UUID userId = UUID.fromString(userIdStr);
            deviceTokenService.upsert(userId, token, platform);

            return Response.ok(new GenericApiResponse<>(200, "Device token registered", null)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(400)
                    .entity(new GenericApiResponse<>(400, "Invalid userId format", null))
                    .build();
        } catch (Exception e) {
            logger.errorf(e, "Failed to register device token");
            return Response.status(500)
                    .entity(new GenericApiResponse<>(500, "Failed to register device token", null))
                    .build();
        }
    }
}
