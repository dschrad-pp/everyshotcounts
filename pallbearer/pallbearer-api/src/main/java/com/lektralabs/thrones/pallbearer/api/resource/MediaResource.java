package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.MediaPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.MediaService;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

import com.lektralabs.thrones.pallbearer.jdbi.model.generated.MediaRow;

import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

@Path("/api/media")
public class MediaResource {

    private static Logger logger = LoggerFactory.getLogger(MediaResource.class);
    private static final String SERVER_BASE_URL = "http://103.99.202.227:8000";

    @Context
    HttpHeaders headers;

    @Inject
    MediaService mediaService;

    @GET
    @Path("/{mediaId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMedia(@PathParam("mediaId") UUID mediaId) {
        return mediaService.findById(mediaId)
                .map(row -> {
                    // Convert local file path to URL if contentUrl is a local path
                    if (row.getContentUrl().isPresent()) {
                        String contentUrl = row.getContentUrl().get();
                        // Check if it's a local file path (starts with /)
                        if (contentUrl.startsWith("/") && !contentUrl.startsWith("http")) {
                            // Convert local path to URL
                            String convertedUrl = convertLocalPathToVideoUrl(contentUrl);
                            logger.debug("Converted local path to video URL: {}", convertedUrl);
                            row.setContentUrl(Optional.of(convertedUrl));
                        }
                        // If it's already a URL (http/https), leave it as is
                    }
                    return Response.ok(row).build();
                })
                .orElseGet(() -> Response.noContent().build());
    }

    /**
     * Converts a local video file path to a URL that can be accessed via HTTP.
     * Supports both old format (with /gallery/) and new format (directly under mediaStorePath).
     * 
     * Old format: {mediaStorePath}/gallery/{group}/{level}_{drill}/{mediaId}/video.mp4
     * New format: {mediaStorePath}/{groupName}/{unique_id}/video.mp4
     * 
     * @param localPath Local file path (e.g., "/home/ankit/Downloads/thrones-development/media/Intermediate/intermediate_1_1/video.mp4")
     * @return URL that can be used to access the video file
     */
    private String convertLocalPathToVideoUrl(String localPath) {
        logger.debug("Converting local path to video URL: {}", localPath);
        if (localPath == null || localPath.isEmpty()) {
            return null;
        }
        
        // If it's already a URL (http/https), return as-is
        if (localPath.startsWith("http://") || localPath.startsWith("https://")) {
            return localPath;
        }
        
        // Try to extract relative path from the local path
        String relativePath = null;
        
        // Check for new format: {mediaStorePath}/{groupName}/{unique_id}/video.mp4
        // This format doesn't have "/gallery/" in it
        if (localPath.contains("/video.mp4") && !localPath.contains("/gallery/")) {
            // Check if path matches new format: ends with /video.mp4 and has structure {group}/{unique_id}/video.mp4
            String[] pathParts = localPath.split("/");
            if (pathParts.length >= 3 && pathParts[pathParts.length - 1].equals("video.mp4")) {
                // Find the directories before video.mp4
                // Last part is "video.mp4", second last is {unique_id}, third last is {groupName}
                String uniqueId = pathParts[pathParts.length - 2];
                String groupName = pathParts.length >= 3 ? pathParts[pathParts.length - 3] : null;
                
                // Check if this looks like the new format (groupName/unique_id/video.mp4)
                // Common group names: Beginner, Intermediate, Advanced, Elite (case-insensitive)
                if (groupName != null) {
                    String[] knownGroups = {"Beginner", "Intermediate", "Advanced", "Elite", 
                                           "beginner", "intermediate", "advanced", "elite"};
                    
                    for (String group : knownGroups) {
                        if (groupName.equalsIgnoreCase(group)) {
                            // Found a group name, extract relative path: {groupName}/{unique_id}/video.mp4
                            relativePath = groupName + "/" + uniqueId + "/video.mp4";
                            logger.debug("Detected new format path - Group: {}, UniqueId: {}, Relative path: {}", 
                                       groupName, uniqueId, relativePath);
                            break;
                        }
                    }
                }
            }
        }
        
        // If we didn't find new format, try old format with /gallery/
        if (relativePath == null) {
            int galleryIndex = localPath.indexOf("/gallery/");
            if (galleryIndex >= 0) {
                // Extract relative path starting from "gallery/"
                relativePath = localPath.substring(galleryIndex + 1); // +1 to skip the leading /
            }
        }
        
        // If we have a relative path (either new or old format), use query parameter endpoint
        if (relativePath != null) {
            try {
                String encodedPath = java.net.URLEncoder.encode(relativePath, java.nio.charset.StandardCharsets.UTF_8)
                        .replace("+", "%20"); // Replace + with %20 for spaces
                String url = String.format("%s/api/media/gallery/video?path=%s", SERVER_BASE_URL, encodedPath);
                logger.debug("Converted local video path {} to URL: {}", localPath, url);
                return url;
            } catch (Exception e) {
                logger.warn("Error encoding video path: " + relativePath, e);
                return String.format("%s/api/media/gallery/video?path=%s", SERVER_BASE_URL, relativePath);
            }
        }
        
        // Fallback: if we can't extract relative path, try to use the full path
        logger.warn("Could not extract relative path from: " + localPath);
        try {
            String encodedPath = java.net.URLEncoder.encode(localPath, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return String.format("%s/api/media/gallery/video?path=%s", SERVER_BASE_URL, encodedPath);
        } catch (Exception e) {
            return localPath; // Return original path if we can't convert it
        }
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMedia(MediaPartial mediaPartial) {
        return Response.ok(mediaService.create(mediaPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMedia(MediaPartial mediaPartial) {
        return Response.ok(mediaService.update(mediaPartial)).build();
    }

}
