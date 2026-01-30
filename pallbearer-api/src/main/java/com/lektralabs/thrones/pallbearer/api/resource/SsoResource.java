package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.keycloak.OpenIdResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.LoginUser;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.security.Authenticated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;

import jakarta.ws.rs.HeaderParam;

@Path("/api/sso")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SsoResource {

    private static Logger logger = LoggerFactory.getLogger(SsoResource.class);

    @Inject
    KeycloakProvider keycloakProvider;

    @Path("/login")
    @POST
    public Response login(LoginUser loginUser) {
        try {
            logger.info("Recieved user request : {}", loginUser.getUsername());
            OpenIdResponse openIdResponse = keycloakProvider.getUserAccessToken(loginUser.getUsername(),
                    loginUser.getPassword());
            return Response.ok(openIdResponse).build();
        } catch (Exception e) {
            logger.warn("Error in current", e);
            return Response.status(Response.Status.FORBIDDEN.getStatusCode()).
                    entity("You shall not pass").build();
        }
    }

    @Path("/logout")
    @POST
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@HeaderParam("Authorization") String authorizationHeader) {
        try {
            // Validate Authorization header
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                logger.warn("Invalid or missing Authorization header");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new GenericApiResponse<>(
                                Response.Status.BAD_REQUEST.getStatusCode(),
                                "Invalid or missing Authorization header",
                                null))
                        .build();
            }

            String token = authorizationHeader.substring("Bearer ".length()).trim();
            logger.info("Initiating logout for user token");

            // Perform logout
            keycloakProvider.logoutUser(token);

            return Response.ok()
                    .entity(new GenericApiResponse<>(
                            Response.Status.OK.getStatusCode(),
                            "Logged out successfully",
                            null))
                    .build();

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid logout request: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new GenericApiResponse<>(
                            Response.Status.BAD_REQUEST.getStatusCode(),
                            e.getMessage(),
                            null))
                    .build();
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(
                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            "Logout failed",
                            null))
                    .build();
        }
    }
}
