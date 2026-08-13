package com.lektralabs.thrones.pallbearer.manager.utils;

import com.lektralabs.thrones.pallbearer.common.DrillStatusConstants;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillDetail;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jboss.logging.Logger;

import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;
import com.lektralabs.thrones.pallbearer.jdbi.service.DrillItemDetailService;

/**
 * Provides utility functions for managing athlete drill items and drill
 * progress
 */
@ApplicationScoped
public class AthleteManagerUtils {

    private Logger logger = Logger.getLogger(AthleteManagerUtils.class);

    /**
     * Find all athlete drill details with drill items in the specified group.
     * Note this will find drill items across all group levels
     *
     * @param drillGroupId Drill group ID
     * @param athleteDrillDetails Athlete drill item details
     * @return Athlete drill details with the specified group ID
     */
    public List<AthleteDrillDetail> withDrillGroup(UUID drillGroupId,
            List<AthleteDrillDetail> athleteDrillDetails) {
        return athleteDrillDetails.stream()
                .filter(row -> row.getDrillGroup().getId().equals(drillGroupId))
                .toList();
    }

    /**
     * Find all athlete drill details with drill items that match the specified
     * level index
     * <p>
     * </p>
     * Note that in most use cases it makes sense to first filter athlete drill
     * details on a group so this search is constrained to a drill group See
     * also: withDrillGroup
     *
     * @param levelIndex Drill group level index
     * @param athleteDrillDetails Athlete drill details
     * @return Athlete drill details with the specified level index
     */
    public List<AthleteDrillDetail> withLevelIndex(int levelIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {
        return athleteDrillDetails.stream()
                .filter(row -> row.getLevelIndex() == levelIndex).toList();
    }

    public List<AthleteDrillDetail> withOrderIndex(int orderIndex, List<AthleteDrillDetail> athleteDrillDetails) {
        return athleteDrillDetails.stream()
                .filter(row -> row.getOrderIndex() == orderIndex).toList();
    }

    /**
     * Find all athlete drill details in the specified group with the specified
     * level index
     *
     * @param drillGroupId Drill group ID
     * @param levelIndex Level index
     * @param athleteDrillDetails Athlete drill details
     * @return Drill details in the specified group with the level index
     */
    public List<AthleteDrillDetail> withLevelIndex(UUID drillGroupId,
            int levelIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {
        return withLevelIndex(levelIndex,
                withDrillGroup(drillGroupId, athleteDrillDetails));
    }

    public List<AthleteDrillDetail> withOrderIndex(UUID drillGroupId, int orderIndex, List<AthleteDrillDetail> athleteDrillDetails) {
        return withOrderIndex(orderIndex, withDrillGroup(drillGroupId, athleteDrillDetails));
    }

    /**
     * Find the list of distinct, sorted, level indexes for the levels in the
     * specified drill group, represented in the provided list of drill details.
     * <p>
     * The assumption is that the athlete drill detail list is a comprehensive
     * representation of the drill group. If for some reason the caller is not
     * providing a comprehensive view of the drill group, then the assumption is
     * that the caller knows what they are doing
     *
     * @param drillGroupID Drill group ID
     * @param athleteDrillDetails Athlete drill details
     * @return List of distinct sorted drill group level indexes
     */
    public List<Integer> levelIndexes(UUID drillGroupID,
            List<AthleteDrillDetail> athleteDrillDetails) {
        return withDrillGroup(drillGroupID, athleteDrillDetails).stream()
                .map(AthleteDrillDetail::getLevelIndex)
                .distinct().sorted().toList();
    }

    /**
     * Return true if the athlete drill detail contains a drill that shows
     * evidence of completion
     *
     * @param athleteDrillDetail Athlete drill detail
     * @return True if the drill detail is complete
     */
    public boolean isComplete(AthleteDrillDetail athleteDrillDetail) {
        Optional<DrillDetail> maybeDrillDetail = athleteDrillDetail.getDrillDetail();
        if (maybeDrillDetail.isPresent()) {
            DrillDetail drillDetail = maybeDrillDetail.get();
            // look for drill status that is complete
            String drillStatus = drillDetail.getDrillStatus();
            if (!Boolean.TRUE.equals(athleteDrillDetail.getLevelTest())) {
                return drillStatus != null
                        && drillStatus.equals(DrillStatusConstants.COMPLETE);
            } else {
                return drillStatus != null && !drillStatus.equals(DrillStatusConstants.NOT_ATTEMPTED);
            }
        } else {
            // no drill associated with drill item
            return false;
        }
    }

    /**
     * Return the number of drill details that show evidence of completion
     *
     * @param athleteDrillDetails Athlete drill details
     * @return Number of completed drills
     */
    public int completionCount(List<AthleteDrillDetail> athleteDrillDetails) {
        List<AthleteDrillDetail> completed = athleteDrillDetails
                .stream().filter(this::isComplete).toList();
        return completed.size();
    }

    /**
     * Return true if the athlete drill detail shows a PASSING submission — the
     * drill is complete AND the athlete's reported makes meet or exceed the
     * drill's passing score (makes-to-advance). A submitted-but-failed drill
     * (e.g. 12 makes when 16 are required) is NOT passed.
     * <p>
     * This is intentionally stricter than {@link #isComplete} (which only means
     * "submitted"). Level completion %% and level/group advancement are based on
     * passing so that the donut and the athlete's level always agree; per-drill
     * navigation/unlock continues to use {@link #isComplete}.
     * <p>
     * Level-test drills retain their original semantics: they advance on any
     * attempt (a status other than NOT-ATTEMPTED), not on a passing score.
     * A drill item with no passing score configured is treated as passed once
     * complete.
     *
     * @param athleteDrillDetail Athlete drill detail
     * @return True if the drill was passed
     */
    public boolean isPassed(AthleteDrillDetail athleteDrillDetail) {
        Optional<DrillDetail> maybeDrillDetail = athleteDrillDetail.getDrillDetail();
        return isPassed(
                athleteDrillDetail.getLevelTest(),
                maybeDrillDetail.map(DrillDetail::getDrillStatus).orElse(null),
                athleteDrillDetail.getPassingScore(),
                maybeDrillDetail.map(DrillDetail::getMakesReported).orElse(null),
                maybeDrillDetail.map(DrillDetail::getMakesDetected).orElse(null),
                maybeDrillDetail.isPresent());
    }

    /**
     * The pass rule itself, over raw field values rather than a loaded
     * {@link AthleteDrillDetail} graph.
     * <p>
     * Extracted so the cheap level-completion query
     * ({@code LevelCompletionInputRow}) and the full detail objects grade a
     * drill through the SAME code. Duplicating this rule is how the coach donut
     * and the athlete's level silently disagree, so there is deliberately only
     * one copy — {@link #isPassed(AthleteDrillDetail)} delegates here.
     *
     * @param levelTest     True for graded "Test" drills
     * @param drillStatus   Drill status, null when never attempted
     * @param passingScore  Makes-to-advance threshold, null means none configured
     * @param makesReported Athlete-corrected score, may be null
     * @param makesDetected AI-detected score, used when reported is null
     * @param drillPresent  False when no drill row exists for the item
     * @return True if the drill was passed
     */
    public boolean isPassed(Boolean levelTest,
            String drillStatus,
            Integer passingScore,
            Integer makesReported,
            Integer makesDetected,
            boolean drillPresent) {
        if (!drillPresent) {
            // no drill associated with drill item
            return false;
        }

        if (Boolean.TRUE.equals(levelTest)) {
            return drillStatus != null
                    && !drillStatus.equals(DrillStatusConstants.NOT_ATTEMPTED);
        }

        // A regular drill must first be complete (submitted + reviewed)...
        if (drillStatus == null || !drillStatus.equals(DrillStatusConstants.COMPLETE)) {
            return false;
        }

        if (passingScore == null) {
            // No makes-to-advance threshold configured — completion is passing.
            return true;
        }

        // A null reported value means the athlete did not override the
        // AI-detected score, so it falls back to the detected value (mirrors the
        // coach-notification contract).
        Integer makes = makesReported != null ? makesReported : makesDetected;
        return makes != null && makes >= passingScore;
    }

    /**
     * Return the number of drill details that show a PASSING submission.
     *
     * @param athleteDrillDetails Athlete drill details
     * @return Number of passed drills
     * @see #isPassed(AthleteDrillDetail)
     */
    public int passedCount(List<AthleteDrillDetail> athleteDrillDetails) {
        return (int) athleteDrillDetails.stream().filter(this::isPassed).count();
    }

    /**
     * Return true if the provided athlete drill details include the specified
     * drill group and the group is complete
     *
     * @param drillGroupId Drill group ID
     * @param athleteDrillDetails Athlete drill details
     * @return True if the athlete drill details contain a complete group
     */
    public boolean isCompleteGroup(UUID drillGroupId,
            List<AthleteDrillDetail> athleteDrillDetails) {
        // look for an incomplete drill
        List<AthleteDrillDetail> details = withDrillGroup(drillGroupId,
                athleteDrillDetails);

        if (details.isEmpty()) {
            // not complete if no group match
            return false;
        } else {
            // Advancing the whole group requires every drill PASSED, not merely
            // submitted — keeps tier advancement consistent with level %.
            List<AthleteDrillDetail> completed = details
                    .stream().filter(this::isPassed).toList();
            return completed.size() == details.size();
        }
    }

    /**
     * Return true if all the athlete drill details in the provided list with
     * the specified group and level are completed
     *
     * @param drillGroupId Drill group ID
     * @param orderIndex Order index
     * @param athleteDrillDetails Athlete drill details
     * @return True if group level is complete
     */
    public boolean isCompleteLevel(UUID drillGroupId,
            int orderIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {
        List<AthleteDrillDetail> details = withOrderIndex(drillGroupId,
                orderIndex, athleteDrillDetails);

        if (details.isEmpty()) {
            // not complete if no group and level match
            logger.info("no group or level match");
            return false;
        } else {
            // Advancing to the next level requires every drill in this level
            // PASSED (makes >= passing score), not merely submitted. This is the
            // same predicate that feeds the level-completion %% donut, so the
            // athlete's level and their donut can never contradict each other.
            List<AthleteDrillDetail> completed = details
                    .stream().filter(this::isPassed).toList();

            for (AthleteDrillDetail drill : details) {
                Optional<DrillDetail> maybeDrillDetail = drill.getDrillDetail();
                if (maybeDrillDetail.isPresent()) {
                    DrillDetail drillDetail = maybeDrillDetail.get();
                    // look for drill status that is complete
                    String drillStatus = drillDetail.getDrillStatus();
                    logger.info("drill status : " + drillStatus + " drill id : " + drillDetail.getId() + " with userId : " + drillDetail.getUserId() + " drill group : " + drill.getDrillGroup().getId());
                    if (!Boolean.TRUE.equals(drill.getLevelTest())) {
                        // return drillStatus != null
                        //         && drillStatus.equals(DrillStatusConstants.COMPLETE);
                    } else {
                        // return drillStatus != null && !drillStatus.equals(DrillStatusConstants.NOT_ATTEMPTED);
                    }
                } else {
                    // no drill associated with drill item
                    // return false;
                }
            }
            logger.info("completed drills : " + details.size() + " athlete drill detail size " + athleteDrillDetails.size() + " order index of first drill " + athleteDrillDetails.get(0).getOrderIndex());
            boolean result = completed.size() == details.size();
            logger.info("the level is completed : " + result + " with order index : " + orderIndex);
            return result;
        }
    }

    /**
     * Return true if the list of athlete drill details contains any drills in
     * the specified drill group and level that the athlete has started and show
     * a status other than complete
     *
     * @param drillGroupId Drill group ID
     * @param levelIndex levelIndex
     * @param athleteDrillDetails Athlete drill detail list
     * @return True if drill details contain an incomplete drill
     */
    public boolean hasActiveIncompleteDrill(UUID drillGroupId,
            int levelIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {
        List<AthleteDrillDetail> details = withLevelIndex(drillGroupId,
                levelIndex, athleteDrillDetails);

        return details.stream().anyMatch(row -> {
            Optional<DrillDetail> maybeDrillDetail = row.getDrillDetail();
            if (maybeDrillDetail.isPresent()) {
                DrillDetail drillDetail = maybeDrillDetail.get();
                return drillDetail.getDrillStatus() != null && (drillDetail.getDrillStatus().equals(DrillStatusConstants.PENDING)
                        || drillDetail.getDrillStatus().equals(DrillStatusConstants.PROCESSING)
                        || drillDetail.getDrillStatus().equals(DrillStatusConstants.RETRY));
            } else {
                return false;
            }
        });
    }

    public boolean hasActiveIncompleteDrillUsingOrderIndex(UUID drillGroupId, int orderIndex, List<AthleteDrillDetail> athleteDrillDetails) {
        List<AthleteDrillDetail> details = withOrderIndex(drillGroupId, orderIndex, athleteDrillDetails);
        return details.stream().anyMatch(row -> {
            Optional<DrillDetail> maybeDrillDetail = row.getDrillDetail();
            if (maybeDrillDetail.isPresent()) {
                DrillDetail drillDetail = maybeDrillDetail.get();
                logger.info("the drill status is : " + drillDetail.getDrillStatus() + " for the drill : " + drillDetail.getId());
                boolean result = drillDetail.getDrillStatus() != null && (drillDetail.getDrillStatus().equals(DrillStatusConstants.PENDING)
                        || drillDetail.getDrillStatus().equals(DrillStatusConstants.PROCESSING)
                        || drillDetail.getDrillStatus().equals(DrillStatusConstants.RETRY));
                return result;
            } else {
                return false;
            }
        });
    }

    public String getPreviousDrillStatus(UUID drillGroupId, int orderIndex, int currentDrillItemOrder, List<AthleteDrillDetail> athleteDrillDetails) {
        if (currentDrillItemOrder <= 1) {
            // No previous drill for the first drill item
            return null;
        }
        return athleteDrillDetails.stream()
                .filter(d -> d.getOrderIndex() == orderIndex
                && d.getDrillItemOrder() == currentDrillItemOrder - 1)
                .findFirst()
                .flatMap(d -> d.getDrillDetail()) // unwrap Optional<DrillDetail>
                .map(DrillDetail::getDrillStatus) // then map to drillStatus
                .orElse(null);

    }

    /**
     * Find and return the ID of the drill item in the athlete's current drill
     * group and level that should be the next available drill item. This is the
     * ID of the next drill item, sorted by item order, for which the athlete
     * has not created and completed a submission
     * <p>
     * Drill item order starts at 1
     * <p>
     * It is possible that no next drill item is available. For example, the
     * current level can be complete
     *
     * @param drillGroupId Drill group ID
     * @param levelIndex Drill group level index
     * @param athleteDrillDetails List of drill item details
     * @return Some athlete drill detail or None
     */
    public Optional<AthleteDrillDetail> findNextDrillItem(UUID drillGroupId,
            int levelIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {
        List<AthleteDrillDetail> details = withLevelIndex(drillGroupId,
                levelIndex, athleteDrillDetails);

        int nextItemIndex = findNextDrillItemIndex(details);

        return details.stream()
                .filter(row -> row.getDrillItemOrder().equals(nextItemIndex))
                .findFirst();
    }

    public Optional<AthleteDrillDetail> findNextDrillItemUsingOrderIndex(UUID drillGroupId, int orderIndex, List<AthleteDrillDetail> athleteDrillDetails) {
        List<AthleteDrillDetail> details = withOrderIndex(drillGroupId, orderIndex, athleteDrillDetails);
        int nextItemIndex = findNextDrillItemIndex(details);
        logger.info("Finding next drill item");
        return details.stream()
                .filter(row -> row.getDrillItemOrder().equals(nextItemIndex))
                .findFirst();
    }

    /**
     * Find the index of the "next" drill item that is available to the athlete
     * in the provided list of drill details
     * <p>
     * This is the drill item with a drill item order that appears after the
     * last drill item that shows evidence of a complete athlete drill
     * submission
     * <p>
     * Note that in pretty much all scenarios this method is a helper for
     * AthleteManagerUtils.findNextDrillItem
     *
     * @param athleteDrillDetails Athlete drill item details
     * @return The next drill item index or 1 if something goes wrong
     */
    public int findNextDrillItemIndex(List<AthleteDrillDetail> athleteDrillDetails) {

        List<AthleteDrillDetail> withCompletedItems = athleteDrillDetails.stream()
                .filter(detail -> isComplete(detail)).toList();

        Optional<AthleteDrillDetail> maybeWithMaxItemOrder = withCompletedItems
                .stream().max(Comparator.comparing(AthleteDrillDetail::getDrillItemOrder));

        if (maybeWithMaxItemOrder.isPresent()) {
            AthleteDrillDetail withMaxItemOrder = maybeWithMaxItemOrder.get();
            return withMaxItemOrder.getDrillItemOrder() + 1;
        } else {
            return 1;
        }
    }

    /**
     * Provides a helper to find the last athlete drill detail of the specified
     * group and level in the provided drill details. This is athlete drill
     * detail that has the highest drill item order
     *
     * @param drillGroupId Drill group ID
     * @param levelIndex Drill group level index
     * @param athleteDrillDetails Athlete drill details
     * @return Athlete drill detail with highest drill item order
     */
    public AthleteDrillDetail findLastDrillItem(UUID drillGroupId,
            Integer levelIndex,
            List<AthleteDrillDetail> athleteDrillDetails) {
        List<AthleteDrillDetail> levelDetails = withLevelIndex(
                drillGroupId, levelIndex, athleteDrillDetails).stream()
                .sorted(Comparator.comparing(AthleteDrillDetail::getDrillItemOrder))
                .toList();
        return levelDetails.get(levelDetails.size() - 1);
    }
}
