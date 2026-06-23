package com.lektralabs.thrones.pallbearer.api.resource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.CoachPartial;
import com.lektralabs.thrones.pallbearer.api.model.response.AthleteDrillStatsItem;
import com.lektralabs.thrones.pallbearer.api.model.response.PlayerSnapshotResponse;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.AthleteSnapshotStatsRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.DrillSkillTagRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.DrillStatsRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.SkillBreakdownRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.service.CoachAthleteSnapshotService;
import com.lektralabs.thrones.pallbearer.jdbi.service.CoachDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.service.TeamService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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

    @Inject
    CoachAthleteSnapshotService snapshotService;

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

    @GET
    @Path("/{coachId}/athlete/{athleteId}/snapshot")
    @RolesAllowed({ "ADMIN", "COACH" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteSnapshot(@PathParam("coachId") UUID coachId,
            @PathParam("athleteId") UUID athleteId) {
        logger.infof("Fetching player snapshot for athlete ID: %s under coach ID: %s", athleteId, coachId);

        AthleteDetail athlete = coachDrillService.findAthleteAssignedToCoach(coachId, athleteId)
                .orElse(null);
        if (athlete == null) {
            return Response.status(403)
                    .entity(new GenericApiResponse<>(403, "Athlete not accessible by this coach", null))
                    .build();
        }

        AthleteSnapshotStatsRow stats = snapshotService.getAthleteStats(athleteId);
        int overallMakePercent = CoachAthleteSnapshotService.computeMakePercent(stats.getTotalMakes(), stats.getTotalAttempts());

        String levelLabel = snapshotService.getLevelLabel(athleteId);
        int levelProgress = snapshotService.getLevelProgress(athleteId);

        List<SkillBreakdownRow> skillRows = snapshotService.getSkillBreakdown(athleteId);
        List<PlayerSnapshotResponse.SkillBreakdown> skillBreakdown = skillRows.stream()
                .map(row -> new PlayerSnapshotResponse.SkillBreakdown(
                        row.getTagCode(),
                        row.getCoveragePercent()))
                .collect(Collectors.toList());

        List<PlayerSnapshotResponse.ShootingZone> shootingZones = snapshotService.getShootingZones(athleteId).stream()
                .map(zone -> new PlayerSnapshotResponse.ShootingZone(
                        zone.getZoneCode(),
                        CoachAthleteSnapshotService.computeMakePercent(zone.getTotalMakes(), zone.getTotalAttempts()),
                        zone.getTotalMakes(),
                        zone.getTotalAttempts()))
                .collect(Collectors.toList());

        String firstName = (athlete.getContactItem() != null) ? athlete.getContactItem().getFirstName() : "";
        String lastName = (athlete.getContactItem() != null) ? athlete.getContactItem().getLastName() : "";
        String name = (firstName + " " + lastName).trim();

        PlayerSnapshotResponse snapshot = PlayerSnapshotResponse.builder()
                .id(athleteId.toString())
                .name(name)
                .levelLabel(levelLabel)
                .sessionCount(stats.getSessionCount())
                .overallMakePercent(overallMakePercent)
                .makePercent(overallMakePercent)
                .totalMakes(stats.getTotalMakes())
                .totalAttempts(stats.getTotalAttempts())
                .bestSessionMakes(stats.getBestSessionMakes())
                .worstSessionMisses(stats.getWorstSessionMisses())
                .bestMakeStreak(stats.getBestMakeStreak())
                .worstMissStreak(stats.getWorstMissStreak())
                .roundsToPass(stats.getRoundsToPass())
                .levelProgress(levelProgress)
                .skillBreakdown(skillBreakdown)
                .shootingZones(shootingZones)
                .build();

        return Response.ok(new GenericApiResponse<>(200, "Success", snapshot)).build();
    }

    @GET
    @Path("/{coachId}/athlete/{athleteId}/drills/stats")
    @RolesAllowed({ "ADMIN", "COACH" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteDrillStats(@PathParam("coachId") UUID coachId,
            @PathParam("athleteId") UUID athleteId,
            @QueryParam("tagCodes") List<String> tagCodes,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("50") int limit) {
        logger.infof("Fetching drill stats for athlete ID: %s under coach ID: %s", athleteId, coachId);

        if (coachDrillService.findAthleteAssignedToCoach(coachId, athleteId).isEmpty()) {
            return Response.status(403)
                    .entity(new GenericApiResponse<>(403, "Athlete not accessible by this coach", null))
                    .build();
        }

        int clampedLimit = Math.min(Math.max(limit, 1), 100);
        int clampedPage = Math.max(page, 0);

        List<DrillStatsRow> drillRows = snapshotService.getDrillStats(athleteId, tagCodes, clampedPage, clampedLimit);

        List<UUID> drillItemIds = drillRows.stream()
                .map(DrillStatsRow::getDrillItemId)
                .collect(Collectors.toList());
        Map<UUID, List<DrillSkillTagRow>> tagsByDrillItem = snapshotService.getSkillTagsForDrillItems(drillItemIds);

        List<AthleteDrillStatsItem> items = drillRows.stream()
                .map(row -> {
                    List<AthleteDrillStatsItem.DrillTag> tags = tagsByDrillItem
                            .getOrDefault(row.getDrillItemId(), Collections.emptyList())
                            .stream()
                            .map(t -> new AthleteDrillStatsItem.DrillTag(t.getTagCode(), t.getTagName()))
                            .collect(Collectors.toList());
                    return AthleteDrillStatsItem.builder()
                            .id(row.getDrillItemId().toString())
                            .name(row.getDrillName())
                            .makePercent(CoachAthleteSnapshotService.computeMakePercent(row.getTotalMakes(), row.getTotalAttempts()))
                            .totalMakes(row.getTotalMakes())
                            .totalAttempts(row.getTotalAttempts())
                            .sessions(row.getSessions())
                            .bestMakeStreak(row.getBestMakeStreak())
                            .longestMissStreak(row.getLongestMissStreak())
                            .avgTimePerRoundSeconds(row.getAvgTimePerRoundSeconds())
                            .tags(tags)
                            .build();
                })
                .collect(Collectors.toList());

        return Response.ok(new GenericApiResponse<>(200, "Success", items)).build();
    }

}

