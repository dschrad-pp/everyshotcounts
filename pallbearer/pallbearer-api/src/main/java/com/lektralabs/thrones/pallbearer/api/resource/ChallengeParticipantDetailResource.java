package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeParticipantDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.ChallengeParticipantDetailService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Path("/api/challenge_participant/detail")
public class ChallengeParticipantDetailResource {

    private static Logger logger = LoggerFactory.getLogger(ChallengeParticipantDetail.class);

    @Inject
    ChallengeParticipantDetailService challengeParticipantDetailService;

    @GET
    @Path("/challenge/{challengeId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByChallengeId(@PathParam("challengeId") UUID challengeId) {
        List<ChallengeParticipantDetail> results = challengeParticipantDetailService.selectByChallengeId(challengeId);
        return Response.ok(results).build();
    }
}

