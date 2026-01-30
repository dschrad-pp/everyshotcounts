package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.LikePartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.service.LikeService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

// This is generated code. Please remove this comment if you modify.

@Path("/api/like")
public class LikeResource {
    private static Logger logger = LoggerFactory.getLogger(LikeResource.class);

    @Inject
    LikeService likeService;

    @GET
    @Path("/{likeId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLike(@PathParam("likeId") UUID likeId) {
        return likeService.findById(likeId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLike(LikePartial likePartial) {
        return Response.ok(likeService.create(likePartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLike(LikePartial likePartial) {
        return Response.ok(likeService.update(likePartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllLikes(@Context UriInfo uriInfo) {
        return Response.ok(likeService.findAll(new FindOptions(uriInfo))).build();
    }

}
