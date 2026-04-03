package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.jdbi.model.UserRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.MediaRow;
import com.lektralabs.thrones.pallbearer.media.common.MediaConstants;
import com.lektralabs.thrones.pallbearer.media.pipeline.everyshotcounts.GalleryMediaStore;
import com.lektralabs.thrones.pallbearer.media.storage.S3StorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Stream;

@ApplicationScoped
public class GalleryMediaStorageService {

    private static final Logger logger = Logger.getLogger(GalleryMediaStorageService.class);

    /**
     * S3 key layout (UTC calendar date from media
     * {@link MediaRow#getCreationDate()} — not “today” — so keys stay
     * stable if sync runs again later):
     * {@code gallery/<dd-MM-yyyy>_<sanitized-username>_<createdById>/<mediaId>/<files under gallery folder>}.
     */
    private static final String GALLERY_S3_PREFIX = "gallery";

    private static final DateTimeFormatter GALLERY_DATE_FOLDER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final int USERNAME_SEGMENT_MAX_LEN = 96;

    @Inject
    GalleryMediaStore galleryMediaStore;

    @Inject
    MediaService mediaService;

    @Inject
    DrillItemService drillItemService;

    @Inject
    UserService userService;

    @Inject
    S3StorageService s3StorageService;

    public boolean isS3Enabled() {
        return s3StorageService.isEnabled();
    }

    public String getStoredThumbnailUrl(UUID mediaId) {
        MediaRow media = requireMediaRow(mediaId);
        String usernameSegment = requireUsernameSegment(media.getCreatedById());
        File localFile = new File(galleryMediaStore.getGalleryMediaPath(mediaId), MediaConstants.STILL_FRAME_FILE_NAME);
        return s3StorageService.getObjectUrl(getStorageKey(localFile, media, usernameSegment));
    }

    public void syncDrillItemMediaToStorage(UUID drillItemId, UUID mediaId) {
        StoredMediaUrls storedMediaUrls = syncMediaToStorage(mediaId);
        if (storedMediaUrls.thumbnailUrl() != null && !storedMediaUrls.thumbnailUrl().isBlank()) {
            drillItemService.updateMediaThumbnail(drillItemId, storedMediaUrls.thumbnailUrl());
        }
    }

    public void finalizeDrillItemMediaStorage(UUID drillItemId, UUID mediaId) {
        syncDrillItemMediaToStorage(drillItemId, mediaId);
        cleanupLocalMediaIfReady(mediaId, false);
    }

    public void syncDrillMediaToStorage(UUID mediaId) {
        syncMediaToStorage(mediaId);
    }

    public void finalizeDrillMediaStorage(UUID mediaId, boolean streamingExpected) {
        syncMediaToStorage(mediaId);
        cleanupLocalMediaIfReady(mediaId, streamingExpected);
    }

    private StoredMediaUrls syncMediaToStorage(UUID mediaId) {
        if (!s3StorageService.isEnabled()) {
            return StoredMediaUrls.empty();
        }

        File mediaFolder = new File(galleryMediaStore.getGalleryMediaPath(mediaId));
        if (!mediaFolder.exists() || !mediaFolder.isDirectory()) {
            throw new IllegalStateException("Gallery media folder not found for mediaId=" + mediaId
                    + " path=" + mediaFolder.getAbsolutePath());
        }

        MediaRow media = requireMediaRow(mediaId);
        String usernameSegment = requireUsernameSegment(media.getCreatedById());

        try (Stream<Path> files = Files.walk(mediaFolder.toPath())) {
            files.filter(Files::isRegularFile)
                    .forEach(path -> s3StorageService.uploadFile(
                            path.toFile(),
                            getStorageKey(path.toFile(), media, usernameSegment),
                            detectContentType(path.toFile())));
        } catch (IOException e) {
            throw new IllegalStateException("Failed uploading gallery folder to S3 for mediaId=" + mediaId, e);
        }

        String videoUrl = null;
        File inputVideoFile = new File(galleryMediaStore.getGalleryInputPath(mediaId));
        if (inputVideoFile.exists()) {
            videoUrl = s3StorageService.getObjectUrl(getStorageKey(inputVideoFile, media, usernameSegment));
            mediaService.updateContentUrl(mediaId, videoUrl);
        } else {
            logger.infof("Skipping content_url update for mediaId=%s because input video is not ready yet at %s",
                    mediaId, inputVideoFile.getAbsolutePath());
        }

        String thumbnailUrl = null;
        File stillFrameFile = new File(galleryMediaStore.getGalleryStillFramePath(mediaId));
        if (stillFrameFile.exists()) {
            thumbnailUrl = s3StorageService.getObjectUrl(getStorageKey(stillFrameFile, media, usernameSegment));
        } else {
            logger.warnf("Still frame not found for mediaId=%s, skipping S3 thumbnail url update", mediaId);
        }

        return new StoredMediaUrls(videoUrl, thumbnailUrl);
    }

    private void cleanupLocalMediaIfReady(UUID mediaId, boolean streamingExpected) {
        if (!s3StorageService.isEnabled()) {
            return;
        }

        File mediaFolder = new File(galleryMediaStore.getGalleryMediaPath(mediaId));
        if (!mediaFolder.exists() || !mediaFolder.isDirectory()) {
            return;
        }

        if (!isReadyForCleanup(mediaId, streamingExpected)) {
            logger.infof("Skipping local gallery cleanup for mediaId=%s because processing is not finished yet",
                    mediaId);
            return;
        }

        try {
            FileUtils.deleteDirectory(mediaFolder);
            logger.infof("Deleted local gallery media folder after S3 sync: %s", mediaFolder.getAbsolutePath());
        } catch (IOException e) {
            logger.errorf(e, "Failed deleting local gallery media folder after S3 sync for mediaId=%s", mediaId);
        }
    }

    private boolean isReadyForCleanup(UUID mediaId, boolean streamingExpected) {
        File inputVideoFile = new File(galleryMediaStore.getGalleryInputPath(mediaId));
        File stillFrameFile = new File(galleryMediaStore.getGalleryStillFramePath(mediaId));
        File processingFile = new File(galleryMediaStore.getGalleryProcessingFilePath(mediaId));

        if (!inputVideoFile.exists() || !stillFrameFile.exists()) {
            return false;
        }

        if (processingFile.exists()) {
            return false;
        }

        return !streamingExpected || hasStreamingArtifacts(mediaId);
    }

    private boolean hasStreamingArtifacts(UUID mediaId) {
        File mediaFolder = new File(galleryMediaStore.getGalleryMediaPath(mediaId));
        if (!mediaFolder.exists()) {
            return false;
        }

        try (Stream<Path> files = Files.walk(mediaFolder.toPath())) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .anyMatch(fileName -> fileName.endsWith(".m3u8")
                            || fileName.endsWith(".ts")
                            || fileName.matches("(240|360|480|720|1080)\\.mp4"));
        } catch (IOException e) {
            logger.warnf(e, "Failed checking streaming artifacts for mediaId=%s", mediaId);
            return false;
        }
    }

    private MediaRow requireMediaRow(UUID mediaId) {
        MediaRow media = mediaService.findById(mediaId)
                .orElseThrow(() -> new IllegalStateException("Media not found for mediaId=" + mediaId));
        if (media.getCreatedById() == null) {
            throw new IllegalStateException("Media missing createdById for mediaId=" + mediaId);
        }
        return media;
    }

    private String requireUsernameSegment(UUID userId) {
        UserRow user = userService.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for gallery S3 prefix userId=" + userId));
        String segment = sanitizeUsernameForS3Path(user.getUsername());
        if (segment.isEmpty()) {
            segment = "user";
        }
        return segment;
    }

    /**
     * Safe single path segment for S3: no slashes, control chars, or other characters that break keys or URLs.
     */
    static String sanitizeUsernameForS3Path(String username) {
        if (username == null || username.isBlank()) {
            return "";
        }
        String trimmed = username.trim();
        StringBuilder sb = new StringBuilder(Math.min(trimmed.length(), USERNAME_SEGMENT_MAX_LEN));
        boolean lastUnderscore = false;
        for (int i = 0; i < trimmed.length() && sb.length() < USERNAME_SEGMENT_MAX_LEN; i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_') {
                sb.append(c);
                lastUnderscore = false;
            } else if (!Character.isISOControl(c)) {
                if (!lastUnderscore) {
                    sb.append('_');
                    lastUnderscore = true;
                }
            }
        }
        String s = sb.toString();
        while (s.endsWith("_") || s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /** UTC calendar date for S3 prefixes, formatted {@code dd-MM-yyyy}. */
    private static String galleryDateFolder(Long creationDateEpochMillis) {
        long millis = creationDateEpochMillis != null ? creationDateEpochMillis : System.currentTimeMillis();
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(GALLERY_DATE_FOLDER);
    }

    /**
     * One “folder” segment for the owner: {@code <dd-MM-yyyy>_<username>_<userId>}, then {@code mediaId/} as before.
     */
    private String getStorageKey(File file, MediaRow media, String usernameSegment) {
        UUID mediaId = media.getId();
        UUID userId = media.getCreatedById();
        String datePart = galleryDateFolder(media.getCreationDate());
        String ownerSegment = datePart + "_" + usernameSegment + "_" + userId;

        Path mediaRoot = Path.of(galleryMediaStore.getMediaStorePath()).toAbsolutePath().normalize();
        Path galleryBase = Path.of(galleryMediaStore.getGalleryMediaPath(mediaId)).toAbsolutePath().normalize();
        Path filePath = file.toPath().toAbsolutePath().normalize();

        if (!filePath.startsWith(mediaRoot)) {
            throw new IllegalArgumentException("File is outside media root: " + filePath);
        }
        if (!filePath.startsWith(galleryBase)) {
            throw new IllegalArgumentException(
                    "File is outside gallery media folder for mediaId=" + mediaId + ": " + filePath);
        }

        String relative = galleryBase.relativize(filePath).toString().replace(File.separatorChar, '/');
        return GALLERY_S3_PREFIX + "/" + ownerSegment + "/" + mediaId + "/" + relative;
    }

    private String detectContentType(File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".mp4")) {
            return MediaConstants.MP4_VIDEO_MIMETYPE;
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return MediaConstants.STILL_FRAME_MIMETYPE;
        }
        if (fileName.endsWith(".m3u8")) {
            return MediaConstants.HLS_VIDEO_MIMETYPE;
        }
        if (fileName.endsWith(".ts")) {
            return MediaConstants.TS_VIDEO_MIMETYPE;
        }
        if (fileName.endsWith(".json")) {
            return "application/json";
        }
        return "application/octet-stream";
    }

    private record StoredMediaUrls(String videoUrl, String thumbnailUrl) {
        private static StoredMediaUrls empty() {
            return new StoredMediaUrls(null, null);
        }
    }
}
