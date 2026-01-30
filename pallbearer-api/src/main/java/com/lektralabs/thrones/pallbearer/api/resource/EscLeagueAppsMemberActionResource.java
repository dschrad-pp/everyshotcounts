package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.EscLeagueAppsMemberActionPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.EscLeagueAppsMemberActionRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.EscLeagueAppsMemberActionService;
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

@Path("/api/esc_league_apps_member_action")
public class EscLeagueAppsMemberActionResource {
    private static Logger logger = LoggerFactory.getLogger(EscLeagueAppsMemberActionResource.class);

    @Inject
    EscLeagueAppsMemberActionService escLeagueAppsMemberActionService;

    @GET
    @Path("/{escLeagueAppsMemberActionId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEscLeagueAppsMemberAction(@PathParam("escLeagueAppsMemberActionId") UUID escLeagueAppsMemberActionId) {
        return escLeagueAppsMemberActionService.findById(escLeagueAppsMemberActionId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEscLeagueAppsMemberAction(EscLeagueAppsMemberActionPartial escLeagueAppsMemberActionPartial) {
        return Response.ok(escLeagueAppsMemberActionService.create(escLeagueAppsMemberActionPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEscLeagueAppsMemberAction(EscLeagueAppsMemberActionPartial escLeagueAppsMemberActionPartial) {
        return Response.ok(escLeagueAppsMemberActionService.update(escLeagueAppsMemberActionPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllEscLeagueAppsMemberActions(@Context UriInfo uriInfo) {
        return Response.ok(escLeagueAppsMemberActionService.findAll(new FindOptions(uriInfo))).build();
    }

}
