package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.AddressPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.service.AddressService;
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

@Path("/api/address")
public class AddressResource {
    private static Logger logger = LoggerFactory.getLogger(AddressResource.class);

    @Inject
    AddressService addressService;

    @GET
    @Path("/{addressId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAddress(@PathParam("addressId") UUID addressId) {
        return addressService.findById(addressId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAddress(AddressPartial addressPartial) {
        return Response.ok(addressService.create(addressPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAddress(AddressPartial addressPartial) {
        return Response.ok(addressService.update(addressPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAddresss(@Context UriInfo uriInfo) {
        return Response.ok(addressService.findAll(new FindOptions(uriInfo))).build();
    }

}
