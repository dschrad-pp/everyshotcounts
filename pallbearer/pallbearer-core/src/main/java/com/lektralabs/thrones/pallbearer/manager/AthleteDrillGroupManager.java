package com.lektralabs.thrones.pallbearer.manager;

import com.lektralabs.thrones.pallbearer.api.util.FindOptions;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.service.AthleteDrillService;
import com.lektralabs.thrones.pallbearer.manager.utils.AthleteManagerUtils;
import com.lektralabs.thrones.pallbearer.manager.utils.DrillItemUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AthleteDrillGroupManager {

    private static Logger logger = LoggerFactory.getLogger(AthleteDrillGroupManager.class);

    @Inject
    AthleteDrillService athleteDrillService;

    @Inject
    AthleteManagerUtils athleteManagerUtils;

    @Inject
    AthleteUserPropertyManager athleteUserPropertyManager;

    @Inject
    AthleteMetricManager athleteMetricManager;

    public UUID changeDrillGroup(UUID athleteUserId,
            UUID drillGroupId) {
        List<AthleteDrillDetail> groupDrillDetails = athleteDrillService
                .findWithAthleteAndGroup(athleteUserId, drillGroupId,
                        FindOptions.builder().limit(9999).offset(0).build());

        String levelIdentifier = findCurrentLevelIdentifier(drillGroupId,
                groupDrillDetails);

        int currentLevelIndex = DrillItemUtils.levelIdentifierToInt(levelIdentifier);

        athleteUserPropertyManager.setActiveDrillGroup(athleteUserId,
                drillGroupId);

        athleteUserPropertyManager.setActiveDrillGroupLevel(athleteUserId,
                levelIdentifier, drillGroupId);

        athleteMetricManager.updateDrillCompletionMetrics(athleteUserId,
                drillGroupId, currentLevelIndex, groupDrillDetails);

        return drillGroupId;
    }

    private String findCurrentLevelIdentifier(UUID drillGroupId,
            List<AthleteDrillDetail> athleteDrillDetails) {

        logger.info("Athlete drill group manager, finding current level"
                + " identifier for new drill group {}", drillGroupId);

        List<AthleteDrillDetail> groupDetails = athleteManagerUtils.withDrillGroup(
                drillGroupId, athleteDrillDetails);

        logger.info("Athlete drill group manager found {}"
                + " drill item detail(s) for group {}", groupDetails.size(),
                drillGroupId);

        List<Integer> levelIndexes = athleteManagerUtils.levelIndexes(
                drillGroupId, groupDetails);

        Optional<Integer> maybeCurrentLevelIndex = levelIndexes.stream()
                .filter(levelIndex -> !athleteManagerUtils.isCompleteLevel(drillGroupId, levelIndex, groupDetails))
                .findFirst();

        if (maybeCurrentLevelIndex.isEmpty()) {
            // all levels complete, return last level
            Integer maxLevelIndex = levelIndexes.get(levelIndexes.size() - 1);
            AthleteDrillDetail lastDetail = athleteManagerUtils
                    .findLastDrillItem(drillGroupId, maxLevelIndex,
                            athleteDrillDetails);
            return DrillItemUtils.levelIdentifier(lastDetail);
        } else {
            Integer currentLevelIndex = maybeCurrentLevelIndex.get();

            Optional<AthleteDrillDetail> maybeNextDetail = athleteManagerUtils
                    .findNextDrillItem(drillGroupId, currentLevelIndex,
                            groupDetails);

            if (maybeNextDetail.isPresent()) {
                AthleteDrillDetail nextDetail = maybeNextDetail.get();
                return DrillItemUtils.levelIdentifier(nextDetail);
            } else {
                // this should not happen as we know the level is not complete
                // and a drill item is available... but just in case...
                boolean isTest = false;
                return DrillItemUtils.levelIdentifier(currentLevelIndex, isTest);
            }
        }
    }
}
