package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeEntryVotePartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.ChallengeEntryVoteService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Path("/api/challenge_entry_vote")
public class ChallengeEntryVoteResource {
    private static Logger logger = LoggerFactory.getLogger(ChallengeEntryVoteResource.class);

    @Inject
    ChallengeEntryVoteService challengeEntryVoteService;

    @GET
    @Path("/{challengeEntryVoteId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChallengeEntryVote(@PathParam("challengeEntryVoteId") UUID challengeEntryVoteId) {
        return challengeEntryVoteService.findById(challengeEntryVoteId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createChallengeEntryVote(ChallengeEntryVotePartial challengeEntryVotePartial) {
        return Response.ok(challengeEntryVoteService.create(challengeEntryVotePartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateChallengeEntryVote(ChallengeEntryVotePartial challengeEntryVotePartial) {
        return Response.ok(challengeEntryVoteService.update(challengeEntryVotePartial)).build();
    }


}
