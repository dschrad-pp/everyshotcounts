package com.lektralabs.thrones.pallbearer.jdbi.service;

import com.lektralabs.thrones.pallbearer.common.UserPropertyConstants;
import com.lektralabs.thrones.pallbearer.jdbi.dao.CoachAthleteSnapshotDao;
import com.lektralabs.thrones.pallbearer.jdbi.model.UserPropertyRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.ShootingZoneRow;
import com.lektralabs.thrones.pallbearer.jdbi.model.snapshot.SkillBreakdownRow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoachAthleteSnapshotServiceTest {

    // ── buildLevelLabel ──────────────────────────────────────────────────────

    @ParameterizedTest(name = "orderIndex={0} → {1}")
    @CsvSource({
        "1,  Level 1",
        "2,  Level 2",
        "3,  Test 1",
        "4,  Level 4",
        "5,  Level 5",
        "6,  Test 2",
        "9,  Test 3",
        "12, Test 4"
    })
    void buildLevelLabel_noGroupName_returnsCorrectLabel(int orderIndex, String expected) {
        assertEquals(expected, CoachAthleteSnapshotService.buildLevelLabel("", orderIndex));
    }

    @Test
    void buildLevelLabel_zeroIndex_returnsLevelZero() {
        assertEquals("Level 0", CoachAthleteSnapshotService.buildLevelLabel("", 0));
    }

    @Test
    void buildLevelLabel_withGroupName_prependsGroupName() {
        assertEquals("Beginner Level 1", CoachAthleteSnapshotService.buildLevelLabel("Beginner", 1));
        assertEquals("Intermediate Test 1", CoachAthleteSnapshotService.buildLevelLabel("Intermediate", 3));
    }

    @Test
    void buildLevelLabel_nullGroupName_treatedAsBlank() {
        assertEquals("Level 2", CoachAthleteSnapshotService.buildLevelLabel(null, 2));
    }

    // ── computeMakePercent ───────────────────────────────────────────────────

    @Test
    void computeMakePercent_zeroAttempts_returnsZero() {
        assertEquals(0, CoachAthleteSnapshotService.computeMakePercent(0, 0));
        assertEquals(0, CoachAthleteSnapshotService.computeMakePercent(5, 0));
    }

    @Test
    void computeMakePercent_allMakes_returns100() {
        assertEquals(100, CoachAthleteSnapshotService.computeMakePercent(10, 10));
    }

    @Test
    void computeMakePercent_halfMakes_returns50() {
        assertEquals(50, CoachAthleteSnapshotService.computeMakePercent(5, 10));
    }

    @Test
    void computeMakePercent_roundsCorrectly() {
        assertEquals(67, CoachAthleteSnapshotService.computeMakePercent(2, 3));
        assertEquals(33, CoachAthleteSnapshotService.computeMakePercent(1, 3));
    }

    @Test
    void computeMakePercent_clampedAt100() {
        assertEquals(100, CoachAthleteSnapshotService.computeMakePercent(200, 100));
    }

    @Test
    void computeMakePercent_clampedAt0() {
        assertEquals(0, CoachAthleteSnapshotService.computeMakePercent(-5, 10));
    }

    // ── translateToDbTagCodes ────────────────────────────────────────────────

    private final CoachAthleteSnapshotService service = new CoachAthleteSnapshotService();

    @Test
    void translateToDbTagCodes_nullInput_returnsNull() {
        assertNull(service.translateToDbTagCodes(null));
    }

    @Test
    void translateToDbTagCodes_emptyInput_returnsNull() {
        assertNull(service.translateToDbTagCodes(List.of()));
    }

    @Test
    void translateToDbTagCodes_pullUp_expandsToBothSides() {
        List<String> result = service.translateToDbTagCodes(List.of("PULL_UP"));
        assertNotNull(result);
        assertTrue(result.containsAll(List.of("PULL_UP_L", "PULL_UP_R")));
        assertEquals(2, result.size());
    }

    @Test
    void translateToDbTagCodes_stepBack_expandsToBothSides() {
        List<String> result = service.translateToDbTagCodes(List.of("STEP_BACK"));
        assertNotNull(result);
        assertTrue(result.containsAll(List.of("STEP_BACK_L", "STEP_BACK_R")));
    }

    @Test
    void translateToDbTagCodes_shooting_expandsToBothCodes() {
        List<String> result = service.translateToDbTagCodes(List.of("SHOOTING"));
        assertNotNull(result);
        assertTrue(result.containsAll(List.of("15FT", "DEPTH_SHOOTING")));
    }

    @Test
    void translateToDbTagCodes_threePoint_returnsSingleCode() {
        List<String> result = service.translateToDbTagCodes(List.of("THREE_POINT"));
        assertNotNull(result);
        assertEquals(List.of("3PT"), result);
    }

    @Test
    void translateToDbTagCodes_catchShoot_returnsSingleCode() {
        List<String> result = service.translateToDbTagCodes(List.of("CATCH_SHOOT"));
        assertNotNull(result);
        assertEquals(List.of("CATCH_AND_SHOOT"), result);
    }

    @Test
    void translateToDbTagCodes_footwork_returnsNullBecauseNoDbTags() {
        assertNull(service.translateToDbTagCodes(List.of("FOOTWORK")));
    }

    @Test
    void translateToDbTagCodes_unknownCode_returnsNull() {
        assertNull(service.translateToDbTagCodes(List.of("UNKNOWN_SKILL")));
    }

    @Test
    void translateToDbTagCodes_caseInsensitiveInput() {
        List<String> result = service.translateToDbTagCodes(List.of("pull_up"));
        assertNotNull(result);
        assertTrue(result.containsAll(List.of("PULL_UP_L", "PULL_UP_R")));
    }

    @Test
    void translateToDbTagCodes_deduplicatesExpandedCodes() {
        List<String> result = service.translateToDbTagCodes(List.of("PULL_UP", "PULL_UP"));
        assertNotNull(result);
        assertEquals(2, result.size()); // PULL_UP_L and PULL_UP_R, not 4
    }

    @Test
    void translateToDbTagCodes_multipleCodes_mergesAll() {
        List<String> result = service.translateToDbTagCodes(List.of("PULL_UP", "THREE_POINT"));
        assertNotNull(result);
        assertTrue(result.containsAll(List.of("PULL_UP_L", "PULL_UP_R", "3PT")));
        assertEquals(3, result.size());
    }

    // ── getSkillBreakdown: difficulty-scoping feature flag ────────────────────

    private static final UUID ATHLETE = UUID.randomUUID();
    private static final UUID ACTIVE_GROUP = UUID.randomUUID();
    private static final UUID LOWEST_GROUP = UUID.randomUUID();

    private CoachAthleteSnapshotService serviceWith(boolean flag,
                                                    CoachAthleteSnapshotDao dao,
                                                    UserPropertyService userProps) {
        CoachAthleteSnapshotService s = new CoachAthleteSnapshotService();
        s.scopeSkillBreakdownByDifficulty = flag;
        s.snapshotDao = dao;
        s.userPropertyService = userProps;
        return s;
    }

    private UserPropertyRow propertyRow(String value) {
        UserPropertyRow row = mock(UserPropertyRow.class);
        when(row.getPropertyValue()).thenReturn(value);
        return row;
    }

    @Test
    void getSkillBreakdown_flagOff_usesWholeCatalogQuery() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        UserPropertyService userProps = mock(UserPropertyService.class);
        when(dao.getSkillBreakdown(ATHLETE)).thenReturn(List.of(new SkillBreakdownRow()));

        CoachAthleteSnapshotService s = serviceWith(false, dao, userProps);
        List<SkillBreakdownRow> result = s.getSkillBreakdown(ATHLETE);

        assertEquals(1, result.size());
        verify(dao).getSkillBreakdown(ATHLETE);
        verify(dao, never()).getSkillBreakdownByDifficulty(any(), any());
        verify(dao, never()).getLowestDrillGroupId();
    }

    @Test
    void getSkillBreakdown_flagOn_activeGroupSet_usesDifficultyScopedQuery() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        UserPropertyService userProps = mock(UserPropertyService.class);
        when(userProps.findByKey(ATHLETE, UserPropertyConstants.USER_DRILL_GROUP_KEY))
                .thenReturn(Optional.of(propertyRow(ACTIVE_GROUP.toString())));
        when(dao.getSkillBreakdownByDifficulty(ATHLETE, ACTIVE_GROUP))
                .thenReturn(List.of(new SkillBreakdownRow()));

        CoachAthleteSnapshotService s = serviceWith(true, dao, userProps);
        List<SkillBreakdownRow> result = s.getSkillBreakdown(ATHLETE);

        assertEquals(1, result.size());
        verify(dao).getSkillBreakdownByDifficulty(ATHLETE, ACTIVE_GROUP);
        verify(dao, never()).getSkillBreakdown(any());
        verify(dao, never()).getLowestDrillGroupId();
    }

    @Test
    void getSkillBreakdown_flagOn_noActiveGroup_fallsBackToLowestGroup() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        UserPropertyService userProps = mock(UserPropertyService.class);
        when(userProps.findByKey(ATHLETE, UserPropertyConstants.USER_DRILL_GROUP_KEY))
                .thenReturn(Optional.empty());
        when(dao.getLowestDrillGroupId()).thenReturn(LOWEST_GROUP);

        CoachAthleteSnapshotService s = serviceWith(true, dao, userProps);
        s.getSkillBreakdown(ATHLETE);

        verify(dao).getSkillBreakdownByDifficulty(ATHLETE, LOWEST_GROUP);
    }

    @Test
    void getSkillBreakdown_flagOn_invalidGroupUuid_fallsBackToLowestGroup() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        UserPropertyService userProps = mock(UserPropertyService.class);
        when(userProps.findByKey(ATHLETE, UserPropertyConstants.USER_DRILL_GROUP_KEY))
                .thenReturn(Optional.of(propertyRow("not-a-uuid")));
        when(dao.getLowestDrillGroupId()).thenReturn(LOWEST_GROUP);

        CoachAthleteSnapshotService s = serviceWith(true, dao, userProps);
        s.getSkillBreakdown(ATHLETE);

        verify(dao).getSkillBreakdownByDifficulty(ATHLETE, LOWEST_GROUP);
    }

    @Test
    void getSkillBreakdown_flagOn_noGroupsExist_returnsEmptyWithoutQuerying() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        UserPropertyService userProps = mock(UserPropertyService.class);
        when(userProps.findByKey(ATHLETE, UserPropertyConstants.USER_DRILL_GROUP_KEY))
                .thenReturn(Optional.empty());
        when(dao.getLowestDrillGroupId()).thenReturn(null);

        CoachAthleteSnapshotService s = serviceWith(true, dao, userProps);
        List<SkillBreakdownRow> result = s.getSkillBreakdown(ATHLETE);

        assertTrue(result.isEmpty());
        verify(dao, never()).getSkillBreakdownByDifficulty(any(), any());
        verify(dao, never()).getSkillBreakdown(any());
    }

    // ── getShootingZones: always three zones, fixed order, difficulty-scoped ──

    /**
     * Builds a service whose active-difficulty resolution returns {@link #ACTIVE_GROUP}
     * (USER_DRILL_GROUP_KEY set), with the given DAO wired in.
     */
    private CoachAthleteSnapshotService zoneServiceWithActiveGroup(CoachAthleteSnapshotDao dao) {
        UserPropertyService userProps = mock(UserPropertyService.class);
        when(userProps.findByKey(ATHLETE, UserPropertyConstants.USER_DRILL_GROUP_KEY))
                .thenReturn(Optional.of(propertyRow(ACTIVE_GROUP.toString())));
        return serviceWith(true, dao, userProps);
    }

    @Test
    void getShootingZones_scopesToActiveDifficultyGroup() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        when(dao.getShootingZonesByDifficulty(ATHLETE, ACTIVE_GROUP)).thenReturn(List.of());

        zoneServiceWithActiveGroup(dao).getShootingZones(ATHLETE);

        verify(dao).getShootingZonesByDifficulty(ATHLETE, ACTIVE_GROUP);
    }

    @Test
    void getShootingZones_noActiveGroup_fallsBackToLowestGroup() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        UserPropertyService userProps = mock(UserPropertyService.class);
        when(userProps.findByKey(ATHLETE, UserPropertyConstants.USER_DRILL_GROUP_KEY))
                .thenReturn(Optional.empty());
        when(dao.getLowestDrillGroupId()).thenReturn(LOWEST_GROUP);
        when(dao.getShootingZonesByDifficulty(ATHLETE, LOWEST_GROUP)).thenReturn(List.of());

        serviceWith(true, dao, userProps).getShootingZones(ATHLETE);

        verify(dao).getShootingZonesByDifficulty(ATHLETE, LOWEST_GROUP);
    }

    @Test
    void getShootingZones_noGroupsExist_returnsAllThreeZeroedWithoutQuerying() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        UserPropertyService userProps = mock(UserPropertyService.class);
        when(userProps.findByKey(ATHLETE, UserPropertyConstants.USER_DRILL_GROUP_KEY))
                .thenReturn(Optional.empty());
        when(dao.getLowestDrillGroupId()).thenReturn(null);

        List<ShootingZoneRow> zones = serviceWith(true, dao, userProps).getShootingZones(ATHLETE);

        assertEquals(List.of("THREE_POINT", "FIFTEEN_FEET", "FREE_THROW"),
                zones.stream().map(ShootingZoneRow::getZoneCode).toList());
        assertTrue(zones.stream().allMatch(z -> z.getTotalMakes() == 0 && z.getTotalAttempts() == 0));
        verify(dao, never()).getShootingZonesByDifficulty(any(), any());
    }

    @Test
    void getShootingZones_noData_returnsAllThreeZonesZeroedInOrder() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        when(dao.getShootingZonesByDifficulty(ATHLETE, ACTIVE_GROUP)).thenReturn(List.of());

        List<ShootingZoneRow> zones = zoneServiceWithActiveGroup(dao).getShootingZones(ATHLETE);

        assertEquals(List.of("THREE_POINT", "FIFTEEN_FEET", "FREE_THROW"),
                zones.stream().map(ShootingZoneRow::getZoneCode).toList());
        assertTrue(zones.stream().allMatch(z -> z.getTotalMakes() == 0 && z.getTotalAttempts() == 0));
    }

    @Test
    void getShootingZones_partialData_padsMissingZonesAndKeepsOrder() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        // DAO returns only the zone(s) with data, in arbitrary order
        when(dao.getShootingZonesByDifficulty(ATHLETE, ACTIVE_GROUP))
                .thenReturn(List.of(new ShootingZoneRow("FREE_THROW", 9, 10)));

        List<ShootingZoneRow> zones = zoneServiceWithActiveGroup(dao).getShootingZones(ATHLETE);

        assertEquals(3, zones.size());
        assertEquals(List.of("THREE_POINT", "FIFTEEN_FEET", "FREE_THROW"),
                zones.stream().map(ShootingZoneRow::getZoneCode).toList());
        assertEquals(0, zones.get(0).getTotalAttempts());
        assertEquals(0, zones.get(1).getTotalAttempts());
        assertEquals(9, zones.get(2).getTotalMakes());
        assertEquals(10, zones.get(2).getTotalAttempts());
    }

    @Test
    void getShootingZones_allZonesPresent_reordersToCanonicalOrder() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        when(dao.getShootingZonesByDifficulty(ATHLETE, ACTIVE_GROUP)).thenReturn(List.of(
                new ShootingZoneRow("FREE_THROW", 5, 6),
                new ShootingZoneRow("FIFTEEN_FEET", 40, 100),
                new ShootingZoneRow("THREE_POINT", 63, 180)));

        List<ShootingZoneRow> zones = zoneServiceWithActiveGroup(dao).getShootingZones(ATHLETE);

        assertEquals(List.of("THREE_POINT", "FIFTEEN_FEET", "FREE_THROW"),
                zones.stream().map(ShootingZoneRow::getZoneCode).toList());
        assertEquals(63, zones.get(0).getTotalMakes());
        assertEquals(180, zones.get(0).getTotalAttempts());
    }

    @Test
    void resolveActiveDifficultyGroupId_validProperty_returnsThatGroup() {
        CoachAthleteSnapshotDao dao = mock(CoachAthleteSnapshotDao.class);
        UserPropertyService userProps = mock(UserPropertyService.class);
        when(userProps.findByKey(eq(ATHLETE), eq(UserPropertyConstants.USER_DRILL_GROUP_KEY)))
                .thenReturn(Optional.of(propertyRow(ACTIVE_GROUP.toString())));

        CoachAthleteSnapshotService s = serviceWith(true, dao, userProps);

        assertEquals(ACTIVE_GROUP, s.resolveActiveDifficultyGroupId(ATHLETE));
        verify(dao, never()).getLowestDrillGroupId();
    }
}
