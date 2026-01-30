package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeEntryDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.ChallengeEntryDetailService;
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

@Path("/api/challenge_entry/detail")
public class ChallengeEntryDetailResource {

    private static Logger logger = LoggerFactory.getLogger(ChallengeEntryDetailResource.class);

    @Inject
    ChallengeEntryDetailService challengeEntryDetailService;

    /**
     * Get the challenge entry detail for the challenge entry associated with
     * the specified challenge entry ID
     * @param challengeEntryId Challenge entry ID
     * @return Challenge entry detail or not found
     */
    @GET
    @Path("/{challengeEntryId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response findByChallengeEntryId(@PathParam("challengeEntryId") UUID challengeEntryId) {
        return challengeEntryDetailService.findByChallengeEntryId(challengeEntryId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    /**
     * Get all challenge entry details corresponding to the specified challenge
     * @param challengeId Challenge ID
     * @return Challenge entry detail
     */
    @GET
    @Path("/challenge/{challengeId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChallengeEntries(@PathParam("challengeId") UUID challengeId) {
        List<ChallengeEntryDetail> results = challengeEntryDetailService.findByChallengeId(challengeId);
        return Response.ok(results).build();
    }
}
