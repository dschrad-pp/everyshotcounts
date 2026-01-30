package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LeagueAppsRegistrationPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.LeagueAppsRegistrationRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.LeagueAppsRegistrationService;
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

@Path("/api/league_apps_registration")
public class LeagueAppsRegistrationResource {
    private static Logger logger = LoggerFactory.getLogger(LeagueAppsRegistrationResource.class);

    @Inject
    LeagueAppsRegistrationService leagueAppsRegistrationService;

    @GET
    @Path("/{leagueAppsRegistrationId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLeagueAppsRegistration(@PathParam("leagueAppsRegistrationId") long leagueAppsRegistrationId) {
        return leagueAppsRegistrationService.findById(leagueAppsRegistrationId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLeagueAppsRegistration(LeagueAppsRegistrationPartial leagueAppsRegistrationPartial) {
        return Response.ok(leagueAppsRegistrationService.create(leagueAppsRegistrationPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLeagueAppsRegistration(LeagueAppsRegistrationPartial leagueAppsRegistrationPartial) {
        return Response.ok(leagueAppsRegistrationService.update(leagueAppsRegistrationPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllLeagueAppsRegistrations(@Context UriInfo uriInfo) {
        return Response.ok(leagueAppsRegistrationService.findAll(new FindOptions(uriInfo))).build();
    }

}
