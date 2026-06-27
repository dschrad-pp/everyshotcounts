package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ActivityDay;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteService;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillAttemptHistoryService;
import com.lektralabs.thrones.pallbearer.jdbi.service.TeamService;
import com.lektralabs.thrones.pallbearer.security.CurrentUserUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Path("/api/athlete")
public class AthleteResource {

    private static Logger logger = LoggerFactory.getLogger(AthleteResource.class);

    @Inject
    AthleteService athleteService;

    @Inject
    TeamService teamService;

    @Inject
    CurrentUserUtils currentUserUtils;

    @Inject
    DrillAttemptHistoryService drillAttemptHistoryService;

    /**
     * Upper bound on the requested window, in days, to reject pathological ranges.
     * ~2 years: comfortably covers a 1-year heatmap plus future "13-month" /
     * "53-week" / multi-year views without throwing 400s. The scan is per-athlete
     * and indexed, so this is a sanity guard, not a performance limit.
     */
    private static final long MAX_RANGE_DAYS = 732;

    @GET
    @Path("/{athleteId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER", "COACH"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response findByAthleteId(@PathParam("athleteId") UUID athleteId) {
        return athleteService.findByAthleteId(athleteId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("/{athleteId}/team")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTeamForAthlete(@PathParam("athleteId") UUID athleteId) {
        return teamService.getTeamForAthlete(athleteId)
                .map(team -> Response.ok(team).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("/featured")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFeaturedAthletes() {
        UUID currentUserId = currentUserUtils.getCurrentUserId();
        List<AthleteDetail> results = athleteService.findFeaturedAthletes(currentUserId);
        return Response.ok(results).build();
    }

    /**
     * Get athletes matching the find options provided in URI parameters
     *
     * @param uriInfo URI parameters
     * @return Athlete details
     */
    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthletes(@Context UriInfo uriInfo) {
        return Response.ok(athleteService.findAll(new FindOptions(uriInfo))).build();
    }

    /**
     * Activity heatmap (GitHub-style) for an athlete: per-day count of drill
     * completions over [from, to] inclusive, bucketed in the athlete's timezone.
     *
     * <p>Athlete-only for now: the caller may only read their own activity, so the
     * path id must match the authenticated user. Zero-completion days are omitted;
     * the client zero-fills.
     *
     * @param athleteId target athlete (must equal the authenticated user)
     * @param from      inclusive start day, yyyy-MM-dd
     * @param to        inclusive end day, yyyy-MM-dd
     * @param tz        IANA timezone id, e.g. America/Chicago; defaults to UTC when
     *                  omitted/blank. A present-but-invalid value is still a 400.
     */
    @GET
    @Path("/{athleteId}/activity")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteActivity(@PathParam("athleteId") UUID athleteId,
                                       @QueryParam("from") String from,
                                       @QueryParam("to") String to,
                                       @QueryParam("tz") String tz) {
        UUID currentUserId = currentUserUtils.getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(athleteId)) {
            logger.warn("Activity heatmap denied: caller {} requested athlete {}", currentUserId, athleteId);
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new GenericApiResponse<>(403, "You may only view your own activity", null))
                    .build();
        }

        if (from == null || to == null) {
            return badRequest("Query params 'from' and 'to' are required");
        }

        // tz is optional: fall back to UTC when omitted/blank.
        final String zone = (tz == null || tz.isBlank()) ? "UTC" : tz;

        final LocalDate fromDate;
        final LocalDate toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            return badRequest("'from' and 'to' must be valid dates in yyyy-MM-dd format");
        }

        if (toDate.isBefore(fromDate)) {
            return badRequest("'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_RANGE_DAYS) {
            return badRequest("Requested range exceeds the maximum of " + MAX_RANGE_DAYS + " days");
        }

        try {
            ZoneId.of(zone);
        } catch (DateTimeException e) {
            return badRequest("'tz' must be a valid IANA timezone id, e.g. America/Chicago");
        }

        List<ActivityDay> data = drillAttemptHistoryService.getActivityByDay(athleteId, fromDate, toDate, zone);
        return Response.ok(new GenericApiResponse<>(200, "OK", data)).build();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new GenericApiResponse<>(400, message, null))
                .build();
    }
}
