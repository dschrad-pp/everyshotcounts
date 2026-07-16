package com.lektralabs.thrones.pallbearer.jdbi.model.snapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * JDBI bean-mapped row for {@code getTeamLeaderboardByTeamId}: one team member's
 * name plus the same per-athlete totals as {@link TeamAthleteStatsRow}, so the
 * athlete-facing leaderboard can be served without a per-athlete fan-out.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamLeaderboardRow {
    private UUID userId;
    private String firstName;
    private String lastName;
    private int totalMakes;
    private int totalAttempts;
    private int sessionCount;
    private int levelOrderIndex;
}
