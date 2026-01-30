package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillAttemptPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillAttemptService;
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

// This is generated code. Please remove this comment if you modify.

@Path("/api/drill_attempt")
public class DrillAttemptResource {
    private static Logger logger = LoggerFactory.getLogger(DrillAttemptResource.class);

    @Inject
    DrillAttemptService drillAttemptService;

    @GET
    @Path("/{drillAttemptId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDrillAttempt(@PathParam("drillAttemptId") UUID drillAttemptId) {
        return drillAttemptService.findById(drillAttemptId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDrillAttempt(DrillAttemptPartial drillAttemptPartial) {
        return Response.ok(drillAttemptService.create(drillAttemptPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDrillAttempt(DrillAttemptPartial drillAttemptPartial) {
        return Response.ok(drillAttemptService.update(drillAttemptPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllDrillAttempts(@Context UriInfo uriInfo) {
        return Response.ok(drillAttemptService.findAll(new FindOptions(uriInfo))).build();
    }

}
