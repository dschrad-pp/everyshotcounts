package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.ChallengeDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.ChallengeDetailService;
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

@Path("/api/challenge/detail")
public class ChallengeDetailResource {

    private static Logger logger = LoggerFactory.getLogger(ChallengeDetailResource.class);

    @Inject
    ChallengeDetailService challengeDetailService;

    /**
     * Get the challenge detail for the challenge associated with the specified
     * challenge ID
     * @param challengeId Challenge ID
     * @return Challenge detail or not found
     */
    @GET
    @Path("/{challengeId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response findByChallengeId(@PathParam("challengeId") UUID challengeId) {
        return challengeDetailService.findByChallengeId(challengeId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    /**
     * Find all currently featured challenges
     * @return Challenge details
     */
    @GET
    @Path("/featured")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFeaturedChallenges() {
        List<ChallengeDetail> results = challengeDetailService.findFeaturedChallenges();
        return Response.ok(results).build();
    }

    /**
     * Get all challenges owned by the specified athlete, regardless of the
     * state of the challenge
     * @param athleteUserId Challenge creation user ID
     * @return Challenge details
     */
    @GET
    @Path("/athlete/{athleteUserId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthleteChallenges(@PathParam("athleteUserId") UUID athleteUserId) {
        List<ChallengeDetail> results = challengeDetailService.findWithAthlete(athleteUserId);
        return Response.ok(results).build();
    }

    /**
     * Get the featured challenge owned by the specified athlete, regardless of
     * the state of the challenge
     * @param athleteUserId Challenge creation user ID
     * @return Challenge details
     */
    @GET
    @Path("/athlete/{athleteUserId}/featured")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFeaturedAthleteChallenge(@PathParam("athleteUserId") UUID athleteUserId) {
        List<ChallengeDetail> results = challengeDetailService.findWithAthlete(athleteUserId);
        return challengeDetailService.findFeaturedAthleteChallenge(athleteUserId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    /**
     * Get all challenges owned by the specified athlete, where the user has
     * initiated the challenge. A challenge is initiated when the athlete
     * creates an entry
     * @param athleteUserId Challenge creation user ID
     * @return Challenge details
     */
    @GET
    @Path("/athlete/{athleteUserId}/initiated")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInitiatedAthleteChallenges(@PathParam("athleteUserId") UUID athleteUserId) {
        List<ChallengeDetail> results = challengeDetailService.findWithAthlete(athleteUserId)
                .stream().filter(c -> c.getChallengeEntryItem() != null)
                .toList();
        return Response.ok(results).build();
    }

    /**
     * Get all challenges that the fan is participating in. The fan is
     * participating in a challenge when they have created a challenge entry
     * @param fanUserId Fan user ID
     * @return Challenge details
     */
    @GET
    @Path("/fan/{fanUserId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFanChallenges(@PathParam("fanUserId") UUID fanUserId) {
        List<ChallengeDetail> results = challengeDetailService.findWithFan(fanUserId);
        return Response.ok(results).build();
    }
}
