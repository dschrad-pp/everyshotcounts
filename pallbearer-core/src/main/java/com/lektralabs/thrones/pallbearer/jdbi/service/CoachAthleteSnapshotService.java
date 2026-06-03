package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.common.UserPropertyConstants;
import com.lektralabs.thrones.pallbearer.jdbi.JdbiProvider;
import com.lektralabs.thrones.pallbearer.jdbi.dao.CoachAthleteSnapshotDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserGroupPropertyRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserPropertyRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.generated.DrillGroupRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.AthleteSnapshotStatsRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.DrillSkillTagRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.DrillStatsRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.SkillBreakdownRow;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CoachAthleteSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(CoachAthleteSnapshotService.class);

    // Maps the 6 iOS skill codes → the existing DB tag codes that represent them
    private static final Map<String, List<String>> SKILL_TO_DB_TAGS = Map.of(
            "PULL_UP",     List.of("PULL_UP_L", "PULL_UP_R"),
            "STEP_BACK",   List.of("STEP_BACK_L", "STEP_BACK_R"),
            "THREE_POINT", List.of("3PT"),
            "SHOOTING",    List.of("15FT", "DEPTH_SHOOTING"),
            "CATCH_SHOOT", List.of("CATCH_AND_SHOOT"),
            "FOOTWORK",    List.of() // no existing DB tag for FOOTWORK; omitted from skill breakdown intentionally
    );

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    UserPropertyService userPropertyService;

    @Inject
    UserGroupPropertyService userGroupPropertyService;

    @Inject
    DrillGroupService drillGroupService;

    private CoachAthleteSnapshotDao snapshotDao;

    @PostConstruct
    public void init() {
        this.snapshotDao = jdbiProvider.getJdbi().onDemand(CoachAthleteSnapshotDao.class);
    }

    public AthleteSnapshotStatsRow getAthleteStats(UUID athleteId) {
        return snapshotDao.getAthleteStats(athleteId);
    }

    public List<SkillBreakdownRow> getSkillBreakdown(UUID athleteId) {
        return snapshotDao.getSkillBreakdown(athleteId);
    }

    public List<DrillStatsRow> getDrillStats(UUID athleteId, List<String> skillCodes, int page, int limit) {
        int offset = page * limit;
        List<String> dbCodes = translateToDbTagCodes(skillCodes);
        if (dbCodes == null || dbCodes.isEmpty()) {
            return snapshotDao.getDrillStats(athleteId, limit, offset);
        }
        return snapshotDao.getDrillStatsWithTagFilter(athleteId, dbCodes, limit, offset);
    }

    public Map<UUID, List<DrillSkillTagRow>> getSkillTagsForDrillItems(List<UUID> drillItemIds) {
        if (drillItemIds == null || drillItemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return snapshotDao.getSkillTagsForDrillItems(drillItemIds).stream()
                .collect(Collectors.groupingBy(DrillSkillTagRow::getDrillItemId));
    }

    /**
     * Translates iOS skill codes (e.g. "PULL_UP") to the actual tag codes stored
     * in t_tag (e.g. "PULL_UP_L", "PULL_UP_R"). Unrecognised codes are silently dropped.
     */
    List<String> translateToDbTagCodes(List<String> skillCodes) {
        if (skillCodes == null || skillCodes.isEmpty()) {
            return null;
        }
        List<String> dbCodes = skillCodes.stream()
                .map(String::toUpperCase)
                .flatMap(code -> SKILL_TO_DB_TAGS.getOrDefault(code, List.of()).stream())
                .distinct()
                .collect(Collectors.toList());
        return dbCodes.isEmpty() ? null : dbCodes;
    }

    /**
     * Returns the athlete's active drill group order index. Defaults to 1 if not set.
     */
    public int getActiveDrillGroupOrderIndex(UUID athleteId) {
        Optional<UserPropertyRow> maybeGroup = userPropertyService
                .findByKey(athleteId, UserPropertyConstants.USER_DRILL_GROUP_KEY);
        if (maybeGroup.isEmpty()) {
            return 1;
        }
        UUID activeGroupId;
        try {
            activeGroupId = UUID.fromString(maybeGroup.get().getPropertyValue());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid drill group UUID for athlete {}", athleteId);
            return 1;
        }
        Optional<UserGroupPropertyRow> maybeIndex = userGroupPropertyService
                .findByKey(athleteId, activeGroupId, UserPropertyConstants.USER_DRILL_GROUP_ORDER_INDEX_KEY);
        if (maybeIndex.isEmpty()) {
            return 1;
        }
        try {
            return (int) Double.parseDouble(maybeIndex.get().getPropertyValue());
        } catch (NumberFormatException e) {
            logger.warn("Unparseable order index for athlete {}", athleteId);
            return 1;
        }
    }

    /**
     * Returns the athlete's level progress percent (0–100). Defaults to 0 if not set.
     */
    public int getLevelProgress(UUID athleteId) {
        Optional<UserPropertyRow> maybeGroup = userPropertyService
                .findByKey(athleteId, UserPropertyConstants.USER_DRILL_GROUP_KEY);
        if (maybeGroup.isEmpty()) {
            return 0;
        }
        UUID activeGroupId;
        try {
            activeGroupId = UUID.fromString(maybeGroup.get().getPropertyValue());
        } catch (IllegalArgumentException e) {
            return 0;
        }
        Optional<UserGroupPropertyRow> maybePct = userGroupPropertyService
                .findByKey(athleteId, activeGroupId, UserPropertyConstants.USER_METRIC_DRILL_LEVEL_COMPLETION_PERCENT);
        if (maybePct.isEmpty()) {
            return 0;
        }
        try {
            int pct = (int) Math.round(Double.parseDouble(maybePct.get().getPropertyValue()));
            return Math.min(100, Math.max(0, pct));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Returns a human-readable level label for the athlete's active drill group and position,
     * e.g. "Beginner Level 3" or "Intermediate Test 2". Falls back to "Level X" if the group
     * has no name or cannot be resolved.
     */
    public String getLevelLabel(UUID athleteId) {
        Optional<UserPropertyRow> maybeGroup = userPropertyService
                .findByKey(athleteId, UserPropertyConstants.USER_DRILL_GROUP_KEY);
        if (maybeGroup.isEmpty()) {
            return buildLevelLabel("", 1);
        }
        UUID activeGroupId;
        try {
            activeGroupId = UUID.fromString(maybeGroup.get().getPropertyValue());
        } catch (IllegalArgumentException e) {
            return buildLevelLabel("", 1);
        }
        String groupName = drillGroupService.findById(activeGroupId)
                .flatMap(DrillGroupRow::getName)
                .orElse("");
        Optional<UserGroupPropertyRow> maybeIndex = userGroupPropertyService
                .findByKey(athleteId, activeGroupId, UserPropertyConstants.USER_DRILL_GROUP_ORDER_INDEX_KEY);
        int orderIndex = 1;
        if (maybeIndex.isPresent()) {
            try {
                orderIndex = (int) Double.parseDouble(maybeIndex.get().getPropertyValue());
            } catch (NumberFormatException e) {
                logger.warn("Unparseable order index for athlete {}", athleteId);
            }
        }
        return buildLevelLabel(groupName, orderIndex);
    }

    public static String buildLevelLabel(String groupName, int orderIndex) {
        String levelPart = (orderIndex > 0 && orderIndex % 3 == 0)
                ? "Test " + (orderIndex / 3)
                : "Level " + orderIndex;
        return groupName == null || groupName.isBlank() ? levelPart : groupName + " " + levelPart;
    }

    public static int computeMakePercent(int totalMakes, int totalAttempts) {
        if (totalAttempts == 0) return 0;
        return (int) Math.min(100, Math.max(0, Math.round(totalMakes * 100.0 / totalAttempts)));
    }
}
