package com.lektralabs.thrones.pallbearer.api.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Path("/api/demo-video")
public class DemoVIdeoResource {

    private static final Logger logger = LoggerFactory.getLogger(DemoVIdeoResource.class);

    @Context
    HttpHeaders headers;

    @GET
    @Path("/")
    @PermitAll
    @Produces("video/mp4")
    public Response streamVideo() {
        String nginxVideoUrl = "http://103.99.202.227/demo_video/";

        try {
            URL url = new URL(nginxVideoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Support byte-range streaming for video
            String range = headers.getRequestHeaders().getFirst("Range");
            if (range != null) {
                connection.setRequestProperty("Range", range);
                logger.debug("Range request: {}", range);
            }

            // Get response headers from nginx
            String contentType = connection.getContentType();
            String contentLength = connection.getHeaderField("Content-Length");
            String contentRange = connection.getHeaderField("Content-Range");
            String acceptRanges = connection.getHeaderField("Accept-Ranges");

            // Build response with appropriate status code
            int statusCode = connection.getResponseCode();
            Response.ResponseBuilder responseBuilder = Response
                    .status(statusCode)
                    .entity(connection.getInputStream());

            // Set content type
            if (contentType != null) {
                responseBuilder.type(contentType);
            } else {
                responseBuilder.type("video/mp4");
            }

            // Set headers for video streaming
            if (contentLength != null) {
                responseBuilder.header("Content-Length", contentLength);
            }
            if (contentRange != null) {
                responseBuilder.header("Content-Range", contentRange);
            }
            if (acceptRanges != null) {
                responseBuilder.header("Accept-Ranges", acceptRanges);
            } else {
                responseBuilder.header("Accept-Ranges", "bytes");
            }

            // Cache control for video
            responseBuilder.header("Cache-Control", "public, max-age=3600");

            logger.info("Streaming video - Status: {}, Content-Type: {}, Range: {}",
                    statusCode, contentType, range);

            return responseBuilder.build();

        } catch (IOException e) {
            logger.error("Error streaming video from nginx", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to stream video: " + e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error streaming video", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal server error")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }

    @GET
    @Path("/info")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVideoInfo() {
        try {
            String nginxVideoUrl = "http://103.99.202.227/demo_video/";
            URL url = new URL(nginxVideoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD"); // HEAD request to get metadata only

            String contentType = connection.getContentType();
            String contentLength = connection.getHeaderField("Content-Length");
            String lastModified = connection.getHeaderField("Last-Modified");
            String acceptRanges = connection.getHeaderField("Accept-Ranges");

            VideoInfo videoInfo = new VideoInfo(
                    contentType,
                    contentLength != null ? Long.parseLong(contentLength) : null,
                    lastModified,
                    acceptRanges != null ? acceptRanges : "bytes",
                    connection.getResponseCode() == 200
            );

            return Response.ok(videoInfo).build();

        } catch (Exception e) {
            logger.error("Error getting video info", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to get video info\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/health")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        try {
            String nginxVideoUrl = "http://103.99.202.227/demo_video/";
            URL url = new URL(nginxVideoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000); // 5 second timeout
            connection.setReadTimeout(5000);

            boolean isAvailable = connection.getResponseCode() == 200;

            HealthStatus status = new HealthStatus(
                    isAvailable,
                    isAvailable ? "Video service is available" : "Video service is unavailable",
                    System.currentTimeMillis()
            );

            return Response.ok(status).build();

        } catch (Exception e) {
            logger.warn("Video service health check failed", e);
            HealthStatus status = new HealthStatus(
                    false,
                    "Video service is unavailable: " + e.getMessage(),
                    System.currentTimeMillis()
            );
            return Response.ok(status).build();
        }
    }

    // Inner classes for JSON responses
    public static class VideoInfo {

        public String contentType;
        public Long contentLength;
        public String lastModified;
        public String acceptRanges;
        public boolean available;

        public VideoInfo(String contentType, Long contentLength, String lastModified,
                String acceptRanges, boolean available) {
            this.contentType = contentType;
            this.contentLength = contentLength;
            this.lastModified = lastModified;
            this.acceptRanges = acceptRanges;
            this.available = available;
        }
    }

    public static class HealthStatus {

        public boolean healthy;
        public String message;
        public long timestamp;

        public HealthStatus(boolean healthy, String message, long timestamp) {
            this.healthy = healthy;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
