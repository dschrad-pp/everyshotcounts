package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.UserGroupPropertyPartial;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserGroupPropertyRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserGroupPropertyService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.representations.idm.GroupRepresentation;

@Path("/api/group_property")
public class UserGroupPropertyResource {

    @Inject
    UserGroupPropertyService userGroupPropertyService;

    @GET
    @Path("/{propertyId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupProperty(@PathParam("propertyId") UUID propertyId) {
        Optional<UserGroupPropertyRow> result = userGroupPropertyService.findById(propertyId);
        return result.map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("/user/{userId}/group/{groupId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupPropertiesForUser(@PathParam("userId") UUID userId,
            @PathParam("groupId") UUID groupId) {
        List<UserGroupPropertyRow> props = userGroupPropertyService.findByUserAndGroup(userId, groupId);
        if (props == null || props.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(props).build();
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGroupProperty(UserGroupPropertyPartial groupPropertyPartial) {
        UUID created = userGroupPropertyService.create(groupPropertyPartial);
        return Response.ok(groupPropertyPartial).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateGroupProperty(UserGroupPropertyPartial groupPropertyPartial) {
        int updated = userGroupPropertyService.update(groupPropertyPartial);
        return Response.ok(updated).build();
    }
}
