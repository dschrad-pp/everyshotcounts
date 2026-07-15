package com.lektralabs.thrones.pallbearer.jdbi.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamAthleteStatsRow {
    private UUID userId;
    private int totalMakes;
    private int totalAttempts;
    private int sessionCount;
    private int levelOrderIndex;
}
