package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.leagueapps.LeagueAppsIntegration;
import com.lektralabs.thrones.pallbearer.jdbi.model.item.RegistrationItem;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Optional;

@Path("/api/league_apps_integration")
public class LeagueAppsIntegrationResource {

    @Inject
    LeagueAppsIntegration leagueAppsIntegration;

    @GET
    @Path("/integrate")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllLeagueAppsMemberActions(@Context UriInfo uriInfo) {
        leagueAppsIntegration.update();
        leagueAppsIntegration.process();
        return Response.ok().build();
    }

    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerNewMember(@Context UriInfo uriInfo, RegistrationItem registrationItem) {
        leagueAppsIntegration.register(registrationItem);
        return Response.ok().build();
    }

    @GET
    @Path("/verify-credentials")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyCredentials(@Context UriInfo uriInfo) {
        return leagueAppsIntegration.verifyCredentials();
    }

    @GET
    @Path("/all-users")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsersFromLeagueApps(@Context UriInfo uriInfo) {
        return leagueAppsIntegration.getAllUsersFromLeagueApps();
    }

    @GET
    @Path("/user/{userId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserFromLeagueApps(@PathParam("userId") long userId, @Context UriInfo uriInfo) {
        return leagueAppsIntegration.getUserFromLeagueApps(userId);
    }

    @GET
    @Path("/user-by-username/{username}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserByUsernameFromLeagueApps(@PathParam("username") String username, @Context UriInfo uriInfo) {
        return leagueAppsIntegration.getUserByUsernameFromLeagueApps(username);
    }

    @GET
    @Path("/payment-status/user-id/{userId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPaymentStatusByUserId(@PathParam("userId") long userId, @Context UriInfo uriInfo) {
        return leagueAppsIntegration.getPaymentStatusByUserId(userId);
    }

    @GET
    @Path("/payment-status/username/{username}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPaymentStatusByUsername(@PathParam("username") String username, @Context UriInfo uriInfo) {
        return leagueAppsIntegration.getPaymentStatusByUsername(username);
    }

    @POST
    @Path("/sync-complete")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncComplete(
            @QueryParam("lastUpdated") Optional<Long> lastUpdatedTimestamp,
            @Context UriInfo uriInfo) {
        return leagueAppsIntegration.syncComplete(lastUpdatedTimestamp);
    }

    @GET
    @Path("/member/{userId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMemberByUserId(@PathParam("userId") long userId, @Context UriInfo uriInfo) {
        return leagueAppsIntegration.getMemberByUserId(userId);
    }

    @GET
    @Path("/basic/users")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBasicUsers(@Context UriInfo uriInfo) {
        return leagueAppsIntegration.listBasicLeagueAppsUsers();
    }

    @GET
    @Path("/basic/members")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBasicMembers(@Context UriInfo uriInfo) {
        return leagueAppsIntegration.listBasicMembers();
    }

    @GET
    @Path("/basic/registrations")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBasicRegistrations(@Context UriInfo uriInfo) {
        return leagueAppsIntegration.listBasicRegistrations();
    }

    @GET
    @Path("/basic/db-users")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBasicDbUsers(@Context UriInfo uriInfo) {
        return leagueAppsIntegration.listBasicUsersFromDb();
    }

}
