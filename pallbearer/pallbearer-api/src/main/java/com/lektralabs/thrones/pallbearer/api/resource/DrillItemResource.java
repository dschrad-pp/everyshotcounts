package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillItemPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillItemService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Path("/api/drill_item")
public class DrillItemResource {

    private static Logger logger = LoggerFactory.getLogger(DrillItemResource.class);

    @Inject
    DrillItemService drillItemService;

    @GET
    @Path("/{drillItemId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDrillItem(@PathParam("drillItemId") UUID drillItemId) {
        return drillItemService.findById(drillItemId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDrillItem(DrillItemPartial drillItemPartial) {
        return Response.ok(drillItemService.createWithOrder(drillItemPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDrillItem(DrillItemPartial drillItemPartial) {
        return Response.ok(drillItemService.update(drillItemPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllDrills() {
        return Response.ok(drillItemService.getAllDrills()).build();
    }

}
