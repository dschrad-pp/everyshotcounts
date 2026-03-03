package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.MediaPartial;
import com.lektralabs.thrones.pallbearer.common.MediaStatusConstants;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.generated.MediaBaseService;
import com.lektralabs.thrones.pallbearer.jdbi.utils.MediaUtils;
import com.lektralabs.thrones.pallbearer.media.pipeline.everyshotcounts.GalleryMediaPipeline;
import com.lektralabs.thrones.pallbearer.media.pipeline.everyshotcounts.GalleryMediaStore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

@ApplicationScoped
public class GalleryMediaService extends MediaBaseService implements MediaUtils {

    private static final Logger logger = Logger.getLogger(GalleryMediaService.class);

    @Inject
    DrillItemService drillItemService;

    @Inject
    DrillService drillService;

    @Inject
    GalleryMediaPipeline galleryMediaPipeline;

    @Inject
    GalleryMediaStore galleryMediaStore;

    @Inject
    ThumbnailGenerationService thumbnailGenerationService;

    public byte[] getDrillItemVideo(UUID drillItemId) {
        Optional<DrillItemRow> maybeDrillItem = drillItemService.findById(drillItemId);

        if (maybeDrillItem.isPresent()) {
            DrillItemRow drillItemRow = maybeDrillItem.get();
            return getDrillItemVideo(drillItemRow);
        } else {
            return new byte[]{};
        }
    }

    private byte[] getDrillItemVideo(DrillItemRow drillItemRow) {
        Optional<UUID> maybeMediaId = drillItemRow.getMediaId();
        if (maybeMediaId.isPresent()) {
            UUID mediaId = maybeMediaId.get();
            String path = galleryMediaStore.getGalleryInputPath(mediaId);
            if (path.isEmpty()) {
                return new byte[]{};
            } else {
                return getMediaBytes(path);
            }
        } else {
            return new byte[]{};
        }
    }

    public byte[] getDrillVideo(UUID drillId) {
        Optional<DrillRow> maybeDrill = drillService.findById(drillId);

        if (maybeDrill.isPresent()) {
            DrillRow drillRow = maybeDrill.get();
            return getDrillVideo(drillRow);
        } else {
            return new byte[]{};
        }
    }

    private byte[] getDrillVideo(DrillRow drillRow) {
        Optional<UUID> maybeMediaId = drillRow.getMediaId();
        logger.info(String.format("fetching media id for drill %s and found media id : %s", drillRow.getId(), drillRow.getMediaId().get()));
        if (maybeMediaId.isPresent()) {
            logger.info("Media id not present");
            UUID mediaId = maybeMediaId.get();
            logger.info("fetching media for id : " + mediaId);
            String path = galleryMediaStore.getGalleryInputPath(mediaId);
            logger.info(String.format("media stored at path : %s", path));
            if (path.isEmpty()) {
                return new byte[]{};
            } else {
                return getMediaBytes(path);
            }
        } else {
            return new byte[]{};
        }
    }

    /**
     * Get the still frame image for the video media associated with the
     * specified drill item
     *
     * @param drillItemId Drill Item ID
     * @return Image byte array or empty array if no media
     */
    public byte[] getDrillItemStillFrame(UUID drillItemId) {
        Optional<DrillItemRow> maybeDrillItem = drillItemService.findById(drillItemId);

        if (maybeDrillItem.isPresent()) {
            DrillItemRow drillItemRow = maybeDrillItem.get();
            return getDrillItemStillFrame(drillItemRow);
        } else {
            return new byte[]{};
        }

    }

    private byte[] getDrillItemStillFrame(DrillItemRow drillItemRow) {
        Optional<UUID> maybeMediaId = drillItemRow.getMediaId();
        if (maybeMediaId.isPresent()) {
            UUID mediaId = maybeMediaId.get();
            String path = galleryMediaStore.getGalleryStillFramePath(mediaId);
            if (path.isEmpty()) {
                return new byte[]{};
            } else {
                return getMediaBytes(path);
            }
        } else {
            return new byte[]{};
        }
    }

    /**
     * Get the still frame image for the video media associated with the
     * specified drill submission
     *
     * @param drillId Drill ID
     * @return Image byte array or empty array if no media
     */
    public byte[] getDrillStillFrame(UUID drillId) {
        Optional<DrillRow> maybeDrill = drillService.findById(drillId);

        if (maybeDrill.isPresent()) {
            DrillRow drillRow = maybeDrill.get();
            return getDrillStillFrame(drillRow);
        } else {
            return new byte[]{};
        }

    }

    private byte[] getDrillStillFrame(DrillRow drillRow) {
        Optional<UUID> maybeMediaId = drillRow.getMediaId();
        if (maybeMediaId.isPresent()) {
            UUID mediaId = maybeMediaId.get();
            String path = galleryMediaStore.getGalleryStillFramePath(mediaId);
            if (path.isEmpty()) {
                return new byte[]{};
            } else {
                return getMediaBytes(path);
            }
        } else {
            return new byte[]{};
        }
    }

    /**
     * Add video media to a drill item
     *
     * @param drillItemId Drill item ID
     * @param fileName Video file name
     * @param videoFile Video file handle
     * @return Some media ID or none
     */
    public Optional<UUID> addDrillItemMedia(UUID drillItemId,
            String fileName,
            File videoFile) {

        Optional<DrillItemRow> maybeDrillItem = drillItemService.findById(drillItemId);
        logger.info("Media drill item fetched");
        if (maybeDrillItem.isPresent()) {
            logger.info("Media drill item is not null");
            DrillItemRow drillItemRow = maybeDrillItem.get();
            return addDrillItemMedia(drillItemRow, fileName, videoFile);
        } else {
            logger.info("Media drill item is null");
            return Optional.empty();
        }
    }

    /**
     * Add video media to a drill item
     *
     * @param drillItemRow Drill item row
     * @param fileName Video file name
     * @param videoFile Video file handle
     * @return Some media ID or none
     */
    private Optional<UUID> addDrillItemMedia(DrillItemRow drillItemRow,
            String fileName,
            File videoFile) {
        if (drillItemRow.getMediaId().isPresent()) {
            // drill item already has media
            return drillItemRow.getMediaId();
        } else {
            logger.info("updating media id");
            UUID mediaId = create(MediaPartial.builder()
                    .mediaId(Optional.of(UUID.randomUUID()))
                    .name(Optional.of(fileName))
                    .description(Optional.empty())
                    .contentUrl(Optional.empty())
                    .statusCode(MediaStatusConstants.PROCESSING)
                    .mimeType(Optional.of("video/mp4"))
                    .build());
            logger.info("media id updated with id " + mediaId);
            drillItemService.updateMediaId(drillItemRow.getId(), mediaId);
            logger.info("ading to gallery media pipeline");
            galleryMediaPipeline.addDrillItemVideoMedia(mediaId, fileName, videoFile);

            // Generate thumbnail/still frame after video is saved
            generateThumbnailForMedia(mediaId, videoFile);

            return Optional.of(mediaId);
        }
    }

    /**
     * Add video media to a drill
     *
     * @param drillRow Drill row
     * @param fileName Video file name
     * @param videoFile Video file handle
     * @return Some media ID or none
     */
    public Optional<UUID> addDrillMedia(DrillRow drillRow,
            String fileName,
            File videoFile) {
        UUID mediaId;

        if (drillRow.getMediaId().isPresent()) {
            // Reuse existing mediaId
            mediaId = drillRow.getMediaId().get();
            logger.info(String.format("Replacing existing media for drill ID=%s with media ID=%s",
                    drillRow.getId(), mediaId));

            // Delete old files and recreate folder
            resetGalleryMediaFolder(mediaId);
        } else {
            // Create new media
            mediaId = create(MediaPartial.builder()
                    .mediaId(Optional.of(UUID.randomUUID()))
                    .name(Optional.of(fileName))
                    .description(Optional.empty())
                    .contentUrl(Optional.empty())
                    .statusCode(MediaStatusConstants.PROCESSING)
                    .mimeType(Optional.of("video/mp4"))
                    .build());

            // Update drill row with new mediaId
            drillService.updateMediaId(drillRow.getId(), mediaId);
            logger.info(String.format("Created new media ID=%s for drill ID=%s", mediaId, drillRow.getId()));
        }

        // Save new video file and re-run processing pipeline (for both cases)
        galleryMediaPipeline.addDrillVideoMedia(drillRow.getId(), mediaId,
                fileName, videoFile);

        // Generate thumbnail/still frame after video is saved
        generateThumbnailForMedia(mediaId, videoFile);

        return Optional.of(mediaId);
    }

    public void resetGalleryMediaFolder(UUID mediaId) {
        String folderPath = galleryMediaStore.getGalleryMediaPath(mediaId);
        File folder = new File(folderPath);

        if (folder.exists()) {
            try {
                FileUtils.deleteDirectory(folder);
                logger.info(String.format("Deleted existing gallery media folder: %s", folderPath));
            } catch (IOException e) {
                logger.error("Failed to delete gallery media folder: {}", folderPath, e);
            }
        }

        // Recreate empty folder
        galleryMediaStore.createGalleryMediaStore(mediaId);
    }

    /**
     * Check if thumbnail exists for a media item, and generate it if it doesn't exist
     * Uses the video file at gallery/{mediaId}/source.mp4 to generate thumbnail
     * 
     * @param mediaId Media ID
     * @return true if thumbnail exists or was successfully generated, false otherwise
     */
    public boolean ensureThumbnailExists(UUID mediaId) {
        try {
            // Get the still frame path where thumbnail should be saved
            String stillFramePath = galleryMediaStore.getGalleryStillFramePath(mediaId);
            
            if (stillFramePath == null || stillFramePath.isEmpty()) {
                logger.warnf("Could not determine still frame path for mediaId: %s", mediaId);
                return false;
            }

            // Check if thumbnail already exists
            File thumbnailFile = new File(stillFramePath);
            if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
                logger.debugf("Thumbnail already exists for mediaId: %s at path: %s", mediaId, stillFramePath);
                return true;
            }

            // Thumbnail doesn't exist, generate it
            logger.infof("Thumbnail not found for mediaId: %s, generating thumbnail...", mediaId);
            
            // Get the video file path: gallery/{mediaId}/source.mp4
            String inputVideoPath = galleryMediaStore.getGalleryInputPath(mediaId);
            File inputVideoFile = null;
            
            if (inputVideoPath != null && !inputVideoPath.isEmpty()) {
                inputVideoFile = new File(inputVideoPath);
            }
            
            // If input path doesn't exist, try to construct path from mediaId
            // Expected path: /home/ankit/Downloads/thrones-development/media/gallery/{mediaId}/source.mp4
            if (inputVideoFile == null || !inputVideoFile.exists()) {
                // Try to extract media store path from still frame path
                // stillFramePath format: /path/to/media/gallery/{mediaId}/still-frame.jpg
                // video path should be: /path/to/media/gallery/{mediaId}/source.mp4
                String videoPath = stillFramePath.replace("/still-frame.jpg", "/source.mp4");
                inputVideoFile = new File(videoPath);
                
                if (!inputVideoFile.exists()) {
                    logger.warnf("Video file not found for thumbnail generation. MediaId: %s, Expected path: %s", 
                        mediaId, videoPath);
                    return false;
                }
            }
            
            logger.infof("Generating thumbnail for mediaId: %s from video: %s", mediaId, inputVideoFile.getAbsolutePath());
            
            boolean success = thumbnailGenerationService.generateThumbnail(inputVideoFile, stillFramePath);
            
            if (success) {
                logger.infof("Successfully generated thumbnail for mediaId: %s at path: %s", mediaId, stillFramePath);
                return true;
            } else {
                logger.errorf("Failed to generate thumbnail for mediaId: %s at path: %s", mediaId, stillFramePath);
                return false;
            }
            
        } catch (Exception e) {
            logger.errorf(e, "Error ensuring thumbnail exists for mediaId: %s", mediaId);
            return false;
        }
    }

    /**
     * Get the thumbnail URL for a media item, ensuring thumbnail exists first
     * 
     * @param mediaId Media ID
     * @param serverBaseUrl Base URL for constructing the thumbnail URL
     * @return Thumbnail URL or null if thumbnail cannot be generated/accessed
     */
    public String getThumbnailUrl(UUID mediaId, String serverBaseUrl) {
        // First ensure thumbnail exists
        boolean thumbnailExists = ensureThumbnailExists(mediaId);
        
        if (!thumbnailExists) {
            logger.warnf("Thumbnail does not exist and could not be generated for mediaId: %s", mediaId);
            return null;
        }
        
        // Get the still frame path
        String stillFramePath = galleryMediaStore.getGalleryStillFramePath(mediaId);
        
        if (stillFramePath == null || stillFramePath.isEmpty()) {
            logger.warnf("Could not determine still frame path for mediaId: %s", mediaId);
            return null;
        }
        
        // Convert local path to URL
        // Extract relative path - look for /gallery/ in the path
        String relativePath = stillFramePath;
        int galleryIndex = stillFramePath.indexOf("/gallery/");
        if (galleryIndex >= 0) {
            // Extract path starting from "gallery/"
            relativePath = stillFramePath.substring(galleryIndex + 1);
        } else {
            // If no /gallery/ found, try to extract just the filename and parent directory
            // Path format: /path/to/media/gallery/{mediaId}/still-frame.jpg
            // We want: gallery/{mediaId}/still-frame.jpg
            String[] parts = stillFramePath.split("/");
            if (parts.length >= 2) {
                // Find mediaId (UUID format) in path
                for (int i = 0; i < parts.length - 1; i++) {
                    String part = parts[i];
                    // Check if this looks like a UUID
                    if (part.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                        // Found mediaId, construct relative path
                        relativePath = "gallery/" + part + "/" + parts[parts.length - 1];
                        break;
                    }
                }
            }
        }
        
        // URL encode the path
        try {
            String encodedPath = java.net.URLEncoder.encode(relativePath, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return String.format("%s/api/media/gallery/thumbnail?path=%s", serverBaseUrl, encodedPath);
        } catch (Exception e) {
            logger.warnf("Failed to encode thumbnail path: %s", relativePath);
            return String.format("%s/api/media/gallery/thumbnail?path=%s", serverBaseUrl, relativePath);
        }
    }

    /**
     * Generate thumbnail/still frame for a media item
     * 
     * @param mediaId Media ID
     * @param videoFile Original video file (used as fallback if input path doesn't exist yet)
     */
    private void generateThumbnailForMedia(UUID mediaId, File videoFile) {
        try {
            // Get the still frame path where thumbnail should be saved
            String stillFramePath = galleryMediaStore.getGalleryStillFramePath(mediaId);
            
            if (stillFramePath == null || stillFramePath.isEmpty()) {
                logger.warnf("Could not determine still frame path for mediaId: %s", mediaId);
                return;
            }

            // Try to get the input video path (where pipeline saves the video)
            String inputVideoPath = galleryMediaStore.getGalleryInputPath(mediaId);
            File inputVideoFile = null;
            
            if (inputVideoPath != null && !inputVideoPath.isEmpty()) {
                inputVideoFile = new File(inputVideoPath);
            }
            
            // Use input video file if it exists, otherwise use the original uploaded file
            File sourceVideoFile = (inputVideoFile != null && inputVideoFile.exists()) 
                ? inputVideoFile 
                : videoFile;
            
            if (sourceVideoFile == null || !sourceVideoFile.exists()) {
                String originalPath = videoFile != null ? videoFile.getAbsolutePath() : "null";
                logger.warnf("Video file not found for thumbnail generation. MediaId: %s, Input path: %s, Original file: %s", 
                    mediaId, inputVideoPath, originalPath);
                return;
            }

            logger.infof("Generating thumbnail for mediaId: %s from video: %s", mediaId, sourceVideoFile.getAbsolutePath());
            
            boolean success = thumbnailGenerationService.generateThumbnail(sourceVideoFile, stillFramePath);
            
            if (success) {
                logger.infof("Successfully generated thumbnail for mediaId: %s at path: %s", mediaId, stillFramePath);
            } else {
                logger.errorf("Failed to generate thumbnail for mediaId: %s at path: %s", mediaId, stillFramePath);
            }
            
        } catch (Exception e) {
            logger.errorf(e, "Error generating thumbnail for mediaId: %s", mediaId);
        }
    }
}
