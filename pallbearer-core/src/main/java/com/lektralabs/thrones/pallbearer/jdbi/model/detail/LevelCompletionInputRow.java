package com.lektralabs.thrones.pallbearer.jdbi.model.detail;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The minimum set of fields the "did the athlete pass this drill" predicate
 * needs — nothing else.
 * <p>
 * Level-completion %% used to be derived from a full {@link AthleteDrillDetail}
 * graph: an 8-table join returning ~50 columns for every drill item in the
 * athlete's whole group (created-by user + contact, modified-by user + contact,
 * media, drill group…), plus a second batched attempt-history round-trip, all
 * so Java could count how many drills at ONE level were passed. This row is the
 * same data at the same grain — one row per (drill item, drill) pair, matching
 * {@code AthleteDrillDetailRowReducer}'s key — with only the columns the
 * predicate reads.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LevelCompletionInputRow {

    /** True for graded "Test" drills, which advance on any attempt. */
    private Boolean levelTest;

    /** Null when the athlete has no drill row for this item (never attempted). */
    private String drillStatus;

    /** Makes-to-advance threshold; null means "completion is passing". */
    private Integer passingScore;

    /** Athlete-corrected score; falls back to {@link #makesDetected} when null. */
    private Integer makesReported;

    /** AI-detected score, used when the athlete did not override it. */
    private Integer makesDetected;

    /** False when no drill row exists for the item — mirrors an empty {@code DrillDetail}. */
    private boolean drillPresent;
}
