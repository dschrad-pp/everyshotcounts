package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillItemPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillItemService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Path("/api/drill_item")
public class DrillItemResource {

    private static Logger logger = LoggerFactory.getLogger(DrillItemResource.class);

    @Inject
    DrillItemService drillItemService;

    @GET
    @Path("/{drillItemId}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDrillItem(@PathParam("drillItemId") UUID drillItemId) {
        return drillItemService.findById(drillItemId)
                .map(row -> {
                    // Convert local file path to URL if mediaThumbnail is a local path
                    if (row.getMediaThumbnail().isPresent()) {
                        String thumbnailPath = row.getMediaThumbnail().get();
                        // Check if it's a local file path (starts with /)
                        if (thumbnailPath != null && thumbnailPath.startsWith("/") && !thumbnailPath.startsWith("http")) {
                            // Convert local path to URL
                            String thumbnailUrl = convertLocalPathToUrl(thumbnailPath);
                            logger.debug("Converted local thumbnail path to URL: {} -> {}", thumbnailPath, thumbnailUrl);
                            row.setMediaThumbnail(java.util.Optional.of(thumbnailUrl));
                        }
                        // If it's already a URL (http/https), leave it as is
                    }
                    return Response.ok(row).build();
                })
                .orElseGet(() -> Response.noContent().build());
    }
    
    /**
     * Converts a local file path to a URL that can be accessed via HTTP.
     * Supports both old format (with /gallery/) and new format (directly under mediaStorePath).
     * 
     * Old format: {mediaStorePath}/gallery/{group}/{level}_{drill}/{mediaId}/thumbnail.jpg
     * New format: {mediaStorePath}/{groupName}/{unique_id}/thumbnail.jpg
     * 
     * @param localPath Local file path (e.g., "/home/ankit/Downloads/thrones-development/media/Beginner/beginners_1_1/thumbnail.jpg")
     * @return URL that can be used to access the file
     */
    private String convertLocalPathToUrl(String localPath) {
        if (localPath == null || localPath.isEmpty()) {
            return null;
        }
        
        // If it's already a URL (http/https), return as-is
        if (localPath.startsWith("http://") || localPath.startsWith("https://")) {
            return localPath;
        }
        
        // Try to extract relative path from the local path
        String relativePath = null;
        
        // Check for new format: {mediaStorePath}/{groupName}/{unique_id}/thumbnail.jpg
        // This format doesn't have "/gallery/" in it
        if (localPath.contains("/thumbnail.jpg") && !localPath.contains("/gallery/")) {
            // Check if path matches new format: ends with /thumbnail.jpg and has structure {group}/{unique_id}/thumbnail.jpg
            String[] pathParts = localPath.split("/");
            if (pathParts.length >= 3 && pathParts[pathParts.length - 1].equals("thumbnail.jpg")) {
                // Find the directories before thumbnail.jpg
                // Last part is "thumbnail.jpg", second last is {unique_id}, third last is {groupName}
                String uniqueId = pathParts[pathParts.length - 2];
                String groupName = pathParts.length >= 3 ? pathParts[pathParts.length - 3] : null;
                
                // Check if this looks like the new format (groupName/unique_id/thumbnail.jpg)
                // Common group names: Beginner, Intermediate, Advanced, Elite (case-insensitive)
                if (groupName != null) {
                    String[] knownGroups = {"Beginner", "Intermediate", "Advanced", "Elite", 
                                           "beginner", "intermediate", "advanced", "elite"};
                    
                    for (String group : knownGroups) {
                        if (groupName.equalsIgnoreCase(group)) {
                            // Found a group name, extract relative path: {groupName}/{unique_id}/thumbnail.jpg
                            relativePath = groupName + "/" + uniqueId + "/thumbnail.jpg";
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
                        .replace("+", "%20");
                String url = String.format("http://103.99.202.227:8000/api/media/gallery/thumbnail?path=%s", encodedPath);
                logger.debug("Converted local path {} to URL: {}", localPath, url);
                return url;
            } catch (Exception e) {
                logger.warn("Error encoding path: " + relativePath, e);
                return String.format("http://103.99.202.227:8000/api/media/gallery/thumbnail?path=%s", relativePath);
            }
        }
        
        // Fallback: if we can't extract relative path, try to use the full path
        logger.warn("Could not extract relative path from: " + localPath);
        try {
            String encodedPath = java.net.URLEncoder.encode(localPath, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return String.format("http://103.99.202.227:8000/api/media/gallery/thumbnail?path=%s", encodedPath);
        } catch (Exception e) {
            return localPath; // Return original path if we can't convert it
        }
    }

    @POST
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDrillItem(DrillItemPartial drillItemPartial) {
        return Response.ok(drillItemService.createWithOrder(drillItemPartial)).build();
    }

    @PUT
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDrillItem(DrillItemPartial drillItemPartial) {
        return Response.ok(drillItemService.update(drillItemPartial)).build();
    }

    @GET
    @Path("/")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "FAN", "USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllDrills() {
        return Response.ok(drillItemService.getAllDrills()).build();
    }

}
