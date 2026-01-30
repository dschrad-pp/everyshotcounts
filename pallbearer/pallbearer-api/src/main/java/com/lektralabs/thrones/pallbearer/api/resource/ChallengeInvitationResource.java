package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeInvitationPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.ChallengeInvitationService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Path("/api/challenge_invitation")
public class ChallengeInvitationResource {
    private static Logger logger = LoggerFactory.getLogger(ChallengeInvitationResource.class);

    @Inject
    ChallengeInvitationService challengeInvitationService;

    @GET
    @Path("/{challengeInvitationId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChallengeInvitation(@PathParam("challengeInvitationId") UUID challengeInvitationId) {
        return challengeInvitationService.findById(challengeInvitationId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createChallengeInvitation(ChallengeInvitationPartial challengeInvitationPartial) {
        return Response.ok(challengeInvitationService.create(challengeInvitationPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateChallengeInvitation(ChallengeInvitationPartial challengeInvitationPartial) {
        return Response.ok(challengeInvitationService.update(challengeInvitationPartial)).build();
    }


}
