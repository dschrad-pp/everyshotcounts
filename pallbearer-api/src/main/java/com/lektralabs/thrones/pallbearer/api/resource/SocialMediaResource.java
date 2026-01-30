package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SocialMediaPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.service.SocialMediaService;
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

@Path("/api/social_media")
public class SocialMediaResource {
    private static Logger logger = LoggerFactory.getLogger(SocialMediaResource.class);

    @Inject
    SocialMediaService socialMediaService;

    @GET
    @Path("/{socialMediaId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSocialMedia(@PathParam("socialMediaId") UUID socialMediaId) {
        return socialMediaService.findById(socialMediaId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSocialMedia(SocialMediaPartial socialMediaPartial) {
        return Response.ok(socialMediaService.create(socialMediaPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSocialMedia(SocialMediaPartial socialMediaPartial) {
        return Response.ok(socialMediaService.update(socialMediaPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSocialMedias(@Context UriInfo uriInfo) {
        return Response.ok(socialMediaService.findAll(new FindOptions(uriInfo))).build();
    }

}
