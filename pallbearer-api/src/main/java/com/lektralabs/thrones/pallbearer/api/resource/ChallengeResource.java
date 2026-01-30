package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengePartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.ChallengeService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Path("/api/challenge")
public class ChallengeResource {
    private static Logger logger = LoggerFactory.getLogger(ChallengeResource.class);

    @Inject
    ChallengeService challengeService;

    @GET
    @Path("/{challengeId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChallenge(@PathParam("challengeId") UUID challengeId) {
        return challengeService.findById(challengeId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createChallenge(ChallengePartial challengePartial) {
        return Response.ok(challengeService.create(challengePartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateChallenge(ChallengePartial challengePartial) {
        return Response.ok(challengeService.update(challengePartial)).build();
    }


}
