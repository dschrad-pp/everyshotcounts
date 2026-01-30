package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteService;
import com.lektralabs.thrones.pallbearer.security.CurrentUserUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Path("/api/athlete")
public class AthleteResource {

    private static Logger logger = LoggerFactory.getLogger(AthleteResource.class);

    @Inject
    AthleteService athleteService;

    @Inject
    CurrentUserUtils currentUserUtils;

    @GET
    @Path("/{athleteId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER", "COACH"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response findByAthleteId(@PathParam("athleteId") UUID athleteId) {
        return athleteService.findByAthleteId(athleteId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("/featured")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFeaturedAthletes() {
        UUID currentUserId = currentUserUtils.getCurrentUserId();
        List<AthleteDetail> results = athleteService.findFeaturedAthletes(currentUserId);
        return Response.ok(results).build();
    }

    /**
     * Get athletes matching the find options provided in URI parameters
     *
     * @param uriInfo URI parameters
     * @return Athlete details
     */
    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAthletes(@Context UriInfo uriInfo) {
        return Response.ok(athleteService.findAll(new FindOptions(uriInfo))).build();
    }
}
