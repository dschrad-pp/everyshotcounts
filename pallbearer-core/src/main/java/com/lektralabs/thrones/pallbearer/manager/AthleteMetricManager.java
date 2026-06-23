package com.lektralabs.thrones.pallbearer.manager;

import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import com.lektralabs.thrones.pallbearer.manager.utils.AthleteManagerUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AthleteMetricManager {

    private static Logger logger = LoggerFactory.getLogger(AthleteMetricManager.class);

    @Inject
    AthleteDrillService athleteDrillService;

    @Inject
    AthleteManagerUtils athleteManagerUtils;

    @Inject
    AthleteUserPropertyManager athleteUserPropertyManager;

    /**
     * Updates the athlete's drill completion metrics for the current drill
     * group and current drill group level
     *
     * @param athleteUserId Athlete user ID
     */
    public void updateDrillCompletionMetrics(UUID athleteUserId) {
        UUID currentDrillGroupId = athleteUserPropertyManager
                .activeDrillGroup(athleteUserId);
        int currentOrderIndex = athleteUserPropertyManager
                .activeDrillGroupOrderIndex(athleteUserId, currentDrillGroupId);
        List<AthleteDrillDetail> athleteGroupDrillDetails = athleteDrillService
                .findWithAthleteAndGroup(athleteUserId, currentDrillGroupId,
                        FindOptions.builder().limit(9999).offset(0).build());

        updateDrillCompletionMetrics(athleteUserId,
                currentDrillGroupId, currentOrderIndex,
                athleteGroupDrillDetails);
    }

    /**
     * Computes the athlete's current-level completion percent LIVE (0–100) for
     * the active drill group/level — the authoritative source of truth for the
     * coach donut. Read paths should call this instead of reading the cached
     * {@code user.metric.drill.level.completion.percent} property, which goes
     * stale per tier once the athlete advances.
     * <p>
     * Completion = drills PASSED (makes >= passing score) ÷ total drills in the
     * current level. See {@link #levelCompletionPercent(int, int)} for rounding.
     *
     * @param athleteUserId Athlete user ID
     * @return Current-level completion percent in [0, 100]
     */
    public int computeCurrentLevelCompletionPercent(UUID athleteUserId) {
        UUID currentDrillGroupId = athleteUserPropertyManager
                .activeDrillGroup(athleteUserId);
        int currentOrderIndex = athleteUserPropertyManager
                .activeDrillGroupOrderIndex(athleteUserId, currentDrillGroupId);
        List<AthleteDrillDetail> athleteGroupDrillDetails = athleteDrillService
                .findWithAthleteAndGroup(athleteUserId, currentDrillGroupId,
                        FindOptions.builder().limit(9999).offset(0).build());
        List<AthleteDrillDetail> levelDetails = athleteManagerUtils
                .withOrderIndex(currentDrillGroupId, currentOrderIndex,
                        athleteGroupDrillDetails);
        return levelCompletionPercent(
                athleteManagerUtils.passedCount(levelDetails), levelDetails.size());
    }

    /**
     * Level completion percent from a passed/total ratio, FLOOR-rounded.
     * <p>
     * Floor (not ceiling or round-half-up) guarantees the donut reads 100% only
     * when every drill in the level is passed: ceiling/round-half-up could show
     * 100% with one drill still unpassed (e.g. 199/200 = 99.5 → 100), which is
     * the exact "soft lie" this fix removes.
     */
    static int levelCompletionPercent(int passed, int total) {
        if (total == 0) {
            return 0;
        }
        int pct = (int) Math.floor((double) passed / (double) total * 100.0);
        return Math.min(100, Math.max(0, pct));
    }

    /**
     * Updates the athlete's drill completion metrics for the current drill
     * group and current drill group level
     *
     * @param athleteUserId Athlete user ID
     * @param currentDrillGroupId Current drill group ID
     * @param currentLevelIndex Current drill group level index
     * @param athleteGroupDrillDetails Athlete drill group details
     */
    public void updateDrillCompletionMetrics(UUID athleteUserId,
            UUID currentDrillGroupId,
            int currentOrderIndex,
            List<AthleteDrillDetail> athleteGroupDrillDetails) {

        logger.info("Recieved drill completion request");
        DecimalFormat decimalFormat = new DecimalFormat("#");
        decimalFormat.setRoundingMode(RoundingMode.CEILING);

        List<AthleteDrillDetail> groupDetails = athleteManagerUtils
                .withDrillGroup(currentDrillGroupId, athleteGroupDrillDetails);

        List<AthleteDrillDetail> orderDetails = athleteManagerUtils
                .withOrderIndex(currentDrillGroupId, currentOrderIndex,
                        athleteGroupDrillDetails);

        int groupCompleteCount = athleteManagerUtils.completionCount(groupDetails);
        int groupCount = groupDetails.size();
        String groupCompletionPercent = groupCount == 0 ? "0"
                : decimalFormat.format(((double) groupCompleteCount / (double) groupCount) * 100.0);

        athleteUserPropertyManager.setCurrentGroupCompletionPercent(
                athleteUserId, groupCompletionPercent, currentDrillGroupId);

        // Level completion is PASSED ÷ total (not submitted ÷ total) and
        // FLOOR-rounded — see levelCompletionPercent. This keeps the cached
        // property consistent with the live read paths and the advance gate.
        int levelPassedCount = athleteManagerUtils.passedCount(orderDetails);
        int levelCount = orderDetails.size();
        String levelCompletionPercent =
                String.valueOf(levelCompletionPercent(levelPassedCount, levelCount));

        athleteUserPropertyManager.setCurrentLevelCompletionPercent(
                athleteUserId, levelCompletionPercent, currentDrillGroupId);

        logger.info("Athlete metrics manager updated drill group and level"
                + " completion metrics for athlete ID={}", athleteUserId);
    }
}
