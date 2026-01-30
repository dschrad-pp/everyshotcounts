package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.SystemPropertiesPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.SystemPropertiesRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.SystemPropertiesService;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

// THis is generated code. Please remove this comment if you modify.

@Path("/api/system_properties")
public class SystemPropertiesResource {
    private static Logger logger = LoggerFactory.getLogger(SystemPropertiesResource.class);

    @Inject
    SystemPropertiesService systemPropertiesService;

    @GET
    @Path("/{systemPropertiesId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystemProperties(@PathParam("systemPropertiesId") UUID systemPropertiesId) {
        return systemPropertiesService.findById(systemPropertiesId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSystemProperties(SystemPropertiesPartial systemPropertiesPartial) {
        return Response.ok(systemPropertiesService.create(systemPropertiesPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSystemProperties(SystemPropertiesPartial systemPropertiesPartial) {
        return Response.ok(systemPropertiesService.update(systemPropertiesPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSystemPropertiess(@Context UriInfo uriInfo) {
        return Response.ok(systemPropertiesService.findAll(new FindOptions(uriInfo))).build();
    }

}
