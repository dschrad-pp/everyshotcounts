package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.partial.generated.TeamPartial;
import com.lektralabs.thrones.pallbearer.api.model.request.JoinTeamRequest;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
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

import java.util.Map;
import java.util.UUID;

@Path("/api/team")
public class TeamResource {
    private static Logger logger = LoggerFactory.getLogger(TeamResource.class);

    @Inject
    TeamService teamService;

    @Inject
    UserService userService;

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
        // Athletes can only remove themselves — coaches/admins can remove anyone
        boolean isAthlete = "ATHLETE".equalsIgnoreCase(currentUser.getRole());
        if (isAthlete && !currentUser.getId().equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Athletes can only remove themselves from a team"))
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
