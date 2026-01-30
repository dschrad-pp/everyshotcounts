package com.lektralabs.thrones.pallbearer.api.resource;

import com.lektralabs.thrones.pallbearer.jdbi.service.DrillItemImportService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/drill_item_import")
public class DrillItemImportResource {

    private static Logger logger = LoggerFactory.getLogger(DrillItemImportResource.class);

    @Inject
    DrillItemImportService drillItemImportService;

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCsv(MultipartFormDataInput input) {
        try {
            Map<String, List<InputPart>> formParts = input.getFormDataMap();
            List<InputPart> fileParts = formParts.get("file");

            if (fileParts == null || fileParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"No file provided\"}")
                        .build();
            }

            InputPart filePart = fileParts.get(0);
            InputStream fileInputStream = filePart.getBody(InputStream.class, null);

            int importedCount = drillItemImportService.importFromCsv(fileInputStream);

            return Response.ok()
                    .entity("{\"message\": \"Successfully imported " + importedCount + " records\", \"count\": "
                            + importedCount + "}")
                    .build();
        } catch (Exception e) {
            logger.error("Error importing CSV file", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDrillItemImport(@PathParam("id") UUID id) {
        return drillItemImportService.findById(id)
                .map(row -> Response.ok(row).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("/")
    @RolesAllowed({ "ADMIN", "ATHLETE", "COACH", "FAN", "USER" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllDrillItemImports() {
        return Response.ok(drillItemImportService.findAll()).build();
    }

    @DELETE
    @Path("/")
    @RolesAllowed({ "ADMIN" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAll() {
        int deletedCount = drillItemImportService.deleteAll();
        return Response.ok()
                .entity("{\"message\": \"Deleted " + deletedCount + " records\", \"count\": " + deletedCount + "}")
                .build();
    }

    /**
     * Download media files from Google Drive and update import tables with local paths.
     * This endpoint only downloads files and updates the import tables - it does NOT migrate to production.
     * 
     * This endpoint:
     * 1. Queries all drill items with group names
     * 2. Queries all media imports with Google Drive URLs
     * 3. Downloads videos and thumbnails to local storage
     * 4. Updates content_url and media_thumbnail in import tables with local paths
     * 
     * Use this endpoint to test the download functionality before running migration.
     * 
     * @param limit Optional query parameter to limit the number of items to download. 
     *              Pass a number (e.g., 10, 5, 45) or omit/null for all items.
     * @return Response with download statistics
     */
    @POST
    @Path("/download-media")
    @Produces(MediaType.APPLICATION_JSON)
    public Response downloadMedia(@QueryParam("limit") Integer limit) {
        try {
            logger.info("Starting download of media files from Google Drive" + 
                       (limit != null ? " (limit: " + limit + ")" : " (all items)"));
            DrillItemImportService.DownloadResult result = drillItemImportService.downloadAndUpdateMediaFiles(limit);

            logger.info("Download completed - Videos: {} downloaded, {} failed. Thumbnails: {} downloaded, {} failed",
                    result.getVideosDownloaded(), result.getVideosFailed(),
                    result.getThumbnailsDownloaded(), result.getThumbnailsFailed());

            String responseJson = String.format(
                    "{\"message\": \"Download completed\", " +
                            "\"downloads\": {" +
                            "\"videosDownloaded\": %d, " +
                            "\"videosFailed\": %d, " +
                            "\"thumbnailsDownloaded\": %d, " +
                            "\"thumbnailsFailed\": %d" +
                            "}}",
                    result.getVideosDownloaded(),
                    result.getVideosFailed(),
                    result.getThumbnailsDownloaded(),
                    result.getThumbnailsFailed());

            return Response.ok()
                    .entity(responseJson)
                    .build();
        } catch (Exception e) {
            logger.error("Error downloading media files", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Migrate all data from import tables (t_drill_item_import, t_media_import)
     * to production tables (t_drill_item, t_media).
     * 
     * This endpoint:
     * 1. Downloads media files from Google Drive and updates import tables with local paths
     * 2. Empties t_drill_item and t_media tables
     * 3. Copies all data from t_media_import to t_media
     * 4. Copies all data from t_drill_item_import to t_drill_item (with UUID
     * conversion for media_id)
     * 
     * @param limit Optional query parameter to limit the number of items to download before migration.
     *              Pass a number (e.g., 10, 5, 45) or omit/null for all items.
     *              Note: Downloads include pauses to avoid Google Drive rate limiting.
     * @return Response with migration results
     */
    @POST
    @Path("/migrate-to-production")
    @Produces(MediaType.APPLICATION_JSON)
    public Response migrateToProduction(@QueryParam("limit") Integer limit) {
        try {
            logger.info("Starting migration from import tables to production tables" + 
                       (limit != null ? " (download limit: " + limit + ")" : " (all items)"));
            DrillItemImportService.MigrationResult result = drillItemImportService.migrateImportToProduction(limit);

            logger.info("Migration completed - Deleted: {} drill items, {} media. Migrated: {} media, {} drill items",
                    result.getDeletedDrillItems(), result.getDeletedMedia(),
                    result.getMigratedMedia(), result.getMigratedDrillItems());

            StringBuilder responseJsonBuilder = new StringBuilder();
            responseJsonBuilder.append(String.format(
                    "{\"message\": \"Migration completed successfully\", " +
                            "\"deletedDrillItems\": %d, " +
                            "\"deletedMedia\": %d, " +
                            "\"migratedMedia\": %d, " +
                            "\"migratedDrillItems\": %d",
                    result.getDeletedDrillItems(),
                    result.getDeletedMedia(),
                    result.getMigratedMedia(),
                    result.getMigratedDrillItems()));
            
            // Add download statistics if available
            if (result.getDownloadResult() != null) {
                DrillItemImportService.DownloadResult downloadResult = result.getDownloadResult();
                responseJsonBuilder.append(String.format(
                        ", \"downloads\": {" +
                                "\"videosDownloaded\": %d, " +
                                "\"videosFailed\": %d, " +
                                "\"thumbnailsDownloaded\": %d, " +
                                "\"thumbnailsFailed\": %d" +
                                "}",
                        downloadResult.getVideosDownloaded(),
                        downloadResult.getVideosFailed(),
                        downloadResult.getThumbnailsDownloaded(),
                        downloadResult.getThumbnailsFailed()));
            }
            
            responseJsonBuilder.append("}");
            String responseJson = responseJsonBuilder.toString();

            return Response.ok()
                    .entity(responseJson)
                    .build();
        } catch (Exception e) {
            logger.error("Error migrating import tables to production", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Download media files from Google Drive in production tables and replace URLs with local paths.
     * This endpoint works on production tables (t_drill_item, t_media) instead of import tables.
     * 
     * This endpoint:
     * 1. Queries all media records in t_media with Google Drive URLs
     * 2. Queries all drill items in t_drill_item with Google Drive thumbnails
     * 3. Downloads videos and thumbnails to local storage
     * 4. Updates content_url in t_media and media_thumbnail in t_drill_item with local paths
     * 
     * This allows the app to use server files instead of Google Drive links.
     * 
     * @param limit Optional query parameter to limit the number of items to download. 
     *              Pass a number (e.g., 10, 5, 45) or omit/null for all items.
     *              Downloads include pauses to avoid Google Drive rate limiting.
     * @return Response with download statistics
     */
    @POST
    @Path("/download-production-media")
    @Produces(MediaType.APPLICATION_JSON)
    public Response downloadProductionMedia(@QueryParam("limit") Integer limit) {
        try {
            logger.info("Starting download of media files from Google Drive (production tables)" + 
                       (limit != null ? " (limit: " + limit + ")" : " (all items)"));
            DrillItemImportService.DownloadResult result = drillItemImportService.downloadAndUpdateProductionMediaFiles(limit);

            logger.info("Download completed - Videos: {} downloaded, {} failed. Thumbnails: {} downloaded, {} failed",
                    result.getVideosDownloaded(), result.getVideosFailed(),
                    result.getThumbnailsDownloaded(), result.getThumbnailsFailed());

            String responseJson = String.format(
                    "{\"message\": \"Download completed\", " +
                            "\"downloads\": {" +
                            "\"videosDownloaded\": %d, " +
                            "\"videosFailed\": %d, " +
                            "\"thumbnailsDownloaded\": %d, " +
                            "\"thumbnailsFailed\": %d" +
                            "}}",
                    result.getVideosDownloaded(),
                    result.getVideosFailed(),
                    result.getThumbnailsDownloaded(),
                    result.getThumbnailsFailed());

            return Response.ok()
                    .entity(responseJson)
                    .build();
        } catch (Exception e) {
            logger.error("Error downloading production media files", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Sync missing videos and thumbnails from import tables to production tables.
     * This endpoint:
     * 1. Iterates through all production drill items one by one
     * 2. For each production drill item, finds the corresponding import drill item by unique_id
     * 3. Checks if import table has Google Drive URLs for video/thumbnail
     * 4. Checks if production table is missing local files (has Google Drive URL or null/empty)
     * 5. Downloads videos and thumbnails from import table's Google Drive URLs
     * 6. Updates production tables with local paths
     * 7. Does NOT modify import tables (they are for backup/reference only)
     * 
     * This ensures every drill has both video and thumbnail when they exist in the import tables.
     * 
     * @param limit Optional query parameter to limit the number of drill items to process. 
     *              Pass a number (e.g., 10, 5, 45) or omit/null for all items.
     *              Downloads include pauses to avoid Google Drive rate limiting.
     * @return Response with sync statistics
     */
    @POST
    @Path("/sync-missing-media")
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncMissingMedia(@QueryParam("limit") Integer limit) {
        try {
            logger.info("Starting sync of missing media from import tables to production tables" + 
                       (limit != null ? " (limit: " + limit + ")" : " (all items)"));
            DrillItemImportService.SyncResult result = drillItemImportService.syncMissingMediaFromImport(limit);

            logger.info("Sync completed - Processed: {} drill items, Skipped: {}. Videos: {} downloaded, {} failed. Thumbnails: {} downloaded, {} failed",
                    result.getDrillsProcessed(), result.getDrillsSkipped(),
                    result.getVideosDownloaded(), result.getVideosFailed(),
                    result.getThumbnailsDownloaded(), result.getThumbnailsFailed());

            String responseJson = String.format(
                    "{\"message\": \"Sync completed successfully\", " +
                            "\"drillsProcessed\": %d, " +
                            "\"drillsSkipped\": %d, " +
                            "\"downloads\": {" +
                            "\"videosDownloaded\": %d, " +
                            "\"videosFailed\": %d, " +
                            "\"thumbnailsDownloaded\": %d, " +
                            "\"thumbnailsFailed\": %d" +
                            "}}",
                    result.getDrillsProcessed(),
                    result.getDrillsSkipped(),
                    result.getVideosDownloaded(),
                    result.getVideosFailed(),
                    result.getThumbnailsDownloaded(),
                    result.getThumbnailsFailed());

            return Response.ok()
                    .entity(responseJson)
                    .build();
        } catch (Exception e) {
            logger.error("Error syncing missing media from import tables", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Download media files from Google Drive URLs in CSV and organize them by group name and unique_id.
     * This endpoint:
     * 1. Accepts CSV file upload (same format as drill_item_import)
     * 2. Reads media_id and media_thumbnail columns containing Google Drive links
     * 3. Downloads videos and thumbnails into folders organized by group name and unique_id
     * 4. Updates database records with local file paths
     * 
     * Folder structure: {mediaStorePath}/{groupName}/{unique_id}/video.mp4 and thumbnail.jpg
     * 
     * @param input Multipart form data containing CSV file
     * @return Response with download statistics
     */
    @POST
    @Path("/download-media-from-csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
//     @RolesAllowed({"ADMIN", "COACH"})
    public Response downloadMediaFromCsv(MultipartFormDataInput input) {
        try {
            Map<String, List<InputPart>> formParts = input.getFormDataMap();
            List<InputPart> fileParts = formParts.get("file");

            if (fileParts == null || fileParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"No file provided\"}")
                        .build();
            }

            InputPart filePart = fileParts.get(0);
            InputStream fileInputStream = filePart.getBody(InputStream.class, null);

            logger.info("Starting download of media files from CSV");
            DrillItemImportService.DownloadResult result = drillItemImportService.downloadMediaFromCsv(fileInputStream);

            logger.info("Download completed - Videos: {} downloaded, {} failed. Thumbnails: {} downloaded, {} failed",
                    result.getVideosDownloaded(), result.getVideosFailed(),
                    result.getThumbnailsDownloaded(), result.getThumbnailsFailed());

            String responseJson = String.format(
                    "{\"message\": \"Download completed\", " +
                            "\"downloads\": {" +
                            "\"videosDownloaded\": %d, " +
                            "\"videosFailed\": %d, " +
                            "\"thumbnailsDownloaded\": %d, " +
                            "\"thumbnailsFailed\": %d" +
                            "}}",
                    result.getVideosDownloaded(),
                    result.getVideosFailed(),
                    result.getThumbnailsDownloaded(),
                    result.getThumbnailsFailed());

            return Response.ok()
                    .entity(responseJson)
                    .build();
        } catch (Exception e) {
            logger.error("Error downloading media files from CSV", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Scans the media folder structure and updates production tables with local file paths.
     * This endpoint:
     * 1. Scans {mediaStorePath}/{groupName}/{unique_id}/ folders
     * 2. Matches folder names (case-insensitive) to unique_id in t_drill_item
     * 3. Verifies drill_group_id matches the group folder name
     * 4. Checks if both video.mp4 and thumbnail.jpg exist
     * 5. Updates t_drill_item.media_thumbnail with thumbnail path
     * 6. Updates t_media.content_url with video path
     * 
     * Folder structure expected: {mediaStorePath}/{groupName}/{unique_id}/video.mp4 and thumbnail.jpg
     * 
     * @return Response with scan statistics
     */
    @POST
    @Path("/scan-and-update-media-paths")
    @Produces(MediaType.APPLICATION_JSON)
//     @RolesAllowed({"ADMIN", "COACH"})
    public Response scanAndUpdateMediaPaths() {
        try {
            logger.info("Starting scan of media folder structure");
            DrillItemImportService.ScanResult result = drillItemImportService.scanAndUpdateMediaPaths();

            logger.info("Scan completed - Scanned: {} folders, {} updated, {} skipped, {} failed",
                    result.getItemsScanned(), result.getItemsUpdated(),
                    result.getItemsSkipped(), result.getItemsFailed());

            StringBuilder responseJsonBuilder = new StringBuilder();
            responseJsonBuilder.append(String.format(
                    "{\"message\": \"Scan completed\", " +
                            "\"itemsScanned\": %d, " +
                            "\"itemsUpdated\": %d, " +
                            "\"itemsSkipped\": %d, " +
                            "\"itemsFailed\": %d",
                    result.getItemsScanned(),
                    result.getItemsUpdated(),
                    result.getItemsSkipped(),
                    result.getItemsFailed()));

            if (result.getErrorMessage() != null) {
                responseJsonBuilder.append(String.format(", \"error\": \"%s\"", result.getErrorMessage()));
            }

            responseJsonBuilder.append("}");

            return Response.ok()
                    .entity(responseJsonBuilder.toString())
                    .build();
        } catch (Exception e) {
            logger.error("Error scanning media folder structure", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
