package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.UserPropertyPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserPropertyService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserPropertyRow;
import java.util.UUID;

@Path("/api/user_property")
public class UserPropertyResource {

    private static Logger logger = LoggerFactory.getLogger(UserPropertyResource.class);

    @Inject
    UserPropertyService userPropertyService;

    @GET
    @Path("/{userPropertyId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProperty(@PathParam("userPropertyId") UUID userPropertyId) {
        return userPropertyService.findById(userPropertyId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUserProperty(UserPropertyPartial userPropertyPartial) {
        return Response.ok(userPropertyService.create(userPropertyPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserProperty(UserPropertyPartial userPropertyPartial) {
        return Response.ok(userPropertyService.update(userPropertyPartial)).build();
    }
}
