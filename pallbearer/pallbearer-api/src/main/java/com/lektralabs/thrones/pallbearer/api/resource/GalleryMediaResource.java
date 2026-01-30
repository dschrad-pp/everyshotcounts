package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.datetime.NumberUtils;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.service.GalleryMediaService;
import com.lektralabs.thrones.pallbearer.media.common.MediaConstants;
import io.quarkus.vertx.http.Compressed;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/api/media/gallery")
public class GalleryMediaResource {

    private static Logger logger = LoggerFactory.getLogger(GalleryMediaResource.class);

    @Inject
    AthleteDrillService athleteDrillService;

    @Inject
    GalleryMediaService galleryMediaService;

    @ConfigProperty(name = "pallbearer.media.store")
    String mediaStorePath;

    /**
     * Return the bytes to a drill item demonstration MP4 video file
     *
     * @param drillItemId Drill item ID
     * @return Video file bytes
     */
    @GET
    @Path("/drill_item/{drillItemId}/video.mp4")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaConstants.MP4_VIDEO_MIMETYPE)
    @Compressed
    public Response getDrillItemVideo(@PathParam("drillItemId") UUID drillItemId) {
        logger.info("Gallery resource received drill item video request"
                + " with drillItemId: {}", drillItemId);
        try {
            byte[] bytes = galleryMediaService.getDrillItemVideo(drillItemId);
            if (bytes.length > 0) {
                int length = bytes.length;
                int start = 0;
                int end = length - 1;
                return Response.ok(bytes)
                        .header("Accept-Ranges", "bytes")
                        .header("Content-Range", "bytes " + start + "-" + end + "/" + length)
                        .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    /**
     * Return the bytes to a drill submission MP4 video file
     *
     * @param drillId Drill ID
     * @return Video file bytes
     */
    @GET
    @Path("/drill/{drillId}/video.mp4")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaConstants.MP4_VIDEO_MIMETYPE)
    public Response getDrillVideo(@PathParam("drillId") UUID drillId) {
        logger.info("Gallery resource received drill video request"
                + " with drillId: {}", drillId);
        try {
            byte[] bytes = galleryMediaService.getDrillVideo(drillId);
            if (bytes.length > 0) {
                int length = bytes.length;
                int start = 0;
                int end = length - 1;
                return Response.ok(bytes)
                        .header("Accept-Ranges", "bytes")
                        .header("Content-Range", "bytes " + start + "-" + end + "/" + length)
                        .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            return Response.serverError().build();
        }
    }

    /**
     * Return the bytes to a drill item still frame image file
     *
     * @param drillItemId Drill item ID
     * @return Still frame image bytes
     */
    @GET
    @Path("/drill_item/{drillItemId}/stillframe")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaConstants.STILL_FRAME_MIMETYPE)
    @Compressed
    public Response getDrillItemStillFrame(@PathParam("drillItemId") UUID drillItemId) {
        try {
            byte[] bytes = galleryMediaService.getDrillItemStillFrame(drillItemId);
            return sendBytes(bytes);
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    /**
     * Return the bytes to a drill submission still frame image file
     *
     * @param drillId Drill ID
     * @return Still frame image bytes
     */
    @GET
    @Path("/drill/{drillId}/stillframe")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaConstants.STILL_FRAME_MIMETYPE)
    @Compressed
    public Response getDrillStillFrame(@PathParam("drillId") UUID drillId) {
        try {
            byte[] bytes = galleryMediaService.getDrillStillFrame(drillId);
            return sendBytes(bytes);
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    /**
     * Serve thumbnail image files from local file paths.
     * This endpoint serves thumbnails that are stored as local file paths in the database.
     * 
     * @param filePath Relative path from media store (e.g., "gallery/Beginner/1_Drill_Name/mediaId/thumbnail.jpg")
     * @return Thumbnail image bytes
     */
    /**
     * Serve thumbnail image files from local file paths.
     * This endpoint serves thumbnails that are stored as local file paths in the database.
     * 
     * @param filePath Relative path from media store (e.g., "gallery/Beginner/1_Drill_Name/mediaId/thumbnail.jpg")
     *                 Can be URL-encoded. If absolute path is provided, it will be used directly.
     * @return Thumbnail image bytes
     */
    @GET
    @Path("/thumbnail")
    @PermitAll
    @Produces("image/jpeg")
    @Compressed
    public Response getThumbnail(@QueryParam("path") String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                logger.warn("Thumbnail request missing path parameter");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Path parameter is required")
                        .build();
            }
            
            // URL decode the path
            String decodedPath;
            try {
                decodedPath = java.net.URLDecoder.decode(filePath, java.nio.charset.StandardCharsets.UTF_8);
                logger.debug("Decoded path: {}", decodedPath);
            } catch (Exception e) {
                decodedPath = filePath; // Use original if decoding fails
                logger.warn("Failed to decode path, using original: {}", filePath);
            }
            
            // Construct full path
            String fullPath;
            if (decodedPath.startsWith("/")) {
                // Absolute path - check if it starts with media store path
                if (decodedPath.startsWith(mediaStorePath)) {
                    fullPath = decodedPath;
                } else {
                    // Try to construct full path by finding gallery directory
                    int galleryIndex = decodedPath.indexOf("/gallery/");
                    if (galleryIndex > 0) {
                        // Extract relative path and prepend media store
                        String relativePath = decodedPath.substring(galleryIndex + 1);
                        fullPath = mediaStorePath + "/" + relativePath;
                    } else {
                        // Fallback: prepend media store path
                        fullPath = mediaStorePath + "/" + decodedPath.replaceFirst("^/", "");
                    }
                }
            } else {
                // Relative path - prepend media store path
                fullPath = mediaStorePath + "/" + decodedPath;
            }
            
            logger.info("Serving thumbnail - Original path: {}, Decoded: {}, Full path: {}, Media store: {}", 
                       filePath, decodedPath, fullPath, mediaStorePath);
            
            // Check if file exists
            java.io.File file = new java.io.File(fullPath);
            if (!file.exists()) {
                logger.warn("Thumbnail file not found at path: {}", fullPath);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Thumbnail file not found")
                        .build();
            }
            
            if (!file.isFile()) {
                logger.warn("Path is not a file: {}", fullPath);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Path is not a file")
                        .build();
            }
            
            return sendFileBytes(fullPath);
        } catch (Exception e) {
            logger.error("Error serving thumbnail from path: " + filePath, e);
            return Response.serverError()
                    .entity("Error serving thumbnail: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Serve video files from local file paths.
     * This endpoint serves videos that are stored as local file paths in the database.
     * 
     * @param filePath Relative path from media store (e.g., "gallery/Beginner/1_Drill_Name/mediaId/video.mp4")
     *                 Can be URL-encoded. If absolute path is provided, it will be used directly.
     * @return Video file bytes
     */
    @GET
    @Path("/video")
    @PermitAll
    @Produces(MediaConstants.MP4_VIDEO_MIMETYPE)
    @Compressed
    public Response getVideo(@QueryParam("path") String filePath, @Context HttpHeaders headers) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                logger.warn("Video request missing path parameter");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Path parameter is required")
                        .build();
            }
            
            // URL decode the path
            String decodedPath;
            try {
                decodedPath = java.net.URLDecoder.decode(filePath, java.nio.charset.StandardCharsets.UTF_8);
                logger.debug("Decoded path: {}", decodedPath);
            } catch (Exception e) {
                decodedPath = filePath; // Use original if decoding fails
                logger.warn("Failed to decode path, using original: {}", filePath);
            }
            
            // Construct full path
            String fullPath;
            if (decodedPath.startsWith("/")) {
                // Absolute path - check if it starts with media store path
                if (decodedPath.startsWith(mediaStorePath)) {
                    fullPath = decodedPath;
                } else {
                    // Try to construct full path by finding gallery directory
                    int galleryIndex = decodedPath.indexOf("/gallery/");
                    if (galleryIndex > 0) {
                        // Extract relative path and prepend media store
                        String relativePath = decodedPath.substring(galleryIndex + 1);
                        fullPath = mediaStorePath + "/" + relativePath;
                    } else {
                        // Fallback: prepend media store path
                        fullPath = mediaStorePath + "/" + decodedPath.replaceFirst("^/", "");
                    }
                }
            } else {
                // Relative path - prepend media store path
                fullPath = mediaStorePath + "/" + decodedPath;
            }
            
            logger.info("Serving video - Original path: {}, Decoded: {}, Full path: {}, Media store: {}", 
                       filePath, decodedPath, fullPath, mediaStorePath);
            
            // Check if file exists
            java.io.File file = new java.io.File(fullPath);
            if (!file.exists()) {
                logger.warn("Video file not found at path: {}", fullPath);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Video file not found")
                        .build();
            }
            
            if (!file.isFile()) {
                logger.warn("Path is not a file: {}", fullPath);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Path is not a file")
                        .build();
            }
            
            long fileLength = file.length();
            if (fileLength == 0) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            
            // Check for Range request header (required for Safari/iOS)
            String rangeHeader = headers.getRequestHeaders().getFirst("Range");
            
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // Parse Range header: "bytes=start-end" or "bytes=start-"
                String range = rangeHeader.substring(6); // Remove "bytes="
                String[] ranges = range.split("-");
                
                long start = 0;
                long end = fileLength - 1;
                
                if (ranges.length >= 1 && !ranges[0].isEmpty()) {
                    start = Long.parseLong(ranges[0]);
                }
                if (ranges.length >= 2 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
                
                // Validate range
                if (start > end || start < 0 || end >= fileLength) {
                    return Response.status(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header("Content-Range", "bytes */" + fileLength)
                            .build();
                }
                
                long contentLength = end - start + 1;
                
                // Read the requested byte range
                try (java.io.RandomAccessFile randomAccessFile = new java.io.RandomAccessFile(file, "r")) {
                    randomAccessFile.seek(start);
                    byte[] buffer = new byte[(int) contentLength];
                    randomAccessFile.readFully(buffer);
                    
                    logger.debug("Serving video range: bytes {}-{}/{}", start, end, fileLength);
                    
                    return Response.status(Response.Status.PARTIAL_CONTENT)
                            .entity(buffer)
                            .header("Accept-Ranges", "bytes")
                            .header("Content-Range", "bytes " + start + "-" + end + "/" + fileLength)
                            .header("Content-Length", contentLength)
                            .header("Cache-Control", "public, max-age=3600")
                            .build();
                }
            } else {
                // No Range header - return full file
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(fullPath));
                return Response.ok(bytes)
                        .header("Accept-Ranges", "bytes")
                        .header("Content-Length", fileLength)
                        .header("Cache-Control", "public, max-age=3600")
                        .build();
            }
        } catch (java.lang.NumberFormatException e) {
            logger.error("Invalid Range header format: " + headers.getRequestHeaders().getFirst("Range"), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid Range header")
                    .build();
        } catch (Exception e) {
            logger.error("Error serving video from path: " + filePath, e);
            return Response.serverError()
                    .entity("Error serving video: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Alternative endpoint: Serve thumbnail using path segments instead of query parameter.
     * This endpoint uses path parameters which are more reliable than query parameters.
     * 
     * URL format: /api/media/gallery/thumbnail/{group}/{level}_{drill}/{mediaId}/thumbnail.jpg
     * Example: /api/media/gallery/thumbnail/Beginner/1_15_ft_One-Dribble_Pull-Up_Right/08c6b31c-b81b-4835-b06c-caabe1008f8c/thumbnail.jpg
     * 
     * @param groupName Drill group name (e.g., "Beginner")
     * @param drillFolder Folder name containing level and drill name (e.g., "1_15_ft_One-Dribble_Pull-Up_Right")
     * @param mediaId Media ID (UUID)
     * @return Thumbnail image bytes
     */
    @GET
    @Path("/thumbnail/{group}/{drillFolder}/{mediaId}/thumbnail.jpg")
    @PermitAll
    @Produces("image/jpeg")
    @Compressed
    public Response getThumbnailByPath(
            @PathParam("group") String groupName,
            @PathParam("drillFolder") String drillFolder,
            @PathParam("mediaId") String mediaId) {
        try {
            // JAX-RS automatically decodes path parameters, so groupName, drillFolder, and mediaId are already decoded
            // Construct the relative path
            String relativePath = String.format("gallery/%s/%s/%s/thumbnail.jpg", 
                    groupName, drillFolder, mediaId);
            
            logger.debug("Path parameters received - Group: '{}', DrillFolder: '{}', MediaId: '{}'", 
                        groupName, drillFolder, mediaId);
            
            // Construct full path
            String fullPath = mediaStorePath + "/" + relativePath;
            
            logger.info("Serving thumbnail by path - Group: {}, DrillFolder: {}, MediaId: {}, Full path: {}", 
                       groupName, drillFolder, mediaId, fullPath);
            
            // Check if file exists
            java.io.File file = new java.io.File(fullPath);
            if (!file.exists()) {
                logger.warn("Thumbnail file not found at path: {}", fullPath);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Thumbnail file not found")
                        .build();
            }
            
            if (!file.isFile()) {
                logger.warn("Path is not a file: {}", fullPath);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Path is not a file")
                        .build();
            }
            
            return sendFileBytes(fullPath);
        } catch (Exception e) {
            logger.error("Error serving thumbnail by path - Group: {}, DrillFolder: {}, MediaId: {}", 
                        groupName, drillFolder, mediaId, e);
            return Response.serverError()
                    .entity("Error serving thumbnail: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Create a video and related artifacts for a drill item
     *
     * @param drillItemId Drill item ID
     * @param upload Multipart media resource
     * @return Media ID or server error
     */
    @POST
    @Path("/drill_item/{drillItemId}/video")
    @RolesAllowed({"ADMIN", "COACH", "ATHLETE"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Compressed
    public Response createDrillItemVideo(@PathParam("drillItemId") UUID drillItemId,
            @MultipartForm MultipartMediaResource upload) {
        try {
            logger.info("Gallery resource received drill item video upload"
                    + " with path: {} and name: {} and file size: {}"
                    + " from user {}",
                    upload.file.getAbsolutePath(),
                    upload.fileName,
                    Files.size(Paths.get(upload.file.getAbsolutePath())),
                    upload.createdById);

            Optional<UUID> result = galleryMediaService.addDrillItemMedia(
                    drillItemId, upload.fileName, upload.file);

            return result
                    .map(mediaId -> Response.ok(mediaId).build())
                    .orElseGet(() -> Response.status(Response.Status.BAD_REQUEST.getStatusCode()).
                    entity("Failed to add media to drill item").build());
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    /**
     * Create an athlete drill submission for a drill item
     *
     * @param drillItemId Drill item ID for the associated video
     * @param upload Multipart media resource
     * @return Media ID or server error
     */
    @POST
    @Path("/drill_item/{drillItemId}/drill/video")
    @RolesAllowed({"ADMIN", "COACH", "ATHLETE"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Compressed
    public Response DrillVideo(@PathParam("drillItemId") UUID drillItemId, @MultipartForm MultipartDrillSubmitResource upload) {
        logger.info("Gallery resource received drill item, athlete drill"
                + " submission video upload for drill item ID {}"
                + " with path: {} and name: {}"
                + " from user {}"
                + " with upload reported attempts: {}"
                + " and upload reported makes: {}",
                drillItemId,
                upload.file.getAbsolutePath(),
                upload.fileName,
                upload.createdById,
                upload.attemptsReported,
                upload.makesReported);
        try {
            int attemptsReported = NumberUtils.toInt(upload.attemptsReported, 0);
            int makesReported = NumberUtils.toInt(upload.makesReported, 0);

            Optional<UUID> result = athleteDrillService.drillSubmission(
                    drillItemId, UUID.fromString(upload.createdById),
                    upload.fileName, upload.file,
                    attemptsReported, makesReported);

            return result
                    .map(mediaId -> Response.ok(mediaId).build())
                    .orElseGet(() -> Response.status(Response.Status.BAD_REQUEST.getStatusCode()).
                    entity("Failed to add drill media to drill item").build());
        } catch (Exception e) {
            logger.error("Exception thrown submitting drill video for"
                    + " drill item ID={}", drillItemId, e);
            logger.error(e.getMessage());
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{mediaId}/output/master.m3u8")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaConstants.HLS_VIDEO_MIMETYPE)
    @Compressed
    public Response getMediaMasterStream(@PathParam("mediaId") UUID mediaId) {
        logger.info("Getting media master stream: {}", mediaId);
        String filePath = "%s/gallery/%s/output/master.m3u8".formatted(mediaStorePath, mediaId.toString());
        byte[] fileWithIframe = removeIframeLines(filePath);
        return sendBytes(fileWithIframe);
    }

    @GET
    @Path("/{mediaId}/output/{mediaIndex}/stream.m3u8")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaConstants.HLS_VIDEO_MIMETYPE)
    @Compressed
    public Response getMediaOutputStream(@PathParam("mediaId") UUID mediaId, @PathParam("mediaIndex") String mediaIndex) {
        logger.info("Getting media output stream: {}/{}", mediaId, mediaIndex);
        String filePath = "%s/gallery/%s/output/%s/stream.m3u8".formatted(mediaStorePath, mediaId.toString(), mediaIndex);
        return sendFileBytes(filePath);
    }

    @GET
    @Path("/{mediaId}/output/{mediaIndex}/iframes.m3u8")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaConstants.HLS_VIDEO_MIMETYPE)
    @Compressed
    public Response getMediaOutputIframes(@PathParam("mediaId") UUID mediaId, @PathParam("mediaIndex") String mediaIndex) {
        logger.info("Getting media output iframes: {}/{}", mediaId, mediaIndex);
        String filePath = "%s/gallery/%s/output/%s/iframes.m3u8".formatted(mediaStorePath, mediaId.toString(), mediaIndex);
        return sendFileBytes(filePath);
    }

    @GET
    @Path("/{mediaId}/output/{mediaIndex}/{mediaFile}")
    @RolesAllowed({"ADMIN", "ATHLETE", "COACH", "USER"})
    @Produces(MediaConstants.TS_VIDEO_MIMETYPE)
    @Compressed
    public Response getMediaStream(@PathParam("mediaId") UUID mediaId, @PathParam("mediaIndex") String mediaIndex,
            @PathParam("mediaFile") String mediaFile) {
        logger.info("Getting media stream: {}/{}/{}", mediaId, mediaIndex, mediaFile);
        String filePath = "%s/gallery/%s/output/%s/%s".formatted(mediaStorePath, mediaId.toString(), mediaIndex, mediaFile);
        return sendFileBytes(filePath);
    }

    private static Response sendFileBytes(String filePath) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            return sendBytes(bytes);
        } catch (IOException ioe) {
            return Response.serverError().build();
        }
    }

    private static final Pattern IFRAME_PATTERN = Pattern.compile("^.*?i-frame.*?$", Pattern.CASE_INSENSITIVE);

    private static byte[] removeIframeLines(String filePath) {
        StringBuilder sb = new StringBuilder();
        try {
            Files.readAllLines(Paths.get(filePath)).forEach(str -> {
                Matcher matcher = IFRAME_PATTERN.matcher(str);
                if (!matcher.matches()) {
                    sb.append(str).append(System.lineSeparator());
                }
            });
            return sb.toString().getBytes();
        } catch (IOException ioe) {
            return "ERROR".getBytes();
        }
    }

    private static Response sendBytes(byte[] bytes) {
        if (bytes.length > 0) {
            return Response.ok(bytes)
                    .header("Accept-Ranges", "bytes")
                    .header("Content-Length", bytes.length)
                    .build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

}
