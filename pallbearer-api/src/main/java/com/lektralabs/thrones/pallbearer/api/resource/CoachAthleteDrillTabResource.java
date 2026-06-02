package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.api.model.response.CompletedDrillResponse;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
            @QueryParam("tagCodes") List<String> tagCodes,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        List<AthleteDrillDetail> drills = athleteDrillService
                .findCompletedDrillsForAthleteUnderCoach(coachId, athleteId, tagCodes, page, limit);

        List<CompletedDrillResponse> response = drills.stream()
                .map(d -> CompletedDrillResponse.builder()
                        .id(d.getDrillDetail().map(dd -> dd.getId() != null ? dd.getId().toString() : null).orElse(null))
                        .name(d.getName().orElse(null))
                        .makesReported(d.getDrillDetail().map(dd -> dd.getMakesReported()).orElse(null))
                        .attemptsReported(d.getDrillDetail().map(dd -> dd.getAttemptsReported()).orElse(null))
                        .tags(d.getTags())
                        .bestMakeStreak(d.getDrillDetail().map(dd -> dd.getBestMakeStreak()).orElse(null))
                        .longestMissStreak(d.getDrillDetail().map(dd -> dd.getLongestMissStreak()).orElse(null))
                        .avgTimePerRoundSeconds(d.getDrillDetail().map(dd -> dd.getAvgTimePerRoundSeconds()).orElse(null))
                        .build())
                .collect(Collectors.toList());

        return Response.ok(new GenericApiResponse<>(200,
                response.isEmpty() ? "No completed drills found" : "Successfully fetched completed drills",
                response)).build();
    }
}
