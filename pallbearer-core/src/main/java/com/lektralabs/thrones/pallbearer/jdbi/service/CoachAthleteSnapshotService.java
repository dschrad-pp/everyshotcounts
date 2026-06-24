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
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.ShootingZoneRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.SkillBreakdownRow;
import com.lektralabs.thrones.pallbearer.manager.AthleteMetricManager;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
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

    /**
     * Feature flag for the metric redefinition that scopes skill-breakdown
     * coverage to the athlete's active difficulty (drill group) instead of the
     * whole catalog. Default on (PM-signed-off): the whole-catalog denominator
     * produced misleadingly low (~1%) coverage. Set the config property to
     * {@code false} to revert per-environment. Toggling it changes the
     * {@code coveragePercent} values but never the response shape.
     */
    @ConfigProperty(name = "snapshot.skill-breakdown.scope-by-difficulty", defaultValue = "true")
    boolean scopeSkillBreakdownByDifficulty;

    @Inject
    JdbiProvider jdbiProvider;

    @Inject
    UserPropertyService userPropertyService;

    @Inject
    UserGroupPropertyService userGroupPropertyService;

    @Inject
    DrillGroupService drillGroupService;

    @Inject
    AthleteMetricManager athleteMetricManager;

    CoachAthleteSnapshotDao snapshotDao;

    @PostConstruct
    public void init() {
        this.snapshotDao = jdbiProvider.getJdbi().onDemand(CoachAthleteSnapshotDao.class);
    }

    /**
     * Headline make/attempt totals for the athlete, scoped to their currently-active
     * difficulty (drill group) — resolved the SAME way as the shooting zones and
     * skill breakdown ({@link #resolveActiveDifficultyGroupId(UUID)}) so the headline
     * spans exactly the tier those numbers cover. When no drill group can be resolved
     * the (null) id is passed through; the query then matches no rows and returns a
     * zeroed stats row rather than mixing every tier together.
     */
    public AthleteSnapshotStatsRow getAthleteStats(UUID athleteId) {
        UUID difficultyGroupId = resolveActiveDifficultyGroupId(athleteId);
        return snapshotDao.getAthleteStats(athleteId, difficultyGroupId);
    }

    public List<SkillBreakdownRow> getSkillBreakdown(UUID athleteId) {
        if (!scopeSkillBreakdownByDifficulty) {
            return snapshotDao.getSkillBreakdown(athleteId);
        }
        UUID difficultyGroupId = resolveActiveDifficultyGroupId(athleteId);
        if (difficultyGroupId == null) {
            // No drill groups exist at all — nothing to scope coverage to. Omit
            // the breakdown rather than silently reverting to the whole-catalog calc.
            logger.warn("No drill group resolvable for athlete {}; returning empty skill breakdown", athleteId);
            return Collections.emptyList();
        }
        return snapshotDao.getSkillBreakdownByDifficulty(athleteId, difficultyGroupId);
    }

    /**
     * Resolves the athlete's active difficulty (drill group) for skill-breakdown
     * scoping. Reads the same {@code USER_DRILL_GROUP_KEY} user property that powers
     * the level label. When it is missing or unparseable, falls back deterministically
     * to the lowest-order drill group (Beginner) — never to a whole-catalog calculation.
     * Returns {@code null} only when no drill groups exist at all.
     */
    UUID resolveActiveDifficultyGroupId(UUID athleteId) {
        Optional<UserPropertyRow> maybeGroup = userPropertyService
                .findByKey(athleteId, UserPropertyConstants.USER_DRILL_GROUP_KEY);
        if (maybeGroup.isPresent()) {
            try {
                return UUID.fromString(maybeGroup.get().getPropertyValue());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid drill group UUID for athlete {}; falling back to lowest group", athleteId);
            }
        }
        return snapshotDao.getLowestDrillGroupId();
    }

    /**
     * Fixed display order for shooting zones. The snapshot always returns all
     * three, in this order, even when the athlete has no attempts in a zone.
     */
    static final List<String> SHOOTING_ZONE_ORDER = List.of("THREE_POINT", "FIFTEEN_FEET", "FREE_THROW");

    /**
     * Returns the athlete's make/attempt totals for every court zone, always as
     * exactly three rows in {@link #SHOOTING_ZONE_ORDER}, scoped to the athlete's
     * currently-active difficulty (drill group). The active difficulty is
     * resolved the same way as the skill breakdown and level label
     * ({@link #resolveActiveDifficultyGroupId(UUID)}): the {@code USER_DRILL_GROUP_KEY}
     * property, falling back to the lowest (Beginner) group. Zones the athlete
     * has no data in for that difficulty are padded with zero makes/attempts.
     * When no drill groups exist at all, every zone is returned zeroed.
     */
    public List<ShootingZoneRow> getShootingZones(UUID athleteId) {
        UUID difficultyGroupId = resolveActiveDifficultyGroupId(athleteId);
        List<ShootingZoneRow> rows = (difficultyGroupId == null)
                ? Collections.emptyList()
                : snapshotDao.getShootingZonesByDifficulty(athleteId, difficultyGroupId);
        Map<String, ShootingZoneRow> byZone = rows.stream()
                .collect(Collectors.toMap(ShootingZoneRow::getZoneCode, row -> row, (a, b) -> a));
        return SHOOTING_ZONE_ORDER.stream()
                .map(zone -> byZone.getOrDefault(zone, new ShootingZoneRow(zone, 0, 0)))
                .collect(Collectors.toList());
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
     * Returns the athlete's current-level progress percent (0–100).
     * <p>
     * Computed LIVE for the active group/level (passed ÷ total drills, floor-
     * rounded) rather than read from the cached
     * {@code user.metric.drill.level.completion.percent} property. The cached
     * value goes stale per tier once an athlete advances; recomputing on read —
     * the same way the shooting-zone aggregations work — permanently removes the
     * stale-tier class of bug. Defaults to 0 when nothing can be resolved.
     */
    public int getLevelProgress(UUID athleteId) {
        return athleteMetricManager.computeCurrentLevelCompletionPercent(athleteId);
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

    /**
     * Returns just the athlete's current difficulty TIER name (e.g. "Beginner",
     * "Intermediate", "Advanced", "Elite") — no level/sub-level suffix. Resolved
     * via {@link #resolveActiveDifficultyGroupId(UUID)}, the SAME group id the
     * shooting-zone aggregation is scoped to, so this label always matches the
     * span of those numbers (the whole tier, every level, no level cap). Falls
     * back to an empty string when no tier can be resolved.
     */
    public String getDifficultyTier(UUID athleteId) {
        UUID tierGroupId = resolveActiveDifficultyGroupId(athleteId);
        if (tierGroupId == null) {
            return "";
        }
        return drillGroupService.findById(tierGroupId)
                .flatMap(DrillGroupRow::getName)
                .orElse("");
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
