package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.api.model.keycloak.OpenIdResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.GenericApiResponse;
import com.lektralabs.thrones.pallbearer.api.model.partial.RegisterUserPartial;
import com.lektralabs.thrones.pallbearer.api.model.request.ProfileUpdateRequest;
import com.lektralabs.thrones.pallbearer.datetime.DateTimeUtils;
import com.lektralabs.thrones.pallbearer.jdbi.exception.RegistrationException;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.manager.AthleteDrillGroupManager;
import com.lektralabs.thrones.pallbearer.security.KeycloakProvider;

import com.lektralabs.thrones.pallbearer.security.CurrentUserUtils;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;

import jakarta.annotation.security.PermitAll;

@Path("/api/user")
public class UserResource {

    private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

    @Inject
    AthleteDrillGroupManager athleteDrillGroupManager;
    @Inject
    KeycloakProvider keycloakProvider;
    @Inject
    SecurityIdentity keycloakSecurityContext;
    @Inject
    UserService userService;
    @Inject
    CurrentUserUtils currentUserUtils;

    @GET
    @Path("/current")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response current() {
        try {
            CurrentUser cu = userService.getCurrentUser();
            return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "User fetched successfully", cu)).build();
        } catch (Exception e) {
            logger.warn("Error in current", e);
            return Response.status(HttpStatus.SC_FORBIDDEN)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_FORBIDDEN, "Unable to fetch current user", null))
                    .build();
        }
    }

    @GET
    @Path("/subscription")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response subscription() {
        try {
            CurrentUser currentUser = userService.getCurrentUser();
            Map<String, String> userProperties = currentUser.getUserProperties();
            
            // Create a new map with only the required properties
            Map<String, String> filteredProperties = new HashMap<>();
            filteredProperties.put("group.name", userProperties.get("user.drill.group.name"));
            filteredProperties.put("drill.group", userProperties.get("user.drill.group"));
            filteredProperties.put("payment.state", userProperties.get("user.registration.payment.state"));
            filteredProperties.put("registration.state", userProperties.get("user.registration.state"));
            
            // Create response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", currentUser.getId());
            responseData.put("keycloakId", currentUser.getKeycloakId());
            responseData.put("username", currentUser.getUsername());
            responseData.put("email", currentUser.getEmail());
            responseData.put("firstName", currentUser.getFirstName());
            responseData.put("lastName", currentUser.getLastName());
            responseData.put("role", currentUser.getRole());
            responseData.put("userProperties", filteredProperties);
            
            return Response.ok(
                new GenericApiResponse<>(
                    HttpStatus.SC_OK, 
                    "User subscription data fetched successfully", 
                    responseData
                )
            ).build();
        } catch (Exception e) {
            logger.warn("Error in current", e);
            return Response.status(HttpStatus.SC_FORBIDDEN)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_FORBIDDEN, "Unable to fetch current user", null))
                    .build();
        }
    }

    @POST
    @Path("/registration/{includeKeycloak}")
    @PermitAll
    public Response registration(@PathParam("includeKeycloak") boolean includeKeycloak, RegisterUserPartial registerUserPartial) {
        try {
            userService.registerUser(registerUserPartial, includeKeycloak);
            if (includeKeycloak) {
                OpenIdResponse openIdResponse = keycloakProvider.getUserAccessToken(registerUserPartial.getUsername(), registerUserPartial.getPassword());
                return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "Registration successful with Keycloak", openIdResponse)).build();
            } else {
                return Response.status(202)
                        .entity(new GenericApiResponse<>(202, "Registration successful. Activation required.", null))
                        .build();
            }
        } catch (RegistrationException re) {
            logger.warn("Error in registration", re);
            String userFriendlyMessage = re.getUserFriendlyMessage();
            return Response.status(HttpStatus.SC_BAD_REQUEST)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_BAD_REQUEST, userFriendlyMessage, re.getMessage()))
                    .build();
        } catch (Exception e) {
            logger.warn("Error in registration", e);
            String errorMessage = "Registration failed. Please try again.";
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("keycloak")) {
                errorMessage = "Registration failed. Please check your information and try again.";
            }
            return Response.status(HttpStatus.SC_BAD_REQUEST)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_BAD_REQUEST, errorMessage, e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/activation")
    @Blocking
    @PermitAll
    public Response activation(RegisterUserPartial registerUserPartial) {
        try {
            userService.activateUser(registerUserPartial);
            OpenIdResponse openIdResponse = keycloakProvider.getUserAccessToken(registerUserPartial.getUsername(), registerUserPartial.getPassword());
            return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "User activated successfully", openIdResponse)).build();
        } catch (RegistrationException re) {
            logger.warn("Error in activation", re);
            String userFriendlyMessage = re.getUserFriendlyMessage();
            return Response.status(HttpStatus.SC_BAD_REQUEST)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_BAD_REQUEST, userFriendlyMessage, re.getMessage()))
                    .build();
        } catch (Exception e) {
            logger.warn("Error in activation", e);
            // Check if it's a Keycloak-related error
            String errorMessage = "Activation failed";
            if (e.getMessage() != null) {
                String lowerMessage = e.getMessage().toLowerCase();
                if (lowerMessage.contains("keycloak") || lowerMessage.contains("activation failed")) {
                    // Check if it's a nested RegistrationException
                    Throwable cause = e.getCause();
                    if (cause instanceof RegistrationException) {
                        errorMessage = ((RegistrationException) cause).getUserFriendlyMessage();
                    } else {
                        errorMessage = "Account activation failed. Please check your information and try again.";
                    }
                } else {
                    errorMessage = e.getMessage();
                }
            }
            return Response.status(HttpStatus.SC_BAD_REQUEST)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_BAD_REQUEST, errorMessage, e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/profile")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(ProfileUpdateRequest request) {
        try {
            UUID userId = currentUserUtils.getCurrentUserId();
            userService.updateMetadata(userId, request.getMetadata());
            CurrentUser cu = userService.getCurrentUser();
            return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "Profile updated successfully", cu)).build();
        } catch (Exception e) {
            logger.warn("Error in updateProfile", e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Profile update failed", null))
                    .build();
        }
    }

    @POST
    @Path("/update")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    public Response updateUser(UserRow userRow) {
        try {
            userService.update(userRow);
            return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "User updated successfully", null)).build();
        } catch (Exception e) {
            logger.warn("Error in updateUser", e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_INTERNAL_SERVER_ERROR, "User update failed", null))
                    .build();
        }
    }

    @GET
    @Path("/{userId}/avatar")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    public Response getUserAvatar(@PathParam("userId") String userId) {
        return userService.findById(UUID.fromString(userId)).map(userRow -> {
            try {
                if (userRow.getAvatarMimeType() == null) {
                    byte[] imageBytes = this.getClass().getClassLoader().getResourceAsStream("/default_small.png").readAllBytes();
                    return Response.ok(new ByteArrayInputStream(imageBytes), "image/png").build();
                } else {
                    return Response.ok(new ByteArrayInputStream(userRow.getAvatarByteArray()), userRow.getAvatarMimeType()).build();
                }
            } catch (IOException ioe) {
                logger.warn("Error in getUserAvatar", ioe);
                return Response.status(HttpStatus.SC_NOT_FOUND)
                        .entity(new GenericApiResponse<>(HttpStatus.SC_NOT_FOUND, "No avatar found", null))
                        .build();
            }
        }).orElseGet(() -> Response.status(HttpStatus.SC_NOT_FOUND)
                .entity(new GenericApiResponse<>(HttpStatus.SC_NOT_FOUND, "User not found", null))
                .build());
    }

    @POST
    @Blocking
    @Path("/forgot/password")
    public Response forgotPassword(@QueryParam("email") String email) {
        try {
            userService.forgotPasswordEmail(email);
            return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "Password reset email sent", null)).build();
        } catch (Exception e) {
            logger.warn("Error in forgotPassword", e);
            return Response.status(HttpStatus.SC_NOT_FOUND)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_NOT_FOUND, "Email not found", null))
                    .build();
        }
    }

    @POST
    @Blocking
    @Path("/forgot/username")
    public Response forgotUsername(@QueryParam("email") String email) {
        try {
            userService.forgotUsernameEmail(email);
            return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "Username email sent", null)).build();
        } catch (Exception e) {
            logger.warn("Error in forgotUsername", e);
            return Response.status(HttpStatus.SC_NOT_FOUND)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_NOT_FOUND, "Email not found", null))
                    .build();
        }
    }

    @POST
    @Blocking
    @Path("/{userId}/change/username")
    public Response changeUsername(@PathParam("userId") UUID userId, @QueryParam("username") String username) {
        try {
            userService.changeUsername(userId, username);
            return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "Username changed successfully", null)).build();
        } catch (Exception e) {
            logger.warn("Error in changeUsername", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_BAD_REQUEST, "Username change failed", null))
                    .build();
        }
    }

    @POST
    @Blocking
    @Path("/{userId}/change/password")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    public Response changePassword(@PathParam("userId") UUID userId, @QueryParam("password") String password) {
        try {
            userService.changePassword(userId, password);
            return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "Password changed successfully", null)).build();
        } catch (Exception e) {
            logger.warn("Error in changePassword", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_BAD_REQUEST, e.getMessage(), e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{userId}/drill_group/{drillGroupId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    public Response changeDrillGroup(@PathParam("userId") UUID userId, @PathParam("drillGroupId") UUID drillGroupId) {
        try {
            athleteDrillGroupManager.changeDrillGroup(userId, drillGroupId);
            CurrentUser cu = userService.getCurrentUser();
            return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "Drill group changed successfully", cu)).build();
        } catch (Exception e) {
            logger.error("Error in changeDrillGroup", e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Drill group change failed", null))
                    .build();
        }
    }

    @POST
    @Path("/send-otp")
    @Blocking
    @PermitAll
    public Response sendEmailOtp(@QueryParam("email") String email) {
        try {
            userService.sendEmailVerificationOTP(email);
            return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "OTP sent to email successfully", null)).build();
        } catch (Exception e) {
            logger.warn("Error in sendEmailOtp", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_BAD_REQUEST, e.getMessage(), null))
                    .build();
        }
    }

    @POST
    @Path("/verify-otp")
    @Blocking
    @PermitAll
    public Response verifyEmailOtp(@QueryParam("email") String email, @QueryParam("otp") String otp) {
        try {
            boolean isValid = userService.verifyEmailOTP(email, otp);
            if (isValid) {
                return Response.ok(new GenericApiResponse<>(HttpStatus.SC_OK, "OTP verified successfully", null)).build();
            } else {
                return Response.status(HttpStatus.SC_BAD_REQUEST)
                        .entity(new GenericApiResponse<>(HttpStatus.SC_BAD_REQUEST, "Invalid or expired OTP", null))
                        .build();
            }
        } catch (Exception e) {
            logger.warn("Error in verifyEmailOtp", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_BAD_REQUEST, "Invalid or expired OTP", null))
                    .build();
        }
    }

    @DELETE
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("userId") UUID userId) {
        try {
            Map<String, Object> deletionStats = userService.deleteUserWithAllRelatedData(userId);
            return Response.ok(new GenericApiResponse<>(
                HttpStatus.SC_OK, 
                "User and all related data deleted successfully", 
                deletionStats
            )).build();
        } catch (RuntimeException e) {
            logger.error("Error deleting user {}", userId, e);
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return Response.status(HttpStatus.SC_NOT_FOUND)
                        .entity(new GenericApiResponse<>(HttpStatus.SC_NOT_FOUND, e.getMessage(), null))
                        .build();
            }
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_INTERNAL_SERVER_ERROR, 
                        "Failed to delete user: " + e.getMessage(), null))
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error deleting user {}", userId, e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(new GenericApiResponse<>(HttpStatus.SC_INTERNAL_SERVER_ERROR, 
                        "Unexpected error occurred while deleting user", null))
                    .build();
        }
    }
}
