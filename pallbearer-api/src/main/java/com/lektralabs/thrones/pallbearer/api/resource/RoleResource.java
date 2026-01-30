package com.lektralabs.thrones.pallbearer.api.resource;

import jakarta.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/role")
public class RoleResource {
    private static Logger logger = LoggerFactory.getLogger(RoleResource.class);

//    @Inject
//    RoleService roleService;
//
//    @GET
//    @Path("/{roleId}")
//    @RolesAllowed({"ADMIN"})
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response getRole(@PathParam("roleId") UUID roleId) {
//        return roleService.findById(roleId)
//                .map(row -> Response.ok(row).build())
//                .orElseGet(() -> Response.noContent().build());
//    }
//
//    @POST
//    @Path("/")
//    @RolesAllowed({"ADMIN"})
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response createRole(RolePartial rolePartial) {
//        return Response.ok(roleService.create(rolePartial)).build();
//    }
//
//    @PUT
//    @Path("/")
//    @RolesAllowed({"ADMIN"})
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response updateRole(RolePartial rolePartial) {
//        return Response.ok(roleService.update(rolePartial)).build();
//    }
//

}
