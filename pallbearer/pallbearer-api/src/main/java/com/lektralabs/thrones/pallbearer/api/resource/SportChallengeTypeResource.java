package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SportChallengeTypePartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.service.SportChallengeTypeService;
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

@Path("/api/sport_challenge_type")
public class SportChallengeTypeResource {
    private static Logger logger = LoggerFactory.getLogger(SportChallengeTypeResource.class);

    @Inject
    SportChallengeTypeService sportChallengeTypeService;

    @GET
    @Path("/{sportChallengeTypeId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSportChallengeType(@PathParam("sportChallengeTypeId") UUID sportChallengeTypeId) {
        return sportChallengeTypeService.findById(sportChallengeTypeId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSportChallengeType(SportChallengeTypePartial sportChallengeTypePartial) {
        return Response.ok(sportChallengeTypeService.create(sportChallengeTypePartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSportChallengeType(SportChallengeTypePartial sportChallengeTypePartial) {
        return Response.ok(sportChallengeTypeService.update(sportChallengeTypePartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSportChallengeTypes(@Context UriInfo uriInfo) {
        return Response.ok(sportChallengeTypeService.findAll(new FindOptions(uriInfo))).build();
    }

}
