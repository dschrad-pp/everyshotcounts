package com.lektralabs.thrones.pallbearer.jdbi.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated make/attempt counts for a single court zone (THREE_POINT,
 * FIFTEEN_FEET or FREE_THROW). makePercent is intentionally not stored here —
 * it is derived at the response layer via
 * {@link com.lektralabs.thrones.pallbearer.jdbi.service.CoachAthleteSnapshotService#computeMakePercent(int, int)}
 * so the rounding/clamping rule stays identical to the rest of the snapshot.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShootingZoneRow {
    private String zoneCode;
    private int totalMakes;
    private int totalAttempts;
}
