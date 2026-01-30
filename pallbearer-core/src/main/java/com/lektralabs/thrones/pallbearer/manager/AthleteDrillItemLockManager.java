package com.lektralabs.thrones.pallbearer.manager;

import java.util.Comparator;

import com.lektralabs.thrones.pallbearer.common.DrillStatusConstants;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillDetail;
import com.lektralabs.thrones.pallbearer.manager.utils.AthleteManagerUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lektralabs.thrones.pallbearer.api.model.partial.generated.DrillPartial;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillService;

/**
 * Provides a service to manage lock status on athlete drill items
 * <p/>
 * The manager expects to work on athlete drill details in a drill group as
 * athletes work drills one group at a time
 * <p/>
 * The manager expects that drill item completion status is set. See:
 * AthleteDrillItemStatusManager
 * <p/>
 * Drill items are locked and unlocked based on payment status and progress
 */
@ApplicationScoped
public class AthleteDrillItemLockManager {

    private static Logger logger = LoggerFactory.getLogger(AthleteDrillItemLockManager.class);

    @Inject
    AthleteUserPropertyManager athleteUserPropertyManager;

    @Inject
    AthleteManagerUtils athleteManagerUtils;

    @Inject
    DrillService drillService;

    /**
     * Manages the lock status on an athlete drill details with the specified
     * drill group
     * <p>
     * Athlete drill details with completed drill items are unlocked. Athlete
     * drills that are pending or processing (should only be 1) are unlocked. If
     * the athlete does not have any pending or processing drills, The "next"
     * athlete drill detail that shows an incomplete drill item is unlocked
     *
     * @param athleteUserId Athlete user ID
     * @param drillGroupId Athlete current drill group ID
     * @param athleteDrillDetails Athlete drill details
     * @return Athlete drill details in the specified group with locks set
     */
    public List<AthleteDrillDetail> manageLockStatus(UUID athleteUserId,
            UUID drillGroupId,
            List<AthleteDrillDetail> athleteDrillDetails) {
        int currentLevelIndex = athleteUserPropertyManager.activeDrillGroupLevelIndex(athleteUserId, drillGroupId);
        return unlockNextDrillItemUsingLevelIndex(athleteUserId, drillGroupId, currentLevelIndex,
                unlockCompletedDrills(
                        unlockActiveIncompleteDrills(
                                lockAllDrillItems(
                                        athleteManagerUtils.withDrillGroup(
                                                drillGroupId, athleteDrillDetails)
                                ))));
    }

    public List<AthleteDrillDetail> manageLockStatusUsingOrderIndex(UUID athleteUserId, UUID drillGroupId, List<AthleteDrillDetail> athleteDrillDetails) {
        int currentOrderIndex = athleteUserPropertyManager.activeDrillGroupOrderIndex(athleteUserId, drillGroupId);
        return unlockNextDrillItemUsingOrderIndex(athleteUserId, drillGroupId, currentOrderIndex,
                unlockCompletedDrills(
                        unlockActiveIncompleteDrills(
                                lockAllDrillItems(
                                        athleteManagerUtils.withDrillGroup(drillGroupId, athleteDrillDetails)
                                )
                        )));
    }

    /**
     * Unlock all drill details in the provided list - bypasses progressive locking logic
     * Use this method when you want to make all drills accessible regardless of completion status
     *
     * @param athleteUserId Athlete user ID (for logging purposes)
     * @param drillGroupId Drill group ID (for logging purposes)
     * @param athleteDrillDetails Athlete drill details to unlock
     * @return Athlete drill details with all drills unlocked
     */
    public List<AthleteDrillDetail> unlockAllDrills(UUID athleteUserId, UUID drillGroupId, List<AthleteDrillDetail> athleteDrillDetails) {
        logger.info("Unlocking all drills for athlete {} in drill group {}", athleteUserId, drillGroupId);
        return unlockAllDrillItems(athleteManagerUtils.withDrillGroup(drillGroupId, athleteDrillDetails));
    }

    /**
     * Lock all drill details in the provided list
     *
     * @param athleteDrillDetails Athlete drill details in the current group
     * @return Athlete drill details
     */
    private List<AthleteDrillDetail> lockAllDrillItems(List<AthleteDrillDetail> athleteDrillDetails) {
        return athleteDrillDetails.stream().map(row -> {
            row.setIsLocked(true);
            return row;
        }).toList();
    }

    /**
     * Unlock all drill details in the provided list
     *
     * @param athleteDrillDetails Athlete drill details in the current group
     * @return Athlete drill details
     */
    private List<AthleteDrillDetail> unlockAllDrillItems(List<AthleteDrillDetail> athleteDrillDetails) {
        return athleteDrillDetails.stream().map(row -> {
            row.setIsLocked(false);
            return row;
        }).toList();
    }

    /**
     * Unlock any drill details for which the athlete has created a submission
     *
     * @param athleteDrillDetails Athlete drill details in the current group
     *
     * @return Athlete drill details
     */
    private List<AthleteDrillDetail> unlockCompletedDrills(List<AthleteDrillDetail> athleteDrillDetails) {
        return athleteDrillDetails.stream().map(row -> {
            Optional<DrillDetail> maybeDrillDetail = row.getDrillDetail();
            if (maybeDrillDetail.isPresent()) {
                DrillDetail drillDetail = maybeDrillDetail.get();
                if (drillDetail.getDrillStatus()
                        .equals(DrillStatusConstants.COMPLETE)) {
                    row.setIsLocked(false);
                }
            }
            return row;
        }).toList();
    }

    /**
     * Unlock any drill details for which the athlete has a pending or
     * processing submission or require retry
     *
     * @param athleteDrillDetails Athlete drill details in the current group
     * @return Athlete drill details
     */
    private List<AthleteDrillDetail> unlockActiveIncompleteDrills(List<AthleteDrillDetail> athleteDrillDetails) {
        return athleteDrillDetails.stream().map(row -> {
            Optional<DrillDetail> maybeDrillDetail = row.getDrillDetail();
            if (maybeDrillDetail.isPresent()) {
                DrillDetail drillDetail = maybeDrillDetail.get();
                if (drillDetail.getDrillStatus()
                        .equals(DrillStatusConstants.PENDING)
                        || drillDetail.getDrillStatus()
                                .equals(DrillStatusConstants.PROCESSING)
                        || drillDetail.getDrillStatus()
                                .equals(DrillStatusConstants.RETRY)) {
                    row.setIsLocked(false);
                }
            }
            return row;
        }).toList();
    }

    /**
     * Look for the next drill item detail that should be unlocked.
     * <p>
     * If the drill item details include drills that are pending or processing
     * there is nothing to unlock as the athlete ultimately needs to review and
     * complete a drill that is in progress
     * <p>
     * See findNextDrillItem for the business logic. If a candidate is found, it
     * is unlocked
     *
     * @param athleteUserId Athlete user ID
     * @param athleteDrillDetails Athlete drill details in the current group
     * @return Athlete drill details
     */
    private List<AthleteDrillDetail> unlockNextDrillItemUsingLevelIndex(UUID athleteUserId,
            UUID drillGroupId,
            int levelIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {
        if (athleteManagerUtils.hasActiveIncompleteDrill(drillGroupId,
                levelIndex, athleteDrillDetails)) {
            // nothing new to unlock, athlete needs to complete most recent
            // active drill before moving on to the next
            return athleteDrillDetails;
        } else {
            Optional<AthleteDrillDetail> maybeNextDrillItem = athleteManagerUtils
                    .findNextDrillItem(drillGroupId, levelIndex, athleteDrillDetails);

            return unlockNextDrillItem(athleteUserId, athleteDrillDetails,
                    maybeNextDrillItem);
        }
    }

    // private List<AthleteDrillDetail> unlockNextDrillItemUsingOrderIndex(UUID athleteUserId,
    //         UUID drillGroupId,
    //         int orderIndex,
    //         List<AthleteDrillDetail> athleteDrillDetails) {
    //     if (athleteManagerUtils.hasActiveIncompleteDrillUsingOrderIndex(drillGroupId,
    //             orderIndex, athleteDrillDetails)) {
    //         // nothing new to unlock, athlete needs to complete most recent
    //         // active drill before moving on to the next
    //         return athleteDrillDetails;
    //     } else {
    //         Optional<AthleteDrillDetail> maybeNextDrillItem = athleteManagerUtils
    //                 .findNextDrillItemUsingOrderIndex(drillGroupId, orderIndex, athleteDrillDetails);
    //         return unlockNextDrillItem(athleteUserId, athleteDrillDetails,
    //                 maybeNextDrillItem);
    //     }
    // }
    private List<AthleteDrillDetail> unlockNextDrillItemUsingOrderIndex(UUID athleteUserId,
            UUID drillGroupId,
            int orderIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {

        List<AthleteDrillDetail> details = athleteManagerUtils.withOrderIndex(drillGroupId, orderIndex, athleteDrillDetails);

        boolean isTestLevel = details.stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getLevelTest()));

        if (isTestLevel) {
            return unlockSequentialTestLevel(drillGroupId, orderIndex, athleteDrillDetails);
        }

        if (isPreviousLevelATestLevel(drillGroupId, orderIndex, athleteDrillDetails)) {
            logger.info("Previous level was test level");
            if (isTestPassed(drillGroupId, orderIndex - 1, athleteDrillDetails)) {
                logger.info("Test level passed for drillGroupId={}, marking test drills complete and unlocking next.", drillGroupId);
                if (markTestLevelDrillsComplete(athleteUserId, drillGroupId, orderIndex - 1, athleteDrillDetails)) {
                    return unlockNextDrillItem(athleteUserId, drillGroupId, orderIndex, athleteDrillDetails);
                }
            } else {
                return athleteDrillDetails;
            }
        } else {
            logger.info("Previous level was not a test level");
            logger.info("Unlocking drill for order index " + orderIndex);
            if (athleteManagerUtils.hasActiveIncompleteDrillUsingOrderIndex(drillGroupId, orderIndex, athleteDrillDetails)) {
                // nothing new to unlock, athlete needs to complete most recent active drill before moving on
                logger.info("Returning original athletes");
                return athleteDrillDetails;
            } else {
                return unlockNextDrillItem(athleteUserId, drillGroupId, orderIndex, athleteDrillDetails);
            }
        }

        return athleteDrillDetails;
    }

    private List<AthleteDrillDetail> unlockNextDrillItem(UUID athleteUserId,
            UUID drillGroupId,
            int orderIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {

        Optional<AthleteDrillDetail> maybeNextDrillItem = athleteManagerUtils
                .findNextDrillItemUsingOrderIndex(drillGroupId, orderIndex, athleteDrillDetails);
        logger.info("unlocking drill ");

        return unlockNextDrillItem(athleteUserId, athleteDrillDetails, maybeNextDrillItem);
    }

    private Boolean markTestLevelDrillsComplete(UUID athleteUserId, UUID drillGroupId, int testOrderIndex, List<AthleteDrillDetail> athleteDrillDetails) {
        List<AthleteDrillDetail> testLevelDrills = athleteManagerUtils.withOrderIndex(drillGroupId, testOrderIndex, athleteDrillDetails);

        for (AthleteDrillDetail drill : testLevelDrills) {
            if (drill.getDrillDetail().isPresent()) {
                DrillDetail drillDetail = drill.getDrillDetail().get();

                DrillPartial drillPartial = new DrillPartial();

                drillPartial.setDrillId(Optional.ofNullable(drillDetail.getId()));
                drillPartial.setDrillItemId(drill.getDrillItemId());
                drillPartial.setUserId(athleteUserId);
                drillPartial.setMediaId(drillDetail.getMediaId());
                drillPartial.setDrillStatus(DrillStatusConstants.COMPLETE);
                drillPartial.setVersion(Optional.ofNullable(drillDetail.getVersion()));
                drillPartial.setAttemptsDetected(drillDetail.getAttemptsDetected());
                drillPartial.setAttemptsReported(drillDetail.getAttemptsReported());
                drillPartial.setMakesDetected(drillDetail.getMakesDetected());
                drillPartial.setMakesReported(drillDetail.getMakesReported());

                int rv = drillService.update(drillPartial, false);
            }
        }

        return true;
    }

    private Boolean isPreviousLevelATestLevel(UUID drillGroupId, int orderIndex, List<AthleteDrillDetail> athleteDrillDetails) {
        if (orderIndex > 1) {
            AthleteDrillDetail previousDrill = athleteManagerUtils.withOrderIndex(drillGroupId, orderIndex - 1, athleteDrillDetails)
                    .stream()
                    .sorted(Comparator.comparing(AthleteDrillDetail::getDrillItemOrder))
                    .findFirst()
                    .orElse(null);

            if (previousDrill != null) {
                return Boolean.TRUE.equals(previousDrill.getLevelTest());
            }
        }
        return false;
    }

    private Boolean isTestPassed(UUID drillGroupId, int orderIndex, List<AthleteDrillDetail> athleteDrillDetails) {
        if (orderIndex > 1) {
            List<AthleteDrillDetail> testLevelDrills = athleteManagerUtils.withOrderIndex(drillGroupId, orderIndex - 1, athleteDrillDetails)
                    .stream()
                    .sorted(Comparator.comparing(AthleteDrillDetail::getDrillItemOrder))
                    .toList();

            int totalPassingScore = testLevelDrills.stream()
                    .mapToInt(d -> d.getPassingScore() != null ? d.getPassingScore() : 0)
                    .sum();

            int totalMakes = testLevelDrills.stream()
                    .mapToInt(d -> {
                        if (d.getDrillDetail().isPresent()) {
                            DrillDetail drillDetail = d.getDrillDetail().get();
                            return drillDetail.getMakesReported() != null ? drillDetail.getMakesReported() : 0;
                        }
                        return 0;
                    }).sum();

            if (totalPassingScore == 0) {
                return false;
            }
            logger.info("total passing score : " + totalPassingScore + " total makes : " + totalMakes);
            return (((totalMakes * 100.0) / totalPassingScore) >= 70.0);
        } else {
            return false;
        }
    }

    private List<AthleteDrillDetail> unlockSequentialTestLevel(UUID drillGroupId,
            int orderIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {
        List<AthleteDrillDetail> details = athleteManagerUtils.withOrderIndex(drillGroupId, orderIndex, athleteDrillDetails)
                .stream()
                .sorted(Comparator.comparing(AthleteDrillDetail::getDrillItemOrder))
                .toList();

        for (AthleteDrillDetail item : details) {
            if (!Boolean.TRUE.equals(item.getLevelTest())) {
                continue; // skip non-test items
            }

            int currentOrder = item.getDrillItemOrder();

            if (currentOrder == 1) {
                // First item always unlocked   
                item.setIsLocked(false);
            } else {
                String prevStatus = athleteManagerUtils.getPreviousDrillStatus(drillGroupId, orderIndex, currentOrder, athleteDrillDetails);
                if (prevStatus == null || prevStatus.equals(DrillStatusConstants.NOT_ATTEMPTED)) {
                    item.setIsLocked(true); // lock if previous is not attempted
                } else {
                    item.setIsLocked(false); // unlock otherwise
                }
            }
        }

        return athleteDrillDetails;
    }

    /**
     * Unlock the optional next drill item detail, if the next drill item detail
     * is present
     *
     * @param athleteUserId Athlete user ID
     * @param athleteDrillDetails Athlete drill item details in the current
     * group
     * @param maybeNextDrillItem Optional next drill item to unlock
     * @return Athlete drill details
     */
    private List<AthleteDrillDetail> unlockNextDrillItem(UUID athleteUserId,
            List<AthleteDrillDetail> athleteDrillDetails,
            Optional<AthleteDrillDetail> maybeNextDrillItem) {
        if (maybeNextDrillItem.isPresent()) {
            AthleteDrillDetail nextDrillItem = maybeNextDrillItem.get();
            logger.info("Athlete drill item lock service"
                    + " got next unlocked drill item, ID={}"
                    + " for athlete ID={}",
                    nextDrillItem.getDrillItemId(), athleteUserId);

            return athleteDrillDetails.stream().map(row -> {
                if (row.getDrillItemId().equals(nextDrillItem.getDrillItemId())) {
                    row.setIsLocked(false);
                }
                return row;
            }).toList();
        } else {
            logger.warn("Athlete drill item lock service"
                    + " did not find a next drill item to unlock"
                    + " for athlete ID={}. Either the level is complete"
                    + " or something is wrong",
                    athleteUserId);
            return athleteDrillDetails;
        }
    }

}
