package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillItemDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillItemDetailService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Path("/api/drill_item/detail")
public class DrillItemDetailResource {

    private static Logger logger = LoggerFactory.getLogger(DrillItemDetailResource.class);

    @Inject
    DrillItemDetailService drillItemDetailService;

    /**
     * Get the drill item detail for the drill item associated with the
     * specified drill item ID
     *
     * @param drillItemId Drill item ID
     * @return Drill item detail or not found
     */
    @GET
    @Path("/{drillItemId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response findByDrillItemId(@PathParam("drillItemId") UUID drillItemId) {
        return drillItemDetailService.findByDrillItemId(drillItemId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("/group/{groupId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDrillItemDetailsByGroup(@PathParam("groupId") UUID groupId) {
        logger.info("Received request for group ID: {}", groupId);
        List<DrillItemDetail> results = drillItemDetailService.findDrillItemByGroupId(groupId);
        logger.info("Service returned {} results", results.size());

        if (results.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(results).build();
    }

    /**
     * Get all drill items owned by the specified team
     *
     * @param teamId ID of team that owns the drill items
     * @return Drill item details
     */
    @GET
    @Path("/team/{teamId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTeamDrillItemDetails(@PathParam("teamId") UUID teamId) {
        List<DrillItemDetail> results = drillItemDetailService.getTeamDrillItemDetails(teamId);
        return Response.ok(results).build();
    }
}
