package com.lektralabs.thrones.pallbearer.api.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/admin")
public class AdminResource {

    @GET
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_PLAIN)
    public String adminResource() {
        return "I'm a teapot";
    }
}