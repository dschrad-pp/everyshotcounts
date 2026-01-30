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
}
