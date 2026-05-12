package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;
import com.lektralabs.thrones.pallbearer.common.DrillStatusConstants;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.AthleteDrillDetailDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillItemRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.MediaRow;
import com.lektralabs.thrones.pallbearer.manager.AthleteDrillItemLockManager;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillAttemptHistoryResponse;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillAttemptHistoryRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.TagRow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Provides search and submit business logic for drills in the context of an
 * Athlete user.
 */
@ApplicationScoped
public class AthleteDrillService {

    private static Logger logger = LoggerFactory.getLogger(AthleteDrillService.class);

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    AthleteDrillItemLockManager athleteDrillItemLockManager;

    @Inject
    DrillItemService drillItemService;

    @Inject
    DrillService drillService;

    @Inject
    CoachDrillService coachService;

    @Inject
    GalleryMediaService galleryMediaService;

    @Inject
    DrillAttemptHistoryService drillAttemptHistoryService;

    @Inject
    DrillGroupService drillGroupService;

    @Inject
    MediaService mediaService;

    @Inject
    DrillTagService drillTagService;

    private AthleteDrillDetailDao athleteDrillDetailDao;

    @Inject
    @ConfigProperty(name = "pallbearer.server.base-url", defaultValue = "http://34.236.102.26:8000")
    String serverBaseUrl;

    @PostConstruct
    public void init() {
        this.athleteDrillDetailDao = jdbiProvider.getJdbi().onDemand(AthleteDrillDetailDao.class);
    }

    /**
     * Find athlete drill detail for the specified athlete and drill item.
     *
     * @param athleteUserId Athlete user ID
     * @param drillItemId   Drill item ID
     * @return Athlete drill details
     */
    public Optional<AthleteDrillDetail> findWithAthleteAndDrillItem(UUID athleteUserId,
            UUID drillItemId) {
        return athleteDrillDetailDao
                .getByAthleteAndDrillItem(athleteUserId, drillItemId);
    }

    /**
     * Find athlete drill details for the specified athlete and drill item
     * group. The returned athlete drill details include lock status on drill
     * items
     *
     * @param athleteUserId Athlete user ID
     * @param drillGroupId  Drill item group ID
     * @param findOptions   Find options
     * @return Athlete drill details
     */
    public List<AthleteDrillDetail> findWithAthleteAndGroup(UUID athleteUserId,
            UUID drillGroupId,
            FindOptions findOptions) {
        List<AthleteDrillDetail> athleteDrillDetails = athleteDrillDetailDao.getByAthleteAndGroup(athleteUserId,
                drillGroupId, findOptions);

        // Progressive locking (commented out for now - uncomment to restore)
        // athleteDrillDetails =
        // athleteDrillItemLockManager.manageLockStatusUsingOrderIndex(
        // athleteUserId, drillGroupId, athleteDrillDetails);

        // Unlock all drills - bypass progressive locking (current active)
        athleteDrillDetails = athleteDrillItemLockManager.unlockAllDrills(
                athleteUserId, drillGroupId, athleteDrillDetails);

        for (AthleteDrillDetail detail : athleteDrillDetails) {
            if (detail.getDrillDetail().isPresent()) {
                DrillDetail drillDetail = detail.getDrillDetail().get();
                List<DrillAttemptHistoryRow> historyRows = drillAttemptHistoryService
                        .findByDrillIdAndUserId(drillDetail.getId(), drillDetail.getUserId());
                drillDetail.setAttemptHistory(historyRows.stream()
                        .map(r -> DrillAttemptHistoryResponse.from(r, serverBaseUrl))
                        .collect(Collectors.toList()));
            } else {
                // Create default DrillDetail when athlete hasn't attempted the drill
                DrillDetail defaultDrillDetail = DrillDetail.builder()
                        .id(null) // No drill submission exists yet
                        .drillItemId(detail.getDrillItemId())
                        .userId(athleteUserId)
                        .mediaId(Optional.empty())
                        .mediaStatus(Optional.empty())
                        .drillStatus(DrillStatusConstants.NOT_ATTEMPTED)
                        .creationDate(null)
                        .modificationDate(null)
                        .createdByUserDetail(null)
                        .modifiedByUserDetail(null)
                        .version(null)
                        .attemptsDetected(0)
                        .attemptsReported(0)
                        .makesDetected(0)
                        .makesReported(0)
                        .attemptHistory(new java.util.ArrayList<>())
                        .build();
                detail.setDrillDetail(Optional.of(defaultDrillDetail));
            }

            // Convert local file path to URL if mediaThumbnail is a local path
            if (detail.getMediaThumbnail() != null && !detail.getMediaThumbnail().isEmpty()) {
                String thumbnailPath = detail.getMediaThumbnail();
                // Check if it's a local file path (starts with /)
                if (thumbnailPath.startsWith("/") && !thumbnailPath.startsWith("http")) {
                    // Convert local path to URL
                    String thumbnailUrl = convertLocalPathToUrl(thumbnailPath);
                    detail.setMediaThumbnail(thumbnailUrl);
                }
                // If it's already a URL (http/https), leave it as is
            }
        }

        return athleteDrillDetails;
    }

    /**
     * Find the timeline of completed drills for the specified athlete
     *
     * @param athleteUserId Athlete user ID
     * @param findOptions   Find options
     * @return Completed athlete drills
     */
    public List<AthleteDrillDetail> findWithAthleteTimeline(UUID athleteUserId,
            FindOptions findOptions) {
        List<AthleteDrillDetail> athleteDrillDetails = athleteDrillDetailDao
                .getByAthleteTimeline(athleteUserId, findOptions);

        return athleteDrillDetails.stream().map(row -> {
            /// TODO - this can come from a case statement in the query
            row.setIsLocked(false);
            return row;
        }).toList();
    }

    /**
     * Create an athlete drill submission for a drill item and attach video
     * media
     * <p>
     * When an athlete submits media for a drill item this creates their drill
     * entry for the item
     *
     * @param drillItemId      Drill item ID
     * @param athleteUserId    Athlete user ID
     * @param fileName         Video file name
     * @param videoFile        Video file handle
     * @param attemptsReported Athlete reported attempts count
     * @param makesReported    Athlete reported makes count
     * @return DrillRow with all submission details including drillId, drillItemId,
     *         mediaId, and userId
     */
    public Optional<DrillRow> drillSubmission(UUID drillItemId,
            UUID athleteUserId,
            String fileName,
            File videoFile,
            int attemptsReported,
            int makesReported) {

        Optional<DrillItemRow> maybeDrillItem = drillItemService.findById(drillItemId);

        if (maybeDrillItem.isPresent()) {
            logger.info("drill item present ");
            DrillItemRow drillItemRow = maybeDrillItem.get();
            return drillSubmission(drillItemRow, athleteUserId,
                    fileName, videoFile, attemptsReported, makesReported);
        } else {
            // something is wrong...
            logger.info("drill item not present ");
            return Optional.empty();
        }
    }

    private Optional<DrillRow> drillSubmission(DrillItemRow drillItemRow,
            UUID athleteUserId,
            String fileName,
            File videoFile,
            int attemptsReported,
            int makesReported) {
        Optional<DrillRow> maybeDrill = drillService
                .findByDrillItemIdAndUserId(drillItemRow.getId(), athleteUserId);

        if (maybeDrill.isPresent()) {
            DrillRow drillRow = maybeDrill.get();
            logger.info("drill row is present");
            logger.info("Athlete drill service adding drill media"
                    + " for athlete {} to existing drill {} missing media",
                    athleteUserId.toString(), drillRow.getId());

            addDrillMedia(drillRow, fileName, videoFile);

            // Refresh drill row to get updated mediaId
            return drillService.findById(drillRow.getId());
        } else {
            logger.info("Athlete drill service creating athlete drill"
                    + " for athlete {} drill item {}",
                    athleteUserId.toString(), drillItemRow.getId());
            UUID drillId = drillService.create(DrillPartial.builder()
                    .drillItemId(drillItemRow.getId())
                    .userId(athleteUserId)
                    .drillStatus(DrillStatusConstants.PROCESSING)
                    .attemptsDetected(0)
                    .attemptsReported(attemptsReported)
                    .makesDetected(0)
                    .makesReported(makesReported)
                    .build());

            logger.info("Athlete drill service created drill submission row"
                    + " ID={} with drill item ID={} for athlete ID={}",
                    drillId, drillItemRow.getId(), athleteUserId);

            Optional<DrillRow> maybeNewDrill = drillService.findById(drillId);

            return drillSubmission(maybeNewDrill, fileName, videoFile);
        }
    }

    private Optional<DrillRow> drillSubmission(Optional<DrillRow> maybeDrillRow,
            String fileName,
            File videoFile) {
        if (maybeDrillRow.isPresent()) {
            DrillRow drillRow = maybeDrillRow.get();

            addDrillMedia(drillRow, fileName, videoFile);

            // Refresh drill row to get updated mediaId
            return drillService.findById(drillRow.getId());
        } else {
            // something is wrong...
            return Optional.empty();
        }
    }

    private Optional<UUID> addDrillMedia(DrillRow drillRow,
            String fileName,
            File videoFile) {

        logger.info("Athlete drill service adding media to"
                + " drill ID={} and sending to model for processing",
                drillRow.getId());

        if (!drillRow.getMediaId().isPresent()) {

            Optional<UUID> maybeMediaId = galleryMediaService.addDrillMedia(
                    drillRow, fileName, videoFile);

            if (maybeMediaId.isPresent()) {
                logger.info("Athlete drill service added media ID={}"
                        + " to drill ID={}", maybeMediaId.get(), drillRow.getId());
            } else {
                logger.info("Athlete drill service failed to add media"
                        + " to drill ID={}. See logs for errors",
                        drillRow.getId());
            }

            return maybeMediaId;
        } else {
            Optional<UUID> mediaId = drillRow.getMediaId();
            logger.info("Replacing existing media for drill ID={} with media ID={}",
                    drillRow.getId(), mediaId);
            galleryMediaService.resetGalleryMediaFolder(mediaId.get());
            Optional<UUID> maybeMediaId = galleryMediaService.addDrillMedia(
                    drillRow, fileName, videoFile);
            return mediaId;
        }

    }

    public List<AthleteDrillDetail> findAllAthletesAssignedToCoach(UUID coachId) {
        // Initialize the list to collect all athlete drill details
        List<AthleteDrillDetail> allAthleteDrillDetails = new ArrayList<>();

        // Get all athletes assigned to the coach
        List<AthleteDetail> athletesAssignedToCoach = coachService.findAllAthletesAssignedToCoach(coachId);

        // Iterate through each athlete
        for (AthleteDetail athlete : athletesAssignedToCoach) {
            UUID athleteUserId = athlete.getUserId();

            // Pass the correct athleteUserId to findRecentDrillsForUser
            List<DrillRow> recentDrills = drillService.findRecentDrillsForUser(athleteUserId, 10);

            // Process each recent drill for the current athlete
            for (DrillRow drillRow : recentDrills) {
                // FIX 2 & 3: Derive mediaStatus as DrillRow does not have getMediaStatus()
                // You might want to enhance DrillRow generation or your DB schema if this
                // should be a direct field.
                Optional<String> derivedMediaStatus = Optional.empty();
                if (drillRow.getMediaId().isPresent()) {
                    // This is a simplified derivation. You might have a more complex logic
                    // to determine status (e.g., from galleryMediaService if processed).
                    derivedMediaStatus = Optional.of("UPLOADED");
                } else {
                    derivedMediaStatus = Optional.of("PENDING_UPLOAD");
                }

                DrillDetail drillDetail = DrillDetail.builder()
                        .id(drillRow.getId())
                        .drillItemId(drillRow.getDrillItemId())
                        .userId(drillRow.getUserId())
                        .mediaId(drillRow.getMediaId())
                        .mediaStatus(derivedMediaStatus) // Use the derived status
                        .drillStatus(drillRow.getDrillStatus())
                        .creationDate(drillRow.getCreationDate())
                        .modificationDate(drillRow.getModificationDate())
                        // TODO: Populate createdByUserDetail and modifiedByUserDetail if available
                        .version(drillRow.getVersion())
                        .attemptsDetected(drillRow.getAttemptsDetected())
                        .attemptsReported(drillRow.getAttemptsReported())
                        .makesDetected(drillRow.getMakesDetected())
                        .makesReported(drillRow.getMakesReported())
                        .build();

                // Fetch all history rows, then find the most recent one
                List<DrillAttemptHistoryRow> historyRows = drillAttemptHistoryService
                        .findByDrillIdAndUserId(drillDetail.getId(), drillDetail.getUserId());

                // FIX 4: Correct `getCreationDate()` or use the appropriate field if it's named
                // differently.
                // Assuming DrillAttemptHistoryRow has a 'creationDate' field and its getter is
                // `getCreationDate()`
                // If the field is named differently (e.g., `createdAt`), then the getter would
                // be `getCreatedAt()`
                Optional<DrillAttemptHistoryRow> mostRecentAttempt = historyRows.stream()
                        .max(Comparator.comparing(DrillAttemptHistoryRow::getRecordedAt));
                // Verify this getter name in DrillAttemptHistoryRow

                drillDetail.setAttemptHistory(
                        mostRecentAttempt
                                .map(r -> Collections.singletonList(DrillAttemptHistoryResponse.from(r, serverBaseUrl)))
                                .orElse(Collections.emptyList()));

                // Fetch DrillItemRow to populate AthleteDrillDetail
                Optional<DrillItemRow> drillItemRow = drillItemService.findById(drillDetail.getDrillItemId());

                if (drillItemRow.isPresent()) {
                    DrillGroupRow drillGroupRow = drillItemRow.get().getDrillGroupId() != null
                            ? drillGroupService.findById(drillItemRow.get().getDrillGroupId()).orElse(null)
                            : null;

                    String mediaThumbnailUrl = null;
                    if (drillItemRow.get().getMediaId().isPresent()) {
                        UUID mediaId = drillItemRow.get().getMediaId().get();
                        mediaThumbnailUrl = galleryMediaService.getThumbnailUrl(mediaId, serverBaseUrl);

                        // Fallback to old method if new method returns null
                        if (mediaThumbnailUrl == null) {
                            mediaThumbnailUrl = generateMediaThumbnailUrl(mediaId);
                        }
                    }

                    AthleteDrillDetail athleteDrillDetail = AthleteDrillDetail.builder()
                            .drillItemId(drillItemRow.get().getId())
                            .teamId(drillItemRow.get().getTeamId())
                            .name(drillItemRow.get().getName())
                            .description(drillItemRow.get().getDescription())
                            .mediaId(drillItemRow.get().getMediaId())
                            .levelIndex(drillItemRow.get().getLevelIndex())
                            .levelTest(drillItemRow.get().getLevelTest())
                            .drillItemOrder(drillItemRow.get().getDrillItemOrder())
                            .passingScore(drillItemRow.get().getPassingScore())
                            .shotsMax(drillItemRow.get().getShotsMax())
                            .visibilityCode(drillItemRow.get().getVisibilityCode())
                            .allowRetryCode(drillItemRow.get().getAllowRetryCode())
                            .retryMax(drillItemRow.get().getRetryMax())
                            .timeLimitMs(drillItemRow.get().getTimeLimitMs())
                            .orderIndex(drillItemRow.get().getOrderIndex() != null ? drillItemRow.get().getOrderIndex() : -1)
                            .drillGroup(drillGroupRow)
                            .drillDetail(Optional.of(drillDetail))
                            .mediaThumbnail(mediaThumbnailUrl)
                            .isLocked(false)
                            .build();
                    allAthleteDrillDetails.add(athleteDrillDetail);
                } else {
                    logger.warn("Could not find DrillItemRow for drillId: {}", drillDetail.getDrillItemId());
                }
            }
        }
        return allAthleteDrillDetails;
    }

    /**
     * Find athlete drill details for the specified athlete, drill group, and level
     * index.
     * The returned athlete drill details include lock status on drill items.
     *
     * @param athleteUserId Athlete user ID
     * @param drillGroupId  Drill group ID
     * @param levelIndex    Level index to filter by
     * @param findOptions   Find options for pagination and sorting
     * @return List of athlete drill details
     */
    public List<AthleteDrillDetail> findWithAthleteGroupAndLevel(UUID athleteUserId,
            UUID drillGroupId,
            Integer levelIndex,
            FindOptions findOptions) {
        List<AthleteDrillDetail> athleteDrillDetails = athleteDrillDetailDao
                .getByAthleteGroupAndLevel(athleteUserId, drillGroupId, levelIndex, findOptions);

        // Progressive locking (commented out for now - uncomment to restore)
        // return athleteDrillItemLockManager.manageLockStatusUsingOrderIndex(
        // athleteUserId, drillGroupId, athleteDrillDetails);

        // Unlock all drills - bypass progressive locking (current active)
        return athleteDrillItemLockManager.unlockAllDrills(
                athleteUserId, drillGroupId, athleteDrillDetails);
    }

    public List<AthleteDrillDetail> findLatestAttemptedDrillsForAthleteUnderCoach(UUID coachId, UUID athleteUserId) {
        List<AthleteDrillDetail> athleteDrillDetails = new ArrayList<>();

        List<AthleteDetail> assignedAthletes = coachService.findAllAthletesAssignedToCoach(coachId);
        // logger.info(String.format("the lsit is : %s", assignedAthletes));
        boolean isAssigned = assignedAthletes.stream()
                .anyMatch(a -> a.getUserId().equals(athleteUserId));

        if (!isAssigned) {
            logger.warn("Athlete {} is not assigned to coach {}", athleteUserId, coachId);
            return Collections.emptyList();
        }

        List<DrillRow> recentDrills = drillService.findRecentDrillsForUser(athleteUserId, 50); // Fetch more to filter
                                                                                               // below

        List<AthleteDrillDetail> processedDrillDetails = new ArrayList<>();
        for (DrillRow drillRow : recentDrills) {

            Optional<DrillItemRow> drillItemRow = drillItemService.findById(drillRow.getDrillItemId());

            if (drillItemRow.isEmpty()) {
                logger.warn("Could not find DrillItemRow for drillId: {}", drillRow.getDrillItemId());
                continue;
            }

            DrillItemRow drillItem = drillItemRow.get();

            boolean isLevelTest = Boolean.TRUE.equals(drillItem.getLevelTest());
            String drillStatus = drillRow.getDrillStatus();

            if ((isLevelTest && DrillStatusConstants.NOT_ATTEMPTED.equals(drillStatus))
                    || (!isLevelTest && !DrillStatusConstants.COMPLETE.equals(drillStatus))) {
                continue; // Skip drills not matching your conditions
            }

            if (drillRow.getMediaId().isEmpty()) {
                logger.debug("Skipping drillId {} due to missing mediaId or thumbnail", drillRow.getId());
                continue;
            }

            DrillDetail drillDetail = DrillDetail.builder()
                    .id(drillRow.getId())
                    .drillItemId(drillRow.getDrillItemId())
                    .userId(drillRow.getUserId())
                    .mediaId(drillRow.getMediaId())
                    .drillStatus(drillRow.getDrillStatus())
                    .creationDate(drillRow.getCreationDate())
                    .modificationDate(drillRow.getModificationDate())
                    .version(drillRow.getVersion())
                    .attemptsDetected(drillRow.getAttemptsDetected())
                    .attemptsReported(drillRow.getAttemptsReported())
                    .makesDetected(drillRow.getMakesDetected())
                    .makesReported(drillRow.getMakesReported())
                    .build();

            List<DrillAttemptHistoryRow> historyRows = drillAttemptHistoryService
                    .findByDrillIdAndUserId(drillDetail.getId(), drillDetail.getUserId());

            Optional<DrillAttemptHistoryRow> mostRecentAttempt = historyRows.stream()
                    .max(Comparator.comparing(DrillAttemptHistoryRow::getRecordedAt));

            drillDetail.setAttemptHistory(
                    mostRecentAttempt
                            .map(r -> Collections.singletonList(DrillAttemptHistoryResponse.from(r, serverBaseUrl)))
                            .orElse(Collections.emptyList()));

            if (drillItemRow.isPresent()) {
                String mediaThumbnailUrl = null;
                if (drillDetail.getMediaId().isPresent()) {
                    UUID mediaId = drillDetail.getMediaId().get();

                    // Check if thumbnail exists and generate if not
                    // This ensures thumbnail is created from video at gallery/{mediaId}/source.mp4
                    mediaThumbnailUrl = galleryMediaService.getThumbnailUrl(mediaId, serverBaseUrl);

                    // Fallback to old method if new method returns null
                    if (mediaThumbnailUrl == null) {
                        logger.warn("Could not get thumbnail URL for mediaId: {}, drillId: {}, using fallback",
                                mediaId, drillRow.getId());
                        mediaThumbnailUrl = generateMediaThumbnailUrl(mediaId);
                    }
                }

                DrillGroupRow drillGroupRow = drillItemRow.get().getDrillGroupId() != null
                        ? drillGroupService.findById(drillItemRow.get().getDrillGroupId()).orElse(null)
                        : null;

                AthleteDrillDetail athleteDrillDetail = AthleteDrillDetail.builder()
                        .drillItemId(drillItemRow.get().getId())
                        .teamId(drillItemRow.get().getTeamId())
                        .name(drillItemRow.get().getName())
                        .description(drillItemRow.get().getDescription())
                        .mediaId(drillItemRow.get().getMediaId())
                        .levelIndex(drillItemRow.get().getLevelIndex())
                        .levelTest(drillItemRow.get().getLevelTest())
                        .drillItemOrder(drillItemRow.get().getDrillItemOrder())
                        .passingScore(drillItemRow.get().getPassingScore())
                        .shotsMax(drillItemRow.get().getShotsMax())
                        .visibilityCode(drillItemRow.get().getVisibilityCode())
                        .allowRetryCode(drillItemRow.get().getAllowRetryCode())
                        .retryMax(drillItemRow.get().getRetryMax())
                        .timeLimitMs(drillItemRow.get().getTimeLimitMs())
                        .orderIndex(drillItemRow.get().getOrderIndex() != null ? drillItemRow.get().getOrderIndex() : -1)
                        .drillGroup(drillGroupRow)
                        .drillDetail(Optional.of(drillDetail))
                        .mediaThumbnail(mediaThumbnailUrl)
                        .isLocked(false)
                        .build();

                processedDrillDetails.add(athleteDrillDetail);
            } else {
                logger.warn("Could not find DrillItemRow for drillId: {}", drillDetail.getDrillItemId());
            }
        }

        return processedDrillDetails.stream()
                .sorted((d1, d2) -> {
                    Optional<DrillAttemptHistoryResponse> h1 = d1.getDrillDetail()
                            .flatMap(DrillDetail::getMostRecentAttempt);
                    Optional<DrillAttemptHistoryResponse> h2 = d2.getDrillDetail()
                            .flatMap(DrillDetail::getMostRecentAttempt);
                    return h2.flatMap(at2 -> h1.map(at1 -> at2.getRecordedAt().compareTo(at1.getRecordedAt())))
                            .orElse(0);
                })
                .limit(10)
                .toList();
    }

    private String generateMediaThumbnailUrl(UUID mediaId) {
        // Fetch the actual contentUrl from media table
        Optional<MediaRow> mediaRow = mediaService.findById(mediaId);
        if (mediaRow.isPresent() && mediaRow.get().getContentUrl().isPresent()) {
            String contentUrl = mediaRow.get().getContentUrl().get();

            // Convert local file path to URL if it's a local path
            if (contentUrl != null && contentUrl.startsWith("/") && !contentUrl.startsWith("http")) {
                // Convert local path to URL
                String convertedUrl = convertLocalPathToVideoUrl(contentUrl);
                logger.debug("Converted local video path to URL: {} -> {}", contentUrl, convertedUrl);
                return convertedUrl;
            } else {
                // If it's already a URL (http/https), use it as-is
                return contentUrl;
            }
        } else {
            // Fallback to the old URL format if contentUrl is not available
            logger.warn("Could not find media contentUrl for mediaId: {}, using fallback URL", mediaId);
            return String.format("%s/media/thumbnail/%s/still-frame.jpg", serverBaseUrl, mediaId.toString());
        }
    }

    /**
     * Converts a local file path to a URL that can be accessed via HTTP.
     * Supports both old format (with /gallery/) and new format (directly under
     * mediaStorePath).
     * 
     * Old format:
     * {mediaStorePath}/gallery/{group}/{level}_{drill}/{mediaId}/thumbnail.jpg
     * New format: {mediaStorePath}/{groupName}/{unique_id}/thumbnail.jpg
     * 
     * @param localPath Local file path (e.g.,
     *                  "/home/ankit/Downloads/thrones-development/media/Intermediate/intermediate_1_1/thumbnail.jpg")
     * @return URL that can be used to access the file (e.g.,
     *         "http://34.236.102.26/api/media/gallery/thumbnail?path=Intermediate/intermediate_1_1/thumbnail.jpg")
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
            // Check if path matches new format: ends with /thumbnail.jpg and has structure
            // {group}/{unique_id}/thumbnail.jpg
            String[] pathParts = localPath.split("/");
            if (pathParts.length >= 3 && pathParts[pathParts.length - 1].equals("thumbnail.jpg")) {
                // Find the directories before thumbnail.jpg
                // Last part is "thumbnail.jpg", second last is {unique_id}, third last is
                // {groupName}
                String uniqueId = pathParts[pathParts.length - 2];
                String groupName = pathParts.length >= 3 ? pathParts[pathParts.length - 3] : null;

                // Check if this looks like the new format (groupName/unique_id/thumbnail.jpg)
                // Common group names: Beginner, Intermediate, Advanced, Elite
                // (case-insensitive)
                if (groupName != null) {
                    String[] knownGroups = { "Beginner", "Intermediate", "Advanced", "Elite",
                            "beginner", "intermediate", "advanced", "elite" };

                    for (String group : knownGroups) {
                        if (groupName.equalsIgnoreCase(group)) {
                            // Found a group name, extract relative path:
                            // {groupName}/{unique_id}/thumbnail.jpg
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

                // Parse the path: gallery/{group}/{drillFolder}/{mediaId}/thumbnail.jpg
                String[] parts = relativePath.split("/");
                if (parts.length >= 5 && parts[0].equals("gallery")
                        && parts[parts.length - 1].equals("thumbnail.jpg")) {
                    // parts[1] = group (e.g., "Beginner")
                    // parts[2] = drillFolder (e.g., "1_15_ft_One-Dribble_Pull-Up_Right")
                    // parts[3] = mediaId (e.g., "08c6b31c-b81b-4835-b06c-caabe1008f8c")
                    String group = parts[1];
                    String drillFolder = parts[2];
                    String mediaId = parts[3];

                    try {
                        // JAX-RS automatically decodes path parameters, so we need to URL encode them
                        String encodedGroup = java.net.URLEncoder.encode(group, java.nio.charset.StandardCharsets.UTF_8)
                                .replace("+", "%20");
                        String encodedDrillFolder = java.net.URLEncoder
                                .encode(drillFolder, java.nio.charset.StandardCharsets.UTF_8)
                                .replace("+", "%20");
                        String encodedMediaId = java.net.URLEncoder
                                .encode(mediaId, java.nio.charset.StandardCharsets.UTF_8)
                                .replace("+", "%20");

                        // Use path-based endpoint:
                        // /api/media/gallery/thumbnail/{group}/{drillFolder}/{mediaId}/thumbnail.jpg
                        String url = String.format("%s/api/media/gallery/thumbnail/%s/%s/%s/thumbnail.jpg",
                                serverBaseUrl, encodedGroup, encodedDrillFolder, encodedMediaId);
                        logger.debug("Converted local path {} to URL: {}", localPath, url);
                        return url;
                    } catch (Exception e) {
                        logger.warn("Error encoding path components: " + relativePath, e);
                    }
                }
            }
        }

        // If we have a relative path (either new or old format), use query parameter
        // endpoint
        if (relativePath != null) {
            try {
                String encodedPath = java.net.URLEncoder.encode(relativePath, java.nio.charset.StandardCharsets.UTF_8)
                        .replace("+", "%20");
                String url = String.format("%s/api/media/gallery/thumbnail?path=%s", serverBaseUrl, encodedPath);
                logger.debug("Converted local path {} to URL: {}", localPath, url);
                return url;
            } catch (Exception e) {
                logger.warn("Error encoding path: " + relativePath, e);
                return String.format("%s/api/media/gallery/thumbnail?path=%s", serverBaseUrl, relativePath);
            }
        }

        // Fallback: if we can't extract relative path, try to use the full path
        logger.warn("Could not extract relative path from: " + localPath);
        try {
            String encodedPath = java.net.URLEncoder.encode(localPath, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return String.format("%s/api/media/gallery/thumbnail?path=%s", serverBaseUrl, encodedPath);
        } catch (Exception e) {
            return localPath; // Return original path if we can't convert it
        }
    }

    /**
     * Find media URLs for athlete drill details for the specified athlete, drill
     * group, and level
     * index. Returns only the media-related information for client-side access.
     *
     * @param athleteUserId Athlete user ID
     * @param drillGroupId  Drill group ID
     * @param levelIndex    Level index to filter by
     * @param findOptions   Find options for pagination and sorting
     * @return List of media URLs and metadata for the drills in sequence order
     */
    public List<DrillMediaInfo> findMediaWithAthleteGroupAndLevel(UUID athleteUserId,
            UUID drillGroupId,
            Integer levelIndex,
            FindOptions findOptions) {
        List<AthleteDrillDetail> athleteDrillDetails = athleteDrillDetailDao
                .getByAthleteGroupAndLevel(athleteUserId, drillGroupId, levelIndex, findOptions);

        // Unlock all drills - bypass progressive locking (current active)
        athleteDrillDetails = athleteDrillItemLockManager.unlockAllDrills(
                athleteUserId, drillGroupId, athleteDrillDetails);

        // Create a mutable copy and sort by drillItemOrder to ensure sequence order
        // (1st drill, 2nd drill, etc.)
        List<AthleteDrillDetail> mutableList = new ArrayList<>(athleteDrillDetails);
        mutableList.sort(Comparator.comparing(AthleteDrillDetail::getDrillItemOrder,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return mutableList.stream()
                .map(this::convertToDrillMediaInfo)
                .filter(mediaInfo -> mediaInfo.getVideoUrl() != null)
                .toList();
    }

    private DrillMediaInfo convertToDrillMediaInfo(AthleteDrillDetail detail) {
        DrillMediaInfo.DrillMediaInfoBuilder builder = DrillMediaInfo.builder()
                .drillItemId(detail.getDrillItemId());

        // Add drill item video URL if mediaId exists
        if (detail.getMediaId().isPresent()) {
            UUID mediaId = detail.getMediaId().get();

            // Fetch the actual contentUrl from media table
            Optional<MediaRow> mediaRow = mediaService.findById(mediaId);
            if (mediaRow.isPresent() && mediaRow.get().getContentUrl().isPresent()) {
                String contentUrl = mediaRow.get().getContentUrl().get();

                // Convert local file path to URL if it's a local path
                if (contentUrl != null && contentUrl.startsWith("/") && !contentUrl.startsWith("http")) {
                    // Convert local path to URL
                    String convertedUrl = convertLocalPathToVideoUrl(contentUrl);
                    logger.debug("Converted local video path to URL: {} -> {}", contentUrl, convertedUrl);
                    builder.videoUrl(convertedUrl);
                } else {
                    // If it's already a URL (http/https), use it as-is
                    builder.videoUrl(contentUrl);
                }
            } else {
                // Fallback to the old URL format if contentUrl is not available
                String drillItemVideoUrl = String.format("%s/api/media/gallery/drill_item/%s/video.mp4",
                        serverBaseUrl, detail.getDrillItemId().toString());
                builder.videoUrl(drillItemVideoUrl);
            }
        } else {
            // If no mediaId, set videoUrl to null
            builder.videoUrl(null);
        }

        return builder.build();
    }

    /**
     * Converts a local video file path to a URL that can be accessed via HTTP.
     * Supports both old format (with /gallery/) and new format (directly under
     * mediaStorePath).
     * 
     * Old format:
     * {mediaStorePath}/gallery/{group}/{level}_{drill}/{mediaId}/video.mp4
     * New format: {mediaStorePath}/{groupName}/{unique_id}/video.mp4
     * 
     * @param localPath Local file path (e.g.,
     *                  "/home/ankit/Downloads/thrones-development/media/Intermediate/intermediate_1_1/video.mp4")
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
            // Check if path matches new format: ends with /video.mp4 and has structure
            // {group}/{unique_id}/video.mp4
            String[] pathParts = localPath.split("/");
            if (pathParts.length >= 3 && pathParts[pathParts.length - 1].equals("video.mp4")) {
                // Find the directories before video.mp4
                // Last part is "video.mp4", second last is {unique_id}, third last is
                // {groupName}
                String uniqueId = pathParts[pathParts.length - 2];
                String groupName = pathParts.length >= 3 ? pathParts[pathParts.length - 3] : null;

                // Check if this looks like the new format (groupName/unique_id/video.mp4)
                // Common group names: Beginner, Intermediate, Advanced, Elite
                // (case-insensitive)
                if (groupName != null) {
                    String[] knownGroups = { "Beginner", "Intermediate", "Advanced", "Elite",
                            "beginner", "intermediate", "advanced", "elite" };

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

        // If we have a relative path (either new or old format), use query parameter
        // endpoint
        if (relativePath != null) {
            try {
                String encodedPath = java.net.URLEncoder.encode(relativePath, java.nio.charset.StandardCharsets.UTF_8)
                        .replace("+", "%20"); // Replace + with %20 for spaces
                String url = String.format("%s/api/media/gallery/video?path=%s", serverBaseUrl, encodedPath);
                logger.debug("Converted local video path {} to URL: {}", localPath, url);
                return url;
            } catch (Exception e) {
                logger.warn("Error encoding video path: " + relativePath, e);
                return String.format("%s/api/media/gallery/video?path=%s", serverBaseUrl, relativePath);
            }
        }

        // Fallback: if we can't extract relative path, try to use the full path
        logger.warn("Could not extract relative path from: " + localPath);
        try {
            String encodedPath = java.net.URLEncoder.encode(localPath, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return String.format("%s/api/media/gallery/video?path=%s", serverBaseUrl, encodedPath);
        } catch (Exception e) {
            return localPath; // Return original path if we can't convert it
        }
    }

    /**
     * Find the full drill curriculum for the specified athlete across all skill
     * groups, scoped to a coach. Returns every drill item regardless of attempt
     * status. If the athlete has attempted a drill, drillDetail is populated with
     * the full attempt history. If the athlete has not attempted a drill,
     * drillDetail is null in the response.
     *
     * @param coachId       Coach user ID — athlete must be assigned to this coach
     * @param athleteUserId Athlete user ID
     * @return All drill items across Beginner, Intermediate, Advanced, and Elite
     *         groups with attempt data where available
     */
    public List<AthleteDrillDetail> findFullCurriculumForAthleteUnderCoach(UUID coachId, UUID athleteUserId) {
        List<AthleteDetail> assignedAthletes = coachService.findAllAthletesAssignedToCoach(coachId);
        boolean isAssigned = assignedAthletes.stream()
                .anyMatch(a -> a.getUserId().equals(athleteUserId));

        if (!isAssigned) {
            logger.warn("Athlete {} is not assigned to coach {}", athleteUserId, coachId);
            return Collections.emptyList();
        }

        FindOptions findOptions = new FindOptions();
        List<AthleteDrillDetail> allDrills = new ArrayList<>();

        List<UUID> groupIds = List.of(
                DrillGroupConstants.BEGINNER_GROUP_ID,
                DrillGroupConstants.INTERMEDIATE_GROUP_ID,
                DrillGroupConstants.ADVANCE_GROUP_ID,
                DrillGroupConstants.ELITE_GROUP_ID);

        for (UUID groupId : groupIds) {
            List<AthleteDrillDetail> groupDrills = findWithAthleteAndGroup(athleteUserId, groupId, findOptions);
            for (AthleteDrillDetail detail : groupDrills) {
                if (detail.getDrillDetail().isPresent()) {
                    DrillDetail dd = detail.getDrillDetail().get();
                    if (DrillStatusConstants.NOT_ATTEMPTED.equals(dd.getDrillStatus())) {
                        detail.setDrillDetail(Optional.empty());
                    }
                }
            }
            allDrills.addAll(groupDrills);
        }

        return allDrills;
    }

    public List<AthleteDrillDetail> findCompletedDrillsForAthleteUnderCoach(
        UUID coachId, UUID athleteId, UUID drillGroupId, List<String> tagCodes, int page, int limit) {
        List<AthleteDetail> assignedAthletes = coachService.findAllAthletesAssignedToCoach(coachId);
        boolean isAssigned = assignedAthletes.stream().anyMatch(a -> a.getUserId().equals(athleteId));
        if (!isAssigned) {
            return Collections.emptyList();
        }

        int offset = page * limit;
        List<AthleteDrillDetail> results = athleteDrillDetailDao.getCompletedByAthleteWithFilters(
                athleteId, drillGroupId, (tagCodes == null || tagCodes.isEmpty()) ? null : tagCodes, limit, offset);

        // attach tags to each result
        List<UUID> drillItemIds = results.stream()
            .map(AthleteDrillDetail::getDrillItemId)
            .collect(Collectors.toList());
        Map<UUID, List<TagRow>> tagsByDrillItem = drillTagService.getTagsForDrillItems(drillItemIds);
        results.forEach(detail ->
            detail.setTags(tagsByDrillItem.getOrDefault(detail.getDrillItemId(), Collections.emptyList())));

        return results;
    }

    /**
     * Data class to hold media information for drill items - simplified to only
     * include drillItemId and videoUrl
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrillMediaInfo {
        private UUID drillItemId;
        private String videoUrl;
    }

}
