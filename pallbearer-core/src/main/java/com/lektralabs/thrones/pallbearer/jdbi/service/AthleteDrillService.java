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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

        // Batch the attempt history for every attempted drill in this group in one
        // round-trip, keyed by drill id, instead of one query per drill in the loop.
        List<UUID> attemptedDrillIds = athleteDrillDetails.stream()
                .filter(d -> d.getDrillDetail().isPresent() && d.getDrillDetail().get().getId() != null)
                .map(d -> d.getDrillDetail().get().getId())
                .collect(java.util.stream.Collectors.toList());
        Map<UUID, List<DrillAttemptHistoryRow>> historyByDrillId = drillAttemptHistoryService
                .findByDrillIdsAndUserId(attemptedDrillIds, athleteUserId);

        for (AthleteDrillDetail detail : athleteDrillDetails) {
            if (detail.getDrillDetail().isPresent()) {
                DrillDetail drillDetail = detail.getDrillDetail().get();
                List<DrillAttemptHistoryRow> historyRows = historyByDrillId
                        .getOrDefault(drillDetail.getId(), Collections.emptyList());
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

        // Always create a new media record/folder per submission so each attempt
        // keeps its own video instead of overwriting a previous attempt's file.
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

    /**
     * Build the coach-facing drill detail for an athlete across ALL difficulty
     * groups, combining same-named drills into a single entry.
     * <p>
     * A logical drill (e.g. "Free Throw Routine") exists as a separate
     * {@code t_drill_item} in every difficulty group and at every level, all
     * sharing the same name. The "My Drills" list ({@code /drills/completed})
     * sums those variants together by name, so the detail must do the same or
     * the per-round breakdown won't match the list number (the original
     * symptom: list shows a total, tapping it opened a current-difficulty-only,
     * often-empty screen).
     * <p>
     * This therefore:
     * <ul>
     *   <li>includes EVERY {@code COMPLETE} drill — the same inclusion rule the
     *       list query uses — with no recency limit, so every list row has a
     *       matching detail entry;</li>
     *   <li>groups variants by {@code LOWER(BTRIM(name))}, the exact key the iOS
     *       app merges the list on, producing exactly one entry per name;</li>
     *   <li>unions every round across all variants into {@code attemptHistory}
     *       and sums totals per ROUND from {@code t_drill_attempt_history} (with a
     *       fall-back to the drill summary row for any drill with no history) so
     *       the detail total equals the list number;</li>
     *   <li>fills the single-valued metadata fields (drillGroup, levelIndex,
     *       passingScore, mediaId, thumbnail, drillItemId) from the variant of the
     *       most recent attempt — the detail screen does not display these, it
     *       only uses name, the drillDetail totals and attemptHistory.</li>
     * </ul>
     * <p>
     * The {@code scope} parameter controls the grouping granularity, because the
     * same endpoint backs two contradictory contracts:
     * <ul>
     *   <li>{@code "name"} (default) — group by {@code LOWER(BTRIM(name))}, the
     *       all-difficulty union the "My Drills" round-history screen requires;</li>
     *   <li>{@code "item"} — group by {@code drillItemId}, so the notification
     *       drill-detail and the athlete "Video" tab each collapse to the single
     *       drill item the athlete actually did (no cross-difficulty union).</li>
     * </ul>
     * Only the grouping key differs; the round-union, totals summation,
     * representative selection and sort are identical for both scopes.
     */
    public List<AthleteDrillDetail> findLatestAttemptedDrillsForAthleteUnderCoach(UUID coachId, UUID athleteUserId) {
        return findLatestAttemptedDrillsForAthleteUnderCoach(coachId, athleteUserId, "name");
    }

    public List<AthleteDrillDetail> findLatestAttemptedDrillsForAthleteUnderCoach(UUID coachId, UUID athleteUserId,
            String scope) {
        boolean groupByItem = "item".equalsIgnoreCase(scope);
        boolean roundLevel = "round".equalsIgnoreCase(scope);
        List<AthleteDetail> assignedAthletes = coachService.findAllAthletesAssignedToCoach(coachId);
        boolean isAssigned = assignedAthletes.stream()
                .anyMatch(a -> a.getUserId().equals(athleteUserId));

        if (!isAssigned) {
            logger.warn("Athlete {} is not assigned to coach {}", athleteUserId, coachId);
            return Collections.emptyList();
        }

        // Every drill the athlete has ever submitted, across all difficulty groups.
        List<DrillRow> allDrills = drillService.findAllDrillsForUser(athleteUserId);

        // scope=round: one entry per PASSING round (one round = one video). Used by the
        // coach Video tab ("Previous 10 Drills"). No grouping/summation.
        if (roundLevel) {
            return buildPassingRoundDetails(allDrills);
        }

        // Group COMPLETE drills by the scope key. LinkedHashMap preserves first-seen
        // order; the final list is re-sorted by most-recent attempt below.
        // scope=name -> LOWER(BTRIM(name)) (cross-difficulty union for My Drills)
        // scope=item -> drillItemId (single drill item for notifications / Video tab)
        Map<String, List<DrillRow>> drillsByGroupKey = new LinkedHashMap<>();
        Map<UUID, DrillItemRow> drillItemCache = new HashMap<>();
        // Resolve each distinct drill group at most once per request (only 4 exist),
        // replacing the per-drill drillGroupService.findById in assembleDetail.
        Map<UUID, DrillGroupRow> drillGroupCache = new HashMap<>();

        for (DrillRow drillRow : allDrills) {
            if (!DrillStatusConstants.COMPLETE.equals(drillRow.getDrillStatus())) {
                continue;
            }

            DrillItemRow drillItem = drillItemCache.computeIfAbsent(drillRow.getDrillItemId(),
                    id -> drillItemService.findById(id).orElse(null));
            if (drillItem == null || drillItem.getName().isEmpty()) {
                logger.warn("Could not resolve drill item / name for drillId: {}", drillRow.getId());
                continue;
            }

            String groupKey;
            if (groupByItem) {
                // One entry per drill item: no cross-difficulty union.
                groupKey = drillRow.getDrillItemId().toString();
            } else {
                // Mirror the iOS app's merge key exactly: lowercase + trim leading/trailing
                // whitespace, no internal-whitespace collapse, no punctuation normalization
                // (equivalent to Postgres LOWER(BTRIM(name))).
                groupKey = drillItem.getName().get().toLowerCase(java.util.Locale.ROOT).strip();
            }
            drillsByGroupKey.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(drillRow);
        }

        // Batch every grouped drill's attempt history in a single round-trip, keyed by
        // drill id. Replaces the former per-drill findByDrillIdAndUserId N+1 fan-out.
        List<UUID> completeDrillIds = drillsByGroupKey.values().stream()
                .flatMap(List::stream)
                .map(DrillRow::getId)
                .collect(java.util.stream.Collectors.toList());
        Map<UUID, List<DrillAttemptHistoryRow>> historyByDrillId = drillAttemptHistoryService
                .findByDrillIdsAndUserId(completeDrillIds, athleteUserId);

        // scope=name (Stats tab) never renders a thumbnail, so skip the per-drill
        // getThumbnailUrl (which generates a still frame via ffmpeg on a cache miss).
        // scope=item consumers may still use it, so keep it there.
        boolean resolveThumbnail = groupByItem;

        List<AthleteDrillDetail> combinedDetails = new ArrayList<>();

        for (List<DrillRow> variants : drillsByGroupKey.values()) {
            List<DrillAttemptHistoryResponse> unionHistory = new ArrayList<>();
            int sumAttemptsDetected = 0;
            int sumAttemptsReported = 0;
            int sumMakesDetected = 0;
            int sumMakesReported = 0;

            DrillRow representative = null;
            Timestamp latestRecordedAt = null;

            for (DrillRow drillRow : variants) {
                List<DrillAttemptHistoryRow> historyRows = historyByDrillId
                        .getOrDefault(drillRow.getId(), Collections.emptyList());

                if (historyRows.isEmpty()) {
                    // Defence-in-depth: a COMPLETE drill with no per-round history.
                    // Fall back to the drill summary row so its totals still count.
                    sumAttemptsDetected += nullSafe(drillRow.getAttemptsDetected());
                    sumAttemptsReported += nullSafe(drillRow.getAttemptsReported());
                    sumMakesDetected += nullSafe(drillRow.getMakesDetected());
                    sumMakesReported += nullSafe(drillRow.getMakesReported());
                    if (representative == null) {
                        representative = drillRow;
                    }
                    continue;
                }

                DrillItemRow drillItemForRow = drillItemCache.get(drillRow.getDrillItemId());
                for (DrillAttemptHistoryRow h : historyRows) {
                    sumAttemptsDetected += nullSafe(h.getAttemptsDetected());
                    sumAttemptsReported += nullSafe(h.getAttemptsReported());
                    sumMakesDetected += nullSafe(h.getMakesDetected());
                    sumMakesReported += nullSafe(h.getMakesReported());
                    unionHistory.add(toHistoryResponse(h, drillRow, drillItemForRow));

                    if (h.getRecordedAt() != null
                            && (latestRecordedAt == null || h.getRecordedAt().after(latestRecordedAt))) {
                        latestRecordedAt = h.getRecordedAt();
                        representative = drillRow;
                    }
                }
            }

            if (representative == null) {
                continue;
            }

            // Newest round first, matching the previous per-drill ordering.
            unionHistory.sort((a, b) -> {
                Timestamp ra = a.getRecordedAt();
                Timestamp rb = b.getRecordedAt();
                if (ra == null && rb == null) return 0;
                if (ra == null) return 1;
                if (rb == null) return -1;
                return rb.compareTo(ra);
            });

            DrillItemRow repItem = drillItemCache.get(representative.getDrillItemId());

            DrillDetail drillDetail = DrillDetail.builder()
                    .id(representative.getId())
                    .drillItemId(representative.getDrillItemId())
                    .userId(athleteUserId)
                    .mediaId(representative.getMediaId())
                    .drillStatus(DrillStatusConstants.COMPLETE)
                    .creationDate(representative.getCreationDate())
                    .modificationDate(representative.getModificationDate())
                    .version(representative.getVersion())
                    .attemptsDetected(sumAttemptsDetected)
                    .attemptsReported(sumAttemptsReported)
                    .makesDetected(sumMakesDetected)
                    .makesReported(sumMakesReported)
                    .attemptHistory(unionHistory)
                    .build();

            combinedDetails.add(assembleDetail(representative, repItem, drillDetail,
                    representative.getMediaId(), resolveThumbnail, drillGroupCache));
        }

        // Most-recently-attempted drill first. No limit: every drill in the list
        // must have a matching detail entry.
        return combinedDetails.stream()
                .sorted((d1, d2) -> {
                    Optional<DrillAttemptHistoryResponse> h1 = d1.getDrillDetail()
                            .flatMap(DrillDetail::getMostRecentAttempt);
                    Optional<DrillAttemptHistoryResponse> h2 = d2.getDrillDetail()
                            .flatMap(DrillDetail::getMostRecentAttempt);
                    return h2.flatMap(at2 -> h1.map(at1 -> at2.getRecordedAt().compareTo(at1.getRecordedAt())))
                            .orElse(0);
                })
                .toList();
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Build a {@link DrillAttemptHistoryResponse} for one round and stamp the
     * fields the row itself does not carry: the owning {@code drillItemId}, the
     * stable {@code attemptLocalId} / {@code attemptNumber} the client keys on, and
     * the server-computed {@code passed} flag.
     */
    private DrillAttemptHistoryResponse toHistoryResponse(DrillAttemptHistoryRow h, DrillRow drillRow,
            DrillItemRow drillItem) {
        DrillAttemptHistoryResponse response = DrillAttemptHistoryResponse.from(h, serverBaseUrl);
        response.setDrillItemId(drillRow.getDrillItemId());
        response.setAttemptLocalId(h.getAttemptLocalId());
        response.setAttemptNumber(h.getAttemptNumber());
        response.setPassed(isRoundPassing(h, drillItem));
        return response;
    }

    /**
     * A round passes when its reported makes meet the drill item's passing score.
     * Mirrors the coach-notification gate exactly: a null threshold counts as
     * passing. {@code makesReported} on a history row is never null (it defaults to
     * 0 at insert), so the reported value is authoritative.
     */
    private boolean isRoundPassing(DrillAttemptHistoryRow h, DrillItemRow drillItem) {
        Integer passingScore = drillItem != null ? drillItem.getPassingScore() : null;
        if (passingScore == null) {
            return true;
        }
        return nullSafe(h.getMakesReported()) >= passingScore;
    }

    /**
     * scope=round: one {@link AthleteDrillDetail} per PASSING round, using that
     * round's own (non-summed) stats and its own video. Newest round first.
     */
    private List<AthleteDrillDetail> buildPassingRoundDetails(List<DrillRow> allDrills) {
        Map<UUID, DrillItemRow> drillItemCache = new HashMap<>();
        Map<UUID, DrillGroupRow> drillGroupCache = new HashMap<>();
        List<AthleteDrillDetail> roundDetails = new ArrayList<>();

        // Batch every COMPLETE drill's history in one round-trip (all drills belong to
        // the same athlete), replacing the former per-drill findByDrillIdAndUserId N+1.
        List<UUID> completeDrillIds = allDrills.stream()
                .filter(d -> DrillStatusConstants.COMPLETE.equals(d.getDrillStatus()))
                .map(DrillRow::getId)
                .collect(java.util.stream.Collectors.toList());
        UUID athleteUserId = allDrills.isEmpty() ? null : allDrills.get(0).getUserId();
        Map<UUID, List<DrillAttemptHistoryRow>> historyByDrillId = drillAttemptHistoryService
                .findByDrillIdsAndUserId(completeDrillIds, athleteUserId);

        for (DrillRow drillRow : allDrills) {
            if (!DrillStatusConstants.COMPLETE.equals(drillRow.getDrillStatus())) {
                continue;
            }
            DrillItemRow drillItem = drillItemCache.computeIfAbsent(drillRow.getDrillItemId(),
                    id -> drillItemService.findById(id).orElse(null));
            if (drillItem == null || drillItem.getName().isEmpty()) {
                continue;
            }
            for (DrillAttemptHistoryRow h : historyByDrillId
                    .getOrDefault(drillRow.getId(), Collections.emptyList())) {
                if (!isRoundPassing(h, drillItem)) {
                    continue;
                }
                roundDetails.add(buildSingleRoundDetail(drillRow, drillItem, h, drillGroupCache));
            }
        }

        roundDetails.sort((d1, d2) -> {
            Optional<DrillAttemptHistoryResponse> a1 = d1.getDrillDetail().flatMap(DrillDetail::getMostRecentAttempt);
            Optional<DrillAttemptHistoryResponse> a2 = d2.getDrillDetail().flatMap(DrillDetail::getMostRecentAttempt);
            Timestamp r1 = a1.map(DrillAttemptHistoryResponse::getRecordedAt).orElse(null);
            Timestamp r2 = a2.map(DrillAttemptHistoryResponse::getRecordedAt).orElse(null);
            if (r1 == null && r2 == null) return 0;
            if (r1 == null) return 1;
            if (r2 == null) return -1;
            return r2.compareTo(r1);
        });
        return roundDetails;
    }

    /**
     * Build a single-round {@link AthleteDrillDetail}: drillDetail totals equal this
     * one round's values (never a sum), attemptHistory holds exactly this round, and
     * the media resolves to the round's own video (falling back to the drill-level
     * media only when the round has none).
     */
    private AthleteDrillDetail buildSingleRoundDetail(DrillRow drillRow, DrillItemRow drillItem,
            DrillAttemptHistoryRow h, Map<UUID, DrillGroupRow> drillGroupCache) {
        DrillAttemptHistoryResponse roundResponse = toHistoryResponse(h, drillRow, drillItem);

        Optional<UUID> roundMediaId = h.getMediaId() != null
                ? Optional.of(h.getMediaId())
                : drillRow.getMediaId();

        List<DrillAttemptHistoryResponse> singleHistory = new ArrayList<>();
        singleHistory.add(roundResponse);

        DrillDetail drillDetail = DrillDetail.builder()
                .id(drillRow.getId())
                .drillItemId(drillRow.getDrillItemId())
                .userId(drillRow.getUserId())
                .mediaId(roundMediaId)
                .drillStatus(DrillStatusConstants.COMPLETE)
                .creationDate(drillRow.getCreationDate())
                .modificationDate(drillRow.getModificationDate())
                .version(drillRow.getVersion())
                .attemptsDetected(nullSafe(h.getAttemptsDetected()))
                .attemptsReported(nullSafe(h.getAttemptsReported()))
                .makesDetected(nullSafe(h.getMakesDetected()))
                .makesReported(nullSafe(h.getMakesReported()))
                .attemptHistory(singleHistory)
                .build();

        // Video tab renders the per-round still frame, so resolve the thumbnail here.
        return assembleDetail(drillRow, drillItem, drillDetail, roundMediaId, true, drillGroupCache);
    }

    /**
     * Shared assembly of an {@link AthleteDrillDetail} from a representative drill
     * row, its drill item, the prepared {@link DrillDetail}, and the media id to
     * derive the card thumbnail from. Used by both the name/item union path and the
     * single-round path so they cannot drift.
     */
    private AthleteDrillDetail assembleDetail(DrillRow representative, DrillItemRow repItem,
            DrillDetail drillDetail, Optional<UUID> thumbnailMediaId, boolean resolveThumbnail,
            Map<UUID, DrillGroupRow> drillGroupCache) {
        String mediaThumbnailUrl = null;
        // getThumbnailUrl can synchronously generate a still frame (ffmpeg) on a cache
        // miss, so only pay it for callers whose client actually renders the thumbnail.
        if (resolveThumbnail && thumbnailMediaId != null && thumbnailMediaId.isPresent()) {
            UUID mediaId = thumbnailMediaId.get();
            mediaThumbnailUrl = galleryMediaService.getThumbnailUrl(mediaId, serverBaseUrl);
            if (mediaThumbnailUrl == null) {
                mediaThumbnailUrl = generateMediaThumbnailUrl(mediaId);
            }
        }

        DrillGroupRow drillGroupRow = repItem != null && repItem.getDrillGroupId() != null
                ? drillGroupCache.computeIfAbsent(repItem.getDrillGroupId(),
                        id -> drillGroupService.findById(id).orElse(null))
                : null;

        AthleteDrillDetail.AthleteDrillDetailBuilder builder = AthleteDrillDetail.builder()
                .drillDetail(Optional.of(drillDetail))
                .drillGroup(drillGroupRow)
                .mediaThumbnail(mediaThumbnailUrl)
                .isLocked(false);

        if (repItem != null) {
            builder.drillItemId(repItem.getId())
                    .teamId(repItem.getTeamId())
                    .name(repItem.getName())
                    .description(repItem.getDescription())
                    .mediaId(repItem.getMediaId())
                    .levelIndex(repItem.getLevelIndex())
                    .levelTest(repItem.getLevelTest())
                    .drillItemOrder(repItem.getDrillItemOrder())
                    .passingScore(repItem.getPassingScore())
                    .shotsMax(repItem.getShotsMax())
                    .visibilityCode(repItem.getVisibilityCode())
                    .allowRetryCode(repItem.getAllowRetryCode())
                    .retryMax(repItem.getRetryMax())
                    .timeLimitMs(repItem.getTimeLimitMs())
                    .orderIndex(repItem.getOrderIndex() != null ? repItem.getOrderIndex() : -1);
        } else {
            builder.drillItemId(representative.getDrillItemId());
        }
        return builder.build();
    }

    /**
     * Resolve the single passing round behind a coach notification. The notification
     * carries the athlete, the drill, and (for newer completions) the stable
     * {@code attemptLocalId}; that id globally and unambiguously pins the round.
     * Older notifications without it fall back to the most recent passing round of
     * the drill. Returns empty if the drill does not belong to the athlete or has no
     * matching round.
     */
    public Optional<AthleteDrillDetail> findPassingRound(UUID athleteUserId, UUID drillId, String attemptLocalId) {
        Optional<DrillRow> drillOpt = drillService.findById(drillId);
        if (drillOpt.isEmpty()) {
            return Optional.empty();
        }
        DrillRow drillRow = drillOpt.get();
        if (!athleteUserId.equals(drillRow.getUserId())) {
            logger.warn("Drill {} does not belong to athlete {}", drillId, athleteUserId);
            return Optional.empty();
        }

        DrillItemRow drillItem = drillItemService.findById(drillRow.getDrillItemId()).orElse(null);
        List<DrillAttemptHistoryRow> rows = drillAttemptHistoryService
                .findByDrillIdAndUserId(drillId, athleteUserId);

        DrillAttemptHistoryRow match = null;
        if (attemptLocalId != null && !attemptLocalId.isBlank()) {
            match = rows.stream()
                    .filter(r -> attemptLocalId.equals(r.getAttemptLocalId()))
                    .findFirst()
                    .orElse(null);
        }
        if (match == null) {
            // Fallback for notifications created before attemptLocalId was stored:
            // the most recent passing round of the drill.
            match = rows.stream()
                    .filter(r -> isRoundPassing(r, drillItem))
                    .max((a, b) -> {
                        Timestamp ra = a.getRecordedAt();
                        Timestamp rb = b.getRecordedAt();
                        if (ra == null && rb == null) return 0;
                        if (ra == null) return -1;
                        if (rb == null) return 1;
                        return ra.compareTo(rb);
                    })
                    .orElse(null);
        }
        if (match == null) {
            return Optional.empty();
        }
        // Single round (notification tap): a fresh one-entry group cache is sufficient.
        return Optional.of(buildSingleRoundDetail(drillRow, drillItem, match, new HashMap<>()));
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
        UUID coachId, UUID athleteId, List<String> tagCodes, int page, int limit) {
        List<AthleteDetail> assignedAthletes = coachService.findAllAthletesAssignedToCoach(coachId);
        boolean isAssigned = assignedAthletes.stream().anyMatch(a -> a.getUserId().equals(athleteId));
        if (!isAssigned) {
            return Collections.emptyList();
        }

        int offset = page * limit;
        List<String> normalizedTagCodes = (tagCodes == null || tagCodes.isEmpty()) ? null : tagCodes;
        List<AthleteDrillDetail> results = normalizedTagCodes == null
                ? athleteDrillDetailDao.getCompletedByAthleteWithFilters(athleteId, limit, offset)
                : athleteDrillDetailDao.getCompletedByAthleteWithTagFilter(
                        athleteId, normalizedTagCodes, normalizedTagCodes.size(), limit, offset);

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
