package com.lektralabs.thrones.pallbearer.jdbi.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoachNotificationServiceTest {

    // ── isScoreAdjusted ──────────────────────────────────────────────────────

    @Test
    void unedited_reportedEqualsDetected_notAdjusted() {
        // iOS pre-fills reported with the AI-detected value when the athlete does not edit.
        assertFalse(CoachNotificationService.isScoreAdjusted(5, 10, 5, 10));
    }

    @Test
    void makesEdited_isAdjusted() {
        assertTrue(CoachNotificationService.isScoreAdjusted(5, 10, 9, 10));
    }

    @Test
    void attemptsEdited_isAdjusted() {
        assertTrue(CoachNotificationService.isScoreAdjusted(5, 10, 5, 12));
    }

    @Test
    void bothEdited_isAdjusted() {
        assertTrue(CoachNotificationService.isScoreAdjusted(5, 10, 9, 12));
    }

    @Test
    void aiDetectedZero_athleteReportsRealScore_isAdjusted() {
        assertTrue(CoachNotificationService.isScoreAdjusted(0, 0, 5, 10));
    }

    @Test
    void editedToSameValueAsDetected_notAdjusted() {
        assertFalse(CoachNotificationService.isScoreAdjusted(5, 10, 5, 10));
    }

    @Test
    void nullReported_notTreatedAsAdjustment() {
        // A missing reported value must not be coalesced to 0 and flagged as an edit.
        assertFalse(CoachNotificationService.isScoreAdjusted(5, 10, null, null));
    }

    @Test
    void nullMakesReported_attemptsEdited_isAdjusted() {
        assertTrue(CoachNotificationService.isScoreAdjusted(5, 10, null, 12));
    }

    @Test
    void reportedZero_detectedNonZero_isAdjusted() {
        // 0 is a legitimate reported value (athlete made nothing), distinct from null.
        assertTrue(CoachNotificationService.isScoreAdjusted(5, 10, 0, 10));
    }
}
