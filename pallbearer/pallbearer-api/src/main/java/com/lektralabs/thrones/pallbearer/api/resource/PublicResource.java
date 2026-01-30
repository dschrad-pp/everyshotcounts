package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.jdbi.service.DrillItemService;
import com.lektralabs.thrones.pallbearer.jdbi.service.MediaService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/public")
public class PublicResource {
    
    private static Logger logger = LoggerFactory.getLogger(PublicResource.class);
    
    @Inject
    DrillItemService drillItemService;
    
    @Inject
    MediaService mediaService;
    
    @GET
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String publicResource() {
        return "public";
    }
    
    /**
     * Converts Google Drive thumbnail URLs in t_drill_item table.
     * Converts from: https://drive.google.com/file/d/{id}/view?usp=drive_link
     * To: https://drive.google.com/uc?export=view&id={id}
     * 
     * @return Response with conversion results
     */
    @POST
    @Path("/convert-drive-thumbnails")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public Response convertDriveThumbnails() {
        try {
            logger.info("Public API: Starting Google Drive thumbnail conversion");
            DrillItemService.ConversionResult result = drillItemService.convertGoogleDriveThumbnails();
            
            String responseJson = String.format(
                    "{\"message\": \"Conversion completed\", " +
                            "\"convertedCount\": %d, " +
                            "\"errorCount\": %d, " +
                            "\"errors\": %s}",
                    result.getConvertedCount(),
                    result.getErrorCount(),
                    formatErrorsAsJson(result.getErrors()));
            
            logger.info("Public API: Conversion completed - {} converted, {} errors", 
                    result.getConvertedCount(), result.getErrorCount());
            
            return Response.ok()
                    .entity(responseJson)
                    .build();
        } catch (Exception e) {
            logger.error("Public API: Error converting Google Drive thumbnails", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }
    
    private String formatErrorsAsJson(java.util.List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(escapeJson(errors.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Converts Google Drive content URLs in t_media table.
     * Converts from: https://drive.google.com/file/d/{id}/view (with optional query params)
     * To: https://drive.google.com/uc?export=download&id={id}
     * 
     * @return Response with conversion results
     */
    @POST
    @Path("/convert-drive-content-urls")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public Response convertDriveContentUrls() {
        try {
            logger.info("Public API: Starting Google Drive content URL conversion");
            MediaService.ConversionResult result = mediaService.convertGoogleDriveContentUrls();
            
            String responseJson = String.format(
                    "{\"message\": \"Conversion completed\", " +
                            "\"convertedCount\": %d, " +
                            "\"errorCount\": %d, " +
                            "\"errors\": %s}",
                    result.getConvertedCount(),
                    result.getErrorCount(),
                    formatErrorsAsJson(result.getErrors()));
            
            logger.info("Public API: Conversion completed - {} converted, {} errors", 
                    result.getConvertedCount(), result.getErrorCount());
            
            return Response.ok()
                    .entity(responseJson)
                    .build();
        } catch (Exception e) {
            logger.error("Public API: Error converting Google Drive content URLs", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}