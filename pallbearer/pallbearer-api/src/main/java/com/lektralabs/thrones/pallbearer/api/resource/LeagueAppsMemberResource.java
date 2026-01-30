package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsMemberPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsMemberRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.LeagueAppsMemberService;
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

@Path("/api/league_apps_member")
public class LeagueAppsMemberResource {
    private static Logger logger = LoggerFactory.getLogger(LeagueAppsMemberResource.class);

    @Inject
    LeagueAppsMemberService leagueAppsMemberService;

    @GET
    @Path("/{leagueAppsMemberId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLeagueAppsMember(@PathParam("leagueAppsMemberId") long leagueAppsMemberId) {
        return leagueAppsMemberService.findById(leagueAppsMemberId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLeagueAppsMember(LeagueAppsMemberPartial leagueAppsMemberPartial) {
        return Response.ok(leagueAppsMemberService.create(leagueAppsMemberPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLeagueAppsMember(LeagueAppsMemberPartial leagueAppsMemberPartial) {
        return Response.ok(leagueAppsMemberService.update(leagueAppsMemberPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllLeagueAppsMembers(@Context UriInfo uriInfo) {
        return Response.ok(leagueAppsMemberService.findAll(new FindOptions(uriInfo))).build();
    }

}
