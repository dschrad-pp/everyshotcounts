package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.TeamPartial;
import com.lektralabs.thrones.pallbearer.api.model.request.JoinTeamRequest;
import com.lektralabs.thrones.pallbearer.api.model.response.TeamLeaderboardResponse;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.TeamLeaderboardRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.CoachAthleteSnapshotService;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillAttemptHistoryService;
import com.lektralabs.thrones.pallbearer.jdbi.service.TeamService;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/team")
public class TeamResource {
    private static Logger logger = LoggerFactory.getLogger(TeamResource.class);

    @Inject
    TeamService teamService;

    @Inject
    UserService userService;

    @Inject
    CoachAthleteSnapshotService snapshotService;

    @Inject
    DrillAttemptHistoryService drillAttemptHistoryService;

    @GET
    @Path("/{teamId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTeam(@PathParam("teamId") UUID teamId) {
        return teamService.findById(teamId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("/{teamId}/leaderboard")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTeamLeaderboard(@PathParam("teamId") UUID teamId) {
        // Membership is the authorization: athletes and coaches may only read the
        // leaderboard of a team they currently belong to (admins any). Leaving the
        // team deletes the membership row, which revokes access here immediately.
        CurrentUser currentUser = userService.getCurrentUser();
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())
                && !teamService.isUserOnTeam(teamId, currentUser.getId())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new GenericApiResponse<>(403, "Leaderboard is only visible to members of this team", null))
                    .build();
        }

        List<TeamLeaderboardRow> rows = snapshotService.getTeamLeaderboard(teamId);

        Map<UUID, String> lastActivityByUserId = drillAttemptHistoryService.getLastActivityByUserIds(
                rows.stream().map(TeamLeaderboardRow::getUserId).collect(Collectors.toList()));

        List<TeamLeaderboardResponse.Entry> entries = rows.stream()
                .map(row -> TeamLeaderboardResponse.Entry.builder()
                        .athleteId(row.getUserId().toString())
                        .name((row.getFirstName() + " " + row.getLastName()).trim())
                        // Null (not 0%) when the athlete has no sessions, so the
                        // client renders a placeholder instead of a fake score.
                        .fgPercent(row.getSessionCount() > 0
                                ? CoachAthleteSnapshotService.computeMakePercent(
                                        row.getTotalMakes(), row.getTotalAttempts())
                                : null)
                        .sessionCount(row.getSessionCount())
                        .levelOrderIndex(row.getLevelOrderIndex())
                        .lastActiveAt(lastActivityByUserId.get(row.getUserId()))
                        .build())
                // Best FG% first, no-session athletes last; ties broken by level then
                // name so the order is stable across refreshes.
                .sorted(Comparator
                        .comparing(TeamLeaderboardResponse.Entry::getFgPercent,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TeamLeaderboardResponse.Entry::getLevelOrderIndex,
                                Comparator.reverseOrder())
                        .thenComparing(TeamLeaderboardResponse.Entry::getName,
                                String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        // Same aggregate definitions as the coach team-stats endpoint (mean of
        // per-athlete percentages, not attempt-weighted) so the two screens agree.
        List<Integer> percents = entries.stream()
                .map(TeamLeaderboardResponse.Entry::getFgPercent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Integer avgFgPercent = percents.isEmpty() ? null
                : (int) Math.round(percents.stream().mapToInt(Integer::intValue).average().orElse(0));
        int levelsPassed = entries.stream().mapToInt(TeamLeaderboardResponse.Entry::getLevelOrderIndex).sum();

        TeamLeaderboardResponse leaderboard = TeamLeaderboardResponse.builder()
                .athleteCount(entries.size())
                .avgFgPercent(avgFgPercent)
                .levelsPassed(levelsPassed)
                .entries(entries)
                .build();

        return Response.ok(new GenericApiResponse<>(200, "Success", leaderboard)).build();
    }

    @GET
    @Path("/{teamId}/join-code")
    @RolesAllowed({"ADMIN", "COACH"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJoinCode(@PathParam("teamId") UUID teamId) {
        return teamService.getJoinCode(teamId)
                .map(code -> Response.ok(Map.of("joinCode", code)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "No join code found for this team"))
                        .build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "COACH"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createTeam(Map<String, String> body) {
        String teamName = body != null ? body.get("name") : null;
        if (teamName == null || teamName.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Team name is required"))
                    .build();
        }
        CurrentUser currentUser = userService.getCurrentUser();
        Map<String, String> result = teamService.createTeamWithJoinCode(teamName, currentUser.getId());
        return Response.ok(result).build();
    }

    @POST
    @Path("/join")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response joinTeam(JoinTeamRequest request) {
        if (request == null || request.getJoinCode() == null || request.getJoinCode().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "joinCode is required"))
                    .build();
        }
        CurrentUser currentUser = userService.getCurrentUser();
        try {
            return teamService.joinTeamByCode(currentUser.getId(), request.getJoinCode())
                    .map(team -> Response.ok(team).build())
                    .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Invalid join code"))
                            .build());
        } catch (IllegalStateException e) {
            if ("ALREADY_ON_TEAM".equals(e.getMessage())) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("error", "You are already on a team"))
                        .build();
            }
            throw e;
        }
    }

    @DELETE
    @Path("/{teamId}/member/{userId}")
    @RolesAllowed({"ADMIN", "COACH", "ATHLETE"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeTeamMember(@PathParam("teamId") UUID teamId, @PathParam("userId") UUID userId) {
        CurrentUser currentUser = userService.getCurrentUser();
        // Athletes can only remove themselves; coaches can only remove members of
        // a team they themselves belong to; admins can remove anyone.
        String role = currentUser.getRole();
        if ("ATHLETE".equalsIgnoreCase(role) && !currentUser.getId().equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Athletes can only remove themselves from a team"))
                    .build();
        }
        if ("COACH".equalsIgnoreCase(role)
                && !currentUser.getId().equals(userId)
                && !teamService.isUserOnTeam(teamId, currentUser.getId())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Coaches can only remove members of their own team"))
                    .build();
        }
        boolean removed = teamService.removeUserFromTeam(teamId, userId);
        if (removed) {
            return Response.ok(Map.of("message", "Member removed from team")).build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Member not found on this team"))
                .build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTeam(TeamPartial teamPartial) {
        return Response.ok(teamService.update(teamPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllTeams(@Context UriInfo uriInfo) {
        return Response.ok(teamService.findAll(new FindOptions(uriInfo))).build();
    }

}
