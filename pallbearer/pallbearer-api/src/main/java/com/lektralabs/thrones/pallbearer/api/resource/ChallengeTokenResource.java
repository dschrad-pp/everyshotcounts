package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeTokenPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.ChallengeTokenService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Path("/api/challenge_token")
public class ChallengeTokenResource {
    private static Logger logger = LoggerFactory.getLogger(ChallengeTokenResource.class);

    @Inject
    ChallengeTokenService challengeTokenService;

    @GET
    @Path("/{challengeTokenId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChallengeToken(@PathParam("challengeTokenId") UUID challengeTokenId) {
        return challengeTokenService.findById(challengeTokenId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createChallengeToken(ChallengeTokenPartial challengeTokenPartial) {
        return Response.ok(challengeTokenService.create(challengeTokenPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateChallengeToken(ChallengeTokenPartial challengeTokenPartial) {
        return Response.ok(challengeTokenService.update(challengeTokenPartial)).build();
    }


}
