package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsMemberActionPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsMemberActionRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.LeagueAppsMemberActionService;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

// THis is generated code. Please remove this comment if you modify.

@Path("/api/league_apps_member_action")
public class LeagueAppsMemberActionResource {
    private static Logger logger = LoggerFactory.getLogger(LeagueAppsMemberActionResource.class);

    @Inject
    LeagueAppsMemberActionService leagueAppsMemberActionService;

    @GET
    @Path("/{leagueAppsMemberActionId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLeagueAppsMemberAction(@PathParam("leagueAppsMemberActionId") long leagueAppsMemberActionId) {
        return leagueAppsMemberActionService.findById(leagueAppsMemberActionId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLeagueAppsMemberAction(LeagueAppsMemberActionPartial leagueAppsMemberActionPartial) {
        return Response.ok(leagueAppsMemberActionService.create(leagueAppsMemberActionPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLeagueAppsMemberAction(LeagueAppsMemberActionPartial leagueAppsMemberActionPartial) {
        return Response.ok(leagueAppsMemberActionService.update(leagueAppsMemberActionPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllLeagueAppsMemberActions(@Context UriInfo uriInfo) {
        return Response.ok(leagueAppsMemberActionService.findAll(new FindOptions(uriInfo))).build();
    }

}
