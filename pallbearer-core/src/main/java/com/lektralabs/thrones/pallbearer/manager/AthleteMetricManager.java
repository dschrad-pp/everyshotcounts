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

        int levelCompleteCount = athleteManagerUtils.completionCount(orderDetails);
        int levelCount = orderDetails.size();
        String levelCompletionPercent = levelCount == 0 ? "0"
                : decimalFormat.format(((double) levelCompleteCount / (double) levelCount) * 100.0);

        athleteUserPropertyManager.setCurrentLevelCompletionPercent(
                athleteUserId, levelCompletionPercent, currentDrillGroupId);

        logger.info("Athlete metrics manager updated drill group and level"
                + " completion metrics for athlete ID={}", athleteUserId);
    }
}
