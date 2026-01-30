package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeParticipantPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.ChallengeParticipantService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Path("/api/challenge_participant")
public class ChallengeParticipantResource {
    private static Logger logger = LoggerFactory.getLogger(ChallengeParticipantResource.class);

    @Inject
    ChallengeParticipantService challengeParticipantService;

    @GET
    @Path("/{challengeParticipantId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChallengeParticipant(@PathParam("challengeParticipantId") UUID challengeParticipantId) {
        return challengeParticipantService.findById(challengeParticipantId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createChallengeParticipant(ChallengeParticipantPartial challengeParticipantPartial) {
        return Response.ok(challengeParticipantService.create(challengeParticipantPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateChallengeParticipant(ChallengeParticipantPartial challengeParticipantPartial) {
        return Response.ok(challengeParticipantService.update(challengeParticipantPartial)).build();
    }


}
