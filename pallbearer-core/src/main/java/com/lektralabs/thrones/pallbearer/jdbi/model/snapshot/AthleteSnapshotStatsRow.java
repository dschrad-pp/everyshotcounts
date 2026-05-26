package com.lektralabs.thrones.pallbearer.jdbi.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AthleteSnapshotStatsRow {
    private int totalMakes;
    private int totalAttempts;
    private int sessionCount;
    private int bestSessionMakes;
    private int worstSessionMisses;
    private int roundsToPass;
}
