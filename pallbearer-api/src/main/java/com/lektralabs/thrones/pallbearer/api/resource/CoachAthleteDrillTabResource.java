package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/coach-athlete-drill-tab")
public class CoachAthleteDrillTabResource {

    @Inject
    AthleteDrillService athleteDrillService;

    @GET
    @Path("/{coachId}/athlete/{athleteId}/drills/completed")
    @RolesAllowed({ "ADMIN", "COACH" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCompletedDrillsForAthlete(
            @PathParam("coachId") UUID coachId,
            @PathParam("athleteId") UUID athleteId,
            @QueryParam("drillGroupId") UUID drillGroupId,
            @QueryParam("tagCodes") List<String> tagCodes,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        List<AthleteDrillDetail> drills = athleteDrillService
                .findCompletedDrillsForAthleteUnderCoach(coachId, athleteId, drillGroupId, tagCodes, page, limit);

        return Response.ok(new GenericApiResponse<>(200,
                drills.isEmpty() ? "No completed drills found" : "Successfully fetched completed drills",
                drills)).build();
    }
}
