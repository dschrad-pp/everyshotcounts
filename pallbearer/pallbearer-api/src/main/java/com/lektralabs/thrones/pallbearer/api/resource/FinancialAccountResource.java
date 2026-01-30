package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.FinancialAccountPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.service.FinancialAccountService;
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

@Path("/api/financial_account")
public class FinancialAccountResource {
    private static Logger logger = LoggerFactory.getLogger(FinancialAccountResource.class);

    @Inject
    FinancialAccountService financialAccountService;

    @GET
    @Path("/{financialAccountId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFinancialAccount(@PathParam("financialAccountId") UUID financialAccountId) {
        return financialAccountService.findById(financialAccountId)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFinancialAccount(FinancialAccountPartial financialAccountPartial) {
        return Response.ok(financialAccountService.create(financialAccountPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFinancialAccount(FinancialAccountPartial financialAccountPartial) {
        return Response.ok(financialAccountService.update(financialAccountPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllFinancialAccounts(@Context UriInfo uriInfo) {
        return Response.ok(financialAccountService.findAll(new FindOptions(uriInfo))).build();
    }

}
