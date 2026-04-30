package com.lektralabs.thrones.pallbearer.api.resource;

import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.CoachPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.CoachDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.service.TeamService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/coach")
public class CoachResource {

    private static final Logger logger = Logger.getLogger(CoachResource.class);

    @Inject
    CoachDrillService coachDrillService;

    @Inject
    AthleteDrillService athleteDrillService;

    @Inject
    TeamService teamService;

    @GET
    @Path("/{coachId}")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCoachById(@PathParam("coachId") UUID coachId) {
        logger.infof("Fetching coach by ID: %s", coachId);

        return coachDrillService.findCoachById(coachId)
                .map(coachPartial -> Response.ok(
                        new GenericApiResponse<>(200, "Successfully fetched coach partial", coachPartial)).build())
                .orElseGet(() -> Response.status(404)
                        .entity(new GenericApiResponse<>(404, "Coach not found", null)).build());
    }

    @GET
    @Path("/{coachId}/team")
    @RolesAllowed({ "ADMIN", "COACH" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTeamForCoach(@PathParam("coachId") UUID coachId) {
        return teamService.getTeamForCoach(coachId)
                .map(team -> Response.ok(new GenericApiResponse<>(200, "Successfully fetched team", team)).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("/{coachId}/athletes")
    @RolesAllowed({ "ADMIN", "COACH" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthletesAssignedToCoach(@PathParam("coachId") UUID coachId) {
        logger.infof("Fetching athletes assigned to coach ID: %s", coachId);

        List<AthleteDetail> athletes = coachDrillService.findAllAthletesAssignedToCoach(coachId);

        return Response.ok(
                new GenericApiResponse<>(200,
                        athletes.isEmpty() ? "No athletes found for this coach"
                                : "Successfully fetched athletes for coach",
                        athletes))
                .build();
    }

    @GET
    @Path("/{coachId}/athlete/{athleteId}/drill_detail")
    @RolesAllowed({ "ADMIN", "COACH" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDrillDetailsOfAthleteAssignedToCoach(@PathParam("coachId") UUID coachId,
            @PathParam("athleteId") UUID athleteId) {
        // logger.infof("Fetching drill details for athletes assigned to coach ID: %s",
        // athleteId);
        List<AthleteDrillDetail> athleteDrillDetails = athleteDrillService
                .findLatestAttemptedDrillsForAthleteUnderCoach(coachId, athleteId);
        // logger.info(String.format("the number of drills returned : %s",
        // athleteDrillDetails.size()));
        return Response.ok(
                new GenericApiResponse<>(200,
                        athleteDrillDetails.isEmpty() ? "No drill details found for athletes of this coach"
                                : "Successfully fetched drill details for coach",
                        athleteDrillDetails))
                .build();
    }

    @GET
    @Path("/{coachId}/athlete/{athleteId}/curriculum")
    @RolesAllowed({ "ADMIN", "COACH" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFullCurriculumForAthleteUnderCoach(@PathParam("coachId") UUID coachId,
            @PathParam("athleteId") UUID athleteId) {
        logger.infof("Fetching full curriculum for athlete ID: %s under coach ID: %s", athleteId, coachId);

        List<AthleteDrillDetail> curriculum = athleteDrillService
                .findFullCurriculumForAthleteUnderCoach(coachId, athleteId);

        return Response.ok(
                new GenericApiResponse<>(200,
                        curriculum.isEmpty() ? "No curriculum found for this athlete"
                                : "Successfully fetched full curriculum for athlete",
                        curriculum))
                .build();
    }

}
