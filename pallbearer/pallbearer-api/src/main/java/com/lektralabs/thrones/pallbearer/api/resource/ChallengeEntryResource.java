package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.ChallengeEntryPartial;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.ChallengeEntryRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.ChallengeEntryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Path("/api/challenge_entry")
public class ChallengeEntryResource {

    private static Logger logger = LoggerFactory.getLogger(ChallengeEntryResource.class);

    @Inject
    ChallengeEntryService challengeEntryService;

    @GET
    @Path("/{challengeEntryId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChallengeEntry(@PathParam("challengeEntryId") UUID challengeEntryId) {
        return challengeEntryService.findById(challengeEntryId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("/challenge/{challengeId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChallengeEntries(@PathParam("challengeId") UUID challengeId) {
        List<ChallengeEntryRow> results = challengeEntryService.findByChallenge(challengeId);
        return Response.ok(results).build();
    }

    @GET
    @Path("/challenge/{challengeId}/user/{userId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserChallengeEntry(@PathParam("challengeId") UUID challengeId,
                                          @PathParam("userId") UUID userId) {
        return challengeEntryService.findByChallengeAndUser(challengeId, userId)
                .map(row -> Response.ok(row).build())
                        .orElseGet(() -> Response.noContent().build());

    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createChallengeEntry(ChallengeEntryPartial challengeEntryPartial) {
        return Response.ok(challengeEntryService.create(challengeEntryPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateChallengeEntry(ChallengeEntryPartial challengeEntryPartial) {
        return Response.ok(challengeEntryService.update(challengeEntryPartial)).build();
    }


}
