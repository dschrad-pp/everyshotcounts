package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillGroupPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillGroupService;
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

@Path("/api/drill_group")
public class DrillGroupResource {
    private static Logger logger = LoggerFactory.getLogger(DrillGroupResource.class);

    @Inject
    DrillGroupService drillGroupService;

    @GET
    @Path("/{drillGroupId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDrillGroup(@PathParam("drillGroupId") UUID drillGroupId) {
        return drillGroupService.findById(drillGroupId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDrillGroup(DrillGroupPartial drillGroupPartial) {
        return Response.ok(drillGroupService.create(drillGroupPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDrillGroup(DrillGroupPartial drillGroupPartial) {
        return Response.ok(drillGroupService.update(drillGroupPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllDrillGroups(@Context UriInfo uriInfo) {
        return Response.ok(drillGroupService.findAll(new FindOptions(uriInfo))).build();
    }

}
