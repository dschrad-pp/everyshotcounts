package com.lektralabs.thrones.pallbearer.manager;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillPartial;
import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;
import com.lektralabs.thrones.pallbearer.common.DrillStatusConstants;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillAttemptHistoryRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillAttemptHistoryService;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillService;
import com.lektralabs.thrones.pallbearer.manager.utils.AthleteManagerUtils;
import com.lektralabs.thrones.pallbearer.manager.utils.DrillItemUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillRow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AthleteDrillItemProgressManager {

    private static Logger logger = LoggerFactory.getLogger(AthleteDrillItemProgressManager.class);

    @Inject
    AthleteDrillService athleteDrillService;

    @Inject
    AthleteManagerUtils athleteManagerUtils;

    @Inject
    AthleteUserPropertyManager athleteUserPropertyManager;

    @Inject
    DrillService drillService;

    @Inject
    DrillAttemptHistoryService drillAttemptHistoryService;

    /**
     * Provides the utilities around completing a drill.
     * <p>
     * The drill is marked as complete and then the athlete might advance,
     * either to a next group (assuming the athlete is not elite), a next level
     * or possibly to the test phase of the current level
     * <p>
     * If the athlete is self-reporting makes and attempts, the values should be
     * provided by the drill partial
     *
     * @param drillId       Drill ID
     * @param athleteUserId Athlete user ID
     * @param drillPartial  Drill partial
     * @return 1 or zero if the drill service fails to update the drill row
     */
    public int completeDrill(UUID drillId, UUID athleteUserId, DrillPartial drillPartial) {
        UUID drillItemId = drillPartial.getDrillItemId();
        logger.info("⏳ Received request to complete drill. DrillItemId={}, AthleteUserId={}, DrillId={}",
                drillItemId, athleteUserId, drillId);

        // Retry mechanism to handle optimistic lock failures
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            Optional<DrillRow> drillRowOpt = drillService.findByDrillItemIdAndUserId(drillItemId, athleteUserId);

            if (drillRowOpt.isPresent()) {
                DrillRow drillRow = drillRowOpt.get();
                Optional<UUID> mediaId = drillRow.getMediaId();

                logger.info("✅ Drill found. DrillItemId={}, UserId={}, MediaId={}, Version={}, RetryCount={}",
                        drillItemId, athleteUserId, mediaId, drillRow.getVersion(), retryCount);

                // Set all required fields including version for optimistic locking
                drillPartial.setDrillStatus(getDrillStatusFromDrillPartial(drillPartial));
                drillPartial.setDrillId(Optional.of(drillRow.getId())); // Use existing drill ID
                drillPartial.setUserId(athleteUserId);
                drillPartial.setMediaId(mediaId);
                drillPartial.setVersion(Optional.ofNullable(drillRow.getVersion()));

                try {
                    int updateResult = drillService.update(drillPartial, true);
                    
                    if (updateResult > 0) {
                        logger.info("📦 Drill updated successfully. UpdateResult={}, DrillItemId={}, UserId={}",
                                updateResult, drillItemId, athleteUserId);

                        insertAttemptHistory(drillRow.getId(), athleteUserId, drillPartial,
                                drillRow.getMediaId().orElse(null), drillRow.getVersion());

                        // Account type check
                        if (athleteUserPropertyManager.isTrialAccount(athleteUserId)) {
                            logger.info("👤 Athlete is a trial account. Advancing trial athlete: {}", athleteUserId);
                            advanceTrialAthlete(athleteUserId);
                        } else {
                            logger.info("👤 Athlete is a full account. Advancing athlete: {}", athleteUserId);
                            advanceAthlete(athleteUserId);
                        }

                        return updateResult;
                    } else {
                        logger.warn("⚠️ Update returned 0 rows. DrillItemId={}, UserId={}, RetryCount={}",
                                drillItemId, athleteUserId, retryCount);
                        return 0;
                    }
                } catch (org.jdbi.v3.core.transaction.TransactionException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Optimistic lock failed")) {
                        retryCount++;
                        if (retryCount < maxRetries) {
                            logger.warn("⚠️ Optimistic lock failure. Retrying ({}/{}) for DrillItemId={}, UserId={}",
                                    retryCount, maxRetries, drillItemId, athleteUserId);
                            // Wait a bit before retrying (exponential backoff)
                            try {
                                Thread.sleep(50 * retryCount); // 50ms, 100ms, 150ms
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                logger.error("❌ Retry interrupted for DrillItemId={}, UserId={}", drillItemId, athleteUserId);
                                throw new RuntimeException("Drill completion interrupted", ie);
                            }
                            continue; // Retry the loop
                        } else {
                            logger.error("❌ Optimistic lock failed after {} retries. DrillItemId={}, UserId={}",
                                    maxRetries, drillItemId, athleteUserId, e);
                            throw e; // Re-throw after max retries
                        }
                    } else {
                        // Not an optimistic lock error, re-throw immediately
                        logger.error("❌ Unexpected error updating drill. DrillItemId={}, UserId={}",
                                drillItemId, athleteUserId, e);
                        throw e;
                    }
                }
            } else {
                // Drill doesn't exist - create it first using DrillService.create()
                logger.info("📝 Drill not found. Creating new drill for DrillItemId={}, UserId={}", 
                        drillItemId, athleteUserId);
                
                try {
                    // Build DrillPartial for creation with status from request or default to COMPLETE
                    String drillStatus = getDrillStatusFromDrillPartial(drillPartial);
                    DrillPartial createPartial = DrillPartial.builder()
                            .drillItemId(drillItemId)
                            .userId(athleteUserId)
                            .drillStatus(drillStatus)
                            .mediaId(drillPartial.getMediaId())
                            .attemptsDetected(drillPartial.getAttemptsDetected() != null ? drillPartial.getAttemptsDetected() : 0)
                            .attemptsReported(drillPartial.getAttemptsReported() != null ? drillPartial.getAttemptsReported() : 0)
                            .makesDetected(drillPartial.getMakesDetected() != null ? drillPartial.getMakesDetected() : 0)
                            .makesReported(drillPartial.getMakesReported() != null ? drillPartial.getMakesReported() : 0)
                            .version(Optional.of(0))
                            .build();
                    
                    // Use DrillService.create() which handles duplicate checking
                    UUID createdDrillId = drillService.create(createPartial);
                    logger.info("✅ Created new drill with ID={} for DrillItemId={}, UserId={}", 
                            createdDrillId, drillItemId, athleteUserId);
                    
                    // Now complete the newly created drill
                    Optional<DrillRow> newDrillRowOpt = drillService.findByDrillItemIdAndUserId(drillItemId, athleteUserId);
                    if (newDrillRowOpt.isPresent()) {
                        DrillRow newDrillRow = newDrillRowOpt.get();
                        drillPartial.setDrillStatus(drillStatus);
                        drillPartial.setDrillId(Optional.of(newDrillRow.getId()));
                        drillPartial.setUserId(athleteUserId);
                        drillPartial.setMediaId(newDrillRow.getMediaId());
                        drillPartial.setVersion(Optional.ofNullable(newDrillRow.getVersion()));
                        
                        int updateResult = drillService.update(drillPartial, true);
                        if (updateResult > 0) {
                            logger.info("📦 Newly created drill completed successfully. DrillId={}, DrillItemId={}, UserId={}",
                                    createdDrillId, drillItemId, athleteUserId);

                            insertAttemptHistory(newDrillRow.getId(), athleteUserId, drillPartial,
                                    newDrillRow.getMediaId().orElse(null), newDrillRow.getVersion());

                            // Account type check
                            if (athleteUserPropertyManager.isTrialAccount(athleteUserId)) {
                                logger.info("👤 Athlete is a trial account. Advancing trial athlete: {}", athleteUserId);
                                advanceTrialAthlete(athleteUserId);
                            } else {
                                logger.info("👤 Athlete is a full account. Advancing athlete: {}", athleteUserId);
                                advanceAthlete(athleteUserId);
                            }
                            
                            return updateResult;
                        }
                    }
                    
                    return 1; // Drill was created successfully
                } catch (Exception e) {
                    logger.error("❌ Error creating drill for DrillItemId={}, UserId={}", 
                            drillItemId, athleteUserId, e);
                    return 0;
                }
            }
        }
        
        logger.error("❌ Failed to complete drill after {} retries. DrillItemId={}, UserId={}",
                maxRetries, drillItemId, athleteUserId);
        return 0;
    }

    private void insertAttemptHistory(UUID drillId, UUID userId, DrillPartial partial,
            UUID mediaId, Integer version) {
        try {
            DrillAttemptHistoryRow row = DrillAttemptHistoryRow.builder()
                    .id(UUID.randomUUID())
                    .drillId(drillId)
                    .userId(userId)
                    .attemptsDetected(partial.getAttemptsDetected() != null ? partial.getAttemptsDetected() : 0)
                    .attemptsReported(partial.getAttemptsReported() != null ? partial.getAttemptsReported() : 0)
                    .makesDetected(partial.getMakesDetected() != null ? partial.getMakesDetected() : 0)
                    .makesReported(partial.getMakesReported() != null ? partial.getMakesReported() : 0)
                    .mediaId(mediaId)
                    .version(version)
                    .attemptLocalId(partial.getAttemptLocalId())
                    .build();
            drillAttemptHistoryService.insertHistory(row);
            logger.info("📋 Inserted attempt history row for DrillId={}, UserId={}, AttemptLocalId={}", drillId, userId, partial.getAttemptLocalId());
        } catch (Exception e) {
            logger.error("❌ Failed to insert attempt history for DrillId={}, UserId={}", drillId, userId, e);
        }
    }

    private String getDrillStatusFromDrillPartial(DrillPartial drillPartial) {

        String inputStatus = drillPartial.getDrillStatus();
        String finalStatus;

        switch (inputStatus != null ? inputStatus.toUpperCase() : "") {
            case DrillStatusConstants.COMPLETE:
                finalStatus = DrillStatusConstants.COMPLETE;
                break;
            case DrillStatusConstants.PENDING:
                finalStatus = DrillStatusConstants.PENDING;
                break;
            case DrillStatusConstants.PROCESSING:
                finalStatus = DrillStatusConstants.PROCESSING;
                break;
            case DrillStatusConstants.RETRY:
                finalStatus = DrillStatusConstants.RETRY;
                break;
            case DrillStatusConstants.NOT_ATTEMPTED:
                finalStatus = DrillStatusConstants.NOT_ATTEMPTED;
                break;
            default:
                finalStatus = DrillStatusConstants.COMPLETE; // default fallback status
                break;
        }
        return finalStatus;
    }

    private void advanceTrialAthlete(UUID athleteUserId) {
        logger.info("Athlete drill item progress manager"
                + " refusing to advance"
                + " athlete ID={} with trial account",
                athleteUserId);
    }

    private void advanceAthlete(UUID athleteUserId) {
        try {

            UUID currentDrillGroupId = athleteUserPropertyManager
                    .activeDrillGroup(athleteUserId);
            int currentOrderIndex = athleteUserPropertyManager
                    .activeDrillGroupOrderIndex(athleteUserId, currentDrillGroupId);
            int currentLevelIndex = athleteUserPropertyManager.activeDrillGroupLevelIndex(athleteUserId,
                    currentDrillGroupId);

            logger.info("currentDrilLGroup " + currentDrillGroupId + " currentOrderIndex : " + currentOrderIndex
                    + " currentLevelIndex " + currentLevelIndex);

            List<AthleteDrillDetail> athleteGroupDrillDetails = athleteDrillService
                    .findWithAthleteAndGroup(athleteUserId, currentDrillGroupId,
                            FindOptions.builder().limit(9999).offset(0).build());
            if (athleteGroupDrillDetails.isEmpty()) {
                logger.warn("No AthleteDrillDetails found for athleteId={} and drillGroupId={}", athleteUserId,
                        currentDrillGroupId);
                return; // or handle accordingly
            }

            if (athleteManagerUtils.isCompleteGroup(currentDrillGroupId, athleteGroupDrillDetails)) {
                // advance to the next group

                advanceToNextGroup(athleteUserId, currentDrillGroupId);
            } else if (athleteManagerUtils.isCompleteLevel(currentDrillGroupId, currentOrderIndex,
                    athleteGroupDrillDetails)) {
                // advance to the next level
                logger.info("group order index " + currentOrderIndex + " completed advancing to "
                        + (currentOrderIndex + 1));
                advanceToNextGroupOrder(athleteUserId, currentDrillGroupId, currentOrderIndex, currentLevelIndex,
                        athleteGroupDrillDetails);
            } else {
                logger.info("not completed..........................................................................");
                // check to see if athlete has advanced to test level
                if (advanceToTestLevel(athleteUserId, currentDrillGroupId, currentOrderIndex,
                        athleteGroupDrillDetails)) {
                    logger.info("Athlete drill item progress manager"
                            + " advanced drill level {} to TEST level for"
                            + " athlete ID={}",
                            currentOrderIndex, athleteUserId);
                } else {
                    logger.info("Athlete drill item progress manager"
                            + " did not find an opportunity to advance"
                            + " athlete ID={} to a next group, level or test level",
                            athleteUserId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private UUID advanceToNextGroup(UUID athleteUserId,
            UUID currentDrillGroupId) {
        if (currentDrillGroupId.equals(DrillGroupConstants.ELITE_GROUP_ID)) {
            logger.info("Athlete drill item progress manager"
                    + " attempting to advance drill group for"
                    + " athlete ID={} at Elite level. No where else to go!",
                    athleteUserId);
            return currentDrillGroupId;
        } else {

            if (currentDrillGroupId.equals(DrillGroupConstants.BEGINNER_GROUP_ID)) {
                String nextGroupName = DrillGroupConstants.drillGroupIdNameMap
                        .get(DrillGroupConstants.INTERMEDIATE_GROUP_ID);
                athleteUserPropertyManager.setActiveDrillGroup(athleteUserId,
                        DrillGroupConstants.INTERMEDIATE_GROUP_ID);
                athleteUserPropertyManager.setActiveDrillGroupName(athleteUserId, nextGroupName);
                logger.info("Athlete drill item progress manager"
                        + " advanced drill group for"
                        + " athlete ID={} to Intermediate",
                        athleteUserId);
                return DrillGroupConstants.INTERMEDIATE_GROUP_ID;
            }
            if (currentDrillGroupId.equals(DrillGroupConstants.INTERMEDIATE_GROUP_ID)) {
                String nextGroupName = DrillGroupConstants.drillGroupIdNameMap
                        .get(DrillGroupConstants.ADVANCE_GROUP_ID);

                athleteUserPropertyManager.setActiveDrillGroup(athleteUserId,
                        DrillGroupConstants.ADVANCE_GROUP_ID);
                athleteUserPropertyManager.setActiveDrillGroupName(athleteUserId, nextGroupName);

                logger.info("Athlete drill item progress manager"
                        + " advanced drill group for"
                        + " athlete ID={} to Advance",
                        athleteUserId);
                return DrillGroupConstants.ADVANCE_GROUP_ID;
            }
            if (currentDrillGroupId.equals(DrillGroupConstants.ADVANCE_GROUP_ID)) {
                String nextGroupName = DrillGroupConstants.drillGroupIdNameMap.get(DrillGroupConstants.ELITE_GROUP_ID);

                athleteUserPropertyManager.setActiveDrillGroup(athleteUserId,
                        DrillGroupConstants.ELITE_GROUP_ID);
                athleteUserPropertyManager.setActiveDrillGroupName(athleteUserId, nextGroupName);

                logger.info("Athlete drill item progress manager"
                        + " advanced drill group for"
                        + " athlete ID={} to Elite",
                        athleteUserId);
                return DrillGroupConstants.ELITE_GROUP_ID;
            }
        }
        // something is wrong...
        return currentDrillGroupId;
    }

    private int advanceToNextGroupOrder(UUID athleteUserId, UUID currentDrillGroupId,
            int currentOrderIndex, int levelIndex, List<AthleteDrillDetail> athleteDrillDetails) {
        int nextOrderIndex = currentOrderIndex + 1;

        Optional<AthleteDrillDetail> maybeNextItem = athleteManagerUtils
                .findNextDrillItemUsingOrderIndex(currentDrillGroupId, nextOrderIndex, athleteDrillDetails);

        boolean isLevelTest = false;
        if (maybeNextItem.isPresent()) {
            Boolean levelTestFlag = maybeNextItem.get().getLevelTest();
            isLevelTest = (levelTestFlag != null && levelTestFlag);
        }

        int nextLevelIndex = levelIndex;
        logger.info("is test level : " + isLevelTest + " is yes then updating the level index to " + (levelIndex + 1)
                + " from " + levelIndex);
        if (!isLevelTest) {
            nextLevelIndex = nextLevelIndex + 1;
        }
        String nextOrderIdentifier = DrillItemUtils.orderIndexIdentifier(nextOrderIndex, isLevelTest);
        athleteUserPropertyManager.setActiveDrillGroupOrderIndex(athleteUserId, nextOrderIdentifier,
                currentDrillGroupId);
        String levelIndexIdentifier = DrillItemUtils.levelIdentifier(nextLevelIndex, isLevelTest);
        athleteUserPropertyManager.setActiveDrillGroupLevel(athleteUserId, levelIndexIdentifier, currentDrillGroupId);
        logger.info("Athlete drill item progress manager"
                + " advanced drill level to {} for"
                + " athlete ID={}",
                nextOrderIndex, athleteUserId);
        return nextOrderIndex;
    }

    private boolean advanceToTestLevel(UUID athleteUserId,
            UUID currentDrillGroupId,
            int currentOrderIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {
        Optional<AthleteDrillDetail> maybeNextItem = athleteManagerUtils
                .findNextDrillItemUsingOrderIndex(currentDrillGroupId, currentOrderIndex, athleteDrillDetails);
        if (maybeNextItem.isPresent()) {
            AthleteDrillDetail nextAthleteDrillDetail = maybeNextItem.get();
            Boolean isLevelTest = nextAthleteDrillDetail.getLevelTest();
            if (isLevelTest != null && isLevelTest) {
                String orderIdentifier = DrillItemUtils.orderIndexIdentifier(currentOrderIndex, isLevelTest);
                athleteUserPropertyManager.setActiveDrillGroupOrderIndex(athleteUserId, orderIdentifier,
                        currentDrillGroupId);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
