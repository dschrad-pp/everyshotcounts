package com.lektralabs.thrones.pallbearer.manager;

import com.lektralabs.thrones.pallbearer.api.model.partial.UserPropertyPartial;
import com.lektralabs.thrones.pallbearer.common.DrillGroupConstants;
import com.lektralabs.thrones.pallbearer.common.UserPropertyConstants;
import com.lektralabs.thrones.pallbearer.datetime.NumberUtils;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserPropertyRow;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserPropertyService;
import com.lektralabs.thrones.pallbearer.manager.utils.DrillItemUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.lektralabs.thrones.pallbearer.jdbi.service.UserGroupPropertyService;
import com.lektralabs.thrones.pallbearer.api.model.partial.UserGroupPropertyPartial;

import java.util.Optional;
import java.util.UUID;

import org.jboss.logging.Logger;

import com.lektralabs.thrones.pallbearer.jdbi.dao.UserGroupPropertyDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserGroupPropertyRow;

/**
 * Provides utilities for working with user properties in the context of an
 * athlete type user
 */
@ApplicationScoped
public class AthleteUserPropertyManager {

    private static Logger logger = Logger.getLogger(AthleteUserPropertyManager.class);

    @Inject
    UserPropertyService userPropertyService;

    @Inject
    UserGroupPropertyService userGroupPropertyService;

    /**
     * Return the current active drill group ID for the athlete identified by
     * the athlete user ID
     * <p>
     * If the athlete does not have an active drill group, return the ID= for
     * Beginner
     *
     * @param athleteUserId Athlete user ID
     * @return Drill group ID
     */
    public UUID activeDrillGroup(UUID athleteUserId) {
        String propKey = UserPropertyConstants.USER_DRILL_GROUP_KEY;
        Optional<UserPropertyRow> maybeRow = userPropertyService
                .findByKey(athleteUserId, propKey);
        if (maybeRow.isEmpty()) {
            return DrillGroupConstants.BEGINNER_GROUP_ID;
        } else {
            return UUID.fromString(maybeRow.get().getPropertyValue());
        }
    }

    /**
     * Set the active drill group property value for the specified athlete user
     * ID
     *
     * @param athleteUserId Athlete user ID
     * @param drillGroupId Active drill group ID
     * @return 1 if success; 0 if error
     */
    public int setActiveDrillGroup(UUID athleteUserId,
            UUID drillGroupId) {
        String propKey = UserPropertyConstants.USER_DRILL_GROUP_KEY;
        Optional<UserPropertyRow> maybeRow = userPropertyService
                .findByKey(athleteUserId, propKey);
        if (maybeRow.isPresent()) {
            return userPropertyService.update(
                    UserPropertyPartial.builder()
                            .userPropertyId(Optional.of(maybeRow.get().getId()))
                            .userId(athleteUserId)
                            .propertyKey(propKey)
                            .propertyValue(drillGroupId.toString())
                            .build());
        } else {
            UUID userPropertyId = userPropertyService.create(
                    UserPropertyPartial.builder()
                            .userPropertyId(Optional.of(UUID.randomUUID()))
                            .userId(athleteUserId)
                            .propertyKey(propKey)
                            .propertyValue(drillGroupId.toString())
                            .build());
            return 1;
        }
    }

    public int setActiveDrillGroupName(UUID athleteUserId, String groupName) {
        String propKey = UserPropertyConstants.USER_DRILL_GROUP_NAME_KEY;
        Optional<UserPropertyRow> maybeRow = userPropertyService.findByKey(athleteUserId, propKey);
        if (maybeRow.isPresent()) {
            return userPropertyService.update(
                    UserPropertyPartial.builder()
                            .userPropertyId(Optional.of(maybeRow.get().getId()))
                            .userId(athleteUserId)
                            .propertyKey(propKey)
                            .propertyValue(groupName)
                            .build());
        } else {
            UUID userPropertyId = userPropertyService.create(
                    UserPropertyPartial.builder()
                            .userPropertyId(Optional.of(UUID.randomUUID()))
                            .userId(athleteUserId)
                            .propertyKey(propKey)
                            .propertyValue(groupName)
                            .build()
            );
            return 1;
        }
    }

    /**
     * Find and return the athlete's current active drill group level identifier
     * <p>
     * This method includes checks for account and registration status
     * <p>
     * If the athlete does not have an active level, default is: 1
     *
     * @param athleteUserId Athlete user ID
     * @return Active drill group level identifier
     */
    // public String activeDrillGroupLevelIdentifier(UUID athleteUserId, UUID currentDrillGroupId) {
    //     if (isPaidAccount(athleteUserId)) {
    //         // most likely case is that user is a paid user
    //         String propKey = UserPropertyConstants.USER_DRILL_GROUP_LEVEL_KEY;
    //         Optional<UserGroupPropertyRow> maybeRow = userGroupPropertyService.findByKey(athleteUserId, currentDrillGroupId, propKey);
    //         if (maybeRow.isEmpty()) {
    //             return "1.0";
    //         } else {
    //             return maybeRow.get().getPropertyValue();
    //         }
    //     } else if (isTrialAccount(athleteUserId)) {
    //         // trial accounts only ever have level 1 active
    //         return "1.0";
    //     } else {
    //         // something is wrong...
    //         return "1.0";
    //     }
    // }
    public String activeDrillGroupLevelIdentifier(UUID athleteUserId, UUID currentDrillGroupId) {
        String propKey = UserPropertyConstants.USER_DRILL_GROUP_LEVEL_KEY;
        Optional<UserGroupPropertyRow> maybeRow = userGroupPropertyService
                .findByKey(athleteUserId, currentDrillGroupId, propKey);
        return maybeRow.map(UserGroupPropertyRow::getPropertyValue).orElse("1.0");
}

    public String activeDrillGroupOrderIndexIdentifier(UUID athleteUserId, UUID currentDrillGroupId) {
        String propKey = UserPropertyConstants.USER_DRILL_GROUP_ORDER_INDEX_KEY;
        Optional<UserGroupPropertyRow> maybeRow = userGroupPropertyService
                .findByKey(athleteUserId, currentDrillGroupId, propKey);
        return maybeRow.map(UserGroupPropertyRow::getPropertyValue).orElse("1.0");
    }
    // public String activeDrillGroupOrderIndexIdentifier(UUID athleteUserId, UUID currentDrillGroupId) {
    //     if (isPaidAccount(athleteUserId)) {
    //         String propKey = UserPropertyConstants.USER_DRILL_GROUP_ORDER_INDEX_KEY;
    //         Optional<UserGroupPropertyRow> maybeRow = userGroupPropertyService.findByKey(athleteUserId, currentDrillGroupId, propKey);
    //         if (maybeRow.isEmpty()) {
    //             logger.info("no order index found");
    //             return "1.0";
    //         } else {
    //             logger.info("order index found");
    //             return maybeRow.get().getPropertyValue();
    //         }
    //     } else if (isTrialAccount(athleteUserId)) {
    //         // trial accounts only ever have order index 1 active
    //         logger.info("is trail account");
    //         return "1.0";
    //     } else {
    //         return "1.0";
    //     }
    // }

    /**
     * Return the currently active drill group level index for the athlete
     * identified by the athlete user ID.
     * <p>
     * Note callers should be aware that the level index is an integer. A level
     * index alone is not sufficient to communicate test status
     * <p>
     * If the athlete does not have an active level, default is: 1
     *
     * @param athleteUserId Athlete user identifier
     * @return Active drill group level
     */
    public int activeDrillGroupLevelIndex(UUID athleteUserId, UUID currentDrillGroupId) {
        String levelIdentifier = activeDrillGroupLevelIdentifier(athleteUserId, currentDrillGroupId);
        return DrillItemUtils.levelIdentifierToInt(levelIdentifier);
    }

    public int activeDrillGroupOrderIndex(UUID athleteUserId, UUID currentDrillGroupId) {
        String orderIndexIdentifier = activeDrillGroupOrderIndexIdentifier(athleteUserId, currentDrillGroupId);
        return DrillItemUtils.orderIndexIdentifierToInt(orderIndexIdentifier);
    }

    /**
     * Set the active drill group level property value for the specified athlete
     * user ID
     *
     * @param athleteUserId Athlete user ID
     * @param levelIdentifier Active drill group level identifier
     * @return 1 if success; 0 if error
     */
    public int setActiveDrillGroupLevel(UUID athleteUserId,
            String levelIdentifier, UUID currentDrillGroupId) {
        String propKey = UserPropertyConstants.USER_DRILL_GROUP_LEVEL_KEY;
        Optional<UserGroupPropertyRow> maybeRow = userGroupPropertyService
                .findByKey(athleteUserId, currentDrillGroupId, propKey);
        if (maybeRow.isPresent()) {
            return userGroupPropertyService.update(
                    UserGroupPropertyPartial.builder()
                            .propertyId(Optional.of(maybeRow.get().getId()))
                            .userId(athleteUserId)
                            .groupId(currentDrillGroupId)
                            .propertyKey(propKey)
                            .propertyValue(levelIdentifier)
                            .build()
            );
        } else {
            UUID userPropertyId = userGroupPropertyService.create(
                    UserGroupPropertyPartial.builder()
                            .propertyId(Optional.empty())
                            .userId(athleteUserId)
                            .groupId(currentDrillGroupId)
                            .propertyKey(propKey)
                            .propertyValue(levelIdentifier)
                            .build()
            );
            return 1;
        }
    }

    public int setActiveDrillGroupOrderIndex(UUID athleteUserId,
            String orderIndexIdentifier, UUID currentDrillGroupId) {

        String propKey = UserPropertyConstants.USER_DRILL_GROUP_ORDER_INDEX_KEY;
        Optional<UserGroupPropertyRow> maybeRow = userGroupPropertyService
                .findByKey(athleteUserId, currentDrillGroupId, propKey);
        if (maybeRow.isPresent()) {
            return userGroupPropertyService.update(
                    UserGroupPropertyPartial.builder()
                            .propertyId(Optional.of(maybeRow.get().getId()))
                            .userId(athleteUserId)
                            .groupId(currentDrillGroupId)
                            .propertyKey(propKey)
                            .propertyValue(orderIndexIdentifier)
                            .build()
            );
        } else {
            UUID userPropertyId = userGroupPropertyService.create(
                    UserGroupPropertyPartial.builder()
                            .propertyId(Optional.empty())
                            .userId(athleteUserId)
                            .groupId(currentDrillGroupId)
                            .propertyKey(propKey)
                            .propertyValue(orderIndexIdentifier)
                            .build()
            );
            return 1;
        }
    }

    /**
     * Set the current level completion percent property value for the specified
     * athlete user ID
     *
     * @param athleteUserId Athlete user ID
     * @param completionPercent Level completion percentage as a string
     * @return 1 if success; 0 if error
     */
    public int setCurrentLevelCompletionPercent(UUID athleteUserId,
            String completionPercent, UUID currentDrillGroupId) {
        String propKey = UserPropertyConstants.USER_METRIC_DRILL_LEVEL_COMPLETION_PERCENT;
        Optional<UserGroupPropertyRow> maybeRow = userGroupPropertyService
                .findByKey(athleteUserId, currentDrillGroupId, propKey);
        if (maybeRow.isPresent()) {
            return userGroupPropertyService.update(
                    UserGroupPropertyPartial.builder()
                            .propertyId(Optional.of(maybeRow.get().getId()))
                            .userId(athleteUserId)
                            .groupId(currentDrillGroupId)
                            .propertyKey(propKey)
                            .propertyValue(completionPercent)
                            .build()
            );
        } else {
            UUID userPropertyId = userGroupPropertyService.create(
                    UserGroupPropertyPartial.builder()
                            .propertyId(Optional.empty())
                            .userId(athleteUserId)
                            .groupId(currentDrillGroupId)
                            .propertyKey(propKey)
                            .propertyValue(completionPercent)
                            .build()
            );
            return 1;
        }
    }

    /**
     * Set the current group completion percent property value for the specified
     * athlete user ID
     *
     * @param athleteUserId Athlete user ID
     * @param completionPercent Group completion percentage as a string
     * @return 1 if success; 0 if error
     */
    public int setCurrentGroupCompletionPercent(UUID athleteUserId,
            String completionPercent, UUID currentDrillGroupId) {
        String propKey = UserPropertyConstants.USER_METRIC_DRILL_GROUP_COMPLETION_PERCENT;
        Optional<UserGroupPropertyRow> maybeRow = userGroupPropertyService
                .findByKey(athleteUserId, currentDrillGroupId, propKey);
        if (maybeRow.isPresent()) {
            return userGroupPropertyService.update(
                    UserGroupPropertyPartial.builder()
                            .propertyId(Optional.of(maybeRow.get().getId()))
                            .userId(athleteUserId)
                            .groupId(currentDrillGroupId)
                            .propertyKey(propKey)
                            .propertyValue(completionPercent)
                            .build()
            );
        } else {
            UUID userPropertyId = userGroupPropertyService.create(
                    UserGroupPropertyPartial.builder()
                            .propertyId(Optional.empty())
                            .userId(athleteUserId)
                            .groupId(currentDrillGroupId)
                            .propertyKey(propKey)
                            .propertyValue(completionPercent)
                            .build()
            );
            return 1;
        }
    }

    /**
     * @param athleteUserId Athlete user ID
     * @return True if the user has a paid account
     */
    public boolean isPaidAccount(UUID athleteUserId) {
        String propKey = UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_KEY;
        Optional<UserPropertyRow> maybeRow = userPropertyService.findByKey(athleteUserId, propKey);
        if (maybeRow.isEmpty()) {
            return false;
        } else {
            return maybeRow.get().getPropertyValue()
                    .equalsIgnoreCase(UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_PAID);
        }
    }

    /**
     * @param athleteUserId Athlete user ID
     * @return True if the user has a trial account
     */
    public boolean isTrialAccount(UUID athleteUserId) {
        String propKey = UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_KEY;
        Optional<UserPropertyRow> maybeRow = userPropertyService.findByKey(athleteUserId, propKey);
        if (maybeRow.isEmpty()) {
            return false;
        } else {
            return maybeRow.get().getPropertyValue()
                    .equalsIgnoreCase(UserPropertyConstants.USER_REGISTRATION_PAYMENT_STATE_TRIAL);
        }
    }
}
