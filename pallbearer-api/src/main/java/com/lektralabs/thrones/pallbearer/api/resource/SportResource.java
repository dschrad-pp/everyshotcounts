package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SportPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.service.SportService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

// This is generated code. Please remove this comment if you modify.

@Path("/api/sport")
public class SportResource {
    private static Logger logger = LoggerFactory.getLogger(SportResource.class);

    @Inject
    SportService sportService;

    @GET
    @Path("/{sportId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSport(@PathParam("sportId") UUID sportId) {
        return sportService.findById(sportId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSport(SportPartial sportPartial) {
        return Response.ok(sportService.create(sportPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSport(SportPartial sportPartial) {
        return Response.ok(sportService.update(sportPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSports(@Context UriInfo uriInfo) {
        return Response.ok(sportService.findAll(new FindOptions(uriInfo))).build();
    }

}
