package com.lektralabs.thrones.pallbearer.manager.utils;

import com.lektralabs.thrones.pallbearer.common.DrillStatusConstants;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.AthleteDrillDetail;
import com.lektralabs.thrones.pallbearer.jdbi.model.detail.DrillDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the pass rule that level-completion %% is built on.
 * <p>
 * The rule now has two entry points — the raw-field overload used by the cheap
 * {@code LevelCompletionInputRow} query, and the {@link AthleteDrillDetail}
 * form used by the full-object paths. These tests assert both that the rule
 * itself is right AND that the two entry points agree, because the whole point
 * of the level-completion query rewrite is that the number does not move.
 */
class AthleteManagerUtilsPassedTest {

    private final AthleteManagerUtils utils = new AthleteManagerUtils();

    // ── the rule ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("no drill row for the item is not a pass")
    void noDrillRow_isNotPassed() {
        assertFalse(utils.isPassed(false, null, 10, null, null, false));
    }

    @Test
    @DisplayName("regular drill: complete and at the threshold passes")
    void regularDrill_meetsPassingScore_passes() {
        assertTrue(utils.isPassed(false, DrillStatusConstants.COMPLETE, 10, 10, null, true));
    }

    @Test
    @DisplayName("regular drill: complete but under the threshold fails")
    void regularDrill_underPassingScore_fails() {
        assertFalse(utils.isPassed(false, DrillStatusConstants.COMPLETE, 16, 12, null, true));
    }

    @Test
    @DisplayName("regular drill: submitted but not COMPLETE fails regardless of makes")
    void regularDrill_notComplete_fails() {
        assertFalse(utils.isPassed(false, DrillStatusConstants.NOT_ATTEMPTED, 10, 99, null, true));
    }

    @Test
    @DisplayName("regular drill: no passing score configured means completion is passing")
    void regularDrill_noPassingScore_completionIsPassing() {
        assertTrue(utils.isPassed(false, DrillStatusConstants.COMPLETE, null, null, null, true));
    }

    @Test
    @DisplayName("makes falls back to the detected score when the athlete did not override it")
    void nullReported_fallsBackToDetected() {
        assertTrue(utils.isPassed(false, DrillStatusConstants.COMPLETE, 10, null, 12, true));
        assertFalse(utils.isPassed(false, DrillStatusConstants.COMPLETE, 10, null, 8, true));
    }

    @Test
    @DisplayName("reported score wins over detected when both are present")
    void reportedWinsOverDetected() {
        assertFalse(utils.isPassed(false, DrillStatusConstants.COMPLETE, 10, 4, 99, true));
    }

    @Test
    @DisplayName("level test advances on any attempt, ignoring the passing score")
    void levelTest_anyAttemptPasses() {
        assertTrue(utils.isPassed(true, DrillStatusConstants.COMPLETE, 100, 0, null, true));
        assertFalse(utils.isPassed(true, DrillStatusConstants.NOT_ATTEMPTED, 100, 0, null, true));
    }

    @Test
    @DisplayName("a drill row with no status is not a pass")
    void nullStatus_isNotPassed() {
        assertFalse(utils.isPassed(false, null, 10, 99, null, true));
        assertFalse(utils.isPassed(true, null, 10, 99, null, true));
    }

    // ── the two entry points agree ────────────────────────────────────────────

    @Test
    @DisplayName("object form and raw-field form return the same verdict")
    void objectFormDelegatesToRawFieldForm() {
        assertEquals(
                utils.isPassed(false, DrillStatusConstants.COMPLETE, 16, 12, null, true),
                utils.isPassed(detail(false, DrillStatusConstants.COMPLETE, 16, 12, null)));
        assertEquals(
                utils.isPassed(false, DrillStatusConstants.COMPLETE, 16, 16, null, true),
                utils.isPassed(detail(false, DrillStatusConstants.COMPLETE, 16, 16, null)));
        assertEquals(
                utils.isPassed(true, DrillStatusConstants.COMPLETE, 16, 0, null, true),
                utils.isPassed(detail(true, DrillStatusConstants.COMPLETE, 16, 0, null)));
    }

    @Test
    @DisplayName("object form with no drill detail matches drillPresent=false")
    void objectFormWithoutDrillDetail_matchesAbsentRow() {
        AthleteDrillDetail noDrill = new AthleteDrillDetail();
        noDrill.setLevelTest(false);
        noDrill.setPassingScore(10);
        noDrill.setDrillDetail(Optional.empty());

        assertEquals(utils.isPassed(false, null, 10, null, null, false), utils.isPassed(noDrill));
        assertFalse(utils.isPassed(noDrill));
    }

    private AthleteDrillDetail detail(Boolean levelTest, String status,
            Integer passingScore, Integer makesReported, Integer makesDetected) {
        DrillDetail drillDetail = new DrillDetail();
        drillDetail.setDrillStatus(status);
        drillDetail.setMakesReported(makesReported);
        drillDetail.setMakesDetected(makesDetected);

        AthleteDrillDetail athleteDrillDetail = new AthleteDrillDetail();
        athleteDrillDetail.setLevelTest(levelTest);
        athleteDrillDetail.setPassingScore(passingScore);
        athleteDrillDetail.setDrillDetail(Optional.of(drillDetail));
        return athleteDrillDetail;
    }
}
